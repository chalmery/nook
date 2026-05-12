package top.yangcc.ui;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import top.yangcc.model.Episode;
import top.yangcc.model.Podcast;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.text.SimpleDateFormat;
import java.util.function.Consumer;

public class EpisodeListView extends VBox {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.episodelist");

    private final ListView<Episode> episodeList;
    private Episode playingEpisode;
    private Consumer<Episode> onEpisodeClicked;
    private Consumer<Episode> onPlayRequested;

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

    public void setPodcast(Podcast podcast) {
        if (podcast == null) {
            episodeList.getItems().clear();
            return;
        }
        episodeList.getItems().setAll(podcast.getEpisodes());
        playingEpisode = null;
        episodeList.refresh();
        LOG.log(Level.INFO, "Episode list populated: {0} episodes", podcast.getEpisodes().size());
    }

    public void setPlayingEpisode(Episode episode) {
        this.playingEpisode = episode;
        episodeList.refresh();
        if (episode != null) {
            LOG.log(Level.INFO, "Now playing indicator set to: {0}", episode.getTitle());
        }
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
            HBox.setHgrow(titleLabel, Priority.ALWAYS);

            dateLabel = new Label();
            dateLabel.getStyleClass().add("episode-date");
            dateLabel.setMinWidth(80);
            dateLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            durationLabel = new Label();
            durationLabel.getStyleClass().add("episode-duration");
            durationLabel.setMinWidth(60);
            durationLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            HBox topRow = new HBox(12, titleLabel, dateLabel, durationLabel);
            topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            topRow.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(titleLabel, Priority.ALWAYS);

            descLabel = new Label();
            descLabel.getStyleClass().add("episode-desc");
            descLabel.setWrapText(true);
            descLabel.setMaxHeight(18);

            VBox cellContent = new VBox(2, topRow, descLabel);

            row = new HBox(8, playingIndicator, cellContent);
            row.setAlignment(javafx.geometry.Pos.TOP_LEFT);
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
                    String clean = desc.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
                    descLabel.setText(clean.length() > 50 ? clean.substring(0, 50) + "..." : clean);
                    descLabel.setVisible(true);
                    descLabel.setManaged(true);
                } else {
                    descLabel.setVisible(false);
                    descLabel.setManaged(false);
                }

                boolean isPlaying = episode == playingEpisode;
                playingIndicator.setVisible(isPlaying);
                if (isPlaying) {
                    titleLabel.getStyleClass().add("episode-playing");
                } else {
                    titleLabel.getStyleClass().remove("episode-playing");
                }

                setGraphic(row);
            }
        }
    }
}
