package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.services.PaginationService;
import org.monostudio.config.ApiProperties;

import java.util.Map;

/**
 * Service that reads pagination params from String-to-String <i>Map</i>s that represent HTTP query params.
 */
@Service
public class PaginationServiceImpl
    implements PaginationService {
    protected final ApiProperties apiProperties;

    @Autowired
    public PaginationServiceImpl(
        ApiProperties apiProperties
    ) {
        this.apiProperties = apiProperties;
    }

    @Override
    public int determineRequestedPageIndex(Map<String, String> requestParams)
        throws NumberFormatException {
        if (requestParams==null || !requestParams.containsKey("pageIndex")) {
            return 0;
        }
        return Integer.parseInt(requestParams.get("pageIndex"));
    }

    @Override
    public int determineRequestedPageSize(Map<String, String> requestParams)
        throws NumberFormatException {
        if (requestParams==null || !requestParams.containsKey("pageSize")) {
            return apiProperties.getItemsPerPage();
        }
        int pageSize = Integer.parseInt(requestParams.get("pageSize"));
        Integer maxAllowedPageSize = apiProperties.getMaxAllowedPageSize();
        return (pageSize < maxAllowedPageSize) ?
            pageSize:
            maxAllowedPageSize;
    }
}
