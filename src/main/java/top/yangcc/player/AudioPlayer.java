package top.yangcc.player;

import javafx.application.Platform;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class AudioPlayer {

    private static final Logger LOG = System.getLogger("top.yangcc.player");

    private MediaPlayerFactory factory;
    private MediaPlayer mediaPlayer;
    private volatile boolean available;
    private String initError;
    private Runnable onStatusChanged;

    private static String platformDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").contains("64") ? "x86_64" : "x86";
        if (os.contains("linux")) return "linux-" + arch;
        if (os.contains("mac")) return "macos-" + arch;
        if (os.contains("win")) return "windows-" + arch;
        return null;
    }

    /** Resolve a candidate native directory, returning its absolute path if it exists. */
    private static String tryPath(Path dir) {
        if (dir != null && Files.isDirectory(dir)) {
            return dir.toAbsolutePath().toString();
        }
        return null;
    }

    private static String detectNativePath() {
        String platform = platformDir();
        if (platform == null) return null;

        // 1. Already set via jpackage --java-options or env
        if (System.getProperty("jna.library.path") != null) return null;

        // 2. CWD-relative (dev mode: mvn javafx:run, or jpackage Linux/Windows)
        String result = tryPath(Path.of("native", platform));
        if (result != null) return result;

        // 3. Relative to the jar location (jpackage macOS .app, or any jlink image)
        try {
            ProtectionDomain pd = AudioPlayer.class.getProtectionDomain();
            if (pd != null) {
                CodeSource cs = pd.getCodeSource();
                if (cs != null) {
                    URL loc = cs.getLocation();
                    if (loc != null) {
                        Path jarDir = Path.of(loc.toURI()).getParent();
                        result = tryPath(jarDir.resolveSibling("native").resolve(platform));
                        if (result != null) return result;
                    }
                }
            }
        } catch (Exception ignore) {
            // not available in all environments
        }

        return null;
    }

    public AudioPlayer() {
        LOG.log(Level.INFO, "Initializing VLCJ AudioPlayer...");
        try {
            String nativePath = detectNativePath();
            if (nativePath != null) {
                LOG.log(Level.INFO, "Bundled VLC native path: {0}", nativePath);
                System.setProperty("jna.library.path", nativePath);
            }
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
        available = false;
        if (mediaPlayer != null) mediaPlayer.release();
        if (factory != null) factory.release();
    }
}
