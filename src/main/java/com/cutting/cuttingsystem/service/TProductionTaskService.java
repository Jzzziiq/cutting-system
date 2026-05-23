package com.cutting.cuttingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cutting.cuttingsystem.entitys.TProductionTask;
import com.cutting.cuttingsystem.entitys.VO.TProductionTaskDetailVO;
import com.cutting.cuttingsystem.entitys.VO.TProductionTaskVO;

import java.util.List;
import java.util.Map;

public interface TProductionTaskService extends IService<TProductionTask> {

    TProductionTaskVO getTaskDetail(Long taskId);

    TProductionTaskVO createTask(TProductionTask task);

    TProductionTaskVO updateTask(Long taskId, TProductionTask task);

    TProductionTaskVO assignTask(Long taskId, Long assigneeId, String assigneeName);

    TProductionTaskVO assignOrderTask(Long orderId, Long assigneeId);

    TProductionTaskVO transitionStatus(Long taskId, int targetStatus, String remark);

    List<TProductionTaskVO> listByOrderId(Long orderId);

    List<TProductionTaskVO> listMyTasks(Long assigneeId);

    TProductionTaskDetailVO getMyTaskDetail(Long taskId, Long assigneeId);

    Map<Integer, List<TProductionTaskVO>> kanbanData();

    int deleteByIdIgnoreTenant(Long taskId);
}
