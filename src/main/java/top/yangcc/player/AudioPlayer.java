package top.yangcc.player;

import javafx.application.Platform;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class AudioPlayer {

    private static final Logger LOG = System.getLogger("top.yangcc.player");

    private MediaPlayerFactory factory;
    private MediaPlayer mediaPlayer;
    private volatile boolean available;
    private String initError;
    private Runnable onStatusChanged;

    public AudioPlayer() {
        LOG.log(Level.INFO, "Initializing VLCJ AudioPlayer...");
        try {
            factory = new MediaPlayerFactory("--no-video");
            mediaPlayer = factory.mediaPlayers().newMediaPlayer();
            available = true;
            LOG.log(Level.INFO, "VLCJ initialized successfully");

            mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
                @Override
                public void playing(MediaPlayer mp) {
                    LOG.log(Level.INFO, "VLC event: playing");
                    notifyStatus();
                }

                @Override
                public void paused(MediaPlayer mp) {
                    LOG.log(Level.INFO, "VLC event: paused");
                    notifyStatus();
                }

                @Override
                public void stopped(MediaPlayer mp) {
                    LOG.log(Level.INFO, "VLC event: stopped");
                    notifyStatus();
                }

                @Override
                public void finished(MediaPlayer mp) {
                    LOG.log(Level.INFO, "VLC event: media finished");
                    notifyStatus();
                }

                @Override
                public void error(MediaPlayer mp) {
                    LOG.log(Level.ERROR, "VLC event: error occurred");
                    notifyStatus();
                }

                @Override
                public void timeChanged(MediaPlayer mp, long newTime) {
                    notifyStatus();
                }

                @Override
                public void lengthChanged(MediaPlayer mp, long newLength) {
                    LOG.log(Level.INFO, "VLC event: length changed to {0}ms", newLength);
                    notifyStatus();
                }
            });
        } catch (Error | Exception e) {
            available = false;
            initError = e.getMessage();
            LOG.log(Level.ERROR, "Failed to initialize VLCJ: " + e.getMessage(), e);
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public String getInitError() {
        return initError;
    }

    public void play(String url) {
        if (!available) {
            LOG.log(Level.WARNING, "play() called but VLC is not available");
            return;
        }
        LOG.log(Level.INFO, "Submitting play request: {0}", url);
        mediaPlayer.submit(() -> {
            LOG.log(Level.INFO, "VLC: starting media: {0}", url);
            mediaPlayer.media().start(url);
        });
    }

    public void resume() {
        if (!available) return;
        LOG.log(Level.INFO, "Resuming playback");
        mediaPlayer.submit(() -> mediaPlayer.controls().play());
    }

    public void pause() {
        if (!available) return;
        LOG.log(Level.INFO, "Pausing playback");
        mediaPlayer.submit(() -> mediaPlayer.controls().pause());
    }

    public void stop() {
        if (!available) return;
        LOG.log(Level.INFO, "Stopping playback");
        mediaPlayer.submit(() -> mediaPlayer.controls().stop());
    }

    public void setRate(float rate) {
        if (!available) return;
        mediaPlayer.submit(() -> mediaPlayer.controls().setRate(rate));
    }

    public void setVolume(int volume) {
        if (!available) return;
        mediaPlayer.submit(() -> mediaPlayer.audio().setVolume(volume));
    }

    public long getTime() {
        if (!available) return 0;
        return mediaPlayer.status().time();
    }

    public long getLength() {
        if (!available) return 0;
        return mediaPlayer.status().length();
    }

    public boolean isPlaying() {
        if (!available) return false;
        return mediaPlayer.status().isPlaying();
    }

    public int getVolume() {
        if (!available) return 100;
        return mediaPlayer.audio().volume();
    }

    public void seekTo(float position) {
        if (!available) return;
        mediaPlayer.submit(() -> mediaPlayer.controls().setPosition(position));
    }

    public void setOnStatusChanged(Runnable callback) {
        this.onStatusChanged = callback;
    }

    private void notifyStatus() {
        if (onStatusChanged != null) {
            Platform.runLater(onStatusChanged);
        }
    }

    public void release() {
        LOG.log(Level.INFO, "Releasing VLCJ resources");
        if (mediaPlayer != null) mediaPlayer.release();
        if (factory != null) factory.release();
    }
}
