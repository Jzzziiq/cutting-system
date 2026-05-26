const pptxgen = require("pptxgenjs");

let pres = new pptxgen();
pres.layout = 'LAYOUT_16x9';
pres.author = '青舶';
pres.title = '柜门板材切割排版系统 - 毕业设计答辩';

// ============ 配色方案 - "工业锻造"风格 ============
const colors = {
  primary:    '1B2130',   // 深炭灰（封面/致谢背景）
  secondary:  '2C3347',   // 钢蓝灰（卡片标题、装饰）
  accent:     'E8893C',   // 温暖琥珀/铜色 - 标志性颜色
  accentAlt:  'D4722A',   // 深铜色（悬停状态）
  data:       '4A90D9',   // 亮蓝色（图表、次要数据）
  success:    '27AE60',   // 绿色（通过率、正向指标）
  danger:     'EB5757',   // 红色（警报、对比）
  text:       '1B2130',   // 近黑色（主要文字）
  lightText:  '6B7394',   // 柔和蓝灰色（次要文字）
  white:      'FFFFFF',
  bg:         'F4F5F9',   // 温暖浅灰（幻灯片背景）
  cardBg:     'FFFFFF',
  border:     'E0E3EB',
  cardAccent: 'FFF3EB'    // 极浅琥珀色调（卡片强调背景）
};

// ============ 图标配置 ============
const sectionIcons = [
  { shape: 'CHEVRON',        color: 'E8893C', bg: 'FFF3EB' },  // 研究背景
  { shape: 'HEXAGON',        color: '4A90D9', bg: 'EBF3FC' },  // 研究现状
  { shape: 'CUBE',           color: '27AE60', bg: 'E8F8F0' },  // 需求分析
  { shape: 'DIAMOND',        color: '4A90D9', bg: 'EBF3FC' },  // 架构设计
  { shape: 'LIGHTNING_BOLT', color: 'E8893C', bg: 'FFF3EB' },  // 数据库
  { shape: 'GEAR_6',         color: 'F2994A', bg: 'FFF3EB' },  // 算法
  { shape: 'GEAR_9',         color: '4A90D9', bg: 'EBF3FC' },  // 前端
  { shape: 'STAR_5_POINT',   color: '27AE60', bg: 'E8F8F0' },  // 后端
  { shape: 'CROSS',          color: '27AE60', bg: 'E8F8F0' },  // 测试
  { shape: 'OVAL',           color: 'E8893C', bg: 'FFF3EB' }   // 总结
];

// ============ 辅助函数 ============
const makeShadow = () => ({ type: "outer", blur: 6, offset: 2, color: "000000", opacity: 0.12, angle: 135 });

function addSectionTitle(slide, num, title) {
  // 大章节编号作为背景水印
  slide.addText(num, {
    x: 0.3, y: 0.15, w: 2, h: 1.5,
    fontSize: 80, fontFace: "Arial",
    color: colors.accent,
    transparency: 85, bold: true,
    align: 'left', valign: 'top'
  });

  // 标题文字
  slide.addText(`${num} ${title}`, {
    x: 0.5, y: 0.4, w: 9, h: 0.8,
    fontSize: 40, fontFace: "Microsoft YaHei",
    color: colors.primary, bold: true,
    charSpacing: 6,
    align: 'left', valign: 'middle'
  });
}

function addIcon(slide, shape, color, bgColor, x, y, size = 0.5) {
  // 背景圆圈
  slide.addShape(pres.shapes.OVAL, {
    x: x, y: y, w: size + 0.15, h: size + 0.15,
    fill: { color: bgColor }
  });
  // 图标形状
  slide.addShape(pres.shapes[shape], {
    x: x + 0.075, y: y + 0.075, w: size, h: size,
    fill: { color: color }
  });
}

function addBigNumber(slide, number, label, x, y, color = colors.accent) {
  // 大数字
  slide.addText(number, {
    x: x, y: y, w: 2.5, h: 0.8,
    fontSize: 48, fontFace: "Arial",
    color: color, bold: true,
    align: 'left', valign: 'bottom',
    charSpacing: 2
  });
  // 标签
  slide.addText(label, {
    x: x, y: y + 0.7, w: 2.5, h: 0.4,
    fontSize: 14, fontFace: "Microsoft YaHei",
    color: colors.lightText,
    align: 'left', valign: 'top'
  });
}

function addCard(slide, x, y, w, h, options = {}) {
  const card = {
    x: x, y: y, w: w, h: h,
    fill: { color: colors.cardBg },
    shadow: makeShadow()
  };

  slide.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    ...card,
    rectRadius: 0.1
  });

  // 左边框装饰
  if (options.accent) {
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: y, w: 0.08, h: h,
      fill: { color: options.accent }
    });
  }
}

function addCardHeader(slide, x, y, w, title, iconShape, iconColor, iconBg) {
  // 图标
  addIcon(slide, iconShape, iconColor, iconBg, x + 0.15, y + 0.15, 0.4);

  // 标题
  slide.addText(title, {
    x: x + 0.7, y: y, w: w - 0.8, h: 0.6,
    fontSize: 22, fontFace: "Microsoft YaHei",
    color: colors.primary, bold: true,
    align: 'left', valign: 'middle',
    margin: 0
  });
}

// ============ 第1页：封面 ============
let slide1 = pres.addSlide();
slide1.background = { color: colors.primary };

// 装饰性齿轮（背景纹理）
slide1.addShape(pres.shapes.GEAR_6, {
  x: 6.5, y: 0.5, w: 4, h: 4,
  fill: { color: colors.white, transparency: 92 }
});

// 左侧双装饰条
slide1.addShape(pres.shapes.RECTANGLE, {
  x: 0, y: 0, w: 0.08, h: 5.625,
  fill: { color: colors.secondary }
});
slide1.addShape(pres.shapes.RECTANGLE, {
  x: 0.12, y: 0, w: 0.08, h: 5.625,
  fill: { color: colors.accent }
});

// 主标题
slide1.addText("柜门板材切割排版系统", {
  x: 0.8, y: 1.0, w: 8.5, h: 1.2,
  fontSize: 48, fontFace: "Microsoft YaHei",
  color: colors.white, bold: true,
  charSpacing: 12,
  align: 'left', valign: 'middle'
});

// 副标题
slide1.addText("毕业设计（论文）答辩", {
  x: 0.8, y: 2.2, w: 8.5, h: 0.8,
  fontSize: 28, fontFace: "Microsoft YaHei",
  color: colors.accent,
  charSpacing: 4,
  align: 'left', valign: 'middle'
});

// 信息区域
slide1.addText([
  { text: "答辩人：XXX", options: { breakLine: true, fontSize: 16, color: colors.white } },
  { text: "指导教师：XXX 教授", options: { breakLine: true, fontSize: 16, color: colors.white } },
  { text: "专    业：软件工程", options: { breakLine: true, fontSize: 16, color: colors.white } },
  { text: "日    期：2026年5月", options: { fontSize: 16, color: colors.white } }
], {
  x: 0.8, y: 3.3, w: 5, h: 1.8,
  fontFace: "Microsoft YaHei",
  align: 'left', valign: 'top',
  lineSpacingMultiple: 1.6
});

// ============ 第2页：目录 ============
let slide2 = pres.addSlide();
slide2.background = { color: colors.bg };

// 标题
slide2.addText("目 录", {
  x: 0.5, y: 0.4, w: 9, h: 0.8,
  fontSize: 40, fontFace: "Microsoft YaHei",
  color: colors.primary, bold: true,
  charSpacing: 8,
  align: 'left', valign: 'middle'
});

// 目录内容
const tocItems = [
  { num: '01', title: '研究背景与意义', icon: sectionIcons[0] },
  { num: '02', title: '国内外研究现状', icon: sectionIcons[1] },
  { num: '03', title: '系统需求分析', icon: sectionIcons[2] },
  { num: '04', title: '系统架构设计', icon: sectionIcons[3] },
  { num: '05', title: '数据库设计', icon: sectionIcons[4] },
  { num: '06', title: '算法设计与实现', icon: sectionIcons[5] },
  { num: '07', title: '系统实现 - 前端', icon: sectionIcons[6] },
  { num: '08', title: '系统实现 - 后端', icon: sectionIcons[7] },
  { num: '09', title: '系统测试', icon: sectionIcons[8] },
  { num: '10', title: '总结与展望', icon: sectionIcons[9] }
];

// 左列
tocItems.slice(0, 5).forEach((item, idx) => {
  const yPos = 1.5 + idx * 0.75;

  // 彩色左边框
  slide2.addShape(pres.shapes.RECTANGLE, {
    x: 0.8, y: yPos, w: 0.06, h: 0.55,
    fill: { color: item.icon.color }
  });

  // 图标
  addIcon(slide2, item.icon.shape, item.icon.color, item.icon.bg, 1.0, yPos + 0.03, 0.4);

  // 标题文字
  slide2.addText(item.title, {
    x: 1.7, y: yPos, w: 3.5, h: 0.55,
    fontSize: 16, fontFace: "Microsoft YaHei",
    color: colors.text,
    align: 'left', valign: 'middle',
    margin: 0
  });
});

// 右列
tocItems.slice(5).forEach((item, idx) => {
  const yPos = 1.5 + idx * 0.75;

  // 彩色左边框
  slide2.addShape(pres.shapes.RECTANGLE, {
    x: 5.5, y: yPos, w: 0.06, h: 0.55,
    fill: { color: item.icon.color }
  });

  // 图标
  addIcon(slide2, item.icon.shape, item.icon.color, item.icon.bg, 5.7, yPos + 0.03, 0.4);

  // 标题文字
  slide2.addText(item.title, {
    x: 6.4, y: yPos, w: 3.5, h: 0.55,
    fontSize: 16, fontFace: "Microsoft YaHei",
    color: colors.text,
    align: 'left', valign: 'middle',
    margin: 0
  });
});

// ============ 第3页：研究背景与意义 ============
let slide3 = pres.addSlide();
slide3.background = { color: colors.bg };

addSectionTitle(slide3, '01', '研究背景与意义');

// 大数字突出显示
addBigNumber(slide3, '5000+', '亿元市场规模', 6.5, 0.6, colors.accent);

// 左侧卡片 - 行业背景
addCard(slide3, 0.5, 1.5, 4.3, 3.5, { accent: colors.accent });
addCardHeader(slide3, 0.5, 1.5, 4.3, '行业背景', 'CHEVRON', colors.accent, colors.cardAccent);

slide3.addText([
  { text: "定制家具市场规模持续扩大", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "板材切割是定制家具生产的核心环节", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "传统人工排版效率低、浪费严重", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "自动化排版系统可提升材料利用率15%-20%", options: { bullet: true, fontSize: 15 } }
], {
  x: 0.8, y: 2.2, w: 3.7, h: 2.5,
  fontFace: "Microsoft YaHei",
  color: colors.text,
  align: 'left', valign: 'top',
  lineSpacingMultiple: 1.5
});

// 右侧卡片 - 研究意义
addCard(slide3, 5.2, 2.6, 4.3, 2.4, { accent: colors.data });
addCardHeader(slide3, 5.2, 2.6, 4.3, '研究意义', 'HEXAGON', colors.data, 'EBF3FC');

slide3.addText([
  { text: "降低企业生产成本，提高经济效益", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "减少材料浪费，响应绿色制造政策", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "推动家具制造业数字化转型升级", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "为同类排样问题提供技术参考", options: { bullet: true, fontSize: 15 } }
], {
  x: 5.5, y: 3.3, w: 3.7, h: 1.5,
  fontFace: "Microsoft YaHei",
  color: colors.text,
  align: 'left', valign: 'top',
  lineSpacingMultiple: 1.4
});

// DOUGHNUT图表
slide3.addChart(pres.charts.DOUGHNUT, [
  { name: '市场规模', labels: ['定制家具', '板材加工', '智能排版'], values: [45, 30, 25] }
], {
  x: 5.2, y: 1.5, w: 2.5, h: 1.8,
  showTitle: false,
  showPercent: true,
  showLabel: true,
  dataLabelPosition: 'outEnd',
  chartColors: [colors.accent, colors.data, colors.success],
  holeSize: 45
});

// ============ 第4页：国内外研究现状 ============
let slide4 = pres.addSlide();
slide4.background = { color: colors.bg };

addSectionTitle(slide4, '02', '国内外研究现状');

// 国外研究 - 时间线风格
addCard(slide4, 0.5, 1.5, 4.3, 3.5);
addCardHeader(slide4, 0.5, 1.5, 4.3, '国外研究', 'CLOUD', colors.data, 'EBF3FC');

slide4.addText([
  { text: "20世纪60年代开始研究排样问题", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "提出了遗传算法、模拟退火等智能算法", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "商业软件如CutRite、Maxcut已成熟应用", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "算法优化和3D排样成为研究热点", options: { bullet: true, fontSize: 15 } }
], {
  x: 0.8, y: 2.2, w: 3.7, h: 2.5,
  fontFace: "Microsoft YaHei",
  color: colors.text,
  align: 'left', valign: 'top',
  lineSpacingMultiple: 1.5
});

// 国内研究
addCard(slide4, 5.2, 1.5, 4.3, 3.5);
addCardHeader(slide4, 5.2, 1.5, 4.3, '国内研究', 'HEXAGON', colors.accent, colors.cardAccent);

slide4.addText([
  { text: "起步较晚，但发展迅速", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "高校和企业合作研发排样系统", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "国产软件逐步替代进口产品", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "中小型家具企业需求旺盛", options: { bullet: true, fontSize: 15 } }
], {
  x: 5.5, y: 2.2, w: 3.7, h: 2.5,
  fontFace: "Microsoft YaHei",
  color: colors.text,
  align: 'left', valign: 'top',
  lineSpacingMultiple: 1.5
});

// ============ 第5页：系统需求分析 ============
let slide5 = pres.addSlide();
slide5.background = { color: colors.bg };

addSectionTitle(slide5, '03', '系统需求分析');

// 大数字突出显示
addBigNumber(slide5, '3', '大功能模块', 0.5, 0.6, colors.accent);

// 功能需求 - 3列布局
const funcReqs = [
  { title: '用户管理', items: ['登录注册', '角色权限', '组织管理'], icon: 'HEXAGON', color: colors.data },
  { title: '业务管理', items: ['客户管理', '订单管理', '余料管理'], icon: 'DIAMOND', color: colors.accent },
  { title: '核心功能', items: ['智能排样', '结果可视化', '数据统计'], icon: 'STAR_5_POINT', color: colors.success }
];

funcReqs.forEach((req, idx) => {
  const xPos = 0.5 + idx * 3.2;

  // 卡片背景
  addCard(slide5, xPos, 1.5, 2.8, 3.5);

  // 图标
  addIcon(slide5, req.icon, req.color, colors.cardBg, xPos + 1.0, 1.65, 0.5);

  // 标题
  slide5.addText(req.title, {
    x: xPos, y: 2.3, w: 2.8, h: 0.5,
    fontSize: 18, fontFace: "Microsoft YaHei",
    color: colors.primary, bold: true,
    align: 'center', valign: 'middle'
  });

  // 内容列表
  slide5.addText(
    req.items.map((item, i) => ({
      text: item,
      options: { bullet: true, breakLine: i < req.items.length - 1, fontSize: 14 }
    })),
    {
      x: xPos + 0.3, y: 2.9, w: 2.2, h: 1.8,
      fontFace: "Microsoft YaHei",
      color: colors.text,
      align: 'left', valign: 'top',
      lineSpacingMultiple: 1.6
    }
  );
});

// RADAR图表
slide5.addChart(pres.charts.RADAR, [
  { name: '功能覆盖', labels: ['用户管理', '订单管理', '排样算法', '数据统计', '余料管理', '权限控制'], values: [90, 95, 100, 85, 88, 92] }
], {
  x: 0.5, y: 4.0, w: 3.0, h: 1.5,
  showTitle: false,
  lineSize: 2,
  chartColors: [colors.accent],
  catAxisLabelColor: colors.text,
  valAxisLabelColor: colors.lightText
});

// ============ 第6页：系统架构设计 ============
let slide6 = pres.addSlide();
slide6.background = { color: colors.bg };

addSectionTitle(slide6, '04', '系统架构设计');

// 技术架构图 - 分层展示
const layers = [
  { name: '表现层', tech: 'Vue 3 + Element Plus + Canvas', color: '3B82F6', y: 1.5, icon: 'CHEVRON' },
  { name: '接口层', tech: 'Spring Boot REST API + JWT', color: '10B981', y: 2.3, icon: 'DIAMOND' },
  { name: '业务层', tech: '用户/订单/排样/余料服务', color: 'F59E0B', y: 3.1, icon: 'GEAR_6' },
  { name: '数据层', tech: 'MyBatis-Plus + MySQL 8.0', color: 'EF4444', y: 3.9, icon: 'CUBE' }
];

layers.forEach((layer) => {
  // 层背景
  slide6.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: 1.5, y: layer.y, w: 7, h: 0.65,
    fill: { color: layer.color, transparency: 15 },
    line: { color: layer.color, width: 1.5 },
    rectRadius: 0.05
  });

  // 图标
  addIcon(slide6, layer.icon, layer.color, colors.cardBg, 1.7, layer.y + 0.08, 0.35);

  // 层名称
  slide6.addText(layer.name, {
    x: 2.2, y: layer.y, w: 1.5, h: 0.65,
    fontSize: 16, fontFace: "Microsoft YaHei",
    color: layer.color, bold: true,
    align: 'left', valign: 'middle'
  });

  // 技术说明
  slide6.addText(layer.tech, {
    x: 3.7, y: layer.y, w: 4.5, h: 0.65,
    fontSize: 14, fontFace: "Microsoft YaHei",
    color: colors.text,
    align: 'left', valign: 'middle'
  });
});

// 箭头连接器
slide6.addShape(pres.shapes.DOWN_ARROW, {
  x: 4.8, y: 2.15, w: 0.3, h: 0.15,
  fill: { color: colors.lightText }
});
slide6.addShape(pres.shapes.DOWN_ARROW, {
  x: 4.8, y: 2.95, w: 0.3, h: 0.15,
  fill: { color: colors.lightText }
});
slide6.addShape(pres.shapes.DOWN_ARROW, {
  x: 4.8, y: 3.75, w: 0.3, h: 0.15,
  fill: { color: colors.lightText }
});

// ============ 第7页：数据库设计 ============
let slide7 = pres.addSlide();
slide7.background = { color: colors.bg };

addSectionTitle(slide7, '05', '数据库设计');

// 大数字突出显示
addBigNumber(slide7, '6', '张核心数据表', 6.5, 0.6, colors.accent);

// 核心表结构展示 - 表格形式
const tableHeaders = ['表名', '说明', '核心字段'];
const tableData = [
  ['sys_user', '用户表', 'id, username, password, role_id'],
  ['customer', '客户表', 'id, name, phone, address'],
  ['order', '订单表', 'id, customer_id, status, create_time'],
  ['board', '板材表', 'id, length, width, thickness, material'],
  ['offcut', '余料表', 'id, board_id, length, width, is_used'],
  ['layout_result', '排样结果', 'id, order_id, utilization_rate, svg_data']
];

// 表格背景
addCard(slide7, 0.5, 1.5, 5.5, 3.5);

// 表格
slide7.addTable(
  [
    [
      { text: tableHeaders[0], options: { fill: { color: colors.secondary }, color: colors.white, bold: true, fontSize: 12 } },
      { text: tableHeaders[1], options: { fill: { color: colors.secondary }, color: colors.white, bold: true, fontSize: 12 } },
      { text: tableHeaders[2], options: { fill: { color: colors.secondary }, color: colors.white, bold: true, fontSize: 12 } }
    ],
    ...tableData.map((row, idx) => [
      { text: row[0], options: { fill: { color: idx % 2 === 0 ? colors.cardAccent : colors.white }, fontSize: 11, fontFace: "Consolas" } },
      { text: row[1], options: { fill: { color: idx % 2 === 0 ? colors.cardAccent : colors.white }, fontSize: 11 } },
      { text: row[2], options: { fill: { color: idx % 2 === 0 ? colors.cardAccent : colors.white }, fontSize: 10, fontFace: "Consolas" } }
    ])
  ],
  {
    x: 0.7, y: 1.7, w: 5.1,
    colW: [1.5, 1.2, 2.4],
    border: { pt: 0.5, color: colors.border }
  }
);

// BAR图表
slide7.addChart(pres.charts.BAR, [
  { name: '字段数', labels: ['sys_user', 'customer', 'order', 'board', 'offcut', 'layout_result'], values: [8, 5, 6, 6, 6, 5] }
], {
  x: 6.3, y: 1.5, w: 3.2, h: 3.0,
  showTitle: false,
  showValue: true,
  chartColors: [colors.accent],
  catAxisLabelColor: colors.text,
  valAxisLabelColor: colors.lightText,
  catAxisLabelFontSize: 9,
  valAxisHidden: true,
  valGridLine: { style: 'none' }
});

// ============ 第8页：算法设计 ============
let slide8 = pres.addSlide();
slide8.background = { color: colors.bg };

addSectionTitle(slide8, '06', '算法设计与实现');

// 算法流程图
const steps = [
  { title: '输入数据', desc: '订单、板材、\n切割规则' },
  { title: '禁忌搜索', desc: '全局优化\n搜索策略' },
  { title: '天际线放置', desc: '空间管理\n智能排样' },
  { title: '输出结果', desc: '排样方案\n利用率计算' }
];

steps.forEach((step, idx) => {
  const xPos = 0.6 + idx * 2.4;

  // 步骤圆角矩形
  slide8.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: xPos, y: 1.6, w: 1.8, h: 1.8,
    fill: { color: colors.primary },
    shadow: makeShadow(),
    rectRadius: 0.1
  });

  // 步骤编号
  slide8.addText(String(idx + 1), {
    x: xPos, y: 1.6, w: 1.8, h: 1.8,
    fontSize: 32, fontFace: "Arial",
    color: colors.white, bold: true,
    align: 'center', valign: 'middle'
  });

  // 步骤标题
  slide8.addText(step.title, {
    x: xPos - 0.2, y: 3.6, w: 2.2, h: 0.5,
    fontSize: 16, fontFace: "Microsoft YaHei",
    color: colors.primary, bold: true,
    align: 'center', valign: 'middle'
  });

  // 步骤说明
  slide8.addText(step.desc, {
    x: xPos - 0.2, y: 4.1, w: 2.2, h: 0.8,
    fontSize: 12, fontFace: "Microsoft YaHei",
    color: colors.lightText,
    align: 'center', valign: 'top'
  });

  // 连接箭头
  if (idx < steps.length - 1) {
    slide8.addShape(pres.shapes.RIGHT_ARROW, {
      x: xPos + 1.8, y: 2.3, w: 0.5, h: 0.3,
      fill: { color: colors.accent }
    });
  }
});

// 关键点高亮
slide8.addShape(pres.shapes.ROUNDED_RECTANGLE, {
  x: 0.5, y: 5.0, w: 9, h: 0.5,
  fill: { color: colors.accent, transparency: 15 },
  rectRadius: 0.05
});

slide8.addText("核心创新：结合禁忌搜索全局优化 + 天际线算法空间管理，实现高效智能排样", {
  x: 0.7, y: 5.0, w: 8.6, h: 0.5,
  fontSize: 13, fontFace: "Microsoft YaHei",
  color: colors.primary, bold: true,
  align: 'center', valign: 'middle'
});

// ============ 第9页：系统实现 - 前端 ============
let slide9 = pres.addSlide();
slide9.background = { color: colors.bg };

addSectionTitle(slide9, '07', '系统实现 - 前端');

// 功能卡片 - 2x2布局
const frontendFeatures = [
  { title: '排样可视化', desc: 'Canvas 2D实时渲染\n支持缩放、拖拽、旋转', icon: 'OVAL', color: colors.data },
  { title: '订单管理', desc: '订单CRUD、状态流转\n批量操作、数据导出', icon: 'RECTANGLE', color: colors.accent },
  { title: '客户管理', desc: '客户信息维护\n联系人管理、地址管理', icon: 'HEXAGON', color: colors.success },
  { title: '余料管理', desc: '余料登记、绑定\n智能推荐可用余料', icon: 'DIAMOND', color: colors.data }
];

frontendFeatures.forEach((feat, idx) => {
  const row = Math.floor(idx / 2);
  const col = idx % 2;
  const xPos = 0.5 + col * 4.8;
  const yPos = 1.5 + row * 1.8;

  // 卡片
  addCard(slide9, xPos, yPos, 4.3, 1.5, { accent: feat.color });

  // 图标
  addIcon(slide9, feat.icon, feat.color, colors.cardBg, xPos + 0.2, yPos + 0.15, 0.4);

  // 标题
  slide9.addText(feat.title, {
    x: xPos + 0.7, y: yPos + 0.15, w: 3.3, h: 0.4,
    fontSize: 16, fontFace: "Microsoft YaHei",
    color: colors.primary, bold: true,
    align: 'left', valign: 'middle',
    margin: 0
  });

  // 描述
  slide9.addText(feat.desc, {
    x: xPos + 0.7, y: yPos + 0.6, w: 3.3, h: 0.7,
    fontSize: 12, fontFace: "Microsoft YaHei",
    color: colors.lightText,
    align: 'left', valign: 'top',
    margin: 0
  });
});

// 系统截图
slide9.addImage({
  path: 'F:/Code/Java/cutting-system/target/frontend-algorithm-smoke.png',
  x: 5.5, y: 3.5, w: 4.0, h: 1.8,
  rounding: true
});

// ============ 第10页：系统实现 - 后端 ============
let slide10 = pres.addSlide();
slide10.background = { color: colors.bg };

addSectionTitle(slide10, '08', '系统实现 - 后端');

// 大数字突出显示
addBigNumber(slide10, '4', '大核心技术', 0.5, 0.6, colors.accent);

// 技术卡片 - 2x2布局
const backendFeatures = [
  { title: 'RESTful API', desc: '统一Result返回结构\n规范接口契约', icon: 'DIAMOND', color: colors.data },
  { title: 'JWT认证', desc: 'Spring MVC拦截器\nToken无状态认证', icon: 'HEXAGON', color: colors.accent },
  { title: 'RBAC权限', desc: '多组织、多角色\n细粒度权限管理', icon: 'STAR_5_POINT', color: colors.success },
  { title: '算法接口', desc: 'POST /algorithm/answer\n返回排样方案', icon: 'GEAR_6', color: colors.data }
];

backendFeatures.forEach((feat, idx) => {
  const row = Math.floor(idx / 2);
  const col = idx % 2;
  const xPos = 0.5 + col * 4.8;
  const yPos = 1.5 + row * 2.0;

  // 卡片
  addCard(slide10, xPos, yPos, 4.3, 1.7, { accent: feat.color });

  // 图标
  addIcon(slide10, feat.icon, feat.color, colors.cardBg, xPos + 0.2, yPos + 0.2, 0.5);

  // 标题
  slide10.addText(feat.title, {
    x: xPos + 0.8, y: yPos + 0.2, w: 3.2, h: 0.5,
    fontSize: 18, fontFace: "Microsoft YaHei",
    color: colors.primary, bold: true,
    align: 'left', valign: 'middle',
    margin: 0
  });

  // 描述
  slide10.addText(feat.desc, {
    x: xPos + 0.8, y: yPos + 0.8, w: 3.2, h: 0.7,
    fontSize: 13, fontFace: "Microsoft YaHei",
    color: colors.lightText,
    align: 'left', valign: 'top',
    margin: 0
  });
});

// ============ 第11页：系统测试 ============
let slide11 = pres.addSlide();
slide11.background = { color: colors.bg };

addSectionTitle(slide11, '09', '系统测试');

// 大数字突出显示
addBigNumber(slide11, '73', '个测试用例', 0.5, 0.6, colors.accent);

// 测试数据表格
const testHeaders = ['测试模块', '用例数', '通过率'];
const testData = [
  ['用户认证', '15', '100%'],
  ['订单管理', '20', '100%'],
  ['排样算法', '12', '100%'],
  ['余料管理', '8', '100%'],
  ['前端交互', '18', '100%']
];

// 表格背景
addCard(slide11, 0.5, 1.5, 5.0, 3.2);

// 表格
slide11.addTable(
  [
    [
      { text: testHeaders[0], options: { fill: { color: colors.secondary }, color: colors.white, bold: true, fontSize: 12 } },
      { text: testHeaders[1], options: { fill: { color: colors.secondary }, color: colors.white, bold: true, fontSize: 12 } },
      { text: testHeaders[2], options: { fill: { color: colors.secondary }, color: colors.white, bold: true, fontSize: 12 } }
    ],
    ...testData.map((row, idx) => [
      { text: row[0], options: { fill: { color: idx % 2 === 0 ? colors.cardAccent : colors.white }, fontSize: 12 } },
      { text: row[1], options: { fill: { color: idx % 2 === 0 ? colors.cardAccent : colors.white }, fontSize: 12, align: 'center' } },
      { text: row[2], options: { fill: { color: idx % 2 === 0 ? colors.cardAccent : colors.white }, fontSize: 12, align: 'center', color: colors.success, bold: true } }
    ])
  ],
  {
    x: 0.7, y: 1.7, w: 4.6,
    colW: [1.8, 1.2, 1.6],
    border: { pt: 0.5, color: colors.border }
  }
);

// BAR图表
slide11.addChart(pres.charts.BAR, [
  { name: '用例数', labels: ['用户认证', '订单管理', '排样算法', '余料管理', '前端交互'], values: [15, 20, 12, 8, 18] }
], {
  x: 5.8, y: 1.5, w: 3.8, h: 2.5,
  showTitle: false,
  showValue: true,
  chartColors: [colors.success],
  catAxisLabelColor: colors.text,
  valAxisLabelColor: colors.lightText,
  valAxisHidden: true,
  catAxisLabelFontSize: 10,
  valGridLine: { style: 'none' }
});

// 测试结论
slide11.addShape(pres.shapes.ROUNDED_RECTANGLE, {
  x: 5.8, y: 4.2, w: 3.8, h: 0.5,
  fill: { color: colors.success, transparency: 15 },
  rectRadius: 0.05
});

slide11.addText("100% 通过", {
  x: 5.8, y: 4.2, w: 3.8, h: 0.5,
  fontSize: 20, fontFace: "Arial",
  color: colors.success, bold: true,
  align: 'center', valign: 'middle'
});

// ============ 第12页：总结与展望 ============
let slide12 = pres.addSlide();
slide12.background = { color: colors.bg };

addSectionTitle(slide12, '10', '总结与展望');

// 工作总结
addCard(slide12, 0.5, 1.5, 4.3, 3.5, { accent: colors.accent });
addCardHeader(slide12, 0.5, 1.5, 4.3, '工作总结', 'CROSS', colors.accent, colors.cardAccent);

slide12.addText([
  { text: "完成了系统需求分析和架构设计", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "实现了基于禁忌搜索的智能排样算法", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "开发了完整的Web端和小程序端", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "实现了RBAC权限控制和多组织管理", options: { bullet: true, fontSize: 15 } }
], {
  x: 0.8, y: 2.2, w: 3.7, h: 2.5,
  fontFace: "Microsoft YaHei",
  color: colors.text,
  align: 'left', valign: 'top',
  lineSpacingMultiple: 1.5
});

// 未来展望
addCard(slide12, 5.2, 1.5, 4.3, 3.5, { accent: colors.data });
addCardHeader(slide12, 5.2, 1.5, 4.3, '未来展望', 'RIGHT_ARROW', colors.data, 'EBF3FC');

slide12.addText([
  { text: "引入3D排样算法", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "集成CAD图纸导入功能", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "对接MES生产系统", options: { bullet: true, breakLine: true, fontSize: 15 } },
  { text: "优化算法性能和用户体验", options: { bullet: true, fontSize: 15 } }
], {
  x: 5.5, y: 2.2, w: 3.7, h: 2.5,
  fontFace: "Microsoft YaHei",
  color: colors.text,
  align: 'left', valign: 'top',
  lineSpacingMultiple: 1.5
});

// ============ 第13页：致谢 ============
let slide13 = pres.addSlide();
slide13.background = { color: colors.primary };

// 装饰性齿轮（背景纹理）
slide13.addShape(pres.shapes.GEAR_6, {
  x: 7, y: 0, w: 4, h: 4,
  fill: { color: colors.white, transparency: 92 }
});
slide13.addShape(pres.shapes.GEAR_9, {
  x: 0, y: 3, w: 3, h: 3,
  fill: { color: colors.white, transparency: 95 }
});

// 左侧双装饰条
slide13.addShape(pres.shapes.RECTANGLE, {
  x: 0, y: 0, w: 0.08, h: 5.625,
  fill: { color: colors.secondary }
});
slide13.addShape(pres.shapes.RECTANGLE, {
  x: 0.12, y: 0, w: 0.08, h: 5.625,
  fill: { color: colors.accent }
});

// 感谢标题
slide13.addText("致 谢", {
  x: 1, y: 1.5, w: 8, h: 1,
  fontSize: 48, fontFace: "Microsoft YaHei",
  color: colors.white, bold: true,
  charSpacing: 12,
  align: 'center', valign: 'middle'
});

// 致谢内容
slide13.addText(
  "感谢指导教师的悉心指导\n感谢各位评委老师的宝贵意见\n感谢同学们的帮助与支持",
  {
    x: 1.5, y: 2.8, w: 7, h: 1.8,
    fontSize: 18, fontFace: "Microsoft YaHei",
    color: colors.accent,
    align: 'center', valign: 'middle',
    lineSpacingMultiple: 1.8
  }
);

// 底部信息
slide13.addText("请各位老师批评指正", {
  x: 1, y: 5.0, w: 8, h: 0.5,
  fontSize: 16, fontFace: "Microsoft YaHei",
  color: colors.white,
  align: 'center', valign: 'middle'
});

// 保存文件
pres.writeFile({ fileName: "F:/Code/Java/cutting-system/docs/ppt/毕业设计答辩-柜门板材切割排版系统.pptx" })
  .then(() => {
    console.log("PPT创建成功！");
  })
  .catch(err => {
    console.error("创建失败：", err);
  });
