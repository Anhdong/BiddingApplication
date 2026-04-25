package com.uet.BiddingApplication.Utils.Factory;

import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Model.Electronics;
import com.uet.BiddingApplication.Model.Art;
import com.uet.BiddingApplication.Model.Vehicle;

public class ItemFactory {

    /**
     * Factory Method để tạo đối tượng Item cụ thể dựa trên loại Category.
     * Đầu vào nhận một chuỗi (String) từ ItemCreateDTO để tiện xử lý.
     *
     * @param categoryType Loại sản phẩm (VD: "ELECTRONICS", "ART", "VEHICLE")
     * @return Đối tượng Item tương ứng
     */
    public static Item createItem(String categoryType) {
        if (categoryType == null || categoryType.trim().isEmpty()) {
            throw new BusinessException("Loại sản phẩm (Category) không được để trống.");
        }

        // Chuyển về chữ in hoa để so sánh tránh lỗi gõ phím (case-insensitive)
        String type = categoryType.trim().toUpperCase();

        switch (type) {
            case "ELECTRONICS":
                return new Electronics();
            case "ART":
                return new Art();
            case "VEHICLE":
                return new Vehicle();
            default:
                throw new BusinessException("Hệ thống chưa hỗ trợ loại sản phẩm: " + categoryType);
        }
    }
}