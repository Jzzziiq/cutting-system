# 算法模块

## 入口与核心文件

| 项 | 路径 |
| --- | --- |
| 异步控制器（主入口） | `src/main/java/com/cutting/cuttingsystem/controller/AlgorithmController.java` |
| 旧版同步控制器 | `src/main/java/com/cutting/cuttingsystem/controller/TestController.java` |
| 算法服务 | `src/main/java/com/cutting/cuttingsystem/service/AlgorithmService.java`、`service/impl/AlgorithmServiceImpl.java` |
| 算法接口 | `src/main/java/com/cutting/cuttingsystem/model/CuttingAlgorithm.java` |
| 算法注册工厂 | `src/main/java/com/cutting/cuttingsystem/model/AlgorithmRegistry.java` |
| 禁忌搜索实现 | `src/main/java/com/cutting/cuttingsystem/model/TabuSearch.java` |
| 遗传算法实现 | `src/main/java/com/cutting/cuttingsystem/model/GeneticAlgorithm.java` |
| 禁忌表树结构 | `src/main/java/com/cutting/cuttingsystem/model/TabuMapTree.java` |
| 输入 DTO | `src/main/java/com/cutting/cuttingsystem/entitys/algorithm/DTO/InstanceDTO.java` |
| 输出 DTO | `src/main/java/com/cutting/cuttingsystem/entitys/algorithm/DTO/SolutionResponseDTO.java` |
| 输入解析与多容器求解 | `src/main/java/com/cutting/cuttingsystem/util/ReadDataUtil.java` |
| 算法任务实体 | `src/main/java/com/cutting/cuttingsystem/entitys/AlgorithmTask.java` |
| 算法测试 | `src/test/java/com/cutting/cuttingsystem/model/AlgorithmUnitTest.java` |

## 输入核心字段

- `L`：容器长度。
- `W`：容器宽度。
- `rotateEnable`：是否允许旋转。
- `gapDistance`：板件间距。
- `squareList`：待排样矩形列表，矩形字段包含 `id`、`l`、`w`。

## 算法架构

两种求解模式并存：

1. **异步模式（主入口）**：`POST /algorithm/submit` → `AlgorithmService.submit()` → 创建 `AlgorithmTask` → 异步执行 → `GET /algorithm/result/{taskId}` 轮询结果。支持多算法对比：`POST /algorithm/compare`。
2. **同步模式（旧版）**：`POST /algorithm/answer` → `TestController` → `ReadDataUtil.getSolution()` 直接返回 `List<SolutionResponseDTO>`。

`CuttingAlgorithm` 接口定义 `name()`、`displayName()`、`search()` 方法。`AlgorithmRegistry`（Spring `@Component`）按名称字符串创建算法实例。当前注册：`tabu_search` → `TabuSearch`，`genetic_algorithm` → `GeneticAlgorithm`。

`GeneticAlgorithm` 内部使用 `TabuSearch` 作为适应度评估器。

## 修改注意事项

- `TabuSearch.evaluate(...)` 负责天际线放置评估。
- `TabuSearch.search()` 负责禁忌搜索迭代寻优。
- `ReadDataUtil.getSolution(...)` 会按多容器循环求解，直到剩余矩形清空或判定无法装入。
- 新增算法实现需：实现 `CuttingAlgorithm` 接口，在 `AlgorithmRegistry` 注册。
- 修改放置、旋转、间距或利用率计算时，必须覆盖”可放入、旋转放入、间距导致不可放入、空列表、多容器”场景。
- 推荐验证命令：`mvn “-Dmaven.repo.local=target\.m2” -Dtest=AlgorithmUnitTest test`。
