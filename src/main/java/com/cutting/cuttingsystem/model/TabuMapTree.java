package com.cutting.cuttingsystem.model;


import com.cutting.cuttingsystem.entitys.algorithm.Square;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 禁忌映射树数据结构
 * 用于存储和管理物品方块的排列组合，支持快速查找和添加操作
 * 通过树形结构记录已访问的物品序列，避免重复搜索
 *
 * @author Packing Algorithm
 * @version 1.0
 */
@Data
public class TabuMapTree {

    /**
     * 子树映射表
     * 键为 Square 的 ID，值为对应的子树节点
     */
    Map<String, TabuMapTree> sonTreeMap = new HashMap<>();

    /**
     * 当前节点的方块对象
     */
    Square nodeSquare;

    /**
     * 向树中添加物品序列
     * 从指定索引位置开始，将物品列表递归添加到树结构中
     * 如果当前节点为空，则先设置当前节点，然后继续添加后续物品
     * 如果已存在对应 ID 的子节点，则递归添加到该子节点；否则创建新节点后添加
     *
     * @param squareList 待添加的物品列表
     * @param index      当前处理的起始索引位置
     */
    public void add(List<Square> squareList, int index) {
        if (index >= squareList.size()) {
            return;
        }
        if (nodeSquare == null) {
            nodeSquare = squareList.get(index);
            index++;
        }
        Square square = squareList.get(index);
        String id = square.getId();
        if (sonTreeMap.containsKey(id)) {
            sonTreeMap.get(id).add(squareList, index + 1);
        } else {
            TabuMapTree tabuMapTree = new TabuMapTree();
            tabuMapTree.setNodeSquare(square);
            sonTreeMap.put(id, tabuMapTree);
            sonTreeMap.get(id).add(squareList, index + 1);
        }
    }

    /**
     * 检查物品序列是否存在于树中
     * 从指定索引位置开始，递归检查物品列表是否完全匹配树中的某条路径
     *
     * @param squareList 待检查的物品列表
     * @param index      当前检查的起始索引位置
     * @return 如果序列存在于树中返回 true，否则返回 false
     */
    public boolean contains(List<Square> squareList, int index) {
        if (index >= squareList.size()) {
            return true;
        }
        Square square = squareList.get(index);
        String id = square.getId();
        if (sonTreeMap.containsKey(id)) {
            return sonTreeMap.get(id).contains(squareList, index + 1);
        } else {
            return false;
        }
    }

    /**
     * 递归展示树结构
     * 遍历并输出所有子节点的信息，用于调试和可视化树结构
     */
    public void show() {
        for (String key : sonTreeMap.keySet()) {
            String id = sonTreeMap.get(key).getNodeSquare().getId();
            sonTreeMap.get(key).show();
        }
    }

    /**
     * 判断两个物品方块是否相等
     * 通过比较两个 Square 对象的 ID 来判断它们是否为同一物品
     *
     * @param square1 第一个待比较的 Square 对象
     * @param square2 第二个待比较的 Square 对象
     * @return 如果两个 Square 的 ID 相同返回 true，否则返回 false
     */
    public boolean isEq(Square square1, Square square2) {
        return square1.getId().equals(square2.getId());
    }

}
