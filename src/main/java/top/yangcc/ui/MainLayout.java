package top.yangcc.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.SplitPane;
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
    private final EpisodeListView episodeList;
    private final DetailView detailView;
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
        episodeList = new EpisodeListView();
        detailView = new DetailView();
        playerBar = new PlayerBar();

        buildLayout();
        wireCallbacks();
        setupProgressTimer();

        sidebar.getSubscriptionList().setItems(subscriptionManager.getPodcasts());
        LOG.log(Level.INFO, "MainLayout initialized complete");
    }

    private void buildLayout() {
        SplitPane centerSplit = new SplitPane(episodeList, detailView);
        centerSplit.setDividerPositions(0.6);

        SplitPane mainSplit = new SplitPane(sidebar, centerSplit);
        mainSplit.setDividerPositions(0.22);
        SplitPane.setResizableWithParent(sidebar, false);

        setCenter(mainSplit);
        setBottom(playerBar);
    }

    private void wireCallbacks() {
        sidebar.setOnAddSubscription(() -> {
            LOG.log(Level.INFO, "Add subscription dialog requested");
            AddSubscriptionDialog.show(subscriptionManager, () -> {
                if (!subscriptionManager.getPodcasts().isEmpty()) {
                    Podcast last = subscriptionManager.getPodcasts()
                            .get(subscriptionManager.getPodcasts().size() - 1);
                    sidebar.selectPodcast(last);
                    LOG.log(Level.INFO, "New subscription added: {0}", last.getTitle());
                }
            });
        });

        sidebar.setOnOpenSettings(() -> {
            LOG.log(Level.INFO, "Open settings requested");
            SettingsDialog.show((Stage) getScene().getWindow());
        });

        sidebar.setOnPodcastSelected(podcast -> {
            selectedPodcast = podcast;
            LOG.log(Level.INFO, "Podcast selected: {0} ({1} episodes)",
                    podcast.getTitle(), podcast.getEpisodes().size());
            episodeList.setPodcast(podcast);
            episodeList.setPlayingEpisode(currentEpisode);
        });

        episodeList.setOnEpisodeSelected(episode -> {
            currentEpisode = episode;
            LOG.log(Level.INFO, "Episode selected: {0}", episode.getTitle());
            detailView.setEpisode(episode);
        });

        episodeList.setOnPlayRequested(episode -> {
            LOG.log(Level.INFO, "Play requested from episode list: {0}", episode.getTitle());
            playEpisode(episode);
        });

        detailView.setOnPlayRequested(() -> {
            if (currentEpisode != null) {
                LOG.log(Level.INFO, "Play requested from detail view: {0}", currentEpisode.getTitle());
                playEpisode(currentEpisode);
            } else {
                LOG.log(Level.WARNING, "Play button clicked but no episode selected");
            }
        });

        playerBar.setOnPlay(() -> {
            LOG.log(Level.INFO, "Player bar: play clicked");
            audioPlayer.resume();
        });
        playerBar.setOnPause(() -> {
            LOG.log(Level.INFO, "Player bar: pause clicked");
            audioPlayer.pause();
        });
        playerBar.setOnSeek(pos -> audioPlayer.seekTo(pos));
        playerBar.setOnSpeedChange(rate -> {
            LOG.log(Level.INFO, "Player bar: speed changed to {0}x", rate);
            audioPlayer.setRate(rate);
        });
        playerBar.setOnVolumeChange(vol -> {
            LOG.log(Level.INFO, "Player bar: volume changed to {0}", vol);
            audioPlayer.setVolume(vol);
        });

        audioPlayer.setOnStatusChanged(this::onPlayerStatusChanged);
    }

    private void playEpisode(Episode episode) {
        if (episode == null) {
            LOG.log(Level.WARNING, "playEpisode: episode is null, aborting");
            return;
        }
        if (episode.getAudioUrl() == null) {
            LOG.log(Level.WARNING, "playEpisode: audioUrl is null for episode: {0}", episode.getTitle());
            showErrorAlert("无法播放", "该单集没有可用的音频地址", "");
            return;
        }
        if (!audioPlayer.isAvailable()) {
            LOG.log(Level.WARNING, "playEpisode: VLC not available, error={0}", audioPlayer.getInitError());
            showErrorAlert("无法播放", "VLC 未安装或初始化失败",
                    "请安装 VLC：sudo dnf install vlc\n\n" +
                            (audioPlayer.getInitError() != null ? audioPlayer.getInitError() : ""));
            return;
        }

        currentEpisode = episode;
        LOG.log(Level.INFO, "Playing episode: {0}, url={1}", episode.getTitle(), episode.getAudioUrl());
        audioPlayer.play(episode.getAudioUrl());
        episodeList.setPlayingEpisode(episode);

        String podcastTitle = selectedPodcast != null ? selectedPodcast.getTitle() : "";
        playerBar.setNowPlaying(episode, podcastTitle);
        LOG.log(Level.INFO, "Playback initiated for: {0} - {1}", podcastTitle, episode.getTitle());
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
        LOG.log(Level.INFO, "Error alert shown: {0} - {1}", title, header);
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
