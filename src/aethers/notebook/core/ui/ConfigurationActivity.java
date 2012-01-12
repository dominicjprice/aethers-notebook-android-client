package aethers.notebook.core.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aethers.notebook.R;
import aethers.notebook.core.Action;
import aethers.notebook.core.AppenderServiceIdentifier;
import aethers.notebook.core.LoggerServiceIdentifier;
import aethers.notebook.core.ManagedAppenderService;
import aethers.notebook.core.Configuration;
import aethers.notebook.core.CoreService;
import aethers.notebook.core.LoggerService;
import aethers.notebook.core.PluginManager;
import aethers.notebook.util.Logger;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class ConfigurationActivity
extends PreferenceActivity
{    
    private static Logger logger = Logger.getLogger(ConfigurationActivity.class);
    
    private class LoggerConfigureOnClickListener
    implements Preference.OnPreferenceClickListener
    {
        private final String serviceClass;
        
        public LoggerConfigureOnClickListener(String serviceClass)
        {
            this.serviceClass = serviceClass;
        }
        
        @Override
        public boolean onPreferenceClick(Preference p) 
        {
            if(loggerConnectionPool.containsKey(serviceClass))
            {
                try
                {
                    loggerConnectionPool.get(serviceClass).configure();
                }
                catch(RemoteException e)
                {
                    throw new RuntimeException(e);
                }
                return false;
            }
            
            ServiceConnection conn;
            Intent i = new Intent();
            i.setComponent(ComponentName.unflattenFromString(serviceClass));
            ConfigurationActivity.this.bindService(
                    i,
                    conn = new ServiceConnection()
                    {
                        @Override
                        public void onServiceDisconnected(ComponentName name)
                        { 
                            loggerConnectionPool.remove(serviceClass);
                        }
                        
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) 
                        { 
                            try
                            {
                                LoggerService s = (LoggerService)service;
                                loggerConnectionPool.put(serviceClass, s);
                                s.configure();
                            }
                            catch(RemoteException e)
                            {
                                throw new RuntimeException(e);
                            }                            
                        }
                    }, 
                    BIND_AUTO_CREATE);
            serviceConnections.add(conn);
            return true;
        }   
    }
    
    private class AppenderConfigureOnClickListener
    implements Preference.OnPreferenceClickListener
    {
        private final String serviceClass;
        
        public AppenderConfigureOnClickListener(String serviceClass)
        {
            this.serviceClass = serviceClass;
        }
        
        @Override
        public boolean onPreferenceClick(Preference p) 
        {
            if(appenderConnectionPool.containsKey(serviceClass))
            {
                try
                {
                    appenderConnectionPool.get(serviceClass).configure();
                }
                catch(RemoteException e)
                {
                    throw new RuntimeException(e);
                }
                return false;
            }
            
            ServiceConnection conn;
            Intent i = new Intent();
            i.setComponent(ComponentName.unflattenFromString(serviceClass));
            ConfigurationActivity.this.bindService(
                    i,
                    conn = new ServiceConnection()
                    {
                        @Override
                        public void onServiceDisconnected(ComponentName name)
                        { 
                            appenderConnectionPool.remove(serviceClass);
                        }
                        
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) 
                        { 
                            try
                            {
                                ManagedAppenderService s = ManagedAppenderService.Stub.asInterface(service);
                                appenderConnectionPool.put(serviceClass, s);
                                s.configure();
                            }
                            catch(RemoteException e)
                            {
                                throw new RuntimeException(e);
                            }                            
                        }
                    }, 
                    BIND_AUTO_CREATE);
            serviceConnections.add(conn);
            return true;
        }   
    }
    
    private Map<String, LoggerService> loggerConnectionPool = 
            Collections.synchronizedMap(new HashMap<String, LoggerService>());
    
    private Map<String, ManagedAppenderService> appenderConnectionPool = 
            Collections.synchronizedMap(new HashMap<String, ManagedAppenderService>());
    
    private List<ServiceConnection> serviceConnections = 
            Collections.synchronizedList(new ArrayList<ServiceConnection>());
    
    private Configuration configuration;
    
    private OnSharedPreferenceChangeListener enabledListener;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(
                Configuration.SHARED_PREFERENCES_NAME);
        configuration = new Configuration(this);
        addPreferencesFromResource(R.xml.coreservice);
        Preference minDistance = findPreference(getString(R.string.Preferences_locationMinDistance));
        Preference minTime = findPreference(getString(R.string.Preferences_locationMinTime));
        IntegerPreferenceChangeListener listener = new IntegerPreferenceChangeListener(
                0, Integer.MAX_VALUE, 
                "You must input a whole number greater than or equal to 0", this);
        minDistance.setOnPreferenceChangeListener(listener);
        minTime.setOnPreferenceChangeListener(listener);
        setupCorePreferences();
        setupLoggerPreferences();
        setupAppenderPreferences();
    }
    
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        if(enabledListener != null)
            configuration.unregisterChangeListener(enabledListener);
        for(ServiceConnection sc : serviceConnections)
            unbindService(sc);
    }
    
    @Override
    protected void onStart() 
    {
        super.onStart();
        if(configuration.isEnabled())
            startService(new Intent(this, CoreService.class));
    }
    
    private void setupCorePreferences()
    {
        configuration.registerChangeListener(
                enabledListener = new SharedPreferences.OnSharedPreferenceChangeListener()
                {
                    @Override
                    public void onSharedPreferenceChanged(
                            SharedPreferences sharedPreferences,
                            String key) 
                    {
                        if(!key.equals(getString(R.string.Preferences_enabled)))
                            return;
                        Intent intent = new Intent(ConfigurationActivity.this, CoreService.class);
                        if(configuration.isEnabled())
                            startService(intent);
                    }
                });
    }
    
    private void setupLoggerPreferences()
    {
        final Configuration conf = new Configuration(this);
        final List<String> enabledLoggers = conf.getEnabledLoggers();
        final PreferenceCategory cat = (PreferenceCategory)findPreference(
                getString(R.string.Preferences_category_loggers));
        
        new PluginManager().findLoggerServices(this, new PluginManager.LoggerServicesFoundCallback()
        {
            @Override
            public void servicesFound(Map<ComponentName, LoggerServiceIdentifier> services) 
            {
                final ArrayList<Entry<ComponentName, LoggerServiceIdentifier>> loggers =
                    new ArrayList<Map.Entry<ComponentName,LoggerServiceIdentifier>>(
                            services.entrySet());      
                Collections.sort(loggers, new Comparator<Entry<ComponentName, LoggerServiceIdentifier>>()
                        {
                            @Override
                            public int compare(
                                    Entry<ComponentName, LoggerServiceIdentifier> object1,
                                    Entry<ComponentName, LoggerServiceIdentifier> object2) 
                            {
                                return object1.getValue().getName().compareTo(
                                        object2.getValue().getName());
                            }
                        });
                        
                for(final Entry<ComponentName, LoggerServiceIdentifier> entry : loggers) 
                {
                    final ComponentName name = entry.getKey();
                    final LoggerServiceIdentifier identifier = entry.getValue();
                    
                    final PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(ConfigurationActivity.this);
                    ps.setTitle(identifier.getName());
                    ps.setSummary(identifier.getDescription());
                                
                    final CheckBoxPreference activate = new CheckBoxPreference(ConfigurationActivity.this)
                    {
                        @Override protected boolean shouldPersist() { return false; }
                    };
                    activate.setKey(identifier.getUniqueID());
                    activate.setTitle("Enabled");
                    activate.setSummary("Enable/Disable this logger");
                    activate.setChecked(enabledLoggers.contains(identifier.getUniqueID()));
                    activate.setOnPreferenceClickListener(
                            new Preference.OnPreferenceClickListener()
                            { 
                                @Override
                                public boolean onPreferenceClick(Preference preference) 
                                {
                                    if(activate.isChecked() && 
                                            !enabledLoggers.contains(identifier.getUniqueID()))
                                    {
                                        logger.debug("Enabling logger (" + identifier.getUniqueID() + ")" );
                                        enabledLoggers.add(identifier.getUniqueID());
                                        conf.setEnabledLoggers(enabledLoggers);                            
                                    }
                                    else if(!activate.isChecked() &&
                                            enabledLoggers.contains(identifier.getUniqueID()))
                                    {
                                        logger.debug("Disabling logger (" + identifier.getUniqueID() + ")" );
                                        enabledLoggers.remove(identifier.getUniqueID());
                                        conf.setEnabledLoggers(enabledLoggers);
                                    }
                                    
                                    return true;
                                }
                            });
                    ps.addItemFromInflater(activate);
                    
                    if(identifier.isConfigurable())
                    {
                        NonPersistingButtonPreference configure = new NonPersistingButtonPreference(ConfigurationActivity.this);
                        configure.setTitle("Configure");
                        configure.setOnPreferenceClickListener(
                                new LoggerConfigureOnClickListener(
                                        name.flattenToString()));
                        ps.addItemFromInflater(configure);
                    }
                    
                    Intent i = new Intent();
                    i.setComponent(name);
                    bindService(
                            i,
                            new ServiceConnection()
                            {
                                @Override
                                public void onServiceDisconnected(ComponentName ccname) { }
                                
                                @Override
                                public void onServiceConnected(ComponentName ccname, IBinder service)
                                {
                                    LoggerService as = LoggerService.Stub.asInterface(service);
                                    try
                                    {
                                        List<Action> actions = as.listActions();
                                        if(actions == null || actions.size() == 0)
                                            return;
                                        for(final Action action : actions)
                                        {
                                            NonPersistingButtonPreference p = new NonPersistingButtonPreference(ConfigurationActivity.this);
                                            p.setTitle(action.getName());
                                            p.setSummary(action.getDescription());
                                            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
                                            {
                                                @Override
                                                public boolean onPreferenceClick(Preference preference) 
                                                {
                                                    
                                                    Intent i = new Intent();
                                                    i.setComponent(name);
                                                    bindService(i,
                                                            new ServiceConnection()
                                                            {
                                                                @Override
                                                                public void onServiceDisconnected(ComponentName name) { }
                                                                
                                                                @Override
                                                                public void onServiceConnected(ComponentName name, IBinder service) 
                                                                {
                                                                    ManagedAppenderService as = ManagedAppenderService.Stub.asInterface(service);
                                                                    try
                                                                    {
                                                                        as.doAction(action);
                                                                    }
                                                                    catch(RemoteException e)
                                                                    {
                                                                        throw new RuntimeException(e);
                                                                    }
                                                                    finally
                                                                    {
                                                                        unbindService(this);
                                                                    }
                                                                }
                                                            }, BIND_AUTO_CREATE);
                                                    return true;
                                                }
                                            });
                                            ps.addItemFromInflater(p);
                                        }
                                    }
                                    catch(RemoteException e)
                                    {
                                        throw new RuntimeException(e);
                                    }
                                    finally
                                    {
                                        unbindService(this);
                                    }
                                }
                            }, BIND_AUTO_CREATE);
                    
                    cat.addItemFromInflater(ps);
                }
            }
        });          
    }
    
    private void setupAppenderPreferences()
    {
        final Configuration conf = new Configuration(this);
        final List<String> enabledAppenders = conf.getEnabledAppenders();
        final PreferenceCategory cat = (PreferenceCategory)findPreference(
                getString(R.string.Preferences_category_appenders));
        
        new PluginManager().findAppenderServices(this, new PluginManager.AppenderServicesFoundCallback()
        {
            @Override
            public void servicesFound(Map<ComponentName, AppenderServiceIdentifier> services) 
            {
                final ArrayList<Entry<ComponentName, AppenderServiceIdentifier>> appenders =
                    new ArrayList<Map.Entry<ComponentName, AppenderServiceIdentifier>>(
                            services.entrySet());      
                Collections.sort(appenders, new Comparator<Entry<ComponentName, AppenderServiceIdentifier>>()
                        {
                            @Override
                            public int compare(
                                    Entry<ComponentName, AppenderServiceIdentifier> object1,
                                    Entry<ComponentName, AppenderServiceIdentifier> object2) 
                            {
                                return object1.getValue().getName().compareTo(
                                        object2.getValue().getName());
                            }
                        });
                        
                for(final Entry<ComponentName, AppenderServiceIdentifier> entry : appenders) 
                {
                    final ComponentName name = entry.getKey();
                    final AppenderServiceIdentifier identifier = entry.getValue();
                    
                    final PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(ConfigurationActivity.this);
                    ps.setTitle(identifier.getName());
                    ps.setSummary(identifier.getDescription());
                                
                    final CheckBoxPreference activate = new CheckBoxPreference(ConfigurationActivity.this)
                    {
                        @Override protected boolean shouldPersist() { return false; }
                    };
                    activate.setKey(identifier.getUniqueID());
                    activate.setTitle("Enabled");
                    activate.setSummary("Enable/Disable this appender");
                    activate.setChecked(enabledAppenders.contains(identifier.getUniqueID()));
                    activate.setOnPreferenceClickListener(
                            new Preference.OnPreferenceClickListener()
                            { 
                                @Override
                                public boolean onPreferenceClick(Preference preference) 
                                {
                                    if(activate.isChecked() && 
                                            !enabledAppenders.contains(identifier.getUniqueID()))
                                    {
                                        logger.debug("Enabling appender (" + identifier.getUniqueID() + ")" );
                                        enabledAppenders.add(identifier.getUniqueID());
                                        conf.setEnabledAppenders(enabledAppenders);                            
                                    }
                                    else if(!activate.isChecked() &&
                                            enabledAppenders.contains(identifier.getUniqueID()))
                                    {
                                        logger.debug("Disabling appender (" + identifier.getUniqueID() + ")" );
                                        enabledAppenders.remove(identifier.getUniqueID());
                                        conf.setEnabledAppenders(enabledAppenders);
                                    }
                                    
                                    return true;
                                }
                            });
                    ps.addItemFromInflater(activate);
                    
                    if(identifier.isConfigurable())
                    {
                        NonPersistingButtonPreference configure = new NonPersistingButtonPreference(ConfigurationActivity.this);
                        configure.setTitle("Configure");
                        configure.setOnPreferenceClickListener(
                                new AppenderConfigureOnClickListener(
                                        name.flattenToString()));
                        ps.addItemFromInflater(configure);
                    }
                    
                    Intent i = new Intent();
                    i.setComponent(name);
                    bindService(
                            i,
                            new ServiceConnection()
                            {
                                @Override
                                public void onServiceDisconnected(ComponentName ccname) { }
                                
                                @Override
                                public void onServiceConnected(ComponentName ccname, IBinder service)
                                {
                                    ManagedAppenderService as = ManagedAppenderService.Stub.asInterface(service);
                                    try
                                    {
                                        List<Action> actions = as.listActions();
                                        if(actions == null || actions.size() == 0)
                                            return;
                                        for(final Action action : actions)
                                        {
                                            NonPersistingButtonPreference p = new NonPersistingButtonPreference(ConfigurationActivity.this);
                                            p.setTitle(action.getName());
                                            p.setSummary(action.getDescription());
                                            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
                                            {
                                                @Override
                                                public boolean onPreferenceClick(Preference preference) 
                                                {
                                                    
                                                    Intent i = new Intent();
                                                    i.setComponent(name);
                                                    bindService(i,
                                                            new ServiceConnection()
                                                            {
                                                                @Override
                                                                public void onServiceDisconnected(ComponentName name) { }
                                                                
                                                                @Override
                                                                public void onServiceConnected(ComponentName name, IBinder service) 
                                                                {
                                                                    ManagedAppenderService as = ManagedAppenderService.Stub.asInterface(service);
                                                                    try
                                                                    {
                                                                        as.doAction(action);
                                                                    }
                                                                    catch(RemoteException e)
                                                                    {
                                                                        throw new RuntimeException(e);
                                                                    }
                                                                    finally
                                                                    {
                                                                        unbindService(this);
                                                                    }
                                                                }
                                                            }, BIND_AUTO_CREATE);
                                                    return true;
                                                }
                                            });
                                            ps.addItemFromInflater(p);
                                        }
                                    }
                                    catch(RemoteException e)
                                    {
                                        throw new RuntimeException(e);
                                    }
                                    finally
                                    {
                                        unbindService(this);
                                    }
                                }
                            }, BIND_AUTO_CREATE);
                    
                    cat.addItemFromInflater(ps);
                }
            }
        });
    }
}
