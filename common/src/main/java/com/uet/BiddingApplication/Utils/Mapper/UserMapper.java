package com.uet.BiddingApplication.Utils.Mapper;

import com.uet.BiddingApplication.DTO.Request.ProfileUpdateRequestDTO;
import com.uet.BiddingApplication.DTO.Request.RegisterRequestDTO;
import com.uet.BiddingApplication.DTO.Response.UserProfileDTO;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Model.Admin;
import com.uet.BiddingApplication.Model.Bidder;
import com.uet.BiddingApplication.Model.Seller;
import com.uet.BiddingApplication.Model.User;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class UserMapper {

    // 1. Map xử lý lấy thuộc tính đặc thù để chuyển thành DTO (toDto)
    private static final Map<Class<? extends User>, Function<User, String>> dtoSpecialAttrMappers = new HashMap<>();

    // 2. Map xử lý khởi tạo đối tượng dựa trên RoleType (Tránh if-else khởi tạo)
    private static final Map<RoleType, Supplier<User>> entityFactories = new EnumMap<>(RoleType.class);

    // 3. Map xử lý nạp thuộc tính đặc thù từ String vào Entity (toEntity / updateEntity)
    private static final Map<Class<? extends User>, BiConsumer<User, String>> entitySpecialAttrMappers = new HashMap<>();

    static {
        // --- Cấu hình cho toDto ---
        dtoSpecialAttrMappers.put(Bidder.class, user -> ((Bidder) user).getShippingAddress());
        dtoSpecialAttrMappers.put(Seller.class, user -> ((Seller) user).getBankAccount());
        // VÁ LỖ HỔNG BẢO MẬT: Không bao giờ trả về OtpSecretKey cho Client
        dtoSpecialAttrMappers.put(Admin.class, user -> null);

        // --- Cấu hình Factory tạo Entity ---
        entityFactories.put(RoleType.BIDDER, Bidder::new);
        entityFactories.put(RoleType.SELLER, Seller::new);
        entityFactories.put(RoleType.ADMIN, Admin::new);

        // --- Cấu hình nạp dữ liệu đặc thù vào Entity ---
        entitySpecialAttrMappers.put(Bidder.class, (user, attr) -> ((Bidder) user).setShippingAddress(attr));
        entitySpecialAttrMappers.put(Seller.class, (user, attr) -> ((Seller) user).setBankAccount(attr));
        entitySpecialAttrMappers.put(Admin.class, (user, attr) -> ((Admin) user).setOtpSecretKey(attr));
    }

    /**
     * Chuyển đổi từ Entity sang DTO để gửi về Client qua mạng.
     */
    public static UserProfileDTO toDto(User entity) {
        if (entity == null) return null;

        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setRole(entity.getRole());

        // Gắn thuộc tính đặc thù dựa trên Class mà không cần instanceof
        Function<User, String> attrMapper = dtoSpecialAttrMappers.get(entity.getClass());
        if (attrMapper != null) {
            dto.setSpecialAttribute(attrMapper.apply(entity));
        }

        return dto;
    }

    /**
     * Chuyển đổi từ DTO thành Entity.
     */
    public static User toEntity(UserProfileDTO dto) {
        if (dto == null) return null;

        User entity = createEntityByRole(dto.getRole(), dto.getSpecialAttribute());
        entity.setId(dto.getId());
        entity.setUsername(dto.getUsername());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setRole(dto.getRole());

        return entity;
    }

    /**
     * Chuyển đổi từ RegisterRequestDTO thành Entity (Tái sử dụng logic).
     */
    public static User toEntity(RegisterRequestDTO dto) {
        if (dto == null) return null;

        User entity = createEntityByRole(dto.getRole(), dto.getSpecialAttribute());
        entity.setUsername(dto.getUsername());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setRole(dto.getRole());

        return entity;
    }

    /**
     * Cập nhật dữ liệu từ Request DTO vào Entity hiện có (Business Rule)
     */
    public static void updateEntity(ProfileUpdateRequestDTO dto, User entity) {
        if (dto == null || entity == null) return;

        if (dto.getPhone() != null) entity.setPhone(dto.getPhone());
        if (dto.getUsername() != null) entity.setUsername(dto.getUsername());

        String newSpecialAttr = dto.getSpecialAttribute();
        if (newSpecialAttr != null && !newSpecialAttr.trim().isEmpty()) {
            BiConsumer<User, String> attrSetter = entitySpecialAttrMappers.get(entity.getClass());
            if (attrSetter != null) {
                attrSetter.accept(entity, newSpecialAttr.trim());
            }
        }
    }

    /**
     * Helper Method: Gộp chung logic khởi tạo Entity và set thuộc tính đặc thù.
     * Giải quyết triệt để vi phạm DRY giữa 2 hàm toEntity.
     */
    private static User createEntityByRole(RoleType role, String specialAttr) {
        Supplier<User> factory = entityFactories.get(role);
        if (factory == null) {
            throw new IllegalArgumentException("Không hỗ trợ RoleType: " + role);
        }

        User entity = factory.get();

        if (specialAttr != null && !specialAttr.trim().isEmpty()) {
            BiConsumer<User, String> attrSetter = entitySpecialAttrMappers.get(entity.getClass());
            if (attrSetter != null) {
                attrSetter.accept(entity, specialAttr.trim());
            }
        }
        return entity;
    }
}