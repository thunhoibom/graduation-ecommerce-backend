package org.monostudio.jpa;

import java.io.Serializable;

/**
 * An entity that has an <i>id</i> field and a getter/setter pair to interact with it.<br/>
 * Meant to be used alongside <code>CrudRepository</code> or <code>JpaRepository</code> since these expose
 * methods <code>findById</code> and <code>getById</code>.
 */
public interface DBEntity
    extends Serializable {
    Long getId();

    void setId(Long id);
}
