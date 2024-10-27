package com.eatspan.SpanTasty.repository.order;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eatspan.SpanTasty.entity.order.TogoEntity;

public interface TogoRepository extends JpaRepository<TogoEntity, Integer> {
	
	List<TogoEntity> findByMemberId(Integer memberId);
	Page<TogoEntity> findByRestaurantId(Integer restaurantId, Pageable pageable);
	List<TogoEntity> findTogoByTgPhoneContaining(String tgPhone);
	List<TogoEntity> findByTgNameAndTgPhone(String tgName, String tgPhone);
	
}
