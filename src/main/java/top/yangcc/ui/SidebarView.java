package top.yangcc.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import top.yangcc.model.Podcast;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Consumer;

public class SidebarView extends VBox {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.sidebar");

    private final ListView<Podcast> subscriptionList;
    private Consumer<Podcast> onPodcastSelected;
    private Runnable onAddSubscription;
    private Runnable onOpenSettings;

    public SidebarView() {
        setPrefWidth(200);
        setMinWidth(180);
        setPadding(new Insets(0));
        getStyleClass().add("sidebar");

        Label appTitle = new Label("Nook");
        appTitle.getStyleClass().add("sidebar-title");
        VBox.setMargin(appTitle, new Insets(12, 12, 8, 12));

        Button discoverBtn = createNavButton("发现");
        discoverBtn.setDisable(true);

        Separator sep1 = new Separator();

        Label subHeader = new Label("订阅列表");
        subHeader.getStyleClass().add("sidebar-section-header");

        Button addBtn = new Button("+");
        addBtn.getStyleClass().addAll("sidebar-add-btn", "accent");
        addBtn.setOnAction(e -> {
            LOG.log(Level.INFO, "Add subscription button clicked");
            if (onAddSubscription != null) onAddSubscription.run();
        });
        addBtn.setTooltip(new Tooltip("添加订阅"));

        subscriptionList = new ListView<>();
        subscriptionList.setPlaceholder(new Label("  暂无订阅\n  点击 + 添加"));
        subscriptionList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Podcast podcast, boolean empty) {
                super.updateItem(podcast, empty);
                if (empty || podcast == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(podcast.getTitle());
                }
            }
        });
        subscriptionList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (onPodcastSelected != null && newVal != null) {
                        LOG.log(Level.INFO, "Sidebar selection changed to: {0}", newVal.getTitle());
                        onPodcastSelected.accept(newVal);
                    }
                });
        VBox.setVgrow(subscriptionList, Priority.ALWAYS);

        Button downloadBtn = createNavButton("下载");
        downloadBtn.setDisable(true);
        Button historyBtn = createNavButton("历史");
        historyBtn.setDisable(true);
        Button settingsBtn = createNavButton("设置");
        settingsBtn.setOnAction(e -> {
            LOG.log(Level.INFO, "Settings button clicked");
            if (onOpenSettings != null) onOpenSettings.run();
        });

        Separator sep2 = new Separator();

        getChildren().addAll(
                appTitle, discoverBtn, sep1,
                subHeader, addBtn, subscriptionList, sep2,
                downloadBtn, historyBtn, settingsBtn
        );
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.getStyleClass().add("sidebar-nav-btn");
        return btn;
    }

    public void setOnPodcastSelected(Consumer<Podcast> handler) { this.onPodcastSelected = handler; }
    public void setOnAddSubscription(Runnable handler) { this.onAddSubscription = handler; }
    public void setOnOpenSettings(Runnable handler) { this.onOpenSettings = handler; }
    public ListView<Podcast> getSubscriptionList() { return subscriptionList; }

    public void selectPodcast(Podcast podcast) {
        subscriptionList.getSelectionModel().select(podcast);
    }
}
