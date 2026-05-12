package top.yangcc.ui;

import atlantafx.base.theme.Theme;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class SettingsDialog {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.settings");

    private SettingsDialog() {}

    public static void show(Stage owner) {
        Stage stage = new Stage();
        stage.setTitle("设置");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setWidth(700);
        stage.setHeight(480);

        ThemeManager tm = ThemeManager.getInstance();

        // --- left: category list ---
        ListView<String> categoryList = new ListView<>();
        categoryList.setItems(FXCollections.observableArrayList("基础设置"));
        categoryList.getSelectionModel().selectFirst();
        categoryList.setPrefWidth(160);
        categoryList.getStyleClass().add("settings-category-list");

        // --- right: settings panel ---
        VBox basicPanel = buildBasicPanel(tm);

        BorderPane root = new BorderPane();
        root.setLeft(categoryList);
        root.setCenter(basicPanel);
        BorderPane.setMargin(basicPanel, new Insets(20, 20, 20, 20));

        categoryList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if ("基础设置".equals(newVal)) {
                root.setCenter(basicPanel);
                BorderPane.setMargin(basicPanel, new Insets(20, 20, 20, 20));
            }
        });

        Scene scene = new Scene(root);
        String cssUrl = SettingsDialog.class.getResource("/css/app.css").toExternalForm();
        scene.getStylesheets().add(cssUrl);

        stage.setScene(scene);
        LOG.log(Level.INFO, "Settings dialog opened");
        stage.showAndWait();
    }

    private static VBox buildBasicPanel(ThemeManager tm) {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(0));
        panel.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("基础设置");
        title.getStyleClass().add("settings-section-title");

        // --- follow system toggle ---
        CheckBox followSystemToggle = new CheckBox("跟随系统浅色深色设置");
        followSystemToggle.setSelected(tm.isFollowSystem());
        followSystemToggle.getStyleClass().add("settings-toggle");

        Label followDesc = new Label("开启后，Nook 将跟随操作系统的浅色/深色模式自动切换主题");
        followDesc.getStyleClass().add("settings-desc");

        // --- light theme combo ---
        Label lightLabel = new Label("浅色主题");
        lightLabel.getStyleClass().add("settings-label");
        ComboBox<String> lightCombo = new ComboBox<>();
        for (Theme t : tm.getLightThemes()) {
            lightCombo.getItems().add(t.getName());
        }
        lightCombo.setValue(tm.getLightThemeName());
        lightCombo.setPrefWidth(220);

        // --- dark theme combo ---
        Label darkLabel = new Label("深色主题");
        darkLabel.getStyleClass().add("settings-label");
        ComboBox<String> darkCombo = new ComboBox<>();
        for (Theme t : tm.getDarkThemes()) {
            darkCombo.getItems().add(t.getName());
        }
        darkCombo.setValue(tm.getDarkThemeName());
        darkCombo.setPrefWidth(220);

        // Disable combos when follow system is on
        lightCombo.setDisable(tm.isFollowSystem());
        darkCombo.setDisable(tm.isFollowSystem());

        followSystemToggle.setOnAction(e -> {
            boolean follow = followSystemToggle.isSelected();
            lightCombo.setDisable(follow);
            darkCombo.setDisable(follow);
            tm.setFollowSystem(follow);
        });

        lightCombo.setOnAction(e -> {
            if (!followSystemToggle.isSelected()) {
                tm.setLightThemeName(lightCombo.getValue());
            }
        });

        darkCombo.setOnAction(e -> {
            if (!followSystemToggle.isSelected()) {
                tm.setDarkThemeName(darkCombo.getValue());
            }
        });

        Separator sep = new Separator();
        sep.setMaxWidth(400);

        panel.getChildren().addAll(
                title,
                followSystemToggle, followDesc,
                sep,
                lightLabel, lightCombo,
                darkLabel, darkCombo
        );

        return panel;
    }
}
