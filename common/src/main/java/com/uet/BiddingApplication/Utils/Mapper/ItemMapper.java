package com.uet.BiddingApplication.Utils.Mapper;

import com.uet.BiddingApplication.DTO.Request.ItemCreateDTO;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Model.*;
import com.uet.BiddingApplication.Utils.Factory.ItemFactory;

public class ItemMapper {
    public Item toEntity(ItemCreateDTO dto, String sellerId, String imageUrl) {
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

        return entity;
    }
}
