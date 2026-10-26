# Bazaar — integration contract

Everything the QA agent needs to know about this fixture. This file is the
contract: the app's behaviour and the agent's expectations must agree, and any
change here is a breaking change for the platform's benchmark.

| | |
|---|---|
| Package | `com.bazaar.demo` |
| Launcher activity | `com.bazaar.demo/.LoginActivity` |
| versionName | `1.0.3` (versionCode 3) |
| minSdk / targetSdk | 24 / 33 |
| UI framework | **Classic Android Views** (no Compose, no React Native, no Flutter) |
| Orientation | portrait |
| Native libs | none — installs on any ABI, no ABI filter needed |
| Backend | `http://10.0.2.2:8100` (host loopback from the emulator) |
| Cleartext | allowed (`usesCleartextTraffic="true"`), **plain HTTP by design** |

> Plain HTTP is intentional. It lets `mitmproxy` observe every request without
> installing a CA certificate and without root, so the network detector can be
> demonstrated on an unrooted image.

---

## 1. Screen graph

```
LoginActivity
   └─(btn_continue, email+password non-empty)─▶ CatalogActivity
CatalogActivity
   ├─(list item)──────────────────────────────▶ ProductActivity
   ├─(btn_open_cart)──────────────────────────▶ CartActivity
   └─(btn_express)── 1/3 of the time ──▶ FATAL NullPointerException
ProductActivity
   ├─(btn_add_cart ×1) ok
   ├─(btn_add_cart ×2) ──▶ FATAL NullPointerException
   └─(back)───────────────────────────────────▶ CatalogActivity
CartActivity
   ├─(btn_apply_coupon) no-op  (dead tap)
   └─(btn_checkout)──── HTTP 500 ──▶ inline error, stays on screen
CheckoutResultActivity
   └─(btn_done)──▶ finish
```

Activity names as reported by `dumpsys`: `.LoginActivity`, `.CatalogActivity`,
`.ProductActivity`, `.CartActivity`, `.CheckoutResultActivity`.

---

## 2. Resource IDs

All interactive elements have stable, app-scoped IDs. Selector strategy should
be `resource-id` first; `text` second; `bounds` last.

| Screen | IDs |
|---|---|
| Login | `login_root`, `tv_login_header`, `tv_login_sub`, `et_email`, `et_password`, `tv_login_error`, `btn_continue` |
| Catalog | `catalog_root`, `tv_cart_count`, `list_products`, `btn_open_cart`, `btn_express` |
| Product | `product_root`, `iv_product_image`, `tv_product_title`, `tv_product_price`, `tv_product_desc`, `tv_specs_header`, `tv_spec_material`, `tv_spec_dimensions`, `tv_spec_delivery`, `btn_add_cart` |
| Cart | `cart_root`, `tv_cart_summary`, `list_cart`, `tv_checkout_error`, `btn_apply_coupon`, `btn_checkout` |
| Result | `result_root`, `tv_result`, `btn_done` |
| List rows | `row_product_root`, `tv_row_title`, `tv_row_price`, `row_cart_root`, `tv_cart_title`, `tv_cart_price` |

Full form: `com.bazaar.demo:id/<id>`.

---

## 3. Logcat contract

### 3.1 Network

Every HTTP call emits exactly one line, from tag **`BAZAAR_NET`**:

```
I/BAZAAR_NET: 500 POST /api/checkout
I/BAZAAR_NET: 200 GET /api/products
I/BAZAAR_NET: 0 GET /api/products      ← 0 means transport failure, not an HTTP status
```

Parse with:

```python
re.compile(r"BAZAAR_NET:\s*(\d{3}|0)\s+(GET|POST|PUT|DELETE)\s+(\S+)")
```

Rules:

- `0` must be classified as `network_error`, **not** `http_5xx`.
- `2xx` must never produce a finding. A clean run has at least
  `200 POST /api/login` and `200 GET /api/products`.
- The same signature repeated across replays is one finding, not three — dedupe
  on `(status, method, path)`.

### 3.2 Crashes

Standard Android crash output, unmodified:

```
FATAL EXCEPTION: main
Process: com.bazaar.demo, PID: 12345
java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.Integer.intValue()' on a null object reference
```

Signature for dedupe: exception class + first app frame. Do **not** include the
PID or line numbers in the fingerprint.

### 3.3 ANR

No ANR is seeded. If one is reported, it is a harness bug or system noise.

---

## 4. Seeded defects

| ID | Kind | Trigger | Expected signal | Repro rate |
|---|---|---|---|---|
| **B1** | `crash` | Product screen, tap `btn_add_cart` **twice** | `NullPointerException` in `Integer.intValue()` | 3/3 |
| **B2** | `http_5xx` | Cart screen, tap `btn_checkout` | `BAZAAR_NET: 500 POST /api/checkout` + on-screen "Checkout failed (HTTP 500)" | 3/3 |
| **B3** | `deadtap` | Cart screen, tap `btn_apply_coupon` | UI state hash unchanged after tap | 3/3 |
| **B4** | `ui_broken` | Product screen | bounds of `tv_product_title` intersect `tv_product_price` | 3/3 |
| **B5** | `crash` | Catalog, tap `btn_express` | `NullPointerException: express checkout: session token missing` | **~1/3** |

### Expected verdicts

- B1–B4 → **confirmed**, 3/3.
- B5 → **flaky** and dropped. Because the trigger is random, a 3-attempt replay
  may see 1/3 or 2/3; a crawl may not trigger it at all. If B5 is never observed,
  the honest result is **not observed (N/A)**, not a confirmed bug.
- Nothing else. A clean crawl of Bazaar has no other findings.

### Trigger sequences (for replay traces)

```
B1  ProductActivity: tap btn_add_cart, tap btn_add_cart
B2  CartActivity:    tap btn_checkout, wait for response
B3  CartActivity:    tap btn_apply_coupon
B4  ProductActivity: no action needed — visible on entry
B5  CatalogActivity: tap btn_express
```

B1 requires two taps **on the same screen**, with no navigation in between. If
the harness taps once, navigates away and back, the crash still occurs on the
second add — but the trace must be replayed verbatim.

---

## 5. Determinism

| Source of variance | Behaviour |
|---|---|
| B5 | `java.util.Random().nextInt(3) == 0` — genuinely random per tap |
| Product list | static, fixed order, fixed text |
| Login | accepts any non-empty email/password; no server validation |
| Network | mock API is deterministic; `/api/checkout` is always 500 |
| Time | no timestamps rendered in the UI except the status bar clock |

The status bar clock **does** change between dumps. A naive
`dump == previous_dump` stability check will never settle. Compare a structural
hash of `(package, sorted resource-ids)`, not the raw XML.

---

## 6. Environment assumptions

- Emulator API **33**, `google_apis` image (rootable, no Play Store).
- Effective display **720 × 1280** at **320 dpi (2.0×)** = **360 × 640 dp**.
  Do not hardcode pixel values; derive from `bounds` at runtime.
- Window/transition/animator scales set to `0`.
- Mock API reachable at `10.0.2.2:8100`.
- No login credentials are validated, but the form is **not** prefilled: the
  crawler must type into both fields or `btn_continue` returns an inline error
  and never navigates.
- The login form is vertically centred. On a 640 dp-tall screen the soft keyboard
  covers `btn_continue` and removes it from the hierarchy. **Dismiss the IME
  before tapping** (or use an input method that does not show a keyboard).

---

## 7. What "good" looks like

A run of 25 steps against Bazaar should produce:

```
confirmed:  4   (B1, B2, B3, B4)
flaky:      1   (B5, dropped)
filtered:   0
false pos:  0
```

If the agent reports B2 as a bug when the mock API is **not running**, that is a
false positive: `BAZAAR_NET: 0 ...` means the harness failed to start the
backend, not that the app is broken. Start the backend before the run, and treat
`0` as a harness error during benchmarking.
