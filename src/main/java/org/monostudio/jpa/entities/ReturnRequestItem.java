package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Entity
@Table(
    name = "return_request_items",
    indexes = {
        @Index(columnList = "return_request_item_return_request_id"),
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ReturnRequestItem
    implements DBEntity {
    private static final long serialVersionUID = 21L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_request_item_id", nullable = false)
    private Long id;

    @Column(name = "return_request_item_quantity", nullable = false)
    @Min(1)
    private int quantity;

    @Column(name = "return_request_item_reason")
    @Size(max = 500)
    private String reason;

    @Column(name = "return_request_item_is_active", nullable = false)
    private boolean isActive;

    @JoinColumn(name = "return_request_item_return_request_id", nullable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ReturnRequest returnRequest;

    @JoinColumn(name = "return_request_item_product_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    /**
     * Please note: this copy-constructor does not include relationships.
     *
     * @param source The original ReturnRequestItem
     */
    public ReturnRequestItem(ReturnRequestItem source) {
        this.id = source.id;
        this.quantity = source.quantity;
        this.reason = source.reason;
        this.isActive = source.isActive;
        this.returnRequest = null;
        this.product = null;
    }
}
