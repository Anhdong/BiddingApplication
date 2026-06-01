package com.uet.BiddingApplication.Util;

import com.uet.BiddingApplication.Controller.CommonController.InputDialogController;
import com.uet.BiddingApplication.Enum.ViewPath;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
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
    public static boolean showConfirmation(String title, String content){
        // Return true ONLY if the user clicked OK
        return Boolean.TRUE.equals(AppExecutor.getResultFromUI(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            
            // LƯU Ý: Không dùng Platform.runLater ở đây vì getResultFromUI 
            // đã đảm bảo code bên trong chạy trên luồng UI rồi.
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            applyStyles(alert.getDialogPane());

            // Check if result is present AND is ButtonType.OK
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        }));
    }

    // ==========================================
    // INPUT DIALOG (Returns String or null)
    // ==========================================
    public static String showInputField(String title, String iconLiteral){
        return AppExecutor.getResultFromUI(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(NotificationUtil.class.getResource(ViewPath.INPUT_DIALOG.getPath()));
                DialogPane dialogPane = loader.load();
                InputDialogController controller = loader.getController();

                controller.setupInterface(title, iconLiteral);
                applyStyles(dialogPane);

                //Create a new Dialog that returns a String
                Dialog<String> dialog = new Dialog<>();
                dialog.setDialogPane(dialogPane);

                dialog.setResultConverter(dialogButton -> {
                    if (dialogButton == ButtonType.OK) {
                        return controller.getInputValue(); // Gọi hàm ta vừa thêm ở Controller
                    }
                    return null; // Trả về null nếu người dùng bấm Cancel hoặc tắt cửa sổ
                });

                // CÁCH AN TOÀN ĐỂ TRẢ VỀ:
                // Nếu người dùng bấm Cancel, orElse(null) sẽ trả về null.
                // Không dùng Objects.requireNonNull vì sẽ gây crash NullPointerException khi bấm Cancel.
                return dialog.showAndWait().orElse(null);
            } catch (Exception e) {
                log.error("[NotificationUtil] Error loading input dialog", e);
                return null;
            }
        });
    }
}