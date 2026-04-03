package org.monostudio.jpa.services.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ReturnRequestItemPojo;
import org.monostudio.api.models.ReturnRequestPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.entities.ReturnRequestItem;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.services.conversion.ReturnRequestItemsConverterService;
import org.monostudio.jpa.services.conversion.ReturnRequestsConverterService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
public class ReturnRequestsConverterServiceImpl
    implements ReturnRequestsConverterService {
    private final OrdersRepository ordersRepository;
    private final ReturnRequestItemsConverterService itemsConverterService;

    @Autowired
    public ReturnRequestsConverterServiceImpl(
        OrdersRepository ordersRepository,
        ReturnRequestItemsConverterService itemsConverterService
    ) {
        this.ordersRepository = ordersRepository;
        this.itemsConverterService = itemsConverterService;
    }

    @Override
    public ReturnRequestPojo convertToPojo(@NotNull ReturnRequest source) {
        ReturnRequestPojo.ReturnRequestPojoBuilder builder = ReturnRequestPojo.builder()
            .reason(source.getReason())
            .adminNotes(source.getAdminNotes())
            .status(source.getStatus().name())
            .refundMethod(source.getRefundMethod().name())
            .refundAmount(source.getRefundAmount())
            .trackingNumber(source.getTrackingNumber());

        builder.id(source.getId());
        if (source.getDate() != null) {
            builder.date(source.getDate());
        }
        if (source.getLastModified() != null) {
            builder.lastModified(source.getLastModified());
        }
        if (source.getOrder() != null) {
            builder.orderId(source.getOrder().getId());
        }

        return builder.build();
    }

    @Override
    public ReturnRequest convertToNewEntity(ReturnRequestPojo source) throws BadInputException {
        ReturnRequest.ReturnRequestBuilder builder = ReturnRequest.builder()
            .reason(source.getReason())
            .adminNotes(source.getAdminNotes())
            .status(ReturnRequest.ReturnRequestStatus.valueOf(source.getStatus()))
            .refundMethod(ReturnRequest.RefundMethod.valueOf(source.getRefundMethod()))
            .refundAmount(source.getRefundAmount())
            .trackingNumber(source.getTrackingNumber());

        if (source.getOrderId() != null) {
            Order order = ordersRepository.findById(source.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + source.getOrderId()));
            builder.order(order);
        }

        return builder.build();
    }

    @Override
    public ReturnRequest applyChangesToExistingEntity(ReturnRequestPojo source, ReturnRequest target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }

    @Override
    public ReturnRequestItemPojo convertItemToPojo(@NotNull ReturnRequestItem source) {
        return itemsConverterService.convertToPojo(source);
    }

    @Override
    public Collection<ReturnRequestItem> convertItemsToEntities(Collection<ReturnRequestItemPojo> items) {
        if (items == null) {
            return new ArrayList<>();
        }
        return items.stream()
            .map(itemsConverterService::convertToNewEntity)
            .collect(Collectors.toList());
    }
}
