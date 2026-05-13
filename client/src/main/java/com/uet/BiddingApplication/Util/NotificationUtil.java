package com.uet.BiddingApplication.Util;

import atlantafx.base.theme.CupertinoDark;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class NotificationUtil {
    private static final Logger log = LoggerFactory.getLogger(NotificationUtil.class);

    // CSS filepath
    private static final String CUSTOM_CSS_PATH = "/app/css/brand.css";

    //Apply css method
    private static void applyStyles(DialogPane dialogPane) {
        try {
            // Nạp Cupertino Dark
            String cupertinoDarkCss = new CupertinoDark().getUserAgentStylesheet();
            // Nạp CSS riêng
            String brandCss = Objects.requireNonNull(NotificationUtil.class.getResource(CUSTOM_CSS_PATH)).toExternalForm();

            // Ép cả 2 vào cửa sổ
            dialogPane.getStylesheets().addAll(cupertinoDarkCss, brandCss);

            // Giúp màu nền Alert đồng bộ với theme Dark
            dialogPane.getStyleClass().add("background");

        } catch (NullPointerException e) {
            log.error("[NotificationUtil] Không tìm thấy file CSS cho Alert tại đường dẫn: {}", CUSTOM_CSS_PATH, e);
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
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        // --- GỌI HÀM ÁP DỤNG CSS CHO CẢ CONFIRMATION ---
        applyStyles(alert.getDialogPane());

        // showAndWait() returns an Optional containing the button the user clicked
        Optional<ButtonType> result = alert.showAndWait();

        // Return true ONLY if the user clicked OK
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}