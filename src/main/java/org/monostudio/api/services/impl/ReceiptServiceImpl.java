package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.api.models.ReceiptDetailPojo;
import org.monostudio.api.models.ReceiptPojo;
import org.monostudio.api.services.ReceiptService;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.OrderDetail;
import org.monostudio.jpa.repositories.OrdersRepository;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReceiptServiceImpl
    implements ReceiptService {
    private final OrdersRepository ordersRepository;
    private final ConversionService conversionService;

    @Autowired
    public ReceiptServiceImpl(
        OrdersRepository ordersRepository,
        ConversionService conversionService
    ) {
        this.ordersRepository = ordersRepository;
        this.conversionService = conversionService;
    }

    @Override
    public ReceiptPojo fetchReceiptByTransactionToken(String token)
        throws EntityNotFoundException {
        Optional<Order> match = ordersRepository.findByTransactionToken(token);
        if (match.isEmpty()) {
            throw new EntityNotFoundException("The transaction could not be found, no receipt can be created");
        }
        Order foundMatch = match.get();

        ReceiptPojo target = conversionService.convert(foundMatch, ReceiptPojo.class);

        if (target!=null) {
            List<ReceiptDetailPojo> targetDetails = new ArrayList<>();
            for (OrderDetail d : foundMatch.getDetails()) {
                ReceiptDetailPojo targetDetail = conversionService.convert(d, ReceiptDetailPojo.class);
                if (targetDetail!=null) {
                    Product pd = d.getProduct();
                    ProductPojo targetDetailProduct = ProductPojo.builder().name(pd.getName()).barcode(pd.getBarcode()).build();
                    targetDetail.setProduct(targetDetailProduct);
                    targetDetail.setUnitValue(d.getUnitValue());
                    targetDetails.add(targetDetail);
                }
            }
            target.setDetails(targetDetails);
            target.setStatus(foundMatch.getStatus().getName());
        }

        return target;
    }
}
