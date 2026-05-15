package org.monostudio.api.services;

import jakarta.servlet.http.HttpServletRequest;
import org.monostudio.api.models.ContactInquiryCreatedResponse;
import org.monostudio.api.models.ContactInquiryRequest;

public interface ContactInquiryService {

    ContactInquiryCreatedResponse submit(ContactInquiryRequest request, HttpServletRequest httpRequest);
}
