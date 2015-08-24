package com.android.settings.aicp;

import android.os.Bundle;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.R;



public class Ui extends SettingsPreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.ui);
    }
}


