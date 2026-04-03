package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Collection;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(columnList = "order_date"),
        @Index(columnList = "order_transaction_token"),
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Order
    implements DBEntity {
    private static final long serialVersionUID = 14L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false)
    private Long id;
    @Column(name = "order_date", nullable = false)
    @CreationTimestamp
    private Instant date;
    @Column(name = "order_total_items", nullable = false)
    private int totalItems;
    @Column(name = "order_net_value", nullable = false)
    private int netValue;
    @Column(name = "order_transport_value", nullable = false)
    private int transportValue;
    @Column(name = "order_taxes_value", nullable = false)
    private int taxesValue;
    @Column(name = "order_total_value", nullable = false)
    private int totalValue;
    @Column(name = "order_discount_code")
    private String discountCode;
    @Column(name = "order_discount_value", nullable = false)
    @Builder.Default
    private int discountValue = 0;
    @Size(min = 64, max = 64)
    @Column(name = "order_transaction_token")
    private String transactionToken;
    @JoinColumn(name = "customer_id")
    @ManyToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private Customer customer;
    @JoinColumn(name = "payment_type_id", updatable = false, nullable = false)
    @ManyToOne(optional = false, cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    private PaymentType paymentType;
    @JoinColumn(name = "order_status_id", updatable = false, nullable = false)
    @ManyToOne(optional = false, cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    private OrderStatus status;
    @JoinColumn(name = "billing_type_id", updatable = false, nullable = false)
    @ManyToOne(optional = false, cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    private BillingType billingType;
    @JoinColumn(name = "billing_company_id")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private BillingCompany billingCompany;
    @JoinColumn(name = "billing_address_id", updatable = false)
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Address billingAddress;
    @JoinColumn(name = "shipping_method_id", updatable = false)
    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    private ShippingMethod shippingMethod;
    @JoinColumn(name = "shipping_address_id", updatable = false)
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Address shippingAddress;
    @JoinColumn(name = "salesperson_id")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Salesperson salesperson;
    @JoinColumn(name = "order_id", nullable = false)
    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private Collection<OrderDetail> details;

    /**
     * The cart session token used for this order.
     * Stored so that stock reservations can be confirmed after payment success.
     * Null for orders placed via the legacy checkout path (no CartSession).
     */
    @Column(name = "order_cart_session_token")
    private String cartSessionToken;

    /**
     * Please note that this copy-constructor only preserves the following relationships.
     * <ul>
     *   <li>OrderStatus</li>
     *   <li>PaymentType</li>
     *   <li>BillingType</li>
     * </ul>
     *
     * @param source The original Order
     */
    public Order(Order source) {
        this.id = source.id;
        this.date = Instant.from(source.date);
        this.totalItems = source.totalItems;
        this.netValue = source.netValue;
        this.transportValue = source.transportValue;
        this.taxesValue = source.taxesValue;
        this.totalValue = source.totalValue;
        this.discountCode = source.discountCode;
        this.discountValue = source.discountValue;
        this.transactionToken = source.transactionToken;
        this.paymentType = source.paymentType;
        this.status = source.status;
        this.billingType = source.billingType;
        this.billingAddress = null;
        this.customer = null;
        this.details = null;
        this.billingCompany = null;
        this.shippingMethod = null;
        this.shippingAddress = null;
        this.salesperson = null;
        this.cartSessionToken = source.cartSessionToken;
    }
}
