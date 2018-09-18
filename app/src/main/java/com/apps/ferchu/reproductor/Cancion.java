package com.apps.ferchu.reproductor;

/**
 * Created by ferch on 13/03/2018.
 */

public class Cancion {

    private String nombre;
    private String artista;
    private String duracion;
    private String ruta;
    private String album;

    public Cancion(String nombre, String artista, String duracion, String ruta, String album) {

        this.nombre = nombre;
        this.artista = artista;
        this.duracion = duracion;
        this.ruta = ruta;
        this.album = album;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getArtista() {
        return artista;
    }

    public void setArtista(String artista) {
        this.artista = artista;
    }

    public String getDuracion() {
        return duracion;
    }

    public void setDuracion(String duracion) {
        this.duracion = duracion;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }
}
