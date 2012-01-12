package aethers.notebook.core;

import java.util.List;

import aethers.notebook.R;
import android.content.Context;

public class Configuration
extends ConfigurationTemplate
{    
    public static final String SHARED_PREFERENCES_NAME
            = "aethers.notebook.core.Configuration";
    
    public Configuration(Context context) 
    {
        super(context, SHARED_PREFERENCES_NAME);        
    }    
    
    public boolean isEnabled()
    {
        return getBoolean(R.string.Preferences_enabled,
                R.string.Preferences_enabled_default);
    }
    
    public boolean isStartOnBoot()
    {
        return getBoolean(R.string.Preferences_startOnBoot,
                R.string.Preferences_startOnBoot_default);
    }
    
    public boolean isLocationLoggingEnabled()
    {
        return getBoolean(R.string.Preferences_logLocation,
                R.string.Preferences_logLocation_default);
    }
    
    public int getLocationMinimumDistance()
    {
        return Integer.parseInt(getString(
                R.string.Preferences_locationMinDistance,
                R.string.Preferences_locationMinDistance_default));
    }
    
    public int getLocationMinimumTime()
    {
        return Integer.parseInt(getString(
                R.string.Preferences_locationMinTime,
                R.string.Preferences_locationMinTime_default));
    }
    
    public List<String> getEnabledLoggers()
    {
        return getStringList(
                R.string.Preferences_loggers_enabled,
                R.string.Preferences_loggers_enabled_default);
    }
    
    public void setEnabledLoggers(List<String> loggers)
    {
        setStringList(R.string.Preferences_loggers_enabled, loggers);
    }
    
    public List<String> getEnabledAppenders()
    {
        return getStringList(
                R.string.Preferences_appenders_enabled,
                R.string.Preferences_appenders_enabled_default);
    }
    
    public void setEnabledAppenders(List<String> appenders)
    {
        setStringList(R.string.Preferences_appenders_enabled, appenders);
    }
}
