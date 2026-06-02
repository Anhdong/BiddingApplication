package com.uet.BiddingApplication.Util;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.scene.Scene;

import java.util.Objects;

public class ThemeManager {
    public enum Theme { LIGHT, DARK }

    private static Theme currentTheme = Theme.DARK;
    private static Scene mainScene;

    public static void setMainScene(Scene scene) {
        mainScene = scene;
    }

    public static void setTheme(Theme theme) {
        currentTheme = theme;

        // 1. Cập nhật User Agent Stylesheet (Global)
        if (theme == Theme.DARK) {
            Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        }

        // 2. Cập nhật CSS cho cửa sổ chính (nếu đã được set)
        if (mainScene != null) {
            String oldCss = getBrandCssPath(theme == Theme.DARK ? Theme.LIGHT : Theme.DARK);
            String newCss = getBrandCssPath(theme);
            
            // Xóa cái cũ và thêm cái mới nếu chưa có
            mainScene.getStylesheets().remove(oldCss);
            if (!mainScene.getStylesheets().contains(newCss)) {
                mainScene.getStylesheets().add(newCss);
            }
        }
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static String getCurrentBrandCssPath() {
        return getBrandCssPath(currentTheme);
    }

    private static String getBrandCssPath(Theme theme) {
        String path = theme == Theme.DARK ? "/app/css/brand-dark.css" : "/app/css/brand-light.css";
        return Objects.requireNonNull(ThemeManager.class.getResource(path)).toExternalForm();
    }
}
