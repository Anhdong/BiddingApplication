package com.uet.BiddingApplication.Utils.Factory;

import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.*;

public class ItemFactory {

    /**
     * Factory Method để tạo đối tượng Item cụ thể dựa trên loại Category.
     * Đầu vào nhận một chuỗi (String) từ ItemCreateDTO để tiện xử lý.
     *
     * @param type Loại sản phẩm (VD: "ELECTRONICS", "ART", "VEHICLE")
     * @return Đối tượng Item tương ứng
     */
    public static Item createItem(Category type) {
        if (type == null) {
            throw new BusinessException("Loại sản phẩm (Category) không được để trống.");
        }

        // Chuyển về chữ in hoa để so sánh tránh lỗi gõ phím (case-insensitive)

        switch (type) {
            case Category.ELECTRONICS:
                return new Electronics();
            case Category.ART:
                return new Art();
            case Category.VEHICLE:
                return new Vehicle();
            case Category.OTHERS:
                return  new Others();
            default:
                throw new BusinessException("Hệ thống chưa hỗ trợ loại sản phẩm: " + type);
        }
    }
}