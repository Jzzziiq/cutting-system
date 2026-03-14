package com.cutting.cuttingsystem.model;


import com.cutting.cuttingsystem.entitys.algorithm.*;
import lombok.Data;

import java.util.*;

@Data
public class TabuSearch {
    public final int MAX_GEN = 200;//最大的迭代次数(提高这个值可以稳定地提高解质量，但是会增加求解时间)
    public final int N = 100;//每次搜索领域的个数(这个值不要太大，太大的话搜索效率会降低)
    public int sqNum;//矩形数量，手动设置
    public HashMap<String, TabuMapTree> tabuTreeMap = new HashMap<>();
    public List<Square> initGhh;//初始顺序
    public List<Square> bestGh;//最佳顺序
    public List<Square> LocalGh;//当前最好顺序
    public List<Square> tempGh;//存放临时顺序
    public int bestT;//最佳的迭代次数
    public Solution bestSolution;//最优解
    public Solution LocalSolution;//每次领域搜索的最优解（领域最优解）
    public Solution tempSolution;//临时解
    public int t;//当前迭代
    public Random random;//随机函数对象
    // 问题实例
    public Instance instance;
    double L, W;

    public TabuSearch(Instance instance) throws Exception {
        this.instance = instance;
        this.initGhh = new ArrayList<>(instance.getSquareList());
        // 初始化变量
        random = new Random(System.currentTimeMillis());
        L = instance.getL();
        W = instance.getW();
        sqNum = initGhh.size();
    }

    public Solution search() throws Exception {
        long start = System.currentTimeMillis();
        // 获取初始解
        getInitSolution();
        System.out.println(bestSolution.getRate());
        //开始迭代，停止条件为达到指定迭代次数
        while (t <= MAX_GEN) {
            //当前领域搜索次数
            int n = 0;
            LocalSolution = new Solution();
            LocalSolution.setRate(0);
            while (n <= N) {
                // 随机打乱顺序 得到当前编码Ghh的邻居编码tempGh
                tempGh = generateNewGh(new ArrayList<>(initGhh), new ArrayList<>(tempGh));
                // 判断其是否在禁忌表中
                if (!judge(tempGh)) {
                    // 如果不在
                    //加入禁忌表
                    enterTabooList(tempGh);
                    tempSolution = evaluate(new ArrayList<>(tempGh));
                    if (tempSolution.getRate() > LocalSolution.getRate()) {
                        // 如果临时解优于本次领域搜索的最优解
                        // 那么就将临时解替换本次领域搜索的最优解
                        LocalGh = new ArrayList<>(tempGh);
                        LocalSolution = tempSolution;
                    }
                } else {
//                    throw new Exception("重复");
                }
                n++;
            }
            if (LocalSolution.getRate() > bestSolution.getRate()) {
                //如果本次搜索的最优解优于全局最优解
                //那么领域最优解替换全局最优解
                bestT = t;
                bestGh = new ArrayList<>(LocalGh);
                bestSolution = LocalSolution;
//                bestSolution = evaluate(bestGh);
            }
            initGhh = new ArrayList<>(LocalGh);
            t++;
//            System.out.println("当前迭代次数为：" + t + ",当前最佳利用率为：" + bestSolution.getRate());
        }
        //求解完毕
        System.out.println("最佳迭代次数:" + bestT);
        System.out.println("最佳利用率为:" + bestSolution.getRate());
        System.out.println("用时：" + (System.currentTimeMillis() - start) + "ms");
        return bestSolution;
    }

    //评价函数
    public Solution evaluate(List<Square> squareList) {
        Solution solution = new Solution();
        solution.setInstance(instance);
        solution.setSquareList(new ArrayList<>(squareList));
        List<PlaceSquare> placeSquareList = new ArrayList<>();
        // 创建初始可放置角点
        List<PlacePoint> placePointList = new ArrayList<>();
        double gap = instance.getGapDistance();
        placePointList.add(new PlacePoint(gap, gap, L - gap));

        // 开始按照顺序和规则放置
        for (int i = 0; i < placePointList.size(); ) {
            PlacePoint placePoint = placePointList.get(i);
            double maxMark = -1.0d;
            int maxIndex = -1;
            double isRotate = -1, curMarks = -1;
            for (int j = 0; j < squareList.size(); j++) {
                Square square = squareList.get(j);
                double[] arr = getMarks(placePoint, square, placeSquareList);
                double is_rotate = arr[0];
                curMarks = arr[1];
                if (curMarks > 0 && curMarks > maxMark) {
                    maxMark = curMarks;
                    maxIndex = j;
                    isRotate = is_rotate;
                }
            }
            if (maxIndex < 0 && i < placePointList.size()) {
                i++;
            } else if (maxIndex < 0 && i >= placePointList.size()) {
                break;
            } else {
                Square square = squareList.remove(maxIndex);
                double l = square.getL();
                double w = square.getW();
                if (isRotate > 0) {
                    // 表示进行了旋转
                    square.setL(w);
                    square.setW(l);
                }
                // 移除当前角点
                placePointList.remove(i);
                //新增已放置的 square
                placeSquareList.add(new PlaceSquare(placePoint.getX(), placePoint.getY(), square.getL(), square.getW()));
                // 新增两个可行角点
                double surplus = placePoint.getLen() - square.getL() - gap; // 剩余长度
                if (surplus > 0) {
                    placePointList.add(new PlacePoint(placePoint.getX() + square.getL() + gap, placePoint.getY(), surplus));
                }
                placePointList.add(new PlacePoint(placePoint.getX(), placePoint.getY() + square.getW() + gap, square.getL()));
                // 重新排序
                Collections.sort(placePointList);
//                System.out.println(placePointList);
                i = 0;
                // 还原矩形
                if (isRotate > 0) {
                    // 表示进行了旋转
                    square.setL(l);
                    square.setW(w);
                }
            }
        }
        // 设置已经放置的矩形列表
        solution.setPlaceSquareList(new ArrayList<>(placeSquareList));
        // 计算利用率
        double rate = 0.0f;
        double s = 0.0f;
        for (PlaceSquare placeSquare : placeSquareList) {
            s += (placeSquare.getL() * placeSquare.getW());
        }
        rate = s / (L * W);
        solution.setRate(rate);
        return solution;
    }

    // 评价该点的得分
    private double[] getMarks(PlacePoint placePoint, Square square, List<PlaceSquare> placeSquareList) {
        // 返回{是否旋转，分数}
        double delta = 0, mark1 = -1d, mark2 = -1d;
        PlaceSquare placeSquare = new PlaceSquare(placePoint.getX(), placePoint.getY(), square.getL(), square.getW());
        if (isOverlap(placeSquareList, placeSquare)) {
            mark1 = -1.0d;
        } else {
            delta = Math.abs(placePoint.getLen() - square.getL());
            mark1 = 1 - delta / placePoint.getLen();
        }
        mark2 = -1.0d;
        if (instance.isRotateEnable()) {
            placeSquare = new PlaceSquare(placePoint.getX(), placePoint.getY(), square.getW(), square.getL());
            if (!isOverlap(placeSquareList, placeSquare)) {
                delta = Math.abs(placePoint.getLen() - square.getW());
                mark2 = 1 - delta / placePoint.getLen();
            }
        }
        if (mark1 >= mark2) {
            return new double[]{-1d, (int) (mark1 * 10)};
        }
        return new double[]{1d, (int) (mark2 * 10)};
    }

    // 判断放置在该位置是否超出边界或者和其他矩形重叠
    public boolean isOverlap(List<PlaceSquare> placeSquareList, PlaceSquare tempPlaceSquare) {
        // 出界
        double gap = instance.getGapDistance();
        if (tempPlaceSquare.getL() > L || tempPlaceSquare.getW() > W) {
            return true;
        }
        // 出界（考虑间隔）
        if (tempPlaceSquare.getX() + tempPlaceSquare.getL() + gap > L || tempPlaceSquare.getY() + tempPlaceSquare.getW() + gap > W) {
            return true;
        }
        for (PlaceSquare placeSquare : placeSquareList) {
            // 角点重合
            if (placeSquare.getX() == tempPlaceSquare.getX() && placeSquare.getY() == tempPlaceSquare.getY()) {
                placeSquareList.remove(placeSquare);
                return true;
            }
            // 判断即将要放置的块是否与之前放置的块有重叠（考虑间隔）
            if (isOverlap2WithGap(placeSquare, tempPlaceSquare, gap)) {
                return true;
            }
        }
        return false;
    }

    // 判断即将要放置的块是否与之前放置的块有重叠
    public boolean isOverlap2(PlaceSquare placeSquare, PlaceSquare tempPlaceSquare) {

        double x1 = Math.max(placeSquare.getX(), tempPlaceSquare.getX());
        double y1 = Math.max(placeSquare.getY(), tempPlaceSquare.getY());
        double x2 = Math.min(placeSquare.getX() + placeSquare.getL(), tempPlaceSquare.getX() + tempPlaceSquare.getL());
        double y2 = Math.min(placeSquare.getY() + placeSquare.getW(), tempPlaceSquare.getY() + tempPlaceSquare.getW());

        if (x1 >= x2 || y1 >= y2) {
            return false;
        }

        return true;

    }

    // 判断即将要放置的块是否与之前放置的块有重叠（考虑间隔距离）
    public boolean isOverlap2WithGap(PlaceSquare placeSquare, PlaceSquare tempPlaceSquare, double gap) {

        double x1 = Math.max(placeSquare.getX(), tempPlaceSquare.getX());
        double y1 = Math.max(placeSquare.getY(), tempPlaceSquare.getY());
        double x2 = Math.min(placeSquare.getX() + placeSquare.getL(), tempPlaceSquare.getX() + tempPlaceSquare.getL());
        double y2 = Math.min(placeSquare.getY() + placeSquare.getW(), tempPlaceSquare.getY() + tempPlaceSquare.getW());

        // 如果有重叠区域，直接返回 true
        if (x1 < x2 && y1 < y2) {
            return true;
        }

        // 检查 X 方向的间隔
        double xGap = 0;
        if (tempPlaceSquare.getX() >= placeSquare.getX() + placeSquare.getL()) {
            xGap = tempPlaceSquare.getX() - (placeSquare.getX() + placeSquare.getL());
        } else if (placeSquare.getX() >= tempPlaceSquare.getX() + tempPlaceSquare.getL()) {
            xGap = placeSquare.getX() - (tempPlaceSquare.getX() + tempPlaceSquare.getL());
        }

        // 检查 Y 方向的间隔
        double yGap = 0;
        if (tempPlaceSquare.getY() >= placeSquare.getY() + placeSquare.getW()) {
            yGap = tempPlaceSquare.getY() - (placeSquare.getY() + placeSquare.getW());
        } else if (placeSquare.getY() >= tempPlaceSquare.getY() + tempPlaceSquare.getW()) {
            yGap = placeSquare.getY() - (tempPlaceSquare.getY() + tempPlaceSquare.getW());
        }

        // 如果任一方向的间隔小于要求的间隔距离，则认为重叠
        if (xGap < gap && yGap < gap) {
            return true;
        }

        return false;
    }

    // 生成初始解
    public void getInitSolution() throws Exception {
        Collections.shuffle(initGhh);
        bestSolution = evaluate(new ArrayList<>(initGhh));
        tempSolution = bestSolution;
        bestGh = new ArrayList<>(initGhh);
        tempGh = new ArrayList<>(initGhh);
        LocalGh = new ArrayList<>(initGhh);
    }

    //加入禁忌队列
    public void enterTabooList(List<Square> squareList) {
        if (tabuTreeMap == null) {
            tabuTreeMap = new HashMap<>();
        }
        Square square = squareList.get(0);
        String id = square.getId();
        if (tabuTreeMap.containsKey(id)) {
            tabuTreeMap.get(id).add(new ArrayList<>(squareList), 1);
        } else {
            TabuMapTree tabuMapTree = new TabuMapTree();
            tabuMapTree.setNodeSquare(square);
            tabuMapTree.add(new ArrayList<>(squareList), 1);
            tabuTreeMap.put(id, tabuMapTree);
        }

    }

    //生成新解
//    public List<Square> generateNewGh(List<Square> localGh,List<Square> tempGh) {
//        tempGh = new ArrayList<>(localGh);
//        Collections.shuffle(tempGh);
//        return tempGh;
//    }
    public List<Square> generateNewGh(List<Square> localGh, List<Square> tempGh) {
        // 边界条件：如果列表长度<=1，无法交换，直接返回原列表
        if (localGh.size() <= 1) {
            return new ArrayList<>(localGh);
        }
        Square temp;
        //将Gh复制到tempGh
        tempGh = new ArrayList<>(localGh);

        for (int i = 0; i < 6; i++) {
            int r1 = 0;
            int r2 = 0;

            while (r1 == r2) {
                r1 = random.nextInt(tempGh.size());
                r2 = random.nextInt(tempGh.size());
            }
            //交换
            temp = tempGh.get(r1);
            tempGh.set(r1, tempGh.get(r2));
            tempGh.set(r2, temp);
        }

        return new ArrayList<>(tempGh);
    }

    //判断路径编码是否存在于禁忌表中
    public boolean judge(List<Square> Gh) {
        Square square = Gh.get(0);
        if (tabuTreeMap.containsKey(square.getId())) {
            return tabuTreeMap.get(square.getId()).contains(Gh, 1);
        } else {
            return false;
        }
    }

    // 判断两个Squre是否相等
    public boolean isEq(Square square1, Square square2) {
        return square1.getId().equals(square2.getId());
    }
}
