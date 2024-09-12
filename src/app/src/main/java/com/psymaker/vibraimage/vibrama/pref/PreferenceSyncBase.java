package com.psymaker.vibraimage.vibrama.pref;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

public class PreferenceSyncBase implements Preference.OnPreferenceChangeListener {

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    public void initSummary(Preference p) {}

    public void startUpdate()  {}
    public void stopUpdate() {}
}
