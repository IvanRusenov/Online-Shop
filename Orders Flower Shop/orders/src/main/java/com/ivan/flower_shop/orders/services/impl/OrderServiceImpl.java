package com.ivan.flower_shop.orders.services.impl;

import com.ivan.flower_shop.orders.enums.StatusType;
import com.ivan.flower_shop.orders.models.dtos.OrderDTO;
import com.ivan.flower_shop.orders.models.dtos.OrderItemDTO;
import com.ivan.flower_shop.orders.models.entities.Order;
import com.ivan.flower_shop.orders.models.entities.OrderItem;
import com.ivan.flower_shop.orders.repositories.OrderItemRepository;
import com.ivan.flower_shop.orders.repositories.OrderRepository;
import com.ivan.flower_shop.orders.services.OrderService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final OrderItemRepository orderItemRepository;

    public OrderServiceImpl(OrderRepository orderRepository, ModelMapper modelMapper, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.modelMapper = modelMapper;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public void createOrder(OrderDTO orderDTO) {

        Order order = mopToOrder(orderDTO);
        saveOrder(order);

    }

    @Override
    public List<OrderDTO> getAllOrders() {

        List<Order> all = orderRepository.findAllByOrderByIdDesc();

        return all.stream()
                .map(order -> modelMapper.map(order, OrderDTO.class))
                .toList();

    }

    /**
     * @param userId
     * @return all orders made by user with given userId in descending order
     */
    @Override
    public List<OrderDTO> getAllOrdersFromUser(Long userId) {

        List<Order> allByUserId = orderRepository.findAllByUserIdOrderByIdDesc(userId);

        return allByUserId.stream()
                .map(order -> modelMapper.map(order, OrderDTO.class))
                .toList();

    }

    @Override
    public OrderDTO getOrderById(Long orderId) {

        Order order = orderRepository.findById(orderId).orElse(null);

        if (order == null) {
            return null;
        }

        return modelMapper.map(order, OrderDTO.class);

    }

    @Override
    public void saveOrder(Order order) {
        orderRepository.save(order);
    }


    @Override
    public void changeStatus(Long orderId, StatusType newStatus) {

        Order order = orderRepository.findById(orderId).orElseThrow();

        order.setStatus(newStatus);

        saveOrder(order);

    }

    @Override
    public List<OrderDTO> getAllPendingOrders() {

        List<Order> allByStatus = orderRepository.findAllByStatus(StatusType.PENDING);

        return allByStatus.stream()
                .map(order -> modelMapper.map(order, OrderDTO.class))
                .toList();

    }

    @Override
    public void updateOrder(OrderDTO orderDTO) {

        Order order = orderRepository.findById(orderDTO.getId()).orElseThrow();

        Order newData = modelMapper.map(orderDTO, Order.class);

        order.setStatus(newData.getStatus());
        order.setShippingAddress(newData.getShippingAddress());

        List<OrderItem> items = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            for (OrderItem mapItem : newData.getItems()) {

                if (item.getId() == mapItem.getId()) {
                    item.setQuantity(mapItem.getQuantity());
                    item.setTotalPrice(item.getQuantity() * item.getUnitPrice());
                    items.add(item);
                    break;
                }
            }
        }

        orderItemRepository.deleteAll(order.getItems());
        order.setItems(items);

        double totalAmount = order.getItems().stream().mapToDouble(OrderItem::getTotalPrice).sum();
        order.setTotalAmount(totalAmount);

        orderItemRepository.saveAll(order.getItems());
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
            orderItem.setBouquetId(itemDTO.getBouquetId());
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
            orderItemDTO.setBouquetId(orderItem.getBouquetId());
            orderItemDTO.setQuantity(orderItem.getQuantity());
            orderItemDTO.setUnitPrice(orderItem.getUnitPrice());

            return orderItemDTO;

        }).toList();

        orderDTO.setItems(orderItemDTOS);

        return orderDTO;
    }

}
