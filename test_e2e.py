"""E2E verification: login → DataInputView → CabinetDesignView → Three.js canvas"""
from playwright.sync_api import sync_playwright
import sys

FRONTEND = 'http://localhost:5173'

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.set_viewport_size({"width": 1440, "height": 900})

    # 1. Navigate to frontend
    print("1. Opening frontend...")
    page.goto(FRONTEND, wait_until='networkidle', timeout=15000)

    # 2. Check login page
    print("2. Checking login page...")
    page.screenshot(path='/tmp/e2e_01_login.png', full_page=True)
    try:
        page.locator('input[placeholder]').first.wait_for(timeout=5000)
        print("   Login form visible")
    except:
        print("   WARNING: Login form not found - might already be authenticated")

    # 3. Navigate directly to DataInputView
    print("3. Opening DataInputView...")
    page.goto(f'{FRONTEND}/cutting/data-input', wait_until='networkidle', timeout=15000)
    page.wait_for_timeout(2000)
    page.screenshot(path='/tmp/e2e_02_datainput.png', full_page=True)

    # 4. Check for order creation elements
    print("4. Checking DataInputView elements...")
    body = page.content()
    has_order_select = '新建订单' in body or 'target' in body
    has_board_panel = 'RawMaterialPanel' in body or '板材' in body
    has_3d_button = '3D 柜体设计' in body or 'cabinet-design' in body
    print(f"   Order area: {has_order_select}")
    print(f"   Board panel: {has_board_panel}")
    print(f"   3D button: {has_3d_button}")

    # 5. Navigate to CabinetDesignView
    print("5. Opening CabinetDesignView...")
    page.goto(f'{FRONTEND}/cutting/cabinet-design?orderId=1', wait_until='networkidle', timeout=15000)
    page.wait_for_timeout(3000)
    page.screenshot(path='/tmp/e2e_03_cabinet_design.png', full_page=True)

    # 6. Check Canvas and presets
    print("6. Checking CabinetDesignView...")
    has_canvas = 'canvas' in body or page.locator('canvas').count() > 0
    has_presets = '预设' in body or '衣柜' in body

    canvas_count = page.locator('canvas').count()
    print(f"   Canvas elements: {canvas_count}")
    print(f"   Presets visible: {has_presets}")

    # 7. Check for order info header
    try:
        header_text = page.locator('.cd-header').inner_text()
        print(f"   Header: {header_text[:80]}")
    except:
        print("   WARNING: Header not found")

    print("\nDONE. Screenshots at /tmp/e2e_*.png")
    browser.close()
