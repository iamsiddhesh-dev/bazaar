"""
Bazaar fixture self-check.

Drives the app with uiautomator2 and asserts that each seeded defect actually
fires. This is the fixture's own test suite: if this passes, the QA agent has a
known-good subject to find bugs in.

    python verify_bugs.py
"""

import hashlib
import re
import sys
import time

import uiautomator2 as u2
from lxml import etree

PKG = "com.bazaar.demo"
ACT_LOGIN = f"{PKG}/.LoginActivity"

d = u2.connect()
results = []


def sh(cmd: str) -> str:
    out = d.shell(cmd)
    return getattr(out, "output", out) or ""


def rid(name: str) -> str:
    return f"{PKG}:id/{name}"


def state_hash() -> str:
    root = etree.fromstring(d.dump_hierarchy().encode())
    rids = sorted({e.get("resource-id") for e in root.iter("node") if e.get("resource-id")})
    return hashlib.sha1("|".join(rids).encode()).hexdigest()[:12]


def fresh():
    sh(f"pm clear {PKG}")
    sh(f"am start -n {ACT_LOGIN}")


def login(retries: int = 3) -> bool:
    for _ in range(retries):
        fresh()
        if not d(resourceId=rid("et_email")).wait(timeout=20):
            continue
        d(resourceId=rid("et_email")).set_text("qa@bazaar.dev")
        d(resourceId=rid("et_password")).set_text("test1234")
        # The form is vertically centred; on short screens the IME can cover the
        # button, which also removes it from the hierarchy. Hide it only if needed.
        if not d(resourceId=rid("btn_continue")).wait(timeout=2):
            d.press("back")
        if not d(resourceId=rid("btn_continue")).wait(timeout=10):
            continue
        d(resourceId=rid("btn_continue")).click()
        if d(resourceId=rid("list_products")).wait(timeout=20):
            return True
    raise RuntimeError("could not reach the catalog screen")


def open_first_product() -> bool:
    row = d(textContains="Handcrafted Mahogany")
    if not row.wait(timeout=20):
        return False
    row.click()
    return bool(d(resourceId=rid("btn_add_cart")).wait(timeout=20))


def open_cart() -> bool:
    btn = d(resourceId=rid("btn_open_cart"))
    if not btn.wait(timeout=20):
        return False
    btn.click()
    return bool(d(resourceId=rid("btn_checkout")).wait(timeout=20))


def record(name, ok, detail=""):
    results.append((name, ok, detail))
    print(("PASS  " if ok else "FAIL  ") + f"{name:<24} {detail}")


def crash_text() -> str:
    return sh("logcat -d -b crash")


def overlap(a, b):
    return not (a[2] <= b[0] or b[2] <= a[0] or a[3] <= b[1] or b[3] <= a[1])


# ---------------------------------------------------------------- B1: NPE
sh("logcat -c")
login()
opened = open_first_product()
d(resourceId=rid("btn_add_cart")).click()
time.sleep(1.0)
d(resourceId=rid("btn_add_cart")).click()
time.sleep(3.0)
c = crash_text()
m = re.search(r"NullPointerException[^\n]*", c)
record("B1 crash on 2nd add", opened and bool(m), (m.group(0)[:80] if m else "no NPE"))

# ------------------------------------------------------------- B4: overlap
login()
opened = open_first_product()
title = d(resourceId=rid("tv_product_title"))
price = d(resourceId=rid("tv_product_price"))
tb, pb = title.bounds(), price.bounds()
record("B4 overlapping text", opened and overlap(tb, pb), f"title={tb} price={pb}")

# ---------------------------------------------------------- B2: HTTP 500
sh("logcat -c")
login()
opened = open_cart()
d(resourceId=rid("btn_checkout")).click()
time.sleep(4.0)
lg = sh("logcat -d")
net = re.findall(r"BAZAAR_NET:\s*(\d+) (\w+) (\S+)", lg)
err = d(resourceId=rid("tv_checkout_error"))
err_text = err.info.get("text", "") if err.exists else ""
record("B2 checkout HTTP 500",
       opened and ("500", "POST", "/api/checkout") in net and "500" in err_text,
       f"net={net[:3]} widget={err_text!r}")

# ------------------------------------------------------------ B3: dead tap
login()
opened = open_cart()
before = state_hash()
d(resourceId=rid("btn_apply_coupon")).click()
time.sleep(1.5)
after = state_hash()
record("B3 dead coupon tap", opened and before == after, f"{before} -> {after}")

# ------------------------------------------------------------- B5: flaky
attempts = 9
crashes = 0
for _ in range(attempts):
    login()
    sh("logcat -c")
    d(resourceId=rid("btn_express")).click()
    time.sleep(2.5)
    if "NullPointerException" in crash_text():
        crashes += 1
record("B5 flaky ~1/3", 0 < crashes < attempts, f"{crashes}/{attempts} ({crashes/attempts:.0%})")

print()
bad = [r for r in results if not r[1]]
print(f"{len(results) - len(bad)}/{len(results)} checks passed")
sys.exit(1 if bad else 0)
