package com.uet.BiddingApplication.Util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class NotificationUtil {
    private static final Logger log = LoggerFactory.getLogger(NotificationUtil.class);

    /**
     * Core method that builds and shows the alert based on the requested type.
     */
    public static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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
    // WARNING NOTIFICATIONS
    // ==========================================
    public static void showWarning(String title, String content) {
        log.warn("Showing Warning Alert: {} - {}", title, content);
        showAlert(Alert.AlertType.WARNING, title, content);
    }

    // ==========================================
    // CONFIRMATION DIALOG (Returns true/false)
    // ==========================================
    public static boolean showConfirmation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        // showAndWait() returns an Optional containing the button the user clicked
        Optional<ButtonType> result = alert.showAndWait();

        // Return true ONLY if the user clicked OK
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}