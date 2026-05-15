Tạo đơn hàng
API Tạo đơn hàng
 
Tự động truyền thông tin đơn hàng sang GHN các thông tin như kích thước, cân nặng, số điện thoại và nhiều thông tin khác. Từ đó sẽ tạo đơn giao nhận.

Lưu ý : Ở API /shiip/public-api/v2/shipping-order/create cần truyền token api và shopid ở header. 

 

 

post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/create
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/create
Curl
curl --location --request POST 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/create' \
--header 'Content-Type: application/json' \
--header 'ShopId: 885' \
--header 'Token: 285518-c4bb-11ea-be3a-f636b1deefb9' \
--data-raw '{
    "payment_type_id": 2,
    "note": "Tintest 123",
    "required_note": "KHONGCHOXEMHANG",
    "return_phone": "0332190158",
    "return_address": "39 NTT",
    "return_district_id": null,
    "return_ward_code": "",
    "client_order_code": "",
    "from_name": "TinTest124",
    "from_phone": "0987654321",
    "from_address": "72 Thành Thái, Phường 14, Quận 10, Hồ Chí Minh, Vietnam",
    "from_ward_name": "Phường 14",
    "from_district_name": "Quận 10",
    "from_province_name": "HCM",
    "to_name": "TinTest124",
    "to_phone": "0987654321",
    "to_address": "72 Thành Thái, Phường 14, Quận 10, Hồ Chí Minh, Vietnam",
    "to_ward_name": "Phường 14",
    "to_district_name": "Quận 10",
    "to_province_name": "HCM",
    "cod_amount": 200000,
    "content": "Theo New York Times",
    "length": 12,
    "width": 12,
    "height": 12,
    "weight": 1200,
    "cod_failed_amount": 2000,                  
    "pick_station_id": 1444,
    "deliver_station_id": null,
    "insurance_value": 10000000,
    "service_type_id": 2,
    "coupon": null,
    "pickup_time": 1692840132,
    "pick_shift": [2],
    "items": [
    {
        "name": "Áo Polo",
        "code": "Polo123",
        "quantity": 1,
        "price": 200000,
        "length": 12,
        "width": 12,
        "height": 12,
        "weight": 1200,
        "category": 
        {
            "level1": "Áo"
        }
    }]
}'
                            
Cấu trúc Request
Trường dữ liệu	Bắt buộc	Kiểu dữ liệu	Độ rộng dữ liệu	Mô tả
token	
X
String	 	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

shop_id	
X
Int	 	
Mã đơn hàng của GHN trả về cho khách hàng.

from_name	 	String	1024	
Tên người gửi

Trường hợp nào nếu không truyền thông tin người gửi thì hệ thống sẽ mặc định lấy thông tin ở ShopID
from_phone	 	String	 	
Số điện thoại người gửi.

from_address	 	String	1024	
Địa chỉ người gửi.

from_ward_name	 	String	 	
Phường/Xã của người gửi hàng.

from_district_name	 	String	 	
Quận/Huyện của người gửi hàng. (Trường hợp dùng đơn vị hành chính mới 2 cấp thì tham khảo tài liệu này Link)

from_province_name	 	String	 	
Tỉnh của người gửi hàng.

to_name	
X
String	 1024	
Tên người nhận hàng.

to_phone	
X
String	 	
Số điện thoại người nhận hàng.

to_address	
X
String	 1024	
Địa chỉ Shiper tới giao hàng.

to_ward_name	
X
String	 	
Phường/Xã của người nhận hàng.

to_district_name	
X
String	 	
Quận/Huyện của người nhận hàng. (Trường hợp dùng đơn vị hành chính mới 2 cấp thì tham khảo tài liệu này Link)

to_province_name	X	String	 	
Tỉnh của người nhận hàng.

return_phone	 	String	 	
Số điện thoại trả hàng khi không giao được.

return_address	 	String	 1024	
Địa chỉ trả hàng khi không giao được.

return_district_name	 	String	 	
Quận/Huyện của người nhận hàng trả.

return_ward_name	 	String	 	
Phường/Xã của người nhận hàng trả.

return_province_name	 	String	 	Tỉnh của người nhận hàng trả.
client_order_code	 	String	 50	
Mã đơn hàng riêng của khách hàng.

Giá trị mặc định: null

Lưu ý: Client_order_code thì sẽ lấy lại đơn hàng đã có Client_order_code này

cod_amount	 	Int	 	
Tiền thu hộ cho người gửi.

Maximum: 50.000.000

Giá trị mặc định: 0

content	 	String	 2000	
Nội dung của đơn hàng.

weight	 	Int	 	
Khối lượng của đơn hàng (gram). Bắt buộc khi truyền service_type_id = 2

Tối đa: 50.000 gram
length	 	Int	 	
Chiều dài của đơn hàng (cm). Bắt buộc khi truyền service_type_id = 2

Tối đa: 200 cm
width	 	Int	 	
Chiều rộng của đơn hàng (cm). Bắt buộc khi truyền service_type_id = 2

Tối đa: 200 cm
height	 	Int	 	
Chiều cao của đơn hàng (cm). Bắt buộc khi truyền service_type_id = 2

Tối đa: 200 cm
pick_station_id	 	Int	 	
Mã bưu cục để gửi hàng tại điểm.

Giá trị mặc định: null

Giá trị truyền vào > 0

insurance_value	 	Int	 	
Giá trị của đơn hàng ( Trường hợp mất hàng , bể hàng sẽ đền theo giá trị của đơn hàng).

Tối đa 5.000.000

Giá trị mặc định: 0

coupon	 	String	 	
Mã giảm giá.

service_type_id	
X
Int	 	
Mã loại dịch vụ: Gọi API lấy gói dịch vụ để lấy mã loại dịch vụ.

Mã loại dịch vụ cố định. Trong đó:  2: Hàng nhẹ, 5: Hàng nặng

Hàng nhẹ sử dụng length, width, height và weight

Hàng nặng sử dụng items[].length, items[].width, items[].height và items[].weight

payment_type_id	
X
Int	 	
Mã người thanh toán phí dịch vụ.

1: Người bán/Người gửi.

2: Người mua/Người nhận.

note	 	String	 5000	
Người gửi ghi chú cho tài xế.

required_note	
X
String	 500	
Ghi chú bắt buộc, Bao gồm: CHOTHUHANG, CHOXEMHANGKHONGTHU, KHONGCHOXEMHANG

CHOTHUHANG nghĩa là Người mua có thể yêu cầu xem và dùng thử hàng hóa

CHOXEMHANGKHONGTHU nghĩa là Người mua được xem hàng nhưng không được dùng thử hàng

KHONGCHOXEMHANG nghĩa là Người mua không được phép xem hàng

pick_shift	 	 Array	 	
Dùng để truyền ca lấy hàng, Sử dụng API Lấy danh sách ca lấy

pickup_time	 	Int	 	
Truyền thời gian mong muốn lấy hàng, định dạng UnixtimeStamp.

Items	 	 Array	 	
Thông tin sản phẩm.

Bắt buộc truyền items khi sử dụng gói dịch vụ Hàng nặng
items[].name	 	String	 	
Tên của sản phẩm.

items[].code	 	String	 	
Mã của sản phẩm.

items[].quantity	 	Int	 	
Số lượng của sản phẩm.

items[].price	 	Int	 	
Giá của sản phẩm.

items[].length	 	Int	 	
Chiều dài của sản phẩm. Hàng nặng đi nhiều kiện thì bắt buộc phải truyền length

items[].width	 	Int	 	
Chiều rộng của sản phẩm. Hàng nặng đi nhiều kiện thì bắt buộc phải truyền width

items[].weight	 	Int	 	
Đối với trường hợp chọn gói dịch. Hàng nặng đi nhiều kiện thì bắt buộc phải truyền weight

items[].height	 	Int	 	
Chiều cao của sản phẩm. Hàng nặng đi nhiều kiện thì bắt buộc phải truyền height

items[].category	 	 Object	 	
Danh mục sản phẩm được phân chia 3 cấp độ level1, level2, level3

items[].category.level1	 	String	 	
Danh mục cấp 1

cod_failed_amount	 	Int	 	
Thu thêm tiền khi giao hàng thất bại

 

Danh sách Tỉnh, Quận/Huyện, Phường/Xã
Link

 
GHN có 2 dịch vụ hàng nhẹ và hàng nặng
Hàng nhẹ có service_type_id = 2, kích thước/khối lượng tính cước length, width, height và weight
Hàng nặng có service_type_id = 5, kích thước/khối lượng tính cước lấy trong items (items[].length, items[].width, items[].height và items[].weight)
Mỗi item là 1 kiện hàng
GHN tính toán kích thước/khối lượng của Items rồi cập nhật lại kích thước/khối lượng của đơn hàng theo công thức: Max(length), Max(width), Sum(height)
Với mỗi item, kích thước dài nhất được tính là Dài, kích thước nhỏ nhất được tính là Cao
Khối lượng quy đổi được tính theo công thức:
(Length x Width x Height) / 5 ; So sánh khối lượng quy đổi và trọng lượng thực tế, giá trị nào lớn hơn sẽ là khối lượng tính cước.
 

Success 200
{
    "code": 200,
    "message": "Success",
    "data": 
    {
        "order_code":"FFFNL9HH",
        "sort_code":"19-60-06",
        "trans_type":"truck",
        "ward_encode":"",
        "district_encode":"", 
        "fee":
        { 
            "main_service":22000,   
            "insurance":11000, 
            "station_do":0,
            "station_pu":0, 
            "return":0, 
            "r2s":0,
            "coupon":0,
            "cod_failed_fee":0,
        },
        "total_fee":"33000"
        "expected_delivery_time":"2020-06-03T16:00:00Z" 
    },
    "message_display":"Tạo đơn hàng thành công. Mã đơn hàng: FFFNL9HH"
}
Cấu trúc Response
Trường dữ liệu	Mô tả
expected_delivery_time	
Thời gian giao dự kiến.

fee	
Phí.

coupon	
Giá trị khuyến mãi.

insurance	
Phí khai giá hàng hóa.

main_service	
Phí vận chuyển.

r2s	
Phí giao lại hàng.

return	
Phí hoàn hàng.

station_do	
Phí gửi hàng tại bưu cục.

station_pu	
Phí lấy hàng tại bưu cục.

order_code	
Mã đơn hàng.

sort_code	
Mã phân loại.

total_fee	
Tổng phí dịch vụ.

trans_type	
Loại vận chuyển.

Error-Response
{
    "code": 400,
    "message": "Sai thông tin Required Note",
    "data": null,
    "code_message":"USER_ERR_COMMON"
}



Chi tiết của đơn hàng bằng client_order_code
API chi tiết đơn hàng bằng client_order_code
 
Sử dụng API /shiip/public-api/v2/shipping-order/detail-by-client-code để lấy chi tiết thông tin của đơn hàng bằng client_order_code.

Lưu ý : API /shiip/public-api/v2/shipping-order/detail-by-client-code cần truyền token api ở header.

 

post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/detail-by-client-code
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/detail-by-client-code
Curl
curl --location --request POST 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/detail-by-client-code' \
--header 'Content-Type: application/json' \
--header 'Token: f48a8928-9fcb-11ea-b2e69541627f' \
--data-raw '{
    "client_order_code": "Tin1234567"
}'
            
            
Cấu trúc Request
Trường dữ liệu	Kiểu dữ liệu	Mô tả
token	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

client_order_code	String	
Mã đơn hàng riêng của Khách hàng.

Success 200
{
                "code": 200,
                "message": "Success",
                "data":[
                {
                "shop_id":855,
                "client_id":500379,
                "return_name":"Lastmile",
                "return_phone":0332190458,
                "return_address":"310 Quan Nhân",
                "return_ward_code":"1A0710",
                "return_district_id":1493,
                "return_location":{
                    "lat":20.998386,
                    "long":105.80931,
                    "cell_code":"AHQAZZJE",
                    "place_id":EkIzMTAgUXVhbiBOaMOibiwgVGhhbmggWHXDom4gVHJ1bmcsIFRoYW5oIFh1w6JuLCBIw6AgTuG7mWksIFZpZXRuYW0iGxIZChQKEgkRLtUSl6w1MREaWf6DtHtu2hC2Ag,
                    "trust_level":5,
                    "wardcode":"1A0710"
                    },
                "from_name":"Nguyen",
                "from_phone":0332190458,
                "from_address":"310 Quan Nhân"",
                "from_ward_code":"1A0710",
                "from_district_id":1493,
                "from_location":{
                    "lat":20.998386,
                    "long":105.80931,
                    "cell_code":"AHQAZZJE",
                    "place_id":EkIzMTAgUXVhbiBOaMOibiwgVGhhbmggWHXDom4gVHJ1bmcsIFRoYW5oIFh1w6JuLCBIw6AgTuG7mWksIFZpZXRuYW0iGxIZChQKEgkRLtUSl6w1MREaWf6DtHtu2hC2Ag,
                    "trust_level":5,
                    "wardcode":"1A0710",
                    },
                "deliver_station_id":0,
                "to_name":"Tindeptrai",
                "to_phone":0987654321,
                "to_address":"48 Bùi Thị Xuân",
                "to_ward_code":"21302",
                "to_district_id":1461,
                "to_location":{
                    "lat":10.810695,,
                    "long":106.682046,,
                    "cell_code":"AJIAEQJQ",
                    "place_id":"ChIJz-YRGgQvdTERoKHzo8O2L8g",
                    "trust_level":5,
                    "wardcode":"21302",
                    },
                "weight":2,
                "length":10,
                "width":10,
                "height":10,
                "converted_weight":200,
                "image_ids":"null",
                "service_type_id":2,
                "service_id":53321,
                "payment_type_id":2,
                "payment_type_ids":[
                    2
                ],
                "custom_service_fee":0,
                "sort_code":"0-000-0-A4",
                "cod_amount":0,
                "cod_collect_date":"null",
                "cod_transfer_date":"null",
                "is_cod_transferred":"false",
                "is_cod_collected":"false",
                "insurance_value":0,
                "order_value":0,
                "pick_station_id":0,
                "client_order_code":"Tin1234567",
                "required_note":"KHONGCHOXEMHANG",
                "content":"Tindeptrai",
                "note":"",
                "employee_note":"",
                "seal_code":"",
                "pickup_time":"2021-11-11T03:04:23.928Z",
                "items":[
                {
                    name:"test",
                    quantity:1,
                    category:{},
                    weight:2,
                }
                ],
                "coupon":"",
                "_id":"5ed11d4f1eb594a402473300",
                "order_code":"Z82T1",
                "version_no":"c01697ec-6190-4b2a-aed9-dd5938a9cf11",
                "updated_ip":35.247.155.234,
                "updated_employee":1005,
                "updated_client":0,
                "updated_source":"lastmile",
                "updated_date":"2021-11-11T03:07:56.882Z",
                "updated_warehouse":0,
                "created_ip":35.247.155.234,
                "created_employee":0,
                "created_client":500379,
                "created_source":"shiip/5sao",
                "created_date":"2021-11-11T03:04:23.461Z",
                "status":"return",
                "pick_warehouse_id":2461,
                "deliver_warehouse_id":1364,
                "current_warehouse_id":2461,
                "return_warehouse_id":2271,
                "next_warehouse_id":0,
                "leadtime":"2021-11-13T23:59:59Z",
                "order_date":"2021-11-11T03:04:23.928Z",
                "data":"{}",
                "soc_id":"5ed121cb1eb594a402473301",
                "s2r_time":"2021-11-11T03:07:53.64Z",
                "return_time":"2021-11-11T03:07:53.64Z",
        
                "finish_date":"null",
                "tag":[
                "air"
                ],
                 "log":[
                 {
                 "status": "picking",
                 "payment_type_id": "2",
                 "updated_date": "2021-11-11T03:04:48.053Z"
                },
                {
                 "status": "picked",
                 "payment_type_id": "2",
                 "updated_date": "2021-11-11T03:04:48.053Z"
                },
                {
                    "status": "delivering",
                    "payment_type_id": "2",
                    "updated_date": "2021-11-11T03:07:47.245Z"
                },
                {
                    "status": "delivery_fail",
                    "payment_type_id": "2",
                    "updated_date": "2021-11-11T03:07:53.554Z"
                },
                {
                    "status": "waiting_to_return",
                    "payment_type_id": "2",
                    "updated_date": "2021-11-11T03:07:53.64Z"
                },
                {
                    "status": "return",
                    "payment_type_id": "2",
                    "updated_date": "2021-11-11T03:07:56.882Z"
                }
                ],
                "is_partial_return": "false",
                "is_partial_return": [
                        "FA87OAE"
                ]
                 }
            }
Cấu trúc Response
Trường dữ liệu	Mô tả
shop_id	
Mã cửa hàng.

client_id	
Thông tin cá nhân.

return_name	
Tên người nhận hoàn hàng.

return_phone	
Số điện thoại người nhận hoàn hàng.

return_address	
Địa chỉ người nhận hoàn hàng.

return_ward_code	
Phường/Xã người nhận hoàn hàng.

return_district_id	
Quận/Huyện người nhận hoàn hàng.

from_name	
Tên người gửi.

from_phone	
Số điện thoại người gửi.

from_address	
Địa chỉ người gửi.

from_ward_code	
Phường/Xã người gửi.

from_district_id	
Quận/Huyện người gửi.

deliver_station_id	
Mã bưu cục giao hàng.

to_name	
Tên người nhận.

to_phone	
Số điện thoại người nhận.

to_address	
Địa chỉ người nhận.

to_ward_code	
Phường/Xã người nhận.

to_district_id	
Quận/Huyện người nhận.

weight	
Cân nặng.

length	
Chiều dài.

width	
Chiều rộng.

height	
Chiều cao.

converted_weight	
Khối lượng quy đổi.

service_type_id	
Mã loại hình dịch vụ.

service_id	
Mã dịch vụ.

payment_type_id	
Mã người thanh toán phí dịch vụ.

cod_amount	
Tiền thu hộ.

cod_collect_date	
Ngày thu tiền thu hộ

cod_transfer_date	
Ngày chuyển tiền thu hộ.

insurance_value	
Phí khai giá hàng hóa.

pick_station_id	
Mã bưu cục để gửi hàng tại điểm.

client_order_code	
Mã đơn hàng riêng của Khách hàng.

required_note	
Ghi chú bắt buộc.

content	
Nội dung.

pickup_time	
Thời gian lấy dự kiến.

note	
Ghi chú.

employee_note	
Nhân viên ghi chú.

coupon	
Giá trị khuyến mãi.

order_code	
Mã đơn hàng.

sort_code	
Mã vận hành.

updated_ip	
Ip cập nhật.

updated_employee	
Nhân viên cập nhật.

updated_client	
Client cập nhật.

updated_source	
Nguồn cập nhật.

updated_date	
Ngày cập nhật.

updated_warehouse	
Bưu cục cập nhật.

created_ip	
Ip tạo.

created_employee	
Nhân viên tạo.

created_client	
Client tạo.

created_source	
Nguồn tạo.

created_date	
Ngày tạo.

status	
Trạng thái.

pick_warehouse_id	
Kho lấy.

deliver_warehouse_id	
Kho giao.

current_warehouse_id	
Kho hiện tại.

return_warehouse_id	
Kho trả.

next_warehouse_id	
Kho kế tiếp

leadtime	
Thời gian giao dự kiến.

order_date	
Ngày tạo đơn.

finish_date	
Ngày giao hàng thành công.

Error-Response
{
                "code": 400,
                "message": "code=401, message=Token is not valid!",
                "data": null
            }




            Lấy danh sách ca lấy
API danh sách ca lấy hàng
 
Sử dụng API /shiip/public-api/v2/shift/date để lấy danh sách các ca lấy hàng

Lưu ý : API /shiip/public-api/v2/shift/date cần truyền token api  ở header.

post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/shift/date
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shift/date
Curl
curl --location --request GET 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shift/date' \
--header 'token: f4928-9fcb-11ea-b3e1-b2e69541627f'
Cấu trúc Request
Trường dữ liệu	Kiểu dữ liệu	Mô tả
token	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

Success 200
{
    "code": 200,
    "message": "Success",
    "data":[  
    {
        "id": 2 
        "title": "Ca lấy 12-03-2021 (12h00 - 18h00)",
        "from_time": 43200,
        "to_time": 64800
    }, 
    {
        "id": 3 
        "title": "Ca lấy 13-03-2021 (7h00 - 12h00)",
        "from_time": 111600,
        "to_time": 129600
    }, 
    {
        "id": 4 
        "title": "Ca lấy 13-03-2021 (12h00 - 18h00)",
        "from_time": 129600,
        "to_time": 151200
    }]
}
Cấu trúc Response
Trường dữ liệu	Mô tả
id	
Mã ca lấy.

title	
Thông tin ca lấy.

from_time	
Từ giờ.

to_time	
Đến giờ.

Error-Response
{
    "code": 400,
    "message": "Token is required!",
    "data": null,
    "code_message":"USER_ERR_COMMON"
}





Tính thời gian dự kiến giao hàng
API Tính thời gian dự kiến giao
 
Sử dụng API này để tính được thời gian dự kiến giao hàng tới người nhận.

Lưu ý : API này cần truyền token và shopid ở header.

post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/leadtime
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/leadtime
Curl
curl --location --request POST 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/leadtime' \
--header 'Content-Type: application/json' \
--header 'ShopId: 916' \
--header 'Token: e835a5a1-b111-11ea-aea1-7aad6bea2429' \
--data-raw '{
    "from_district_id": 1750,
    "from_ward_code": "511110",
    "to_district_id": 1750,
    "to_ward_code": "511110",
    "service_id": 53320
}'
        
        
Cấu trúc Request
Trường dữ liệu	Kiểu dữ liệu	Mô tả
token	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

ShopID	Int	
Mã định danh của cửa hàng.

from_district_id	Int	
Quận/Huyện của người gửi hàng.

from_ward_code	Int	
Phường/Xã của người gửi hàng.

to_district_id	Int	
Quận/Huyện của người nhận hàng.

to_ward_code	String	
Phường/Xã của người nhận hàng.

service_id	Int	
Mã loại dịch vụ.Để lấy thông tin chính xác từng tuyến và thời gian dự kiến giao.

API lấy gói dịch vụ

Success 200
{
    "code": 200,
    "message": "Success",
    "data":
    {
        "leadtime": 1593187200
        "order_date": 1592981718
    }
}
Cấu trúc Response
Trường dữ liệu	Mô tả
leadtime	
Thời gian giao dự kiến.

order_date	
Ngày tạo đơn hàng.

Error-Response
{
    "code": 400,
    "message": "code=400, message=Syntax error: offset=30, error=invalid character '}' after array element",
    "data": null,
    "code_message": "USER_ERR_COMMON"
}




Hủy đơn hàng
API Hủy đơn hàng
 
Sử dụng API này để hủy đơn hàng.

Lưu ý : API này cần truyền token và shopid ở header.

post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/switch-status/cancel
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/switch-status/cancel
Curl
curl --location --request POST 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/switch-status/cancel' \
--header 'Content-Type: application/json' \
--header 'ShopId: 885' \
--header 'Token: 637170d5-942b-11ea-9821-0281a26fb5d4' \
--data-raw '{
    "order_codes":["5E3NK3RS"]
}'
                
                
                
Cấu trúc Request
Trường dữ liệu	Kiểu dữ liệu	Mô tả
token	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

shop_id	Int	
Mã định danh của cửa hàng.

order_codes	String	
Mã đơn hàng của GHN trả về cho khách hàng.

Success 200
{
    "code": 200,
    "message": "Success",
    "data":[
    {
        "order_code": "5E3NK3RS"  
        "result": true,
        "message": "OK" 
    }]
}
Cấu trúc Response
Trường dữ liệu	Mô tả
order_code	
Mã đơn hàng.

result	
Kết quả.

message	
Thông báo.

Error-Response
{
    "code": 400,
    "message": "code=400, message=Syntax error: offset=30, error=invalid character '}' after array element",
    "data": null,
    "code_message": "USER_ERR_COMMON"
}




Trả lại hàng
API Trả lại hàng
 
Sử dụng API này để trả lại hàng khi người gửi muốn hủy giao.

Lưu ý : API này cần truyền token và shopid ở header.

post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/switch-status/return
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/switch-status/return
Curl
curl --location --request POST 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/switch-status/return' \
--header 'token: 637170d5-942b-11ea-9821-0281a26fb5d4' \
--header 'ShopId: 885' \
--header 'Content-Type: application/json' \
--data-raw '{
    "order_codes":["5ENLKKHD"]
}'
        

        
        
Cấu trúc Request
Trường dữ liệu	Kiểu dữ liệu	Mô tả
token	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

shop_id	Int	
Mã định danh của cửa hàng.

order_codes	String	
Mã đơn hàng của GHN trả về cho khách hàng.

Success 200
{
    "code": 200,
    "message": "Success",
    "data":[
    {
        "order_code":"5ENLKKHD"  
        "result":true 
        "message":"OK" 
    }]
}
h2>Cấu trúc Response
Trường dữ liệu	Mô tả
order_code	
Mã đơn hàng.

result	
Kết quả.

message	
Thông báo.

Error-Response
{
    "code": 400,
    "message": "code=400, message=Syntax error: offset=30, error=invalid character '}' after array element",
    "data": null,
    "code_message": "USER_ERR_COMMON"
}



In thông tin đơn hàng
API In thông tin đơn hàng
 
 

post/get
Lưu ý : API này cần truyền token ở header.

Production
https://online-gateway.ghn.vn/shiip/public-api/v2/a5/gen-token
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/a5/gen-token
Sau khi chạy API In thông tin đơn hàng để lấy được token ABC thì sẽ gán vào link dưới , trong đó ABC là token khi gọi API. Thời gian hết hạn 30 phút

 

Production
- Print A5: https://online-gateway.ghn.vn/a5/public-api/printA5?token=ABC
- Print 80x80: https://online-gateway.ghn.vn/a5/public-api/print80x80?token=ABC
- Print 50x72 : https://online-gateway.ghn.vn/a5/public-api/print52x70?token=ABC
Test
- Print A5: https://dev-online-gateway.ghn.vn/a5/public-api/printA5?token=ABC
- Print 80x80: https://dev-online-gateway.ghn.vn/a5/public-api/print80x80?token=ABC
- Print 50x72 : https://dev-online-gateway.ghn.vn/a5/public-api/print52x70?token=ABC
Curl
curl --location --request POST 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/a5/gen-token' \
--header 'Token: 3b75c988-ff34-11eb-b255-166e45bc1992123' \
--header 'Content-Type: application/json' \
--data-raw '{
    "order_codes":["GA99W4RREB"]
}'
    
    
Cấu trúc Request
Trường dữ liệu	Kiểu dữ liệu	Mô tả
token	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

order_codes	Array 	
Mã đơn hàng của GHN trả về cho khách hàng.

Success 200
{
    "code": 200,
    "message": "Success",
    "data": {
        "token":"e27db030-a1bf-11ea-b421-6a186c15e40e" 
    }
}
Cấu trúc Response
Trường dữ liệu	Mô tả
token	
Token ABC này được sinh ra để gán vào URL in đơn hàng theo mã đơn hàng.

 

Error-Response
{
    "code": 400,
    "message": "Lỗi gọi API: corev2_tenant_order_detail - code=404, message=Đơn hàng không tồn tại",
    "data": null
}


Thay đổi thông tin đơn hàng
API Thay đổi thông tin đơn hàng
 
Sử dụng API này để thay đổi các thông tin đơn hàng ngoại trừ COD.

Lưu ý : API này cần truyền token và shopid ở header.

post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/update
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/update
Curl
curl --location --request POST 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/update' \
--header 'Content-Type: application/json' \
--header 'ShopId: 885' \
--header 'Token: 637170d5-942b-11ea-9821-0281a26fb5d4' \
--header 'Content-Type: text/plain' \
--data-raw '{
    "note":"nhớ gọi 30p khi giao","order_code":"5F5NH3LN"
}'
                
                
Cấu trúc Request
Trường dữ liệu	Kiểu dữ liệu	Mô tả
token	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

shop_id	Int	
Mã định danh của cửa hàng.

order_code	String	
Mã đơn hàng của GHN trả về cho khách hàng.

from_name	String	
Tên người gửi.

from_phone	String	
Số điện thoại người gửi

from_address	String	
Địa chỉ người gửi.

from_ward_code	String	
Phường/Xã người gửi.

from_district_id	String	
Quận/Huyện người gửi.

to_name	String	
Tên người nhận hàng.

to_phone	String	
Số điện thoại người nhận hàng.

to_address	String	
Địa chỉ Shiper tới giao hàng.

to_ward_code	String	
Phường/Xã của người nhận hàng.

to_district_id	Int	
Quận/Huyện của người nhận hàng.

return_phone	String	
Số điện thoại trả hàng khi không giao được.

return_address	String	
Địa chỉ trả hàng khi không giao được.

return_ward_code	String	
Phường/Xã của người nhận hàng trả.

return_district_id	Int	
Quận/Huyện của người nhận hàng trả.

client_order_code	String	
Mã đơn hàng riêng của khách hàng.

Mặc định: null

cod_amount	Int	
Tiền thu hộ cho người gửi.

Tối đa: 10.000.000

Giá trị mặc định: 0

content	String	
Nội dung của đơn hàng.

weight	Int	
Khối lượng của đơn hàng (gram).

Tối đa : 30000 gram
length	Int	
Chiều dài của đơn hàng (cm).

Tối đa : 150 cm
width	Int	
Chiều rộng của đơn hàng (cm).

Tối đa : 150 cm
height	Int	
Chiều cao của đơn hàng (cm).

Tối đa : 150 cm
pick_station_id	Int	
Mã bưu cục để gửi hàng tại điểm.

Giá trị mặc định : null

insurance_value	Int	
Giá trị của đơn hàng ( Trường hợp mất hàng, bể hàng sẽ đền theo giá trị của đơn hàng).

Tối đa 5.000.000

Giá trị mặc định: 0

coupon	String	
Mã giảm giá.

payment_type_id	Int	
Mã người thanh toán phí dịch vụ.

1: Người bán/Người gửi.

2: Người mua/Người nhận.

note	String	
Người gửi ghi chú cho tài xế.

required_note	String	
Ghi chú bắt buộc, Bao gồm: CHOTHUHANG, CHOXEMHANGKHONGTHU, KHONGCHOXEMHANG

CHOTHUHANG nghĩa là Người mua có thể yêu cầu xem và dùng thử hàng hóa

CHOXEMHANGKHONGTHU nghĩa là Người mua được xem hàng nhưng không được dùng thử hàng

KHONGCHOXEMHANG nghĩa là Người mua không được phép xem hàng

pick_shift	 	
Dùng để truyền ca lấy hàng , Sử dụng API Lấy danh sách ca lấy

Items	 	
Thông tin sản phẩm.

name	String	
Tên của sản phẩm.

code	String	
Mã của sản phẩm.

quantity	Int	
Số lượng của sản phẩm.

price	Int	
Giá của sản phẩm.

length	Int	
Chiều dài của sản phẩm.

width	Int	
Chiều rộng của sản phẩm.

height	Int	
Chiều cao của sản phẩm.

category	 	
Danh mục sản phẩm được phân chia 3 cấp độlevel1, level2, level3

level1	String	
Danh mục cấp 1

Success 200
{
    "code": 200,
    "message": "Success",
    "data":null
}
Error-Response
{
    "code": 400,
    "message": "code=400, message=Syntax error: offset=30, error=invalid character '}' after array element",
    "data": null,
    "code_message": "USER_ERR_COMMON"
}


Giao lại đơn hàng
API Giao lại đơn hàng
 
Sử dụng API này để giao lại đơn hàng khi những đơn hàng đang trong quá trình chuẩn bị trả hàng.

Lưu ý : API cần truyền token và shopid ở header.

post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/switch-status/storing
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/switch-status/storing
Curl
curl --location --request POST 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/switch-status/storing' \
--header 'Content-Type: application/json' \
--header 'Token: 637170d5-942b-11ea-9821-0281a26fb5d4' \
--header 'ShopId: 885' \
--data-raw '{"order_codes":["5ENLKKHD"]}'
    
    
    
Cấu trúc Request
Field	Type	Description
token	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

order_codes	String	
Mã đơn hàng của GHN trả về cho khách hàng.

shop_id	Int	
Mã định danh của cửa hàng.

Success 200
{
    "code": 200,
    "message": "Success",
    "data":[
    {
        "order_code":"5ENLKKHD"
        "result":true
        "message":"OK"
    }]
}
Cấu trúc Response
Trường dữ liệu	Mô tả
order_code	
Mã đơn hàng.

result	
Kết quả.

message	
Thông báo.

Error-Response
{
    "code": 400,
    "message": "ShopID is invalid: Lỗi gọi API: corev2_tenant_shop - Shop ID is invalid: %!s",
    "data": null,
    "code_message": "SHOP_NOT_FOUND"
}


Thay đổi COD của đơn hàng
API Thay đổi COD của đơn hàng
 
Sử dụng API này để thay đổi COD của đơn hàng.

Lưu ý : API này cần truyền token ở header.

post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/updateCOD 
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/updateCOD 
Curl
curl --location --request POST 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/updateCOD' \
--header 'Content-Type: application/json' \
--header 'Token: 5503c660-971c-11e9-9b4a-d3c6f28cd30c' \
--data-raw '{
    "order_code": "5E3NK3RS",
    "cod_amount": 100000
}'
        
        
Cấu trúc Request
Trường dữ liệu	Kiểu dữ liệu	Mô tả
token	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

order_code	String	
Mã đơn hàng của GHN trả về cho khách hàng.

cod_amount	Int	
Tiền thu hộ cho người gửi.

Tối đa 5.000.000

Giá trị mặc định: 0

Success 200
{
    "code": 200,
    "message": "Success",
    "data": null
}
Error-Response
{
    "code": 400,
    "message": "Lỗi gọi API: corev2_tenant_order_detail - code=404, message=Đơn hàng không tồn tại",
    "data": null
}


Lấy danh sách bưu cục
API Lấy danh sách bưu cục
 
Sử dụng API này để lấy ra danh sách bưu cục để tạo đơn hàng khi mang hàng ra bưu cục gửi.

Lưu ý : API này cần truyền token ở header.

post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/station/get
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/station/get
Curl
curl --location --request GET 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/station/get' \
--header 'token: 637170d5-942b-11ea-9821-0281a26fb5d4' \
--header 'Content-Type: application/json' \
--data-raw'{
    "district_id":"1442",
    "ward_code":"20101",
    "offset":0,
    "limit":1000
}'
            
Cấu trúc Request
Trường dữ liệu	Kiểu dữ liệu	Mô tả
token	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

district_id	Int	
Mã Quận/Huyện được lấy từ API Lấy Quận/Huyện.

ward_code	String	
Mã Phường/Xã được lấy từ API Lấy Phường/Xã.

Có thể : null

offset	Int	
Mặc định =0

limit	Int	
Số liệu trả về không quá 1000.

Success 200
{
    "code": 200,
    "message": "Success",
    "data":[
    {
        "address":"2Bis Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1, TP.HCM", 
        "locationCode":2443, 
        "locationId":2443,           
        "locationName":"Bưu Cục 2 Bis Nguyễn Thị Minh Khai-Q.1-HCM", 
        "parentLocation":
        [ 
            "REGION"/"E",
            "PROVINCE"/202,
            "DISTRICT"/1442,
            "WARD"/"20106",
            "SECTION"/"EXREG0081S",
        ],
        "email":"",
        "latitude":10.7900041,
        "longitude":106.7041314,
        "wardName":"Phường Đa Kao",
        "districtName":"Quận 1",
        "provinceName":"Hồ Chí Minh",
        "iframeMap":""
    },
    ...
    ]
}
Cấu trúc Response
Trường dữ liệu	Mô tả
address	
Địa chỉ bưu cục.

locationName	
Tên bưu cục.

REGION/1	
Khu vực.

PROVINCE/202	
Tỉnh thành.

DISTRICT/1442	
Quận/Huyện.

WARD/20101	
Phường/Xã.

email	
Email.

latitude	
Vĩ độ.

longitude	
Kinh độ.

wardName	
Tên phường/xã.

districtName	
Tên quận/huyện.

provinceName	
Tên tỉnh thành.

iframeMap	
Khung nội tuyến.

Error-Response
{
    "code": 400,
    "message": "code=400, message=Syntax error: offset=24, error=invalid character '\\n' in string literal",
    "data": null,
    "code_message":"USER_ERR_COMMON"
}


Tính phí dịch vụ
API Tính phí dịch vụ
 
Sử dụng API này để tính phí dịch vụ trước khi Tạo đơn qua GHN.

Lưu ý : API này cần truyền token và shopid ở header.

 

 
post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee
Curl
curl --location 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee' \
--header 'Content-Type: application/json' \
--header 'Token: c518-c4bb-11ea-be3a-f636b1deefb9' \
--header 'ShopId: 885' \
--data '{
    "service_type_id":5,
    "from_district_id":1442,
    "from_ward_code": "21211",
    "to_district_id":1820,
    "to_ward_code":"030712",
    "length":30,
    "width":40,
    "height":20,
    "weight":3000,
    "insurance_value":0,
    "coupon": null,
    "items": [
    {
        "name": "TEST1",
        "quantity": 1,
        "length": 200,
        "width": 200,
        "height": 200,
        "weight": 1000
    }]
}'
Cấu trúc Request
Trường dữ liệu	Bắt buộc	Kiểu dữ liệu	Mô tả
token	X	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

shop_id	X	Int	
Mã định danh của cửa hàng.

service_type_id	 	Int	
Mã loại dịch vụ: Gọi API lấy gói dịch vụ để lấy mã loại dịch vụ.

Mã loại dịch vụ cố định. 2: Hàng nhẹ    5: Hàng nặng

Trong đó: 

Hàng nhẹ sử dụng length, width, height và weight

Hàng nặng sử dụng items[].length, items[].width, items[].height và items[].weight

 

insurance_value	 	Int	
Giá trị của đơn hàng (Trường hợp mất hàng, bể hàng sẽ đền theo giá trị của đơn hàng).

Tối đa: 5.000.000

Giá trị mặc định: 0

coupon	 	String	
Mã giảm giá.

cod_failed_amount	 	Int	
Giá trị giao thất bại thu tiền.

from_district_id	 	Int	
Mã Quận/Huyện người gửi hàng.

Nếu không truyền sẽ lấy thông tin từ ShopId

from_ward_code	 	String	
Mã Phường/Xã người nhận hàng.

Nếu không truyền sẽ lấy thông tin từ ShopId

to_ward_code	X	String	
Mã Phường/Xã người nhận hàng.

to_district_id	X	Int	
Mã Quận/Huyện người nhận hàng.

weight	X	Int	
Khối lượng của đơn hàng (gram).

Tối đa: 1.600.000 gram
length	 	Int	
Chiều dài của đơn hàng (cm).

Tối đa: 200 cm
width	 	Int	
Chiều rộng của đơn hàng (cm).

Tối đa: 200 cm
height	 	Int	
Chiều cao của đơn hàng (cm).

Tối đa: 200 cm
cod_value	 	Int	
Tiền thu hộ cho người gửi.

Maximum: 10.000.000

Giá trị mặc định: 0

items	X	 Array	
Thông tin sản phẩm.

Bắt buộc truyền Item khi sử dụng gói dịch vụ Hàng nặng
items[].name	X	String	
Tên của sản phẩm.

items[].code	 	String	
Mã của sản phẩm.

items[].quantity	X	Int	
Số lượng của sản phẩm.

items[].height	X	Int	
Chiều cao của sản phẩm Hàng nặng đi nhiều kiện thì bắt buộc phải truyền height

items[].weight	X	Int	
Đối với trường hợp chọn gói dịch Hàng nặng đi nhiều kiện thì bắt buộc phải truyền weight

items[].width	X	Int	
Chiều rộng của sản phẩm Hàng nặng đi nhiều kiện thì bắt buộc phải truyền width

items[].length	X	Int	
Chiều dài của sản phẩm Hàng nặng đi nhiều kiện thì bắt buộc phải truyền length

GHN có 2 dịch vụ hàng nhẹ và hàng nặng
Hàng nhẹ có service_type_id = 2, kích thước/khối lượng tính cước length, width, height và weight
Hàng nặng có service_type_id = 5, kích thước/khối lượng tính cước lấy trong items (items[].length, items[].width, items[].height và items[].weight)
Mỗi item là 1 kiện hàng
GHN tính toán kích thước/khối lượng của Items rồi cập nhật lại kích thước/khối lượng của đơn hàng theo công thức: Max(length), Max(width), Sum(height)
Với mỗi item, kích thước dài nhất được tính là Dài, kích thước nhỏ nhất được tính là Cao
Khối lượng quy đổi được tính theo công thức:
(Length x Width x Height) / 5 ; So sánh khối lượng quy đổi và trọng lượng thực tế, giá trị nào lớn hơn sẽ là khối lượng tính cước.
 

 

Success 200
{
    "code": 200,
    "message": "Success",
    "data":
    {
        "total": 36300,
        "service_fee": 36300,
        "insurance_fee": 0,
        "pick_station_fee": 0,
        "coupon_value": 0,
        "r2s_fee": 0,
        "document_return": 0,
        "double_check": 0,
        "cod_fee": 0,
        "pick_remote_areas_fee": 0,
        "deliver_remote_areas_fee": 0,
        "cod_failed_fee": 0
    }
}
Cấu trúc Response
Trường dữ liệu	Mô tả
total	
Tổng tiền dịch vụ.

service_fee	
Phí dịch vụ.

insurance_fee	
Phí khai giá hàng hóa.

pick_station_fee	
Phí gửi hàng tại bưu cục.

coupon_value	
Giá trị khuyến mãi.

r2s_fee	
Phí giao lại hàng.

document_return	
Phí giao tài liệu

double_check	
Phí đồng kiểm.

cod_fee	
Phí thu tiền COD

pick_remote_areas_fee	
Phí lấy hàng vùng xa.

deliver_remote_areas_fee	
Phí giao hàng vùng xa.

cod_failed_fee	
Phí thu tiền khi giao thất bại.

Error-Response
{
    "code": 400,
    "message": "code=400, message=Syntax error: offset=30, error=invalid character '}' after array element",
    "data": null
    "code_message": "USER_ERR_COMMON"
}


Lấy chi tiết phí dịch vụ của đơn hàng
API Lấy chi tiết phí dịch vụ của đơn hàng
 
Sử dụng API này để giúp lấy ra chi tiết từng loại phí dịch vụ của một đơn hàng.

Lưu ý : API này cần truyền token và shop_id ở header.

post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/soc
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/soc
Curl
curl --location --request POST 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/soc' \
--header 'Content-Type: application/json' \
--header 'ShopID: 885' \
--header 'Token: 637170d5-942b-11ea-9821-0281a26fb5d4' \
--data-raw '{"order_code":"5ENLKKHD"}'
        
        
Cấu trúc Request
Trường dữ liệu	Kiểu dữ liệu	Mô tả
token	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

shop_id	Int	
Mã định danh của cửa hàng.

order_code	String	
Mã đơn hàng của GHN trả về cho khách hàng.

Success 200
{
    "code": 200,
    "message": "Success",
    "data":
    {
        "_id": "5ed121cb1eb594a402473301",
        "order_code": "S5ENLKKHD",
        "detail":
        { 
            "main_service": 29700,
            "insurance": 11000, 
            "station_do": 0, 
            "station_pu": 0, 
            "return": 22000,
            "r2s": 0, 
            "coupon": 0
        }, 
        "payment":[
        { 
            "value": 40700,  
            "payment_type": 2,  
            "paid_date": "0001-01-01T00:00:00Z",  
            "created_date": "2020-05-29T14:33:51.082Z"  
        },
        { 
            "value": 22000,  
            "payment_type": 2,  
            "paid_date": "0001-01-01T00:00:00Z",  
            "created_date": "2020-05-29T14:52:59.484Z"  
        }],
        "cod_collect_date": "0001-01-01T00:00:00Z", 
        "transaction_id": "54ca80f5-5154-4486-9fc6-8cc218086de8", 
        "created_ip": "", 
        "created_date": "2020-05-29T14:52:59.484Z", 
        "updated_ip": "", 
        "updated_client": 0, 
        "updated_employee": 0, 
        "updated_source": "", 
        "updated_date": "0001-01-01T00:00:00Z" 
    }
}

Cấu trúc Response
Trường dữ liệu	Mô tả
order_code	
Mã đơn hàng.

detail	
Chi tiết.

main_service	
Phí dịch vụ.

insurance	
Phí khai giá hàng hóa.

station_do	
Phí gửi hàng tại bưu cục.

station_pu	
Phí lấy hàng tại bưu cục.

return	
Phí hoàn hàng.

r2s	
Phí giao lại hàng.

coupon	
Giá trị khuyến mãi.

payment	
Thanh toán.

value	
Tổng tiền thanh toán.

payment_type	
Mã người thanh toán phí dịch vụ.

paid_date	
Ngày thanh toán.

created_date	
Ngày tạo.

cod_collect_date	
Ngày thu tiền thu hộ.

created_ip	
Ip tạo.

created_date	
Ngày tạo.

updated_ip	
Ip cập nhật.

updated_client	
Client cập nhật.

updated_employee	
Nhân viên cập nhật.

updated_source	
Nguồn cập nhật.

updated_date	
Ngày cập nhật.

Error-Response
{
    "code": 404,
    "message": "Không tìm thấy thông tin đơn hàng",
    "data": null
}


Lấy gói dịch vụ
API Lấy gói dịch vụ
 
Sử dụng API này để lấy các gói dịch vụ theo tuyến.

Lưu ý : Ở API này cần truyền token ở header.

post/get
Production
https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/available-services
Test
https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/available-services
Curl
curl --location --request POST 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/available-services' \
--header 'token: 637170d5-942b-11ea-9821-0281a26fb5d4' \
--header 'Content-Type: application/json' \
--data-raw '{
     "shop_id":885,
     "from_district": 1447,
     "to_district": 1442
}'
            
Cấu trúc Request
Trường dữ liệu	Kiểu dữ liệu	Mô tả
token	String	
Dùng để xác định định danh của tài khoản và dùng cho các trường hợp gọi tới các API.

from_district	Int	
Quận/Huyện của người gửi hàng.

to_district	Int	
Quận/Huyện của người nhận hàng.

shop_id	Int	
Mã định danh của cửa hàng.

Success 200
{
    "code": 200,
    "message": "Success",
    "data":[
    {
        "short_name":"Hàng nặng"
        "service_type_id":5
    },
    {
        "short_name":"Hàng nhẹ"
        "service_type_id":2
    }]
}
Cấu trúc Response
Trường dữ liệu	Mô tả
service_id	
Mã dịch vụ.

short_name	
Tên dịch vụ.

service_type_id	
Loại hình dịch vụ.

Error-Response
{
    "code": 400,
    "message": "code=400, message=Syntax error: offset=30, error=invalid character '}' after array element"
    "data": null
    "code_message": "USER_ERR_COMMON"
}