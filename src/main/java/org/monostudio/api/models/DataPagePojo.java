package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A container object that holds a page of data for a specific type, and information about that page.<br/>
 * By default, it uses an {@link java.util.ArrayList}, but any {@link java.util.Collection} may be used.
 *
 * @param <T> The type of data of this container.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude
public class DataPagePojo<T> {
    private Collection<T> items = new ArrayList<>();
    private int pageIndex = 0;
    private long totalCount = 0;
    private int pageSize = 0;
}
