package top.yangcc.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import top.yangcc.model.Episode;
import top.yangcc.model.Podcast;
import top.yangcc.util.HtmlUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

public class PodcastDetailView extends BorderPane {

    private static final int IMAGE_SIZE = 150;

    private final ImageView imageView;
    private final StackPane imagePane;
    private final Rectangle gradientRect;
    private final Label titleLabel;
    private final Label authorLabel;
    private final Label descLabel;
    private final VBox descSection;
    private final EpisodeListView episodeList;
    private final Button playLatestBtn;
    private final Label subscribedBadge;
    private final Button unsubBtn;
    private final Label episodeCountLabel;
    private final HBox genreChipRow;
    private final HBox statsRow;

    private Podcast podcast;
    private Runnable onBack;
    private Consumer<Episode> onPlayLatest;
    private Runnable onUnsubscribe;

    public PodcastDetailView() {
        // --- back button ---
        Button backBtn = new Button("←  订阅");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });

        // --- image ---
        imageView = new ImageView();
        imageView.setPreserveRatio(false);
        imageView.setVisible(false);

        gradientRect = new Rectangle(IMAGE_SIZE, IMAGE_SIZE);
        gradientRect.setVisible(false);

        imagePane = new StackPane(gradientRect, imageView);
        imagePane.setPrefSize(IMAGE_SIZE, IMAGE_SIZE);
        imagePane.setMinSize(IMAGE_SIZE, IMAGE_SIZE);
        imagePane.setMaxSize(IMAGE_SIZE, IMAGE_SIZE);
        imagePane.getStyleClass().add("podcast-detail-image-pane");
        Rectangle clip = new Rectangle(IMAGE_SIZE, IMAGE_SIZE);
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        imagePane.setClip(clip);

        // --- text info ---
        titleLabel = new Label();
        titleLabel.getStyleClass().add("podcast-detail-title");
        titleLabel.setWrapText(true);

        authorLabel = new Label();
        authorLabel.getStyleClass().add("podcast-detail-author");

        genreChipRow = new HBox(6);
        genreChipRow.setAlignment(Pos.CENTER_LEFT);

        statsRow = new HBox(10);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getStyleClass().add("podcast-detail-stats");

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

        unsubBtn = new Button("取消订阅");
        unsubBtn.getStyleClass().add("unsub-btn");
        unsubBtn.setOnAction(e -> {
            if (onUnsubscribe != null) onUnsubscribe.run();
        });

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);

        HBox actionRow = new HBox(12, playLatestBtn, subscribedBadge, actionSpacer, unsubBtn);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        // --- header assembly ---
        VBox infoBox = new VBox(4, titleLabel, authorLabel, genreChipRow, statsRow, actionRow);
        infoBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        HBox headerRow = new HBox(18, imagePane, infoBox);
        headerRow.setAlignment(Pos.TOP_LEFT);

        VBox headerBox = new VBox(8, backBtn, headerRow);
        headerBox.getStyleClass().add("podcast-detail-header");
        headerBox.setPadding(new Insets(16, 28, 14, 28));

        // --- description section ---
        Label descSectionLabel = new Label("简介");
        descSectionLabel.getStyleClass().add("podcast-detail-section-label");

        descLabel = new Label();
        descLabel.getStyleClass().add("podcast-detail-desc");
        descLabel.setWrapText(true);

        descSection = new VBox(4, descSectionLabel, descLabel);
        descSection.setPadding(new Insets(0, 28, 12, 28));
        descSection.setVisible(false);
        descSection.setManaged(false);
        descSection.getStyleClass().add("podcast-detail-desc-section");

        // --- episode count header ---
        episodeCountLabel = new Label();
        episodeCountLabel.getStyleClass().add("episode-count-header");
        episodeCountLabel.setPadding(new Insets(8, 28, 4, 28));

        // --- episode list (native ListView virtualization, no setShowAll) ---
        episodeList = new EpisodeListView();

        // --- assemble: top = header + desc + count, center = episode list ---
        VBox topBox = new VBox(headerBox, descSection, episodeCountLabel);

        setTop(topBox);
        setCenter(episodeList);
    }

    public void setPodcast(Podcast podcast) {
        this.podcast = podcast;
        titleLabel.setText(podcast.getTitle());
        authorLabel.setText(podcast.getAuthor() != null ? podcast.getAuthor() : "");

        // image
        Image img = loadImage(podcast.getImageUrl());
        if (img != null) {
            imageView.setImage(img);
            imageView.setFitWidth(IMAGE_SIZE);
            imageView.setFitHeight(IMAGE_SIZE);
            imageView.setVisible(true);
            gradientRect.setVisible(false);
        } else {
            imageView.setVisible(false);
            gradientRect.setFill(gradientFor(podcast.getTitle()));
            gradientRect.setVisible(true);
        }

        buildGenreChips();
        buildStats();
        buildDescription();

        episodeCountLabel.setText("全部剧集 (" + podcast.getEpisodes().size() + ")");
        episodeList.setPodcast(podcast);
    }

    /** Refresh UI after lookup enrichment completes. */
    public void refreshAfterEnrich() {
        if (podcast == null) return;
        buildGenreChips();
        buildStats();
        buildDescription();
    }

    private void buildGenreChips() {
        genreChipRow.getChildren().clear();
        List<String> genres = podcast.getGenres();
        if ((genres == null || genres.isEmpty())
                && podcast.getPrimaryGenre() != null && !podcast.getPrimaryGenre().isBlank()) {
            genres = List.of(podcast.getPrimaryGenre());
        }
        if (genres != null && !genres.isEmpty()) {
            for (String g : genres) {
                Label chip = new Label(g);
                chip.getStyleClass().add("podcast-detail-genre-chip");
                genreChipRow.getChildren().add(chip);
            }
        }
    }

    private void buildStats() {
        statsRow.getChildren().clear();
        if (podcast.getReleaseDate() != null && !podcast.getReleaseDate().isBlank()) {
            String formatted = formatDate(podcast.getReleaseDate());
            if (!formatted.isBlank()) {
                statsRow.getChildren().add(buildStatPill("最新 " + formatted));
            }
            String since = timeSince(podcast.getReleaseDate());
            if (since != null && !since.isBlank()) {
                Label sincePill = buildStatPill(since);
                sincePill.getStyleClass().add("podcast-detail-since");
                statsRow.getChildren().add(sincePill);
            }
        }
    }

    private Label buildStatPill(String text) {
        Label pill = new Label(text);
        pill.getStyleClass().add("podcast-detail-stat-pill");
        return pill;
    }

    private void buildDescription() {
        String desc = podcast.getDescription();
        if (desc != null && !desc.isBlank()) {
            String clean = HtmlUtils.clean(desc);
            descLabel.setText(clean);
            descLabel.setMaxHeight(65);
            Tooltip tt = new Tooltip(clean);
            tt.getStyleClass().add("desc-tooltip");
            tt.setMaxWidth(480);
            tt.setWrapText(true);
            descLabel.setTooltip(tt);
            descSection.setVisible(true);
            descSection.setManaged(true);
        } else {
            descSection.setVisible(false);
            descSection.setManaged(false);
        }
    }

    public void setPlayingEpisode(Episode episode) {
        episodeList.setPlayingEpisode(episode);
    }

    public void setOnEpisodeClicked(Consumer<Episode> handler) {
        episodeList.setOnEpisodeClicked(handler);
    }

    public void setOnPlayLatest(Consumer<Episode> handler) { this.onPlayLatest = handler; }
    public void setOnBack(Runnable handler) { this.onBack = handler; }
    public void setOnUnsubscribe(Runnable handler) { this.onUnsubscribe = handler; }

    private static Episode findLatestWithAudio(Podcast podcast) {
        if (podcast.getEpisodes() == null) return null;
        for (Episode ep : podcast.getEpisodes()) {
            if (ep.getAudioUrl() != null && !ep.getAudioUrl().isBlank()) {
                return ep;
            }
        }
        return null;
    }

    private static String formatDate(String releaseDate) {
        try {
            java.time.Instant instant = java.time.Instant.parse(releaseDate);
            ZonedDateTime dt = instant.atZone(java.time.ZoneId.systemDefault());
            return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return "";
        }
    }

    private static String timeSince(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) return null;
        try {
            java.time.Instant instant = java.time.Instant.parse(releaseDate);
            ZonedDateTime then = instant.atZone(java.time.ZoneId.systemDefault());
            ZonedDateTime now = ZonedDateTime.now();
            long days = ChronoUnit.DAYS.between(then, now);
            if (days < 60) return null;
            long months = ChronoUnit.MONTHS.between(then, now);
            if (months >= 12) {
                long years = ChronoUnit.YEARS.between(then, now);
                return years + "年未更新";
            }
            return months + "个月未更新";
        } catch (Exception e) {
            return null;
        }
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
