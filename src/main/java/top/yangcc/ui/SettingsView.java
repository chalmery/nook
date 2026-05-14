package top.yangcc.ui;

import atlantafx.base.theme.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsView extends BorderPane {

    public SettingsView() {
        getStyleClass().add("settings-view");

        ThemeManager tm = ThemeManager.getInstance();

        Label header = new Label("设置");
        header.getStyleClass().add("settings-header");
        header.setPadding(new Insets(20, 24, 12, 24));

        VBox panel = new VBox(16);
        panel.setPadding(new Insets(8, 24, 20, 24));
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setMaxWidth(500);

        // --- follow system toggle ---
        CheckBox followSystemToggle = new CheckBox("跟随系统浅色深色设置");
        followSystemToggle.setSelected(tm.isFollowSystem());
        followSystemToggle.getStyleClass().add("settings-toggle");

        Label followDesc = new Label("开启后，Nook 将跟随操作系统的浅色/深色模式自动切换主题");
        followDesc.getStyleClass().add("settings-desc");
        followDesc.setWrapText(true);

        Separator sep1 = new Separator();
        sep1.setMaxWidth(400);

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

        // disable combos when follow system is on
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

        Separator sep2 = new Separator();
        sep2.setMaxWidth(400);

        panel.getChildren().addAll(
                followSystemToggle, followDesc,
                sep1,
                lightLabel, lightCombo,
                darkLabel, darkCombo,
                sep2
        );

        setTop(header);
        setCenter(panel);
    }
}
