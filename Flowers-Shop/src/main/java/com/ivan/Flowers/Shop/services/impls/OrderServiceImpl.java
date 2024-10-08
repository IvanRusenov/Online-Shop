package com.ivan.Flowers.Shop.services.impls;

import com.ivan.Flowers.Shop.configs.OrderApiConfig;
import com.ivan.Flowers.Shop.enums.StatusType;
import com.ivan.Flowers.Shop.models.dtos.*;
import com.ivan.Flowers.Shop.models.entities.Bouquet;
import com.ivan.Flowers.Shop.models.entities.Cart;
import com.ivan.Flowers.Shop.models.entities.User;
import com.ivan.Flowers.Shop.models.user.ShopUserDetails;
import com.ivan.Flowers.Shop.repositories.BouquetRepository;
import com.ivan.Flowers.Shop.repositories.CartRepository;
import com.ivan.Flowers.Shop.repositories.UserRepository;
import com.ivan.Flowers.Shop.services.CartService;
import com.ivan.Flowers.Shop.services.OrderService;
import com.ivan.Flowers.Shop.services.exceptions.ObjectNotFoundException;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final RestClient orderRestClient;
    private final CartRepository cartRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final BouquetRepository bouquetRepository;
    private final CartService cartService;
    private OrderDetailsDTO currentOrder;
    private final OrderApiConfig orderApiConfig;

    public OrderServiceImpl(@Qualifier("ordersRestClient") RestClient orderRestClient,
                            CartRepository cartRepository,
                            ModelMapper modelMapper,
                            UserRepository userRepository,
                            BouquetRepository bouquetRepository,
                            CartService cartService, OrderApiConfig orderApiConfig) {
        this.orderRestClient = orderRestClient;
        this.cartRepository = cartRepository;
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
        this.bouquetRepository = bouquetRepository;
        this.cartService = cartService;
        this.orderApiConfig = orderApiConfig;
    }

    public OrderDetailsDTO getCurrentOrder() {
        return currentOrder;
    }

    public void setCurrentOrder(OrderDetailsDTO currentOrder) {
        this.currentOrder = currentOrder;
    }

    @Override
    public void createOrder(long cartId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ObjectNotFoundException("Cart not found", Cart.class.getSimpleName()));

        CreateOrderDTO createOrderDTO = modelMapper.map(cart, CreateOrderDTO.class);
        createOrderDTO.setUserId(cart.getOwner().getId());
        createOrderDTO.setShippingAddress(cart.getOwner().getShippingAddress());

        LOGGER.info("Creating new order");

        orderRestClient
                .post()
                .uri(orderApiConfig.getBaseUrl() + "/orders")//"http://localhost:8888/orders"
                .body(createOrderDTO)
                .retrieve();

        cartService.clearCart(cartId);

    }

    @Override
    public List<OrderDetailsDTO> getAllOrdersFromUser(UserDetails userDetails) {

        if (!(userDetails instanceof ShopUserDetails shopUserDetails)) {
            throw new RuntimeException("User is not authenticated.");
        }

        LOGGER.info("Get all user orders in descending order");

        User user = userRepository.findByUsername(shopUserDetails.getUsername())
                .orElseThrow(() -> new ObjectNotFoundException("User not found", User.class.getSimpleName()));

        List<OrderDTO> orderDTOS = orderRestClient
                .get()
                .uri("/orders/user/" + user.getId())
                .accept(MediaType.asMediaType(MediaType.APPLICATION_JSON))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return getOrderDetailsDTOS(orderDTOS);

    }

    private List<OrderDetailsDTO> getOrderDetailsDTOS(List<OrderDTO> orderDTOS) {

        if (orderDTOS == null) {
            throw new RuntimeException("Failed to retrieve orders");
        }

        return orderDTOS.stream().map(this::getOrderDetailsDTO).toList();
    }

    @Override
    public OrderDetailsDTO getLastOrderByUser(UserDetails userDetails) {

        LOGGER.info("Get last user order");
        return getAllOrdersFromUser(userDetails).getFirst();

    }

    @Override
    public void changOrderStatus(long orderId, StatusType newStatus) {

        orderRestClient
                .put()
                .uri("/orders/" + orderId + "/" + newStatus)
//                .accept(MediaType.asMediaType(MediaType.APPLICATION_JSON))
                .retrieve();

    }

    @Override
    public List<OrderDetailsDTO> getAllPendingOrders() {

        List<OrderDTO> orderDTOS = orderRestClient
                .get()
                .uri("/orders/pending")
                .accept(MediaType.asMediaType(MediaType.APPLICATION_JSON))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return getOrderDetailsDTOS(orderDTOS);

    }

    @Override
    public List<OrderDetailsDTO> getAllOrdersDesc() {

        List<OrderDTO> orderDTOS = orderRestClient
                .get()
                .uri( orderApiConfig.getBaseUrl() + "/orders")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return getOrderDetailsDTOS(orderDTOS);
    }

    @Override
    public void delete(long id) {
        orderRestClient
                .delete()
                .uri("/orders/" + id)
                .retrieve();

    }

    @Override
    public OrderDetailsDTO getOrder(long id) {

        OrderDTO orderDTO = orderRestClient
                .get()
                .uri("/orders/" + id)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return getOrderDetailsDTO(orderDTO);

    }

    private OrderDetailsDTO getOrderDetailsDTO(OrderDTO orderDTO) {
        OrderDetailsDTO orderDetailsDTO = modelMapper.map(orderDTO, OrderDetailsDTO.class);

        List<OrderItemDetailDTO> orderItemDetailDTOS = orderDTO.getItems().stream().map(orderItemDTO -> {

            OrderItemDetailDTO orderItemDetailDTO = modelMapper.map(orderItemDTO, OrderItemDetailDTO.class);
            Bouquet bouquet = bouquetRepository.findById(orderItemDTO.getBouquetId())
                    .orElseThrow(
                            () -> new ObjectNotFoundException("Bouquet not found", Bouquet.class.getSimpleName())
                    );

            orderItemDetailDTO.setItemNumber(bouquet.getItemNumber());
            orderItemDetailDTO.setDescription(bouquet.getDescription());
            orderItemDetailDTO.setUrl(bouquet.getUrl());

            return orderItemDetailDTO;

        }).toList();

        orderDetailsDTO.setItems(orderItemDetailDTOS);
        return orderDetailsDTO;
    }

    @Override
    public void edit(OrderDetailsDTO orderDetailsDTO) {

        if (currentOrder != null) {
            for (OrderItemDetailDTO item : currentOrder.getItems()) {
                for (OrderItemDetailDTO dtoItem : orderDetailsDTO.getItems()) {
                    if (item.getId() == dtoItem.getId()) {
                        item.setQuantity(dtoItem.getQuantity());
                        break;
                    }
                }
            }

            currentOrder.setStatus(orderDetailsDTO.getStatus());
            currentOrder.setShippingAddress(orderDetailsDTO.getShippingAddress());
        }

        currentOrder = orderDetailsDTO;

        OrderDTO orderDTO = modelMapper.map(currentOrder, OrderDTO.class);

        orderRestClient
                .put()
                .uri("/orders/update")
                .body(orderDTO)
                .retrieve();

        currentOrder = null;

    }

    @Override
    public void deleteItem(Long itemId, long orderId) {

        if (currentOrder == null) {
            currentOrder = getOrder(orderId);
        }

        List<OrderItemDetailDTO> newItems = new ArrayList<>();
        for (OrderItemDetailDTO item : currentOrder.getItems()) {
            if (item.getId() != itemId) {
                newItems.add(item);

            }
        }
        currentOrder.setItems(newItems);

        //       orderRestClient
//                .delete()
//                .uri("/order-items/" + itemId)
//                .retrieve();

    }

    @Override
    public void cancelOrder() {
        currentOrder = null;
    }

    @Override
    public void save(OrderDetailsDTO orderDetailsDTO) {

        OrderDTO orderDTO = modelMapper.map(orderDetailsDTO, OrderDTO.class);

        orderRestClient
                .put()
                .uri("/orders/update")
                .body(orderDTO)
                .retrieve();

        currentOrder = null;

    }

    @Override
    public OrderItemDetailDTO getOrderItem(long itemId) {
        OrderItemDTO orderItemDTO = orderRestClient
                .get()
                .uri("/order-items/" + itemId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return modelMapper.map(orderItemDTO, OrderItemDetailDTO.class);

    }

}
