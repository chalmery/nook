package top.yangcc.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import top.yangcc.model.Podcast;
import top.yangcc.service.ITunesSearchService;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DiscoverView extends BorderPane {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.discover");

    private static final int BATCH_SIZE = 30;
    private static final int PAGE_CHUNK = 15;
    private static final double TRENDING_CARD_WIDTH = 190;
    private static final double TRENDING_IMG_SIZE = 190;
    private static final double TRENDING_SCROLL_HEIGHT = 290;
    private static final double GRID_CARD_WIDTH = 190;
    private static final double GRID_IMG_SIZE = 190;

    private final ITunesSearchService searchService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "discover-worker");
        t.setDaemon(true);
        return t;
    });

    // sections
    private final VBox trendingSection;
    private final HBox trendingCards;
    private final Button viewAllBtn;
    private final TextField searchField;
    private final GenreChipBar genreChipBar;
    private final HBox searchBar;
    private final HBox backBar;
    private final VBox topSection;

    // grid
    private final FlowPane cardGrid;
    private final Label loadingLabel;
    private final ScrollPane rootScroll;

    // state
    private final List<Podcast> loadedResults = new ArrayList<>();
    private int shownCount = 0;
    private String currentSearchTerm;
    private String currentGenreName;
    private boolean isLoading;
    private boolean hasMore = true;
    private boolean trendingExpanded;
    private boolean isSearching;
    private boolean isTrendingFullView;
    private boolean scrollThrottle;

    private Consumer<Podcast> onPodcastSelected;

    public DiscoverView() {
        searchService = new ITunesSearchService();
        getStyleClass().add("discover-view");

        // ==========================================
        // Trending section
        // ==========================================
        Label trendingTitle = new Label("热门推荐");
        trendingTitle.getStyleClass().add("discover-section-title");

        viewAllBtn = new Button("查看全部 →");
        viewAllBtn.getStyleClass().addAll("accent");
        viewAllBtn.setStyle("-fx-font-size: 11px; -fx-padding: 2 10 2 10;");
        viewAllBtn.setOnAction(e -> openTrendingFullView());

        HBox trendingHeader = new HBox(trendingTitle, spacer(), viewAllBtn);
        trendingHeader.setAlignment(Pos.CENTER_LEFT);

        trendingCards = new HBox(16);
        trendingCards.setPadding(new Insets(8, 0, 4, 0));
        trendingCards.setAlignment(Pos.CENTER_LEFT);

        ScrollPane trendingScroll = new ScrollPane(trendingCards);
        trendingScroll.setFitToHeight(true);
        trendingScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        trendingScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        trendingScroll.getStyleClass().add("discover-trending-scroll");
        trendingScroll.setPrefHeight(TRENDING_SCROLL_HEIGHT);
        trendingScroll.setMinHeight(TRENDING_SCROLL_HEIGHT);

        trendingSection = new VBox(4, trendingHeader, trendingScroll);
        trendingSection.getStyleClass().add("discover-trending-section");

        // ==========================================
        // Back bar (shown in trending full view)
        // ==========================================
        Button backBtn = new Button("←  返回");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> closeTrendingFullView());
        backBar = new HBox(backBtn);
        backBar.setAlignment(Pos.CENTER_LEFT);
        backBar.setPadding(new Insets(4, 24, 0, 24));
        backBar.setVisible(false);
        backBar.setManaged(false);

        // ==========================================
        // Search bar
        // ==========================================
        searchField = new TextField();
        searchField.setPromptText("搜索播客或输入关键词...");
        searchField.getStyleClass().add("discover-search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Runnable runSearch = () -> {
            String query = searchField.getText();
            if (query != null && !query.isBlank()) {
                doSearch(query.trim());
            } else if (isSearching) {
                clearSearch();
            }
        };
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) runSearch.run();
        });

        Button searchBtn = new Button("搜索");
        searchBtn.getStyleClass().addAll("accent");
        searchBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 14 4 14;");
        searchBtn.setOnAction(e -> runSearch.run());

        searchBar = new HBox(8, searchField, searchBtn);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.getStyleClass().add("discover-search-bar");
        searchBar.setPadding(new Insets(8, 14, 8, 14));
        VBox.setMargin(searchBar, new Insets(12, 0, 4, 0));

        // ==========================================
        // Genre bar
        // ==========================================
        genreChipBar = new GenreChipBar();

        // ==========================================
        // Podcast grid
        // ==========================================
        cardGrid = new FlowPane();
        cardGrid.setHgap(16);
        cardGrid.setVgap(16);
        cardGrid.setPadding(new Insets(8, 24, 0, 24));
        cardGrid.getStyleClass().add("discover-card-grid");

        loadingLabel = new Label();
        loadingLabel.getStyleClass().add("discover-loading");
        loadingLabel.setVisible(false);
        loadingLabel.setMaxWidth(Double.MAX_VALUE);
        loadingLabel.setAlignment(Pos.CENTER);
        loadingLabel.setPadding(new Insets(24, 0, 60, 0));

        VBox gridBox = new VBox(cardGrid, loadingLabel);
        ScrollPane gridScroll = new ScrollPane(gridBox);
        gridScroll.setFitToWidth(true);
        gridScroll.getStyleClass().add("discover-grid-scroll");
        VBox.setVgrow(gridScroll, Priority.ALWAYS);

        // ==========================================
        // Layout assembly
        // ==========================================
        topSection = new VBox(trendingSection, searchBar, genreChipBar);
        topSection.setPadding(new Insets(16, 24, 0, 24));

        VBox contentBox = new VBox(backBar, topSection, gridScroll);
        contentBox.setFillWidth(true);

        rootScroll = new ScrollPane(contentBox);
        rootScroll.setFitToWidth(true);
        rootScroll.getStyleClass().add("discover-root-scroll");

        setCenter(rootScroll);

        // ==========================================
        // Scroll listener (throttled)
        // ==========================================
        rootScroll.vvalueProperty().addListener((obs, old, val) -> {
            if (scrollThrottle) return;
            if (val.doubleValue() > 0.8 && !isLoading) {
                scrollThrottle = true;
                showNextChunk();
                PauseTransition pt = new PauseTransition(Duration.millis(200));
                pt.setOnFinished(ev -> scrollThrottle = false);
                pt.play();
            }
        });

        // ==========================================
        // Genre selection
        // ==========================================
        genreChipBar.setOnGenreSelected((genreId, genreName) -> {
            searchField.clear();
            isSearching = false;
            isTrendingFullView = false;
            currentGenreName = genreName;
            currentSearchTerm = null;
            resetAndLoad();
        });

        // initial load — default to "新闻" genre
        currentGenreName = "新闻";
        loadTrending();
        resetAndLoad();
    }

    // ==========================================
    // Trending full view
    // ==========================================
    private void openTrendingFullView() {
        isTrendingFullView = true;
        topSection.setVisible(false);
        topSection.setManaged(false);
        backBar.setVisible(true);
        backBar.setManaged(true);

        loadedResults.clear();
        shownCount = 0;
        cardGrid.getChildren().clear();
        hasMore = false;
        isLoading = true;
        loadingLabel.setText("正在加载...");
        loadingLabel.setVisible(true);

        executor.submit(() -> {
            try {
                List<Podcast> trending = searchService.getTrending(50);
                Platform.runLater(() -> {
                    loadedResults.addAll(trending);
                    isLoading = false;
                    showNextChunk();
                });
            } catch (Exception e) {
                LOG.log(Level.ERROR, "Failed to load trending full: " + e.getMessage(), e);
                Platform.runLater(() -> {
                    isLoading = false;
                    loadingLabel.setVisible(false);
                });
            }
        });
    }

    private void closeTrendingFullView() {
        isTrendingFullView = false;
        backBar.setVisible(false);
        backBar.setManaged(false);
        topSection.setVisible(true);
        topSection.setManaged(true);
        loadTrending();
        resetAndLoad();
    }

    private void loadTrending() {
        executor.submit(() -> {
            List<Podcast> trending = searchService.getTrending(10);
            Platform.runLater(() -> renderTrendingCards(trending));
        });
    }

    private void renderTrendingCards(List<Podcast> trending) {
        trendingCards.getChildren().clear();
        for (int i = 0; i < trending.size(); i++) {
            trendingCards.getChildren().add(buildTrendingCard(trending.get(i), i + 1));
        }
    }

    private void doSearch(String query) {
        isSearching = true;
        isTrendingFullView = false;
        currentSearchTerm = query;
        currentGenreName = null;
        trendingSection.setVisible(false);
        trendingSection.setManaged(false);
        genreChipBar.setVisible(false);
        genreChipBar.setManaged(false);
        resetAndLoad();
    }

    private void clearSearch() {
        isSearching = false;
        currentSearchTerm = null;
        currentGenreName = "新闻";
        trendingSection.setVisible(true);
        trendingSection.setManaged(true);
        genreChipBar.setVisible(true);
        genreChipBar.setManaged(true);
        resetAndLoad();
    }

    private void resetAndLoad() {
        loadedResults.clear();
        shownCount = 0;
        cardGrid.getChildren().clear();
        hasMore = true;
        isLoading = false;
        loadBatch();
    }

    private void loadBatch() {
        if (isLoading) return;
        isLoading = true;
        loadingLabel.setText("正在加载...");
        loadingLabel.setVisible(true);

        executor.submit(() -> {
            try {
                List<Podcast> batch;
                if (currentSearchTerm != null) {
                    batch = searchService.search(currentSearchTerm, BATCH_SIZE);
                } else if (currentGenreName != null) {
                    batch = searchService.searchByGenreName(currentGenreName, BATCH_SIZE);
                } else {
                    batch = searchService.search("podcast", BATCH_SIZE);
                }

                Platform.runLater(() -> {
                    loadedResults.addAll(batch);
                    isLoading = false;
                    if (batch.size() < BATCH_SIZE) {
                        hasMore = false;
                    }
                    showNextChunk();
                });
            } catch (Exception e) {
                LOG.log(Level.ERROR, "Failed to load: " + e.getMessage(), e);
                Platform.runLater(() -> {
                    isLoading = false;
                    loadingLabel.setVisible(false);
                });
            }
        });
    }

    private void showNextChunk() {
        if (isLoading) return;
        int end = Math.min(shownCount + PAGE_CHUNK, loadedResults.size());

        if (end <= shownCount) {
            if (!hasMore) { loadingLabel.setVisible(false); return; }
            loadBatch();
            return;
        }

        for (int i = shownCount; i < end; i++) {
            cardGrid.getChildren().add(buildGridCard(loadedResults.get(i)));
        }
        shownCount = end;

        if (shownCount >= loadedResults.size()) {
            if (!hasMore) {
                loadingLabel.setVisible(false);
                if (shownCount == 0) showEmpty();
            } else {
                loadingLabel.setText("正在加载更多...");
                loadingLabel.setVisible(true);
                loadBatch();
            }
        } else {
            loadingLabel.setText("向下滚动加载更多");
            loadingLabel.setVisible(true);
        }
    }

    private void showEmpty() {
        Label emptyLabel = new Label("没有找到播客");
        emptyLabel.getStyleClass().add("discover-empty");
        emptyLabel.setMaxWidth(Double.MAX_VALUE);
        emptyLabel.setAlignment(Pos.CENTER);
        emptyLabel.setPadding(new Insets(40, 0, 40, 0));
        cardGrid.getChildren().add(emptyLabel);
    }

    // ==========================================
    // Trending card
    // ==========================================
    private VBox buildTrendingCard(Podcast podcast, int rank) {
        VBox card = new VBox(6);
        card.setPrefWidth(TRENDING_CARD_WIDTH);
        card.setMaxWidth(TRENDING_CARD_WIDTH);
        card.setMinWidth(TRENDING_CARD_WIDTH);
        card.setAlignment(Pos.TOP_CENTER);
        card.getStyleClass().add("discover-trending-card");
        card.setOnMouseClicked(e -> {
            if (onPodcastSelected != null) onPodcastSelected.accept(podcast);
        });

        StackPane imagePane = new StackPane();
        imagePane.setPrefSize(TRENDING_CARD_WIDTH, TRENDING_IMG_SIZE);
        imagePane.setMinSize(TRENDING_CARD_WIDTH, TRENDING_IMG_SIZE);
        imagePane.setMaxSize(TRENDING_CARD_WIDTH, TRENDING_IMG_SIZE);

        Image img = loadImage(podcast.getImageUrl());
        if (img != null) {
            ImageView imageView = new ImageView(img);
            imageView.setFitWidth(TRENDING_CARD_WIDTH);
            imageView.setFitHeight(TRENDING_IMG_SIZE);
            imageView.setPreserveRatio(false);
            imageView.getStyleClass().add("discover-trending-image");
            imagePane.getChildren().add(imageView);
        } else {
            Rectangle gradient = new Rectangle(TRENDING_CARD_WIDTH, TRENDING_IMG_SIZE);
            gradient.setFill(PodcastDetailView.gradientFor(podcast.getTitle()));
            imagePane.getChildren().add(gradient);
        }

        Label rankLabel = new Label(String.valueOf(rank));
        rankLabel.getStyleClass().add("discover-rank-badge");
        StackPane.setAlignment(rankLabel, Pos.TOP_LEFT);
        StackPane.setMargin(rankLabel, new Insets(6, 0, 0, 6));
        imagePane.getChildren().add(rankLabel);

        Rectangle clip = new Rectangle(TRENDING_CARD_WIDTH, TRENDING_IMG_SIZE);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        imagePane.setClip(clip);

        Label titleLabel = new Label(podcast.getTitle());
        titleLabel.getStyleClass().add("discover-trending-title");
        titleLabel.setMaxWidth(TRENDING_CARD_WIDTH);
        titleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        javafx.scene.control.Tooltip.install(titleLabel, new javafx.scene.control.Tooltip(podcast.getTitle()));

        Label authorLabel = new Label(podcast.getAuthor());
        authorLabel.getStyleClass().add("discover-trending-author");
        authorLabel.setMaxWidth(TRENDING_CARD_WIDTH);

        // text content with padding
        VBox textBox = new VBox(4);
        textBox.setPadding(new Insets(6, 6, 8, 6));
        textBox.setAlignment(Pos.TOP_LEFT);
        textBox.getChildren().addAll(titleLabel, authorLabel);

        // genre chips — tiny, max 2
        List<String> trendingGenres = podcast.getGenres();
        if (trendingGenres != null && !trendingGenres.isEmpty()) {
            HBox chipRow = new HBox(4);
            chipRow.setAlignment(Pos.TOP_LEFT);
            int count = 0;
            for (String g : trendingGenres) {
                if (count >= 2) break;
                Label chip = new Label(g);
                chip.getStyleClass().add("discover-trending-genre-chip");
                chipRow.getChildren().add(chip);
                count++;
            }
            textBox.getChildren().add(chipRow);
        }

        card.getChildren().addAll(imagePane, textBox);
        return card;
    }

    // ==========================================
    // Grid card
    // ==========================================
    private VBox buildGridCard(Podcast podcast) {
        VBox card = new VBox();
        card.setPrefWidth(GRID_CARD_WIDTH);
        card.setMaxWidth(GRID_CARD_WIDTH);
        card.getStyleClass().add("discover-grid-card");
        card.setOnMouseClicked(e -> {
            if (onPodcastSelected != null) onPodcastSelected.accept(podcast);
        });

        // --- square image, same proportion as trending cards ---
        StackPane imagePane = new StackPane();
        imagePane.setPrefSize(GRID_CARD_WIDTH, GRID_IMG_SIZE);
        imagePane.setMinSize(GRID_CARD_WIDTH, GRID_IMG_SIZE);
        imagePane.setMaxSize(GRID_CARD_WIDTH, GRID_IMG_SIZE);

        Rectangle clip = new Rectangle(GRID_CARD_WIDTH, GRID_IMG_SIZE);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        imagePane.setClip(clip);

        Image img = loadImage(podcast.getImageUrl());
        if (img != null) {
            ImageView imageView = new ImageView(img);
            imageView.setFitWidth(GRID_CARD_WIDTH);
            imageView.setFitHeight(GRID_IMG_SIZE);
            imageView.setPreserveRatio(true);
            imageView.getStyleClass().add("discover-grid-image");
            imagePane.getChildren().add(imageView);
        } else {
            Rectangle gradient = new Rectangle(GRID_CARD_WIDTH, GRID_IMG_SIZE);
            gradient.setFill(PodcastDetailView.gradientFor(podcast.getTitle()));
            imagePane.getChildren().add(gradient);
        }

        // --- text content area ---
        VBox textBox = new VBox(4);
        textBox.setPadding(new Insets(8, 8, 0, 8));
        textBox.setAlignment(Pos.TOP_LEFT);

        // title — single line, ellipsis, tooltip for full name
        Label titleLabel = new Label(podcast.getTitle());
        titleLabel.getStyleClass().add("discover-grid-title");
        titleLabel.setMaxWidth(GRID_CARD_WIDTH - 16);
        titleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        javafx.scene.control.Tooltip.install(titleLabel, new javafx.scene.control.Tooltip(podcast.getTitle()));

        Label authorLabel = new Label(podcast.getAuthor() != null ? podcast.getAuthor() : "");
        authorLabel.getStyleClass().add("discover-grid-author");

        textBox.getChildren().addAll(titleLabel, authorLabel);

        // genre chips — small pill badges, max 3
        List<String> displayGenres = podcast.getGenres();
        if ((displayGenres == null || displayGenres.isEmpty())
                && podcast.getPrimaryGenre() != null && !podcast.getPrimaryGenre().isBlank()) {
            displayGenres = List.of(podcast.getPrimaryGenre());
        }
        if (displayGenres != null && !displayGenres.isEmpty()) {
            HBox chipRow = new HBox(4);
            chipRow.setAlignment(Pos.TOP_LEFT);
            int count = 0;
            for (String g : displayGenres) {
                if (count >= 3) break;
                Label chip = new Label(g);
                chip.getStyleClass().add("discover-grid-genre-chip");
                chipRow.getChildren().add(chip);
                count++;
            }
            textBox.getChildren().add(chipRow);
        }

        // --- footer: dashed separator + date (left) / since badge (right) ---
        String dateText = buildDateText(podcast);
        SinceInfo since = timeSinceInfo(podcast.getReleaseDate());
        if (!dateText.isBlank() || since != null) {
            Separator footerSep = new Separator();
            footerSep.getStyleClass().add("discover-grid-footer-sep");
            VBox.setMargin(footerSep, new Insets(8, 0, 0, 0));

            HBox footer = new HBox(6);
            footer.setAlignment(Pos.CENTER_LEFT);
            footer.setPadding(new Insets(6, 8, 10, 8));

            if (!dateText.isBlank()) {
                Label dateLabel = new Label(dateText);
                dateLabel.getStyleClass().add("discover-grid-date");
                footer.getChildren().add(dateLabel);
            }

            Region footerSpacer = new Region();
            HBox.setHgrow(footerSpacer, Priority.ALWAYS);
            footer.getChildren().add(footerSpacer);

            if (since != null) {
                Label sinceLabel = new Label(since.text);
                sinceLabel.getStyleClass().add(since.styleClass);
                footer.getChildren().add(sinceLabel);
            }

            card.getChildren().addAll(imagePane, textBox, footerSep, footer);
        } else {
            // no date info — still need bottom padding
            textBox.setPadding(new Insets(8, 8, 10, 8));
            card.getChildren().addAll(imagePane, textBox);
        }
        return card;
    }

    private record SinceInfo(String text, String styleClass) {}

    private static SinceInfo timeSinceInfo(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) return null;
        try {
            java.time.Instant instant = java.time.Instant.parse(releaseDate);
            ZonedDateTime then = instant.atZone(java.time.ZoneId.systemDefault());
            ZonedDateTime now = ZonedDateTime.now();
            long days = ChronoUnit.DAYS.between(then, now);
            if (days < 60) return null; // still active — nothing noteworthy
            long months = ChronoUnit.MONTHS.between(then, now);
            if (months >= 12) {
                long years = ChronoUnit.YEARS.between(then, now);
                return new SinceInfo(years + "年未更新", "discover-grid-since-danger");
            }
            return new SinceInfo(months + "个月未更新", "discover-grid-since");
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildDateText(Podcast p) {
        if (p.getReleaseDate() == null || p.getReleaseDate().isBlank()) return "";
        try {
            java.time.Instant instant = java.time.Instant.parse(p.getReleaseDate());
            ZonedDateTime dt = instant.atZone(java.time.ZoneId.systemDefault());
            return "最新: " + dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return "";
        }
    }

    private static Image loadImage(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            return new Image(url, 300, 300, true, true, true);
        } catch (Exception e) {
            return null;
        }
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public void setOnPodcastSelected(Consumer<Podcast> handler) { this.onPodcastSelected = handler; }
}
