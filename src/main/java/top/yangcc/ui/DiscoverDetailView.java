package top.yangcc.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import top.yangcc.model.Podcast;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

public class DiscoverDetailView extends BorderPane {

    private final ImageView imageView;
    private final StackPane imagePane;
    private final Label titleLabel;
    private final Label authorLabel;
    private final Label descLabel;
    private final Button subscribeBtn;
    private final Label statusLabel;
    private final VBox metaBox;

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

        imagePane = new StackPane(imageView);
        imagePane.setPrefSize(180, 180);
        imagePane.setMinSize(180, 180);
        imagePane.setMaxSize(180, 180);
        imagePane.getStyleClass().add("discover-detail-image-pane");
        Rectangle clip = new Rectangle(180, 180);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        imagePane.setClip(clip);

        // --- text info ---
        titleLabel = new Label();
        titleLabel.getStyleClass().add("discover-detail-title");
        titleLabel.setWrapText(true);

        authorLabel = new Label();
        authorLabel.getStyleClass().add("discover-detail-author");

        metaBox = new VBox(4);
        metaBox.getStyleClass().add("discover-detail-meta");

        descLabel = new Label();
        descLabel.getStyleClass().add("discover-detail-desc");
        descLabel.setWrapText(true);
        descLabel.setTextAlignment(TextAlignment.LEFT);

        // --- subscribe button ---
        subscribeBtn = new Button("+  订阅");
        subscribeBtn.getStyleClass().addAll("accent");
        subscribeBtn.setOnAction(e -> {
            if (podcast != null && onSubscribe != null) {
                subscribeBtn.setDisable(true);
                subscribeBtn.setText("订阅中...");
                onSubscribe.accept(podcast);
            }
        });

        statusLabel = new Label();
        statusLabel.getStyleClass().add("discover-detail-status");

        HBox actions = new HBox(12, subscribeBtn, statusLabel);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox infoBox = new VBox(8, titleLabel, authorLabel, metaBox, descLabel, actions);
        infoBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        HBox headerRow = new HBox(20, imagePane, infoBox);
        headerRow.setAlignment(Pos.TOP_LEFT);

        VBox header = new VBox(8, backRow, headerRow);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.getStyleClass().add("discover-detail-header");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox content = new VBox(header, spacer);
        setCenter(content);
    }

    public void setPodcast(Podcast podcast) {
        this.podcast = podcast;
        titleLabel.setText(podcast.getTitle());
        authorLabel.setText(podcast.getAuthor() != null ? podcast.getAuthor() : "");

        // build metadata lines
        metaBox.getChildren().clear();
        addMetaLine("分类", buildGenreText(podcast));

        if (podcast.getReleaseDate() != null && !podcast.getReleaseDate().isBlank()) {
            String formatted = formatDate(podcast.getReleaseDate());
            if (!formatted.isBlank()) addMetaLine("最新一集", formatted);
            String since = timeSince(podcast.getReleaseDate());
            if (since != null && !since.isBlank()) addMetaLine("更新状态", since);
        }

        if (podcast.getTrackCount() > 0) {
            addMetaLine("单集数量", podcast.getTrackCount() + " 集");
        }

        // description
        String desc = podcast.getDescription();
        if (desc != null && !desc.isBlank()) {
            descLabel.setText(desc);
            descLabel.setVisible(true);
            descLabel.setManaged(true);
        } else {
            descLabel.setVisible(false);
            descLabel.setManaged(false);
        }

        Image img = loadImage(podcast.getImageUrl());
        if (img != null) {
            imageView.setImage(img);
            imageView.setFitWidth(180);
            imageView.setFitHeight(180);
            imageView.setVisible(true);
        }

        subscribeBtn.setDisable(false);
        subscribeBtn.setText("+  订阅");
        statusLabel.setText("");
    }

    private void addMetaLine(String key, String value) {
        if (value == null || value.isBlank()) return;
        Label line = new Label(key + ":  " + value);
        line.getStyleClass().add("discover-detail-meta-line");
        line.setWrapText(true);
        metaBox.getChildren().add(line);
    }

    private static String buildGenreText(Podcast p) {
        List<String> genres = p.getGenres();
        if (genres != null && !genres.isEmpty()) {
            return String.join(" · ", genres);
        }
        if (p.getPrimaryGenre() != null && !p.getPrimaryGenre().isBlank()) {
            return p.getPrimaryGenre();
        }
        return "";
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
            if (days < 60) return null; // active podcast, nothing noteworthy
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

    private static Image loadImage(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            return new Image(url, true);
        } catch (Exception e) {
            return null;
        }
    }
}
