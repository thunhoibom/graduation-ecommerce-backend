# Order Status Workflow Checklist

## Core Transition Matrix

- [ ] Pending -> Payment Started (online payment)
- [ ] Payment Started -> Payment Failed
- [ ] Payment Started -> Payment Cancelled
- [ ] Payment Started -> Paid, Unconfirmed (successful webhook)
- [ ] Pending -> Paid, Unconfirmed (COD checkout)
- [ ] Paid, Unconfirmed -> Rejected
- [ ] Paid, Unconfirmed -> Paid, Confirmed
- [ ] Paid, Confirmed -> Delivery On Route
- [ ] Delivery On Route -> Delivery Complete
- [ ] Delivery On Route -> Delivery Failed
- [ ] Delivery On Route -> Delivery Cancelled
- [ ] Delivery Failed -> Returned
- [ ] Delivery Cancelled -> Returned
- [ ] Delivery Complete -> Returned

## Terminal Status Enforcement

- [ ] Payment Failed rejects any next transition
- [ ] Payment Cancelled rejects any next transition
- [ ] Rejected rejects any next transition
- [ ] Returned rejects any next transition
- [ ] PATCH `/api/data/orders/{id}` with `status` returns validation error

## Payment Callback Idempotency

- [ ] Re-send same callback token after success: no duplicate state transition
- [ ] Re-send same callback token after failure: no duplicate state transition
- [ ] Verify status remains unchanged on duplicated callback

## Shipping Webhook Mapping

- [ ] `IN_TRANSIT`/`OUT_FOR_DELIVERY` maps to Delivery On Route
- [ ] `DELIVERED` maps to Delivery Complete
- [ ] failed delivery event maps to Delivery Failed
- [ ] recall/cancel event maps to Delivery Cancelled
- [ ] return event maps to Returned

## Concurrency / Race Cases

- [ ] Admin confirm vs shipping webhook race does not skip required intermediate status
- [ ] Admin reject while callback retry arrives does not reopen rejected order
- [ ] Two concurrent status actions on same order only allow one valid winner
- [ ] Stale Payment Started expiry job does not affect already-paid orders

## Admin UI Consistency

- [ ] Order list/detail/edit pages display same labels/colors for same status
- [ ] Action buttons shown only for valid transitions from current status
- [ ] Invalid actions are hidden and backend still rejects forged calls
