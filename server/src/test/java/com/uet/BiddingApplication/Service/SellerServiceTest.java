package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.CoreService.SessionStartScheduler;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
import com.uet.BiddingApplication.DTO.Request.ItemUpdateRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.SellerHistoryResponseDTO;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.Art;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.Electronics;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Utils.StorageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SellerServiceTest {

    private SellerService sellerService;

    @Mock private ItemDAO mockItemDAO;
    @Mock private AuctionSessionDAO mockSessionDAO;
    @Mock private StorageService mockStorageService;
    @Mock private SearchCacheManager mockCacheManager;
    @Mock private SessionStartScheduler mockScheduler;

    @BeforeEach
    void setUp() throws Exception {
        sellerService = SellerService.getInstance();
        injectSingleton(ItemDAO.class, mockItemDAO);
        injectSingleton(AuctionSessionDAO.class, mockSessionDAO);
        injectSingleton(StorageService.class, mockStorageService);
        injectSingleton(SearchCacheManager.class, mockCacheManager);
        injectSingleton(SessionStartScheduler.class, mockScheduler);
    }

    @AfterEach
    void tearDown() throws Exception {
        injectSingleton(ItemDAO.class, null);
        injectSingleton(AuctionSessionDAO.class, null);
        injectSingleton(StorageService.class, null);
        injectSingleton(SearchCacheManager.class, null);
        injectSingleton(SessionStartScheduler.class, null);
    }

    private void injectSingleton(Class<?> clazz, Object mockInstance) throws Exception {
        Field field = clazz.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, mockInstance);
    }

    // ==========================================
    // TEST CASES: getSellerHistory
    // ==========================================

    @Test
    @DisplayName("getSellerHistory: Thành công lấy lịch sử người bán")
    void testGetSellerHistory_Success() {
        String sellerId = "seller-1";
        List<SellerHistoryResponseDTO> mockHistory = Collections.singletonList(new SellerHistoryResponseDTO());
        when(mockSessionDAO.getSellerHistory(sellerId)).thenReturn(mockHistory);

        List<SellerHistoryResponseDTO> result = sellerService.getSellerHistory(sellerId);

        assertFalse(result.isEmpty(), "Danh sách trả về không được rỗng");
        verify(mockSessionDAO, times(1)).getSellerHistory(sellerId);
    }

    @Test
    @DisplayName("getSellerHistory: Thất bại do Seller ID trống")
    void testGetSellerHistory_Fail_EmptyId() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> sellerService.getSellerHistory(""));

        assertTrue(exception.getMessage().contains("không được để trống"));
    }

    // ==========================================
    // TEST CASES: updateItem
    // ==========================================

    @Test
    @DisplayName("updateItem: Thành công cập nhật vật phẩm (Electronics) không kèm ảnh mới")
    void testUpdateItem_Success_NoNewImage() {
        String itemId = "item-1";
        ItemUpdateRequestDTO request = new ItemUpdateRequestDTO();
        request.setItemId(itemId);
        request.setName("Laptop Gaming Mới");
        request.setCategory(Category.ELECTRONICS);

        Item mockItem = new Electronics();
        mockItem.setId(itemId);
        mockItem.setImageURL("http://old-image.url");
        ((Electronics) mockItem).setWarrantyMonths(12);

        AuctionSession mockSession = new AuctionSession();
        mockSession.setId("session-1");
        // Áp dụng trạng thái CANCELED hợp lệ để có thể cập nhật vật phẩm
        mockSession.setStatus(SessionStatus.CANCELED);

        when(mockItemDAO.getItemById(itemId)).thenReturn(mockItem);
        when(mockSessionDAO.getSessionByItemId(itemId)).thenReturn(mockSession);
        when(mockItemDAO.updateItem(any(Item.class))).thenReturn(true);
        when(mockSessionDAO.updateSession(any(AuctionSession.class))).thenReturn(true);

        boolean result = sellerService.updateItem(request);

        assertTrue(result);
        verify(mockItemDAO, times(1)).updateItem(any(Item.class));
        verify(mockCacheManager, times(1)).updateItem(eq(itemId), any(Item.class));
    }

    @Test
    @DisplayName("updateItem: Thất bại do phiên đang RUNNING")
    void testUpdateItem_Fail_SessionRunning() {
        String itemId = "item-1";
        ItemUpdateRequestDTO request = new ItemUpdateRequestDTO();
        request.setItemId(itemId);

        Item mockItem = new Art();
        mockItem.setId(itemId);

        AuctionSession mockSession = new AuctionSession();
        mockSession.setStatus(SessionStatus.RUNNING);

        when(mockItemDAO.getItemById(itemId)).thenReturn(mockItem);
        when(mockSessionDAO.getSessionByItemId(itemId)).thenReturn(mockSession);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sellerService.updateItem(request));

        assertTrue(exception.getMessage().contains("Không thể cập nhật vật phẩm khi phiên đấu giá đang "));
        verify(mockItemDAO, never()).updateItem(any());
    }

    // ==========================================
    // TEST CASES: deleteItem
    // ==========================================

    @Test
    @DisplayName("deleteItem: Thành công xóa vật phẩm và dọn dẹp Storage")
    void testDeleteItem_Success() throws Exception {
        String itemId = "item-1";

        Item mockItem = new Electronics();
        mockItem.setId(itemId);
        mockItem.setImageURL("http://delete-me.url");

        when(mockItemDAO.getItemById(itemId)).thenReturn(mockItem);
        when(mockSessionDAO.getSessionByItemId(itemId)).thenReturn(null);
        when(mockItemDAO.deleteItem(itemId)).thenReturn(true);

        boolean result = sellerService.deleteItem(itemId);

        assertTrue(result);
        verify(mockStorageService, times(1)).deleteImage("http://delete-me.url");
        verify(mockItemDAO, times(1)).deleteItem(itemId);
        verify(mockCacheManager, times(1)).removeItem(itemId);
    }

    @Test
    @DisplayName("deleteItem: Thất bại do phiên đang RUNNING")
    void testDeleteItem_Fail_SessionRunning() {
        String itemId = "item-1";
        Item mockItem = new Electronics();
        mockItem.setId(itemId);

        AuctionSession mockSession = new AuctionSession();
        mockSession.setStatus(SessionStatus.RUNNING);

        when(mockItemDAO.getItemById(itemId)).thenReturn(mockItem);
        when(mockSessionDAO.getSessionByItemId(itemId)).thenReturn(mockSession);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sellerService.deleteItem(itemId));

        assertTrue(exception.getMessage().contains("Không thể xóa vật phẩm khi phiên đấu giá đang"));
        verify(mockItemDAO, never()).deleteItem(anyString());
    }

    // ==========================================
    // TEST CASES: getItemsBySellerId
    // ==========================================

    @Test
    @DisplayName("getItemsBySellerId: Thành công lấy danh sách sản phẩm theo Seller")
    void testGetItemsBySellerId_Success() {
        String sellerId = "seller-1";
        List<AuctionCardDTO> mockItems = Collections.singletonList(new AuctionCardDTO());
        when(mockItemDAO.getSellerItems(sellerId)).thenReturn(mockItems);

        List<AuctionCardDTO> result = sellerService.getItemsBySellerId(sellerId);

        assertFalse(result.isEmpty(), "Danh sách không được rỗng");
        verify(mockItemDAO, times(1)).getSellerItems(sellerId);
    }
}