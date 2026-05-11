package top.yangcc.ui;

import javafx.geometry.Insets;
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

    private final Label podcastTitle;
    private final Label podcastAuthor;
    private final ListView<Episode> episodeList;
    private Episode playingEpisode;
    private Consumer<Episode> onEpisodeSelected;
    private Consumer<Episode> onPlayRequested;

    public EpisodeListView() {
        setPadding(new Insets(0));
        getStyleClass().add("episode-list-view");

        podcastTitle = new Label("选择一个播客");
        podcastTitle.getStyleClass().add("podcast-title");
        VBox.setMargin(podcastTitle, new Insets(12, 12, 0, 12));

        podcastAuthor = new Label("");
        podcastAuthor.getStyleClass().add("podcast-author");
        VBox.setMargin(podcastAuthor, new Insets(0, 12, 8, 12));

        Separator sep = new Separator();

        episodeList = new ListView<>();
        episodeList.setPlaceholder(new Label("  暂无单集"));
        episodeList.setCellFactory(listView -> new EpisodeCell());
        episodeList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (onEpisodeSelected != null && newVal != null) {
                        onEpisodeSelected.accept(newVal);
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

        getChildren().addAll(podcastTitle, podcastAuthor, sep, episodeList);
    }

    public void setPodcast(Podcast podcast) {
        if (podcast == null) {
            podcastTitle.setText("选择一个播客");
            podcastAuthor.setText("");
            episodeList.getItems().clear();
            return;
        }
        podcastTitle.setText(podcast.getTitle());
        podcastAuthor.setText(podcast.getAuthor() != null ? podcast.getAuthor() : "");
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

    public void setOnEpisodeSelected(Consumer<Episode> handler) { this.onEpisodeSelected = handler; }
    public void setOnPlayRequested(Consumer<Episode> handler) { this.onPlayRequested = handler; }

    private class EpisodeCell extends ListCell<Episode> {
        private final VBox content;
        private final Label titleLabel;
        private final Label metaLabel;
        private final Label playingIndicator;
        private final HBox row;

        EpisodeCell() {
            content = new VBox(2);
            titleLabel = new Label();
            titleLabel.getStyleClass().add("episode-title");
            titleLabel.setWrapText(true);
            metaLabel = new Label();
            metaLabel.getStyleClass().add("episode-meta");
            content.getChildren().addAll(titleLabel, metaLabel);
            playingIndicator = new Label("▸");
            playingIndicator.getStyleClass().add("playing-indicator");
            playingIndicator.setVisible(false);
            row = new HBox(6, playingIndicator, content);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            HBox.setHgrow(content, Priority.ALWAYS);
        }

        @Override
        protected void updateItem(Episode episode, boolean empty) {
            super.updateItem(episode, empty);
            if (empty || episode == null) {
                setGraphic(null);
            } else {
                titleLabel.setText(episode.getTitle());
                StringBuilder meta = new StringBuilder();
                if (episode.getPubDate() != null) {
                    meta.append(new SimpleDateFormat("yyyy-MM-dd").format(episode.getPubDate()));
                }
                if (episode.getDuration() != null && !episode.getDuration().isEmpty()) {
                    meta.append("  •  ").append(episode.getDuration());
                }
                metaLabel.setText(meta.toString());

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
