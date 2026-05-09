package com.cutting.cuttingsystem.aop;

import com.cutting.cuttingsystem.annotation.AuditLog;
import com.cutting.cuttingsystem.entitys.TAuditLog;
import com.cutting.cuttingsystem.util.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Aspect
@Component
@Slf4j
public class AuditLogAspect {

    @Autowired
    private AuditLogWriter auditLogWriter;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        TAuditLog logEntry = new TAuditLog();
        logEntry.setCreateTime(new Date());
        logEntry.setModule(auditLog.module());
        logEntry.setAction(auditLog.action());
        logEntry.setTargetClass(joinPoint.getSignature().getDeclaringTypeName());
        logEntry.setTargetMethod(joinPoint.getSignature().getName());

        Long userId = UserContext.getCurrentUserId();
        if (userId != null) {
            logEntry.setUserId(userId);
        }
        logEntry.setUsername("uid:" + (userId != null ? userId : "?"));

        try {
            logEntry.setIpAddress(getClientIp());
        } catch (Exception e) {
            logEntry.setIpAddress("unknown");
        }

        logEntry.setRequestParams(toJsonString(joinPoint.getArgs()));

        long start = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
            logEntry.setStatus(0);
        } catch (Throwable e) {
            logEntry.setStatus(1);
            String errMsg = e.getMessage();
            if (errMsg != null && errMsg.length() > 500) {
                errMsg = errMsg.substring(0, 500);
            }
            logEntry.setErrorMsg(errMsg);
            throw e;
        } finally {
            logEntry.setDurationMs(System.currentTimeMillis() - start);
            auditLogWriter.asyncSave(logEntry);
        }

        return result;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String toJsonString(Object[] args) {
        if (args == null || args.length == 0) return null;
        List<Object> filtered = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                continue;
            }
            filtered.add(arg);
        }
        if (filtered.isEmpty()) return null;
        try {
            String json = MAPPER.writeValueAsString(filtered.size() == 1 ? filtered.get(0) : filtered);
            if (json != null && json.length() > 4000) {
                json = json.substring(0, 4000);
            }
            return json;
        } catch (JsonProcessingException e) {
            return "[serialization error]";
        }
    }
}
