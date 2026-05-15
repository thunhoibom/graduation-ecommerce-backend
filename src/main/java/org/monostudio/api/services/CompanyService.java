package org.monostudio.api.services;

import org.monostudio.api.models.CompanyDetailsPojo;

/**
 * Service that informs about the business itself. As this data is constant, it should be provided faster than anything else.
 */
public interface CompanyService {
    /**
     * Fetches information about the business.
     *
     * @return An object containing all general and relevant information about the business.
     */
    CompanyDetailsPojo readDetails();
}
