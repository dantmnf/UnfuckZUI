package xyz.cirno.unfuckzui;

import android.preference.PreferenceDataStore;


import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class DummyPreferenceStore implements PreferenceDataStore {
    @Override
    public void putString(String key, @Nullable String value) {
    }

    @Override
    public void putStringSet(String key, @Nullable Set<String> values) {
    }

    @Override
    public void putInt(String key, int value) {
    }

    @Override
    public void putLong(String key, long value) {
    }

    @Override
    public void putFloat(String key, float value) {
    }

    @Override
    public void putBoolean(String key, boolean value) {
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return null;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return null;
    }

    @Override
    public int getInt(String key, int defValue) {
        return 0;
    }

    @Override
    public long getLong(String key, long defValue) {
        return 0;
    }

    @Override
    public float getFloat(String key, float defValue) {
        return 0.0f;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return false;
    }
}
