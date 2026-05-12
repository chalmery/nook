package top.yangcc.ui;

import atlantafx.base.theme.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import javafx.application.Application;

import java.io.*;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ThemeManager {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.theme");
    private static final Path SETTINGS_FILE = Paths.get(System.getProperty("user.home"), ".nook", "theme.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ThemeManager instance;

    private final List<Theme> lightThemes = List.of(
            new PrimerLight(),
            new CupertinoLight(),
            new NordLight(),
            new PrimerDark(),
            new CupertinoDark(),
            new NordDark(),
            new Dracula()
    );

    private final List<Theme> darkThemes = List.of(
            new PrimerLight(),
            new CupertinoLight(),
            new NordLight(),
            new PrimerDark(),
            new CupertinoDark(),
            new NordDark(),
            new Dracula()
    );

    private ThemeConfig config;
    private Theme currentTheme;
    private ScheduledFuture<?> watcherFuture;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "theme-watcher");
        t.setDaemon(true);
        return t;
    });

    private ThemeManager() {
        loadConfig();
        if (config.followSystem) {
            applySystemTheme();
        } else {
            applyConfiguredTheme();
        }
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    // --- public API ---

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public List<Theme> getLightThemes() {
        return lightThemes;
    }

    public List<Theme> getDarkThemes() {
        return darkThemes;
    }

    public boolean isFollowSystem() {
        return config.followSystem;
    }

    public void setFollowSystem(boolean follow) {
        config.followSystem = follow;
        saveConfig();
        if (follow) {
            startWatchingSystemTheme();
            applySystemTheme();
        } else {
            stopWatchingSystemTheme();
            applyConfiguredTheme();
        }
    }

    public String getLightThemeName() {
        return config.lightThemeName;
    }

    public void setLightThemeName(String name) {
        config.lightThemeName = name;
        saveConfig();
        if (!config.followSystem && !isSystemDarkMode()) {
            applyConfiguredTheme();
        }
    }

    public String getDarkThemeName() {
        return config.darkThemeName;
    }

    public void setDarkThemeName(String name) {
        config.darkThemeName = name;
        saveConfig();
        if (!config.followSystem && isSystemDarkMode()) {
            applyConfiguredTheme();
        }
    }

    public Theme findThemeByName(String name) {
        for (Theme t : lightThemes) {
            if (t.getName().equals(name)) return t;
        }
        for (Theme t : darkThemes) {
            if (t.getName().equals(name)) return t;
        }
        return null;
    }

    // --- internal ---

    private void applySystemTheme() {
        Theme theme = isSystemDarkMode()
                ? findThemeByName(config.darkThemeName)
                : findThemeByName(config.lightThemeName);
        if (theme == null) theme = new PrimerLight();
        applyTheme(theme);
    }

    private void applyConfiguredTheme() {
        Theme theme;
        if (isSystemDarkMode()) {
            theme = findThemeByName(config.darkThemeName);
        } else {
            theme = findThemeByName(config.lightThemeName);
        }
        if (theme == null && !config.lightThemeName.isEmpty()) {
            theme = findThemeByName(config.lightThemeName);
        }
        if (theme == null) theme = new PrimerLight();
        applyTheme(theme);
    }

    private void applyTheme(Theme theme) {
        if (currentTheme != null && currentTheme.getName().equals(theme.getName())) return;
        currentTheme = theme;
        Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
        LOG.log(Level.INFO, "Theme applied: {0} (dark={1})", theme.getName(), theme.isDarkMode());
    }

    // --- OS dark mode detection ---

    public boolean isSystemDarkMode() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            return isLinuxDarkMode();
        } else if (os.contains("mac")) {
            return isMacDarkMode();
        } else if (os.contains("win")) {
            return isWindowsDarkMode();
        }
        return false;
    }

    private static boolean isLinuxDarkMode() {
        // Try GNOME / GTK color-scheme setting
        try {
            Process p = new ProcessBuilder(
                    "gsettings", "get",
                    "org.gnome.desktop.interface", "color-scheme")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(p.getInputStream().readAllBytes()).trim();
            int rc = p.waitFor();
            if (rc == 0 && output.contains("dark")) {
                return true;
            }
        } catch (Exception ignored) { /* not available */ }

        // Fallback: check if GTK theme name contains "dark"
        try {
            Process p = new ProcessBuilder(
                    "gsettings", "get",
                    "org.gnome.desktop.interface", "gtk-theme")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(p.getInputStream().readAllBytes()).trim().toLowerCase();
            int rc = p.waitFor();
            if (rc == 0 && output.contains("dark")) {
                return true;
            }
        } catch (Exception ignored) { /* not available */ }

        return false;
    }

    private static boolean isMacDarkMode() {
        try {
            Process p = new ProcessBuilder(
                    "defaults", "read", "-g", "AppleInterfaceStyle")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(p.getInputStream().readAllBytes()).trim();
            int rc = p.waitFor();
            if (rc == 0 && output.equalsIgnoreCase("dark")) {
                return true;
            }
        } catch (Exception ignored) { /* not available */ }
        return false;
    }

    private static boolean isWindowsDarkMode() {
        try {
            Process p = new ProcessBuilder(
                    "reg", "query",
                    "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(p.getInputStream().readAllBytes()).trim();
            int rc = p.waitFor();
            if (rc == 0 && output.contains("0x0")) {
                return true;
            }
        } catch (Exception ignored) { /* not available */ }
        return false;
    }

    // --- system theme watching (polling) ---

    private void startWatchingSystemTheme() {
        if (watcherFuture != null && !watcherFuture.isDone()) {
            return; // already watching
        }
        LOG.log(Level.INFO, "Starting system theme watcher (polling every 2s)");
        watcherFuture = scheduler.scheduleWithFixedDelay(() -> {
            try {
                boolean systemDark = isSystemDarkMode();
                boolean themeDark = currentTheme != null && currentTheme.isDarkMode();
                if (systemDark != themeDark) {
                    LOG.log(Level.INFO, "System theme changed (systemDark={0}, themeDark={1}), switching...",
                            systemDark, themeDark);
                    javafx.application.Platform.runLater(this::applySystemTheme);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Theme watcher error", e);
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    private void stopWatchingSystemTheme() {
        if (watcherFuture != null) {
            watcherFuture.cancel(false);
            watcherFuture = null;
            LOG.log(Level.INFO, "System theme watcher stopped");
        }
    }

    // --- persistence ---

    private void loadConfig() {
        if (Files.exists(SETTINGS_FILE)) {
            try (Reader r = Files.newBufferedReader(SETTINGS_FILE)) {
                config = GSON.fromJson(r, ThemeConfig.class);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to load theme config, using defaults", e);
            }
        }
        if (config == null) {
            config = new ThemeConfig();
        }
    }

    private void saveConfig() {
        try {
            Files.createDirectories(SETTINGS_FILE.getParent());
            try (Writer w = Files.newBufferedWriter(SETTINGS_FILE)) {
                GSON.toJson(config, w);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save theme config", e);
        }
    }

    // --- config POJO ---

    static class ThemeConfig {
        @Expose boolean followSystem;
        @Expose String lightThemeName = "PrimerLight";
        @Expose String darkThemeName = "PrimerDark";

        ThemeConfig() {}
    }
}
