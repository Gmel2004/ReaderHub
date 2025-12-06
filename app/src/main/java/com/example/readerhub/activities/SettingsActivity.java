package com.example.readerhub.activities;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import com.example.readerhub.R;
import com.example.readerhub.utils.PreferencesManager;

public class SettingsActivity extends AppCompatActivity {

    private PreferencesManager prefsManager;
    private SeekBar fontSizeSeekBar, brightnessSeekBar;
    private Switch nightModeSwitch, autoScrollSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefsManager = new PreferencesManager(this);

        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
        nightModeSwitch = findViewById(R.id.nightModeSwitch);
        autoScrollSwitch = findViewById(R.id.autoScrollSwitch);

        loadSettings();
        setupListeners();
    }

    private void loadSettings() {
        fontSizeSeekBar.setProgress(prefsManager.getFontSize() - 10);
        brightnessSeekBar.setProgress((int)(prefsManager.getBrightness() * 100));
        nightModeSwitch.setChecked(prefsManager.isNightMode());
        autoScrollSwitch.setChecked(prefsManager.isAutoScroll());
    }

    private void setupListeners() {
        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefsManager.setFontSize(progress + 10);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefsManager.setBrightness(progress / 100f);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        nightModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefsManager.setNightMode(isChecked));

        autoScrollSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefsManager.setAutoScroll(isChecked));
    }
}