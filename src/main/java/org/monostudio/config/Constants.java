package org.monostudio.config;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

/**
 * A temporary helper class to hold on to some name keys
 */
@NoArgsConstructor(access = PRIVATE)
public final class Constants {
    public static final String ORDER_STATUS_PENDING = "Pending";
    public static final String ORDER_STATUS_PAYMENT_STARTED = "Payment Started";
    public static final String ORDER_STATUS_PAYMENT_CANCELLED = "Payment Cancelled";
    public static final String ORDER_STATUS_PAYMENT_FAILED = "Payment Failed";
    public static final String ORDER_STATUS_PAID_UNCONFIRMED = "Paid, Unconfirmed";
    public static final String ORDER_STATUS_PAID_CONFIRMED = "Paid, Confirmed";
    public static final String ORDER_STATUS_REJECTED = "Rejected";
    public static final String ORDER_STATUS_DELIVERY_ON_ROUTE = "Delivery On Route";
    public static final String ORDER_STATUS_DELIVERY_FAILED = "Delivery Failed";
    public static final String ORDER_STATUS_DELIVERY_CANCELLED = "Delivery Cancelled";
    public static final String ORDER_STATUS_RETURNED = "Returned";
    public static final String ORDER_STATUS_COMPLETED = "Delivery Complete";
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
