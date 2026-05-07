package com.uet.BiddingApplication.Util;

import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

public class UIUtil {
    public static void roundedImageView(ImageView imageView, double radius) {
        // Tạo một hình chữ nhật bo góc làm "khuôn" (clip)
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius);
        clip.setArcHeight(radius);

        // Ràng buộc kích thước khuôn theo kích thước ImageView
        clip.widthProperty().bind(imageView.fitWidthProperty());
        clip.heightProperty().bind(imageView.fitHeightProperty());

        // Áp dụng khuôn lên ImageView
        imageView.setClip(clip);
    }
    public static void roundedImageView(ImageView img){roundedImageView(img,24);}
}