package top.yangcc.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DiscoverView extends BorderPane {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.discover");

    private static final int BATCH_SIZE = 30;
    private static final int PAGE_CHUNK = 15;
    private static final double TRENDING_CARD_WIDTH = 140;
    private static final double TRENDING_IMG_SIZE = 140;
    private static final double TRENDING_SCROLL_HEIGHT = 230;
    private static final double GRID_CARD_WIDTH = 180;
    private static final double GRID_IMG_HEIGHT = 140;

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
        viewAllBtn.setOnAction(e -> toggleTrendingExpand());

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
        // Search bar (button/Enter trigger only, no auto-debounce)
        // ==========================================
        searchField = new TextField();
        searchField.setPromptText("搜索播客或输入关键词...");
        searchField.getStyleClass().add("discover-search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // Only trigger search on Enter key or button — no textProperty listener
        // so IME composition on Linux works correctly
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

        HBox searchBar = new HBox(8, searchField, searchBtn);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.getStyleClass().add("discover-search-bar");
        searchBar.setPadding(new Insets(8, 14, 8, 14));
        VBox.setMargin(searchBar, new Insets(12, 0, 4, 0));

        // ==========================================
        // Genre bar (responsive FlowPane)
        // ==========================================
        genreChipBar = new GenreChipBar();

        // ==========================================
        // Podcast grid
        // ==========================================
        cardGrid = new FlowPane();
        cardGrid.setHgap(16);
        cardGrid.setVgap(16);
        cardGrid.setPadding(new Insets(8, 0, 0, 0));
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
        VBox topSection = new VBox(trendingSection, searchBar, genreChipBar);
        topSection.setPadding(new Insets(16, 24, 0, 24));

        VBox contentBox = new VBox(topSection, gridScroll);
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
            if (val.doubleValue() > 0.8 && !isLoading && hasMore) {
                scrollThrottle = true;
                showNextChunk();
                // re-arm throttle after 200ms
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
            if (GenreChipBar.isAllGenres(genreId)) {
                currentGenreName = null;
                currentSearchTerm = null;
            } else {
                currentGenreName = genreName;
                currentSearchTerm = null;
            }
            resetAndLoad();
        });

        // initial load
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

    private void toggleTrendingExpand() {
        trendingExpanded = !trendingExpanded;
        if (trendingExpanded) {
            viewAllBtn.setText("收起 ←");
            executor.submit(() -> {
                List<Podcast> trending = searchService.getTrending(50);
                Platform.runLater(() -> renderTrendingCards(trending));
            });
        } else {
            viewAllBtn.setText("查看全部 →");
            loadTrending();
        }
    }

    private void doSearch(String query) {
        isSearching = true;
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
        currentGenreName = null;
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
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(TRENDING_CARD_WIDTH);
        titleLabel.setPrefHeight(32);

        Label authorLabel = new Label(podcast.getAuthor());
        authorLabel.getStyleClass().add("discover-trending-author");
        authorLabel.setMaxWidth(TRENDING_CARD_WIDTH);

        card.getChildren().addAll(imagePane, titleLabel, authorLabel);
        return card;
    }

    // ==========================================
    // Grid card
    // ==========================================
    private VBox buildGridCard(Podcast podcast) {
        VBox card = new VBox();
        card.setPrefWidth(GRID_CARD_WIDTH);
        card.setMaxWidth(GRID_CARD_WIDTH);
        card.setAlignment(Pos.TOP_CENTER);
        card.getStyleClass().add("discover-grid-card");
        card.setOnMouseClicked(e -> {
            if (onPodcastSelected != null) onPodcastSelected.accept(podcast);
        });

        StackPane imagePane = new StackPane();
        imagePane.setPrefSize(GRID_CARD_WIDTH, GRID_IMG_HEIGHT);
        imagePane.setMinSize(GRID_CARD_WIDTH, GRID_IMG_HEIGHT);
        imagePane.setMaxSize(GRID_CARD_WIDTH, GRID_IMG_HEIGHT);

        Rectangle clip = new Rectangle(GRID_CARD_WIDTH, GRID_IMG_HEIGHT);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        imagePane.setClip(clip);

        Image img = loadImage(podcast.getImageUrl());
        if (img != null) {
            ImageView imageView = new ImageView(img);
            imageView.setFitWidth(GRID_CARD_WIDTH);
            imageView.setFitHeight(GRID_IMG_HEIGHT);
            imageView.setPreserveRatio(false);
            imageView.getStyleClass().add("discover-grid-image");
            imagePane.getChildren().add(imageView);
        } else {
            Rectangle gradient = new Rectangle(GRID_CARD_WIDTH, GRID_IMG_HEIGHT);
            gradient.setFill(PodcastDetailView.gradientFor(podcast.getTitle()));
            imagePane.getChildren().add(gradient);
        }

        Label titleLabel = new Label(podcast.getTitle());
        titleLabel.getStyleClass().add("discover-grid-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(GRID_CARD_WIDTH - 24);
        titleLabel.setAlignment(Pos.CENTER);
        VBox.setMargin(titleLabel, new Insets(10, 10, 0, 10));

        Label authorLabel = new Label(podcast.getAuthor() != null ? podcast.getAuthor() : "");
        authorLabel.getStyleClass().add("discover-grid-author");
        VBox.setMargin(authorLabel, new Insets(4, 0, 8, 0));

        card.getChildren().addAll(imagePane, titleLabel, authorLabel);
        return card;
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
