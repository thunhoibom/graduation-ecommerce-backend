# GHN Rollout Runbook

## Test Matrix

- Rate quote success: `GET /api/public/shipping/methods` with GHN method + valid `toDistrictId`/`toWardCode`.
- Rate fallback: simulate invalid GHN token and verify response still returns shipping methods with `providerFeeSource=LOCAL_FALLBACK` and `estimated=true`.
- Checkout with GHN method: create order and confirm status to `Paid, Confirmed`; verify shipment dispatch queue item is created.
- Shipment create success: verify `orders.order_tracking_number` and `orders.order_shipper_code` are set.
- Shipment retry: force GHN create-order failure, verify queue retries with backoff and eventually marks `FAILED_PERMANENT`.
- Tracking webhook idempotency: send same payload twice, verify only one `shipment_trackings` row exists.
- Tracking status mapping: test `DELIVERING`, `DELIVERED`, `DELIVERY_FAIL`, `RETURNED` and verify internal status transitions.

## Observability

- Key logs:
  - GHN rate fallback warning from `ShippingMethodsServiceImpl`.
  - Shipment dispatch failures and permanent failures from `ShipmentOrchestratorServiceImpl`.
  - Duplicate/unmapped webhook events from `ShipmentTrackingServiceImpl`.
- Suggested log filters:
  - `shippingMethodId=`
  - `orderId=`
  - `tracking=`
- Recommended dashboards:
  - GHN rate fallback count / minute.
  - Shipment dispatch success vs retry vs permanent failure.
  - Webhook duplicate count and unmapped status count.

## Rollout Steps

1. Keep GHN shipping method `active=false` in admin.
2. Configure `GHN_TOKEN`, `GHN_SHOP_ID`, `GHN_FROM_DISTRICT_ID`, `GHN_FROM_WARD_CODE`, `GHN_WEBHOOK_SECRET`.
3. Enable integration with `GHN_ENABLED=true` in staging.
4. Run full test matrix with one sandbox shop.
5. Activate GHN method for internal users only (or low-traffic subset).
6. Monitor fallback ratio and dispatch failure rate for 24 hours.
7. Expand activation gradually by warehouse/region.
