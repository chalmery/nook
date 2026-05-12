package top.yangcc.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SidebarView extends VBox {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.sidebar");

    public enum NavTarget { DISCOVER, SUBSCRIPTIONS, DOWNLOADS, HISTORY, SETTINGS }

    private final List<Button> navButtons = new ArrayList<>();
    private Consumer<NavTarget> onNavigate;

    public SidebarView() {
        setPrefWidth(180);
        setMinWidth(160);
        setPadding(new Insets(0));
        getStyleClass().add("sidebar");

        Label appTitle = new Label("Nook");
        appTitle.getStyleClass().add("sidebar-title");
        appTitle.setMaxWidth(Double.MAX_VALUE);
        appTitle.setAlignment(Pos.CENTER);
        VBox.setMargin(appTitle, new Insets(16, 12, 16, 12));

        addNavButton("发现", NavTarget.DISCOVER, true);
        Button subsBtn = addNavButton("订阅", NavTarget.SUBSCRIPTIONS, false);
        addNavButton("下载", NavTarget.DOWNLOADS, true);
        addNavButton("历史", NavTarget.HISTORY, true);
        addNavButton("设置", NavTarget.SETTINGS, false);

        // spacer to push buttons to top
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        getChildren().add(appTitle);
        getChildren().addAll(navButtons);
        getChildren().add(spacer);

        // default selected
        selectButton(subsBtn);
    }

    private Button addNavButton(String text, NavTarget target, boolean disabled) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.getStyleClass().add("sidebar-nav-btn");
        btn.setDisable(disabled);
        btn.setOnAction(e -> {
            LOG.log(Level.INFO, "Sidebar nav: {0}", text);
            selectButton(btn);
            if (onNavigate != null) onNavigate.accept(target);
        });
        navButtons.add(btn);
        return btn;
    }

    private void selectButton(Button selected) {
        for (Button btn : navButtons) {
            btn.getStyleClass().remove("sidebar-nav-selected");
        }
        selected.getStyleClass().add("sidebar-nav-selected");
    }

    public void setOnNavigate(Consumer<NavTarget> handler) { this.onNavigate = handler; }
}
