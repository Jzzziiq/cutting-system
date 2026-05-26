package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TLayoutResult;
import com.cutting.cuttingsystem.entitys.TOrder;
import com.cutting.cuttingsystem.entitys.TOrderItem;
import com.cutting.cuttingsystem.entitys.TProductionTask;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.entitys.TaskStatus;
import com.cutting.cuttingsystem.entitys.VO.TLayoutResultVO;
import com.cutting.cuttingsystem.entitys.VO.TOrderItemVO;
import com.cutting.cuttingsystem.entitys.VO.TOrderVO;
import com.cutting.cuttingsystem.entitys.VO.TProductionTaskDetailVO;
import com.cutting.cuttingsystem.entitys.VO.TProductionTaskVO;
import com.cutting.cuttingsystem.mapper.TLayoutResultMapper;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.mapper.TBoardMapper;
import com.cutting.cuttingsystem.mapper.TOrderItemMapper;
import com.cutting.cuttingsystem.mapper.TOrderMapper;
import com.cutting.cuttingsystem.mapper.TProductionTaskMapper;
import com.cutting.cuttingsystem.service.TNotificationService;
import com.cutting.cuttingsystem.service.TProductionTaskService;
import com.cutting.cuttingsystem.service.TUserService;
import com.cutting.cuttingsystem.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private TUserService userService;

    @Autowired
    private TLayoutResultMapper layoutResultMapper;

    @Autowired
    private TOrderMapper orderMapper;

    @Autowired
    private TOrderItemMapper orderItemMapper;

    @Autowired
    private TBoardMapper boardMapper;

    @Autowired
    private TNotificationService notificationService;

    @Override
    public TProductionTaskVO getTaskDetail(Long taskId) {
        TProductionTask task = baseMapper.selectByIdIgnoreTenant(taskId);
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
        TProductionTask task = baseMapper.selectByIdIgnoreTenant(taskId);
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
        TProductionTask task = baseMapper.selectByIdIgnoreTenant(taskId);
        if (task == null) throw new RuntimeException("任务不存在");
        task.setAssigneeId(assigneeId);
        task.setAssigneeName(assigneeName);
        updateById(task);

        // 创建通知
        String taskName = task.getTaskName() != null ? task.getTaskName() : "任务";
        notificationService.createNotification(
            assigneeId,
            "任务分配变更",
            "任务已分配给您：" + taskName,
            task.getTaskId()
        );

        return getTaskDetail(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductionTaskVO assignOrderTask(Long orderId, Long assigneeId) {
        TOrder order = orderMapper.selectByIdIgnoreTenant(orderId);
        if (order == null) throw new RuntimeException("订单不存在");

        TUser assignee = userService.getById(assigneeId);
        if (assignee == null) throw new RuntimeException("员工不存在");

        TProductionTask task = baseMapper.selectLatestByOrderIdIgnoreTenant(orderId);
        if (task == null) {
            task = new TProductionTask();
            task.setUserId(order.getUserId());
            task.setOrderId(orderId);
            task.setLayoutResultId(order.getLayoutResultId());
            task.setTaskName(StringUtils.hasText(order.getProcessName()) ? order.getProcessName() : "生产任务");
            task.setStatus(TaskStatus.PENDING.getCode());
        }

        task.setAssigneeId(assigneeId);
        task.setAssigneeName(displayUserName(assignee));
        if (task.getTaskId() == null) {
            save(task);
        } else {
            baseMapper.updateAssignmentIgnoreTenant(task.getTaskId(), task.getAssigneeId(), task.getAssigneeName());
        }

        // 同步排版结果的生产任务状态
        syncLayoutResultTaskStatus(task.getLayoutResultId(), task.getStatus());

        // 创建通知
        String taskName = task.getTaskName() != null ? task.getTaskName() : "新任务";
        notificationService.createNotification(
            assigneeId,
            "新任务分配",
            "您有新的生产任务：" + taskName,
            task.getTaskId()
        );

        return toVO(baseMapper.selectByIdIgnoreTenant(task.getTaskId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductionTaskVO transitionStatus(Long taskId, int targetStatus, String remark) {
        TProductionTask task = baseMapper.selectByIdIgnoreTenant(taskId);
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

        // 同步排版结果的生产任务状态
        syncLayoutResultTaskStatus(task.getLayoutResultId(), task.getStatus());

        return getTaskDetail(taskId);
    }

    @Override
    public List<TProductionTaskVO> listByOrderId(Long orderId) {
        return baseMapper.selectByOrderIdIgnoreTenant(orderId)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<TProductionTaskVO> listMyTasks(Long assigneeId) {
        return baseMapper.selectByAssigneeIdIgnoreTenant(assigneeId)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public TProductionTaskDetailVO getMyTaskDetail(Long taskId, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        TProductionTask task = baseMapper.selectAssignedByIdIgnoreTenant(taskId, assigneeId);
        if (task == null) {
            return null;
        }

        TProductionTaskDetailVO detail = new TProductionTaskDetailVO();
        detail.setTask(toVO(task));
        if (task.getOrderId() != null) {
            detail.setOrder(getOrderDetailIgnoreTenant(task.getOrderId()));
        }
        if (task.getLayoutResultId() != null) {
            detail.setLayoutResult(toLayoutResultVO(layoutResultMapper.selectByIdIgnoreTenant(task.getLayoutResultId())));
        }
        return detail;
    }

    @Override
    public Map<Integer, List<TProductionTaskVO>> kanbanData() {
        Long orgId = UserContext.getCurrentOrgId();
        QueryWrapper<TProductionTask> qw = new QueryWrapper<>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        qw.orderByDesc("create_time");
        List<TProductionTask> all = baseMapper.selectList(qw);
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
            TOrder order = orderMapper.selectByIdIgnoreTenant(task.getOrderId());
            vo.setOrderNo(order != null ? order.getOrderNo() : null);
        }
        return vo;
    }

    private String displayUserName(TUser user) {
        if (StringUtils.hasText(user.getRealName())) return user.getRealName();
        return user.getUsername();
    }

    private TLayoutResultVO toLayoutResultVO(TLayoutResult layoutResult) {
        if (layoutResult == null) return null;
        TLayoutResultVO vo = new TLayoutResultVO();
        BeanUtils.copyProperties(layoutResult, vo);
        if (layoutResult.getTaskStatus() != null) {
            vo.setTaskStatusLabel(TaskStatus.fromCode(layoutResult.getTaskStatus()).getLabel());
        }
        if (layoutResult.getOrderId() != null) {
            TOrder order = orderMapper.selectByIdIgnoreTenant(layoutResult.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setOrderName(order.getOrderNo());
                vo.setCustomer(order.getCustomerName());
            }
        }
        return vo;
    }

    private TOrderVO getOrderDetailIgnoreTenant(Long orderId) {
        TOrder order = orderMapper.selectByIdIgnoreTenant(orderId);
        if (order == null) return null;

        TOrderVO vo = new TOrderVO();
        BeanUtils.copyProperties(order, vo);
        List<TOrderItemVO> items = orderItemMapper.selectByOrderIdIgnoreTenant(orderId)
                .stream()
                .map(this::toItemVO)
                .toList();
        vo.setItems(items);
        return vo;
    }

    private TOrderItemVO toItemVO(TOrderItem item) {
        TOrderItemVO vo = new TOrderItemVO();
        BeanUtils.copyProperties(item, vo);
        if (item.getBoardId() != null) {
            TBoard board = boardMapper.selectById(item.getBoardId());
            if (board != null) {
                vo.setBrand(board.getBrand());
            }
        }
        return vo;
    }

    @Override
    public int deleteByIdIgnoreTenant(Long taskId) {
        return baseMapper.deleteByIdIgnoreTenant(taskId);
    }

    @Override
    public void syncLayoutResultTaskStatus(Long layoutResultId, Integer taskStatus) {
        if (layoutResultId == null) return;
        TLayoutResult lr = layoutResultMapper.selectById(layoutResultId);
        if (lr != null) {
            lr.setTaskStatus(taskStatus);
            layoutResultMapper.updateById(lr);
        }
    }
}
