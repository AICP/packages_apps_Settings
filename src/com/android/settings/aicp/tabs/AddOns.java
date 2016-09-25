package com.android.settings.aicp.tabs;

import android.os.Bundle;
import com.android.settings.SettingsPreferenceFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.aicp.util.Helpers;
import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;

public class AddOns extends SettingsPreferenceFragment implements Indexable {

    private static final String TAG = "AicpLabs";

    private static final String KEY_AICPOTA_START = "aicp_ota_start";
    private static final String KEY_KERNEL_AUDIUTOR_START = "kernel_adiutor_start";
    private static final String KEY_ADAWAY_START = "adaway_start";

    // Package name of the AICP OTA app
    public static final String AICPOTA_PACKAGE_NAME = "com.aicp.aicpota";
    // Intent for launching the AICP OTA main actvity
    public static Intent INTENT_AICPOTA = new Intent(Intent.ACTION_MAIN)
            .setClassName(AICPOTA_PACKAGE_NAME, AICPOTA_PACKAGE_NAME + ".MainActivity");

    // Package name of the Kernel Adiutor app
    public static final String KERNEL_AUDIUTOR_PACKAGE_NAME = "com.grarak.kerneladiutor";
    // Intent for launching the Kernel Adiutor main actvity
    public static Intent INTENT_KERNEL_AUDIUTOR = new Intent(Intent.ACTION_MAIN)
            .setClassName(KERNEL_AUDIUTOR_PACKAGE_NAME, KERNEL_AUDIUTOR_PACKAGE_NAME + ".MainActivity");

    // Package name of the AdAway app
    public static final String ADAWAY_PACKAGE_NAME = "org.adaway";
    // Intent for launching the AdAway main actvity
    public static Intent INTENT_ADAWAY = new Intent(Intent.ACTION_MAIN)
            .setClassName(ADAWAY_PACKAGE_NAME, ADAWAY_PACKAGE_NAME + ".ui.BaseActivity");

    private Preference mAicpOta;
    private Preference mKernelAdiutor;
    private Preference mAdAway;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.aicp_extras_addons);

        PreferenceScreen prefSet = getPreferenceScreen();
        PackageManager pm = getActivity().getPackageManager();

        mAicpOta = (Preference)
                prefSet.findPreference(KEY_AICPOTA_START);
        if (!Helpers.isPackageInstalled(AICPOTA_PACKAGE_NAME, pm)) {
            prefSet.removePreference(mAicpOta);
        }

        mKernelAdiutor = (Preference)
                prefSet.findPreference(KEY_KERNEL_AUDIUTOR_START);
        if (!Helpers.isPackageInstalled(KERNEL_AUDIUTOR_PACKAGE_NAME, pm)) {
            prefSet.removePreference(mKernelAdiutor);
        }

        mAdAway = (Preference)
                prefSet.findPreference(KEY_ADAWAY_START);
        if (!Helpers.isPackageInstalled(ADAWAY_PACKAGE_NAME, pm)) {
            prefSet.removePreference(mAdAway);
        }

    }

	@Override
	public boolean onPreferenceTreeClick(Preference preference) {
		if (preference == mAicpOta) {
		    startActivity(INTENT_AICPOTA);
		    return true;
		} else if (preference == mKernelAdiutor) {
		    startActivity(INTENT_KERNEL_AUDIUTOR);
		    return true;
		} else if (preference == mAdAway) {
		    startActivity(INTENT_ADAWAY);
		    return true;
		}
		return super.onPreferenceTreeClick(preference);
	}

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                    boolean enabled) {
            ArrayList<SearchIndexableResource> result =
                new ArrayList<SearchIndexableResource>();

            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.aicp_extras_addons;
            result.add(sir);

            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> result = new ArrayList<String>();
            return result;
        }
    };

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATION;
    }
}
