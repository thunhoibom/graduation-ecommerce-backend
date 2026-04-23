package org.monostudio.shipping.ghn.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GhnCreateOrderRequest {
    long orderCode;
    long shopId;
    int serviceId;
    Integer serviceTypeId;
    int paymentTypeId;
    String note;
    int codAmount;
    int codFailedAmount;
    int insuranceValue;
    String returnPhone;
    String returnAddress;
    Integer returnDistrictId;
    String returnWardCode;
    String fromName;
    String fromPhone;
    String fromAddress;
    String fromWardName;
    String fromDistrictName;
    String fromProvinceName;
    String requiredNote;
    String content;
    String toName;
    String toPhone;
    String toAddress;
    String toWardName;
    String toDistrictName;
    String toProvinceName;
    String toWardCode;
    Integer toDistrictId;
    Integer pickStationId;
    Integer deliverStationId;
    String coupon;
    Long pickupTime;
    List<Integer> pickShift;
    int weight;
    int length;
    int width;
    int height;
    List<GhnCreateOrderItem> items;
}
