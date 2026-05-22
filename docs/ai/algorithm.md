# 算法模块

## 入口与核心文件

| 项 | 路径 |
| --- | --- |
| 入口控制器 | `src/main/java/com/cutting/cuttingsystem/controller/TestController.java` |
| 输入 DTO | `src/main/java/com/cutting/cuttingsystem/entitys/algorithm/DTO/InstanceDTO.java` |
| 输出 DTO | `src/main/java/com/cutting/cuttingsystem/entitys/algorithm/DTO/SolutionResponseDTO.java` |
| 核心实现 | `src/main/java/com/cutting/cuttingsystem/model/TabuSearch.java` |
| 输入解析与多容器求解 | `src/main/java/com/cutting/cuttingsystem/util/ReadDataUtil.java` |
| 算法测试 | `src/test/java/com/cutting/cuttingsystem/model/AlgorithmUnitTest.java` |

## 输入核心字段

- `L`：容器长度。
- `W`：容器宽度。
- `rotateEnable`：是否允许旋转。
- `gapDistance`：板件间距。
- `squareList`：待排样矩形列表，矩形字段包含 `id`、`l`、`w`。

## 修改注意事项

- `TabuSearch.evaluate(...)` 负责天际线放置评估。
- `TabuSearch.search()` 负责禁忌搜索迭代寻优。
- `ReadDataUtil.getSolution(...)` 会按多容器循环求解，直到剩余矩形清空或判定无法装入。
- 修改放置、旋转、间距或利用率计算时，必须覆盖“可放入、旋转放入、间距导致不可放入、空列表、多容器”场景。
- 推荐验证命令：`mvn "-Dmaven.repo.local=target\.m2" -Dtest=AlgorithmUnitTest test`。
