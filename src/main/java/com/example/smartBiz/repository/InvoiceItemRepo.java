package com.example.smartBiz.repository;

import com.example.smartBiz.dto.TopProductDto;
import com.example.smartBiz.entity.InvoiceItem;
import com.example.smartBiz.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceItemRepo extends JpaRepository<InvoiceItem, Long> {
    @Query("""
        select new com.example.smartBiz.dto.TopProductDto(
            p.id, p.name, sum(ii.quantity)
        )
        from InvoiceItem ii
        join ii.invoice i
        join ii.product p
        where i.status = :status
          and i.invoiceDate between :start and :end
          and i.businessId = :businessId
        group by p.id, p.name
        order by sum(ii.quantity) desc
    """)
    List<TopProductDto> findTopProductsByBusiness(
            @Param("status") InvoiceStatus status,
         
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("businessId") Long businessId,
            Pageable pageable
    );

}
