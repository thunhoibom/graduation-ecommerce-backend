package org.monostudio.shipping.ghn.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GhnCreateOrderItem {
    String name;
    String code;
    int quantity;
    int price;
    int length;
    int width;
    int height;
    int weight;
}
