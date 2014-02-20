package com.android.settings.aicp;

import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.Helpers;


public class Team extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "AicpTeam";

    Preference mLordClockan;
    Preference mZipsNet;
    Preference mN3ocort3x;
    Preference mSemdoc;
    Preference mEyosen;
    Preference mOblikas;
    Preference mTtoivainen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ContentResolver resolver = getActivity().getContentResolver();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.aicp_team);

        PreferenceScreen prefSet = getPreferenceScreen();

        mLordClockan = prefSet.findPreference("aicp_lordclockan");
        mZipsNet = prefSet.findPreference("aicp_zipsnet");
        mN3ocort3x = prefSet.findPreference("aicp_n3ocort3x");
        mSemdoc = prefSet.findPreference("aicp_semdoc");
        mEyosen = prefSet.findPreference("aicp_eyosen");
        mOblikas = prefSet.findPreference("aicp_oblikas");
        mTtoivainen = prefSet.findPreference("aicp_ttoivainen");

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;
        if (preference == mZipsNet) {
            Toast.makeText(getActivity(), "Likes: As long as they are hairy, fat and old!",
                    Toast.LENGTH_LONG).show();
        } else if (preference == mSemdoc) {
            Toast.makeText(getActivity(), "Likes: Everything you throw at him, it's his job to like 'em all!",
                    Toast.LENGTH_LONG).show();
        } else if (preference == mN3ocort3x) {
            Toast.makeText(getActivity(), "Likes: Red and tatooed!",
                    Toast.LENGTH_LONG).show();
        } else if (preference == mOblikas) {
            Toast.makeText(getActivity(), "Likes: Contrary to his father, long walks on the beach!",
                    Toast.LENGTH_LONG).show();
        } else if (preference == mLordClockan) {
            Toast.makeText(getActivity(), "Likes: Skinny with small buts!",
                    Toast.LENGTH_LONG).show();
        } else if (preference == mEyosen) {
            Toast.makeText(getActivity(), "Likes: Long limbs! He wishes!",
                    Toast.LENGTH_LONG).show();
        } else if (preference == mTtoivainen) {
            Toast.makeText(getActivity(), "Likes: Geek girls that are good in math!",
                    Toast.LENGTH_LONG).show();
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();

        return true;
    }
}

