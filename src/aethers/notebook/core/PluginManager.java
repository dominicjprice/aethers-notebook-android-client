package aethers.notebook.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;

public class PluginManager
{
    public static interface LoggerServicesFoundCallback
    {
        public void servicesFound(Map<ComponentName, LoggerServiceIdentifier> services);
    }
    
    public static interface AppenderServicesFoundCallback
    {
        public void servicesFound(Map<ComponentName, AppenderServiceIdentifier> services);
    }
    
    public void findAppenderServices(
            final Context context,
            final AppenderServicesFoundCallback callback)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run() 
            {
                callback.servicesFound(findAppenderServices(context));                
            }
        }).start();
    }
    
    public void findLoggerServices(
            final Context context,
            final LoggerServicesFoundCallback callback)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run() 
            {
                callback.servicesFound(findLoggerServices(context));                
            }
        }).start();
    }
    
    private Map<ComponentName, AppenderServiceIdentifier> findAppenderServices(final Context context)
    {
        PackageManager pm = context.getPackageManager();
        Intent i = new Intent();
        i.setAction("aethers.notebook.action.CORE");
        i.addCategory("aethers.notebook.action.category.APPENDER");

        final Map<ComponentName, AppenderServiceIdentifier> services = 
                Collections.synchronizedMap(
                        new HashMap<ComponentName, AppenderServiceIdentifier>());
        final List<ComponentName> queue = Collections.synchronizedList(
                new ArrayList<ComponentName>());
        for(ResolveInfo info : pm.queryIntentServices(i, 0))
        {
            final ComponentName name = new ComponentName(
                    info.serviceInfo.applicationInfo.packageName,
                    info.serviceInfo.name);
            Intent ci = new Intent();
            ci.setComponent(name);
            if(context.bindService(ci, new ServiceConnection()
            {
                @Override public void onServiceDisconnected(ComponentName name) { }
                
                @Override
                public void onServiceConnected(ComponentName cname, IBinder service) 
                {
                    synchronized(queue)
                    {
                        while(!queue.contains(name))
                            try { queue.wait(); }
                            catch(InterruptedException e) { }
                        ManagedAppenderService s = ManagedAppenderService.Stub.asInterface(service);
                        try
                        {
                            services.put(name, s.getIdentifier());
                        }
                        catch(RemoteException e)
                        {
                            throw new RuntimeException(e);
                        }
                        finally
                        {
                            queue.remove(name);
                            queue.notifyAll();
                            context.unbindService(this);
                        }   
                    }
                }
            }, Context.BIND_AUTO_CREATE))
                queue.add(name);
        }
        synchronized(queue)
        {
            while(queue.size() > 0)
            {
                try { queue.wait(); }
                catch(InterruptedException e) { }
            }
        }
        return services;
    }
    
    private Map<ComponentName, LoggerServiceIdentifier> findLoggerServices(final Context context)
    {
        PackageManager pm = context.getPackageManager();
        Intent i = new Intent();
        i.setAction("aethers.notebook.action.CORE");
        i.addCategory("aethers.notebook.action.category.LOGGER");

        final Map<ComponentName, LoggerServiceIdentifier> services = 
                Collections.synchronizedMap(
                        new HashMap<ComponentName, LoggerServiceIdentifier>());
        final List<ComponentName> queue = Collections.synchronizedList(
                new ArrayList<ComponentName>());
        for(ResolveInfo info : pm.queryIntentServices(i, 0))
        {
            final ComponentName name = new ComponentName(
                    info.serviceInfo.applicationInfo.packageName,
                    info.serviceInfo.name);
            Intent ci = new Intent();
            ci.setComponent(name);
            if(context.bindService(ci, new ServiceConnection()
            {
                @Override public void onServiceDisconnected(ComponentName name) { }
                
                @Override
                public void onServiceConnected(ComponentName cname, IBinder service) 
                {
                    synchronized(queue)
                    {
                        while(!queue.contains(name))
                            try { queue.wait(); }
                            catch(InterruptedException e) { }
                        LoggerService s = LoggerService.Stub.asInterface(service);
                        try
                        {
                            services.put(name, s.getIdentifier());
                        }
                        catch(RemoteException e)
                        {
                            throw new RuntimeException(e);
                        }
                        finally
                        {
                            queue.remove(name);
                            queue.notifyAll();
                            context.unbindService(this);
                        }   
                    }
                }
            }, Context.BIND_AUTO_CREATE))
                queue.add(name);
        }
        synchronized(queue)
        {
            while(queue.size() > 0)
            {
                try { queue.wait(); }
                catch(InterruptedException e) { }
            }
        }
        return services;
    }
}
