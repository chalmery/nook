package top.yangcc.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import top.yangcc.model.Episode;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Consumer;

public class PlayerBar extends VBox {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.playerbar");

    private final Slider progressSlider;
    private final Label nowPlayingLabel;
    private final Label timeLabel;
    private final Button playPauseBtn;
    private final ComboBox<String> speedCombo;
    private final Slider volumeSlider;

    private boolean isPlaying;
    private Runnable onPlay;
    private Runnable onPause;
    private Consumer<Float> onSeek;
    private Consumer<Float> onSpeedChange;
    private Consumer<Integer> onVolumeChange;

    public PlayerBar() {
        setPadding(new Insets(6, 12, 6, 12));
        getStyleClass().add("player-bar");

        progressSlider = new Slider(0, 1, 0);
        progressSlider.getStyleClass().add("progress-slider");
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        progressSlider.setOnMouseReleased(e -> {
            if (onSeek != null) {
                float pos = (float) progressSlider.getValue();
                LOG.log(Level.INFO, "Progress seek to: {0}", pos);
                onSeek.accept(pos);
            }
        });

        HBox controlRow = new HBox(8);
        controlRow.setAlignment(Pos.CENTER_LEFT);

        playPauseBtn = new Button("▶");
        playPauseBtn.getStyleClass().add("control-btn");
        playPauseBtn.setOnAction(e -> {
            if (isPlaying) {
                LOG.log(Level.INFO, "Pause button clicked");
                if (onPause != null) onPause.run();
            } else {
                LOG.log(Level.INFO, "Play button clicked");
                if (onPlay != null) onPlay.run();
            }
        });

        Button skipBackBtn = new Button("⏮");
        skipBackBtn.getStyleClass().add("control-btn");

        Button skipFwdBtn = new Button("⏭");
        skipFwdBtn.getStyleClass().add("control-btn");

        speedCombo = new ComboBox<>();
        speedCombo.getItems().addAll("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x");
        speedCombo.setValue("1.0x");
        speedCombo.setPrefWidth(80);
        speedCombo.setOnAction(e -> {
            String val = speedCombo.getValue();
            if (val != null && onSpeedChange != null) {
                onSpeedChange.accept(Float.parseFloat(val.replace("x", "")));
            }
        });

        Label volLabel = new Label("🔊");
        volumeSlider = new Slider(0, 150, 100);
        volumeSlider.setPrefWidth(100);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (onVolumeChange != null) {
                onVolumeChange.accept(newVal.intValue());
            }
        });

        timeLabel = new Label("--:--");
        timeLabel.getStyleClass().add("time-label");

        nowPlayingLabel = new Label("未在播放");
        nowPlayingLabel.getStyleClass().add("now-playing-label");
        nowPlayingLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nowPlayingLabel, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        controlRow.getChildren().addAll(
                skipBackBtn, playPauseBtn, skipFwdBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                speedCombo,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                volLabel, volumeSlider,
                spacer,
                timeLabel
        );

        getChildren().addAll(progressSlider, nowPlayingLabel, controlRow);
    }

    public void setNowPlaying(Episode episode, String podcastTitle) {
        if (episode == null) {
            nowPlayingLabel.setText("未在播放");
        } else {
            nowPlayingLabel.setText("正在播放：" + podcastTitle + " · " + episode.getTitle());
            LOG.log(Level.INFO, "Now playing: {0} - {1}", podcastTitle, episode.getTitle());
        }
    }

    public void updateProgress(long time, long length) {
        if (length > 0) {
            progressSlider.setValue((double) time / length);
        }
        timeLabel.setText(formatTime(time));
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

    private static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s);
        }
        return String.format("%d:%02d", m, s);
    }
}
