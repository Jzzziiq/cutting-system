(function () {
    const app = document.getElementById('app');
    const API_BASE = '';

    const state = {
        token: localStorage.getItem('token') || '',
        user: JSON.parse(localStorage.getItem('user') || 'null'),
        route: 'dashboard',
        loading: false,
        toast: null,
        customers: { records: [], total: 0, pageNum: 1, pageSize: 10 },
        boards: { records: [], total: 0, pageNum: 1, pageSize: 10 },
        modal: null,
        algorithm: {
            form: { L: 100, W: 50, rotateEnable: false, gapDistance: 0 },
            squares: [
                { id: 'item1', l: 20, w: 15 },
                { id: 'item2', l: 30, w: 20 }
            ],
            solutions: [],
            activeIndex: 0
        }
    };

    const navItems = [
        { key: 'dashboard', label: '概览' },
        { key: 'customers', label: '客户管理' },
        { key: 'boards', label: '板材管理' },
        { key: 'algorithm', label: '算法排样' }
    ];

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function showToast(message, type = 'info') {
        state.toast = { message, type };
        render();
        window.clearTimeout(showToast.timer);
        showToast.timer = window.setTimeout(() => {
            state.toast = null;
            render();
        }, 2600);
    }

    function setToken(token, user) {
        state.token = token;
        state.user = user;
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(user));
    }

    function clearToken() {
        state.token = '';
        state.user = null;
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    }

    function toQuery(data) {
        const params = new URLSearchParams();
        Object.keys(data || {}).forEach(key => {
            const value = data[key];
            if (value !== undefined && value !== null && value !== '') {
                params.append(key, value);
            }
        });
        return params.toString();
    }

    async function request(path, options = {}) {
        const method = options.method || 'GET';
        const headers = {};
        let url = `${API_BASE}${path}`;
        let body;

        if (options.auth !== false) {
            if (!state.token) {
                clearToken();
                render();
                throw new Error('请先登录');
            }
            headers.Authorization = `Bearer ${state.token}`;
        }

        if (method === 'GET' && options.data) {
            const query = toQuery(options.data);
            if (query) {
                url += `${url.includes('?') ? '&' : '?'}${query}`;
            }
        } else if (options.form) {
            headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
            body = toQuery(options.data);
        } else if (options.data !== undefined) {
            headers['Content-Type'] = 'application/json;charset=UTF-8';
            body = JSON.stringify(options.data);
        }

        const response = await fetch(url, { method, headers, body });

        if (response.status === 401) {
            clearToken();
            render();
            throw new Error('登录已失效');
        }

        const contentType = response.headers.get('content-type') || '';
        const payload = contentType.includes('application/json') ? await response.json() : await response.text();

        if (!response.ok) {
            throw new Error(typeof payload === 'string' ? payload : `请求失败 ${response.status}`);
        }

        if (options.raw) {
            if (payload && typeof payload.code === 'number' && payload.code !== 200) {
                throw new Error(payload.msg || '请求失败');
            }
            return payload;
        }

        if (payload && payload.code === 200) {
            return payload.data;
        }

        throw new Error((payload && payload.msg) || '请求失败');
    }

    const api = {
        login(username, password) {
            return request('/auth/login', {
                method: 'POST',
                auth: false,
                form: true,
                data: { username, password }
            });
        },
        customers(pageNum, pageSize) {
            return request('/customers', { data: { pageNum, pageSize } });
        },
        customer(id) {
            return request(`/customers/${id}`);
        },
        saveCustomer(data) {
            return request('/customers', { method: 'POST', data });
        },
        updateCustomer(id, data) {
            return request(`/customers/${id}`, { method: 'PUT', data });
        },
        deleteCustomer(id) {
            return request(`/customers/${id}`, { method: 'DELETE' });
        },
        boards(pageNum, pageSize) {
            return request('/boards', { data: { pageNum, pageSize } });
        },
        board(id) {
            return request(`/boards/${id}`);
        },
        saveBoard(data) {
            return request('/boards', { method: 'POST', data });
        },
        updateBoard(id, data) {
            return request(`/boards/${id}`, { method: 'PUT', data });
        },
        deleteBoard(id) {
            return request(`/boards/${id}`, { method: 'DELETE' });
        },
        solve(data) {
            return request('/algorithm/answer', { method: 'POST', data, raw: true });
        }
    };

    function navigate(route) {
        window.location.hash = route;
    }

    function syncRoute() {
        const route = (window.location.hash || '#dashboard').replace('#', '');
        state.route = navItems.some(item => item.key === route) ? route : 'dashboard';
    }

    async function loadRouteData() {
        if (!state.token) return;
        try {
            if (state.route === 'customers') {
                await loadCustomers();
            }
            if (state.route === 'boards') {
                await loadBoards();
            }
        } catch (error) {
            showToast(error.message, 'error');
        }
    }

    async function bootRoute() {
        syncRoute();
        render();
        await loadRouteData();
        render();
    }

    async function loadCustomers() {
        const page = await api.customers(state.customers.pageNum, state.customers.pageSize);
        state.customers.records = page?.records || [];
        state.customers.total = page?.total || 0;
    }

    async function loadBoards() {
        const page = await api.boards(state.boards.pageNum, state.boards.pageSize);
        state.boards.records = page?.records || [];
        state.boards.total = page?.total || 0;
    }

    function render() {
        if (!state.token) {
            app.innerHTML = renderLogin();
            return;
        }

        app.innerHTML = `
            <div class="app-shell">
                ${renderSidebar()}
                <main class="content">
                    ${renderTopbar()}
                    <div class="page">
                        ${renderRoute()}
                    </div>
                </main>
            </div>
            ${state.modal ? renderModal() : ''}
            ${state.toast ? `<div class="toast ${state.toast.type === 'error' ? 'error' : ''}">${escapeHtml(state.toast.message)}</div>` : ''}
        `;

        if (state.route === 'algorithm') {
            drawLayout();
        }
    }

    function renderLogin() {
        return `
            <div class="login-screen">
                <form class="login-panel" data-action="login">
                    <div class="login-title">板材切割系统</div>
                    <div class="login-subtitle">后台管理端</div>
                    <div class="field">
                        <label for="username">用户名</label>
                        <input class="input" id="username" name="username" autocomplete="username" required>
                    </div>
                    <div class="field" style="margin-top: 14px;">
                        <label for="password">密码</label>
                        <input class="input" id="password" name="password" type="password" autocomplete="current-password" required>
                    </div>
                    <button class="button primary" style="margin-top: 22px; width: 100%;" type="submit">登录</button>
                </form>
                ${state.toast ? `<div class="toast ${state.toast.type === 'error' ? 'error' : ''}">${escapeHtml(state.toast.message)}</div>` : ''}
            </div>
        `;
    }

    function renderSidebar() {
        return `
            <aside class="sidebar">
                <div class="brand">
                    <div class="brand-name">切割系统</div>
                    <div class="brand-meta">Web Console</div>
                </div>
                <nav class="nav">
                    ${navItems.map(item => `
                        <button class="nav-button ${state.route === item.key ? 'active' : ''}" data-route="${item.key}">
                            <span>${escapeHtml(item.label)}</span>
                        </button>
                    `).join('')}
                </nav>
                <div class="user-box">
                    <div>${escapeHtml(state.user?.realName || state.user?.username || '已登录用户')}</div>
                    <div class="brand-meta">${escapeHtml(state.user?.username || '')}</div>
                    <button class="button ghost" data-action="logout" style="margin-top: 12px; width: 100%;">退出登录</button>
                </div>
            </aside>
        `;
    }

    function renderTopbar() {
        const active = navItems.find(item => item.key === state.route);
        return `
            <header class="topbar">
                <div>
                    <div class="page-title">${escapeHtml(active?.label || '概览')}</div>
                    <div class="page-subtitle">${renderRouteSubtitle()}</div>
                </div>
                <div class="toolbar">
                    <button class="button ghost" data-action="refresh">刷新</button>
                </div>
            </header>
        `;
    }

    function renderRouteSubtitle() {
        if (state.route === 'customers') return `客户总数 ${state.customers.total}`;
        if (state.route === 'boards') return `板材总数 ${state.boards.total}`;
        if (state.route === 'algorithm') return '输入矩形尺寸并查看排样结果';
        return '客户、板材与排样管理';
    }

    function renderRoute() {
        if (state.route === 'customers') return renderCustomers();
        if (state.route === 'boards') return renderBoards();
        if (state.route === 'algorithm') return renderAlgorithm();
        return renderDashboard();
    }

    function renderDashboard() {
        return `
            <section class="metrics">
                <div class="metric">
                    <div class="metric-label">客户数量</div>
                    <div class="metric-value">${Number(state.customers.total || 0)}</div>
                </div>
                <div class="metric">
                    <div class="metric-label">板材数量</div>
                    <div class="metric-value">${Number(state.boards.total || 0)}</div>
                </div>
                <div class="metric">
                    <div class="metric-label">算法方案</div>
                    <div class="metric-value">${state.algorithm.solutions.length}</div>
                </div>
                <div class="metric">
                    <div class="metric-label">当前利用率</div>
                    <div class="metric-value">${state.algorithm.solutions[0] ? `${(state.algorithm.solutions[0].rate * 100).toFixed(1)}%` : '-'}</div>
                </div>
            </section>
            <section class="panel">
                <div class="panel-header">
                    <div class="panel-title">快捷操作</div>
                </div>
                <div class="panel-body actions">
                    <button class="button secondary" data-route="customers">客户管理</button>
                    <button class="button secondary" data-route="boards">板材管理</button>
                    <button class="button primary" data-route="algorithm">算法排样</button>
                </div>
            </section>
        `;
    }

    function renderCustomers() {
        return `
            <section class="panel">
                <div class="panel-header">
                    <div class="panel-title">客户列表</div>
                    <button class="button primary" data-action="customer-create">新增客户</button>
                </div>
                <div class="table-wrap">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>客户名称</th>
                                <th>电话</th>
                                <th>地址</th>
                                <th>状态</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${state.customers.records.length ? state.customers.records.map(customer => `
                                <tr>
                                    <td>${escapeHtml(customer.customerId)}</td>
                                    <td>${escapeHtml(customer.customerName)}</td>
                                    <td>${escapeHtml(customer.phone)}</td>
                                    <td>${escapeHtml(customer.address || '-')}</td>
                                    <td>${renderStatus(customer.isEnabled)}</td>
                                    <td>
                                        <div class="actions">
                                            <button class="button ghost" data-action="customer-detail" data-id="${customer.customerId}">查看</button>
                                            <button class="button secondary" data-action="customer-edit" data-id="${customer.customerId}">编辑</button>
                                            <button class="button danger" data-action="customer-delete" data-id="${customer.customerId}">删除</button>
                                        </div>
                                    </td>
                                </tr>
                            `).join('') : `<tr><td colspan="6"><div class="empty">暂无客户数据</div></td></tr>`}
                        </tbody>
                    </table>
                </div>
            </section>
        `;
    }

    function renderBoards() {
        return `
            <section class="panel">
                <div class="panel-header">
                    <div class="panel-title">板材列表</div>
                    <button class="button primary" data-action="board-create">新增板材</button>
                </div>
                <div class="table-wrap">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>品牌</th>
                                <th>材质</th>
                                <th>颜色</th>
                                <th>尺寸类型</th>
                                <th>尺寸</th>
                                <th>状态</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${state.boards.records.length ? state.boards.records.map(board => `
                                <tr>
                                    <td>${escapeHtml(board.boardId)}</td>
                                    <td>${escapeHtml(board.brand)}</td>
                                    <td>${escapeHtml(board.materialType)}</td>
                                    <td>${escapeHtml(board.color)}</td>
                                    <td>${escapeHtml(board.sizeType)}</td>
                                    <td>${escapeHtml(formatBoardSize(board))}</td>
                                    <td>${renderStatus(board.isEnabled)}</td>
                                    <td>
                                        <div class="actions">
                                            <button class="button ghost" data-action="board-detail" data-id="${board.boardId}">查看</button>
                                            <button class="button secondary" data-action="board-edit" data-id="${board.boardId}">编辑</button>
                                            <button class="button danger" data-action="board-delete" data-id="${board.boardId}">删除</button>
                                        </div>
                                    </td>
                                </tr>
                            `).join('') : `<tr><td colspan="8"><div class="empty">暂无板材数据</div></td></tr>`}
                        </tbody>
                    </table>
                </div>
            </section>
        `;
    }

    function renderStatus(value) {
        const enabled = value === undefined || value === null || Number(value) === 1;
        return `<span class="status ${enabled ? 'enabled' : 'disabled'}">${enabled ? '启用' : '禁用'}</span>`;
    }

    function formatBoardSize(board) {
        const length = board.length ?? '-';
        const width = board.width ?? '-';
        const thickness = board.thickness ?? '-';
        return `${length} x ${width} x ${thickness} mm`;
    }

    function renderAlgorithm() {
        const solution = state.algorithm.solutions[state.algorithm.activeIndex];
        return `
            <div class="algorithm-layout">
                <section class="panel">
                    <div class="panel-header">
                        <div class="panel-title">输入参数</div>
                        <button class="button ghost" data-action="algorithm-reset">重置</button>
                    </div>
                    <div class="panel-body">
                        <form data-action="algorithm-solve">
                            <div class="grid two">
                                <div class="field">
                                    <label>容器长度</label>
                                    <input class="input" name="L" type="number" min="1" value="${escapeHtml(state.algorithm.form.L)}" required>
                                </div>
                                <div class="field">
                                    <label>容器宽度</label>
                                    <input class="input" name="W" type="number" min="1" value="${escapeHtml(state.algorithm.form.W)}" required>
                                </div>
                                <div class="field">
                                    <label>间隙距离</label>
                                    <input class="input" name="gapDistance" type="number" min="0" value="${escapeHtml(state.algorithm.form.gapDistance)}">
                                </div>
                                <div class="field">
                                    <label>允许旋转</label>
                                    <select class="select" name="rotateEnable">
                                        <option value="false" ${!state.algorithm.form.rotateEnable ? 'selected' : ''}>否</option>
                                        <option value="true" ${state.algorithm.form.rotateEnable ? 'selected' : ''}>是</option>
                                    </select>
                                </div>
                            </div>

                            <div style="margin-top: 18px;">
                                <div class="panel-title" style="margin-bottom: 10px;">待排矩形</div>
                                <div id="square-list" class="grid">
                                    ${state.algorithm.squares.map((square, index) => renderSquareEditor(square, index)).join('')}
                                </div>
                                <div class="actions" style="margin-top: 12px;">
                                    <button class="button secondary" type="button" data-action="square-add">添加矩形</button>
                                    <button class="button primary" type="submit">计算排样</button>
                                </div>
                            </div>
                        </form>
                    </div>
                </section>

                <section class="panel">
                    <div class="panel-header">
                        <div class="panel-title">结果可视化</div>
                        <div class="muted">${solution ? `利用率 ${(solution.rate * 100).toFixed(2)}%` : '暂无结果'}</div>
                    </div>
                    <div class="panel-body">
                        ${state.algorithm.solutions.length ? renderSolutionTabs() : ''}
                        <canvas id="layout-canvas" class="layout-canvas" width="960" height="520"></canvas>
                    </div>
                </section>
            </div>
            ${state.algorithm.solutions.length ? renderSolutionTable() : ''}
        `;
    }

    function renderSquareEditor(square, index) {
        return `
            <div class="square-editor" data-square-index="${index}">
                <input class="input" name="square-l" type="number" min="1" value="${escapeHtml(square.l)}" placeholder="长">
                <input class="input" name="square-w" type="number" min="1" value="${escapeHtml(square.w)}" placeholder="宽">
                <button class="button danger" type="button" data-action="square-remove" data-index="${index}">删除</button>
            </div>
        `;
    }

    function renderSolutionTabs() {
        return `
            <div class="result-tabs">
                ${state.algorithm.solutions.map((solution, index) => `
                    <button class="tab ${state.algorithm.activeIndex === index ? 'active' : ''}" data-action="solution-tab" data-index="${index}">
                        容器 ${index + 1} · ${(solution.rate * 100).toFixed(1)}%
                    </button>
                `).join('')}
            </div>
        `;
    }

    function renderSolutionTable() {
        const solution = state.algorithm.solutions[state.algorithm.activeIndex];
        return `
            <section class="panel">
                <div class="panel-header">
                    <div class="panel-title">放置明细</div>
                    <div class="small">容器尺寸 ${escapeHtml(solution.containerLength)} x ${escapeHtml(solution.containerWidth)}</div>
                </div>
                <div class="table-wrap">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>序号</th>
                                <th>X</th>
                                <th>Y</th>
                                <th>长</th>
                                <th>宽</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${(solution.placeSquareList || []).length ? solution.placeSquareList.map((square, index) => `
                                <tr>
                                    <td>${index + 1}</td>
                                    <td>${escapeHtml(square.x)}</td>
                                    <td>${escapeHtml(square.y)}</td>
                                    <td>${escapeHtml(square.l)}</td>
                                    <td>${escapeHtml(square.w)}</td>
                                </tr>
                            `).join('') : `<tr><td colspan="5"><div class="empty">该容器未放入矩形</div></td></tr>`}
                        </tbody>
                    </table>
                </div>
            </section>
        `;
    }

    function drawLayout() {
        const canvas = document.getElementById('layout-canvas');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        const solution = state.algorithm.solutions[state.algorithm.activeIndex];
        const width = canvas.width;
        const height = canvas.height;
        ctx.clearRect(0, 0, width, height);
        ctx.fillStyle = '#fbfcfe';
        ctx.fillRect(0, 0, width, height);

        if (!solution) {
            ctx.fillStyle = '#667085';
            ctx.font = '24px Microsoft YaHei';
            ctx.fillText('暂无排样结果', 36, 58);
            return;
        }

        const padding = 40;
        const containerL = Number(solution.containerLength);
        const containerW = Number(solution.containerWidth);
        const scale = Math.min((width - padding * 2) / containerL, (height - padding * 2) / containerW);
        const drawW = containerL * scale;
        const drawH = containerW * scale;
        const originX = padding;
        const originY = padding;
        const colors = ['#93c5fd', '#86efac', '#fde68a', '#fca5a5', '#c4b5fd', '#67e8f9', '#f9a8d4'];

        ctx.strokeStyle = '#172033';
        ctx.lineWidth = 2;
        ctx.strokeRect(originX, originY, drawW, drawH);

        (solution.placeSquareList || []).forEach((square, index) => {
            const x = originX + Number(square.x) * scale;
            const y = originY + drawH - (Number(square.y) + Number(square.w)) * scale;
            const rectW = Number(square.l) * scale;
            const rectH = Number(square.w) * scale;
            ctx.fillStyle = colors[index % colors.length];
            ctx.fillRect(x, y, rectW, rectH);
            ctx.strokeStyle = '#344054';
            ctx.lineWidth = 1;
            ctx.strokeRect(x, y, rectW, rectH);
            ctx.fillStyle = '#172033';
            ctx.font = '18px Microsoft YaHei';
            ctx.fillText(String(index + 1), x + 8, y + 24);
        });
    }

    function renderModal() {
        const modal = state.modal;
        if (modal.type === 'customer') {
            return renderCustomerModal(modal);
        }
        if (modal.type === 'board') {
            return renderBoardModal(modal);
        }
        return '';
    }

    function renderCustomerModal(modal) {
        const data = modal.data || {};
        const readonly = modal.mode === 'detail';
        return `
            <div class="modal-backdrop">
                <form class="modal" data-action="${modal.mode === 'create' ? 'customer-save' : 'customer-update'}" data-id="${escapeHtml(data.customerId || '')}">
                    <div class="modal-header">
                        <div class="panel-title">${readonly ? '客户详情' : modal.mode === 'create' ? '新增客户' : '编辑客户'}</div>
                        <button class="button ghost" type="button" data-action="modal-close">关闭</button>
                    </div>
                    <div class="modal-body">
                        <div class="grid two">
                            <div class="field">
                                <label>客户名称</label>
                                <input class="input" name="customerName" value="${escapeHtml(data.customerName)}" ${readonly ? 'readonly' : 'required'}>
                            </div>
                            <div class="field">
                                <label>联系电话</label>
                                <input class="input" name="phone" value="${escapeHtml(data.phone)}" ${readonly ? 'readonly' : 'required'}>
                            </div>
                            <div class="field" style="grid-column: 1 / -1;">
                                <label>地址</label>
                                <input class="input" name="address" value="${escapeHtml(data.address)}" ${readonly ? 'readonly' : ''}>
                            </div>
                            <div class="field" style="grid-column: 1 / -1;">
                                <label>备注</label>
                                <textarea class="textarea" name="remark" ${readonly ? 'readonly' : ''}>${escapeHtml(data.remark)}</textarea>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        ${readonly ? `<button class="button secondary" type="button" data-action="customer-edit" data-id="${data.customerId}">编辑</button>` : '<button class="button primary" type="submit">保存</button>'}
                    </div>
                </form>
            </div>
        `;
    }

    function renderBoardModal(modal) {
        const data = modal.data || {};
        const readonly = modal.mode === 'detail';
        return `
            <div class="modal-backdrop">
                <form class="modal" data-action="${modal.mode === 'create' ? 'board-save' : 'board-update'}" data-id="${escapeHtml(data.boardId || '')}">
                    <div class="modal-header">
                        <div class="panel-title">${readonly ? '板材详情' : modal.mode === 'create' ? '新增板材' : '编辑板材'}</div>
                        <button class="button ghost" type="button" data-action="modal-close">关闭</button>
                    </div>
                    <div class="modal-body">
                        <div class="grid three">
                            <div class="field">
                                <label>品牌</label>
                                <input class="input" name="brand" value="${escapeHtml(data.brand)}" ${readonly ? 'readonly' : 'required'}>
                            </div>
                            <div class="field">
                                <label>材质</label>
                                <input class="input" name="materialType" value="${escapeHtml(data.materialType)}" ${readonly ? 'readonly' : 'required'}>
                            </div>
                            <div class="field">
                                <label>颜色</label>
                                <input class="input" name="color" value="${escapeHtml(data.color)}" ${readonly ? 'readonly' : 'required'}>
                            </div>
                            <div class="field">
                                <label>尺寸类型</label>
                                <input class="input" name="sizeType" value="${escapeHtml(data.sizeType)}" ${readonly ? 'readonly' : 'required'}>
                            </div>
                            <div class="field">
                                <label>长度 mm</label>
                                <input class="input" name="length" type="number" value="${escapeHtml(data.length)}" ${readonly ? 'readonly' : 'required'}>
                            </div>
                            <div class="field">
                                <label>宽度 mm</label>
                                <input class="input" name="width" type="number" value="${escapeHtml(data.width)}" ${readonly ? 'readonly' : 'required'}>
                            </div>
                            <div class="field">
                                <label>厚度 mm</label>
                                <input class="input" name="thickness" type="number" value="${escapeHtml(data.thickness)}" ${readonly ? 'readonly' : 'required'}>
                            </div>
                            <div class="field" style="grid-column: 1 / -1;">
                                <label>备注</label>
                                <textarea class="textarea" name="remark" ${readonly ? 'readonly' : ''}>${escapeHtml(data.remark)}</textarea>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        ${readonly ? `<button class="button secondary" type="button" data-action="board-edit" data-id="${data.boardId}">编辑</button>` : '<button class="button primary" type="submit">保存</button>'}
                    </div>
                </form>
            </div>
        `;
    }

    function formToObject(form) {
        const data = {};
        new FormData(form).forEach((value, key) => {
            data[key] = typeof value === 'string' ? value.trim() : value;
        });
        return data;
    }

    function boardPayload(form) {
        const data = formToObject(form);
        data.width = Number(data.width);
        data.length = Number(data.length);
        data.thickness = Number(data.thickness);
        return data;
    }

    async function handleLogin(form) {
        const data = formToObject(form);
        if (!data.username || !data.password) {
            showToast('请输入用户名和密码', 'error');
            return;
        }
        try {
            const info = await api.login(data.username, data.password);
            setToken(info.token, info);
            if (!window.location.hash) {
                navigate('dashboard');
            }
            await bootRoute();
            showToast('登录成功');
        } catch (error) {
            showToast(error.message, 'error');
        }
    }

    async function openCustomerModal(mode, id) {
        try {
            const data = id ? await api.customer(id) : {};
            state.modal = { type: 'customer', mode, data };
            render();
        } catch (error) {
            showToast(error.message, 'error');
        }
    }

    async function openBoardModal(mode, id) {
        try {
            const data = id ? await api.board(id) : {};
            state.modal = { type: 'board', mode, data };
            render();
        } catch (error) {
            showToast(error.message, 'error');
        }
    }

    async function saveCustomer(form) {
        const data = formToObject(form);
        try {
            if (form.dataset.action === 'customer-save') {
                await api.saveCustomer(data);
            } else {
                await api.updateCustomer(form.dataset.id, data);
            }
            state.modal = null;
            await loadCustomers();
            render();
            showToast('客户已保存');
        } catch (error) {
            showToast(error.message, 'error');
        }
    }

    async function saveBoard(form) {
        const data = boardPayload(form);
        try {
            if (form.dataset.action === 'board-save') {
                await api.saveBoard(data);
            } else {
                await api.updateBoard(form.dataset.id, data);
            }
            state.modal = null;
            await loadBoards();
            render();
            showToast('板材已保存');
        } catch (error) {
            showToast(error.message, 'error');
        }
    }

    async function deleteCustomer(id) {
        if (!window.confirm('确认删除该客户？')) return;
        try {
            await api.deleteCustomer(id);
            await loadCustomers();
            render();
            showToast('客户已删除');
        } catch (error) {
            showToast(error.message, 'error');
        }
    }

    async function deleteBoard(id) {
        if (!window.confirm('确认删除该板材？')) return;
        try {
            await api.deleteBoard(id);
            await loadBoards();
            render();
            showToast('板材已删除');
        } catch (error) {
            showToast(error.message, 'error');
        }
    }

    function readAlgorithmForm(form) {
        const data = formToObject(form);
        const rows = Array.from(form.querySelectorAll('[data-square-index]'));
        return {
            L: Number(data.L),
            W: Number(data.W),
            rotateEnable: data.rotateEnable === 'true',
            gapDistance: Number(data.gapDistance || 0),
            squareList: rows.map((row, index) => ({
                id: `item${index + 1}`,
                l: Number(row.querySelector('[name="square-l"]').value),
                w: Number(row.querySelector('[name="square-w"]').value)
            }))
        };
    }

    function validateAlgorithmPayload(payload) {
        if (payload.L <= 0 || payload.W <= 0 || payload.gapDistance < 0) {
            throw new Error('请填写正确的容器参数');
        }
        if (!payload.squareList.length || payload.squareList.some(item => item.l <= 0 || item.w <= 0)) {
            throw new Error('请填写正确的矩形尺寸');
        }
    }

    async function solveAlgorithm(form) {
        try {
            const payload = readAlgorithmForm(form);
            validateAlgorithmPayload(payload);
            state.algorithm.form = {
                L: payload.L,
                W: payload.W,
                rotateEnable: payload.rotateEnable,
                gapDistance: payload.gapDistance
            };
            state.algorithm.squares = payload.squareList;
            const solutions = await api.solve(payload);
            state.algorithm.solutions = Array.isArray(solutions) ? solutions : [];
            state.algorithm.activeIndex = 0;
            render();
            showToast('计算完成');
        } catch (error) {
            showToast(error.message, 'error');
        }
    }

    function resetAlgorithm() {
        state.algorithm = {
            form: { L: 100, W: 50, rotateEnable: false, gapDistance: 0 },
            squares: [
                { id: 'item1', l: 20, w: 15 },
                { id: 'item2', l: 30, w: 20 }
            ],
            solutions: [],
            activeIndex: 0
        };
        render();
    }

    document.addEventListener('click', event => {
        const routeButton = event.target.closest('[data-route]');
        if (routeButton) {
            navigate(routeButton.dataset.route);
            return;
        }

        const button = event.target.closest('[data-action]');
        if (!button) return;
        const action = button.dataset.action;
        const id = button.dataset.id;

        if (action === 'logout') {
            clearToken();
            render();
        }
        if (action === 'refresh') {
            bootRoute();
        }
        if (action === 'modal-close') {
            state.modal = null;
            render();
        }
        if (action === 'customer-create') openCustomerModal('create');
        if (action === 'customer-detail') openCustomerModal('detail', id);
        if (action === 'customer-edit') openCustomerModal('edit', id);
        if (action === 'customer-delete') deleteCustomer(id);
        if (action === 'board-create') openBoardModal('create');
        if (action === 'board-detail') openBoardModal('detail', id);
        if (action === 'board-edit') openBoardModal('edit', id);
        if (action === 'board-delete') deleteBoard(id);
        if (action === 'square-add') {
            state.algorithm.squares.push({ id: `item${state.algorithm.squares.length + 1}`, l: '', w: '' });
            render();
        }
        if (action === 'square-remove') {
            state.algorithm.squares.splice(Number(button.dataset.index), 1);
            render();
        }
        if (action === 'solution-tab') {
            state.algorithm.activeIndex = Number(button.dataset.index);
            render();
        }
        if (action === 'algorithm-reset') resetAlgorithm();
    });

    document.addEventListener('submit', event => {
        const form = event.target.closest('form[data-action]');
        if (!form) return;
        event.preventDefault();
        const action = form.dataset.action;
        if (action === 'login') handleLogin(form);
        if (action === 'customer-save' || action === 'customer-update') saveCustomer(form);
        if (action === 'board-save' || action === 'board-update') saveBoard(form);
        if (action === 'algorithm-solve') solveAlgorithm(form);
    });

    window.addEventListener('hashchange', bootRoute);

    bootRoute();
})();
