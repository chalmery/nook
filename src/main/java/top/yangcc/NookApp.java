package top.yangcc;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import top.yangcc.ui.MainLayout;
import top.yangcc.ui.ThemeManager;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class NookApp extends Application {

    private static final Logger LOG = System.getLogger("top.yangcc.app");

    private MainLayout mainLayout;

    @Override
    public void start(Stage stage) {
        LOG.log(Level.INFO, "Nook starting...");
        LOG.log(Level.INFO, "Java version: {0}", System.getProperty("java.version"));
        LOG.log(Level.INFO, "JavaFX version: {0}", System.getProperty("javafx.version"));

        ThemeManager tm = ThemeManager.getInstance();
        LOG.log(Level.INFO, "ThemeManager initialized, theme: {0}", tm.getCurrentTheme().getName());

        mainLayout = new MainLayout();
        LOG.log(Level.INFO, "MainLayout created");

        Scene scene = new Scene(mainLayout, 1100, 700);
        String cssUrl = getClass().getResource("/css/app.css").toExternalForm();
        scene.getStylesheets().add(cssUrl);
        LOG.log(Level.INFO, "CSS loaded: {0}", cssUrl);

        stage.setTitle("Nook");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon/nook-256x256.png")));
        stage.setScene(scene);
        stage.show();
        LOG.log(Level.INFO, "Stage shown");

        stage.setOnCloseRequest(e -> {
            LOG.log(Level.INFO, "Window close requested, shutting down");
            mainLayout.shutdown();
        });
    }

    @Override
    public void stop() {
        LOG.log(Level.INFO, "Application stopping");
        if (mainLayout != null) {
            mainLayout.shutdown();
        }
    }

    public static void main(String[] args) {
        setupInputMethod();
        System.setProperty("prism.lcdtext", "true");
        System.setProperty("prism.subpixeltext", "true");
        System.setProperty("prism.forceGPU", "true");
        LOG.log(Level.INFO, "Prism properties set: lcdtext=true, subpixeltext=true, forceGPU=true");
        launch(args);
    }

    private static void setupInputMethod() {
        if (System.getProperty("im.module") != null) return;
        String module = detectInputMethod();
        if (module != null) {
            System.setProperty("im.module", module);
            LOG.log(Level.INFO, "Input method auto-detected: {0}", module);
        }
    }

    private static String detectInputMethod() {
        String gtkIM = System.getenv("GTK_IM_MODULE");
        String qtIM = System.getenv("QT_IM_MODULE");
        String xmod = System.getenv("XMODIFIERS");

        if (containsIgnoreCase(gtkIM, "fcitx") || containsIgnoreCase(qtIM, "fcitx") || containsIgnoreCase(xmod, "fcitx"))
            return "fcitx";
        if (containsIgnoreCase(gtkIM, "ibus") || containsIgnoreCase(qtIM, "ibus") || containsIgnoreCase(xmod, "ibus"))
            return "ibus";

        try {
            if (ProcessHandle.allProcesses().anyMatch(p -> p.info().command().orElse("").contains("fcitx5")))
                return "fcitx";
            if (ProcessHandle.allProcesses().anyMatch(p -> p.info().command().orElse("").contains("ibus-daemon")))
                return "ibus";
        } catch (Exception ignore) { }

        return null;
    }

    private static boolean containsIgnoreCase(String s, String sub) {
        return s != null && s.toLowerCase().contains(sub);
    }
}
