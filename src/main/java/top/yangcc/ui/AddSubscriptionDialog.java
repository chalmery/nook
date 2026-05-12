package top.yangcc.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import top.yangcc.service.SubscriptionManager;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Optional;

public class AddSubscriptionDialog {

    private static final Logger LOG = System.getLogger("top.yangcc.ui.dialog");

    public static void show(SubscriptionManager manager, Runnable onSuccess) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("添加订阅");
        dialog.setHeaderText("输入播客 RSS 地址");
        dialog.setContentText("RSS URL:");
        dialog.setGraphic(null);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(url -> {
            if (!url.isBlank()) {
                LOG.log(Level.INFO, "RSS URL submitted: {0}", url.trim());
                try {
                    manager.addSubscription(url.trim());
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    LOG.log(Level.ERROR, "Failed to add subscription: " + ex.getMessage(), ex);
                    showError("无法添加订阅", "请检查 RSS 地址是否正确\n" + ex.getMessage());
                }
            }
        });
    }

    private static void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
