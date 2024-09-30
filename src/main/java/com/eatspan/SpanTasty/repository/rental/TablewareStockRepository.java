package com.eatspan.SpanTasty.repository.rental;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.eatspan.SpanTasty.entity.rental.TablewareStock;
import com.eatspan.SpanTasty.entity.rental.TablewareStock.TablewareStockId;

@Repository
public interface TablewareStockRepository extends JpaRepository<TablewareStock, TablewareStockId> {
	
	@Query(value = "from TablewareStock where"
            + "(:tablewareId IS NULL OR tablewareId = :tablewareId) AND "
            + "(:restaurantId IS NULL OR restaurantId = :restaurantId)")
	List<TablewareStock> findTablewareStocksByCriteria(
	        @Param("tablewareId") Integer tablewareId,
	        @Param("restaurantId") Integer restaurantId);
	
	@Query(value = "from TablewareStock where tablewareId = :tablewareId AND restaurantId = :restaurantId")
	TablewareStock findTablewareStocksByStockId(@Param("tablewareId") Integer tablewareId, @Param("restaurantId") Integer restaurantId);
	
}