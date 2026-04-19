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
    public static final String ORDER_STATUS_COMPLETED = "Delivery Complete";
    public static final String ORDER_STATUS_ADMIN_CANCELLED = "Admin Cancelled";
    public static final String BILLING_TYPE_INDIVIDUAL = "Bill";
    public static final String BILLING_TYPE_ENTERPRISE = "Enterprise Invoice";
    public static final String VNPAY_TXN_REF_PARAM = "vnp_TxnRef";
    public static final String VNPAY_RESPONSE_CODE_PARAM = "vnp_ResponseCode";
    public static final String AUTHORITY_CHECKOUT = "checkout";
    public static final String JWT_CLAIM_AUTHORITIES = "authorities";
    public static final String JWT_PREFIX = "Bearer ";
    public static final String PERSON_DATA_MAP_KEYS_PREFIX = "person.";
}
