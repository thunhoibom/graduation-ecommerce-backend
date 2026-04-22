package org.monostudio.jpa.services.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.monostudio.jpa.entities.*;
import org.monostudio.jpa.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.AddressPojo;
import org.monostudio.api.models.BillingCompanyPojo;
import org.monostudio.api.models.OrderDetailPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.services.conversion.AddressesConverterService;
import org.monostudio.jpa.services.conversion.BillingCompaniesConverterService;
import org.monostudio.jpa.services.conversion.CustomersConverterService;
import org.monostudio.jpa.services.conversion.ProductsConverterService;
import org.monostudio.jpa.services.conversion.OrdersConverterService;
import org.monostudio.jpa.services.conversion.ProductVariantsConverterService;
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
    private final ProductVariantsConverterService productVariantsConverterService;
    private final UsersRepository usersRepository;
    private final CustomersRepository customersRepository;
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
        AddressesConverterService addressesConverterService,
        ProductVariantsConverterService productVariantsConverterService,
        UsersRepository usersRepository,
        CustomersRepository customersRepository
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
        this.productVariantsConverterService = productVariantsConverterService;
        this.usersRepository = usersRepository;
        this.customersRepository = customersRepository;
    }

    @Override
    public OrderPojo convertToPojo(Order source) {
        OrderPojo target = OrderPojo.builder()
            .id(source.getId())
            .buyOrder(source.getId())
            .date(source.getDate())
            .netValue(source.getNetValue())
            .taxValue(source.getTaxesValue())
            .totalValue(source.getTotalValue())
            .totalItems(source.getTotalItems())
            .transportValue(source.getTransportValue())
            .token(source.getTransactionToken())
            .discountCode(source.getDiscountCode())
            .discountValue(source.getDiscountValue())
            .cartSessionToken(source.getCartSessionToken())
            .totalRefundedAmount(source.getTotalRefundedAmount())
            .build();

        PersonPojo customer = customersConverterService.convertToPojo(source.getCustomer());
        target.setCustomer(customer);

        target.setStatus(source.getStatus().getName());
        target.setPaymentType(source.getPaymentType().getName());
        target.setBillingType(source.getBillingType().getName());

        // Derive payment status
        String statusName = source.getStatus().getName();
        String paymentTypeName = source.getPaymentType().getName();
        boolean isCompleted = statusName.equals(org.monostudio.config.Constants.ORDER_STATUS_COMPLETED);
        boolean isPaidStatus = statusName.equals(org.monostudio.config.Constants.ORDER_STATUS_PAID_UNCONFIRMED)
            || statusName.equals(org.monostudio.config.Constants.ORDER_STATUS_PAID_CONFIRMED);
        boolean isOnlinePayment = !paymentTypeName.equalsIgnoreCase("COD");

        if (isCompleted || (isPaidStatus && isOnlinePayment)) {
            target.setPaymentStatus("PAID");
        } else {
            target.setPaymentStatus("UNPAID");
        }

        if (source.getBillingAddress() != null) {
            target.setBillingAddress(addressesConverterService.convertToPojo(source.getBillingAddress()));
        }

        if (source.getShippingAddress() != null) {
            target.setShippingAddress(addressesConverterService.convertToPojo(source.getShippingAddress()));
        }

        if (source.getDetails() != null) {
            List<OrderDetailPojo> details = source.getDetails().stream()
                .map(this::convertDetailToPojo)
                .collect(Collectors.toList());
            target.setDetails(details);
        }

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

        Person sourcePerson = source.getCustomer().getPerson();
        String fullName = String.format("%s %s", sourcePerson.getFirstName(), sourcePerson.getLastName()).trim();
        target.setCustomerName(fullName);
        target.setCustomerEmail(sourcePerson.getEmail());
        target.setRecipientName(fullName);
        target.setRecipientPhone(sourcePerson.getPhone1());
        target.setRecipientEmail(sourcePerson.getEmail());

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
        if (model.getDiscountCode()!=null) {
            target.setDiscountCode(model.getDiscountCode());
            target.setDiscountValue(model.getDiscountValue());
        }
        if (model.getCartSessionToken()!=null) {
            target.setCartSessionToken(model.getCartSessionToken());
        }
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
        ProductVariantPojo variant = null;
        if (source.getProductVariant() != null) {
            variant = productVariantsConverterService.convertToPojo(source.getProductVariant());
        }
        return OrderDetailPojo.builder()
            .unitValue(source.getUnitValue())
            .units(source.getUnits())
            .product(product)
            .variant(variant)
            .description(source.getDescription())
            .variantId(source.getProductVariant() != null ? source.getProductVariant().getId() : null)
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
                // Enforce canonical variant pricing from server-side data.
                // Client-provided unitValue must not override variant pricing.
                unitValue = product.getPrice() + variant.getPriceModifier();
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
            // Normalize common aliases
            String normalizedName = paymentType.trim();
            if (normalizedName.equalsIgnoreCase("COD")) {
                normalizedName = "COD";
            } else if (normalizedName.equalsIgnoreCase("VNPAY") || normalizedName.equalsIgnoreCase("WebPay Plus")) {
                normalizedName = "VNPAY";
            }

            final String finalPaymentType = normalizedName;
            
            // 1. Try finding by name (case-insensitive)
            List<PaymentType> allTypes = paymentTypesRepository.findAll();
            Optional<PaymentType> found = allTypes.stream()
                .filter(pt -> pt.getName().equalsIgnoreCase(finalPaymentType))
                .findFirst();
            
            if (found.isPresent()) {
                target.setPaymentType(found.get());
                return;
            }

            // 2. If name search failed but it's a common type, try finding by known IDs
            // (Mapping based on data.sql: 1=VNPAY, 2=COD)
            Long targetId = finalPaymentType.equals("VNPAY") ? 1L : (finalPaymentType.equals("COD") ? 2L : null);
            if (targetId != null) {
                Optional<PaymentType> foundById = paymentTypesRepository.findById(targetId);
                if (foundById.isPresent()) {
                    PaymentType pt = foundById.get();
                    // If we found it by ID but name was different, let's update the name to be sure
                    pt.setName(finalPaymentType);
                    target.setPaymentType(paymentTypesRepository.save(pt));
                    return;
                }
            }

            // 3. Last resort: try to create it (Risk: might fail if sequence is out of sync)
            if (finalPaymentType.equals("VNPAY") || finalPaymentType.equals("COD")) {
                PaymentType newType = PaymentType.builder()
                    .name(finalPaymentType)
                    .build();
                target.setPaymentType(paymentTypesRepository.save(newType));
            } else {
                throw new RuntimeException("The payment type '" + finalPaymentType + "' does not exist");
            }
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

        if (existingCustomer.isPresent()) {
            target.setCustomer(existingCustomer.get());
            return;
        }

        // If no Customer record exists, check if a User exists with the same email
        if (pojoCustomer != null && StringUtils.isNotBlank(pojoCustomer.getEmail())) {
            Optional<User> existingUser = usersRepository.findByNameWithProfile(pojoCustomer.getEmail());
            if (existingUser.isEmpty()) {
                // Also check by person email specifically if name != email
                existingUser = usersRepository.findAll().stream()
                    .filter(u -> u.getPerson() != null && pojoCustomer.getEmail().equalsIgnoreCase(u.getPerson().getEmail()))
                    .findFirst();
            }

            if (existingUser.isPresent() && existingUser.get().getPerson() != null) {
                Person existingPerson = existingUser.get().getPerson();
                // Check if this Person already has a Customer record (double check)
                Optional<Customer> customerForPerson = customersRepository.findByPersonId(existingPerson.getId());
                if (customerForPerson.isPresent()) {
                    target.setCustomer(customerForPerson.get());
                } else {
                    // Create a new Customer record pointing to the EXISTING Person
                    Customer newCustomer = Customer.builder()
                        .person(existingPerson)
                        .build();
                    target.setCustomer(customersRepository.save(newCustomer));
                }
                return;
            }
        }

        // Fallback: create a brand new Customer (and new Person)
        Customer customer = customersConverterService.convertToNewEntity(pojoCustomer);
        target.setCustomer(customer);
    }

    /**
     * Tries to deduce billing information.<br/>
     * If the customer requested an invoice for a business company, it will try to match said company
     * against the existing records, or create a new record for that company.<br/>
     * The same process goes for the billing address.
     */
    private void convertBillingInformationForEntity(OrderPojo model, Order target) throws BadInputException {
        String pojoBillingTypeName = model.getBillingType();

        // Normalize common aliases to match DB constants
        if (pojoBillingTypeName != null) {
            if (pojoBillingTypeName.equalsIgnoreCase("individual")) {
                pojoBillingTypeName = org.monostudio.config.Constants.BILLING_TYPE_INDIVIDUAL;
            } else if (pojoBillingTypeName.equalsIgnoreCase("enterprise")) {
                pojoBillingTypeName = org.monostudio.config.Constants.BILLING_TYPE_ENTERPRISE;
            }
        }

        String finalBillingType = pojoBillingTypeName;
        Optional<BillingType> existingBillingType = billingTypesRepository.findByName(finalBillingType);
        if (existingBillingType.isEmpty()) {
            throw new BadInputException("Specified billing type '" + finalBillingType + "' does not exist");
        }
        target.setBillingType(existingBillingType.get());

        if (finalBillingType.equals(BILLING_TYPE_ENTERPRISE)) {
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
            // Extract tax from the inclusive price: unitNet = unitValue / 1.19
            double unitNetValue = unitValue / (1 + TAX_PERCENT);
            int unitTaxValue = unitValue - (int) unitNetValue;
            netValue += (int) unitNetValue * sd.getUnits();
            taxesValue += unitTaxValue * sd.getUnits();
            totalUnits += sd.getUnits();
        }
        entity.setNetValue(netValue);
        entity.setTaxesValue(taxesValue);
        int subtotal = netValue + taxesValue;
        int discountValue = entity.getDiscountValue();
        entity.setTotalValue(Math.max(0, subtotal + entity.getTransportValue() - discountValue));
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
