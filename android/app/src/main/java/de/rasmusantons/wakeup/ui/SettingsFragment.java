package de.rasmusantons.wakeup.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import de.rasmusantons.wakeup.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}
