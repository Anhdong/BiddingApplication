package com.uet.BiddingApplication.Utils.Mapper;

import com.uet.BiddingApplication.DTO.Request.ItemCreateDTO;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.*;
import com.uet.BiddingApplication.Utils.Factory.ItemFactory;

public class ItemMapper {
    public static Item toEntity(ItemCreateDTO dto, String sellerId, String imageUrl) {
        if (dto == null) return null;

        // 1. Gọi Factory để đúc ra đúng class con (Electronics, Art, hoặc Vehicle)
        Item entity = ItemFactory.createItem(dto.getCategory());

        // 2. Set các trường thông tin cơ bản chung của mọi Item
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setSellerId(sellerId);
        entity.setImageURL(imageUrl);

        // Chuyển chuỗi String thành Enum Category để lưu vào Model
        // (Giả sử Entity Item có hàm setCategory nhận tham số là Enum)
        try {
            entity.setCategory(Category.valueOf(dto.getCategory().toUpperCase()));
        } catch (IllegalArgumentException e) {
            // Xử lý nếu Enum không khớp
        }

        // 4. XỬ LÝ THUỘC TÍNH ĐẶC THÙ (ATTRIBUTE)
        String rawAttribute = dto.getAttribute();

        // Chỉ xử lý nếu Client có gửi dữ liệu đặc thù lên
        if (rawAttribute != null && !rawAttribute.trim().isEmpty()) {

            // Nếu là đồ điện tử -> Ép kiểu String thành int
            if (entity instanceof Electronics) {
                Electronics electronics = (Electronics) entity;
                try {
                    // Dùng Integer.parseInt để chuyển chuỗi thành số
                    int warranty = Integer.parseInt(rawAttribute.trim());
                    electronics.setWarrantyMonths(warranty);
                } catch (NumberFormatException e) {
                    // PHÒNG THỦ: Tránh sập Server nếu UI gửi lên chuỗi "Mười hai tháng" thay vì số "12"
                    // throw new BusinessException("Lỗi dữ liệu: Thời gian bảo hành của đồ điện tử phải là một số nguyên (ví dụ: 12).");
                }
            }
            // Nếu là Tác phẩm nghệ thuật -> Giữ nguyên kiểu String
            else if (entity instanceof Art) {
                Art art = (Art) entity;
                art.setArtistName(rawAttribute.trim());
            }
            // Nếu là Phương tiện -> Giữ nguyên kiểu String
            else if (entity instanceof Vehicle) {
                Vehicle vehicle = (Vehicle) entity;
                vehicle.setCondition(rawAttribute.trim());
            }
            // Lớp Others không có thuộc tính riêng nên hệ thống tự động bỏ qua, không cần viết else.
        }

        return entity;
    }
}
