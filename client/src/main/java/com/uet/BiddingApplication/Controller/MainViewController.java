package com.uet.BiddingApplication.Controller;

import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Interface.ViewControllerLifecycle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class MainViewController implements Initializable {

    // --- SINGLETON ---
    private static MainViewController instance;

    public static MainViewController getInstance() {
        return instance;
    }

    @FXML private StackPane SidebarSlot;
    @FXML private StackPane ContentSlot;

    // --- BỘ NHỚ CACHE ---
    // Lưu Thể xác (Giao diện)
    private final Map<ViewPath, Node> viewCache = new EnumMap<>(ViewPath.class);
    // Lưu Linh hồn (Controller để gọi hàm)
    private final Map<ViewPath, Object> controllerCache = new EnumMap<>(ViewPath.class);

    // Lưu vết Controller đang hiển thị trên màn hình hiện tại
    private Object currentController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Gán instance bằng chính object này khi giao diện load xong
        instance = this;

        setupUserInterface(RoleType.SELLER);
        loadView(ViewPath.BIDDER_AUCTION);
    }

    public void setupUserInterface(RoleType role) {
        //Check role
        ViewPath sidebarPath = switch (role) {
            case RoleType.BIDDER -> ViewPath.BIDDER_SIDEBAR;
            case RoleType.SELLER -> ViewPath.SELLER_SIDEBAR;
            case RoleType.ADMIN  -> ViewPath.ADMIN_SIDEBAR;
        };

        try { //set Sidebar
            FXMLLoader loader = new FXMLLoader(getClass().getResource(sidebarPath.getPath()));
            Parent sidebar = loader.load();
            SidebarSlot.getChildren().setAll(sidebar);
        } catch (Exception e) {
            System.err.println("[MainViewController] Thiết lập Sidebar không thành công: " + e.getMessage());
        }
    }

    // Hàm loadView cơ bản cho các trường hợp chuyển trang không cần bơm dữ liệu
    public void loadView(ViewPath target) {
        loadView(target, null);
    }

    // --- HÀM CHUYỂN TRANG VẠN NĂNG (CÓ CACHE, LIFECYCLE VÀ INJECT) ---
    public <T> void loadView(ViewPath target, Consumer<T> controllerAction) {
        try {
            Node view;
            Object nextController;

            // 1. CHO TRANG CŨ ĐI NGỦ (Ngắt kết nối Socket, giải phóng tài nguyên)
            if (currentController instanceof ViewControllerLifecycle lifecycleController) {
                lifecycleController.onHide();
            }

            // 2. TẢI TRANG MỚI HOẶC LẤY TỪ CACHE
            if (target.isCacheable() && viewCache.containsKey(target)) {
                System.out.println("Đang lấy từ Cache: " + target.getPath());
                view = viewCache.get(target);
                nextController = controllerCache.get(target);
            } else {
                System.out.println("Đang load mới: " + target.getPath());
                FXMLLoader loader = new FXMLLoader(getClass().getResource(target.getPath()));
                view = loader.load();
                nextController = loader.getController();

                // Lưu vào cache nếu ViewPath đó cho phép
                if (target.isCacheable()) {
                    viewCache.put(target, view);
                    controllerCache.put(target, nextController);
                }
            }

            // 3. INJECT DỮ LIỆU (Bơm ID, cài đặt cấu hình...)
            if (controllerAction != null && nextController != null) {
                @SuppressWarnings("unchecked")
                T typedController = (T) nextController;
                controllerAction.accept(typedController);
            }

            // 4. HIỂN THỊ LÊN MÀN HÌNH
            if (view != null) {
                ContentSlot.getChildren().setAll(view);
            }

            // 5. ĐÁNH THỨC TRANG MỚI (Đăng ký lắng nghe Socket trở lại)
            currentController = nextController;
            if (currentController instanceof ViewControllerLifecycle lifecycleController) {
                lifecycleController.onShow();
            }

        } catch (IOException e) {
            System.err.println("[MainViewController] Không thể thiết lập giao diện nội dung: " + target.getPath());
            e.printStackTrace();
        }
    }
}