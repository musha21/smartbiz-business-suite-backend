package com.example.smartBiz.repository;

import com.example.smartBiz.entity.RegisterTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RegisterTransactionRepo extends JpaRepository<RegisterTransaction, Long> {

    List<RegisterTransaction> findBySessionIdOrderByTimestampDesc(Long sessionId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM RegisterTransaction t
            WHERE t.sessionId = :sessionId AND t.type = :type
            """)
    BigDecimal sumAmountBySessionIdAndType(
            @Param("sessionId") Long sessionId,
            @Param("type") RegisterTransaction.TransactionType type);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM RegisterTransaction t
            WHERE t.sessionId = :sessionId AND t.type = :type AND t.paymentMethod = :paymentMethod
            """)
    BigDecimal sumAmountBySessionIdAndTypeAndPaymentMethod(
            @Param("sessionId") Long sessionId,
            @Param("type") RegisterTransaction.TransactionType type,
            @Param("paymentMethod") RegisterTransaction.PaymentMethod paymentMethod);

    List<RegisterTransaction> findBySessionIdAndType(Long sessionId, RegisterTransaction.TransactionType type);
}
