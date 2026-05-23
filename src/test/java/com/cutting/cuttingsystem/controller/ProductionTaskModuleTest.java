package com.cutting.cuttingsystem.controller;

import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.entitys.VO.TLayoutResultVO;
import com.cutting.cuttingsystem.entitys.VO.TOrderItemVO;
import com.cutting.cuttingsystem.entitys.VO.TOrderVO;
import com.cutting.cuttingsystem.entitys.VO.TProductionTaskDetailVO;
import com.cutting.cuttingsystem.entitys.VO.TProductionTaskVO;
import com.cutting.cuttingsystem.mapper.TPermissionMapper;
import com.cutting.cuttingsystem.service.TProductionTaskService;
import com.cutting.cuttingsystem.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductionTaskModuleTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TProductionTaskService productionTaskService;

    @MockitoBean
    private TPermissionMapper permissionMapper;

    @BeforeEach
    void setupPermissions() {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("admin")))
                .thenReturn(List.of("order:read", "order:write"));
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("viewer")))
                .thenReturn(List.of("order:read"));
    }

    @Test
    void assignByOrderReturnsUpdatedTask() throws Exception {
        when(productionTaskService.assignOrderTask(10L, 2L)).thenReturn(taskVO(100L, 10L, 2L));

        mockMvc.perform(put("/production-tasks/order/10/assign")
                        .header("Authorization", bearerToken(1L, "admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assigneeId": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value(100))
                .andExpect(jsonPath("$.data.assigneeId").value(2))
                .andExpect(jsonPath("$.data.assigneeName").value("worker-a"));
    }

    @Test
    void assignByOrderReturnsBusinessErrorWhenAssigneeIsInvalid() throws Exception {
        when(productionTaskService.assignOrderTask(10L, 999L)).thenThrow(new RuntimeException("员工不存在"));

        mockMvc.perform(put("/production-tasks/order/10/assign")
                        .header("Authorization", bearerToken(1L, "admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assigneeId": 999
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("员工不存在"));
    }

    @Test
    void myTasksReturnsOnlyCurrentUsersAssignments() throws Exception {
        when(productionTaskService.listMyTasks(2L)).thenReturn(List.of(taskVO(100L, 10L, 2L)));

        mockMvc.perform(get("/production-tasks/my")
                        .header("Authorization", bearerToken(2L, "worker", "viewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].taskId").value(100))
                .andExpect(jsonPath("$.data[0].assigneeId").value(2));
    }

    @Test
    void myTaskDetailReturnsDetailForOwnTask() throws Exception {
        when(productionTaskService.getMyTaskDetail(100L, 2L)).thenReturn(detailVO());

        mockMvc.perform(get("/production-tasks/my/100")
                        .header("Authorization", bearerToken(2L, "worker", "viewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.task.taskId").value(100))
                .andExpect(jsonPath("$.data.order.orderId").value(10))
                .andExpect(jsonPath("$.data.order.items[0].partName").value("left-door"))
                .andExpect(jsonPath("$.data.layoutResult.resultId").value(20));
    }

    @Test
    void myTaskDetailReturnsBusinessErrorForOtherUsersTask() throws Exception {
        when(productionTaskService.getMyTaskDetail(100L, 2L)).thenReturn(null);

        mockMvc.perform(get("/production-tasks/my/100")
                        .header("Authorization", bearerToken(2L, "worker", "viewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("任务不存在或未分配给当前用户"));
    }

    @Test
    void readonlyUserCanAccessMyTasksButCannotAccessKanban() throws Exception {
        mockMvc.perform(get("/production-tasks/my")
                        .header("Authorization", bearerToken(2L, "worker", "viewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/production-tasks/kanban")
                        .header("Authorization", bearerToken(2L, "worker", "viewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("权限不足"));
    }

    private TProductionTaskDetailVO detailVO() {
        TProductionTaskDetailVO detail = new TProductionTaskDetailVO();
        detail.setTask(taskVO(100L, 10L, 2L));

        TOrderVO order = new TOrderVO();
        order.setOrderId(10L);
        order.setOrderNo("ORD-10");
        order.setCustomerName("ACME");
        order.setItems(List.of(orderItemVO()));
        detail.setOrder(order);

        TLayoutResultVO layoutResult = new TLayoutResultVO();
        layoutResult.setResultId(20L);
        layoutResult.setOrderId(10L);
        layoutResult.setUsageRate(new BigDecimal("0.86"));
        layoutResult.setContainerCount(2);
        detail.setLayoutResult(layoutResult);
        return detail;
    }

    private TProductionTaskVO taskVO(Long taskId, Long orderId, Long assigneeId) {
        TProductionTaskVO vo = new TProductionTaskVO();
        vo.setTaskId(taskId);
        vo.setOrderId(orderId);
        vo.setOrderNo("ORD-10");
        vo.setTaskName("生产任务");
        vo.setAssigneeId(assigneeId);
        vo.setAssigneeName("worker-a");
        vo.setStatus(0);
        vo.setStatusLabel("待生产");
        return vo;
    }

    private TOrderItemVO orderItemVO() {
        TOrderItemVO item = new TOrderItemVO();
        item.setItemId(1L);
        item.setOrderId(10L);
        item.setPartName("left-door");
        item.setLength(500);
        item.setWidth(300);
        item.setThickness(18);
        item.setQuantity(2);
        return item;
    }

    private String bearerToken(Long userId, String username, String role) {
        TUser user = new TUser();
        user.setUserId(userId);
        user.setUsername(username);
        return "Bearer " + jwtUtil.generateToken(user, List.of(role));
    }
}
