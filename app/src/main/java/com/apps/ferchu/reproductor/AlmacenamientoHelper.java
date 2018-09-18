package com.apps.ferchu.reproductor;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class AlmacenamientoHelper {

    private final String ALMACENAMIENTO = "com.apps.ferchu.reproductor.ALMACENAMIENTO";

    private SharedPreferences preferences;
    private Context context;

    public AlmacenamientoHelper(Context context) {
        this.context = context;
    }

    public void guardarPlaylist(Playlist lista) {
        preferences = context.getSharedPreferences(ALMACENAMIENTO, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(lista);
        editor.putString("audioArrayList", json);
        editor.apply();
    }

    public Playlist cargarPlaylist() {
        preferences = context.getSharedPreferences(ALMACENAMIENTO, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("audioArrayList", null);
        Type type = new TypeToken<Playlist>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void guardarIndicePlaylist(int index) {
        preferences = context.getSharedPreferences(ALMACENAMIENTO, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioIndex", index);
        editor.apply();
    }

    public int cargarIndicePlaylist() {
        preferences = context.getSharedPreferences(ALMACENAMIENTO, Context.MODE_PRIVATE);
        return preferences.getInt("audioIndex", -1);//return -1 if no data found
    }

    public void borrarPlaylistActual() {
        preferences = context.getSharedPreferences(ALMACENAMIENTO, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }
}
