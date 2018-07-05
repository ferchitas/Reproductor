package com.apps.ferchu.reproductor;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class ActividadInicial extends AppCompatActivity {

    private static final int PETICION_DE_PERMISOS = 1;

    private ArrayList<String> canciones;
    private ArrayList<String> rutasCanciones;

    private Playlist playlist;

    private ListView listView;
    private ArrayAdapter<String> adaptador;
    private ImageButton reproducir;
    private ImageButton siguiente;
    private ImageButton anterior;
    TextView nombreCancion;

    MediaPlayer mediaPlayer;

    private void inicializar() {

        listView = (ListView) findViewById(R.id.lvCanciones);
        canciones = new ArrayList<>();
        rutasCanciones = new ArrayList<>();
        nombreCancion = findViewById(R.id.tsNombreCancion);
        reproducir = (ImageButton) findViewById(R.id.btReproducir);
        siguiente = (ImageButton) findViewById(R.id.btSiguiente);
        anterior = (ImageButton) findViewById(R.id.btAnterior);
        playlist = new Playlist(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actividad_inicial);

        if(ContextCompat.checkSelfPermission(ActividadInicial.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

            if(ActivityCompat.shouldShowRequestPermissionRationale(ActividadInicial.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(ActividadInicial.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PETICION_DE_PERMISOS);
            }
            else {

                ActivityCompat.requestPermissions(ActividadInicial.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PETICION_DE_PERMISOS);
            }
        }
        else {

            inicializar();
            obtenerMusica();
            mostrarCancionesEnLista();
            crearMediaPlayer();
            hacerCosas();
        }
    }

    private void crearMediaPlayer() {

        String ruta = playlist.obtenerRutasDeLasCanciones().get(playlist.getCancionActual());
        String tituloCancion = playlist.obtenerNombresDeLasCanciones().get(playlist.getCancionActual());
        mediaPlayer = MediaPlayer.create(ActividadInicial.this,  Uri.parse(ruta));
        nombreCancion.setText(tituloCancion);
    }

    public void hacerCosas(){

        listView.setOnItemClickListener(new ListaCancionesListener());
        reproducir.setOnClickListener(new BtRepoducir());
        siguiente.setOnClickListener(new BtSiguiente());
        anterior.setOnClickListener( new BtAnterior());
        mediaPlayer.setOnCompletionListener(new FinDeCancion());
    }

    private void mostrarCancionesEnLista() {
        
        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, playlist.obtenerNombresYArtistasDeLasCanciones());
        listView.setAdapter(adaptador);
    }

    public void obtenerMusica(){

        ContentResolver contentResolver = getContentResolver();
        Uri cancionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cancionCursor = contentResolver.query(cancionUri, null, null, null, "RANDOM()");

        if(cancionCursor != null && cancionCursor.moveToFirst()) {

            int cancionTitulo = cancionCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int cancionArtista = cancionCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int cancionRuta = cancionCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int cancionDuracion = cancionCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

            do {
                String tituloActual = cancionCursor.getString(cancionTitulo);
                String artistaActual = cancionCursor.getString(cancionArtista);
                String rutaActual = cancionCursor.getString(cancionRuta);
                String duracionActual = cancionCursor.getString(cancionDuracion);

                Cancion cancion = new Cancion(tituloActual, artistaActual, duracionActual, rutaActual);
                playlist.getCanciones().add(cancion);

                canciones.add(tituloActual + "\n" + artistaActual);
                rutasCanciones.add(rutaActual);
            } while (cancionCursor.moveToNext());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode){

            case PETICION_DE_PERMISOS:{

                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    if(ContextCompat.checkSelfPermission(ActividadInicial.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){

                        Toast.makeText(this, "Permiso obtenido", Toast.LENGTH_SHORT).show();
                        hacerCosas();
                    }
                    else {
                        Toast.makeText(this, "Permiso no obtenido", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    return;
                }
            }
        }
    }

    private void checkInicioArray() {

        if(playlist.getCancionActual() >= playlist.getCanciones().size() - 1) {
            playlist.setCancionActual(0);
        }
        else {
            playlist.setCancionActual(playlist.getCancionActual() + 1);
        }
    }

    private void pasarDeCancion(MediaPlayer mediaPlayer) {
        String ruta = playlist.obtenerRutasDeLasCanciones().get(playlist.getCancionActual());
        String tituloCancion = playlist.obtenerNombresYArtistasDeLasCanciones().get(playlist.getCancionActual());
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(ruta);
            mediaPlayer.prepare();

        } catch (IOException e) {
            e.printStackTrace();
        }
        //mediaPlayer = MediaPlayer.create(ActividadInicial.this,  Uri.parse(ruta));
        nombreCancion.setText(tituloCancion);
        mediaPlayer.start();
    }

    class FinDeCancion implements MediaPlayer.OnCompletionListener {


        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {

            if(mediaPlayer != null) {

                checkInicioArray();
                pasarDeCancion(mediaPlayer);
            }
        }
    }

    class BtAnterior extends ImagenBoton {

        @Override
        public void onClick(View v) {

            if(mediaPlayer != null) {

                checkFinalArray();
                pasarDeCancion(mediaPlayer);
            }
        }

        private void checkFinalArray() {
            if(playlist.getCancionActual() - 1 < 0) {
                playlist.setCancionActual(playlist.getCanciones().size() - 1);
            }
            else {
                playlist.setCancionActual(playlist.getCancionActual() - 1);
            }
        }
    }

    class BtSiguiente extends ImagenBoton {

        @Override
        public void onClick(View view) {

            if(mediaPlayer != null) {

                checkInicioArray();
                pasarDeCancion(mediaPlayer);
            }
        }
    }

    class BtRepoducir extends ImagenBoton {

        @Override
        public void onClick(View view) {

            if(mediaPlayer != null && mediaPlayer.isPlaying()) {

                mediaPlayer.pause();
            }
            else {

                mediaPlayer.start();
            }
        }
    }

    class ListaCancionesListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            String cancion = (String) playlist.obtenerNombresYArtistasDeLasCanciones().get(i);
            String ruta = (String) playlist.obtenerRutasDeLasCanciones().get(i);

            if (mediaPlayer != null) {

                mediaPlayer.release();
            }
            playlist.setCancionActual(i);
            mediaPlayer = MediaPlayer.create(ActividadInicial.this, Uri.parse(ruta));
            nombreCancion.setText(cancion);
            mediaPlayer.start();
        }
    }
}