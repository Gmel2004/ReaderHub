package com.example.readerhub.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREF_NAME = "ReaderHubPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_TOKEN = "userToken";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_EMAIL = "userEmail";

    // Reading settings
    private static final String KEY_FONT_SIZE = "fontSize";
    private static final String KEY_FONT_FAMILY = "fontFamily";
    private static final String KEY_BRIGHTNESS = "brightness";
    private static final String KEY_NIGHT_MODE = "nightMode";
    private static final String KEY_AUTO_SCROLL = "autoScroll";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // User session management
    public void saveUserSession(int userId, String token, String username, String email) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_TOKEN, token);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }

    public void clearUserSession() {
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_TOKEN);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_USER_EMAIL);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUserToken() {
        return prefs.getString(KEY_USER_TOKEN, null);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    // Reading settings
    public void setFontSize(int size) {
        editor.putInt(KEY_FONT_SIZE, size);
        editor.apply();
    }

    public int getFontSize() {
        return prefs.getInt(KEY_FONT_SIZE, 16);
    }

    public void setFontFamily(String fontFamily) {
        editor.putString(KEY_FONT_FAMILY, fontFamily);
        editor.apply();
    }

    public String getFontFamily() {
        return prefs.getString(KEY_FONT_FAMILY, "serif");
    }

    public void setBrightness(float brightness) {
        editor.putFloat(KEY_BRIGHTNESS, brightness);
        editor.apply();
    }

    public float getBrightness() {
        return prefs.getFloat(KEY_BRIGHTNESS, 0.5f);
    }

    public void setNightMode(boolean enabled) {
        editor.putBoolean(KEY_NIGHT_MODE, enabled);
        editor.apply();
    }

    public boolean isNightMode() {
        return prefs.getBoolean(KEY_NIGHT_MODE, false);
    }

    public void setAutoScroll(boolean enabled) {
        editor.putBoolean(KEY_AUTO_SCROLL, enabled);
        editor.apply();
    }

    public boolean isAutoScroll() {
        return prefs.getBoolean(KEY_AUTO_SCROLL, false);
    }
}