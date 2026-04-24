package org.monostudio.config;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

/**
 * A temporary helper class to hold on to some name keys
 */
@NoArgsConstructor(access = PRIVATE)
public final class Constants {
    public static final String ORDER_FULFILLMENT_STATUS_PENDING = "PENDING";
    public static final String ORDER_FULFILLMENT_STATUS_PROCESSING = "PROCESSING";
    public static final String ORDER_FULFILLMENT_STATUS_CONFIRMED = "CONFIRMED";
    public static final String ORDER_FULFILLMENT_STATUS_REJECTED = "REJECTED";
    public static final String ORDER_FULFILLMENT_STATUS_DELIVERY_ON_ROUTE = "DELIVERY_ON_ROUTE";
    public static final String ORDER_FULFILLMENT_STATUS_DELIVERY_FAILED = "DELIVERY_FAILED";
    public static final String ORDER_FULFILLMENT_STATUS_DELIVERY_CANCELLED = "DELIVERY_CANCELLED";
    public static final String ORDER_FULFILLMENT_STATUS_COMPLETED = "DELIVERY_COMPLETE";
    public static final String ORDER_FULFILLMENT_STATUS_RETURNED = "RETURNED";

    public static final String ORDER_PAYMENT_STATUS_UNPAID = "UNPAID";
    public static final String ORDER_PAYMENT_STATUS_PAYMENT_STARTED = "PAYMENT_STARTED";
    public static final String ORDER_PAYMENT_STATUS_PAID = "PAID";
    public static final String ORDER_PAYMENT_STATUS_PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String ORDER_PAYMENT_STATUS_PAYMENT_CANCELLED = "PAYMENT_CANCELLED";
    public static final String ORDER_PAYMENT_STATUS_REFUND_PENDING = "REFUND_PENDING";
    public static final String ORDER_PAYMENT_STATUS_REFUNDED = "REFUNDED";
    public static final String ORDER_PAYMENT_STATUS_PARTIALLY_REFUNDED = "PARTIALLY_REFUNDED";
    public static final String BILLING_TYPE_INDIVIDUAL = "Bill";
    public static final String BILLING_TYPE_ENTERPRISE = "Enterprise Invoice";
    public static final String VNPAY_TXN_REF_PARAM = "vnp_TxnRef";
    public static final String VNPAY_RESPONSE_CODE_PARAM = "vnp_ResponseCode";
    public static final String MOMO_ORDER_ID_PARAM = "orderId";
    public static final String MOMO_RESULT_CODE_PARAM = "resultCode";
    public static final String AUTHORITY_CHECKOUT = "checkout";
    public static final String JWT_CLAIM_AUTHORITIES = "authorities";
    public static final String JWT_PREFIX = "Bearer ";
    public static final String PERSON_DATA_MAP_KEYS_PREFIX = "person.";
    public static final String LOYALTY_TIER_SILVER = "Silver";
    public static final String LOYALTY_TIER_GOLD = "Gold";
    public static final String LOYALTY_EVENT_EARN_PAID = "EARN_PAID";
    public static final String LOYALTY_EVENT_REVERSE_REJECTED = "REVERSE_REJECTED";
    public static final String LOYALTY_EVENT_REVERSE_RETURNED = "REVERSE_RETURNED";
    public static final String LOYALTY_EVENT_REVERSE_REFUND = "REVERSE_REFUND";
}
