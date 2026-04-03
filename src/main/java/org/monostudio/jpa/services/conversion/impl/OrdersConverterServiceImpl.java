package org.monostudio.jpa.services.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.AddressPojo;
import org.monostudio.api.models.BillingCompanyPojo;
import org.monostudio.api.models.OrderDetailPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Address;
import org.monostudio.jpa.entities.BillingCompany;
import org.monostudio.jpa.entities.BillingType;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.OrderDetail;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.repositories.AddressesRepository;
import org.monostudio.jpa.repositories.BillingTypesRepository;
import org.monostudio.jpa.repositories.PaymentTypesRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.repositories.OrderStatusesRepository;
import org.monostudio.jpa.repositories.ShippingMethodsRepository;
import org.monostudio.jpa.services.conversion.AddressesConverterService;
import org.monostudio.jpa.services.conversion.BillingCompaniesConverterService;
import org.monostudio.jpa.services.conversion.CustomersConverterService;
import org.monostudio.jpa.services.conversion.ProductsConverterService;
import org.monostudio.jpa.services.conversion.OrdersConverterService;
import org.monostudio.jpa.services.conversion.SalespeopleConverterService;
import org.monostudio.jpa.services.crud.BillingCompaniesCrudService;
import org.monostudio.jpa.services.crud.CustomersCrudService;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.monostudio.config.Constants.BILLING_TYPE_ENTERPRISE;
import static org.monostudio.config.Constants.ORDER_STATUS_PENDING;

@Transactional
@Service
public class OrdersConverterServiceImpl
    implements OrdersConverterService {
    private final CustomersCrudService customersCrudService;
    private final CustomersConverterService customersConverterService;
    private final BillingTypesRepository billingTypesRepository;
    private final BillingCompaniesCrudService billingCompaniesCrudService;
    private final BillingCompaniesConverterService billingCompaniesConverterService;
    private final SalespeopleConverterService salespeopleConverterService;
    private final ProductsConverterService productConverterService;
    private final ProductsRepository productsRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final ShippingMethodsRepository shippingMethodsRepository;
    private final AddressesRepository addressesRepository;
    private final PaymentTypesRepository paymentTypesRepository;
    private final OrderStatusesRepository orderStatusesRepository;
    private final AddressesConverterService addressesConverterService;
    static final double TAX_PERCENT = 0.19; // TODO refactor into a "tax service" of sorts
    static final String UNEXISTING_BILLING_TYPE = "Specified billing type does not exist";

    @Autowired
    public OrdersConverterServiceImpl(
        CustomersCrudService customersCrudService,
        CustomersConverterService customersConverterService,
        BillingTypesRepository billingTypesRepository,
        BillingCompaniesCrudService billingCompaniesCrudService,
        BillingCompaniesConverterService billingCompaniesConverterService,
        SalespeopleConverterService salespeopleConverterService,
        ProductsConverterService productConverterService,
        ProductsRepository productsRepository,
        ProductVariantsRepository productVariantsRepository,
        ShippingMethodsRepository shippingMethodsRepository,
        AddressesRepository addressesRepository,
        PaymentTypesRepository paymentTypesRepository,
        OrderStatusesRepository orderStatusesRepository,
        AddressesConverterService addressesConverterService
    ) {
        this.customersCrudService = customersCrudService;
        this.customersConverterService = customersConverterService;
        this.billingTypesRepository = billingTypesRepository;
        this.billingCompaniesCrudService = billingCompaniesCrudService;
        this.billingCompaniesConverterService = billingCompaniesConverterService;
        this.salespeopleConverterService = salespeopleConverterService;
        this.productConverterService = productConverterService;
        this.productsRepository = productsRepository;
        this.productVariantsRepository = productVariantsRepository;
        this.shippingMethodsRepository = shippingMethodsRepository;
        this.addressesRepository = addressesRepository;
        this.paymentTypesRepository = paymentTypesRepository;
        this.orderStatusesRepository = orderStatusesRepository;
        this.addressesConverterService = addressesConverterService;
    }

    @Override
    public OrderPojo convertToPojo(Order source) {
        OrderPojo target = OrderPojo.builder()
            .buyOrder(source.getId())
            .date(source.getDate())
            .netValue(source.getNetValue())
            .taxValue(source.getTaxesValue())
            .totalValue(source.getTotalValue())
            .totalItems(source.getTotalItems())
            .transportValue(source.getTransportValue())
            .token(source.getTransactionToken())
            .build();

        PersonPojo customer = customersConverterService.convertToPojo(source.getCustomer());
        target.setCustomer(customer);

        target.setStatus(source.getStatus().getName());
        target.setPaymentType(source.getPaymentType().getName());
        target.setBillingType(source.getBillingType().getName());

        if (target.getBillingType().equals(BILLING_TYPE_ENTERPRISE)) {
            BillingCompany sourceBillingCompany = source.getBillingCompany();
            BillingCompanyPojo targetBillingCompany = billingCompaniesConverterService.convertToPojo(sourceBillingCompany);
            target.setBillingCompany(targetBillingCompany);
        }

        if (source.getShippingMethod()!=null) {
            target.setShipper(source.getShippingMethod().getName());
        }

        if (source.getSalesperson()!=null) {
            PersonPojo salesperson = salespeopleConverterService.convertToPojo(source.getSalesperson());
            target.setSalesperson(salesperson);
        }
        return target;
    }

    /**
     * Converts a brand-new sell input object into a fully-fledged entity equivalent.<br/>
     * It validates that every piece of information on it is valid and cohesive.
     * For that, it makes several database queries in the process.
     */
    @Override
    public Order convertToNewEntity(OrderPojo model) throws BadInputException {
        Order target = Order.builder().build();
        // databases usually can take care of null dates. besides, the field is annotated with @CreationTimeStamp
        if (model.getDate()!=null) {
            target.setDate(model.getDate());
        }
        orderStatusesRepository.findByName(ORDER_STATUS_PENDING)
            .ifPresent(target::setStatus);
        this.convertPaymentTypeInformationForEntity(model, target);
        this.convertCustomerInformationForEntity(model, target);
        this.convertBillingInformationForEntity(model, target);
        this.convertShippingInformationForEntity(model, target);
        this.convertDetailsForEntity(model, target);
        this.updateTotals(target);
        return target;
    }

    @Override
    public OrderDetailPojo convertDetailToPojo(OrderDetail source) {
        Product sourceProduct = source.getProduct();
        ProductPojo product = productConverterService.convertToPojo(sourceProduct);
        return OrderDetailPojo.builder()
            .unitValue(source.getUnitValue())
            .units(source.getUnits())
            .product(product)
            .description(source.getDescription())
            .build();
    }

    /**
     * Converts a detail of the sell into a new entity counterpart.<br/>
     * If variantId is set, looks up the ProductVariant to resolve product and unit value.
     * Otherwise falls back to barcode lookup on Product (legacy path).
     */
    @Override
    public OrderDetail convertDetailToNewEntity(OrderDetailPojo detail) throws RuntimeException {
        try {
            Product product;
            ProductVariant variant = null;
            int unitValue;

            if (detail.getVariantId() != null) {
                // Variant path — resolve variant and product from variantId
                variant = productVariantsRepository.findById(detail.getVariantId())
                    .orElseThrow(() -> new BadInputException("Variant not found: " + detail.getVariantId()));
                product = variant.getProduct();
                // Use provided unit value if set; otherwise compute from product + variant modifier
                unitValue = detail.getUnitValue() > 0
                    ? detail.getUnitValue()
                    : product.getPrice() + variant.getPriceModifier();
            } else {
                // Legacy path — resolve by barcode
                String barcode = detail.getProduct() != null ? detail.getProduct().getBarcode() : null;
                if (StringUtils.isBlank(barcode)) {
                    throw new BadInputException("Product barcode must be valid when no variantId is provided");
                }
                Optional<Product> productOpt = productsRepository.findByBarcode(barcode);
                if (productOpt.isEmpty()) {
                    throw new BadInputException("Unexisting product in sell details");
                }
                product = productOpt.get();
                unitValue = detail.getUnitValue() > 0 ? detail.getUnitValue() : product.getPrice();
            }

            String description = detail.getUnits() + "x " + product.getName();
            return OrderDetail.builder()
                .units(detail.getUnits())
                .product(product)
                .productVariant(variant)
                .unitValue(unitValue)
                .description(description)
                .build();
        } catch (BadInputException exc) {
            throw new RuntimeException(exc.getMessage(), exc);
        }
    }

    @Override
    public Order applyChangesToExistingEntity(OrderPojo source, Order target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }

    private void convertPaymentTypeInformationForEntity(OrderPojo model, Order target) throws BadInputException {
        String paymentType = model.getPaymentType();
        if (!StringUtils.isBlank(paymentType)) {
            paymentTypesRepository.findByName(paymentType)
                .ifPresentOrElse(target::setPaymentType,
                    () -> {
                        throw new RuntimeException("The payment type does not exist");
                    });
        } else {
            throw new BadInputException("A payment type has to be included");
        }
    }

    /**
     * Tries to find a matching customer from the info placed in the order.<br/>
     * If it can't find any match, it will convert the data into a new customer.
     * Either way, the sell will have the customer set.
     */
    private void convertCustomerInformationForEntity(OrderPojo model, Order target) throws BadInputException {
        PersonPojo pojoCustomer = model.getCustomer();
        Optional<Customer> existingCustomer = customersCrudService.getExisting(pojoCustomer);
        if (existingCustomer.isEmpty()) {
            Customer customer = customersConverterService.convertToNewEntity(pojoCustomer);
            target.setCustomer(customer);
        } else {
            target.setCustomer(existingCustomer.get());
        }
    }

    /**
     * Tries to deduce billing information.<br/>
     * If the customer requested an invoice for a business company, it will try to match said company
     * against the existing records, or create a new record for that company.<br/>
     * The same process goes for the billing address.
     */
    private void convertBillingInformationForEntity(OrderPojo model, Order target) throws BadInputException {
        String pojoBillingTypeName = model.getBillingType();
        Optional<BillingType> existingBillingType = billingTypesRepository.findByName(pojoBillingTypeName);
        if (existingBillingType.isEmpty()) {
            throw new BadInputException(UNEXISTING_BILLING_TYPE);
        }
        target.setBillingType(existingBillingType.get());

        if (pojoBillingTypeName.equals(BILLING_TYPE_ENTERPRISE)) {
            BillingCompanyPojo pojoBillingCompany = model.getBillingCompany();
            Optional<BillingCompany> existingCompany = billingCompaniesCrudService.getExisting(pojoBillingCompany);
            if (existingCompany.isEmpty()) {
                BillingCompany billingCompany = billingCompaniesConverterService.convertToNewEntity(pojoBillingCompany);
                target.setBillingCompany(billingCompany);
            } else {
                target.setBillingCompany(existingCompany.get());
            }
            AddressPojo pojoBillingAddress = model.getBillingAddress();
            Optional<Address> existingBillingAddress = this.findAddress(pojoBillingAddress);
            if (existingBillingAddress.isEmpty()) {
                Address address = addressesConverterService.convertToNewEntity(pojoBillingAddress);
                target.setBillingAddress(address);
            } else {
                target.setBillingAddress(existingBillingAddress.get());
            }
        }
    }

    /**
     * Checks whether the order included shipping information, and if so, tries to deduce it.<br/>
     * Tries to find the requested shipping method, and the shipping address.
     * In the case of the address only, it may create a new record.
     */
    private void convertShippingInformationForEntity(OrderPojo model, Order target) throws BadInputException {
        String pojoShipperName = model.getShipper();
        AddressPojo pojoShippingAddress = model.getShippingAddress();
        if (!StringUtils.isBlank(pojoShipperName)) {
            Optional<ShippingMethod> existingShippingMethod = shippingMethodsRepository.findByName(pojoShipperName);
            if (existingShippingMethod.isEmpty()) {
                throw new BadInputException("Specified shipping method does not exist");
            }
            target.setShippingMethod(existingShippingMethod.get());
            Optional<Address> existingShippingAddress = this.findAddress(pojoShippingAddress);
            if (existingShippingAddress.isEmpty()) {
                Address address = addressesConverterService.convertToNewEntity(pojoShippingAddress);
                target.setShippingAddress(address);
            } else {
                target.setShippingAddress(existingShippingAddress.get());
            }
        }
    }

    /**
     * Straightly converts the details data.
     * Uses a stream function chain, see {@link #convertDetailToNewEntity}(SellDetailPojo d).
     */
    private void convertDetailsForEntity(OrderPojo model, Order target) {
        List<OrderDetail> detailEntities = model.getDetails().stream()
            .map(this::convertDetailToNewEntity)
            .collect(Collectors.toList());
        target.setDetails(detailEntities);
    }

    /**
     * Updates the net value, taxes, total units and total value of the sell.
     */
    private void updateTotals(Order entity) {
        int netValue = 0;
        int taxesValue = 0;
        int totalUnits = 0;
        for (OrderDetail sd : entity.getDetails()) {
            int unitValue = sd.getUnitValue();
            double unitTaxValue = unitValue * TAX_PERCENT;
            double unitNetValue = unitValue - unitTaxValue;
            taxesValue += (unitTaxValue * sd.getUnits());
            netValue += (unitNetValue * sd.getUnits());
            totalUnits += sd.getUnits();
        }
        entity.setTaxesValue(taxesValue);
        entity.setNetValue(netValue);
        entity.setTotalValue(taxesValue + netValue);
        entity.setTotalItems(totalUnits);
    }

    /**
     * Matches an address using all of its fields.
     */
    private Optional<Address> findAddress(@NotNull AddressPojo address) {
        return addressesRepository.findByFields(
            address.getCity(),
            address.getMunicipality(),
            address.getFirstLine(),
            address.getSecondLine(),
            address.getPostalCode(),
            address.getNotes()
        );
    }
}
