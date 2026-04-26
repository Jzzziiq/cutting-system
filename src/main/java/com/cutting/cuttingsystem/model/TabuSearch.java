package com.cutting.cuttingsystem.model;

import com.cutting.cuttingsystem.entitys.algorithm.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.Comparator;

@Data
@Slf4j
public class TabuSearch {
    public final int MAX_GEN = 300; // 最大迭代次数（可调）
    public final int N = 500;       // 每次领域搜索数（可调）
    public int sqNum;               // 矩形数量
    public HashMap<String, TabuMapTree> tabuTreeMap = new HashMap<>();
    public List<Square> initGhh;    // 初始顺序
    public List<Square> bestGh;     // 最佳顺序
    public List<Square> LocalGh;    // 当前最优顺序
    public List<Square> tempGh;     // 临时顺序
    public int bestT;               // 最佳迭代次数
    public Solution bestSolution;   // 全局最优解
    public Solution LocalSolution;  // 领域最优解
    public Solution tempSolution;   // 临时解
    public int t;                   // 当前迭代次数
    public Random random;           // 随机数对象
    public Instance instance;       // 问题实例
    double L, W;                    // 容器尺寸

    // 天际线线段内部类
    @Data
    public static class SkylineSegment {
        private double x;
        private double y;
        private double width;

        public SkylineSegment(double x, double y, double width) {
            this.x = x;
            this.y = y;
            this.width = width;
        }
    }

    public TabuSearch(Instance instance) throws Exception {
        this.instance = instance;
        this.initGhh = new ArrayList<>(instance.getSquareList());
        random = new Random(System.currentTimeMillis());
        L = instance.getL();
        W = instance.getW();
        sqNum = initGhh.size();
    }

    // 禁忌搜索主逻辑（迭代寻优）
    public Solution search() throws Exception {
        long start = System.currentTimeMillis();
        getInitSolution(); // 初始化初始解
        log.info("初始利用率：{}", bestSolution.getRate());

        while (t <= MAX_GEN) {
            int n = 0;
            LocalSolution = new Solution();
            LocalSolution.setRate(0);

            // 遍历领域，寻找当前最优解
            while (n <= N) {
                tempGh = generateNewGh(new ArrayList<>(initGhh), new ArrayList<>(tempGh));
                if (!judge(tempGh)) { // 不在禁忌表中
                    enterTabooList(tempGh);
                    tempSolution = evaluate(new ArrayList<>(tempGh));
                    if (tempSolution.getRate() > LocalSolution.getRate()) {
                        LocalGh = new ArrayList<>(tempGh);
                        LocalSolution = tempSolution;
                    }
                }
                n++;
            }

            // 更新全局最优解
            if (LocalSolution.getRate() > bestSolution.getRate()) {
                bestT = t;
                bestGh = new ArrayList<>(LocalGh);
                bestSolution = LocalSolution;
            }

            initGhh = new ArrayList<>(LocalGh);
            t++;
        }

        // 输出结果
        log.info("最佳迭代次数：{}", bestT);
        log.info("最佳利用率：{}", bestSolution.getRate());
        log.info("耗时：{}ms", System.currentTimeMillis() - start);
        return bestSolution;
    }

    // 天际线算法：评价函数（核心）
    public Solution evaluate(List<Square> squareList) {
        Solution solution = new Solution();
        solution.setInstance(instance);
        solution.setSquareList(new ArrayList<>(squareList));
        List<PlaceSquare> placeSquareList = new ArrayList<>();
        double gap = instance.getGapDistance();
        double containerL = instance.getL();
        double containerW = instance.getW();

        // 初始化天际线
        List<SkylineSegment> skyline = new ArrayList<>();
        skyline.add(new SkylineSegment(gap, gap, containerL - 2 * gap));

        // 遍历矩形放置
        for (Square square : squareList) {
            double rectL = square.getL();
            double rectW = square.getW();
            boolean rotated = false;
            double bestX = -1, bestY = -1;
            double minHeight = Double.MAX_VALUE;

            // 尝试不旋转/旋转
            List<double[]> rectOptions = new ArrayList<>();
            rectOptions.add(new double[]{rectL, rectW});
            if (instance.isRotateEnable()) {
                rectOptions.add(new double[]{rectW, rectL});
            }

            // 寻找最优放置位置
            for (double[] rectSize : rectOptions) {
                double w = rectSize[0];
                double h = rectSize[1];
                for (int i = 0; i < skyline.size(); i++) {
                    SkylineSegment seg = skyline.get(i);
                    if (seg.getWidth() >= w && seg.getX() + w + gap <= containerL && seg.getY() + h + gap <= containerW) {
                        double availableWidth = seg.getWidth();
                        int j = i;
                        double currentX = seg.getX();
                        while (availableWidth < w && j + 1 < skyline.size() && skyline.get(j + 1).getY() == seg.getY()) {
                            j++;
                            availableWidth += skyline.get(j).getWidth();
                        }
                        if (availableWidth >= w) {
                            double placeY = seg.getY();
                            if (placeY < minHeight || (placeY == minHeight && currentX < bestX)) {
                                minHeight = placeY;
                                bestX = currentX;
                                bestY = placeY;
                                rotated = (rectSize[0] == rectW && rectSize[1] == rectL);
                            }
                        }
                    }
                }
            }

            // 放置矩形并更新天际线
            if (bestX != -1 && bestY != -1) {
                double finalW = rotated ? square.getW() : square.getL();
                double finalH = rotated ? square.getL() : square.getW();
                placeSquareList.add(new PlaceSquare(bestX, bestY, finalW, finalH));
                updateSkyline(skyline, bestX, bestY, finalW, finalH, gap);
            }
        }

        // 计算利用率
        solution.setPlaceSquareList(new ArrayList<>(placeSquareList));
        double totalArea = containerL * containerW;
        double usedArea = placeSquareList.stream().mapToDouble(p -> p.getL() * p.getW()).sum();
        solution.setRate(usedArea / totalArea);
        return solution;
    }

    // 更新天际线
    private void updateSkyline(List<SkylineSegment> skyline, double x, double y, double w, double h, double gap) {
        List<SkylineSegment> newSkyline = new ArrayList<>();
        double rectRight = x + w + gap;
        double rectTop = y + h + gap;

        // 分割原有线段
        for (SkylineSegment seg : skyline) {
            if (seg.getX() + seg.getWidth() <= x || seg.getX() >= rectRight) {
                newSkyline.add(seg);
            } else {
                if (seg.getX() < x) {
                    newSkyline.add(new SkylineSegment(seg.getX(), seg.getY(), x - seg.getX()));
                }
                if (seg.getX() + seg.getWidth() > rectRight) {
                    newSkyline.add(new SkylineSegment(rectRight, seg.getY(), seg.getX() + seg.getWidth() - rectRight));
                }
            }
        }

        // 添加新线段
        newSkyline.add(new SkylineSegment(x, rectTop, w));
        mergeSkylineSegments(newSkyline);

        // 替换天际线
        skyline.clear();
        skyline.addAll(newSkyline);
    }

    // 合并同高度连续线段
    private void mergeSkylineSegments(List<SkylineSegment> skyline) {
        skyline.sort(Comparator.comparingDouble(SkylineSegment::getX));
        for (int i = 0; i < skyline.size() - 1; ) {
            SkylineSegment curr = skyline.get(i);
            SkylineSegment next = skyline.get(i + 1);
            if (curr.getY() == next.getY() && curr.getX() + curr.getWidth() == next.getX()) {
                curr.setWidth(curr.getWidth() + next.getWidth());
                skyline.remove(i + 1);
            } else {
                i++;
            }
        }
    }

    // 初始化初始解
    public void getInitSolution() throws Exception {
        Collections.shuffle(initGhh);
        bestSolution = evaluate(new ArrayList<>(initGhh));
        tempSolution = bestSolution;
        bestGh = new ArrayList<>(initGhh);
        tempGh = new ArrayList<>(initGhh);
        LocalGh = new ArrayList<>(initGhh);
    }

    // 加入禁忌表
    public void enterTabooList(List<Square> squareList) {
        if (tabuTreeMap == null) tabuTreeMap = new HashMap<>();
        Square square = squareList.get(0);
        String id = square.getId();
        if (tabuTreeMap.containsKey(id)) {
            tabuTreeMap.get(id).add(new ArrayList<>(squareList), 1);
        } else {
            TabuMapTree tree = new TabuMapTree();
            tree.setNodeSquare(square);
            tree.add(new ArrayList<>(squareList), 1);
            tabuTreeMap.put(id, tree);
        }
    }

    // 生成新解（随机交换6次）
    public List<Square> generateNewGh(List<Square> localGh, List<Square> tempGh) {
        if (localGh.size() <= 1) return new ArrayList<>(localGh);
        tempGh = new ArrayList<>(localGh);
        Square temp;
        for (int i = 0; i < 6; i++) {
            int r1 = random.nextInt(tempGh.size());
            int r2 = random.nextInt(tempGh.size());
            while (r1 == r2) r2 = random.nextInt(tempGh.size());
            temp = tempGh.get(r1);
            tempGh.set(r1, tempGh.get(r2));
            tempGh.set(r2, temp);
        }
        return new ArrayList<>(tempGh);
    }

    // 判断是否在禁忌表中
    public boolean judge(List<Square> Gh) {
        if (Gh.isEmpty()) return false;
        Square square = Gh.get(0);
        return tabuTreeMap.containsKey(square.getId()) && tabuTreeMap.get(square.getId()).contains(Gh, 1);
    }

    // 辅助方法（保留，兼容旧逻辑）
    public boolean isEq(Square square1, Square square2) {
        return square1.getId().equals(square2.getId());
    }
}
