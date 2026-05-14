package top.yangcc.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class SubscriptionGridView extends BorderPane {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.grid");

    private final Label countLabel;
    private final FlowPane cardGrid;
    private final List<Podcast> podcasts;

    private Consumer<Podcast> onPodcastSelected;
    private Runnable onAddSubscription;

    public SubscriptionGridView(List<Podcast> podcasts) {
        this.podcasts = podcasts;
        getStyleClass().add("grid-view");

        // --- top bar ---
        VBox headerLeft = new VBox(2);
        Label title = new Label("我的订阅");
        title.getStyleClass().add("grid-title");
        countLabel = new Label();
        countLabel.getStyleClass().add("grid-subtitle");
        headerLeft.getChildren().addAll(title, countLabel);

        Button addBtn = new Button("+  添加播客源");
        addBtn.getStyleClass().addAll("accent");
        addBtn.setOnAction(e -> {
            LOG.log(Level.INFO, "Add subscription from grid");
            if (onAddSubscription != null) onAddSubscription.run();
        });

        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("grid-header");
        topBar.getChildren().addAll(headerLeft, spacer(), addBtn);

        // --- card grid ---
        cardGrid = new FlowPane();
        cardGrid.setHgap(16);
        cardGrid.setVgap(16);
        cardGrid.setPadding(new Insets(16, 0, 0, 0));
        cardGrid.getStyleClass().add("card-grid");

        ScrollPane scrollPane = new ScrollPane(cardGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("grid-scroll");

        setTop(topBar);
        setCenter(scrollPane);
        BorderPane.setMargin(topBar, new Insets(20, 24, 0, 24));
        BorderPane.setMargin(scrollPane, new Insets(0, 24, 20, 24));

        refreshCards();
    }

    public void refreshCards() {
        cardGrid.getChildren().clear();
        for (Podcast p : podcasts) {
            cardGrid.getChildren().add(buildCard(p));
        }
        countLabel.setText("共 " + podcasts.size() + " 个播客源");
    }

    private PodcastCard buildCard(Podcast podcast) {
        PodcastCard card = new PodcastCard(podcast);
        card.setOnMouseClicked(e -> {
            LOG.log(Level.INFO, "Podcast card clicked: {0}", podcast.getTitle());
            if (onPodcastSelected != null) onPodcastSelected.accept(podcast);
        });
        return card;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    // --- card ---
    private static class PodcastCard extends VBox {

        private static final double CARD_WIDTH = 190;
        private static final double IMG_SIZE = 190;

        PodcastCard(Podcast podcast) {
            setPrefWidth(CARD_WIDTH);
            setMaxWidth(CARD_WIDTH);
            getStyleClass().add("podcast-card");

            // square image, edge-to-edge at top
            StackPane imagePane = new StackPane();
            imagePane.setPrefSize(CARD_WIDTH, IMG_SIZE);
            imagePane.setMinSize(CARD_WIDTH, IMG_SIZE);
            imagePane.setMaxSize(CARD_WIDTH, IMG_SIZE);

            Rectangle clip = new Rectangle(CARD_WIDTH, IMG_SIZE);
            clip.setArcWidth(12);
            clip.setArcHeight(12);
            imagePane.setClip(clip);

            Image img = loadImage(podcast.getImageUrl());
            if (img != null) {
                ImageView imageView = new ImageView(img);
                imageView.setFitWidth(CARD_WIDTH);
                imageView.setFitHeight(IMG_SIZE);
                imageView.setPreserveRatio(true);
                imageView.getStyleClass().add("podcast-card-image");
                imagePane.getChildren().add(imageView);
            } else {
                Rectangle gradient = new Rectangle(CARD_WIDTH, IMG_SIZE);
                gradient.setFill(gradientFor(podcast.getTitle()));
                imagePane.getChildren().add(gradient);
            }

            // --- text content area ---
            VBox textBox = new VBox(4);
            textBox.setPadding(new Insets(8, 8, 0, 8));
            textBox.setAlignment(Pos.TOP_LEFT);

            Label titleLabel = new Label(podcast.getTitle());
            titleLabel.getStyleClass().add("podcast-card-title");
            titleLabel.setMaxWidth(CARD_WIDTH - 16);
            titleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            javafx.scene.control.Tooltip.install(titleLabel, new javafx.scene.control.Tooltip(podcast.getTitle()));

            String author = podcast.getAuthor() != null ? podcast.getAuthor() : "";
            Label authorLabel = new Label(author);
            authorLabel.getStyleClass().add("podcast-card-author");

            textBox.getChildren().addAll(titleLabel, authorLabel);

            // --- footer: dashed separator + date (left) / since badge (right) ---
            Separator footerSep = new Separator();
            footerSep.getStyleClass().add("podcast-card-footer-sep");
            VBox.setMargin(footerSep, new Insets(8, 0, 0, 0));

            HBox footer = new HBox(6);
            footer.setAlignment(Pos.CENTER_LEFT);
            footer.setPadding(new Insets(6, 8, 10, 8));

            Date latestPubDate = latestPubDate(podcast.getEpisodes());
            String dateText = buildDateText(latestPubDate);
            if (!dateText.isBlank()) {
                Label dateLabel = new Label(dateText);
                dateLabel.getStyleClass().add("podcast-card-date");
                footer.getChildren().add(dateLabel);
            }

            Region footerSpacer = new Region();
            HBox.setHgrow(footerSpacer, Priority.ALWAYS);
            footer.getChildren().add(footerSpacer);

            SinceInfo since = timeSinceInfo(latestPubDate);
            if (since != null) {
                Label sinceLabel = new Label(since.text);
                sinceLabel.getStyleClass().add(since.styleClass);
                footer.getChildren().add(sinceLabel);
            }

            getChildren().addAll(imagePane, textBox, footerSep, footer);
        }

        private static Date latestPubDate(List<Episode> episodes) {
            if (episodes == null || episodes.isEmpty()) return null;
            Date latest = null;
            for (Episode ep : episodes) {
                if (ep.getPubDate() != null) {
                    if (latest == null || ep.getPubDate().after(latest)) {
                        latest = ep.getPubDate();
                    }
                }
            }
            return latest;
        }

        private static String buildDateText(Date pubDate) {
            if (pubDate == null) return "";
            return "最新: " + DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    .format(pubDate.toInstant().atZone(java.time.ZoneId.systemDefault()));
        }

        private record SinceInfo(String text, String styleClass) {}

        private static SinceInfo timeSinceInfo(Date pubDate) {
            if (pubDate == null) return null;
            try {
                ZonedDateTime then = pubDate.toInstant().atZone(java.time.ZoneId.systemDefault());
                ZonedDateTime now = ZonedDateTime.now();
                long days = ChronoUnit.DAYS.between(then, now);
                if (days < 60) return null;
                long months = ChronoUnit.MONTHS.between(then, now);
                if (months >= 12) {
                    long years = ChronoUnit.YEARS.between(then, now);
                    return new SinceInfo(years + "年未更新", "podcast-card-since-danger");
                }
                return new SinceInfo(months + "个月未更新", "podcast-card-since");
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

        private static Image loadImage(String url) {
            if (url == null || url.isBlank()) return null;
            try {
                return new Image(url, true);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public void setOnPodcastSelected(Consumer<Podcast> handler) { this.onPodcastSelected = handler; }
    public void setOnAddSubscription(Runnable handler) { this.onAddSubscription = handler; }
}
