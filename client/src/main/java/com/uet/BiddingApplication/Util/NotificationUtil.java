package com.uet.BiddingApplication.Util;

import com.uet.BiddingApplication.Controller.CommonController.InputDialogController;
import com.uet.BiddingApplication.Controller.CommonController.ToastController;
import com.uet.BiddingApplication.Enum.ViewPath;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
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

    private static final int TOAST_DISPLAY_TIME_MS = 3000;
    private static final double TOAST_MARGIN = 20.0;
    private static final java.util.List<javafx.stage.Stage> activeToasts = new java.util.ArrayList<>();

    // ==========================================
    // TOAST NOTIFICATION METHOD
    // ==========================================
    private static void showToast(String title, String content, String iconLiteral, String colorClass) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(NotificationUtil.class.getResource(ViewPath.TOAST.getPath()));
                HBox toastNode = loader.load();
                ToastController controller = loader.getController();
                controller.setNotification(title, content, iconLiteral, colorClass);

                try {
                    toastNode.getStylesheets().add(ThemeManager.getCurrentBrandCssPath());
                } catch (Exception e) {}

                Scene scene = new Scene(toastNode);
                scene.setFill(Color.TRANSPARENT);

                Stage stage = new Stage();
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.setAlwaysOnTop(true);
                stage.setScene(scene);

                activeToasts.add(stage);

                // Show temporarily to calculate width & height
                stage.setOpacity(0);
                stage.show();

                Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                double x = bounds.getMaxX() - stage.getWidth() - TOAST_MARGIN;
                
                double yOffset = 0;
                for (int i = 0; i < activeToasts.size() - 1; i++) {
                    yOffset += activeToasts.get(i).getHeight() + 10;
                }
                double y = bounds.getMaxY() - stage.getHeight() - TOAST_MARGIN - yOffset;
                
                stage.setX(x);
                stage.setY(y);

                // Fade in
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastNode);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);

                // Fade out
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toastNode);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setDelay(Duration.millis(TOAST_DISPLAY_TIME_MS));

                fadeOut.setOnFinished(e -> {
                    stage.close();
                    activeToasts.remove(stage);
                });

                stage.setOpacity(1.0);
                fadeIn.play();
                fadeIn.setOnFinished(e -> fadeOut.play());

            } catch (Exception e) {
                log.error("[NotificationUtil] Lỗi khi tạo Toast", e);
            }
        });
    }

    // ==========================================
    // ERROR NOTIFICATIONS
    // ==========================================
    public static void showError(String title, String content) {
        log.error("Showing Error Toast: {} - {}", title, content);
        showToast(title, content, "mdi2a-alert-circle", "danger");
    }

    public static void showError(String content) {
        showError("Error!", content);
    }

    // ==========================================
    // INFORMATION NOTIFICATIONS
    // ==========================================
    public static void showInfo(String title, String content) {
        log.info("Showing Info Toast: {} - {}", title, content);
        showToast(title, content, "mdi2i-information", "accent");
    }

    public static void showInfo(String content) {
        showInfo("Information", content);
    }

    // ==========================================
    // NOTIFICATION ALERT
    // ==========================================
    public static void showAlert(String title, String content) {
        Platform.runLater(()->{
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            applyStyles(alert.getDialogPane());
            alert.showAndWait();
        });
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