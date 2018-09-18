package com.apps.ferchu.reproductor;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;

public class PlayService extends Service implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    //variables de las comunicaciones con el bradcastReceiver
    public static final String ACCION_REPRODUCIR = "com.apps.ferchu.reproductor.ACCION_REPRODUCIR";
    public static final String ACCION_PAUSAR = "com.apps.ferchu.reproductor.ACCION_PAUSAR";
    public static final String ACCION_ANTERIOR = "com.apps.ferchu.reproductor.ACCION_ANTERIOR";
    public static final String ACCION_SIGUIENTE = "com.apps.ferchu.reproductor.ACCION_SIGUIENTE";
    public static final String ACCION_PARAR = "com.apps.ferchu.reproductor.ACCION_PARAR";

    private final IBinder iBinder = new LocalBinder();

    //musica
    private MediaPlayer mediaPlayer;
    private int posicionReanudar;
    private AudioManager audioManager;
    private Playlist playlist;

    //Gestion de las llamadas
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;

    public PlayService() {
    }

    @Override
    public void onCreate(){

        callStateListener();
        register_playNewAudio();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        AlmacenamientoHelper storage = new AlmacenamientoHelper(getApplicationContext());
        playlist = storage.cargarPlaylist();

        if (playlist.getIndiceCancionActual() != -1 && playlist.getIndiceCancionActual() < playlist.getCanciones().size()) {
            //index is in a valid range
        } else {
            stopSelf();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);

        if(mediaPlayer == null) initMediaPlayer();

        return super.onStartCommand(intent, flags, startId);
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

    private void checkInicioArray() {

        if(playlist.getIndiceCancionActual() >= playlist.getCanciones().size() - 1) {
            playlist.setIndiceCancionActual(0);
        }
        else {
            playlist.setIndiceCancionActual(playlist.getIndiceCancionActual() + 1);
        }
    }

    private void checkFinalArray() {
        if(playlist.getIndiceCancionActual() - 1 < 0) {
            playlist.setIndiceCancionActual(playlist.getCanciones().size() - 1);
        }
        else {
            playlist.setIndiceCancionActual(playlist.getIndiceCancionActual() - 1);
        }
    }

    private void pasarDeCancion(MediaPlayer mediaPlayer) {

        //Update stored index
        int i = playlist.getIndiceCancionActual();
        new AlmacenamientoHelper(getApplicationContext()).guardarIndicePlaylist(playlist.getIndiceCancionActual());

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void initMediaPlayer(){

        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnCompletionListener(new FinDeCancion());
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(playlist.obtenerCancionActual().getRuta());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    @Override
    public IBinder onBind(Intent intent) {

        return iBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        //removeNotification();

        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        unregisterReceiver(reanudarPausar);
        unregisterReceiver(siguienteAudioBR);
        unregisterReceiver(anteriorAudioBR);

        //clear cached playlist
        new AlmacenamientoHelper(getApplicationContext()).borrarPlaylistActual();
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    @Override
    public void onAudioFocusChange(int focusState) {

        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

        stopMedia();
        stopSelf();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {

        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    public class LocalBinder extends Binder {
        public PlayService getService() {
            return PlayService.this;
        }
    }

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Get the new media index form SharedPreferences
            playlist.setIndiceCancionActual(new AlmacenamientoHelper(getApplicationContext()).cargarIndicePlaylist());
            if (playlist.getIndiceCancionActual() != -1 && playlist.getIndiceCancionActual() < playlist.getCanciones().size()) {
                //index is in a valid range
            } else {
                stopSelf();
            }
            pasarDeCancion(mediaPlayer);
            //updateMetaData();
            //buildNotification(com.apps.ferchu.reproductor.PlaybackStatus.PLAYING);
        }
    };

    private BroadcastReceiver reanudarPausar = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            playPauseMedia();
            //updateMetaData();
            //buildNotification(com.apps.ferchu.reproductor.PlaybackStatus.PLAYING);
        }
    };

    private BroadcastReceiver anteriorAudioBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            checkFinalArray();
            pasarDeCancion(mediaPlayer);
            //updateMetaData();
            //buildNotification(com.apps.ferchu.reproductor.PlaybackStatus.PLAYING);
        }
    };

    private BroadcastReceiver siguienteAudioBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(mediaPlayer != null) {

                checkInicioArray();
                pasarDeCancion(mediaPlayer);
            }
            //updateMetaData();
            //buildNotification(com.apps.ferchu.reproductor.PlaybackStatus.PLAYING);
        }
    };

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            //buildNotification(com.apps.ferchu.reproductor.PlaybackStatus.PAUSED);
        }
    };

    private void register_playNewAudio() {
        //Registramos el broadcast para empezar a reproducir un nuevo audio
        IntentFilter nuevoAudio = new IntentFilter(ActividadInicial.Broadcast_REPRDUCIR_NUEVO_AUDIO);
        registerReceiver(playNewAudio, nuevoAudio);

        IntentFilter reanudarPausarAudio = new IntentFilter(ActividadInicial.Broadcast_REANUDAR_PAUSAR_AUDIO);
        registerReceiver(reanudarPausar, reanudarPausarAudio);

        IntentFilter anteriorAudio = new IntentFilter(ActividadInicial.Broadcast_ANTERIOR_AUDIO);
        registerReceiver(anteriorAudioBR, anteriorAudio);

        IntentFilter posteriorAudio = new IntentFilter(ActividadInicial.Broadcast_SIGUIENTE_AUDIO);
        registerReceiver(siguienteAudioBR, posteriorAudio);

        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    private void playPauseMedia() {
        boolean a = mediaPlayer.isPlaying();
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(posicionReanudar);
            mediaPlayer.start();
        }
        else {
            mediaPlayer.pause();
            posicionReanudar = mediaPlayer.getCurrentPosition();
        }
    }

    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            posicionReanudar = mediaPlayer.getCurrentPosition();
        }
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(posicionReanudar);
            mediaPlayer.start();
        }
    }

    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mediaSession's MetaData
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                AlmacenamientoHelper storage = new AlmacenamientoHelper(getApplicationContext());
                playlist.setIndiceCancionActual(storage.cargarIndicePlaylist());
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                AlmacenamientoHelper storage = new AlmacenamientoHelper(getApplicationContext());
                playlist.setIndiceCancionActual(storage.cargarIndicePlaylist());
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                AlmacenamientoHelper storage = new AlmacenamientoHelper(getApplicationContext());
                playlist.setIndiceCancionActual(storage.cargarIndicePlaylist());
                checkInicioArray();
                pasarDeCancion(mediaPlayer);
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                AlmacenamientoHelper storage = new AlmacenamientoHelper(getApplicationContext());
                playlist.setIndiceCancionActual(storage.cargarIndicePlaylist());
                checkFinalArray();
                pasarDeCancion(mediaPlayer);
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private void updateMetaData() {
        //Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.image); //replace with medias albumArt
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                //.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, playlist.obtenerCancionActual().getArtista())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, playlist.obtenerCancionActual().getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, playlist.obtenerCancionActual().getNombre())
                .build());
    }

    private void buildNotification(PlaybackStatus playbackStatus) {

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        //Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.image); //replace with your own image

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.colorPrimary))
                // Set the large and small icons
                //.setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information
                .setContentText(playlist.obtenerCancionActual().getArtista())
                .setContentTitle(playlist.obtenerCancionActual().getAlbum())
                .setContentInfo(playlist.obtenerCancionActual().getNombre())
                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, PlayService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACCION_REPRODUCIR);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACCION_PAUSAR);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACCION_SIGUIENTE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACCION_ANTERIOR);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACCION_REPRODUCIR)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACCION_PAUSAR)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACCION_SIGUIENTE)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACCION_ANTERIOR)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACCION_PARAR)) {
            transportControls.stop();
        }
    }

}
