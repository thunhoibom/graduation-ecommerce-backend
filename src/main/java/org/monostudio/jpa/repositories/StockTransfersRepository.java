package org.monostudio.jpa.repositories;

import java.util.List;
import java.util.Optional;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.StockTransfer;
import org.monostudio.jpa.entities.StockTransfer.StockTransferStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface StockTransfersRepository
    extends Repository<StockTransfer> {

    Optional<StockTransfer> findByCode(String code);

    @Query("""
        SELECT DISTINCT st FROM StockTransfer st
        WHERE (:status IS NULL OR st.status = :status)
        ORDER BY st.createdAt DESC
        """)
    List<StockTransfer> findByStatus(@Param("status") StockTransferStatus status);

    long countByStatus(StockTransferStatus status);
}
