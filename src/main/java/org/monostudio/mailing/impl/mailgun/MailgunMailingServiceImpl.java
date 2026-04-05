package org.monostudio.mailing.impl.mailgun;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import kong.unirest.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.models.ReceiptDetailPojo;
import org.monostudio.api.models.ReceiptPojo;
import org.monostudio.api.models.OrderDetailPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.ReturnRequestPojo;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.services.conversion.CustomersConverterService;
import org.monostudio.mailing.MailingProperties;
import org.monostudio.mailing.MailingService;
import org.monostudio.mailing.MailingServiceException;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static org.monostudio.config.Constants.ORDER_STATUS_COMPLETED;
import static org.monostudio.config.Constants.ORDER_STATUS_PAID_CONFIRMED;
import static org.monostudio.config.Constants.ORDER_STATUS_PAID_UNCONFIRMED;
import static org.monostudio.config.Constants.ORDER_STATUS_REJECTED;

/**
 * Implements Mailgun HTTP API as a mail service provider.<br/>
 * A Mailgun account is required to use this.<br/>
 * Read about Mailgun on <a href="https://www.mailgun.com/">their website here</a>.
 */
@Service
@Profile("mailgun")
public class MailgunMailingServiceImpl
    implements MailingService {
    public static final String MAILGUN_HOST = "https://api.mailgun.net/v3/";
    private static final String CUSTOMER_MAPS_KEY_PREFIX = "customer:";
    private static final String OWNERS_MAPS_KEY_PREFIX = "owners:";
    private static final String RETURN_REQUEST_PREFIX = "returnRequest:";

    private final Logger logger = LoggerFactory.getLogger(MailgunMailingServiceImpl.class);
    private final MailingProperties internalMailingIntegrationProperties;
    private final MailgunMailingProperties mailgunProperties;
    private final OrdersRepository ordersRepository;
    private final CustomersConverterService customersConverterService;
    private final Map<String, String> orderStatus2MailgunTemplatesMap;
    private final Map<String, String> orderStatus2MailSubjectMap;
    private final Map<String, String> returnRequestTemplatesMap;
    private final Map<String, String> returnRequestSubjectsMap;
    private final ConversionService conversionService;
    private final ObjectMapper mailObjectMapper;
    private final HttpRequestWithBody baseRequestWithBody;

    @Autowired
    public MailgunMailingServiceImpl(
        MailingProperties mailingIntegrationProperties,
        MailgunMailingProperties mailgunProperties,
        OrdersRepository ordersRepository,
        CustomersConverterService customersConverterService,
        ConversionService conversionService
    ) {
        this.internalMailingIntegrationProperties = mailingIntegrationProperties;
        this.mailgunProperties = mailgunProperties;
        this.ordersRepository = ordersRepository;
        this.customersConverterService = customersConverterService;
        this.conversionService = conversionService;
        this.orderStatus2MailgunTemplatesMap = this.makeTemplatesMap();
        this.orderStatus2MailSubjectMap = this.makeSubjectsMap();
        this.returnRequestTemplatesMap = this.makeReturnRequestTemplatesMap();
        this.returnRequestSubjectsMap = this.makeReturnRequestSubjectsMap();
        this.mailObjectMapper = this.mailObjectMapper();
        this.baseRequestWithBody = this.prepareBaseApiRequest();
    }

    @Override
    public void notifyOrderStatusToClient(OrderPojo sell)
        throws MailingServiceException {
        String mapsKey = CUSTOMER_MAPS_KEY_PREFIX + sell.getStatus();
        if (!orderStatus2MailSubjectMap.containsKey(mapsKey)) {
            return;
        }

        PersonPojo customer = sell.getCustomer();
        String customerName = customer.getFirstName() + " " + customer.getLastName();
        String recipient = customerName + " <" + customer.getEmail() + ">";
        String messageSubject = orderStatus2MailSubjectMap.get(mapsKey);
        String mailgunTemplateName = orderStatus2MailgunTemplatesMap.get(mapsKey);
        String fullSubject = messageSubject + " [#" + sell.getBuyOrder() + "]";
        String variables = this.makeMailgunVariablesFrom(sell);
        HttpResponse<JsonNode> request = this.preparePOST(
                recipient,
                fullSubject,
                mailgunTemplateName,
                variables)
            .asJson();
        try {
            if (((String) request.getBody().getObject().get("id")).isBlank()) {
                logger.warn("Mailgun returned the following JSON: {}", request.getBody());
                throw new MailingServiceException("Status of the sent e-mail is unknown, Mailgun did not provide an ID for this api");
            }
        } catch (JSONException ex) {
            throw new MailingServiceException("Status of the sent e-mail is unknown, Mailgun threw an exception while validating the response", ex);
        }
    }

    @Override
    public void notifyOrderStatusToOwners(OrderPojo sell)
        throws MailingServiceException {
        String mapsKey = OWNERS_MAPS_KEY_PREFIX + sell.getStatus();
        if (!orderStatus2MailSubjectMap.containsKey(mapsKey)) {
            return;
        }

        String messageSubject = orderStatus2MailSubjectMap.get(mapsKey);
        String mailgunTemplateName = orderStatus2MailgunTemplatesMap.get(mapsKey);
        String fullSubject = messageSubject + " [#" + sell.getBuyOrder() + "]";
        String variables = this.makeMailgunVariablesFrom(sell);
        HttpResponse<JsonNode> response = this.preparePOST(
                internalMailingIntegrationProperties.getOwnerEmail(),
                fullSubject,
                mailgunTemplateName,
                variables)
            .asJson();
        try {
            if (((String) response.getBody().getObject().get("id")).isBlank()) {
                logger.warn("Mailgun returned the following JSON: {}", response.getBody());
                throw new MailingServiceException("Status of the sent e-mail is unknown, Mailgun did not provide an ID for this api");
            }
        } catch (JSONException ex) {
            throw new MailingServiceException("Status of the sent e-mail is unknown, Mailgun threw an exception while validating the response", ex);
        }
    }

    @Override
    public void notifyLowStockAlert(String productName, int currentStock)
        throws MailingServiceException {
        String subject = "[Mono Studio] Low Stock Alert: " + productName;
        HttpResponse<JsonNode> response = this.preparePOST(
                internalMailingIntegrationProperties.getOwnerEmail(),
                subject,
                null,
                "{\"product\":\"" + productName + "\",\"stock\":" + currentStock + "}")
            .asJson();
        logger.info("Low stock alert sent for {}: response={}", productName, response.getStatus());
    }

    @Override
    public void notifyReturnRequestStatusToClient(ReturnRequestPojo request)
        throws MailingServiceException {
        String mapsKey = RETURN_REQUEST_PREFIX + request.getStatus();
        if (!returnRequestSubjectsMap.containsKey(mapsKey)) {
            logger.info("No email template configured for return request status: {}", request.getStatus());
            return;
        }

        Optional<Order> orderOpt = ordersRepository.findById(
            request.getOrderId() != null ? request.getOrderId() : -1L);

        if (orderOpt.isEmpty()) {
            logger.warn("Cannot send return request email: order {} not found", request.getOrderId());
            return;
        }

        Order order = orderOpt.get();
        PersonPojo customer = customersConverterService.convertToPojo(order.getCustomer());
        String customerName = customer.getFirstName() + " " + customer.getLastName();
        String recipient = customerName + " <" + customer.getEmail() + ">";

        String messageSubject = returnRequestSubjectsMap.get(mapsKey);
        String fullSubject = messageSubject + " [#" + request.getId() + "]";
        String mailgunTemplateName = returnRequestTemplatesMap.get(mapsKey);
        String variables = this.makeReturnRequestMailgunVariables(request);

        HttpResponse<JsonNode> response = this.preparePOST(
                recipient,
                fullSubject,
                mailgunTemplateName,
                variables)
            .asJson();

        try {
            if (((String) response.getBody().getObject().get("id")).isBlank()) {
                logger.warn("Mailgun returned: {}", response.getBody());
                throw new MailingServiceException("Return request email status unknown, Mailgun did not return an ID");
            }
        } catch (JSONException ex) {
            throw new MailingServiceException("Return request email failed", ex);
        }
    }

    @Override
    public void notifyReturnRequestToOwners(ReturnRequestPojo request)
        throws MailingServiceException {
        String subject = "[Mono Studio] New Return Request #" + request.getId();
        String variables = this.makeReturnRequestMailgunVariables(request);
        HttpResponse<JsonNode> response = this.preparePOST(
                internalMailingIntegrationProperties.getOwnerEmail(),
                subject,
                null,
                variables)
            .asJson();
        logger.info("Return request notification sent: response={}", response.getStatus());
    }

    private String makeReturnRequestMailgunVariables(ReturnRequestPojo request) {
        try {
            String requestJson = mailObjectMapper.writeValueAsString(request);
            return "{\"returnRequest\": " + requestJson + "}";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not stringify return request object", e);
        }
    }

    private MultipartBody preparePOST(String to, String subject, String templateName, String templateVariables) {
        MultipartBody req = baseRequestWithBody
            .field("from", internalMailingIntegrationProperties.getSenderEmail())
            .field("to", to)
            .field("subject", subject)
            .field("h:X-Mailgun-Variables", templateVariables);
        if (templateName != null) {
            req.field("template", templateName);
        }
        return req;
    }

    private Map<String, String> makeTemplatesMap() {
        return Map.of(
            CUSTOMER_MAPS_KEY_PREFIX + ORDER_STATUS_PAID_UNCONFIRMED, mailgunProperties.getCustomerOrderPaymentTemplate(),
            CUSTOMER_MAPS_KEY_PREFIX + ORDER_STATUS_PAID_CONFIRMED, mailgunProperties.getCustomerOrderConfirmationTemplate(),
            CUSTOMER_MAPS_KEY_PREFIX + ORDER_STATUS_REJECTED, mailgunProperties.getCustomerOrderRejectionTemplate(),
            CUSTOMER_MAPS_KEY_PREFIX + ORDER_STATUS_COMPLETED, mailgunProperties.getCustomerOrderCompletionTemplate(),
            OWNERS_MAPS_KEY_PREFIX + ORDER_STATUS_PAID_CONFIRMED, mailgunProperties.getOwnerOrderConfirmationTemplate(),
            OWNERS_MAPS_KEY_PREFIX + ORDER_STATUS_REJECTED, mailgunProperties.getOwnerOrderRejectionTemplate(),
            OWNERS_MAPS_KEY_PREFIX + ORDER_STATUS_COMPLETED, mailgunProperties.getOwnerOrderCompletionTemplate()
        );
    }

    private Map<String, String> makeSubjectsMap() {
        return Map.of(
            CUSTOMER_MAPS_KEY_PREFIX + ORDER_STATUS_PAID_UNCONFIRMED, internalMailingIntegrationProperties.getCustomerOrderPaymentSubject(),
            CUSTOMER_MAPS_KEY_PREFIX + ORDER_STATUS_PAID_CONFIRMED, internalMailingIntegrationProperties.getCustomerOrderConfirmationSubject(),
            CUSTOMER_MAPS_KEY_PREFIX + ORDER_STATUS_REJECTED, internalMailingIntegrationProperties.getCustomerOrderRejectionSubject(),
            CUSTOMER_MAPS_KEY_PREFIX + ORDER_STATUS_COMPLETED, internalMailingIntegrationProperties.getCustomerOrderCompletionSubject(),
            OWNERS_MAPS_KEY_PREFIX + ORDER_STATUS_PAID_CONFIRMED, internalMailingIntegrationProperties.getOwnerOrderConfirmationSubject(),
            OWNERS_MAPS_KEY_PREFIX + ORDER_STATUS_REJECTED, internalMailingIntegrationProperties.getOwnerOrderRejectionSubject(),
            OWNERS_MAPS_KEY_PREFIX + ORDER_STATUS_COMPLETED, internalMailingIntegrationProperties.getOwnerOrderCompletionSubject()
        );
    }

    private Map<String, String> makeReturnRequestTemplatesMap() {
        return Map.of(
            RETURN_REQUEST_PREFIX + ReturnRequest.ReturnRequestStatus.PENDING.name(),
                mailgunProperties.getCustomerReturnRequestCreatedTemplate(),
            RETURN_REQUEST_PREFIX + ReturnRequest.ReturnRequestStatus.APPROVED.name(),
                mailgunProperties.getCustomerReturnRequestApprovedTemplate(),
            RETURN_REQUEST_PREFIX + ReturnRequest.ReturnRequestStatus.REJECTED.name(),
                mailgunProperties.getCustomerReturnRequestRejectedTemplate(),
            RETURN_REQUEST_PREFIX + ReturnRequest.ReturnRequestStatus.REFUND_COMPLETED.name(),
                mailgunProperties.getCustomerReturnRequestRefundCompletedTemplate()
        );
    }

    private Map<String, String> makeReturnRequestSubjectsMap() {
        return Map.of(
            RETURN_REQUEST_PREFIX + ReturnRequest.ReturnRequestStatus.PENDING.name(),
                internalMailingIntegrationProperties.getCustomerReturnRequestCreatedSubject(),
            RETURN_REQUEST_PREFIX + ReturnRequest.ReturnRequestStatus.APPROVED.name(),
                internalMailingIntegrationProperties.getCustomerReturnRequestApprovedSubject(),
            RETURN_REQUEST_PREFIX + ReturnRequest.ReturnRequestStatus.REJECTED.name(),
                internalMailingIntegrationProperties.getCustomerReturnRequestRejectedSubject(),
            RETURN_REQUEST_PREFIX + ReturnRequest.ReturnRequestStatus.REFUND_COMPLETED.name(),
                internalMailingIntegrationProperties.getCustomerReturnRequestRefundCompletedSubject()
        );
    }

    private ObjectMapper mailObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper
            .configOverride(Instant.class)
            .setFormat(JsonFormat.Value
                .forPattern(internalMailingIntegrationProperties.getDateFormat())
                .withTimeZone(TimeZone.getTimeZone(internalMailingIntegrationProperties.getDateTimezone())));
        return objectMapper;
    }

    private HttpRequestWithBody prepareBaseApiRequest() {
        URI targetApiUrl = URI.create(MAILGUN_HOST + this.mailgunProperties.getDomain() + "/messages");
        return Unirest.post(targetApiUrl.toString())
            .basicAuth("api", this.mailgunProperties.getApiKey());
    }

    private String makeMailgunVariablesFrom(OrderPojo sell) {
        String variables;
        try {
            ReceiptPojo receipt = this.turnIntoReceipt(sell);
            String transactionJson = mailObjectMapper.writeValueAsString(receipt);
            String customerJson = mailObjectMapper.writeValueAsString(sell.getCustomer());
            variables = "{\"transaction\": " + transactionJson +
                ", \"customer\": " + customerJson + "}";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not stringify transaction object", e);
        }
        return variables;
    }

    private ReceiptPojo turnIntoReceipt(OrderPojo sell) {
        ReceiptPojo receipt = conversionService.convert(sell, ReceiptPojo.class);
        if (receipt!=null && sell.getDetails()!=null) {
            List<ReceiptDetailPojo> receiptDetails = new ArrayList<>();
            for (OrderDetailPojo detail : sell.getDetails()) {
                ReceiptDetailPojo convert = conversionService.convert(detail, ReceiptDetailPojo.class);
                receiptDetails.add(convert);
            }
            receipt.setDetails(receiptDetails);
        }
        return receipt;
    }
}
