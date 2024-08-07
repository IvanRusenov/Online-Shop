package com.ivan.flower_shop.orders.services;

import com.ivan.flower_shop.orders.enums.StatusType;
import com.ivan.flower_shop.orders.models.dtos.OrderDTO;
import com.ivan.flower_shop.orders.models.entities.Order;

import java.util.List;

public interface OrderService {

    void saveOrder(Order order);

    void createOrder(OrderDTO orderDTO);

    List<OrderDTO> getAllOrders();

    List<OrderDTO> getAllOrdersFromUser(Long userId);

    OrderDTO getOrderById(Long orderId);

    void deleteOrder(Long orderId);

    void changeStatus(Long orderId, StatusType newStatus);

    List<OrderDTO> getAllPendingOrders();

    void updateOrder(OrderDTO orderDTO);
}
