package top.yangcc.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import top.yangcc.model.Episode;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class DetailView extends VBox {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.detail");

    private final Label titleLabel;
    private final TextArea showNotes;
    private final Label chaptersPlaceholder;
    private Runnable onPlayRequested;

    public DetailView() {
        setPrefWidth(300);
        setMinWidth(250);
        setPadding(new Insets(0));
        getStyleClass().add("detail-view");

        titleLabel = new Label("单集详情");
        titleLabel.getStyleClass().add("detail-title");
        titleLabel.setWrapText(true);
        VBox.setMargin(titleLabel, new Insets(12, 12, 8, 12));

        Separator sep1 = new Separator();

        Label notesHeader = new Label("Show Notes");
        notesHeader.getStyleClass().add("detail-section-header");
        VBox.setMargin(notesHeader, new Insets(8, 12, 4, 12));

        showNotes = new TextArea();
        showNotes.setEditable(false);
        showNotes.setWrapText(true);
        showNotes.getStyleClass().add("show-notes");
        showNotes.setText("选择一个单集查看详情");
        VBox.setVgrow(showNotes, Priority.ALWAYS);

        Label chaptersHeader = new Label("章节");
        chaptersHeader.getStyleClass().add("detail-section-header");
        VBox.setMargin(chaptersHeader, new Insets(8, 12, 4, 12));

        chaptersPlaceholder = new Label("  暂无章节信息");
        chaptersPlaceholder.getStyleClass().add("detail-placeholder");
        VBox.setMargin(chaptersPlaceholder, new Insets(0, 12, 12, 12));

        Button playBtn = new Button("▶  播放");
        playBtn.getStyleClass().add("detail-play-btn");
        playBtn.setMaxWidth(Double.MAX_VALUE);
        playBtn.setOnAction(e -> {
            LOG.log(Level.INFO, "Detail play button clicked");
            if (onPlayRequested != null) {
                onPlayRequested.run();
            } else {
                LOG.log(Level.WARNING, "onPlayRequested handler is null");
            }
        });
        VBox.setMargin(playBtn, new Insets(8, 12, 12, 12));

        getChildren().addAll(titleLabel, sep1, notesHeader, showNotes,
                chaptersHeader, chaptersPlaceholder, playBtn);
    }

    public void setEpisode(Episode episode) {
        if (episode == null) {
            titleLabel.setText("单集详情");
            showNotes.setText("选择一个单集查看详情");
            return;
        }
        LOG.log(Level.INFO, "Detail view updated: {0}", episode.getTitle());
        titleLabel.setText(episode.getTitle());
        String desc = episode.getDescription();
        if (desc != null && !desc.isEmpty()) {
            showNotes.setText(desc.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim());
        } else {
            showNotes.setText("暂无 Show Notes");
        }
    }

    public void setOnPlayRequested(Runnable handler) { this.onPlayRequested = handler; }
}
