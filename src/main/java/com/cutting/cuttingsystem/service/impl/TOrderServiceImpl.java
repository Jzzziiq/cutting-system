package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.DTO.TOrderDTO;
import com.cutting.cuttingsystem.entitys.DTO.TOrderItemDTO;
import com.cutting.cuttingsystem.entitys.OrderStatus;
import com.cutting.cuttingsystem.entitys.TCustomer;
import com.cutting.cuttingsystem.entitys.TOrder;
import com.cutting.cuttingsystem.entitys.TOrderItem;
import com.cutting.cuttingsystem.entitys.VO.TOrderItemVO;
import com.cutting.cuttingsystem.entitys.VO.TOrderVO;
import com.cutting.cuttingsystem.mapper.TOrderMapper;
import com.cutting.cuttingsystem.service.TCustomerService;
import com.cutting.cuttingsystem.service.TOrderItemService;
import com.cutting.cuttingsystem.service.TOrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TOrderServiceImpl extends ServiceImpl<TOrderMapper, TOrder> implements TOrderService {
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Autowired
    private TOrderItemService orderItemService;

    @Autowired
    private TCustomerService customerService;

    @Override
    @Transactional
    public TOrderVO createOrder(TOrderDTO orderDTO) {
        TOrder order = new TOrder();
        BeanUtils.copyProperties(orderDTO, order);
        if (!StringUtils.hasText(order.getOrderNo())) {
            order.setOrderNo("ORD" + LocalDateTime.now().format(ORDER_NO_FORMATTER));
        }
        if (order.getOrderStatus() == null) {
            order.setOrderStatus(0);
        }
        fillCustomerSnapshot(order);
        save(order);
        saveItems(order.getOrderId(), orderDTO.getItems());
        return getOrderDetail(order.getOrderId());
    }

    @Override
    @Transactional
    public boolean updateOrder(Long orderId, TOrderDTO orderDTO) {
        TOrder order = new TOrder();
        BeanUtils.copyProperties(orderDTO, order);
        order.setOrderId(orderId);
        fillCustomerSnapshot(order);
        boolean updated = updateById(order);
        if (updated && orderDTO.getItems() != null) {
            orderItemService.remove(new QueryWrapper<TOrderItem>().eq("order_id", orderId));
            saveItems(orderId, orderDTO.getItems());
        }
        return updated;
    }

    @Override
    public TOrderVO getOrderDetail(Long orderId) {
        TOrder order = getById(orderId);
        if (order == null) {
            return null;
        }
        TOrderVO orderVO = toOrderVO(order);
        List<TOrderItemVO> itemVOList = orderItemService
                .list(new QueryWrapper<TOrderItem>().eq("order_id", orderId).orderByAsc("item_id"))
                .stream()
                .map(this::toItemVO)
                .toList();
        orderVO.setItems(itemVOList);
        return orderVO;
    }

    @Override
    @Transactional
    public TOrderVO transitionStatus(Long orderId, int targetStatus, String remark) {
        TOrder order = getById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");

        int currentCode = order.getOrderStatus() != null ? order.getOrderStatus() : 0;
        OrderStatus current = OrderStatus.fromCode(currentCode);
        if (!current.canTransitionTo(targetStatus)) {
            throw new RuntimeException(
                String.format("不允许从 [%s] 转换到 [%s]", current.getLabel(), OrderStatus.fromCode(targetStatus).getLabel()));
        }

        List<Map<String, Object>> history = parseHistory(order.getStatusHistory());
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("from", currentCode);
        entry.put("to", targetStatus);
        entry.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (remark != null && !remark.isBlank()) entry.put("remark", remark);
        history.add(entry);

        order.setOrderStatus(targetStatus);
        order.setStatusHistory(toJson(history));
        if (targetStatus == 5) { // 已完工
            order.setFinishTime(java.util.Date.from(java.time.Instant.now()));
        }
        updateById(order);
        return getOrderDetail(orderId);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseHistory(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, List.class);
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) { return "[]"; }
    }

    private void fillCustomerSnapshot(TOrder order) {
        if (order.getCustomerId() == null) {
            return;
        }
        if (StringUtils.hasText(order.getCustomerName()) && StringUtils.hasText(order.getCustomerAddress())) {
            return;
        }
        TCustomer customer = customerService.getById(order.getCustomerId());
        if (customer == null) {
            return;
        }
        if (!StringUtils.hasText(order.getCustomerName())) {
            order.setCustomerName(customer.getCustomerName());
        }
        if (!StringUtils.hasText(order.getCustomerAddress())) {
            order.setCustomerAddress(customer.getAddress());
        }
    }

    private void saveItems(Long orderId, List<TOrderItemDTO> itemDTOList) {
        if (CollectionUtils.isEmpty(itemDTOList)) {
            return;
        }
        List<TOrderItem> items = itemDTOList.stream()
                .map(itemDTO -> toItem(orderId, itemDTO))
                .toList();
        orderItemService.saveBatch(items);
    }

    private TOrderItem toItem(Long orderId, TOrderItemDTO itemDTO) {
        TOrderItem item = new TOrderItem();
        BeanUtils.copyProperties(itemDTO, item);
        item.setItemId(null);
        item.setOrderId(orderId);
        return item;
    }

    private TOrderVO toOrderVO(TOrder order) {
        TOrderVO orderVO = new TOrderVO();
        BeanUtils.copyProperties(order, orderVO);
        if (order.getOrderStatus() != null) {
            orderVO.setStatusLabel(OrderStatus.fromCode(order.getOrderStatus()).getLabel());
        }
        return orderVO;
    }

    private TOrderItemVO toItemVO(TOrderItem item) {
        TOrderItemVO itemVO = new TOrderItemVO();
        BeanUtils.copyProperties(item, itemVO);
        return itemVO;
    }
}
