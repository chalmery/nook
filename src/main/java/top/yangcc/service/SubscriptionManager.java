package top.yangcc.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import top.yangcc.model.Podcast;
import top.yangcc.rss.RssParser;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionManager {

    private static final Logger LOG = System.getLogger("top.yangcc.service");

    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), ".nook");
    private static final Path SUBSCRIPTIONS_FILE = DATA_DIR.resolve("subscriptions.json");

    private final ObservableList<Podcast> podcasts = FXCollections.observableArrayList();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public SubscriptionManager() {
        loadSubscriptions();
    }

    public ObservableList<Podcast> getPodcasts() {
        return podcasts;
    }

    public Podcast addSubscription(String rssUrl) throws Exception {
        LOG.log(Level.INFO, "Adding subscription: {0}", rssUrl);
        Podcast podcast = RssParser.parse(rssUrl);

        for (Podcast existing : podcasts) {
            if (existing.getRssUrl().equals(rssUrl)) {
                LOG.log(Level.INFO, "Already subscribed, refreshing: {0}", existing.getTitle());
                existing.setEpisodes(podcast.getEpisodes());
                existing.setTitle(podcast.getTitle());
                existing.setImageUrl(podcast.getImageUrl());
                saveSubscriptions();
                return existing;
            }
        }

        podcasts.add(podcast);
        saveSubscriptions();
        LOG.log(Level.INFO, "Subscription added: {0}, total: {1}", podcast.getTitle(), podcasts.size());
        return podcast;
    }

    public void removeSubscription(Podcast podcast) {
        LOG.log(Level.INFO, "Removing subscription: {0}", podcast.getTitle());
        podcasts.remove(podcast);
        saveSubscriptions();
    }

    public void refreshPodcast(Podcast podcast) throws Exception {
        LOG.log(Level.INFO, "Refreshing podcast: {0}", podcast.getTitle());
        Podcast refreshed = RssParser.parse(podcast.getRssUrl());
        podcast.setEpisodes(refreshed.getEpisodes());
        podcast.setTitle(refreshed.getTitle());
        podcast.setImageUrl(refreshed.getImageUrl());
    }

    private void saveSubscriptions() {
        try {
            Files.createDirectories(DATA_DIR);
            List<Podcast> list = new ArrayList<>(podcasts);
            try (FileWriter writer = new FileWriter(SUBSCRIPTIONS_FILE.toFile())) {
                gson.toJson(list, writer);
            }
            LOG.log(Level.INFO, "Subscriptions saved to {0}", SUBSCRIPTIONS_FILE);
        } catch (IOException e) {
            LOG.log(Level.ERROR, "Failed to save subscriptions: " + e.getMessage(), e);
        }
    }

    private void loadSubscriptions() {
        if (!Files.exists(SUBSCRIPTIONS_FILE)) {
            LOG.log(Level.INFO, "No saved subscriptions at {0}", SUBSCRIPTIONS_FILE);
            return;
        }
        try {
            List<Podcast> saved = gson.fromJson(
                    new FileReader(SUBSCRIPTIONS_FILE.toFile()),
                    new TypeToken<List<Podcast>>() {}.getType()
            );
            podcasts.addAll(saved);
            LOG.log(Level.INFO, "Loaded {0} subscriptions from {1}", saved.size(), SUBSCRIPTIONS_FILE);
        } catch (IOException e) {
            LOG.log(Level.ERROR, "Failed to load subscriptions: " + e.getMessage(), e);
        }
    }
}
