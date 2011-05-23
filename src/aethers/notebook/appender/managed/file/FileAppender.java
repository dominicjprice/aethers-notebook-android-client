package aethers.notebook.appender.managed.file;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import aethers.notebook.R;
import aethers.notebook.core.AppenderService;
import aethers.notebook.core.LoggerServiceIdentifier;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

public class FileAppender
extends Service
implements Runnable
{
    private final AppenderService.Stub appenderServiceStub = 
            new AppenderService.Stub()
            {
                @Override
                public void stop()
                throws RemoteException 
                {
                    FileAppender.this.stopSelf();
                }
                
                @Override
                public void start()
                throws RemoteException 
                {
                    startService(new Intent(
                            FileAppender.this, 
                            FileAppender.this.getClass()));
                }
                
                @Override
                public void log(
                        final LoggerServiceIdentifier identifier,
                        final long timestamp,
                        final Location location,
                        final byte[] data)
                throws RemoteException 
                {
                    handler.post(new Runnable()
                    {
                        @Override
                        public void run() 
                        {
                            synchronized(fileLockSync)
                            {
                                try
                                {
                                    ObjectMapper m = new ObjectMapper();
                                    JsonFactory fac = new JsonFactory();
                                    JsonGenerator gen = fac.createJsonGenerator(fileOut);
                                    gen.setCodec(m);
                                    ObjectNode o = m.createObjectNode();
                                    ObjectNode o2 = o.objectNode();
                                    o2.put("uniqueID", identifier.getUniqueID());
                                    o2.put("version", identifier.getVersion());
                                    o.put("identifier", o2);
                                    o.put("timestamp", timestamp);
                                    o.putPOJO("location", location);
                                    o.put("data", data);
                                    m.writeTree(gen, o);
                                    fileOut.write("\n");
                                    fileOut.flush();
                                }
                                catch(Exception e)
                                {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    });
                }
                
                @Override
                public boolean isRunning() 
                throws RemoteException 
                {
                    synchronized(sync)
                    {
                        return running;
                    }
                }
                
                @Override
                public void configure() 
                throws RemoteException 
                {
                    Intent i = new Intent(FileAppender.this, ConfigurationActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            };
    
    private final Object sync = new Object();
    
    private final Object fileLockSync = new Object();
    
    private volatile Writer fileOut;
    
    private volatile boolean running = false;
    
    private Handler handler;
    
    @Override
    public IBinder onBind(Intent intent) 
    {
        return appenderServiceStub;
    }
    
    @Override
    public void onDestroy() 
    {
        super.onDestroy();
        synchronized(sync)
        {
            if(running)
            {
                running = false;
                if(handler != null)
                {
                    handler.getLooper().quit();
                    handler = null;
                }
            }
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) 
    {      
        synchronized(sync)
        {
            if(running)
                return START_STICKY;
            running = true;
            new Thread(this).start();
            return START_STICKY;
        }        
    }

    @Override
    public void run() 
    {
        final Configuration config = new Configuration(this);
        synchronized(fileLockSync)
        {
            try
            {
                fileOut = new BufferedWriter(new FileWriter(config.getLogfilePath(), true));
            }
            catch(IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        final OnSharedPreferenceChangeListener listener =
                new OnSharedPreferenceChangeListener()
                {
                    
                    @Override
                    public void onSharedPreferenceChanged(
                            SharedPreferences sharedPreferences,
                            String key) 
                    {
                        if(!key.equals(getString(
                                R.string.FileAppender_Preferences_logfilePath)))
                            return;
                        synchronized(fileLockSync)
                        {
                            try
                            {
                                fileOut.close();
                                fileOut = new BufferedWriter(new FileWriter(config.getLogfilePath(), true));
                            }
                            catch(IOException e)
                            {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                };
        config.registerChangeListener(listener);
        Looper.prepare();
        handler = new Handler();        
        Looper.loop();
        config.unregisterChangeListener(listener);
        try
        {
            fileOut.close();
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
