package top.yangcc.ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import top.yangcc.model.Podcast;
import top.yangcc.util.HtmlUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

public class DiscoverDetailView extends BorderPane {

    private final ImageView imageView;
    private final StackPane imagePane;
    private final Rectangle gradientPlaceholder;
    private final Label titleLabel;
    private final Label authorLabel;
    private final Label descLabel;
    private final Label descSectionLabel;
    private final Button subscribeBtn;
    private final Label statusLabel;
    private final HBox statsRow;
    private final HBox genreChipRow;
    private final VBox detailFields;
    private final VBox descSection;
    private final VBox detailSection;
    private final Label detailSectionLabel;
    private final Region loadingOverlay;
    private final Label loadingLabel;

    private Podcast podcast;
    private Runnable onBack;
    private Consumer<Podcast> onSubscribe;

    public DiscoverDetailView() {
        getStyleClass().add("discover-detail");

        // --- back button ---
        Button backBtn = new Button("←  发现");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });
        HBox backRow = new HBox(backBtn);
        backRow.setAlignment(Pos.CENTER_LEFT);

        // --- image ---
        imageView = new ImageView();
        imageView.setPreserveRatio(false);
        imageView.setVisible(false);

        gradientPlaceholder = new Rectangle(200, 200);
        gradientPlaceholder.setVisible(false);

        imagePane = new StackPane(gradientPlaceholder, imageView);
        imagePane.setPrefSize(200, 200);
        imagePane.setMinSize(200, 200);
        imagePane.setMaxSize(200, 200);
        imagePane.getStyleClass().add("discover-detail-image-pane");
        Rectangle clip = new Rectangle(200, 200);
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        imagePane.setClip(clip);

        // --- text info ---
        titleLabel = new Label();
        titleLabel.getStyleClass().add("discover-detail-title");
        titleLabel.setWrapText(true);

        authorLabel = new Label();
        authorLabel.getStyleClass().add("discover-detail-author");

        // genre chips row
        genreChipRow = new HBox(6);
        genreChipRow.setAlignment(Pos.CENTER_LEFT);

        // stats row — dot-separated pills
        statsRow = new HBox(10);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getStyleClass().add("discover-detail-stats");

        // --- subscribe button ---
        subscribeBtn = new Button("+  订阅");
        subscribeBtn.getStyleClass().addAll("accent", "discover-detail-subscribe-btn");
        subscribeBtn.setOnAction(e -> {
            if (podcast != null && onSubscribe != null) {
                subscribeBtn.setDisable(true);
                subscribeBtn.setText("订阅中...");
                onSubscribe.accept(podcast);
            }
        });

        statusLabel = new Label();
        statusLabel.getStyleClass().add("discover-detail-status");

        HBox actionRow = new HBox(12, subscribeBtn, statusLabel);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        // info column — right side of header
        VBox infoBox = new VBox(8,
                titleLabel,
                authorLabel,
                genreChipRow,
                statsRow,
                actionRow);
        infoBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // header row
        HBox headerRow = new HBox(24, imagePane, infoBox);
        headerRow.setAlignment(Pos.TOP_LEFT);

        VBox headerBox = new VBox(8, backRow, headerRow);
        headerBox.setPadding(new Insets(24, 28, 20, 28));
        headerBox.getStyleClass().add("discover-detail-header");

        // --- description section ---
        descSectionLabel = new Label("简介");
        descSectionLabel.getStyleClass().add("discover-detail-section-label");

        descLabel = new Label();
        descLabel.getStyleClass().add("discover-detail-desc");
        descLabel.setWrapText(true);

        descSection = new VBox(8, descSectionLabel, descLabel);
        descSection.setPadding(new Insets(20, 28, 20, 28));
        descSection.getStyleClass().add("discover-detail-section");
        descSection.setVisible(false);
        descSection.setManaged(false);

        // --- detail fields section ---
        detailSectionLabel = new Label("详细信息");
        detailSectionLabel.getStyleClass().add("discover-detail-section-label");

        detailFields = new VBox(6);
        detailFields.getStyleClass().add("discover-detail-fields");

        detailSection = new VBox(8, detailSectionLabel, detailFields);
        detailSection.setPadding(new Insets(20, 28, 20, 28));
        detailSection.getStyleClass().add("discover-detail-section");
        detailSection.setVisible(false);
        detailSection.setManaged(false);

        // --- assemble scrollable content ---
        VBox content = new VBox(headerBox, descSection, detailSection);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("discover-detail-scroll");

        // --- loading overlay ---
        loadingLabel = new Label("正在加载详细信息...");
        loadingLabel.getStyleClass().add("discover-detail-loading");

        loadingOverlay = new StackPane(loadingLabel);
        loadingOverlay.getStyleClass().add("discover-detail-loading-overlay");
        loadingOverlay.setVisible(false);
        loadingOverlay.setMouseTransparent(false);
        loadingOverlay.setPickOnBounds(true);

        StackPane centerStack = new StackPane(scrollPane, loadingOverlay);
        StackPane.setAlignment(loadingOverlay, Pos.CENTER);
        setCenter(centerStack);
    }

    public void setPodcast(Podcast podcast) {
        this.podcast = podcast;
        titleLabel.setText(podcast.getTitle());
        authorLabel.setText(podcast.getAuthor() != null ? podcast.getAuthor() : "");

        // image
        Image img = loadImage(podcast.getImageUrl());
        if (img != null) {
            imageView.setImage(img);
            imageView.setFitWidth(200);
            imageView.setFitHeight(200);
            imageView.setVisible(true);
            gradientPlaceholder.setVisible(false);
        } else {
            imageView.setVisible(false);
            gradientPlaceholder.setFill(gradientFor(podcast.getTitle()));
            gradientPlaceholder.setVisible(true);
        }

        // genre chips
        buildGenreChips();

        // stats
        buildStats();

        // description
        buildDescription();

        // detail fields
        buildDetailFields();

        // subscribe button state
        subscribeBtn.setDisable(false);
        subscribeBtn.setText("+  订阅");
        statusLabel.setText("");
    }

    /** Show a loading state while enrichment is in progress. */
    public void setLoading(boolean loading) {
        loadingOverlay.setVisible(loading);
        if (loading) {
            FadeTransition ft = new FadeTransition(Duration.millis(200), loadingOverlay);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    /** Refresh UI after podcast data has been enriched by lookup. */
    public void refreshAfterEnrich() {
        if (podcast == null) return;
        buildGenreChips();
        buildStats();
        buildDescription();
        buildDetailFields();
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
                chip.getStyleClass().add("discover-detail-genre-chip");
                genreChipRow.getChildren().add(chip);
            }
        }
    }

    private void buildStats() {
        statsRow.getChildren().clear();

        if (podcast.getTrackCount() > 0) {
            statsRow.getChildren().add(buildStatPill(podcast.getTrackCount() + " 集"));
        }

        if (podcast.getReleaseDate() != null && !podcast.getReleaseDate().isBlank()) {
            String formatted = formatDate(podcast.getReleaseDate());
            if (!formatted.isBlank()) {
                statsRow.getChildren().add(buildStatPill("最新 " + formatted));
            }
            String since = timeSince(podcast.getReleaseDate());
            if (since != null && !since.isBlank()) {
                Label sincePill = buildStatPill(since);
                sincePill.getStyleClass().add("discover-detail-since");
                statsRow.getChildren().add(sincePill);
            }
        }
    }

    private Label buildStatPill(String text) {
        Label pill = new Label(text);
        pill.getStyleClass().add("discover-detail-stat-pill");
        return pill;
    }

    private void buildDescription() {
        String desc = podcast.getDescription();
        if (desc != null && !desc.isBlank()) {
            descLabel.setText(HtmlUtils.clean(desc));
            descSection.setVisible(true);
            descSection.setManaged(true);
        } else {
            descSection.setVisible(false);
            descSection.setManaged(false);
        }
    }

    private void buildDetailFields() {
        detailFields.getChildren().clear();

        if (podcast.getPrimaryGenre() != null && !podcast.getPrimaryGenre().isBlank()) {
            detailFields.getChildren().add(buildFieldRow("主分类", podcast.getPrimaryGenre()));
        }

        if (podcast.getReleaseDate() != null && !podcast.getReleaseDate().isBlank()) {
            String formatted = formatDate(podcast.getReleaseDate());
            if (!formatted.isBlank()) {
                detailFields.getChildren().add(buildFieldRow("最新发布", formatted));
            }
        }

        if (podcast.getTrackCount() > 0) {
            detailFields.getChildren().add(buildFieldRow("单集总数", podcast.getTrackCount() + " 集"));
        }

        if (podcast.getCopyright() != null && !podcast.getCopyright().isBlank()) {
            detailFields.getChildren().add(buildFieldRow("版权", podcast.getCopyright()));
        }

        if (podcast.getLanguage() != null && !podcast.getLanguage().isBlank()) {
            detailFields.getChildren().add(buildFieldRow("语言", podcast.getLanguage()));
        }

        if (podcast.getRssUrl() != null && !podcast.getRssUrl().isBlank()) {
            String rss = podcast.getRssUrl();
            if (rss.length() > 60) rss = rss.substring(0, 57) + "...";
            detailFields.getChildren().add(buildFieldRow("RSS", rss));
        }

        if (detailFields.getChildren().isEmpty()) {
            detailSection.setVisible(false);
            detailSection.setManaged(false);
        } else {
            detailSection.setVisible(true);
            detailSection.setManaged(true);
        }
    }

    private HBox buildFieldRow(String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("discover-detail-field-key");
        keyLabel.setMinWidth(70);
        keyLabel.setMaxWidth(70);

        Label valLabel = new Label(value);
        valLabel.getStyleClass().add("discover-detail-field-value");
        valLabel.setWrapText(true);
        HBox.setHgrow(valLabel, Priority.ALWAYS);

        HBox row = new HBox(12, keyLabel, valLabel);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    public void setSubscribeSuccess() {
        subscribeBtn.setVisible(false);
        subscribeBtn.setManaged(false);
        statusLabel.setText("✓ 已订阅，可在订阅列表中查看");
        statusLabel.getStyleClass().add("discover-detail-success");
    }

    public void setSubscribeError(String msg) {
        subscribeBtn.setDisable(false);
        subscribeBtn.setText("+  订阅");
        statusLabel.setText("订阅失败: " + (msg != null ? msg : "未知错误"));
        statusLabel.getStyleClass().add("discover-detail-error");
    }

    public void setOnSubscribe(Consumer<Podcast> handler) { this.onSubscribe = handler; }
    public void setOnBack(Runnable handler) { this.onBack = handler; }

    // --- helpers ---

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

    private static LinearGradient gradientFor(String seed) {
        int hash = seed != null ? seed.hashCode() : 0;
        double hue1 = ((hash & 0xFF) * 1.4) % 360;
        double hue2 = (hue1 + 40) % 360;
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.hsb(hue1, 0.5, 0.85)),
                new Stop(1, Color.hsb(hue2, 0.6, 0.7)));
    }
}
