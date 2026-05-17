package com.uet.BiddingApplication.Controller.SellerController;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Request.ItemCreateDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ServerConnection;
import com.uet.BiddingApplication.Util.NotificationUtil;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.uet.BiddingApplication.BiddingApplication.primaryStage;

public class SellerItemsFormController implements Initializable {
    //--LOG--
    private static final Logger log = LoggerFactory.getLogger(SellerItemsFormController.class);

    //--FXML--
    @FXML ImageView imgItem;
    byte[] imageBytes = null;
    String imageExtension = null;
    @FXML TextArea txtDesc;
    @FXML TextField txtName, txtStartPrice, txtMinBid;
    @FXML ChoiceBox<Category> cbxCategory;
    @FXML DatePicker datePicker;
    @FXML Spinner<Integer> spnStartHour, spnStartMinute, spnEndHour, spnEndMinute;
    @FXML Button btnAction;

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

    //--Initialize--
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

    private boolean validateInput(){
        LocalDateTime now = LocalDateTime.now();
        LocalTime startTime = LocalTime.of(spnStartHour.getValue(),spnStartMinute.getValue());
        LocalTime endTime = LocalTime.of(spnEndHour.getValue(),spnEndMinute.getValue());

        if(txtName.getText().isEmpty()){
            NotificationUtil.showError("Item name cannot be empty.");
            return false;
        }
        if(txtStartPrice.getText().isEmpty() || !txtStartPrice.getText().matches("\\d+")){
            NotificationUtil.showError("StartPrice is invalid.");
            return false;
        }
        if(txtMinBid.getText().isEmpty() || !txtStartPrice.getText().matches("\\d+")){
            NotificationUtil.showError("Minimum Bid increment is invalid.");
            return false;
        }
        if(cbxCategory.getValue() == null){
            NotificationUtil.showError("Please choose a suitable Category!");
            return false;
        }
        if(datePicker.getValue() == null){
            NotificationUtil.showError("Please choose a date!");
            return false;
        }
        if(datePicker.getValue().isBefore(now.toLocalDate())){
            NotificationUtil.showError("Choosen Date is in the past");
            return false;
        }
        if(!startTime.isBefore(endTime)){
            NotificationUtil.showError("Choosen Time is invalid!");
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
        String category  = cbxCategory.getValue().name();
        LocalDateTime startDateTime = LocalDateTime.of(datePicker.getValue(),LocalTime.of(spnStartHour.getValue(),spnStartMinute.getValue()));
        LocalDateTime endDateTime = LocalDateTime.of(datePicker.getValue(),LocalTime.of(spnEndHour.getValue(),spnEndMinute.getValue()));

        //Send request
        if (isUpdateMode()) {
            //TODO:get Item info, place and send request
        } else {
            ItemCreateDTO itemCreateDTO = new ItemCreateDTO(
                    name,description,category,imageBytes,imageExtension,startPrice,minBid,startDateTime,endDateTime,null
            );

            RequestPacket<ItemCreateDTO> request = new RequestPacket<>();
            request.setAction(ActionType.CREATE_ITEM);
            request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
            request.setToken(ClientSession.getInstance().getCurrentToken());
            request.setPayload(itemCreateDTO);

            if(!NotificationUtil.showConfirmation("Create new item?","Are you sure???")) return;

            log.info("[SellerItemsFormController] Gửi đăng kí tạo sản phầm đấu giá mới");
            ServerConnection.getInstance().sendRequest(request);

        }
    }
}