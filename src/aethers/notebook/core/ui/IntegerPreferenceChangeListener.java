package aethers.notebook.core.ui;

import android.content.Context;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.widget.Toast;

public class IntegerPreferenceChangeListener
implements OnPreferenceChangeListener
{
    private final int minimum;
    
    private final int maximum;
    
    private final String toastMessage;
    
    private final Context context;
    
    public IntegerPreferenceChangeListener(
            int minimum,
            int maximum,
            String toastMessage,
            Context context)
    {
        this.minimum = minimum;
        this.maximum = maximum;
        this.toastMessage = toastMessage;
        this.context = context;
    }   
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) 
    {
        try
        {
            int val = Integer.parseInt((String)newValue);
            if(val < minimum || val > maximum)
                throw new RuntimeException();
            return true;
        }
        catch(Exception e)
        {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
            return false;   
        }
    }
}
