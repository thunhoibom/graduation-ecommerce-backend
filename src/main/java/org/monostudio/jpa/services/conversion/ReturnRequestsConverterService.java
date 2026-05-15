package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ReturnRequestItemPojo;
import org.monostudio.api.models.ReturnRequestPojo;
import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.entities.ReturnRequestItem;
import org.monostudio.jpa.services.ConverterService;

import java.util.Collection;

public interface ReturnRequestsConverterService
    extends ConverterService<ReturnRequestPojo, ReturnRequest> {
    ReturnRequestItemPojo convertItemToPojo(ReturnRequestItem source);
    Collection<ReturnRequestItem> convertItemsToEntities(Collection<ReturnRequestItemPojo> items);
}
