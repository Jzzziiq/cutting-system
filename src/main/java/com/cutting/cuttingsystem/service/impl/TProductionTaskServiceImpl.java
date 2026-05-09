package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TOrder;
import com.cutting.cuttingsystem.entitys.TProductionTask;
import com.cutting.cuttingsystem.entitys.TaskStatus;
import com.cutting.cuttingsystem.entitys.VO.TProductionTaskVO;
import com.cutting.cuttingsystem.mapper.TProductionTaskMapper;
import com.cutting.cuttingsystem.service.TOrderService;
import com.cutting.cuttingsystem.service.TProductionTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TProductionTaskServiceImpl extends ServiceImpl<TProductionTaskMapper, TProductionTask>
        implements TProductionTaskService {

    @Lazy
    @Autowired
    private TOrderService orderService;

    @Override
    public TProductionTaskVO getTaskDetail(Long taskId) {
        TProductionTask task = getById(taskId);
        return task == null ? null : toVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductionTaskVO createTask(TProductionTask task) {
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.PENDING.getCode());
        }
        save(task);
        return getTaskDetail(task.getTaskId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductionTaskVO updateTask(Long taskId, TProductionTask update) {
        TProductionTask task = getById(taskId);
        if (task == null) return null;
        if (StringUtils.hasText(update.getTaskName())) task.setTaskName(update.getTaskName());
        if (update.getEstimatedHours() != null) task.setEstimatedHours(update.getEstimatedHours());
        if (update.getActualHours() != null) task.setActualHours(update.getActualHours());
        if (update.getAssigneeId() != null) task.setAssigneeId(update.getAssigneeId());
        if (update.getAssigneeName() != null) task.setAssigneeName(update.getAssigneeName());
        if (update.getRemark() != null) task.setRemark(update.getRemark());
        updateById(task);
        return getTaskDetail(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductionTaskVO assignTask(Long taskId, Long assigneeId, String assigneeName) {
        TProductionTask task = getById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");
        task.setAssigneeId(assigneeId);
        task.setAssigneeName(assigneeName);
        updateById(task);
        return getTaskDetail(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductionTaskVO transitionStatus(Long taskId, int targetStatus, String remark) {
        TProductionTask task = getById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");

        int currentCode = task.getStatus() != null ? task.getStatus() : 0;
        TaskStatus current = TaskStatus.fromCode(currentCode);
        if (!current.canTransitionTo(targetStatus)) {
            throw new RuntimeException(
                    String.format("不允许从 [%s] 转换到 [%s]",
                            current.getLabel(), TaskStatus.fromCode(targetStatus).getLabel()));
        }

        task.setStatus(targetStatus);
        if (targetStatus == TaskStatus.IN_PROGRESS.getCode() && task.getStartTime() == null) {
            task.setStartTime(new Date());
        }
        if (targetStatus == TaskStatus.COMPLETED.getCode()) {
            task.setCompleteTime(new Date());
        }
        updateById(task);
        return getTaskDetail(taskId);
    }

    @Override
    public List<TProductionTaskVO> listByOrderId(Long orderId) {
        return list(new QueryWrapper<TProductionTask>()
                        .eq("order_id", orderId)
                        .orderByDesc("create_time"))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Map<Integer, List<TProductionTaskVO>> kanbanData() {
        List<TProductionTask> all = list(new QueryWrapper<TProductionTask>().orderByDesc("create_time"));
        return all.stream()
                .map(this::toVO)
                .collect(Collectors.groupingBy(
                        vo -> vo.getStatus() != null ? vo.getStatus() : 0,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private TProductionTaskVO toVO(TProductionTask task) {
        TProductionTaskVO vo = new TProductionTaskVO();
        BeanUtils.copyProperties(task, vo);
        if (task.getStatus() != null) {
            vo.setStatusLabel(TaskStatus.fromCode(task.getStatus()).getLabel());
        }
        if (task.getOrderId() != null) {
            TOrder order = orderService.getById(task.getOrderId());
            vo.setOrderNo(order != null ? order.getOrderNo() : null);
        }
        return vo;
    }
}
