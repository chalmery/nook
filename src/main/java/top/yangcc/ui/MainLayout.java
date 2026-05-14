package top.yangcc.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import top.yangcc.model.Episode;
import top.yangcc.model.Podcast;
import top.yangcc.player.AudioPlayer;
import top.yangcc.service.ITunesSearchService;
import top.yangcc.service.SubscriptionManager;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class MainLayout extends BorderPane {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.layout");

    private final SubscriptionManager subscriptionManager;
    private final ITunesSearchService iTunesSearchService;
    private final AudioPlayer audioPlayer;

    private final SidebarView sidebar;
    private final SubscriptionGridView gridView;
    private final DiscoverView discoverView;
    private final DiscoverDetailView discoverDetailView;
    private final PodcastDetailView detailView;
    private final SettingsView settingsView;
    private final PlayerBar playerBar;

    private Podcast selectedPodcast;
    private Episode currentEpisode;
    private Timeline progressTimer;

    public MainLayout() {
        LOG.log(Level.INFO, "Initializing MainLayout");

        subscriptionManager = new SubscriptionManager();
        LOG.log(Level.INFO, "SubscriptionManager ready, {0} subscriptions loaded",
                subscriptionManager.getPodcasts().size());

        iTunesSearchService = new ITunesSearchService();

        audioPlayer = new AudioPlayer();
        LOG.log(Level.INFO, "AudioPlayer initialized, available={0}", audioPlayer.isAvailable());

        sidebar = new SidebarView();
        gridView = new SubscriptionGridView(subscriptionManager.getPodcasts());
        discoverView = new DiscoverView();
        discoverDetailView = new DiscoverDetailView();
        detailView = new PodcastDetailView();
        settingsView = new SettingsView();
        playerBar = new PlayerBar();

        buildLayout();
        wireCallbacks();
        setupProgressTimer();

        LOG.log(Level.INFO, "MainLayout initialized complete");
    }

    private void buildLayout() {
        setLeft(sidebar);
        setCenter(discoverView);
        setBottom(playerBar);
    }

    private void showDiscoverView() {
        setCenter(discoverView);
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
                case DISCOVER:
                    LOG.log(Level.INFO, "Navigate to discover view");
                    showDiscoverView();
                    break;
                case SUBSCRIPTIONS:
                    LOG.log(Level.INFO, "Navigate to subscriptions grid");
                    showGridView();
                    break;
                case SETTINGS:
                    LOG.log(Level.INFO, "Navigate to settings view");
                    setCenter(settingsView);
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

        // --- discover ---
        discoverView.setOnPodcastSelected(podcast -> {
            LOG.log(Level.INFO, "Discover podcast selected: {0}", podcast.getTitle());
            selectedPodcast = podcast;
            discoverDetailView.setPodcast(podcast);
            discoverDetailView.setOnSubscribe(p -> subscribeFromDiscover(p));
            setCenter(discoverDetailView);
        });

        discoverDetailView.setOnBack(() -> {
            LOG.log(Level.INFO, "Back to discover from detail");
            showDiscoverView();
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

    private void subscribeFromDiscover(Podcast podcast) {
        String feedUrl = podcast.getRssUrl();
        // trending podcasts store collectionId in link field, need to look up feedUrl
        if ((feedUrl == null || feedUrl.isBlank()) && podcast.getLink() != null) {
            feedUrl = iTunesSearchService.lookupFeedUrl(podcast.getLink());
        }
        if (feedUrl == null || feedUrl.isBlank()) {
            discoverDetailView.setSubscribeError("无法获取该播客的 RSS 地址");
            return;
        }

        try {
            Podcast subscribed = subscriptionManager.addSubscription(feedUrl);
            LOG.log(Level.INFO, "Subscribed to: {0}", subscribed.getTitle());
            discoverDetailView.setSubscribeSuccess();
            gridView.refreshCards();
        } catch (Exception ex) {
            LOG.log(Level.ERROR, "Failed to subscribe: " + ex.getMessage(), ex);
            discoverDetailView.setSubscribeError(ex.getMessage());
        }
    }

    private void playEpisode(Episode episode) {
        if (episode == null) return;
        if (episode.getAudioUrl() == null) {
            showErrorAlert("无法播放", "该单集没有可用的音频地址", "");
            return;
        }
        if (!audioPlayer.isAvailable()) {
            showErrorAlert("无法播放", "音频引擎初始化失败",
                    audioPlayer.getInitError() != null ? audioPlayer.getInitError() : "");
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
