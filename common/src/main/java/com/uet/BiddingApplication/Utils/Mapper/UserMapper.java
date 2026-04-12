package com.uet.BiddingApplication.Utils.Mapper;

import com.uet.BiddingApplication.DTO.Request.ProfileUpdateRequestDTO;
import com.uet.BiddingApplication.DTO.Response.UserProfileDTO;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Model.Admin;
import com.uet.BiddingApplication.Model.Bidder;
import com.uet.BiddingApplication.Model.Seller;
import com.uet.BiddingApplication.Model.User;

// TODO (Kiến trúc): Chuyển class này thành Singleton và thêm "implements DataMapper<User, UserProfileDTO>"
// TODO (Kiến trúc): Xóa bỏ các từ khóa "static" ở các phương thức bên dưới sau khi áp dụng Singleton.
public class UserMapper {

    /**
     * Chuyển đổi từ Entity (Database) sang DTO để gửi về Client qua mạng.
     */
    public static UserProfileDTO toDto(User entity) { // [cite: 1087]
        // Kiểm tra an toàn: Nếu đầu vào null thì trả về null để tránh NullPointerException
        if (entity == null) return null;

        // Bắt đầu copy các thuộc tính cơ bản chung của mọi User
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setRole(entity.getRole());

        // Xử lý specialAttribute dựa trên vai trò thực tế của Entity (OOP Inheritance)
        if (entity instanceof Bidder) {
            Bidder bidder = (Bidder) entity;
            // Gửi địa chỉ giao hàng cho Bidder
            dto.setSpecialAttribute(bidder.getShippingAddress());
        } else if (entity instanceof Seller) {
            Seller seller = (Seller) entity;
            // TODO (Bảo mật): Cân nhắc xem BankAccount có phải thông tin nhạy cảm không.
            // Nếu gửi về chỉ để hiển thị cho chính Seller xem thì ổn, nếu gửi cho người khác xem thì nên che đi.
            dto.setSpecialAttribute(seller.getBankAccount());
        } else if (entity instanceof Admin) {
            Admin admin = (Admin) entity;
            // TODO (Bảo mật NGUY HIỂM): Tuyệt đối KHÔNG gửi OtpSecretKey về Client!
            // Hãy thay bằng một chuỗi hiển thị quyền hạn, ví dụ: "Super Admin" hoặc bỏ trống.
            dto.setSpecialAttribute(admin.getOtpSecretKey());
        }

        return dto;
    }

    /**
     * Chuyển đổi từ DTO (Client gửi lên) thành Entity để xử lý hoặc lưu Database.
     */
    public static User toEntity(UserProfileDTO dto) {
        if (dto == null) return null;

        // Lưu ý: Việc khởi tạo User cụ thể (Bidder/Seller) thường do Factory đảm nhận
        // TODO (Design Pattern): Nên gọi UserFactory.createUser(dto.getRole()) ở đây thay vì if-else

        User entity;
        // Kiểm tra Role để khởi tạo đúng Class con tương ứng
        if (dto.getRole() == RoleType.ADMIN){
            entity = new Admin();
            ((Admin) entity).setOtpSecretKey(dto.getSpecialAttribute());
        } else if (dto.getRole() == RoleType.BIDDER) {
            entity = new Bidder();
            ((Bidder) entity).setShippingAddress(dto.getSpecialAttribute());
        } else {
            entity = new Seller();
            ((Seller) entity).setBankAccount(dto.getSpecialAttribute());
        }

        // TODO (Bảo mật): Cân nhắc KHÔNG map ID từ DTO vào Entity nếu đây là thao tác tạo mới tài khoản (tránh Hacker giả mạo ID).
        entity.setId(dto.getId());

        entity.setUsername(dto.getUsername());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setRole(dto.getRole());

        return entity;
    }

    /**
     * Cập nhật dữ liệu từ Request DTO vào Entity hiện có (Business Rule)
     * Không làm thay đổi ID hay các trường không được phép sửa
     */
    public static void updateEntity(ProfileUpdateRequestDTO dto, User entity) {
        // Kiểm tra an toàn trước khi cập nhật
        if (dto == null || entity == null) return;

        // Chỉ ghi đè (update) nếu Client có gửi dữ liệu mới (khác null)
        if (dto.getPhone() != null){
            entity.setPhone(dto.getPhone());
        }

        if (dto.getUsername() != null){
            entity.setUsername(dto.getUsername());
        }

        // 2. Cập nhật trường đặc thù dựa vào đối tượng thực tế (Entity)
        String newSpecialAttr = dto.getSpecialAttribute();

        if (newSpecialAttr != null && !newSpecialAttr.trim().isEmpty()) {

            if (entity instanceof Bidder) {
                Bidder bidder = (Bidder) entity;
                // Với Bidder, specialAttribute chính là địa chỉ giao hàng
                bidder.setShippingAddress(newSpecialAttr);

            } else if (entity instanceof Seller) {
                Seller seller = (Seller) entity;
                // Với Seller, specialAttribute là tài khoản ngân hàng
                seller.setBankAccount(newSpecialAttr);

            } else if (entity instanceof Admin) {
                Admin admin = (Admin) entity;
                admin.setOtpSecretKey(newSpecialAttr);
            }
        }
    }

}