package org.monostudio.search.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Document(indexName = "shipment-tracking")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentTrackingDocument {
    @Id
    private String id;              // trackingId

    @Field(type = FieldType.Long)
    private Long orderId;

    @Field(type = FieldType.Keyword)
    private String trackingNumber;

    @Field(type = FieldType.Keyword)
    private String shipperCode;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Text)
    private String location;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Date)
    private Instant eventTime;

    @Field(type = FieldType.Date)
    private Instant receivedAt;
}
