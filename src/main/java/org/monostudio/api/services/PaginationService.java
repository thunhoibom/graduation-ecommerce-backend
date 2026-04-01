package org.monostudio.api.services;

import java.util.Map;

/**
 * Reads and parses information stored in {@link java.util.Map}s as parameters for pagination of data.
 */
public interface PaginationService {

    /**
     * Dictates the desired (or valid, if none) index page to be fetched.
     *
     * @param requestParams Input request params stored as key-value String pairs.
     * @return The 0-based index of the page to be fetched.
     * @throws NumberFormatException If the requested index is not a number.
     */
    int determineRequestedPageIndex(Map<String, String> requestParams) throws NumberFormatException;

    /**
     * Dictates the desired (or valid, if none) size of the page to be fetched.
     *
     * @param requestParams Input request params stored as key-value String pairs.
     * @return The size, in items, of the page to be fetched.
     * @throws NumberFormatException If the requested page size is not a number.
     */
    int determineRequestedPageSize(Map<String, String> requestParams) throws NumberFormatException;
}
