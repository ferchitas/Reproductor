package com.apps.ferchu.reproductor;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;


public class ReproductorFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    //Lista de canciones
    private ListView listView;
    private ArrayAdapter<String> adaptador;

    //botones texto con el nombre de la cancion
    private ImageButton reproducir;
    private ImageButton siguiente;
    private ImageButton anterior;
    TextView nombreCancion;

    //Lista de reproduccion que esta cargada en ese momento
    private Playlist playlist;

    OnFragmentInteractionListener mListener;

    public ReproductorFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReproductorFragment.
     */
    public static ReproductorFragment newInstance(String param1, String param2) {
        ReproductorFragment fragment = new ReproductorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        playlist = new Playlist(0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_reproductor, container, false);

        //cargamos los ids de los elementos de la interfaz
        inicializarIU(rootView);
        obtenerMusica();
        mostrarCancionesEnLista();
        configuarListeners();
        return rootView;
    }

    private void inicializarIU(View rootView) {

        listView = (ListView) rootView.findViewById(R.id.lvCanciones);
        nombreCancion = rootView.findViewById(R.id.tsNombreCancion);
        reproducir = (ImageButton) rootView.findViewById(R.id.btReproducir);
        siguiente = (ImageButton) rootView.findViewById(R.id.btSiguiente);
        anterior = (ImageButton) rootView.findViewById(R.id.btAnterior);
    }

    public void obtenerMusica(){

        ContentResolver contentResolver = getActivity().getContentResolver();
        Uri cancionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cancionCursor = contentResolver.query(cancionUri, null, null, null, "RANDOM()");

        if(cancionCursor != null && cancionCursor.moveToFirst()) {

            int cancionTitulo = cancionCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int cancionArtista = cancionCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int cancionRuta = cancionCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int cancionDuracion = cancionCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int cancionAlbum = cancionCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);

            do {
                String tituloActual = cancionCursor.getString(cancionTitulo);
                String artistaActual = cancionCursor.getString(cancionArtista);
                String rutaActual = cancionCursor.getString(cancionRuta);
                String duracionActual = cancionCursor.getString(cancionDuracion);
                String albumActual = cancionCursor.getString(cancionAlbum);

                Cancion cancion = new Cancion(tituloActual, artistaActual, duracionActual, rutaActual, albumActual);
                playlist.getCanciones().add(cancion);

            } while (cancionCursor.moveToNext());
        }
    }

    private void mostrarCancionesEnLista() {

        adaptador = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, playlist.obtenerNombresYArtistasDeLasCanciones());
        listView.setAdapter(adaptador);
    }

    public void configuarListeners(){

        listView.setOnItemClickListener(new ListaCancionesListener());
        reproducir.setOnClickListener(new BtRepoducir());
        siguiente.setOnClickListener(new BtSiguiente());
        anterior.setOnClickListener(new BtAnterior());
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

        class BtAnterior extends ImagenBoton {

            @Override
            public void onClick(View v) {

                mListener.audioAtras(playlist);
                nombreCancion.setText(playlist.obtenerCancionActual().getNombre());
            }
        }

        class BtSiguiente extends ImagenBoton {

            @Override
            public void onClick(View view) {

                nombreCancion.setText(playlist.obtenerCancionActual().getNombre());
                mListener.audioAdelante(playlist);
            }
        }

        class BtRepoducir extends ImagenBoton {

            @Override
            public void onClick(View view) {

                nombreCancion.setText(playlist.obtenerCancionActual().getNombre());
                mListener.reanudarPausarAudio(playlist);
            }
        }

        class ListaCancionesListener implements AdapterView.OnItemClickListener {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                playlist.setIndiceCancionActual(i);
                nombreCancion.setText(playlist.obtenerCancionActual().getNombre());
                mListener.reproducirAudio(playlist);

            }
        }

    private void checkInicioArray() {

        if(playlist.getIndiceCancionActual() >= playlist.getCanciones().size() - 1) {
            playlist.setIndiceCancionActual(0);
        }
        else {
            playlist.setIndiceCancionActual(playlist.getIndiceCancionActual() + 1);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);

        //enviar la cancion que se debe de reproducir a la actividad
        void reproducirAudio(Playlist playlist);
        void audioAtras(Playlist playlist);
        void audioAdelante(Playlist playlist);
        void reanudarPausarAudio(Playlist playlist);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
