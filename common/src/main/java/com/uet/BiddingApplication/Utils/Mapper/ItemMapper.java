package com.uet.BiddingApplication.Utils.Mapper;

import com.uet.BiddingApplication.DTO.Request.ItemCreateDTO;
import com.uet.BiddingApplication.DTO.Request.ItemUpdateRequestDTO;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.*;
import com.uet.BiddingApplication.Utils.Factory.ItemFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ItemMapper {

    private static final Map<Class<? extends Item>, BiConsumer<Item, String>> attributeMappers = new HashMap<>();

    static {
        attributeMappers.put(Electronics.class, (entity, attr) -> {
            try {
                ((Electronics) entity).setWarrantyMonths(Integer.parseInt(attr));
            } catch (NumberFormatException e) {
                throw new BusinessException("Lỗi dữ liệu: Thời gian bảo hành của đồ điện tử phải là một số nguyên (ví dụ: 12).");
            }
        });

        attributeMappers.put(Art.class, (entity, attr) -> {
            ((Art) entity).setArtistName(attr);
        });

        attributeMappers.put(Vehicle.class, (entity, attr) -> {
            ((Vehicle) entity).setCondition(attr);
        });
    }

    public static Item toEntity(ItemCreateDTO dto, String sellerId, String imageUrl) {
        if (dto == null) return null;

        Item entity = ItemFactory.createItem(dto.getCategory());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setSellerId(sellerId);
        entity.setImageURL(imageUrl);
        entity.setCategory(dto.getCategory());

        mapSpecificAttribute(entity, dto.getAttribute());

        return entity;
    }

    public static Item toEntity(ItemUpdateRequestDTO dto, String imageUrl) {
        if (dto == null) return null;

        Item entity = ItemFactory.createItem(dto.getCategory());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setImageURL(imageUrl);
        entity.setCategory(dto.getCategory());
        entity.setId(dto.getItemId());

        mapSpecificAttribute(entity, dto.getAttribute());

        return entity;
    }

    /**
     * Hàm helper xử lý chung cho phần thuộc tính đặc thù, tuân thủ nguyên tắc DRY.
     */
    private static void mapSpecificAttribute(Item entity, String rawAttribute) {
        if (rawAttribute == null || rawAttribute.trim().isEmpty()) {
            return;
        }

        BiConsumer<Item, String> mapper = attributeMappers.get(entity.getClass());
        if (mapper != null) {
            mapper.accept(entity, rawAttribute.trim());
        }
    }
}