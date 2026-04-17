package com.example.tleilax.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.R;

public final class MusicManager implements AppSettings.Listener {

    private static final String TAG = "MusicManager";

    private final Context appContext;
    private final AudioManager audioManager;
    private final AudioFocusRequest focusRequest;

    private MediaPlayer player;
    private boolean prepared;
    private boolean wantsPlay;
    private boolean musicEnabled;
    private boolean hasAudioFocus;

    public MusicManager(@NonNull Context context) {
        musicEnabled = AppSettings.isMusicEnabled(context);
        AppSettings.addListener(this);

        appContext = context.getApplicationContext();
        audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(this::onAudioFocusChange)
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(true)
                .build();
    }

    public void start(@NonNull Context context) {
        if (!musicEnabled) {
            wantsPlay = false;
            return;
        }
        wantsPlay = true;
        if (!requestAudioFocus()) {
            Log.w(TAG, "Audio focus not granted; music will not start");
            return;
        }
        beginPlayback(context);
    }

    public void pause() {
        wantsPlay = false;
        if (player != null && player.isPlaying()) {
            player.pause();
        }
        abandonAudioFocus();
    }

    public void resume(@NonNull Context context) {
        if (musicEnabled) {
            start(context);
        }
    }

    public void release() {
        AppSettings.removeListener(this);
        wantsPlay = false;
        abandonAudioFocus();
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (IllegalStateException exception) {
                Log.e(TAG, "Error stopping MediaPlayer", exception);
            }
            player.release();
            player = null;
            prepared = false;
        }
    }

    @Override
    public void onMusicEnabledChanged(boolean enabled) {
        musicEnabled = enabled;
        if (!enabled) {
            if (player != null && player.isPlaying()) {
                player.pause();
            }
            wantsPlay = false;
            abandonAudioFocus();
        } else if (wantsPlay || player != null) {
            wantsPlay = true;
            if (requestAudioFocus()) {
                beginPlayback(null);
            }
        }
    }

    @Override
    public void onShowGridChanged(boolean visible) {
    }

    @Override
    public void onGrassCoverageChanged(int percent) {
    }

    private void beginPlayback(@Nullable Context context) {
        if (player == null) {
            Context ctx = context != null ? context : appContext;
            player = MediaPlayer.create(ctx, R.raw.sound);
            if (player == null) {
                Log.e(TAG, "Failed to create MediaPlayer for raw/sound.mp3");
                return;
            }
            player.setLooping(true);
            player.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra);
                prepared = false;
                return false;
            });
            prepared = true;
        }
        if (prepared && !player.isPlaying()) {
            player.start();
        }
    }

    private boolean requestAudioFocus() {
        if (hasAudioFocus) {
            return true;
        }
        int result = audioManager.requestAudioFocus(focusRequest);
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        return hasAudioFocus;
    }

    private void abandonAudioFocus() {
        if (!hasAudioFocus) {
            return;
        }
        audioManager.abandonAudioFocusRequest(focusRequest);
        hasAudioFocus = false;
    }

    private void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                hasAudioFocus = true;
                if (player != null) {
                    player.setVolume(1.0f, 1.0f);
                }
                if (musicEnabled && wantsPlay && player != null && prepared && !player.isPlaying()) {
                    player.start();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                hasAudioFocus = false;
                if (player != null && player.isPlaying()) {
                    player.pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (player != null) {
                    player.setVolume(0.2f, 0.2f);
                }
                break;

            default:
                break;
        }
    }
}
