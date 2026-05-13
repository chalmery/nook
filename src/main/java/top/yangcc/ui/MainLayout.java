package top.yangcc.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import top.yangcc.model.Episode;
import top.yangcc.model.Podcast;
import top.yangcc.player.AudioPlayer;
import top.yangcc.service.SubscriptionManager;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class MainLayout extends BorderPane {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.layout");

    private final SubscriptionManager subscriptionManager;
    private final AudioPlayer audioPlayer;

    private final SidebarView sidebar;
    private final SubscriptionGridView gridView;
    private final PodcastDetailView detailView;
    private final PlayerBar playerBar;

    private Podcast selectedPodcast;
    private Episode currentEpisode;
    private Timeline progressTimer;

    public MainLayout() {
        LOG.log(Level.INFO, "Initializing MainLayout");

        subscriptionManager = new SubscriptionManager();
        LOG.log(Level.INFO, "SubscriptionManager ready, {0} subscriptions loaded",
                subscriptionManager.getPodcasts().size());

        audioPlayer = new AudioPlayer();
        LOG.log(Level.INFO, "AudioPlayer initialized, available={0}", audioPlayer.isAvailable());

        sidebar = new SidebarView();
        gridView = new SubscriptionGridView(subscriptionManager.getPodcasts());
        detailView = new PodcastDetailView();
        playerBar = new PlayerBar();

        buildLayout();
        wireCallbacks();
        setupProgressTimer();

        LOG.log(Level.INFO, "MainLayout initialized complete");
    }

    private void buildLayout() {
        setLeft(sidebar);
        setCenter(gridView);
        setBottom(playerBar);
    }

    private void showGridView() {
        setCenter(gridView);
        gridView.refreshCards();
    }

    private void showPodcastDetail(Podcast podcast) {
        selectedPodcast = podcast;
        detailView.setPodcast(podcast);
        detailView.setPlayingEpisode(currentEpisode);
        setCenter(detailView);
    }

    private void wireCallbacks() {
        // --- sidebar navigation ---
        sidebar.setOnNavigate(target -> {
            switch (target) {
                case SUBSCRIPTIONS:
                    LOG.log(Level.INFO, "Navigate to subscriptions grid");
                    showGridView();
                    break;
                case SETTINGS:
                    LOG.log(Level.INFO, "Open settings");
                    SettingsDialog.show((Stage) getScene().getWindow());
                    break;
                default:
                    break;
            }
        });

        // --- grid ---
        gridView.setOnAddSubscription(() -> {
            LOG.log(Level.INFO, "Add subscription dialog from grid");
            AddSubscriptionDialog.show(subscriptionManager, () -> {
                gridView.refreshCards();
                showGridView();
            });
        });

        gridView.setOnPodcastSelected(podcast -> {
            LOG.log(Level.INFO, "Podcast selected from grid: {0}", podcast.getTitle());
            showPodcastDetail(podcast);
        });

        // --- podcast detail ---
        detailView.setOnBack(() -> {
            LOG.log(Level.INFO, "Back to grid");
            showGridView();
        });

        detailView.setOnEpisodeClicked(episode -> {
            LOG.log(Level.INFO, "Episode clicked → play: {0}", episode.getTitle());
            playEpisode(episode);
        });

        detailView.setOnPlayLatest(episode -> {
            LOG.log(Level.INFO, "Play latest episode: {0}", episode.getTitle());
            playEpisode(episode);
        });

        // --- player bar ---
        playerBar.setOnPlay(() -> audioPlayer.resume());
        playerBar.setOnPause(() -> audioPlayer.pause());
        playerBar.setOnSeek(pos -> audioPlayer.seekTo(pos));
        playerBar.setOnSpeedChange(rate -> audioPlayer.setRate(rate));
        playerBar.setOnVolumeChange(vol -> audioPlayer.setVolume(vol));

        audioPlayer.setOnStatusChanged(this::onPlayerStatusChanged);
    }

    private void playEpisode(Episode episode) {
        if (episode == null) return;
        if (episode.getAudioUrl() == null) {
            showErrorAlert("无法播放", "该单集没有可用的音频地址", "");
            return;
        }
        if (!audioPlayer.isAvailable()) {
            showErrorAlert("无法播放", "VLC 未安装或初始化失败",
                    "请安装 VLC：sudo dnf install vlc\n\n" +
                            (audioPlayer.getInitError() != null ? audioPlayer.getInitError() : ""));
            return;
        }

        currentEpisode = episode;
        audioPlayer.play(episode.getAudioUrl());
        detailView.setPlayingEpisode(episode);

        String podcastTitle = selectedPodcast != null ? selectedPodcast.getTitle() : "";
        String imageUrl = selectedPodcast != null ? selectedPodcast.getImageUrl() : null;
        playerBar.setNowPlaying(episode, podcastTitle, imageUrl);
        LOG.log(Level.INFO, "Playback initiated: {0} - {1}", podcastTitle, episode.getTitle());
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void onPlayerStatusChanged() {
        boolean playing = audioPlayer.isPlaying();
        playerBar.setPlayingState(playing);
        playerBar.updateProgress(audioPlayer.getTime(), audioPlayer.getLength());
        playerBar.setVolume(audioPlayer.getVolume());
    }

    private void setupProgressTimer() {
        progressTimer = new Timeline(
                new KeyFrame(Duration.millis(500), e -> {
                    if (!shuttingDown && audioPlayer.isAvailable() && audioPlayer.isPlaying()) {
                        playerBar.updateProgress(audioPlayer.getTime(), audioPlayer.getLength());
                    }
                })
        );
        progressTimer.setCycleCount(Timeline.INDEFINITE);
        progressTimer.play();
    }

    private volatile boolean shuttingDown;

    public void shutdown() {
        if (shuttingDown) return;
        shuttingDown = true;
        LOG.log(Level.INFO, "Shutting down");

        if (progressTimer != null) {
            progressTimer.stop();
        }
        audioPlayer.stop();
        audioPlayer.release();
    }
}
