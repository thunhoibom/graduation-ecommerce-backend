-- Phase 1: Database Migration SQL

-- Add tracking columns to orders table
ALTER TABLE orders 
    ADD COLUMN IF NOT EXISTS order_tracking_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS order_shipper_code VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_orders_tracking_number ON orders(order_tracking_number);

-- Create shipment_trackings table for history/timeline
CREATE TABLE IF NOT EXISTS shipment_trackings (
    tracking_id        BIGSERIAL PRIMARY KEY,
    order_id           BIGINT       NOT NULL,
    tracking_number    VARCHAR(100) NOT NULL,
    shipper_code       VARCHAR(50)  NOT NULL,
    status             VARCHAR(50)  NOT NULL,
    location           VARCHAR(255),
    description        TEXT,
    event_time         TIMESTAMPTZ  NOT NULL,
    received_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_tracking_order FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE INDEX IF NOT EXISTS idx_shipment_tracking_order_id ON shipment_trackings(order_id);
CREATE INDEX IF NOT EXISTS idx_shipment_tracking_tracking_number ON shipment_trackings(tracking_number);
