package com.android.settings.aicp;

import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.Helpers;

import net.margaritov.preference.colorpicker.ColorPickerPreference;
import net.margaritov.preference.colorpicker.ColorPickerView;

public class TileColors extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "TileColors";

    private static final String PREF_TILE_BACKGROUND_STYLE = "tile_background_style";
    private static final String PREF_TILE_BACKGROUND_COLOR = "tile_background_color";
    private static final String PREF_TILE_BACKGROUND_PRESSED_COLOR = "tile_background_pressed_color";
    private static final String PREF_TILE_TEXT_COLOR = "tile_text_color";
    private static final String PREF_RANDOM_COLORS = "random_colors";


    ListPreference mTileBgStyle;
    ColorPickerPreference mTileBgColor;
    ColorPickerPreference mTileBgPresColor;
    ColorPickerPreference mTileTextColor;
    Preference mRandomColors;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ContentResolver resolver = getActivity().getContentResolver();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.aicp_color_tiles);


        mRandomColors = (Preference) findPreference(PREF_RANDOM_COLORS);

        mTileBgStyle = (ListPreference) findPreference(PREF_TILE_BACKGROUND_STYLE);
        mTileBgStyle.setOnPreferenceChangeListener(this);

        mTileBgColor = (ColorPickerPreference) findPreference(PREF_TILE_BACKGROUND_COLOR);
        mTileBgColor.setOnPreferenceChangeListener(this);

        mTileBgPresColor = (ColorPickerPreference) findPreference(PREF_TILE_BACKGROUND_PRESSED_COLOR);
        mTileBgPresColor.setOnPreferenceChangeListener(this);

        mTileTextColor = (ColorPickerPreference) findPreference(PREF_TILE_TEXT_COLOR);
        mTileTextColor.setOnPreferenceChangeListener(this);

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
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTileBgColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_SETTINGS_BACKGROUND_COLOR, intHex);
            return true;
        } else if (preference == mTileBgPresColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_SETTINGS_BACKGROUND_PRESSED_COLOR, intHex);
            return true;
        } else if (preference == mTileTextColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_SETTINGS_TEXT_COLOR, intHex);
            Helpers.restartSystemUI();
            return true;
        } else if (preference == mTileBgStyle) {
            int val = Integer.valueOf((String) newValue);
            int index = mTileBgStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_SETTINGS_BACKGROUND_STYLE, val);
            updateVisibility();
            return true;
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mRandomColors) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            RandomColors fragment = new RandomColors();
            ft.addToBackStack("pick_random_colors");
            ft.replace(this.getId(), fragment);
            ft.commit();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateVisibility() {
        int visible = Settings.System.getInt(getContentResolver(),
                    Settings.System.QUICK_SETTINGS_BACKGROUND_STYLE, 2);
        if (visible == 2) {
            mRandomColors.setEnabled(false);
            mTileBgColor.setEnabled(false);
            mTileBgPresColor.setEnabled(false);
        } else if (visible == 1) {
            mRandomColors.setEnabled(false);
            mTileBgColor.setEnabled(true);
            mTileBgPresColor.setEnabled(true);
        } else {
            mRandomColors.setEnabled(true);
            mTileBgColor.setEnabled(false);
            mTileBgPresColor.setEnabled(true);
        }
    }
}

