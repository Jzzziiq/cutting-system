USE board_cutting_db;

-- 官方预设：平开门衣柜（materialSlot 占位，boardId 为空，is_official=1）
INSERT INTO t_cabinet_template (name, category, cabinet_json, is_official, created_by)
VALUES (
    '平开门衣柜',
    'wardrobe',
    '{
      "cabinet": {"name":"衣柜","room":"","purpose":"","width":1200,"height":2200,"depth":600},
      "boards": [
        {"id":"b-001","type":"side","displayName":"左侧板","materialSlot":"cabinet_body","boardId":null,"designLength":2200,"designWidth":600,"thickness":18,"position":{"x":-591,"y":1100,"z":0},"rotation":{"x":0,"y":0,"z":0},"placementFace":"left","connectedTo":[],"grain":"vertical","edgeBanding":{"left":false,"right":false,"top":true,"bottom":true},"edgeRole":{"left":"靠墙侧","right":"前口","top":"上端","bottom":"下端"},"hingeHoles":[]},
        {"id":"b-002","type":"side","displayName":"右侧板","materialSlot":"cabinet_body","boardId":null,"designLength":2200,"designWidth":600,"thickness":18,"position":{"x":591,"y":1100,"z":0},"rotation":{"x":0,"y":0,"z":0},"placementFace":"right","connectedTo":[],"grain":"vertical","edgeBanding":{"left":false,"right":false,"top":true,"bottom":true},"edgeRole":{"left":"靠墙侧","right":"前口","top":"上端","bottom":"下端"},"hingeHoles":[]},
        {"id":"b-003","type":"top","displayName":"顶板","materialSlot":"cabinet_body","boardId":null,"designLength":1200,"designWidth":600,"thickness":18,"position":{"x":0,"y":2191,"z":0},"rotation":{"x":0,"y":0,"z":0},"placementFace":"top","connectedTo":["b-001","b-002"],"grain":"horizontal","edgeBanding":{"left":false,"right":false,"top":false,"bottom":true},"edgeRole":{"left":"靠墙侧","right":"前口","top":"上端","bottom":"下端"},"hingeHoles":[]},
        {"id":"b-004","type":"bottom","displayName":"底板","materialSlot":"cabinet_body","boardId":null,"designLength":1200,"designWidth":600,"thickness":18,"position":{"x":0,"y":9,"z":0},"rotation":{"x":0,"y":0,"z":0},"placementFace":"bottom","connectedTo":["b-001","b-002"],"grain":"horizontal","edgeBanding":{"left":false,"right":false,"top":true,"bottom":false},"edgeRole":{"left":"靠墙侧","right":"前口","top":"上端","bottom":"下端"},"hingeHoles":[]},
        {"id":"b-005","type":"back","displayName":"背板","materialSlot":"back","boardId":null,"designLength":1164,"designWidth":2164,"thickness":5,"position":{"x":0,"y":1100,"z":-297.5},"rotation":{"x":0,"y":0,"z":0},"placementFace":"back","connectedTo":[],"grain":"vertical","edgeBanding":{"left":false,"right":false,"top":false,"bottom":false},"edgeRole":{},"hingeHoles":[]},
        {"id":"b-006","type":"door","displayName":"左门板","materialSlot":"door","boardId":null,"designLength":2150,"designWidth":400,"thickness":18,"position":{"x":-391,"y":1075,"z":10},"rotation":{"x":0,"y":0,"z":0},"placementFace":"front","connectedTo":["b-001"],"grain":"vertical","edgeBanding":{"left":true,"right":true,"top":true,"bottom":true},"hingeHoles":[{"edge":"left","count":3,"spacing":"even","diameter":35,"depth":12,"doorGap":2,"edgeDistance":22,"direction":"height","opening":"left"}]},
        {"id":"b-007","type":"door","displayName":"右门板","materialSlot":"door","boardId":null,"designLength":2150,"designWidth":400,"thickness":18,"position":{"x":391,"y":1075,"z":10},"rotation":{"x":0,"y":0,"z":0},"placementFace":"front","connectedTo":["b-002"],"grain":"vertical","edgeBanding":{"left":true,"right":true,"top":true,"bottom":true},"hingeHoles":[{"edge":"right","count":3,"spacing":"even","diameter":35,"depth":12,"doorGap":2,"edgeDistance":22,"direction":"height","opening":"right"}]}
      ]
    }',
    1,
    NULL
);

-- 官方预设：开门地柜（materialSlot 占位，boardId 为空，is_official=1）
INSERT INTO t_cabinet_template (name, category, cabinet_json, is_official, created_by)
VALUES (
    '开门地柜',
    'base-cabinet',
    '{
      "cabinet": {"name":"地柜","room":"","purpose":"","width":800,"height":800,"depth":500},
      "boards": [
        {"id":"b-001","type":"side","displayName":"左侧板","materialSlot":"cabinet_body","boardId":null,"designLength":800,"designWidth":500,"thickness":18,"position":{"x":-391,"y":400,"z":0},"rotation":{"x":0,"y":0,"z":0},"placementFace":"left","connectedTo":[],"grain":"vertical","edgeBanding":{"left":false,"right":false,"top":true,"bottom":true},"edgeRole":{"left":"靠墙侧","right":"前口","top":"上端","bottom":"下端"},"hingeHoles":[]},
        {"id":"b-002","type":"side","displayName":"右侧板","materialSlot":"cabinet_body","boardId":null,"designLength":800,"designWidth":500,"thickness":18,"position":{"x":391,"y":400,"z":0},"rotation":{"x":0,"y":0,"z":0},"placementFace":"right","connectedTo":[],"grain":"vertical","edgeBanding":{"left":false,"right":false,"top":true,"bottom":true},"edgeRole":{"left":"靠墙侧","right":"前口","top":"上端","bottom":"下端"},"hingeHoles":[]},
        {"id":"b-003","type":"top","displayName":"顶板","materialSlot":"cabinet_body","boardId":null,"designLength":800,"designWidth":500,"thickness":18,"position":{"x":0,"y":791,"z":0},"rotation":{"x":0,"y":0,"z":0},"placementFace":"top","connectedTo":["b-001","b-002"],"grain":"horizontal","edgeBanding":{"left":false,"right":false,"top":false,"bottom":true},"edgeRole":{"left":"靠墙侧","right":"前口","top":"上端","bottom":"下端"},"hingeHoles":[]},
        {"id":"b-004","type":"bottom","displayName":"底板","materialSlot":"cabinet_body","boardId":null,"designLength":800,"designWidth":500,"thickness":18,"position":{"x":0,"y":9,"z":0},"rotation":{"x":0,"y":0,"z":0},"placementFace":"bottom","connectedTo":["b-001","b-002"],"grain":"horizontal","edgeBanding":{"left":false,"right":false,"top":true,"bottom":false},"edgeRole":{"left":"靠墙侧","right":"前口","top":"上端","bottom":"下端"},"hingeHoles":[]},
        {"id":"b-005","type":"back","displayName":"背板","materialSlot":"back","boardId":null,"designLength":764,"designWidth":764,"thickness":5,"position":{"x":0,"y":400,"z":-247.5},"rotation":{"x":0,"y":0,"z":0},"placementFace":"back","connectedTo":[],"grain":"vertical","edgeBanding":{"left":false,"right":false,"top":false,"bottom":false},"edgeRole":{},"hingeHoles":[]},
        {"id":"b-006","type":"layer","displayName":"层板","materialSlot":"cabinet_body","boardId":null,"designLength":764,"designWidth":482,"thickness":18,"position":{"x":0,"y":400,"z":0},"rotation":{"x":0,"y":0,"z":0},"placementFace":"inner","connectedTo":["b-001","b-002"],"grain":"horizontal","edgeBanding":{"left":false,"right":false,"top":false,"bottom":false},"hingeHoles":[]},
        {"id":"b-007","type":"door","displayName":"左门板","materialSlot":"door","boardId":null,"designLength":750,"designWidth":400,"thickness":18,"position":{"x":-191,"y":375,"z":10},"rotation":{"x":0,"y":0,"z":0},"placementFace":"front","connectedTo":["b-001"],"grain":"vertical","edgeBanding":{"left":true,"right":true,"top":true,"bottom":true},"hingeHoles":[{"edge":"left","count":2,"spacing":"even","diameter":35,"depth":12,"doorGap":2,"edgeDistance":22,"direction":"height","opening":"left"}]},
        {"id":"b-008","type":"door","displayName":"右门板","materialSlot":"door","boardId":null,"designLength":750,"designWidth":400,"thickness":18,"position":{"x":191,"y":375,"z":10},"rotation":{"x":0,"y":0,"z":0},"placementFace":"front","connectedTo":["b-002"],"grain":"vertical","edgeBanding":{"left":true,"right":true,"top":true,"bottom":true},"hingeHoles":[{"edge":"right","count":2,"spacing":"even","diameter":35,"depth":12,"doorGap":2,"edgeDistance":22,"direction":"height","opening":"right"}]}
      ]
    }',
    1,
    NULL
);
