# MOMO Sandbox Setup

This project reads MOMO credentials from environment variables via `application.properties`.

## Required environment variables

- `MOMO_PARTNER_CODE`
- `MOMO_ACCESS_KEY`
- `MOMO_SECRET_KEY`
- `MOMO_CREATE_URL` (optional, defaults to MOMO sandbox create URL)
- `MOMO_QUERY_URL` (optional, defaults to MOMO sandbox query URL)
- `MOMO_REFUND_URL` (optional, defaults to MOMO sandbox refund URL)
- `MOMO_RETURN_URL` (optional, defaults to backend callback endpoint)
- `MOMO_NOTIFY_URL` (optional, defaults to backend callback endpoint)
- `MOMO_BROWSER_REDIRECTION_URL` (optional, defaults to frontend receipt URL)
- `MOMO_REQUEST_TYPE` (optional, default `payWithMethod` for AIO)

## Quick start (PowerShell)

```powershell
$env:MOMO_PARTNER_CODE="MOMON52S20260422_TEST"
$env:MOMO_ACCESS_KEY="513X8znS1UgfJADn"
$env:MOMO_SECRET_KEY="SEHwsRJjkDm44HVoG5paogKEE6LnzvJc"
$env:MOMO_CREATE_URL="https://test-payment.momo.vn/v2/gateway/api/create"
$env:MOMO_REQUEST_TYPE="payWithMethod"
```

If your backend is exposed through a tunnel (for MOMO IPN callbacks), also set:

```powershell
$env:MOMO_RETURN_URL="https://<your-tunnel-domain>/api/public/checkout/validate"
$env:MOMO_NOTIFY_URL="https://<your-tunnel-domain>/api/public/checkout/validate"
```

## Notes

- Do not commit real production credentials into git.
- MOMO callback signature is validated server-side before order status changes.
- `paymentType` must be `MOMO` in checkout payload.
- For AIO integration, request type should be `payWithMethod`.
