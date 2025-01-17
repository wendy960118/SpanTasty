package com.eatspan.SpanTasty.repository.store;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eatspan.SpanTasty.entity.store.ProductType;

public interface ProductTypeRepository extends JpaRepository<ProductType, Integer> {

	 @Query("SELECT pt FROM ProductType pt WHERE pt.productTypeName LIKE %:name%")
	    List<ProductType> findByProductTypeName(@Param("name") String name);
	 
	 //Coupon使用 取得productType選項
	 @Query("SELECT pt.productTypeName FROM ProductType pt ")
	 	List<String> findProductTypeName();

	List<ProductType> findByProductTypeId(Integer productTypeId);
//	 Optional<ProductType> findByProductTypeId(Integer productTypeId);
	 
}
