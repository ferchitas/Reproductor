package com.apps.ferchu.reproductor;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import java.io.IOException;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ReproductorFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ReproductorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReproductorFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ListView listView;
    private ArrayAdapter<String> adaptador;
    private ImageButton reproducir;
    private ImageButton siguiente;
    private ImageButton anterior;
    private Playlist playlist;
    TextView nombreCancion;

    MediaPlayer mediaPlayer;

    private OnFragmentInteractionListener mListener;

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
    // TODO: Rename and change types and number of parameters
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
    }

    private void inicializarIU(View rootView) {

        listView = (ListView) rootView.findViewById(R.id.lvCanciones);
        nombreCancion = rootView.findViewById(R.id.tsNombreCancion);
        reproducir = (ImageButton) rootView.findViewById(R.id.btReproducir);
        siguiente = (ImageButton) rootView.findViewById(R.id.btSiguiente);
        anterior = (ImageButton) rootView.findViewById(R.id.btAnterior);
        //Se mueve al servicio
        playlist = new Playlist(0);
    }

    public void hacerCosas(){

        listView.setOnItemClickListener(new ListaCancionesListener());
        reproducir.setOnClickListener(new BtRepoducir());
        siguiente.setOnClickListener(new BtSiguiente());
        anterior.setOnClickListener( new BtAnterior());
        mediaPlayer.setOnCompletionListener(new FinDeCancion());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_reproductor, container, false);
        inicializarIU(rootView);
        obtenerMusica();
        mostrarCancionesEnLista();
        crearMediaPlayer();
        hacerCosas();
        return rootView;
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

            do {
                String tituloActual = cancionCursor.getString(cancionTitulo);
                String artistaActual = cancionCursor.getString(cancionArtista);
                String rutaActual = cancionCursor.getString(cancionRuta);
                String duracionActual = cancionCursor.getString(cancionDuracion);

                Cancion cancion = new Cancion(tituloActual, artistaActual, duracionActual, rutaActual);
                playlist.getCanciones().add(cancion);

            } while (cancionCursor.moveToNext());
        }
    }

    private void crearMediaPlayer() {

        String ruta = playlist.obtenerRutasDeLasCanciones().get(playlist.getCancionActual());
        String tituloCancion = playlist.obtenerNombresDeLasCanciones().get(playlist.getCancionActual());
        mediaPlayer = MediaPlayer.create(getActivity(),  Uri.parse(ruta));
        nombreCancion.setText(tituloCancion);
    }

    private void mostrarCancionesEnLista() {

        adaptador = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, playlist.obtenerNombresYArtistasDeLasCanciones());
        listView.setAdapter(adaptador);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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

        /*
        private void playAudio(String media) {
            //Check is service is active
            if (!serviceBound) {
                Intent playerIntent = new Intent(this, PlayService.class);
                playerIntent.putExtra("media", media);
                playService.
                        startService(playerIntent);
            } else {

                Toast.makeText(ActividadInicial.this, "El servicio no esta disponible", Toast.LENGTH_SHORT).show();
            }
        }
*/
        class ListaCancionesListener implements AdapterView.OnItemClickListener {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                String cancion = (String) playlist.obtenerNombresYArtistasDeLasCanciones().get(i);
                String ruta = (String) playlist.obtenerRutasDeLasCanciones().get(i);

                if (mediaPlayer != null) {

                    mediaPlayer.release();
                }
                playlist.setCancionActual(i);
                mediaPlayer = MediaPlayer.create(getActivity(), Uri.parse(ruta));
                nombreCancion.setText(cancion);
                mediaPlayer.start();
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
        nombreCancion.setText(tituloCancion);
        mediaPlayer.start();
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
    }
}
