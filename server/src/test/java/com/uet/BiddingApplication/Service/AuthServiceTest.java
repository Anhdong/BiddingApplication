package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.DAO.Impl.UserDAO;
import com.uet.BiddingApplication.DTO.Request.AuthRequestDTO;
import com.uet.BiddingApplication.DTO.Request.RegisterRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuthResponseDTO;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.Bidder;
import com.uet.BiddingApplication.Model.User;
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

    @BeforeEach
    void setUp() throws Exception {
        authService = AuthService.getInstance();

        // Tiêm mockUserDAO vào AuthService
        injectPrivateField(authService, "userDAO", mockUserDAO);

        // Dọn dẹp cache RAM trước mỗi test case để đảm bảo tính độc lập
        ConcurrentHashMap<?, ?> cache = (ConcurrentHashMap<?, ?>) getPrivateField(authService, "userCache");
        cache.clear();
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

    // --- Test Cases: Login ---

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

    // --- Test Cases: Register ---

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

    // --- Test Cases: Logout ---

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
}