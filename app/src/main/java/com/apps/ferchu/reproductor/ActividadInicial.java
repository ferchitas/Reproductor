package com.apps.ferchu.reproductor;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class ActividadInicial extends AppCompatActivity implements
        PlayListsFragment.OnFragmentInteractionListener,
        ReproductorFragment.OnFragmentInteractionListener{

    //Strings para la comunicacion con los bradcastreceiver
    public static final String Broadcast_REPRDUCIR_NUEVO_AUDIO = "com.apps.ferchu.reproductor.PlayNewAudio";
    public static final String Broadcast_REANUDAR_PAUSAR_AUDIO = "com.apps.ferchu.reproductor.ReanudarPausarAudio";
    public static final String Broadcast_SIGUIENTE_AUDIO = "com.apps.ferchu.reproductor.SiguienteAudio";
    public static final String Broadcast_ANTERIOR_AUDIO = "com.apps.ferchu.reproductor.AnteriorAudio";

    //obtencion de permisos, permiso para leer en disco del dispositivo
    private static final int PETICION_DE_PERMISOS = 1;

    //variables para gestionar el servicio y si este se encuentra ligado
    private PlayService playService;
    boolean serviceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actividad_inicial);

        if(ContextCompat.checkSelfPermission(ActividadInicial.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(ActividadInicial.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PETICION_DE_PERMISOS);
        }
        else {

            setUpMenu();
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PlayService.LocalBinder binder = (PlayService.LocalBinder) service;
            playService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void setUpMenu(){

        TabLayout tabLayout = (TabLayout)findViewById(R.id.tablayout);
        tabLayout.addTab(tabLayout.newTab().setText("Playlists"));
        tabLayout.addTab(tabLayout.newTab().setText("Reproductor"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager)findViewById(R.id.pager);
        final AdaptadorPagina adapter = new AdaptadorPagina(getSupportFragmentManager(),tabLayout.getTabCount(), ActividadInicial.this);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) { viewPager.setCurrentItem(tab.getPosition());}

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode){

            case PETICION_DE_PERMISOS:{

                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    if(ContextCompat.checkSelfPermission(ActividadInicial.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){ }
                    else {
                        finish();}
                    return;
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            playService.stopSelf();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    private void enlaceAlServicio(){

        Intent playerIntent = new Intent(this, PlayService.class);
        startService(playerIntent);
        bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private boolean controlServicioEnlazado(Playlist playlist){

        boolean enlazado = false;
        AlmacenamientoHelper storage = new AlmacenamientoHelper(getApplicationContext());
        if (!serviceBound) {
            //guardamos en las sherePreferences los datos para tomarlos en el servicio
            //conectamos con el servicio para la proxima vez que tratemos de interactuar con el servicio
            storage.guardarPlaylist(playlist);
            storage.guardarIndicePlaylist(playlist.getIndiceCancionActual());
            enlaceAlServicio();

        } else {
            //guardamos el indice de la playlist en las SharedPreferences para que el servicio lo tome
            //se envia un broadcast al servicio para que haga la accion requerida
            storage.guardarIndicePlaylist(playlist.getIndiceCancionActual());
            enlazado = true;
        }
        return enlazado;
    }

    @Override
    public void reproducirAudio(Playlist playlist) {

        if (controlServicioEnlazado(playlist)) {
            //si la actividad esta enlazada con el servicio enviamos el broadcast
            Intent broadcastIntent = new Intent(Broadcast_REPRDUCIR_NUEVO_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    @Override
    public void audioAtras(Playlist playlist) {

        if (controlServicioEnlazado(playlist)) {
            //si la actividad esta enlazada con el servicio enviamos el broadcast
            Intent broadcastIntent = new Intent(Broadcast_ANTERIOR_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    @Override
    public void audioAdelante(Playlist playlist) {

        if (controlServicioEnlazado(playlist)) {
            //si la actividad esta enlazada con el servicio enviamos el broadcast
            Intent broadcastIntent = new Intent(Broadcast_SIGUIENTE_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    @Override
    public void reanudarPausarAudio(Playlist playlist) {

        if (controlServicioEnlazado(playlist)) {
            //si la actividad esta enlazada con el servicio enviamos el broadcast
            Intent broadcastIntent = new Intent(Broadcast_REANUDAR_PAUSAR_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }
}