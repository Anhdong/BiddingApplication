package com.uet.BiddingApplication.Util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class NotificationUtil {
    private static final Logger log = LoggerFactory.getLogger(NotificationUtil.class);

    //Apply css method
    private static void applyStyles(DialogPane dialogPane) {
        try {
            // Chỉ nạp CSS Brand tương ứng với Theme hiện tại
            String currentBrandCss = ThemeManager.getCurrentBrandCssPath();

            // Ép vào cửa sổ
            dialogPane.getStylesheets().add(currentBrandCss);

            // Giúp màu nền Alert đồng bộ với theme
            dialogPane.getStyleClass().add("background");

        } catch (Exception e) {
            log.error("[NotificationUtil] Lỗi khi nạp CSS cho Alert", e);
        }
    }

    // ==========================================
    // CORE ALERT METHOD
    // ==========================================
    public static void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(()->{
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);

            // --- GỌI HÀM ÁP DỤNG CSS ---
            applyStyles(alert.getDialogPane());

            alert.showAndWait();
        });
    }

    // ==========================================
    // ERROR NOTIFICATIONS
    // ==========================================
    public static void showError(String title, String content) {
        log.error("Showing Error Alert: {} - {}", title, content);
        showAlert(Alert.AlertType.ERROR, title, content);
    }

    public static void showError(String content) {
        showError("Error!", content);
    }

    // ==========================================
    // INFORMATION NOTIFICATIONS
    // ==========================================
    public static void showInfo(String title, String content) {
        log.info("Showing Info Alert: {} - {}", title, content);
        showAlert(Alert.AlertType.INFORMATION, title, content);
    }

    public static void showInfo(String content) {
        showInfo("Information", content);
    }

    // ==========================================
    // CONFIRMATION DIALOG (Returns true/false)
    // ==========================================
    public static boolean showConfirmation(String title, String content) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            Platform.runLater(()->{
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            applyStyles(alert.getDialogPane());
            });

        // showAndWait() returns an Optional containing the button the user clicked
        Optional<ButtonType> result = AppExecutor.getResultFromUI(alert::showAndWait);

        // Return true ONLY if the user clicked OK
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}