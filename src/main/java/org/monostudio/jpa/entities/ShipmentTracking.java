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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "shipment_trackings",
    indexes = {
        @Index(columnList = "order_id"),
        @Index(columnList = "tracking_number")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"order_id", "tracking_number", "status", "event_time"})
    }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ShipmentTracking implements DBEntity {
    private static final long serialVersionUID = 20L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tracking_id", nullable = false)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "tracking_number", nullable = false)
    private String trackingNumber;

    @Column(name = "shipper_code", nullable = false)
    private String shipperCode;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "location")
    private String location;

    @Column(name = "description")
    private String description;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public ShipmentTracking(ShipmentTracking source) {
        this.id = source.id;
        this.order = source.order;
        this.trackingNumber = source.trackingNumber;
        this.shipperCode = source.shipperCode;
        this.status = source.status;
        this.location = source.location;
        this.description = source.description;
        this.eventTime = source.eventTime;
        this.receivedAt = source.receivedAt;
    }
}
