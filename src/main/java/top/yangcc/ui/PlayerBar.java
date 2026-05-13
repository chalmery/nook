package top.yangcc.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import top.yangcc.model.Episode;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Consumer;

public class PlayerBar extends HBox {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.playerbar");

    // left
    private final ImageView coverImage;
    private final Rectangle coverGradient;
    private final StackPane coverPane;
    private final Label episodeTitleLabel;
    private final Label podcastNameLabel;

    // center
    private final Button playPauseBtn;
    private final Slider progressSlider;
    private final Label currentTimeLabel;
    private final Label totalTimeLabel;

    // right
    private final Button speedBtn;
    private final Slider volumeSlider;
    private final String[] speeds = {"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x"};
    private int speedIndex = 2; // 1.0x

    private boolean isPlaying;
    private Runnable onPlay;
    private Runnable onPause;
    private Consumer<Float> onSeek;
    private Consumer<Float> onSpeedChange;
    private Consumer<Integer> onVolumeChange;

    private long totalLength;

    public PlayerBar() {
        setPadding(new Insets(8, 16, 8, 16));
        setAlignment(Pos.CENTER);
        getStyleClass().add("player-bar");

        // ========== LEFT: cover + episode info ==========
        coverImage = new ImageView();
        coverImage.setFitWidth(48);
        coverImage.setFitHeight(48);
        coverImage.setPreserveRatio(false);
        coverImage.setVisible(false);

        coverGradient = new Rectangle(48, 48);
        coverGradient.setVisible(false);

        coverPane = new StackPane(coverGradient, coverImage);
        coverPane.setPrefSize(48, 48);
        coverPane.setMinSize(48, 48);
        coverPane.setMaxSize(48, 48);
        coverPane.setVisible(false);
        coverPane.setManaged(false);
        Rectangle clip = new Rectangle(48, 48);
        clip.setArcWidth(6);
        clip.setArcHeight(6);
        coverPane.setClip(clip);

        episodeTitleLabel = new Label();
        episodeTitleLabel.getStyleClass().add("player-episode-title");
        episodeTitleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(episodeTitleLabel, Priority.ALWAYS);

        podcastNameLabel = new Label("");
        podcastNameLabel.getStyleClass().add("player-podcast-name");

        VBox textBox = new VBox(2, episodeTitleLabel, podcastNameLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox leftBox = new HBox(12, coverPane, textBox);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        leftBox.setPrefWidth(280);
        leftBox.setMinWidth(200);

        // ========== CENTER: controls + progress ==========
        playPauseBtn = new Button("▶");
        playPauseBtn.getStyleClass().add("player-play-btn");
        playPauseBtn.setOnAction(e -> {
            if (isPlaying) {
                if (onPause != null) onPause.run();
            } else {
                if (onPlay != null) onPlay.run();
            }
        });

        Button skipBackBtn = new Button("⏪");
        skipBackBtn.getStyleClass().add("player-ctrl-btn");

        Button skipFwdBtn = new Button("⏩");
        skipFwdBtn.getStyleClass().add("player-ctrl-btn");

        HBox controlBtns = new HBox(12, skipBackBtn, playPauseBtn, skipFwdBtn);
        controlBtns.setAlignment(Pos.CENTER);

        currentTimeLabel = new Label("--:--");
        currentTimeLabel.getStyleClass().add("player-time");

        progressSlider = new Slider(0, 1, 0);
        progressSlider.getStyleClass().add("player-progress");
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        progressSlider.setOnMouseReleased(e -> {
            if (onSeek != null) {
                onSeek.accept((float) progressSlider.getValue());
            }
        });

        totalTimeLabel = new Label("--:--");
        totalTimeLabel.getStyleClass().add("player-time");

        HBox progressRow = new HBox(8, currentTimeLabel, progressSlider, totalTimeLabel);
        progressRow.setAlignment(Pos.CENTER);
        progressRow.setMaxWidth(500);

        VBox centerBox = new VBox(4, controlBtns, progressRow);
        centerBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(centerBox, Priority.ALWAYS);

        // ========== RIGHT: speed + volume ==========
        speedBtn = new Button("1.0x");
        speedBtn.getStyleClass().add("player-speed-btn");
        speedBtn.setOnAction(e -> {
            speedIndex = (speedIndex + 1) % speeds.length;
            String val = speeds[speedIndex];
            speedBtn.setText(val);
            if (onSpeedChange != null) {
                onSpeedChange.accept(Float.parseFloat(val.replace("x", "")));
            }
        });

        Label volIcon = new Label("🔊");
        volIcon.getStyleClass().add("player-vol-icon");

        volumeSlider = new Slider(0, 150, 100);
        volumeSlider.setPrefWidth(80);
        volumeSlider.getStyleClass().add("player-vol-slider");
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (onVolumeChange != null) {
                onVolumeChange.accept(newVal.intValue());
            }
        });

        HBox rightBox = new HBox(8, speedBtn, volIcon, volumeSlider);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setPrefWidth(240);
        rightBox.setMinWidth(180);

        getChildren().addAll(leftBox, centerBox, rightBox);
    }

    // --- public API ---

    public void setNowPlaying(Episode episode, String podcastTitle, String imageUrl) {
        if (episode == null) {
            episodeTitleLabel.setText("");
            podcastNameLabel.setText("");
            coverPane.setVisible(false);
            coverPane.setManaged(false);
            totalLength = 0;
            progressSlider.setValue(0);
            currentTimeLabel.setText("--:--");
            totalTimeLabel.setText("--:--");
        } else {
            episodeTitleLabel.setText(episode.getTitle());
            podcastNameLabel.setText(podcastTitle != null ? podcastTitle : "");
            setCoverImage(imageUrl, podcastTitle);
            LOG.log(Level.INFO, "Player bar: {0} - {1}", podcastTitle, episode.getTitle());
        }
    }

    private void setCoverImage(String imageUrl, String fallbackSeed) {
        Image img = loadImage(imageUrl);
        if (img != null) {
            coverImage.setImage(img);
            coverImage.setVisible(true);
            coverGradient.setVisible(false);
            coverPane.setVisible(true);
            coverPane.setManaged(true);
        } else if (fallbackSeed != null && !fallbackSeed.isEmpty()) {
            coverImage.setVisible(false);
            coverGradient.setFill(gradientFor(fallbackSeed));
            coverGradient.setVisible(true);
            coverPane.setVisible(true);
            coverPane.setManaged(true);
        } else {
            coverPane.setVisible(false);
            coverPane.setManaged(false);
        }
    }

    public void updateProgress(long time, long length) {
        this.totalLength = length;
        if (length > 0) {
            progressSlider.setValue((double) time / length);
        }
        currentTimeLabel.setText(formatTime(time));
        totalTimeLabel.setText(formatTime(length));
    }

    public void setPlayingState(boolean playing) {
        this.isPlaying = playing;
        playPauseBtn.setText(playing ? "⏸" : "▶");
    }

    public void setVolume(int volume) {
        volumeSlider.setValue(volume);
    }

    public void setOnPlay(Runnable handler) { this.onPlay = handler; }
    public void setOnPause(Runnable handler) { this.onPause = handler; }
    public void setOnSeek(Consumer<Float> handler) { this.onSeek = handler; }
    public void setOnSpeedChange(Consumer<Float> handler) { this.onSpeedChange = handler; }
    public void setOnVolumeChange(Consumer<Integer> handler) { this.onVolumeChange = handler; }

    // --- helpers ---

    private static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%d:%02d", m, s);
    }

    private static Image loadImage(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            return new Image(url, true);
        } catch (Exception e) {
            return null;
        }
    }

    static LinearGradient gradientFor(String seed) {
        int hash = seed != null ? seed.hashCode() : 0;
        double hue1 = ((hash & 0xFF) * 1.4) % 360;
        double hue2 = (hue1 + 40) % 360;
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.hsb(hue1, 0.5, 0.85)),
                new Stop(1, Color.hsb(hue2, 0.6, 0.7)));
    }
}
