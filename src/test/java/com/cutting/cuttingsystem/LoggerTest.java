package com.cutting.cuttingsystem;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 日志记录功能测试类
 * 展示如何使用 SLF4J + Logback 进行日志记录
 */
@Slf4j
@SpringBootTest
public class LoggerTest {

    @Test
    public void testLogLevel() {
        // TRACE 级别 - 最详细的调试信息
        log.trace("这是一条 TRACE 级别的日志 - 最详细的追踪信息");
        
        // DEBUG 级别 - 调试信息
        log.debug("这是一条 DEBUG 级别的日志 - 调试开发时使用");
        
        // INFO 级别 - 一般信息
        log.info("这是一条 INFO 级别的日志 - 记录一般信息");
        
        // WARN 级别 - 警告信息
        log.warn("这是一条 WARN 级别的日志 - 潜在的问题");
        
        // ERROR 级别 - 错误信息
        log.error("这是一条 ERROR 级别的日志 - 记录错误信息");
    }

    @Test
    public void testVariableLogging() {
        String username = "admin";
        Integer age = 25;
        Double score = 95.5;
        
        // 使用占位符记录变量
        log.info("用户信息 - 姓名：{}, 年龄：{}, 分数：{}", username, age, score);
        
        // 记录对象信息
        log.debug("调试信息：当前处理的用户为 {}", username);
    }

    @Test
    public void testExceptionLogging() {
        try {
            // 模拟异常
            int result = 10 / 0;
        } catch (Exception e) {
            // 记录异常堆栈
            log.error("发生算术异常：除以零错误", e);
            
            // 记录异常消息
            log.error("异常消息：{}", e.getMessage());
        }
    }

    @Test
    public void testConditionalLogging() {
        // 判断日志级别是否启用
        if (log.isDebugEnabled()) {
            log.debug("DEBUG 级别已启用，执行详细的调试逻辑");
        }
        
        if (log.isInfoEnabled()) {
            log.info("INFO 级别已启用，记录业务流程信息");
        }
        
        // 实际业务逻辑
        log.info("开始执行业务逻辑...");
        // 模拟业务处理
        log.info("业务逻辑执行完成");
    }

    @Test
    public void testPerformanceLogging() {
        long startTime = System.currentTimeMillis();
        
        // 模拟业务处理
        log.info("开始处理性能敏感操作");
        try {
            Thread.sleep(100); // 模拟耗时操作
        } catch (InterruptedException e) {
            log.error("线程被中断", e);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        log.info("性能敏感操作完成，耗时：{} ms", duration);
        
        // 使用 DEBUG 级别记录性能详情
        log.debug("性能详情 - 开始时间：{}, 结束时间：{}, 总耗时：{} ms", 
                startTime, endTime, duration);
    }

    @Test
    public void testBusinessLogicLogging() {
        log.info("========== 开始业务流程 ==========");
        
        // 步骤 1
        log.debug("步骤 1: 数据验证");
        log.info("数据验证通过");
        
        // 步骤 2
        log.debug("步骤 2: 数据处理");
        log.info("数据处理完成");
        
        // 步骤 3
        log.debug("步骤 3: 数据保存");
        log.info("数据保存成功");
        
        log.info("========== 业务流程结束 ==========");
    }

    @Test
    public void testLoggerName() {
        // 使用当前类的 Logger
        log.info("使用 SLF4J 的默认 Logger");
        
        // 可以在实际项目中为不同功能创建不同的 Logger
        // 例如：SQL 日志、业务日志、安全日志等
    }
}
