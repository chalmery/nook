package top.yangcc.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import top.yangcc.model.Episode;
import top.yangcc.model.Podcast;

import java.util.function.Consumer;

public class PodcastDetailView extends BorderPane {

    private final ImageView imageView;
    private final StackPane imagePane;
    private final Rectangle gradientRect;
    private final Label titleLabel;
    private final Label descLabel;
    private final EpisodeListView episodeList;
    private final Button playLatestBtn;
    private final Label subscribedBadge;
    private final Label episodeCountLabel;

    private Podcast podcast;
    private Runnable onBack;
    private Consumer<Episode> onPlayLatest;

    public PodcastDetailView() {
        // --- back button ---
        Button backBtn = new Button("←  订阅");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });
        HBox backRow = new HBox(backBtn);
        backRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(backRow, new Insets(0, 0, 12, 0));

        // --- image ---
        imageView = new ImageView();
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(false);
        imageView.setVisible(false);

        gradientRect = new Rectangle(80, 80);
        gradientRect.setVisible(false);

        imagePane = new StackPane(gradientRect, imageView);
        imagePane.setPrefSize(80, 80);
        imagePane.setMinSize(80, 80);
        imagePane.setMaxSize(80, 80);
        Rectangle clip = new Rectangle(80, 80);
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        imagePane.setClip(clip);

        // --- title + description ---
        titleLabel = new Label();
        titleLabel.getStyleClass().add("podcast-detail-title");
        titleLabel.setWrapText(true);

        descLabel = new Label();
        descLabel.getStyleClass().add("podcast-detail-desc");
        descLabel.setWrapText(true);
        descLabel.setTextAlignment(TextAlignment.LEFT);
        descLabel.setMaxHeight(48);

        // --- action buttons ---
        playLatestBtn = new Button("▶  播放最新一集");
        playLatestBtn.getStyleClass().addAll("accent");
        playLatestBtn.setOnAction(e -> {
            if (podcast != null && onPlayLatest != null) {
                Episode latest = findLatestWithAudio(podcast);
                if (latest != null) onPlayLatest.accept(latest);
            }
        });

        subscribedBadge = new Label("✓ 已订阅");
        subscribedBadge.getStyleClass().add("subscribed-badge");

        HBox actions = new HBox(12, playLatestBtn, subscribedBadge);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox infoBox = new VBox(6, titleLabel, descLabel, actions);
        infoBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        HBox headerRow = new HBox(16, imagePane, infoBox);
        headerRow.setAlignment(Pos.TOP_LEFT);

        VBox header = new VBox();
        header.getStyleClass().add("podcast-detail-header");
        header.getChildren().addAll(backRow, headerRow);
        header.setPadding(new Insets(16, 24, 12, 24));

        // --- episode list ---
        episodeList = new EpisodeListView();

        // --- episode count header ---
        episodeCountLabel = new Label();
        episodeCountLabel.getStyleClass().add("episode-count-header");
        episodeCountLabel.setPadding(new Insets(12, 24, 8, 24));
        VBox centerBox = new VBox(episodeCountLabel, episodeList);
        VBox.setVgrow(episodeList, Priority.ALWAYS);

        setTop(header);
        setCenter(centerBox);
    }

    public void setPodcast(Podcast podcast) {
        this.podcast = podcast;
        titleLabel.setText(podcast.getTitle());

        String desc = podcast.getDescription();
        if (desc != null && !desc.isBlank()) {
            descLabel.setText(desc.length() > 200 ? desc.substring(0, 200) + "..." : desc);
            descLabel.setVisible(true);
            descLabel.setManaged(true);
        } else {
            descLabel.setVisible(false);
            descLabel.setManaged(false);
        }

        Image img = loadImage(podcast.getImageUrl());
        if (img != null) {
            imageView.setImage(img);
            imageView.setVisible(true);
            gradientRect.setVisible(false);
        } else {
            imageView.setVisible(false);
            gradientRect.setFill(gradientFor(podcast.getTitle()));
            gradientRect.setVisible(true);
        }

        episodeCountLabel.setText("全部剧集 (" + podcast.getEpisodes().size() + ")");
        episodeList.setPodcast(podcast);
    }

    public void setPlayingEpisode(Episode episode) {
        episodeList.setPlayingEpisode(episode);
    }

    public void setOnEpisodeClicked(Consumer<Episode> handler) {
        episodeList.setOnEpisodeClicked(handler);
    }

    public void setOnPlayLatest(Consumer<Episode> handler) { this.onPlayLatest = handler; }
    public void setOnBack(Runnable handler) { this.onBack = handler; }

    private static Episode findLatestWithAudio(Podcast podcast) {
        if (podcast.getEpisodes() == null) return null;
        for (Episode ep : podcast.getEpisodes()) {
            if (ep.getAudioUrl() != null && !ep.getAudioUrl().isBlank()) {
                return ep;
            }
        }
        return null;
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
