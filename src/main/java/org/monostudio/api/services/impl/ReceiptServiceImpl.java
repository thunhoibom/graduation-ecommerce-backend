package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.AddressPojo;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.api.models.ReceiptDetailPojo;
import org.monostudio.api.models.ReceiptPojo;
import org.monostudio.api.services.ReceiptService;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.OrderDetail;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.entities.ProductImage;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.ProductImagesRepository;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReceiptServiceImpl
    implements ReceiptService {
    private final OrdersRepository ordersRepository;
    private final ConversionService conversionService;
    private final ProductImagesRepository productImagesRepository;

    @Autowired
    public ReceiptServiceImpl(
        OrdersRepository ordersRepository,
        ConversionService conversionService,
        ProductImagesRepository productImagesRepository
    ) {
        this.ordersRepository = ordersRepository;
        this.conversionService = conversionService;
        this.productImagesRepository = productImagesRepository;
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
        if (target != null) {
            target.setSubtotal(foundMatch.getNetValue());
            target.setTotal(foundMatch.getTotalValue());
            target.setShippingFee(foundMatch.getTransportValue());
            target.setDiscountAmount(foundMatch.getDiscountValue());
            target.setPaymentType(foundMatch.getPaymentType().getName());
            target.setCreatedAt(foundMatch.getDate());

            if (foundMatch.getCustomer() != null && foundMatch.getCustomer().getPerson() != null) {
                Person p = foundMatch.getCustomer().getPerson();
                target.setCustomerName(p.getFirstName() + " " + p.getLastName());
                target.setCustomerEmail(p.getEmail());
            }

            if (foundMatch.getShippingAddress() != null) {
                AddressPojo addr = AddressPojo.builder()
                    .firstLine(foundMatch.getShippingAddress().getFirstLine())
                    .secondLine(foundMatch.getShippingAddress().getSecondLine())
                    .municipality(foundMatch.getShippingAddress().getMunicipality())
                    .city(foundMatch.getShippingAddress().getCity())
                    .postalCode(foundMatch.getShippingAddress().getPostalCode())
                    .build();
                target.setShippingAddress(addr);
            }

            List<ReceiptDetailPojo> targetItems = new ArrayList<>();
            for (OrderDetail d : foundMatch.getDetails()) {
                ReceiptDetailPojo item = conversionService.convert(d, ReceiptDetailPojo.class);
                if (item != null) {
                    Product pd = d.getProduct();
                    item.setProductName(pd.getName());
                    if (d.getProductVariant() != null) {
                        item.setVariantSku(d.getProductVariant().getSku());
                    }
                    item.setQuantity(d.getUnits());
                    item.setUnitPrice(d.getUnitValue());
                    item.setLineTotal(d.getUnits() * d.getUnitValue());
                    
                    if (d.getProductVariant() != null && d.getProductVariant().getImages() != null && !d.getProductVariant().getImages().isEmpty()) {
                        String imgUrl = d.getProductVariant().getImages().stream()
                            .filter(vi -> vi.getIsPrimary() != null && vi.getIsPrimary())
                            .findFirst()
                            .map(vi -> vi.getImage().getUrl())
                            .orElse(d.getProductVariant().getImages().get(0).getImage().getUrl());
                        item.setImageUrl(imgUrl);
                    } else {
                        // Fallback to Product images
                        List<ProductImage> pImages = productImagesRepository.deepFindProductImagesByProductIdOrdered(pd.getId());
                        if (pImages != null && !pImages.isEmpty()) {
                            String imgUrl = pImages.stream()
                                .filter(pi -> pi.getIsPrimary() != null && pi.getIsPrimary())
                                .findFirst()
                                .map(pi -> pi.getImage().getUrl())
                                .orElse(pImages.get(0).getImage().getUrl());
                            item.setImageUrl(imgUrl);
                        }
                    }

                    ProductPojo targetDetailProduct = ProductPojo.builder().name(pd.getName()).barcode(pd.getBarcode()).build();
                    item.setProduct(targetDetailProduct);
                    targetItems.add(item);
                }
            }
            target.setItems(targetItems);
            target.setStatus(foundMatch.getFulfillmentStatus());
        }

        return target;
    }
}
