"""Basic UI smoke test: login, dashboard, and page navigation."""
from playwright.sync_api import sync_playwright
import sys

BASE = "http://127.0.0.1:5173"
SCREENSHOTS = "scripts/screenshots"

import os
os.makedirs(SCREENSHOTS, exist_ok=True)

errors = []

def check(condition, label):
    if not condition:
        errors.append(label)
        print(f"  FAIL: {label}")
    else:
        print(f"  OK:   {label}")

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1280, "height": 800})

    # 1. Login page loads
    print("\n[1] Login page")
    page.goto(f"{BASE}/login")
    page.wait_for_load_state("networkidle")
    page.screenshot(path=f"{SCREENSHOTS}/01-login.png")
    check(page.url.endswith("/login"), "URL is /login")
    check(page.locator("input").count() >= 2, "Has username/password inputs")
    check(page.locator("button").count() >= 1, "Has a submit button")

    # 2. Login with admin/123456
    print("\n[2] Login flow")
    # Find inputs - try common patterns
    inputs = page.locator("input")
    input_count = inputs.count()
    print(f"  Found {input_count} input fields")

    # Fill username and password
    username_input = None
    password_input = None
    for i in range(input_count):
        inp = inputs.nth(i)
        inp_type = inp.get_attribute("type") or ""
        placeholder = inp.get_attribute("placeholder") or ""
        if inp_type == "password" or "密码" in placeholder:
            password_input = inp
        elif inp_type in ("text", "") and username_input is None:
            username_input = inp

    check(username_input is not None, "Found username input")
    check(password_input is not None, "Found password input")

    if username_input and password_input:
        username_input.fill("admin")
        password_input.fill("123456")
        page.screenshot(path=f"{SCREENSHOTS}/02-login-filled.png")

        # Click login button
        login_btn = page.locator("button").first
        login_btn.click()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1000)
        page.screenshot(path=f"{SCREENSHOTS}/03-after-login.png")
        check("/login" not in page.url, "Navigated away from login")
        print(f"  Current URL: {page.url}")

    # 3. Dashboard
    print("\n[3] Dashboard")
    page.goto(f"{BASE}/dashboard")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(500)
    page.screenshot(path=f"{SCREENSHOTS}/04-dashboard.png")
    check("/dashboard" in page.url or "/login" not in page.url, "Dashboard accessible")
    check(page.locator(".el-menu, .sidebar, nav, [class*=menu]").count() > 0, "Has navigation menu")

    # 4. Navigate to key pages
    pages_to_test = [
        ("/customers", "Customer management"),
        ("/boards", "Board management"),
        ("/cutting/data-input", "Data input"),
        ("/cutting/layout-workbench", "Layout workbench"),
    ]

    print("\n[4] Page navigation")
    for path, label in pages_to_test:
        page.goto(f"{BASE}{path}")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        fname = path.replace("/", "_").strip("_")
        page.screenshot(path=f"{SCREENSHOTS}/05-page-{fname}.png")
        is_ok = "/login" not in page.url
        check(is_ok, f"{label} ({path}) accessible")
        if not is_ok:
            print(f"    Redirected to: {page.url}")

    browser.close()

# Summary
print("\n" + "=" * 40)
if errors:
    print(f"FAILED: {len(errors)} check(s)")
    for e in errors:
        print(f"  - {e}")
    sys.exit(1)
else:
    print("ALL CHECKS PASSED")
    sys.exit(0)
