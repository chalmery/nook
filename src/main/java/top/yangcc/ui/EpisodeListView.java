package top.yangcc.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import top.yangcc.model.Episode;
import top.yangcc.util.HtmlUtils;
import top.yangcc.model.Podcast;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.text.SimpleDateFormat;
import java.util.function.Consumer;

public class EpisodeListView extends VBox {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.episodelist");

    private final ListView<Episode> episodeList;
    private Episode playingEpisode;
    private Episode loadingEpisode;
    private Consumer<Episode> onEpisodeClicked;
    private Consumer<Episode> onPlayRequested;
    private boolean showAll = false;

    public EpisodeListView() {
        getStyleClass().add("episode-list-view");

        episodeList = new ListView<>();
        episodeList.setPlaceholder(new Label("  暂无单集"));
        episodeList.setCellFactory(listView -> new EpisodeCell());
        episodeList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (onEpisodeClicked != null && newVal != null) {
                        onEpisodeClicked.accept(newVal);
                    }
                });
        episodeList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Episode selected = episodeList.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getAudioUrl() != null && onPlayRequested != null) {
                    LOG.log(Level.INFO, "Episode double-clicked: {0}", selected.getTitle());
                    onPlayRequested.accept(selected);
                }
            }
        });
        VBox.setVgrow(episodeList, Priority.ALWAYS);

        getChildren().add(episodeList);
    }

    /** Show all episodes without internal scrolling — for use inside an outer ScrollPane. */
    public void setShowAll(boolean showAll) {
        this.showAll = showAll;
        if (showAll) {
            episodeList.setFixedCellSize(56);
            VBox.setVgrow(episodeList, Priority.NEVER);
            updateListViewHeight();
            // hide internal scroll bars after skin is laid out
            javafx.application.Platform.runLater(() ->
                episodeList.lookupAll(".scroll-bar").forEach(n -> n.setVisible(false)));
        }
    }

    private void updateListViewHeight() {
        if (!showAll) return;
        int n = episodeList.getItems().size();
        double h = n > 0 ? n * episodeList.getFixedCellSize() + 2 : 80;
        episodeList.setPrefHeight(h);
        episodeList.setMinHeight(h);
        episodeList.setMaxHeight(h);
    }

    public void setPodcast(Podcast podcast) {
        if (podcast == null) {
            episodeList.getItems().clear();
            return;
        }
        episodeList.getItems().setAll(podcast.getEpisodes());
        playingEpisode = null;
        episodeList.refresh();
        updateListViewHeight();
        LOG.log(Level.INFO, "Episode list populated: {0} episodes", podcast.getEpisodes().size());
    }

    public void setPlayingEpisode(Episode episode) {
        this.playingEpisode = episode;
        this.loadingEpisode = null;
        episodeList.refresh();
        if (episode != null) {
            LOG.log(Level.INFO, "Now playing indicator set to: {0}", episode.getTitle());
        }
    }

    public void setLoadingEpisode(Episode episode) {
        this.loadingEpisode = episode;
        this.playingEpisode = null;
        episodeList.refresh();
    }

    public void setOnEpisodeClicked(Consumer<Episode> handler) { this.onEpisodeClicked = handler; }
    public void setOnPlayRequested(Consumer<Episode> handler) { this.onPlayRequested = handler; }

    private class EpisodeCell extends ListCell<Episode> {
        private final Label titleLabel;
        private final Label dateLabel;
        private final Label durationLabel;
        private final Label descLabel;
        private final Label playingIndicator;
        private final HBox row;

        EpisodeCell() {
            playingIndicator = new Label("▸");
            playingIndicator.getStyleClass().add("playing-indicator");
            playingIndicator.setVisible(false);

            titleLabel = new Label();
            titleLabel.getStyleClass().add("episode-title");
            titleLabel.setMinWidth(0);

            dateLabel = new Label();
            dateLabel.getStyleClass().add("episode-date");
            dateLabel.setMinWidth(80);
            dateLabel.setAlignment(Pos.CENTER_RIGHT);

            durationLabel = new Label();
            durationLabel.getStyleClass().add("episode-duration");
            durationLabel.setMinWidth(60);
            durationLabel.setAlignment(Pos.CENTER_RIGHT);

            HBox rightBox = new HBox(8, dateLabel, durationLabel);
            rightBox.setAlignment(Pos.CENTER_RIGHT);

            BorderPane topRow = new BorderPane();
            topRow.setLeft(titleLabel);
            topRow.setRight(rightBox);
            BorderPane.setMargin(rightBox, new javafx.geometry.Insets(0, 0, 0, 8));

            descLabel = new Label();
            descLabel.getStyleClass().add("episode-desc");
            descLabel.setWrapText(true);
            descLabel.setMaxHeight(18);

            VBox cellContent = new VBox(2, topRow, descLabel);

            row = new HBox(8, playingIndicator, cellContent);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(cellContent, Priority.ALWAYS);
        }

        @Override
        protected void updateItem(Episode episode, boolean empty) {
            super.updateItem(episode, empty);
            if (empty || episode == null) {
                setGraphic(null);
            } else {
                titleLabel.setText(episode.getTitle());

                if (episode.getPubDate() != null) {
                    dateLabel.setText(new SimpleDateFormat("yyyy-MM-dd").format(episode.getPubDate()));
                } else {
                    dateLabel.setText("");
                }

                durationLabel.setText(episode.getDuration() != null ? episode.getDuration() : "");

                String desc = episode.getDescription();
                if (desc != null && !desc.isBlank()) {
                    String clean = HtmlUtils.clean(desc);
                    clean = clean.replace('\n', ' ');
                    descLabel.setText(clean.length() > 50 ? clean.substring(0, 50) + "..." : clean);
                    descLabel.setVisible(true);
                    descLabel.setManaged(true);
                } else {
                    descLabel.setVisible(false);
                    descLabel.setManaged(false);
                }

                boolean isLoading = episode == loadingEpisode;
                boolean isPlaying = episode == playingEpisode;

                if (isLoading) {
                    playingIndicator.setText("⟳");
                    playingIndicator.getStyleClass().add("episode-loading");
                    playingIndicator.getStyleClass().remove("playing-indicator");
                    playingIndicator.setVisible(true);
                    titleLabel.getStyleClass().remove("episode-playing");
                } else if (isPlaying) {
                    playingIndicator.setText("▸");
                    playingIndicator.getStyleClass().remove("episode-loading");
                    playingIndicator.getStyleClass().add("playing-indicator");
                    playingIndicator.setVisible(true);
                    titleLabel.getStyleClass().add("episode-playing");
                } else {
                    playingIndicator.setVisible(false);
                    playingIndicator.getStyleClass().remove("episode-loading");
                    titleLabel.getStyleClass().remove("episode-playing");
                }

                setGraphic(row);
            }
        }
    }
}
