"""基础功能测试：登录、页面导航、数据输入、排样工作台"""
from playwright.sync_api import sync_playwright
import sys
import os

# 配置
BASE_URL = "http://127.0.0.1:5173"
SCREENSHOTS_DIR = "scripts/screenshots"
os.makedirs(SCREENSHOTS_DIR, exist_ok=True)

# 测试结果统计
results = {"passed": 0, "failed": 0, "errors": []}


def check(condition, label):
    """验证条件并记录结果"""
    if condition:
        results["passed"] += 1
        print(f"  [PASS] {label}")
    else:
        results["failed"] += 1
        results["errors"].append(label)
        print(f"  [FAIL] {label}")


def take_screenshot(page, name):
    """截图并保存"""
    path = f"{SCREENSHOTS_DIR}/{name}.png"
    page.screenshot(path=path, full_page=True)
    return path


def test_login(page):
    """测试登录功能"""
    print("\n[1/4] 登录功能测试")

    # 访问登录页
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state("networkidle")
    take_screenshot(page, "01-login-page")

    # 验证登录表单
    inputs = page.locator("input")
    check(inputs.count() >= 2, "登录表单包含用户名和密码输入框")

    # 找到用户名和密码输入框
    username_input = None
    password_input = None
    for i in range(inputs.count()):
        inp = inputs.nth(i)
        inp_type = inp.get_attribute("type") or ""
        placeholder = inp.get_attribute("placeholder") or ""
        if inp_type == "password" or "密码" in placeholder:
            password_input = inp
        elif inp_type in ("text", "") and username_input is None:
            username_input = inp

    check(username_input is not None, "找到用户名输入框")
    check(password_input is not None, "找到密码输入框")

    # 填写登录信息
    if username_input and password_input:
        username_input.fill("admin")
        password_input.fill("123456")
        take_screenshot(page, "02-login-filled")

        # 点击登录按钮
        login_btn = page.locator("button").first
        login_btn.click()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1000)
        take_screenshot(page, "03-after-login")

        # 验证登录成功
        check("/login" not in page.url, "登录成功，已跳转")
        check("/dashboard" in page.url, "跳转到仪表板页面")

        # 验证 token 存储
        token = page.evaluate("() => localStorage.getItem('token')")
        check(token is not None and len(token) > 0, "JWT token 已存储")


def test_dashboard(page):
    """测试仪表板页面"""
    print("\n[2/4] 仪表板测试")

    page.goto(f"{BASE_URL}/dashboard")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1000)
    take_screenshot(page, "04-dashboard")

    # 验证页面加载
    check("/dashboard" in page.url, "仪表板页面可访问")

    # 验证导航菜单
    nav = page.locator(".el-menu, .sidebar, nav, [class*=menu]")
    check(nav.count() > 0, "导航菜单显示")

    # 验证摘要卡片
    cards = page.locator(".el-card, .summary-card, [class*=card]")
    check(cards.count() > 0, "摘要卡片显示")


def test_page_navigation(page):
    """测试页面导航"""
    print("\n[3/4] 页面导航测试")

    pages_to_test = [
        ("/customers", "客户管理"),
        ("/boards", "板材管理"),
        ("/cutting/data-input", "数据输入"),
        ("/cutting/layout-workbench", "排样工作台"),
    ]

    for path, label in pages_to_test:
        page.goto(f"{BASE_URL}{path}")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)

        fname = path.replace("/", "_").strip("_")
        take_screenshot(page, f"05-page-{fname}")

        # 验证页面可访问（未重定向到登录页）
        is_accessible = "/login" not in page.url
        check(is_accessible, f"{label} ({path}) 页面可访问")


def test_data_input(page):
    """测试数据输入页面"""
    print("\n[4/4] 数据输入页面测试")

    page.goto(f"{BASE_URL}/cutting/data-input")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1000)
    take_screenshot(page, "06-data-input")

    # 验证页面加载
    check("/cutting/data-input" in page.url, "数据输入页面加载")

    # 验证订单信息区域
    order_info = page.locator("[class*=order], [class*=Order]")
    check(order_info.count() > 0, "订单信息区域显示")

    # 验证板材选择面板
    board_panel = page.locator("[class*=board], [class*=Board], [class*=material]")
    check(board_panel.count() > 0, "板材选择面板显示")

    # 验证工件数据表格
    tables = page.locator("table, .el-table")
    check(tables.count() > 0, "工件数据表格显示")


def main():
    """主测试流程"""
    print("=" * 50)
    print("柜门板材切割排版系统 - 基础功能测试")
    print("=" * 50)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1280, "height": 800})

        try:
            # 运行测试
            test_login(page)
            test_dashboard(page)
            test_page_navigation(page)
            test_data_input(page)

        except Exception as e:
            print(f"\n测试执行异常: {e}")
            results["failed"] += 1
            results["errors"].append(f"异常: {e}")

        finally:
            browser.close()

    # 输出测试结果
    print("\n" + "=" * 50)
    print(f"测试完成：{results['passed']} 通过 / {results['failed']} 失败")
    print(f"截图保存至：{SCREENSHOTS_DIR}/")

    if results["errors"]:
        print("\n失败项：")
        for error in results["errors"]:
            print(f"  - {error}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
