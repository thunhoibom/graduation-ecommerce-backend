package org.monostudio.jpa.repositories;

import org.monostudio.jpa.entities.ReturnRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestItemsRepository
    extends JpaRepository<ReturnRequestItem, Long> {
    List<ReturnRequestItem> findByReturnRequestId(Long returnRequestId);
}
