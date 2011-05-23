package aethers.notebook.appender.managed.file;

import aethers.notebook.R;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class ConfigurationActivity 
extends PreferenceActivity
{
    private static final int REQUEST_CODE = 111;
    
    private Preference filePathPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(Configuration.SHARED_PREFERENCES_NAME);
        addPreferencesFromResource(R.xml.fileappender);
        filePathPreference = findPreference(getString(R.string.FileAppender_Preferences_logfilePath));
        filePathPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference) 
            {
                Configuration config = new Configuration(ConfigurationActivity.this);
                Intent i = new Intent();
                i.setAction("org.openintents.action.PICK_FILE");
                i.setData(Uri.parse(config.getLogfilePath()));
                startActivityForResult(i, REQUEST_CODE);
                return true;
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        if(REQUEST_CODE != requestCode)
            return;
        if(data == null || data.getData() == null)
            return;
        Editor e = getPreferenceManager().getSharedPreferences().edit();
        e.putString(
                filePathPreference.getKey(),
                data.getDataString().replace("file:/", ""));
    }
}
