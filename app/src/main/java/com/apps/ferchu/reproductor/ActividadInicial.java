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
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ActividadInicial extends AppCompatActivity {

    private static final int PETICION_DE_PERMISOS = 1;

    List<String> canciones;
    List<String> rutasCanciones;

    ListView listView;

    ArrayAdapter<String> adaptador;

    MediaPlayer mediaPlayer;



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

            hacerCosas();
        }
    }

    public void hacerCosas(){

        listView = (ListView) findViewById(R.id.lvCanciones);

        canciones = new ArrayList<>();
        rutasCanciones = new ArrayList<>();

        obtenerMusica();
        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, canciones);

        listView.setAdapter(adaptador);

        final int[] cancionEnReproduccion = new int[1];

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {


            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if(mediaPlayer != null) {

                    mediaPlayer.release();
                }
                cancionEnReproduccion[0] = i;
                mediaPlayer = MediaPlayer.create(ActividadInicial.this,  Uri.parse(rutasCanciones.get(i)));
                mediaPlayer.start();
            }
        });

        Button reproducir = (Button) findViewById(R.id.btReproducir);

        reproducir.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if(mediaPlayer != null && mediaPlayer.isPlaying()) {

                    mediaPlayer.pause();
                }
                else {

                    mediaPlayer.start();
                }
            }
        });

        Button siguiente = (Button) findViewById(R.id.btSiguiente);

        siguiente.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if(mediaPlayer != null) {

                    mediaPlayer = MediaPlayer.create(ActividadInicial.this,  Uri.parse(rutasCanciones.get(cancionEnReproduccion[0] + 1)));
                    mediaPlayer.start();
                }
            }
        });
    }

    public void obtenerMusica(){

        ContentResolver contentResolver = getContentResolver();
        Uri cancionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cancionCursor = contentResolver.query(cancionUri, null, null, null, null);

        if(cancionCursor != null && cancionCursor.moveToFirst()) {

            int cancionTitulo = cancionCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int cancionArtista = cancionCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int cancionRuta = cancionCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            do {
                String tituloActual = cancionCursor.getString(cancionTitulo);
                String artistaActual = cancionCursor.getString(cancionArtista);
                String rutaActual = cancionCursor.getString(cancionRuta);

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
}