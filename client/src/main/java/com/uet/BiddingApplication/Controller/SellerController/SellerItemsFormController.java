package com.uet.BiddingApplication.Controller.SellerController;

import com.uet.BiddingApplication.Controller.MainViewController;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.ItemCreateDTO;
import com.uet.BiddingApplication.DTO.Request.ItemUpdateRequestDTO;
import com.uet.BiddingApplication.DTO.Request.SessionTargetRequestDTO;
import com.uet.BiddingApplication.DTO.Response.SessionInfoResponseDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Interface.ViewControllerLifecycle;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import com.uet.BiddingApplication.Util.NotificationUtil;
import com.uet.BiddingApplication.Util.SupabaseUtil;
import com.uet.BiddingApplication.Util.UIUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.uet.BiddingApplication.BiddingApplication.primaryStage;

public class SellerItemsFormController implements Initializable, ViewControllerLifecycle {
    //--LOG--
    private static final Logger log = LoggerFactory.getLogger(SellerItemsFormController.class);

    //--FXML--
    @FXML ImageView imgItem;
    String imageURL = null;
    byte[] imageBytes = null;
    String imageExtension = null;
    @FXML TextArea txtDesc;
    @FXML TextField txtName, txtStartPrice, txtMinBid;
    @FXML ChoiceBox<Category> cbxCategory;
    @FXML DatePicker dpStart, dpEnd;
    @FXML Spinner<Integer> spnStartHour, spnStartMinute, spnEndHour, spnEndMinute;
    @FXML Button btnAction;

    //CALLBACKS
    private final Consumer<ResponsePacket<?>> addItemCallback = this::handleAddItemResponse;

    private final Consumer<ResponsePacket<?>> updateItemCallback = this::handleUpdateItemResponse;
    private final Consumer<ResponsePacket<?>> sessionDetailCallback = this::handleSessionInfoResponse;


    //--STATE--
    private String currentItemId = null;

    public void setupFormMode(String itemId) {
        this.currentItemId = itemId;
        log.info("Item form set to  {} mode", isUpdateMode() ? "Update" : "Create");
        Platform.runLater(() -> {
            if (isUpdateMode()) {
                btnAction.setText("Update");
            } else {
                btnAction.setText("Create");
            }
        });
    }

    private boolean isUpdateMode() {
        return currentItemId != null;
    }

    //--INIT--
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //ImageView
        UIUtil.roundedImageView(imgItem);

        //Choice box
        cbxCategory.getItems().setAll(Category.values());

        //Spinner
        SpinnerValueFactory<Integer> startHourFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0, 1);
        SpinnerValueFactory<Integer> startMinuteFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 1);
        SpinnerValueFactory<Integer> endHourFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0, 1);
        SpinnerValueFactory<Integer> endMinuteFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 1);

        spnStartHour.setValueFactory(startHourFactory);
        spnEndHour.setValueFactory(endHourFactory);
        spnStartMinute.setValueFactory(startMinuteFactory);
        spnEndMinute.setValueFactory(endMinuteFactory);

    }

    //LIFE CYCLE--
    @Override
    public void onShow() {
        if(isUpdateMode()){
            //Subscribe update handler
            ResponseDispatcher.getInstance().subscribe(ActionType.UPDATE_ITEM,updateItemCallback);
            ResponseDispatcher.getInstance().subscribe(ActionType.GET_SESSION_DETAIL,sessionDetailCallback);

            //Request get Item Data
            requestItemDetail(currentItemId);

        } else{
            ResponseDispatcher.getInstance().subscribe(ActionType.CREATE_ITEM,addItemCallback);
        }

    }
    @Override
    public void onHide() {
        if(isUpdateMode()){
            ResponseDispatcher.getInstance().unsubscribe(ActionType.UPDATE_ITEM,updateItemCallback);
            ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_SESSION_DETAIL,sessionDetailCallback);
        } else{
            ResponseDispatcher.getInstance().unsubscribe(ActionType.CREATE_ITEM,addItemCallback);
        }
    }

    //--MAIN METHODS--
    private boolean validateInput(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime, endTime;
        try {
             startTime = LocalDateTime.of(dpStart.getValue(), LocalTime.of(spnStartHour.getValue(), spnStartMinute.getValue()));
             endTime = LocalDateTime.of(dpEnd.getValue(), LocalTime.of(spnEndHour.getValue(), spnEndMinute.getValue()));
        } catch (NullPointerException e) {
            NotificationUtil.showError("Please choose a date!");
            return false;
        }

        if(txtName.getText().isEmpty()){
            NotificationUtil.showError("Item name cannot be empty.");
            return false;
        }
        if(txtStartPrice.getText().isEmpty() || !txtStartPrice.getText().matches("\\d+(\\.\\d+)?")){
            NotificationUtil.showError("StartPrice is invalid.");
            return false;
        }
        if(txtMinBid.getText().isEmpty() || !txtStartPrice.getText().matches("\\d+(\\.\\d+)?")){
            NotificationUtil.showError("Minimum Bid increment is invalid.");
            return false;
        }
        if(cbxCategory.getValue() == null){
            NotificationUtil.showError("Please choose a suitable Category!");
            return false;
        }
        if(startTime.isBefore(now)){
            NotificationUtil.showError("Choosen Date is in the past");
            return false;
        }
        if(!startTime.isBefore(endTime)){
            NotificationUtil.showError("Choosen Date/Time is invalid!");
            return false;
        }

        return true;
    }

    @FXML
    private void handleImageChooser(){
        log.info("[SellerItemsFormController] Khởi tạo cửa sổ chọn ảnh");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");

        // Filter for image files only
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        // 4. Show the open dialog box
        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        // 5. If a file was selected, load it into the ImageView
        if (selectedFile != null) {
            try {
                // Convert file path to a URL string format that JavaFX Image expects
                Image image = new Image(selectedFile.toURI().toString());
                imgItem.setImage(image);

                //Convert images
                imageBytes = Files.readAllBytes(selectedFile.toPath());
                log.info("[SellerItemsFormController] Chuyển đổi ảnh sang bytes thành công");

                //Get image extension
                String fileName = selectedFile.getName();


                // Regex pattern to capture everything include the last dot
                Pattern pattern = Pattern.compile("(\\.[^.]+)$");
                Matcher matcher = pattern.matcher(fileName);

                if (matcher.find()) {
                    // group(1) contains the actual extension text inside the parentheses
                    imageExtension = matcher.group(1).toLowerCase();
                }

            } catch (IOException e){log.error("Cannot convert image file into byte[]");}
        }
    }

    @FXML
    private void handleBtnAction() {

        if(!validateInput()) return;

        //Get Value
        String name = txtName.getText();
        String description = txtDesc.getText();
        BigDecimal startPrice = new BigDecimal(txtStartPrice.getText());
        BigDecimal minBid = new BigDecimal(txtMinBid.getText());
        Category category  = cbxCategory.getValue();
        LocalDateTime startDateTime = LocalDateTime.of(dpStart.getValue(),LocalTime.of(spnStartHour.getValue(),spnStartMinute.getValue()));
        LocalDateTime endDateTime = LocalDateTime.of(dpEnd.getValue(),LocalTime.of(spnEndHour.getValue(),spnEndMinute.getValue()));

        //Send request
        if (isUpdateMode()) {
            requestUpdateItem(currentItemId,name,description,category,imageURL,imageBytes,imageExtension,startPrice,minBid,startDateTime,endDateTime);
        } else {
            requestAddItem(name,description,category,imageBytes,imageExtension,startPrice,minBid,startDateTime,endDateTime);
        }
    }

    public void setData(SessionInfoResponseDTO sessionInfoDTO){
        Platform.runLater(()->{
            if(sessionInfoDTO.getImageUrl()!= null) {
                imageURL = sessionInfoDTO.getImageUrl();
                imgItem.setImage(new Image(imageURL));
                imageBytes = SupabaseUtil.downloadImageBytes(sessionInfoDTO.getImageUrl());
                imageExtension = SupabaseUtil.getExtensionFromUrl(sessionInfoDTO.getImageUrl());
            }
            txtName.setText(sessionInfoDTO.getItemName());

            dpStart.setValue(sessionInfoDTO.getStartTime().toLocalDate());
            dpEnd.setValue(sessionInfoDTO.getEndTime().toLocalDate());
            spnStartHour.getValueFactory().setValue(sessionInfoDTO.getStartTime().getHour());
            spnEndHour.getValueFactory().setValue(sessionInfoDTO.getEndTime().getHour());
            spnStartMinute.getValueFactory().setValue(sessionInfoDTO.getStartTime().getMinute());
            spnEndMinute.getValueFactory().setValue(sessionInfoDTO.getEndTime().getMinute());

            txtStartPrice.setText(sessionInfoDTO.getStartPrice().toString());
            txtMinBid.setText(sessionInfoDTO.getBidStep().toString());
            cbxCategory.setValue(sessionInfoDTO.getCategory());
            txtDesc.setText(sessionInfoDTO.getDescription());
        });
    }

    //--HANDLE REQUEST--
    private void requestAddItem(String name, String description, Category category, byte[] imageBytes,
                                String imageExtension, BigDecimal startPrice,BigDecimal bidStep, LocalDateTime startTime,
                                LocalDateTime endTime){
        ItemCreateDTO itemCreateDTO = new ItemCreateDTO(name, description, category, imageBytes,
                                                        imageExtension, startPrice, bidStep, startTime,
                                                        endTime, null);

        RequestPacket<ItemCreateDTO> request = new RequestPacket<>();
        request.setAction(ActionType.CREATE_ITEM);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setPayload(itemCreateDTO);

        if(!NotificationUtil.showConfirmation("Create new item?","Are you sure???")) return;

        log.info("[SellerItemsFormController] Gửi đăng kí tạo sản phầm đấu giá mới");
        ServerConnection.getInstance().sendRequest(request);
    }

    private void requestUpdateItem(String itemId, String name, String description, Category category, String oldImageURL, byte[] imageBytes,
                                String imageExtension, BigDecimal startPrice,BigDecimal bidStep, LocalDateTime startTime,
                                LocalDateTime endTime){
        ItemUpdateRequestDTO itemUpdateDTO = new ItemUpdateRequestDTO(itemId,name,description,category,oldImageURL,imageBytes,imageExtension,null,startPrice,bidStep,startTime,endTime);

        RequestPacket<ItemUpdateRequestDTO> request = new RequestPacket<>();
        request.setAction(ActionType.UPDATE_ITEM);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setPayload(itemUpdateDTO);

        if(!NotificationUtil.showConfirmation("Update current item?","Are you sure???")) return;

        log.info("[SellerItemsFormController] Gửi đăng kí cập nhật thông tin sản phẩm đấu giá mới");
        ServerConnection.getInstance().sendRequest(request);
    }

    private void requestItemDetail(String sessionId){
            SessionTargetRequestDTO sessionTargetDTO = new SessionTargetRequestDTO(sessionId);

            RequestPacket<SessionTargetRequestDTO> request = new RequestPacket<>();

            request.setToken(ClientSession.getInstance().getCurrentToken());
            request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
            request.setAction(ActionType.GET_SESSION_DETAIL);
            request.setPayload(sessionTargetDTO);

            ServerConnection.getInstance().sendRequest(request);
    }

    //--HANDLE RESPONSE--
    private void handleAddItemResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            NotificationUtil.showInfo("Add item successfully!");
            log.info("[SellerItemsFormController] Thêm sản phẩm thành công.");
            MainViewController.getInstance().loadView(ViewPath.SELLER_ITEMS);
        } else {
            log.error("[SellerItemsFormController] Không thể thêm sản phẩm: {}", response.getMessage());
        }
    }

    private void handleUpdateItemResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            NotificationUtil.showInfo("Update item successfully!");
            log.info("[SellerItemsFormController] Cập nhật thông tin sản phẩm thành công.");
            MainViewController.getInstance().loadView(ViewPath.SELLER_ITEMS);
        } else {
            log.error("[SellerItemsFormController] Không thể cập nhật thông tin sản phẩm: {}", response.getMessage());
        }
    }

    private void handleSessionInfoResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            log.info("[SellerItemsFormController] Đang tải thông tin của sản phẩm.");
            SessionInfoResponseDTO info = (SessionInfoResponseDTO) response.getPayload();
            setData(info);
        } else {
            log.error("[SellerItemsFormController] Không thể tải thông tin của sản phẩm {}", response.getMessage());
        }
    }
}