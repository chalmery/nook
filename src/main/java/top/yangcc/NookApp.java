package top.yangcc;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import top.yangcc.ui.MainLayout;

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

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        LOG.log(Level.INFO, "AtlantaFX PrimerLight theme applied");

        mainLayout = new MainLayout();
        LOG.log(Level.INFO, "MainLayout created");

        Scene scene = new Scene(mainLayout, 1100, 700);
        String cssUrl = getClass().getResource("/css/app.css").toExternalForm();
        scene.getStylesheets().add(cssUrl);
        LOG.log(Level.INFO, "CSS loaded: {0}", cssUrl);

        stage.setTitle("Nook");
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
        System.setProperty("prism.lcdtext", "true");
        System.setProperty("prism.subpixeltext", "true");
        System.setProperty("prism.forceGPU", "true");
        LOG.log(Level.INFO, "Prism properties set: lcdtext=true, subpixeltext=true, forceGPU=true");
        launch(args);
    }
}
