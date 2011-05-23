package aethers.notebook.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class ConfigurationTemplate
{
    private final Context context;
    
    private final SharedPreferences prefs;
    
    public ConfigurationTemplate(Context context, String sharedPreferencesName) 
    {
        this.context = context;
        prefs = context.getSharedPreferences(
                sharedPreferencesName, 
                Context.MODE_PRIVATE);
    }
    
    public void registerChangeListener(OnSharedPreferenceChangeListener listener)
    {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }
    
    public void unregisterChangeListener(OnSharedPreferenceChangeListener listener)
    {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }
    
    protected boolean getBoolean(int prefName, int prefDefault)
    {
        return prefs.getBoolean(context.getString(prefName), 
                Boolean.parseBoolean(context.getString(prefDefault)));
    }
    
    protected int getInt(int prefName, int prefDefault)
    {
        return prefs.getInt(context.getString(prefName),
                Integer.parseInt(context.getString(prefDefault)));
    }
    
    protected String getString(int prefName, int prefDefault)
    {
        return prefs.getString(context.getString(prefName),
                context.getString(prefDefault));
    }
    
    protected Context getContext()
    {
        return context;
    }
    
    protected SharedPreferences getSharedPreferences()
    {
        return prefs;
    }
}
