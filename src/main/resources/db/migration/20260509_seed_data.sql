-- Seed data: 基础业务数据填充
-- 覆盖：激活用户、多状态订单、订单明细、余料、排版结果、生产任务、算法任务、审计日志
-- 所有密码哈希对应明文 "123456"
-- Run with: USE board_cutting_db; source this file;

USE board_cutting_db;

-- ============================================================
-- 1. 激活待审批用户 + 新增用户
-- ============================================================

UPDATE t_user SET account_status = 1 WHERE username IN ('zhangsan', 'lisi');

INSERT INTO t_user (username, password, real_name, phone, role_type, account_status, create_time, update_time) VALUES
('wangwu',   'e10adc3949ba59abbe56e057f20f883e', '王工',   '13800001111', 2, 1, NOW(), NOW()),
('zhaoliu',  'e10adc3949ba59abbe56e057f20f883e', '赵主管', '13800002222', 1, 1, NOW(), NOW()),
('sunqi',    'e10adc3949ba59abbe56e057f20f883e', '孙师傅', '13800003333', 2, 1, NOW(), NOW()),
('viewer01', 'e10adc3949ba59abbe56e057f20f883e', '查阅员', '13800004444', 2, 1, NOW(), NOW());

-- 为新增用户分配角色 (admin=1, operator=2, viewer=3)
INSERT INTO t_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM t_user u, t_role r
WHERE u.username IN ('wangwu', 'sunqi')   AND r.role_code = 'operator'
   OR u.username = 'zhaoliu'               AND r.role_code = 'admin'
   OR u.username = 'viewer01'              AND r.role_code = 'viewer';

-- ============================================================
-- 2. 订单 (多种状态，覆盖生命周期)
-- ============================================================

INSERT INTO t_order (user_id, order_no, customer_id, customer_name, customer_address, process_name, order_status, status_history, raw_material_json, remnant_json, config_json, create_time, update_time, remark) VALUES
-- 待审核
(2, 'ORD20260509001', 1, '张三', '福建省福州市仓山区', '常规切割', 0,
 '[]', '[]', '[]', '{"gap":5,"allowRotation":true}', NOW(), NOW(), '厨房柜门板材切割'),
-- 已审核
(2, 'ORD20260509002', 2, '福州恒通建材有限公司', '福建省福州市晋安区', '高精度切割', 1,
 '[]', '[]', '[]', '{"gap":3,"allowRotation":true}', NOW(), NOW(), '办公家具定制'),
-- 排样中
(3, 'ORD20260509003', 3, '李四', '福建省福州市台江区', '常规切割', 2,
 '[]', '[]', '[]', '{"gap":5,"allowRotation":false}', NOW(), NOW(), '卧室衣柜门板'),
-- 已排样
(2, 'ORD20260509004', 4, '福州鑫盛木业有限公司', '福建省福州市马尾区', '精密切割', 3,
 '[]', '[]', '[]', '{"gap":4,"allowRotation":true}', NOW(), NOW(), '酒店装修板材'),
-- 生产中
(3, 'ORD20260509005', 5, '王五', '福建省福州市鼓楼区', '常规切割', 4,
 '[]', '[]', '[]', '{"gap":5,"allowRotation":true}', NOW(), NOW(), '书房书柜门板'),
-- 已完工
(2, 'ORD20260509006', 6, '福州宏远装饰工程有限公司', '福建省福州市长乐区', '常规切割', 5,
 '[]', '[]', '[]', '{"gap":5,"allowRotation":true}', NOW(), NOW(), '商场展柜定制'),
-- 已交付
(3, 'ORD20260509007', 7, '赵六', '福建省福州市闽侯县', '高精度切割', 6,
 '[]', '[]', '[]', '{"gap":3,"allowRotation":true}', NOW(), NOW(), '别墅门窗套板'),
-- 已驳回
(2, 'ORD20260509008', 8, '福州华瑞板材加工厂', '福建省福州市连江县', '常规切割', -1,
 '[]', '[]', '[]', '{"gap":5,"allowRotation":true}', NOW(), NOW(), '规格不符已驳回'),
-- 已取消
(3, 'ORD20260509009', 9, '孙七', '福建省福州市福清市', '常规切割', -2,
 '[]', '[]', '[]', '{"gap":5,"allowRotation":false}', NOW(), NOW(), '客户主动取消'),
-- 排样失败
(2, 'ORD20260509010', 10, '福州启航家居有限公司', '福建省福州市平潭县', '精密切割', -3,
 '[]', '[]', '[]', '{"gap":4,"allowRotation":true}', NOW(), NOW(), '板材不足无法排样');

-- ============================================================
-- 3. 订单明细 (零件清单)
-- ============================================================

-- 订单1 (待审核): 厨房柜门板材 — 3种零件
INSERT INTO t_order_item (user_id, order_id, part_name, part_code, board_id, width, length, thickness, quantity, material_name, color, edge_left, edge_right, edge_front, edge_back, allow_rotation, label, create_time) VALUES
(2, 2, '上柜门板', 'KM-001', 1, 400, 600, 18, 4, '实木颗粒板', '暖白色', 1, 1, 1, 0, 1, '上柜-暖白', NOW()),
(2, 2, '下柜门板', 'KM-002', 1, 450, 700, 18, 4, '实木颗粒板', '暖白色', 1, 1, 1, 0, 1, '下柜-暖白', NOW()),
(2, 2, '抽屉面板', 'KM-003', 2, 350, 200, 15, 6, '生态板', '浅灰色', 1, 1, 0, 0, 0, '抽屉-灰', NOW());

-- 订单2 (已审核): 办公家具定制 — 2种零件
INSERT INTO t_order_item (user_id, order_id, part_name, part_code, board_id, width, length, thickness, quantity, material_name, color, edge_left, edge_right, edge_front, edge_back, allow_rotation, label, create_time) VALUES
(2, 3, '办公桌面板', 'BG-001', 3, 600, 1200, 18, 2, '多层实木板', '胡桃色', 1, 1, 1, 1, 1, '桌板-胡桃', NOW()),
(2, 3, '办公桌侧板', 'BG-002', 3, 500, 700, 18, 4, '多层实木板', '胡桃色', 1, 0, 1, 0, 0, '侧板-胡桃', NOW());

-- 订单3 (排样中): 卧室衣柜门板 — 4种零件
INSERT INTO t_order_item (user_id, order_id, part_name, part_code, board_id, width, length, thickness, quantity, material_name, color, edge_left, edge_right, edge_front, edge_back, allow_rotation, label, create_time) VALUES
(3, 4, '衣柜左门', 'YG-001', 4, 500, 1800, 12, 2, '密度板', '象牙白', 1, 1, 1, 1, 1, '左门-象牙白', NOW()),
(3, 4, '衣柜右门', 'YG-002', 4, 500, 1800, 12, 2, '密度板', '象牙白', 1, 1, 1, 1, 1, '右门-象牙白', NOW()),
(3, 4, '衣柜顶板', 'YG-003', 4, 550, 1000, 12, 1, '密度板', '象牙白', 0, 0, 1, 0, 0, '顶板-象牙白', NOW()),
(3, 4, '衣柜底板', 'YG-004', 4, 550, 1000, 12, 1, '密度板', '象牙白', 0, 0, 1, 0, 0, '底板-象牙白', NOW());

-- 订单4 (已排样): 酒店装修 — 3种零件
INSERT INTO t_order_item (user_id, order_id, part_name, part_code, board_id, width, length, thickness, quantity, material_name, color, edge_left, edge_right, edge_front, edge_back, allow_rotation, label, create_time) VALUES
(2, 5, '床头背板', 'JD-001', 5, 600, 1500, 25, 1, '实木多层板', '深灰色', 1, 0, 1, 0, 0, '背板-深灰', NOW()),
(2, 5, '床头柜面板', 'JD-002', 5, 400, 500, 25, 2, '实木多层板', '深灰色', 1, 1, 1, 1, 1, '床头柜-深灰', NOW()),
(2, 5, '墙板装饰条', 'JD-003', 6, 100, 2440, 18, 8, '颗粒板', '原木色', 1, 1, 0, 0, 1, '装饰条-原木', NOW());

-- 订单5 (生产中): 书房书柜 — 3种零件
INSERT INTO t_order_item (user_id, order_id, part_name, part_code, board_id, width, length, thickness, quantity, material_name, color, edge_left, edge_right, edge_front, edge_back, allow_rotation, label, create_time) VALUES
(3, 6, '书柜侧板', 'SG-001', 7, 350, 2000, 15, 2, '生态板', '浅木色', 1, 0, 1, 0, 0, '侧板-浅木', NOW()),
(3, 6, '书柜隔板', 'SG-002', 7, 300, 800, 15, 4, '生态板', '浅木色', 1, 1, 0, 0, 1, '隔板-浅木', NOW()),
(3, 6, '书柜顶底板', 'SG-003', 7, 350, 800, 15, 2, '生态板', '浅木色', 0, 0, 1, 1, 0, '顶底-浅木', NOW());

-- 订单6 (已完工): 商场展柜 — 2种零件
INSERT INTO t_order_item (user_id, order_id, part_name, part_code, board_id, width, length, thickness, quantity, material_name, color, edge_left, edge_right, edge_front, edge_back, allow_rotation, label, create_time) VALUES
(2, 7, '展柜面板', 'ZG-001', 8, 500, 1200, 12, 3, '密度板', '奶咖色', 1, 1, 1, 0, 1, '面板-奶咖', NOW()),
(2, 7, '展柜底座', 'ZG-002', 8, 500, 300, 12, 2, '密度板', '奶咖色', 1, 1, 0, 0, 0, '底座-奶咖', NOW());

-- 订单7 (已交付): 别墅门窗 — 3种零件
INSERT INTO t_order_item (user_id, order_id, part_name, part_code, board_id, width, length, thickness, quantity, material_name, color, edge_left, edge_right, edge_front, edge_back, allow_rotation, label, create_time) VALUES
(3, 8, '门套板', 'BS-001', 9, 200, 2200, 20, 3, '多层实木板', '黑色', 1, 1, 0, 0, 0, '门套-黑色', NOW()),
(3, 8, '窗套板', 'BS-002', 9, 150, 1500, 20, 4, '多层实木板', '黑色', 1, 1, 0, 0, 0, '窗套-黑色', NOW()),
(3, 8, '门槛板', 'BS-003', 10, 250, 900, 18, 1, '颗粒板', '浅棕色', 0, 0, 1, 0, 1, '门槛-浅棕', NOW());

-- 订单8 (已驳回): 无明细（驳回订单通常不保留明细）
-- 订单9 (已取消): 少量零件
INSERT INTO t_order_item (user_id, order_id, part_name, part_code, board_id, width, length, thickness, quantity, material_name, color, edge_left, edge_right, edge_front, edge_back, allow_rotation, label, create_time) VALUES
(3, 10, '简易搁板', 'GB-001', 1, 300, 800, 18, 2, '实木颗粒板', '暖白色', 1, 1, 0, 0, 1, '搁板-暖白', NOW());

-- 订单10 (排样失败): 无明细

-- ============================================================
-- 4. 余料
-- ============================================================

INSERT INTO t_offcut (user_id, board_id, source_order_id, width, length, thickness, material_type, brand, color, status, is_enabled, create_time, update_time) VALUES
(2, 1, 7, 300, 500, 18, '实木颗粒板', '兔宝宝', '暖白色', 0, 1, NOW(), NOW()),
(2, 3, 8, 200, 800, 18, '多层实木板', '千年舟', '胡桃色', 0, 1, NOW(), NOW()),
(3, 4, 4, 150, 400, 12, '密度板', '索菲亚', '象牙白', 1, 1, NOW(), NOW()),
(2, 6, 7, 250, 600, 18, '颗粒板', '好莱客', '原木色', 0, 1, NOW(), NOW()),
(3, 8, 7, 350, 450, 12, '密度板', '尚品宅配', '奶咖色', 0, 0, NOW(), NOW());

-- ============================================================
-- 5. 排版结果
-- ============================================================

INSERT INTO t_layout_result (order_id, user_id, usage_rate, total_area, container_count, result_json, create_time) VALUES
(7, 3, 0.8724, 5953600.00, 1,
 '{"solutions":[{"container":{"width":1220,"length":2440},"placements":[],"utilization":0.8724}]}',
 DATE_SUB(NOW(), INTERVAL 5 DAY)),

(5, 2, 0.9315, 7500000.00, 1,
 '{"solutions":[{"container":{"width":1220,"length":2440},"placements":[],"utilization":0.9315}]}',
 DATE_SUB(NOW(), INTERVAL 3 DAY)),

(8, 3, 0.8950, 8800000.00, 2,
 '{"solutions":[{"container":{"width":1220,"length":2440},"placements":[],"utilization":0.8950}]}',
 DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 5.1 订单6 (已排样) 绑定排版结果
UPDATE t_order SET layout_result_id = 2 WHERE order_id = 6;

-- 5.2 订单7 (已完工) 绑定排版结果
UPDATE t_order SET layout_result_id = 1 WHERE order_id = 7;

-- 5.3 订单8 (已交付) 绑定排版结果
UPDATE t_order SET layout_result_id = 3 WHERE order_id = 8;

-- ============================================================
-- 6. 生产任务
-- ============================================================

INSERT INTO t_production_task (user_id, order_id, layout_result_id, task_name, assignee_id, assignee_name, estimated_hours, actual_hours, status, start_time, complete_time, create_time, update_time) VALUES
-- 已完成任务
(2, 7, 1, '商场展柜切割', 3, '孙师傅', 4.0, 3.5, 2,
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- 已完成任务
(3, 8, 3, '别墅门窗切割', 4, '王工', 6.0, 5.5, 2,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 进行中任务
(2, 6, 2, '书房书柜切割', 4, '王工', 3.0, NULL, 1,
 DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 待生产任务
(3, 2, NULL, '厨房柜门切割', 3, '孙师傅', 2.5, NULL, 0,
 NULL, NULL, NOW(), NOW()),
-- 待生产任务
(2, 5, 2, '酒店装修切割', NULL, NULL, 5.0, NULL, 0,
 NULL, NULL, NOW(), NOW());

-- ============================================================
-- 7. 算法任务记录
-- ============================================================

INSERT INTO t_algorithm_task (task_id, user_id, algorithm, status, input_json, result_json, best_rate, container_count, duration_ms, create_time, complete_time) VALUES
('alg-001-20260507', 2, 'tabu_search', 2,
 '{"items":[],"containerSize":{"width":1220,"length":2440}}',
 '{"solutions":[]}', 0.8724, 1, 1230, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

('alg-002-20260508', 3, 'tabu_search', 2,
 '{"items":[],"containerSize":{"width":1220,"length":2440}}',
 '{"solutions":[]}', 0.9315, 1, 980, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

('alg-003-20260508', 2, 'genetic_algorithm', 2,
 '{"items":[],"containerSize":{"width":1220,"length":2440}}',
 '{"solutions":[]}', 0.8950, 2, 2450, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

('alg-004-20260509', 3, 'tabu_search', -1,
 '{"items":[],"containerSize":{"width":1220,"length":2440}}',
 NULL, NULL, NULL, NULL, NOW(), NULL);

-- ============================================================
-- 8. 审计日志样例
-- ============================================================

INSERT INTO t_audit_log (user_id, username, module, action, target_class, target_method, request_params, ip_address, duration_ms, status, create_time) VALUES
(1, 'admin', '用户', '创建', 'TUserController', 'createUser', '{"username":"wangwu","realName":"王工"}', '127.0.0.1', 45, 0, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(1, 'admin', '用户', '更新', 'TUserController', 'updateUser', '{"userId":2,"accountStatus":1}', '127.0.0.1', 38, 0, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2, 'zhangsan', '订单', '创建', 'OrderController', 'createOrder', '{"customerId":1,"processName":"常规切割"}', '127.0.0.1', 120, 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 'zhangsan', '订单', '创建', 'OrderController', 'createOrder', '{"customerId":2,"processName":"高精度切割"}', '127.0.0.1', 95, 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 'lisi', '订单', '创建', 'OrderController', 'createOrder', '{"customerId":3,"processName":"常规切割"}', '127.0.0.1', 88, 0, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 'zhangsan', '算法', '执行', 'AlgorithmController', 'submitAlgorithm', '{"algorithm":"tabu_search"}', '127.0.0.1', 1250, 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 'lisi', '算法', '执行', 'AlgorithmController', 'submitAlgorithm', '{"algorithm":"tabu_search"}', '127.0.0.1', 1000, 0, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 'zhangsan', '算法', '执行', 'AlgorithmController', 'submitAlgorithm', '{"algorithm":"genetic_algorithm"}', '127.0.0.1', 2480, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 'admin', '订单', '更新', 'OrderController', 'updateOrderStatus', '{"orderId":4,"orderStatus":-1}', '127.0.0.1', 52, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'zhangsan', '生产任务', '分配', 'ProductionTaskController', 'assignTask', '{"taskId":1,"assigneeId":3}', '127.0.0.1', 67, 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 'lisi', '生产任务', '分配', 'ProductionTaskController', 'assignTask', '{"taskId":2,"assigneeId":4}', '127.0.0.1', 55, 0, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 'zhangsan', '生产任务', '创建', 'ProductionTaskController', 'createTask', '{"orderId":2,"taskName":"厨房柜门切割"}', '127.0.0.1', 72, 0, NOW());
