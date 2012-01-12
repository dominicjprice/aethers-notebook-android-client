package aethers.notebook.core;

import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
    
    protected void setBoolean(int prefName, boolean value)
    {
        Editor e = prefs.edit();
        e.putBoolean(context.getString(prefName), value);
        e.commit();
    }
    
    protected int getInt(int prefName, int prefDefault)
    {
        return prefs.getInt(context.getString(prefName),
                Integer.parseInt(context.getString(prefDefault)));
    }
    
    protected void setInt(int prefName, int value)
    {
        Editor e = prefs.edit();
        e.putInt(context.getString(prefName), value);
        e.commit();
    }
    
    protected long getLong(int prefName, int prefDefault)
    {
        return prefs.getLong(context.getString(prefName),
                Integer.parseInt(context.getString(prefDefault)));
    }
    
    protected void setLong(int prefName, long value)
    {
        Editor e = prefs.edit();
        e.putLong(context.getString(prefName), value);
        e.commit();
    }
    
    protected float getFloat(int prefName, int prefDefault)
    {
        return prefs.getFloat(context.getString(prefName),
                Integer.parseInt(context.getString(prefDefault)));
    }
    
    protected void setFloat(int prefName, float value)
    {
        Editor e = prefs.edit();
        e.putFloat(context.getString(prefName), value);
        e.commit();
    }
    
    protected String getString(int prefName, int prefDefault)
    {
        return prefs.getString(context.getString(prefName),
                context.getString(prefDefault));
    }
    
    protected void setString(int prefName, String value)
    {
        Editor e = prefs.edit();
        e.putString(context.getString(prefName), value);
        e.commit();
    }
    
    protected List<String> getStringList(int prefName, int prefDefault)
    {
        String l = prefs.getString(context.getString(prefName),
                context.getString(prefDefault));
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            return mapper.readValue(l, new TypeReference<List<String>>() { });
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    protected void setStringList(int prefName, List<String> value)
    {
        try
        {
            Editor e = prefs.edit();
            ObjectMapper mapper = new ObjectMapper();
            e.putString(context.getString(prefName), mapper.writeValueAsString(value));
            e.commit();
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
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
