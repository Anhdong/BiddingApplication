package com.uet.BiddingApplication.DAO.Impl;

import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SessionRegistrationDAOTest {

    // Khai báo các DAO theo chuẩn Singleton từ kiến trúc
    private SessionRegistrationDAO registrationDAO;
    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private AuctionSessionDAO sessionDAO;

    // Các UUID cố định dùng cho một luồng Test
    private final String testBidderId = UUID.randomUUID().toString();
    private final String testSellerId = UUID.randomUUID().toString();
    private final String testItemId = UUID.randomUUID().toString();
    private final String testSessionId = UUID.randomUUID().toString();
    private final String testRegistrationId = UUID.randomUUID().toString();

    @BeforeAll
    void init() {
        // Khởi tạo các instance Singleton
        registrationDAO = SessionRegistrationDAO.getInstance();
        userDAO = UserDAO.getInstance();
        itemDAO = ItemDAO.getInstance();
        sessionDAO = AuctionSessionDAO.getInstance();
    }

    @BeforeEach
    void setUp() throws SQLException {
        // 1. Dọn dẹp trước để tránh rác từ các test run bị lỗi trước đó
        cleanUpData();

        // 2. Insert dữ liệu phụ thuộc bằng các DAO có sẵn (Setup Mock Data thật vào DB)

        // Tạo User đóng vai trò Seller
        User seller = new Seller();
        seller.setId(testSellerId);
        seller.setUsername("test_seller");
        seller.setEmail("seller@test.com");
        seller.setPasswordHash("hashed_pass");
        seller.setRole(RoleType.SELLER);
        userDAO.insertUser(seller);

        // Tạo User đóng vai trò Bidder
        User bidder = new Bidder();
        bidder.setId(testBidderId);
        bidder.setUsername("test_bidder");
        bidder.setEmail("bidder@test.com");
        bidder.setPasswordHash("hashed_pass");
        bidder.setRole(RoleType.BIDDER);
        userDAO.insertUser(bidder);

        // Tạo Item thuộc về Seller
        Item item = new Electronics();
        item.setId(testItemId);
        item.setName("Laptop Gaming Test");
        item.setImageURL("http://image.com/laptop.jpg");
        item.setSellerId(testSellerId);
        item.setCategory(Category.ELECTRONICS);
        item.setCreatedAt(LocalDateTime.now());
        itemDAO.insertItem(item);

        // Tạo AuctionSession bán Item đó
        AuctionSession session = new AuctionSession();
        session.setId(testSessionId);
        session.setItemId(testItemId);
        session.setSellerId(testSellerId);
        session.setStartPrice(new BigDecimal("15000000.00"));
        session.setStartTime(LocalDateTime.now());
        session.setEndTime(LocalDateTime.now().plusDays(2));
        session.setStatus(SessionStatus.OPEN);
        session.setCreatedAt(LocalDateTime.now());
        sessionDAO.insertSession(session);
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Dọn dẹp dữ liệu để trả lại môi trường sạch
        cleanUpData();
    }

    /**
     * Hàm dọn dẹp sử dụng raw JDBC vì trong kiến trúc DAO không định nghĩa
     * sẵn hàm deleteUser hay deleteSession (nhằm tránh rủi ro xóa nhầm hệ thống).
     * Phải xóa theo thứ tự để không vi phạm Foreign Key (từ con lên cha).
     */
    private void cleanUpData()  {
        userDAO.deleteUser(testSellerId);
        itemDAO.deleteItem(testItemId);
        userDAO.deleteUser(testBidderId);
        registrationDAO.deleteRegistration(testBidderId,testSessionId);
    }

    // ================== CÁC TESTCASE CHÍNH ==================

    @Test
    @DisplayName("Đảm bảo DAO áp dụng đúng chuẩn Singleton theo thiết kế")
    void testSingletonInstance() {
        SessionRegistrationDAO instance1 = SessionRegistrationDAO.getInstance();
        SessionRegistrationDAO instance2 = SessionRegistrationDAO.getInstance();
        assertSame(instance1, instance2, "Chỉ được phép tồn tại 1 instance duy nhất trên RAM");
    }

    @Test
    @DisplayName("registerBidder: Thành công khi user đăng ký phiên hợp lệ")
    void testRegisterBidder_Success() {
        SessionRegistration registration = new SessionRegistration();
        registration.setId(testRegistrationId);
        registration.setCreatedAt(LocalDateTime.now());
        registration.setBidderId(testBidderId);
        registration.setSessionId(testSessionId);

        boolean isRegistered = registrationDAO.registerBidder(registration);

        assertTrue(isRegistered, "Phải insert thành công vào bảng session_registrations");
        assertTrue(registrationDAO.checkRegistration(testBidderId, testSessionId), "Data thực tế phải được tìm thấy trong DB");
    }

    @Test
    @DisplayName("checkRegistration: Trả về false khi user chưa hề đăng ký")
    void testCheckRegistration_NotFound() {
        boolean exists = registrationDAO.checkRegistration(testBidderId, UUID.randomUUID().toString());
        assertFalse(exists, "Phải trả về false nếu truyền vào một session_id không tồn tại");
    }

    @Test
    @DisplayName("deleteRegistration: Hủy đăng ký thành công")
    void testDeleteRegistration_Success() {
        // Cần đăng ký trước
        SessionRegistration registration = new SessionRegistration();
        registration.setId(testRegistrationId);
        registration.setCreatedAt(LocalDateTime.now());
        registration.setBidderId(testBidderId);
        registration.setSessionId(testSessionId);
        registrationDAO.registerBidder(registration);

        // Act
        boolean isDeleted = registrationDAO.deleteRegistration(testBidderId, testSessionId);

        // Assert
        assertTrue(isDeleted, "Xóa thành công phải trả về true");
        assertFalse(registrationDAO.checkRegistration(testBidderId, testSessionId), "Bản ghi phải biến mất hoàn toàn");
    }

    @Test
    @DisplayName("deleteRegistration: Trả về false nếu hủy phiên chưa đăng ký")
    void testDeleteRegistration_NotFound() {
        boolean isDeleted = registrationDAO.deleteRegistration(testBidderId, UUID.randomUUID().toString());
        assertFalse(isDeleted, "Không có bản ghi nào bị tác động thì phải trả về false");
    }

    @Test
    @DisplayName("getRegisteredSessions: Trả về danh sách DTO đã được mapping chuẩn từ 3 bảng")
    void testGetRegisteredSessions_Success() {
        // Đăng ký tham gia phiên
        SessionRegistration registration = new SessionRegistration();
        registration.setId(testRegistrationId);
        registration.setCreatedAt(LocalDateTime.now());
        registration.setBidderId(testBidderId);
        registration.setSessionId(testSessionId);
        registrationDAO.registerBidder(registration);

        // Act
        List<AuctionCardDTO> list = registrationDAO.getRegisteredSessions(testBidderId);

        // Assert
        assertNotNull(list, "Hàm không được trả về null");
        assertEquals(1, list.size(), "Phải trả về đúng 1 CardDTO");

        AuctionCardDTO dto = list.get(0);
        assertEquals(testSessionId, dto.getSessionId(), "Phải map đúng session_id");
        assertEquals("Laptop Gaming Test", dto.getItemName(), "Phải join lấy được item_name từ bảng items");
        assertEquals("http://image.com/laptop.jpg", dto.getImageURL(), "Phải join lấy được image_url");
        assertNotNull(dto.getStartPrice(), "Giá khởi điểm không được null");
        assertEquals(SessionStatus.OPEN, dto.getStatus(), "Enum Status phải parse thành công");
    }
}