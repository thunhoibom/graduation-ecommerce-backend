# Đặc tả Ngữ cảnh 7 Use Case Trọng yếu — Mono Studio E-Commerce

Tài liệu này cung cấp thông tin chi tiết về luồng nghiệp vụ và kỹ thuật cho 7 Use Case quan trọng nhất của hệ thống Mono Studio, phục vụ cho việc viết đặc tả chi tiết trong báo cáo đồ án.

---

## UC 1: Quy trình Thanh toán & Đặt hàng (Checkout & Payment)
*   **Tác nhân:** Khách hàng (Member) hoặc Khách vãng lai (Guest).
*   **Tiền điều kiện:** Giỏ hàng có ít nhất một sản phẩm hợp lệ và còn tồn kho.
*   **Luồng cơ bản:**
    1.  Người dùng nhấn "Thanh toán" từ giỏ hàng.
    2.  Hệ thống gọi `CartPricingService` để tính toán: Giá gốc -> Áp dụng Khuyến mãi -> Phí vận chuyển (API GHN) -> Tổng tiền cuối cùng.
    3.  Người dùng chọn phương thức thanh toán (VNPay/MoMo/PayOS/COD) và nhập địa chỉ.
    4.  Hệ thống tạo bản ghi `Order` với trạng thái `PENDING` và `UNPAID`.
    5.  Hệ thống tạo link thanh toán và điều hướng người dùng sang cổng thanh toán.
    6.  Sau khi thanh toán xong, cổng thanh toán gọi Webhook/IPN tới `PublicPaymentController`.
    7.  Hệ thống xác nhận thanh toán, chuyển trạng thái đơn hàng sang `PAID` và xác nhận trừ kho vĩnh viễn.
*   **Ghi chú kỹ thuật:** Sử dụng `CheckoutService` để điều phối. Lưu `transactionToken` để đối soát.

---

## UC 2: Hệ thống Giữ chỗ tồn kho (Stock Reservation)
*   **Tác nhân:** Khách hàng / Hệ thống.
*   **Tiền điều kiện:** Sản phẩm được thêm vào giỏ hàng hoặc bắt đầu checkout.
*   **Luồng cơ bản:**
    1.  Khi khách thêm hàng vào giỏ, hệ thống kiểm tra tồn kho khả dụng (`currentStock - reservedStock`).
    2.  Nếu đủ, tạo một bản ghi trong bảng `stock_reservations` với thời gian hết hạn (ví dụ: 15 phút).
    3.  Số lượng "tồn kho ảo" của sản phẩm bị giảm xuống để ngăn người khác mua mất.
    4.  **Nếu thanh toán thành công:** Chuyển trạng thái Reservation thành `CONFIRMED`, trừ `currentStock` thực tế.
    5.  **Nếu hết hạn/Hủy đơn:** Hệ thống (background task) tự động xóa bản ghi reservation và hoàn lại tồn kho khả dụng.
*   **Ghi chú kỹ thuật:** Xử lý nguyên tử (Atomic update) trong Database để tránh Race Condition.

---

## UC 3: Tìm kiếm nâng cao với Elasticsearch & Kafka
*   **Tác nhân:** Khách hàng / Hệ thống.
*   **Luồng cơ bản:**
    1.  **Đồng bộ:** Mỗi khi Admin thay đổi thông tin sản phẩm, `KafkaOrderProducer` gửi một message vào Kafka topic `product-index-events`.
    2.  `ElasticsearchSyncConsumer` lắng nghe và cập nhật dữ liệu vào index của Elasticsearch.
    3.  **Tìm kiếm:** Khi khách hàng tìm kiếm trên giao diện, `PublicProductsController` sẽ gửi query tới Elasticsearch thay vì Database.
    4.  Elasticsearch trả về kết quả tìm kiếm mờ (Fuzzy), hỗ trợ tiếng Việt có dấu/không dấu và tốc độ cực nhanh.
*   **Ghi chú kỹ thuật:** Tách biệt database nghiệp vụ (PostgreSQL) và database tìm kiếm (ES) để tối ưu hiệu năng.

---

## UC 4: Động cơ Khuyến mãi & Giảm giá (Promotion Engine)
*   **Tác nhân:** Hệ thống (tự động áp dụng) / Quản trị viên (thiết lập).
*   **Luồng cơ bản:**
    1.  Admin thiết lập `PromotionRule` (ví dụ: Giảm 20% cho danh mục Áo thun nếu đơn hàng > 500k).
    2.  Khi tính giá giỏ hàng, `PromotionRulesService` sẽ duyệt qua tất cả các Rule đang kích hoạt.
    3.  Kiểm tra `PromotionRuleCondition` (Điều kiện): Đơn hàng có thỏa mãn không?
    4.  Nếu thỏa mãn, thực thi `PromotionRuleAction` (Hành động): Trừ tiền hoặc miễn phí vận chuyển.
    5.  Hệ thống ghi lại `DiscountUsage` để giới hạn số lần sử dụng của mỗi khách hàng.
*   **Ghi chú kỹ thuật:** Logic ưu tiên (Priority) để áp dụng các khuyến mãi chồng nhau hoặc loại trừ nhau.

---

## UC 5: Quản lý Kho & Chuỗi cung ứng (Inventory Management)
*   **Tác nhân:** Nhân viên kho / Quản trị viên.
*   **Luồng cơ bản:**
    1.  **Nhập hàng:** Tạo `PurchaseOrder` -> Khi hàng về tạo `GoodsReceipt` -> Hệ thống tự động tăng `currentStock`.
    2.  **Điều chuyển:** Tạo `StockTransfer` để chuyển hàng từ kho A sang kho B.
    3.  **Kiểm kê:** Tạo `StockCountSession`, nhân viên nhập số lượng thực tế -> Hệ thống tính toán chênh lệch và tạo `StockAdjustment` để khớp dữ liệu.
*   **Ghi chú kỹ thuật:** Mọi biến động đều được ghi vào `ProductAuditLog` để truy vết (ai sửa, sửa lúc nào, giá trị cũ/mới).

---

## UC 6: Bảo mật & Phân quyền (Security & RBAC)
*   **Tác nhân:** Tất cả người dùng.
*   **Luồng cơ bản:**
    1.  Người dùng đăng nhập -> Hệ thống cấp một JWT (JSON Web Token) chứa thông tin định danh và quyền.
    2.  Mọi request tiếp theo phải gửi kèm Token này trong Header.
    3.  `JwtTokenVerifierFilter` kiểm tra tính hợp lệ của Token.
    4.  `UserDetailsServiceImpl` nạp danh sách `Permission` từ DB gắn với Role của người dùng.
    5.  Sử dụng `@PreAuthorize` trên các Controller để chặn các truy cập trái phép (ví dụ: Khách không được gọi API xóa sản phẩm).
*   **Ghi chú kỹ thuật:** Bảo mật không trạng thái (Stateless), an toàn và dễ mở rộng.

---

## UC 7: Tích hợp Vận chuyển GHN (Shipping Integration)
*   **Tác nhân:** Hệ thống / Quản trị viên.
*   **Luồng cơ bản:**
    1.  Sau khi đơn hàng được thanh toán, Admin nhấn "Giao hàng".
    2.  Hệ thống gọi API của Giao Hàng Nhanh (GHN) để tạo vận đơn.
    3.  GHN trả về `TrackingNumber`. Hệ thống lưu lại và hiển thị cho khách hàng.
    4.  Shipper đi giao hàng, trạng thái trên hệ thống GHN thay đổi.
    5.  GHN gọi Webhook tới `WebhookShippingController` của hệ thống để cập nhật: Đang giao -> Giao thành công -> Đã đối soát tiền.
*   **Ghi chú kỹ thuật:** Xử lý lỗi kết nối API và đảm bảo dữ liệu trạng thái luôn khớp với thực tế vận chuyển.
