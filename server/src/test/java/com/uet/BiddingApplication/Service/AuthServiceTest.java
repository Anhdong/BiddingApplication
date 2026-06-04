package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.DAO.Impl.UserDAO;
import com.uet.BiddingApplication.DTO.Request.AuthRequestDTO;
import com.uet.BiddingApplication.DTO.Request.PasswordChangeRequestDTO;
import com.uet.BiddingApplication.DTO.Request.ProfileUpdateRequestDTO;
import com.uet.BiddingApplication.DTO.Request.RegisterRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuthResponseDTO;
import com.uet.BiddingApplication.DTO.Response.UserProfileDTO;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.Bidder;
import com.uet.BiddingApplication.Model.User;
import com.uet.BiddingApplication.ServerClass.AuctionServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    private AuthService authService;

    @Mock
    private UserDAO mockUserDAO;

    @Mock
    private AuctionServer mockAuctionServer;

    @BeforeEach
    void setUp() throws Exception {
        authService = AuthService.getInstance();

        // Tiêm mockUserDAO vào AuthService
        injectPrivateField(authService, "userDAO", mockUserDAO);

        // Tiêm mockAuctionServer vào Singleton
        injectSingleton(AuctionServer.class, mockAuctionServer);

        // Dọn dẹp cache RAM trước mỗi test case để đảm bảo tính độc lập
        ConcurrentHashMap<?, ?> cache = (ConcurrentHashMap<?, ?>) getPrivateField(authService, "userCache");
        cache.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Trả lại trạng thái sạch cho AuctionServer Singleton
        injectSingleton(AuctionServer.class, null);
    }

    // --- Helper Methods ---
    private void injectPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private void injectSingleton(Class<?> clazz, Object mockInstance) throws Exception {
        Field field = clazz.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, mockInstance);
    }

    // ==========================================
    // TEST CASES: Login
    // ==========================================

    @Test
    @DisplayName("Login: Thành công khi user tồn tại và đúng mật khẩu")
    void testLogin_Success() {
        // Arrange
        String plainPassword = "password123";
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

        // Thay new User() bằng new Bidder() để áp dụng tính Đa hình
        User mockUser = new Bidder();
        mockUser.setId("user-1");
        mockUser.setUsername("testuser");
        mockUser.setPasswordHash(hashedPassword);
        mockUser.setActive(true);

        when(mockUserDAO.findByUsername("testuser")).thenReturn(mockUser);

        AuthRequestDTO request = new AuthRequestDTO("testuser", plainPassword);

        // Act
        AuthResponseDTO response = authService.login(request);

        // Assert
        assertNotNull(response.getToken(), "Token không được null khi đăng nhập thành công");
        assertEquals("testuser", response.getUserProfile().getUsername(), "Profile trả về phải khớp với user");

        // Xác minh cache RAM đã lưu token
        String cachedUserId = authService.validateToken(response.getToken());
        assertEquals("user-1", cachedUserId, "UserId trong cache RAM phải khớp với thực tế");
    }

    @Test
    @DisplayName("Login: Bắn lỗi BusinessException khi sai mật khẩu")
    void testLogin_Fail_WrongPassword() {
        // Arrange
        User mockUser = new Bidder();
        mockUser.setUsername("testuser");
        mockUser.setPasswordHash(BCrypt.hashpw("correct_pass", BCrypt.gensalt()));

        when(mockUserDAO.findByUsername("testuser")).thenReturn(mockUser);

        AuthRequestDTO request = new AuthRequestDTO("testuser", "wrong_pass");

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals("Tài khoản không tồn tại hoặc sai mật khẩu.", exception.getMessage());
    }

    @Test
    @DisplayName("Login: Bắn lỗi BusinessException khi tài khoản bị khóa")
    void testLogin_Fail_InactiveUser() {
        // Arrange
        String plainPassword = "password123";
        User mockUser = new Bidder();
        mockUser.setUsername("testuser");
        mockUser.setPasswordHash(BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
        mockUser.setActive(false); // Tài khoản bị khóa

        when(mockUserDAO.findByUsername("testuser")).thenReturn(mockUser);

        AuthRequestDTO request = new AuthRequestDTO("testuser", plainPassword);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals("Tài khoản của bạn đã bị Admin khóa.", exception.getMessage());
    }

    // ==========================================
    // TEST CASES: Register
    // ==========================================

    @Test
    @DisplayName("Register: Thất bại khi Email đã tồn tại")
    void testRegister_Fail_EmailExists() {
        // Arrange
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("exist@test.com");
        request.setUsername("newuser");

        // Giả lập DB báo email đã tồn tại bằng một đối tượng cụ thể (Bidder)
        when(mockUserDAO.findByEmail("exist@test.com")).thenReturn(new Bidder());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals("Email này đã được đăng ký.", exception.getMessage());
    }

    @Test
    @DisplayName("Register: Thành công đăng ký tài khoản Bidder mới")
    void testRegister_Success() {
        // Arrange
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("new@test.com");
        request.setUsername("newuser");
        request.setPassword("pass123");
        request.setRole(RoleType.BIDDER);

        when(mockUserDAO.findByEmail("new@test.com")).thenReturn(null);
        when(mockUserDAO.findByUsername("newuser")).thenReturn(null);
        when(mockUserDAO.insertUser(any(User.class))).thenReturn(true);

        // Act
        boolean result = authService.register(request);

        // Assert
        assertTrue(result, "Đăng ký phải trả về true khi thành công");
        verify(mockUserDAO, times(1)).insertUser(any(User.class));
    }

    // ==========================================
    // TEST CASES: Logout
    // ==========================================

    @Test
    @DisplayName("Logout: Trả về true và xóa token khỏi RAM thành công")
    void testLogout_Success() throws Exception {
        // Arrange
        ConcurrentHashMap<String, String> cache = (ConcurrentHashMap<String, String>) getPrivateField(authService, "userCache");
        cache.put("valid-token", "user-1");

        // Act
        boolean result = authService.logout("valid-token");

        // Assert
        assertTrue(result, "Đăng xuất phải trả về true");
        assertNull(authService.validateToken("valid-token"), "Token phải bị xóa hoàn toàn khỏi cache RAM");
    }

    // ==========================================
    // TEST CASES: changePassword
    // ==========================================

    @Test
    @DisplayName("changePassword: Thành công đổi mật khẩu khi mật khẩu cũ đúng")
    void testChangePassword_Success() {
        // Arrange
        String userId = "user-1";
        String oldPassword = "oldpass";
        String newPassword = "newpass";

        User mockUser = new Bidder();
        mockUser.setId(userId);
        mockUser.setPasswordHash(BCrypt.hashpw(oldPassword, BCrypt.gensalt()));

        when(mockUserDAO.findById(userId)).thenReturn(mockUser);
        when(mockUserDAO.changePassword(eq(userId), anyString())).thenReturn(true);

        PasswordChangeRequestDTO request = new PasswordChangeRequestDTO(oldPassword, newPassword);

        // Act
        boolean result = authService.changePassword(request, userId);

        // Assert
        assertTrue(result, "Đổi mật khẩu phải trả về true khi thành công");
        verify(mockUserDAO, times(1)).changePassword(eq(userId), anyString());
    }

    @Test
    @DisplayName("changePassword: Thất bại khi mật khẩu cũ không đúng")
    void testChangePassword_Fail_WrongOldPassword() {
        // Arrange
        String userId = "user-1";

        User mockUser = new Bidder();
        mockUser.setId(userId);
        mockUser.setPasswordHash(BCrypt.hashpw("correct_old", BCrypt.gensalt()));

        when(mockUserDAO.findById(userId)).thenReturn(mockUser);

        PasswordChangeRequestDTO request = new PasswordChangeRequestDTO("wrong_old", "new_pass");

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.changePassword(request, userId));

        assertEquals("Mật khẩu cũ không chính xác.", exception.getMessage());
        verify(mockUserDAO, never()).changePassword(anyString(), anyString());
    }

    // ==========================================
    // TEST CASES: updateProfile
    // ==========================================

    @Test
    @DisplayName("updateProfile: Thành công trả về UserProfileDTO sau khi cập nhật")
    void testUpdateProfile_Success() {
        // Arrange
        String userId = "user-1";

        Bidder mockUser = new Bidder();
        mockUser.setId(userId);
        mockUser.setUsername("oldname");
        mockUser.setEmail("test@test.com");
        mockUser.setPhone("0123456789");
        mockUser.setRole(RoleType.BIDDER);
        mockUser.setShippingAddress("Hà Nội");

        when(mockUserDAO.findById(userId)).thenReturn(mockUser);
        when(mockUserDAO.updateProfile(any(User.class))).thenReturn(true);

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO("newname", "0987654321", "HCM");

        // Act
        UserProfileDTO result = authService.updateProfile(request, userId);

        // Assert
        assertNotNull(result, "Phải trả về UserProfileDTO thay vì null");
        verify(mockUserDAO, times(1)).updateProfile(any(User.class));
    }

    @Test
    @DisplayName("updateProfile: Thất bại khi tên đăng nhập đã được sử dụng bởi người khác")
    void testUpdateProfile_Fail_DuplicateUsername() {
        String userId = "user-1";

        Bidder mockUser = new Bidder();
        mockUser.setId(userId);

        Bidder otherUser = new Bidder();
        otherUser.setId("user-2");
        otherUser.setUsername("existingName");

        when(mockUserDAO.findById(userId)).thenReturn(mockUser);
        when(mockUserDAO.findByUsername("existingName")).thenReturn(otherUser);

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO("existingName", "0987654321", "HCM");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.updateProfile(request, userId));

        assertEquals("Tên đăng nhập (Username) đã tồn tại.", exception.getMessage());
        verify(mockUserDAO, never()).updateProfile(any(User.class));
    }

    @Test
    @DisplayName("updateProfile: Thất bại khi số điện thoại đã được sử dụng bởi người khác")
    void testUpdateProfile_Fail_DuplicatePhone() {
        String userId = "user-1";

        Bidder mockUser = new Bidder();
        mockUser.setId(userId);

        Bidder otherUser = new Bidder();
        otherUser.setId("user-2");
        otherUser.setPhone("0987654321");

        when(mockUserDAO.findById(userId)).thenReturn(mockUser);
        when(mockUserDAO.findByPhone("0987654321")).thenReturn(otherUser);

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO("newName", "0987654321", "HCM");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.updateProfile(request, userId));

        assertEquals("Số điện thoại (Phone Number) đã tồn tại.", exception.getMessage());
        verify(mockUserDAO, never()).updateProfile(any(User.class));
    }

    @Test
    @DisplayName("updateProfile: Thành công khi tên đăng nhập/số điện thoại trùng với chính mình")
    void testUpdateProfile_Success_SameUserKeepingDetails() {
        String userId = "user-1";

        Bidder mockUser = new Bidder();
        mockUser.setId(userId);
        mockUser.setUsername("myUsername");
        mockUser.setPhone("0987654321");
        mockUser.setEmail("test@test.com");
        mockUser.setRole(RoleType.BIDDER);

        when(mockUserDAO.findById(userId)).thenReturn(mockUser);
        when(mockUserDAO.findByUsername("myUsername")).thenReturn(mockUser);
        when(mockUserDAO.findByPhone("0987654321")).thenReturn(mockUser);
        when(mockUserDAO.updateProfile(any(User.class))).thenReturn(true);

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO("myUsername", "0987654321", "HCM");

        UserProfileDTO result = authService.updateProfile(request, userId);

        assertNotNull(result, "Phải trả về UserProfileDTO thay vì null");
        verify(mockUserDAO, times(1)).updateProfile(any(User.class));
    }
}