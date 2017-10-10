package com.rosel.roselsalesapp.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.rosel.roselsalesapp.R;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        //show prefs values in summary
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        setPreferenceValueAsSummary(prefs, getString(R.string.pref_server_address_key));
        setPreferenceValueAsSummary(prefs, getString(R.string.pref_server_port_key));
        setPreferenceValueAsSummary(prefs, getString(R.string.pref_server_timeout_key));
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    private void setPreferenceValueAsSummary(SharedPreferences preferences, String key){
        if(preferences.contains(key)){
            Preference serverPortPref = findPreference(key);
            serverPortPref.setSummary(preferences.getString(key,""));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        pref.setSummary(sharedPreferences.getString(key,""));
    }
}
