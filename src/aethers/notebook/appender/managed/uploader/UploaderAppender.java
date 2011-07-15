package aethers.notebook.appender.managed.uploader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import aethers.notebook.R;
import aethers.notebook.appender.managed.uploader.Configuration.ConnectionType;
import aethers.notebook.core.Action;
import aethers.notebook.core.ManagedAppenderService;
import aethers.notebook.core.LoggerServiceIdentifier;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

public class UploaderAppender
extends Service
implements Runnable
{
    private static final String ENCODING = "UTF-8";
    
    private static final ArrayList<Action> actions = new ArrayList<Action>();
    static 
    {
        Action upload = new Action(
                "aethers.notebook.appender.managed.uploader.UploaderAppender.upload");
        upload.setName("Upload");
        upload.setDescription("Upload all complete log files now");
        actions.add(upload);
    }
    
    private final ManagedAppenderService.Stub appenderServiceStub = 
        new ManagedAppenderService.Stub()
        {
            @Override
            public void stop()
            throws RemoteException 
            {
                UploaderAppender.this.stopSelf();
            }
            
            @Override
            public void start()
            throws RemoteException 
            {
                startService(new Intent(
                        UploaderAppender.this, 
                        UploaderAppender.this.getClass()));
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
                                checkSize();
                                List<File> ready = checkUploadConditions();
                                if(ready.size() > 0)
                                    upload(ready);
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
                return running;
            }
            
            @Override
            public void configure() 
            throws RemoteException 
            {
                Intent i = new Intent(UploaderAppender.this, ConfigurationActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }

            @Override
            public List<Action> listActions() 
            throws RemoteException 
            {
                return actions;
            }

            @Override
            public void doAction(Action action) 
            throws RemoteException 
            {
                File[] files = currentDirectory.listFiles(new FilenameFilter()
                {
                    @Override
                    public boolean accept(File dir, String filename) 
                    {
                        Log.d("test", filename);
                        return filename.endsWith(".gz");
                    }
                });
                if(files != null && files.length > 0)
                    upload(Arrays.asList(files));
            }
        };
        
    private final Object sync = new Object();
    
    private final Object fileLockSync = new Object();
    
    private volatile Writer fileOut;
    
    private volatile File currentDirectory;
    
    private volatile File currentFile;
    
    private volatile boolean running = false;
    
    private Handler handler;
    
    private Configuration configuration;
    
    private WifiManager wifiManager;
    
    private TelephonyManager telephonyManager;

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
    public void onCreate() 
    {
        super.onCreate();
        wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
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
        configuration = new Configuration(this);
        synchronized(fileLockSync)
        {
            try
            {
                currentDirectory = configuration.getLogDirectory();
                if(!currentDirectory.exists())
                    currentDirectory.mkdirs();
                currentFile = File.createTempFile("aether", "", currentDirectory);
                fileOut = new OutputStreamWriter(new GZIPOutputStream(
                        new BufferedOutputStream(new FileOutputStream(currentFile))), ENCODING);
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
                                R.string.UploaderAppender_Preferences_logDirectory)))
                            return;
                        synchronized(fileLockSync)
                        {
                            try
                            {
                                fileOut.flush();
                                fileOut.close();
                                File oldDir = currentDirectory;
                                currentDirectory = configuration.getLogDirectory();
                                if(!currentDirectory.exists())
                                    currentDirectory.mkdirs();
                                currentFile.renameTo(new File(currentDirectory, currentFile.getName() + ".gz"));
                                for(File f : oldDir.listFiles())
                                    f.renameTo(new File(currentDirectory, f.getName()));
                                currentFile = File.createTempFile("aether", "", currentDirectory);
                                fileOut = new OutputStreamWriter(new GZIPOutputStream(
                                        new BufferedOutputStream(new FileOutputStream(currentFile))), ENCODING);
                            }
                            catch(IOException e)
                            {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                };
        configuration.registerChangeListener(listener);
        Looper.prepare();
        handler = new Handler();        
        Looper.loop();
        configuration.unregisterChangeListener(listener);
        synchronized(fileLockSync)
        {
            try
            {
                fileOut.flush();
                fileOut.close();
                currentFile.renameTo(new File(currentFile.getParentFile(), currentFile.getName() + ".gz"));
                fileOut = null;
                currentFile = null;
                currentDirectory = null;
            }
            catch(IOException e)
            {
                throw new RuntimeException(e);
            }
        }    
    }
    
    private void checkSize()
    {
        synchronized(fileLockSync)
        {
            if(currentFile.length() >= (configuration.getMaxFileSize() * 1024))
            {
                try
                {
                    fileOut.flush();
                    fileOut.close();
                    currentFile.renameTo(new File(currentFile.getParentFile(), currentFile.getName() + ".gz"));
                    currentFile = File.createTempFile("aether", "", currentDirectory);
                    fileOut = new OutputStreamWriter(new GZIPOutputStream(
                            new BufferedOutputStream(new FileOutputStream(currentFile))), ENCODING);
                }
                catch(Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    
    private List<File> checkUploadConditions()
    {
        ConnectionType ct = configuration.getConnectionType();
        if(ct.equals(ConnectionType.Manual))
            return new ArrayList<File>();
        
        File[] files = currentDirectory.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String filename) 
            {
                return filename.endsWith(".gz");
            }
        });
        if(files == null || files.length == 0)
            return new ArrayList<File>();
                
        if((ct.equals(ConnectionType.Wifi) || ct.equals(ConnectionType.WifiAnd3G)) 
                && wifiManager.isWifiEnabled()
                && wifiManager.pingSupplicant())
            return Arrays.asList(files);
        
        if(ct.equals(ConnectionType.WifiAnd3G)
                && telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED)
            return Arrays.asList(files);
        
        return new ArrayList<File>();
    }
    
    private void upload(List<File> files)
    {
        Log.d("test", "uploading");
        HttpClient client = new DefaultHttpClient();
        URL url = configuration.getUrl();
        try
        {
            for(File f : files)
            {
                try 
                {
                    HttpPost httppost = new HttpPost(url.toURI());
                    FileEntity reqEntity = new FileEntity(f, "application/x-gzip");
                    reqEntity.setContentType("binary/octet-stream");
                    reqEntity.setChunked(true);
                    httppost.setEntity(reqEntity);
                    HttpResponse response = client.execute(httppost);
                    HttpEntity resEntity = response.getEntity();
                    EntityUtils.toString(resEntity);
                    resEntity.consumeContent();
                    f.delete();
                }
                catch (Exception e) 
                {
                    Log.e("test", "OH NO!", e);
                }
            }
        }
        finally
        {
            client.getConnectionManager().shutdown();
        }
    }
}
