package com.ivan.flower_shop.orders.services.impl;

import com.ivan.flower_shop.orders.enums.StatusType;
import com.ivan.flower_shop.orders.models.dtos.OrderDTO;
import com.ivan.flower_shop.orders.models.dtos.OrderItemDTO;
import com.ivan.flower_shop.orders.models.entities.Order;
import com.ivan.flower_shop.orders.models.entities.OrderItem;
import com.ivan.flower_shop.orders.repositories.OrderRepository;
import com.ivan.flower_shop.orders.services.OrderService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void createOrder(OrderDTO orderDTO) {

        Order order = mopToOrder(orderDTO);
        saveOrder(order);

    }

    @Override
    public List<OrderDTO> getAllOrders() {

        List<Order> all = orderRepository.findAll();

        return all.stream().map(this::mapToOrderDTO).toList();

    }

    @Override
    public OrderDTO getOrderById(Long orderId) {

        Order order = orderRepository.findById(orderId).orElse(null);

        return mapToOrderDTO(order);

    }

    @Override
    public void saveOrder(Order order) {
        orderRepository.save(order);
    }


    @Override
    public void updateOrder(Long orderId, OrderDTO orderDTO) {

        Order order = orderRepository.findById(orderId).orElseThrow();

        order.setUserId(orderDTO.getUserId());
        order.setOrderDateTime(orderDTO.getOrderDateTime());
        order.setTotalAmount(orderDTO.getTotalAmount());
        order.setStatus(orderDTO.getStatus());
        order.setShippingAddress(orderDTO.getShippingAddress());

        mapToOrderItems(orderDTO, order);

        saveOrder(order);

    }

    @Override
    public void deleteOrder(Long orderId) {

        Order order = orderRepository.findById(orderId).orElseThrow();
        orderRepository.delete(order);//exception NoSuchElementException
//        orderRepository.deleteById(orderId);//no return value, no exception always 200 OK
    }

    private static void mapToOrderItems(OrderDTO orderDTO, Order order) {
        List<OrderItem> orderItems = orderDTO.getItems().stream().map(itemDTO -> {

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(itemDTO.getProductId());
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setUnitPrice(itemDTO.getUnitPrice());
            orderItem.setTotalPrice(itemDTO.getQuantity() * itemDTO.getUnitPrice());
            orderItem.setOrder(order);

            return orderItem;

        }).toList();

        order.setItems(orderItems);

        double totalAmount = orderItems.stream().mapToDouble(OrderItem::getTotalPrice).sum();

        order.setTotalAmount(totalAmount);
    }

    private static Order mopToOrder(OrderDTO orderDTO) {
        Order order = new Order();
        order.setUserId(orderDTO.getUserId());
        order.setOrderDateTime(LocalDateTime.now());
        order.setStatus(StatusType.PENDING);
        order.setShippingAddress(orderDTO.getShippingAddress());

        mapToOrderItems(orderDTO, order);
        return order;
    }

    private OrderDTO mapToOrderDTO(Order order) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setUserId(order.getUserId());
        orderDTO.setOrderDateTime(order.getOrderDateTime());
        orderDTO.setTotalAmount(order.getTotalAmount());
        orderDTO.setStatus(order.getStatus());
        orderDTO.setShippingAddress(order.getShippingAddress());

        List<OrderItemDTO> orderItemDTOS = order.getItems().stream().map(orderItem -> {

            OrderItemDTO orderItemDTO = new OrderItemDTO();
            orderItemDTO.setProductId(orderItem.getProductId());
            orderItemDTO.setQuantity(orderItem.getQuantity());
            orderItemDTO.setUnitPrice(orderItem.getUnitPrice());

            return orderItemDTO;

        }).toList();

        orderDTO.setItems(orderItemDTOS);

        return orderDTO;
    }

}