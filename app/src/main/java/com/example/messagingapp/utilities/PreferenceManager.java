package com.example.messagingapp.utilities;

import android.content.Context;
import android.content.SharedPreferences;

//preference manager class, uses a shared preferences object to store user data
public class PreferenceManager {

    //shared preferences object used to store user data
    private final SharedPreferences sharedPreferences;

    //constructor, creates a shared preferences object using the app's context.
    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    //Allows for adding a boolean to the shared preferences object using a given key for the boolean in its hashmap/collection
    public void putBoolean(String key, Boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    //gets a boolean from the shared preferences object
    public Boolean getBoolean(String key) {
        return sharedPreferences.getBoolean(key, false);
    }


    //Allows for adding a string to the shared preferences object using a given key for the string in its hashmap/collection
    public void putString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }


    //gets a boolean from the shared preferences object
    public String getString(String key) {
        return sharedPreferences.getString(key, null);
    }

    //clears the shared preferences object, removing any user data
    public void clear() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
