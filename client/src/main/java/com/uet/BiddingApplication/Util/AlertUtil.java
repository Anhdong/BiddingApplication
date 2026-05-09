package com.uet.BiddingApplication.Util;

import javafx.scene.control.Alert;

public class AlertUtil {
    public static void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.NONE); // Thay đổi Type tùy ý
        alert.setTitle(title);
        alert.setHeaderText(null); // Để null nếu không muốn có dòng tiêu đề phụ
        alert.setContentText(content);
        alert.showAndWait(); // Hiển thị và dừng các tương tác khác cho đến khi nhấn OK
    }

    public static void showAlert(String content){
        showAlert("Error!",content);
    }
}
