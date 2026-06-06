<div align="center"> <img src="https://raw.githubusercontent.com/Anhdong/BiddingApplication/refs/heads/ReadMe/assets/banner.jpg"> <h1>✨Bidding Application</h1>

# 📷Screenshots
<p align="center">
  <img src="https://raw.githubusercontent.com/Anhdong/BiddingApplication/refs/heads/ReadMe/assets/login.png" width="410" />
  <img src="https://raw.githubusercontent.com/Anhdong/BiddingApplication/refs/heads/ReadMe/assets/items.png" width="410" />
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/Anhdong/BiddingApplication/refs/heads/ReadMe/assets/auction.png" width="410" />
  <img src="https://raw.githubusercontent.com/Anhdong/BiddingApplication/refs/heads/ReadMe/assets/history.png" width="410" />
</p>

# 💻Bài toán phạm vi hệ thống
### Mục tiêu  
**BiddingApplication** mô phỏng quy trình đấu giá điện tử đầy đủ trên nền desktop (Java), đồng thời thực hành toàn diện OOP, kiến trúc Client-Server, xử lý đồng thời, kết nối CSDL và xây dựng giao diện JavaFX. Cụ thể:  
 - **Hệ thống Client-Server:** Server xử lý nghiệp vụ qua Socket TCP, Client giao diện JavaFX — toàn bộ logic nghiệp vụ phía Server.
 - **OOP & Design Pattern chuẩn:** phân cấp lớp rõ ràng (User → Bidder/Seller), áp dụng DAO, Singleton, Observer, MVC đúng ngữ cảnh.
 - **An toàn đồng thời:** nhiều Client bid cùng lúc không gây race condition/lost update nhờ SERIALIZABLE transaction.
 - **Realtime:** tất cả Client nhận giá mới tức thì; quy trình phát triển chuẩn: 
    - Maven
    - JUnit 5 + Mockito
    - GitHub Actions
    - Logback

### Stack kỹ thuật
- Java 25
- JavaFX 25 + AtlantaFX 2.0 + Ikonli 12.4
- PostgreSQL + HikariCP 6.2
- BCrypt 0.4
- Gson 2.12
- JUnit 5.11 + Mockito 5.14
- Logback 1.5 
- Maven + GitHub Actions 
- dotenv-java 3.2

### Nghiệp vụ 
 Hai vai trò Seller và Bidder; luồng đầy đủ đăng ký → tạo phiên → đặt bid → kết thúc → công bố kết quả; lịch sử bid minh bạch. 

### Giới hạn
 Ứng dụng desktop LAN/localhost, chưa thể thực hiện thanh toán thật, mục đích học thuật và nghiên cứu.

# 🕹️Công nghệ, môi trường và cài đặt
### Công nghệ sử dụng  
 - **Ngôn ngữ:** Java 25
 - **Giao diện (Client):** JavaFX (tích hợp AtlantaFX theme, Ikonli icons)
 - **Kết nối mạng:** TCP Socket (Truyền tải Request/Response Packet), UDP Broadcast (Tự động rò tìm IP Server trên mạng LAN)
 - **Cơ sở dữ liệu:** PostgreSQL (lưu trữ trên Supabase)
 - **Quản lý dự án:** Maven (Multi-module)
 - **CI/CD:** Github Actions (Hỗ trợ chạy test và build tự động)

### Môi trường chạy  
 Ứng dụng hỗ trợ đa nền tảng (Cross-platform) bao gồm Windows, macOS và Linux.

### Yêu cầu cài đặt:  
 - Hệ máy cần cài đặt sẵn JDK 25.
 - Cài đặt Apache Maven (phiên bản 3.x trở lên).
 - Kết nối mạng ổn định để kết nối với cơ sở dữ liệu Supabase.

# 📂Cấu trúc thư mục
```text
    # 3 Module chính
    BiddingApplication/                                                                                              
    ├── README.md                                                                                                    
    ├── pom.xml                        # Root POM (Quản lý các module)                                               
    │                                                                                                                
    ├── client/                        # Module Client (JavaFX UI)                                                   
    │   ├── pom.xml                                                                                                  
    │   └── src/                                                                                                     
    │       └── main/                                                                                                
    │           ├── java/com/uet/BiddingApplication/                                                                 
    │           │   ├── Controller/    # Các controllers xử lý logic giao diện                                       
    │           │   ├── Enum/          # Các enums dành riêng cho client                                             
    │           │   ├── Interface/                                                                                   
    │           │   ├── Session/       # Quản lý phiên làm việc ở client                                             
    │           │   └── Util/          # Các tiện ích phía client                                                    
    │           │                                                                                                    
    │           └── resources/         # Tài nguyên tĩnh của ứng dụng GUI                                                                       
    │                                                                                                                
    ├── common/                        # Module Common (Dùng chung cho cả Client & Server)                           
    │   ├── pom.xml                                                                                                  
    │   └── src/                                                                                                     
    │       └── main/                                                                                                
    │           └── java/com/uet/BiddingApplication/                                                                 
    │               ├── DTO/           # Data Transfer Objects (Packet, Request, Response)                           
    │               ├── Enum/          # Các hằng số/enum dùng chung                                                 
    │               ├── Exception/     # Xử lý ngoại lệ                                                              
    │               ├── Model/         # Các Entity/Model chính của hệ thống                                         
    │               └── Utils/         # Các Mapper, Factory dùng chung
    │
    └── server/                        # Module Server (Core logic & Database)
        ├── pom.xml
        └── src/
            ├── main/
            │   ├── java/com/uet/BiddingApplication/
            │   │   ├── Config/        # Cấu hình server
            │   │   ├── CoreService/   # Các service cốt lõi (BidProcessing, Cache, Scheduler)
            │   │   ├── DAO/           # Data Access Object (Tương tác database)
            │   │   ├── Launcher/      # Điểm khởi chạy server
            │   │   ├── ServerClass/   # Xử lý kết nối TCP/UDP, Request Router
            │   │   ├── Service/       # Business logic (Admin, Auction, Auth, Bidder, Seller)
            │   │   └── Utils/         # Quản lý kết nối Database, Storage
            │   │
            │   └── resources/
            │
            └── test/                  # Unit tests cho Server

```

# 📖Hướng dẫn chạy chương trình
## Hướng dẫn cài đặt và Thiết lập (Setup)

### **1. Yêu cầu tiền quyết (Prerequisites)**  
 Để cài đặt và chạy được dự án, máy tính của bạn cần được cài đặt sẵn các công cụ sau:

 - **Git**: Dùng để clone mã nguồn.
 - **Java Development Kit (JDK)**: Bắt buộc sử dụng JDK 25.
 - **Apache Maven**: Công cụ quản lý thư viện và đóng gói dự án.
 - **Cơ sở dữ liệu (Database)**: Dự án sử dụng PostgreSQL thông qua nền tảng Supabase. Bạn cần chuẩn bị sẵn một project trên Supabase.

### **2. Tải mã nguồn về máy**  
 Mở **Terminal** (trên macOS/Linux) hoặc **Command Prompt/PowerShell** (trên Windows) và chạy lệnh:

```Bash
git clone https://github.com/Anhdong/BiddingApplication.git
cd BiddingApplication
```

### **3. Thiết lập Cơ sở dữ liệu (Database Configuration)**  

- Để Server có thể kết nối với cơ sở dữ liệu, bạn cần cấu hình thông tin kết nối vào file properties.
- Đi tới đường dẫn: **server/src/main/resources/application.properties**
- Mở file và cập nhật các thông số cấu hình kết nối trỏ tới Supabase của bạn (ví dụ minh họa):  
```Properties
# Thông tin cấu hình kết nối DB
db.url=jdbc:postgresql://<supabase-host>:5432/postgres
db.username=<your-supabase-username>
db.password=<your-supabase-password>
```

> [!NOTE]
>Hãy đảm bảo bạn đã chạy các script SQL để khởi tạo bảng và schema cần thiết cho hệ thống trên Supabase trước khi khởi chạy.

### **4. Thiết lập Lưu trữ ảnh (Supabase Storage)**  
Do ứng dụng có chức năng quản lý, tải xuống và đọc các định dạng ảnh của vật phẩm thông qua URL từ Supabase, bạn cần thiết lập một Storage Bucket trên Supabase, thiết lập quyền (policies) cho phép đọc/ghi public để hiển thị ảnh trên UI JavaFX một cách chính xác.

## Câu lệnh dòng lệnh để chạy chương trình & Hướng dẫn chạy Server/Client

### **Bước 1:** Biên dịch và đóng gói (Build)

- Ứng dụng sử dụng Maven, do đó các câu lệnh build là độc lập với hệ điều hành. 
- Mở **Terminal** (MacOS/Linux) hoặc **Command Prompt/PowerShell** (Windows) tại thư mục gốc của dự án:
```Bash
mvn clean package
```

- Lệnh này sẽ tạo ra hai file thực thi đa nền tảng: **server-executable.jar** và **client-executable.jar**.

### **Bước 2:** Khởi chạy Server (Bắt buộc chạy trước)  

- Bạn cần khởi chạy Server trước để mở cổng kết nối Socket và UDP Discovery. 
- Di chuyển vào thư mục target của server và chạy bằng môi trường Java:
- Trên Windows, Linux, hoặc macOS:
```Bash
cd server/target
java -jar server-executable.jar
```
### **Bước 3:** Khởi chạy Client (Chạy sau khi Server đã bật)

- Mở một cửa sổ **Terminal/Command Prompt** mới để khởi chạy ứng dụng Client.
- Trên Windows, Linux, hoặc macOS:
```Bash
cd client/target
java -jar client-executable.jar
```

> [!NOTE]
> Khi thực hiện lệnh trên Windows có thể cần dùng **\\** thay cho **/** trong đường dẫn thư mục.

# 📑Danh sách chức năng
### 1. Thiết kế lớp & cây kế thừa
 - [x] Các lớp chính (User, Bidder, Seller, Item, Auction, BidTransaction…)   
 - [x] Nguyên tắc OOP (Encapsulation, Inheritance, Polymorphism, Abstraction)   
 - [x] Design Pattern (DAO, Singleton, Observer, MVC)   
### 2. Chức năng chính
 - [x] Quản lý người dùng & sản phẩm   
 - [x] Chức năng đấu giá (Đặt bid, kiểm tra, xác định người thắng)   
 - [x] Xử lý lỗi & ngoại lệ   
### 3. Kỹ thuật quan trọng & Concurrency
 - [x] Đấu giá đồng thời an toàn (Lost update, race condition, rollback)   
 - [x] Realtime update (Observer/Socket — broadcast đến tất cả Client)   
### 4. Tích hợp, kiến trúc & chất lượng mã
 - [x] Kiến trúc Client-Server rõ ràng   
 - [x] MVC (JavaFX+FXML; Controller-Service-DAO)   
 - [x] Maven, coding convention, mã nguồn sạch   
 - [x] Unit Test (JUnit 5 + Mockito)   
 - [x] CI/CD (GitHub Actions — test tự động)   
### 5. Chức năng nâng cao (Tùy chọn)
 - [x] Auto-Bidding (maxBid, increment, PriorityQueue)   
 - [x] Anti-sniping (Gia hạn khi bid cuối)   
 - [x] Bid History Visualization (Line chart realtime)   
### 6. Tính năng bổ sung ngoài barem (Nhóm tự thêm)
 - [x] Admin Panel (Ban user, hủy phiên có OTP)  
 - [x] UDP Auto-Discovery (Client tự tìm Server trong LAN)   
 - [x] Upload ảnh Supabase Storage (Ảnh sản phẩm thật)   
 - [x] In-Memory Cache (SearchCacheManager — giảm tải DB)   
 - [x] Retry DB tự động (Kết thúc phiên an toàn)   
 - [x] Reconnect & Quản lý hồ sơ (Khôi phục session tự động)   
 - [x] CI/CD 2 jobs + Cross-platform
