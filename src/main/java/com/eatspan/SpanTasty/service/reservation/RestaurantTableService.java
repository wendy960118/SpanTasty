package com.eatspan.SpanTasty.service.reservation;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eatspan.SpanTasty.entity.reservation.RestaurantTable;
import com.eatspan.SpanTasty.entity.reservation.RestaurantTableId;
import com.eatspan.SpanTasty.repository.reservation.RestaurantTableRepository;

@Service
public class RestaurantTableService {
	
	@Autowired
	private RestaurantTableRepository restaurantTableRepository;
	
	
	
	// 新增餐廳桌位
	public RestaurantTable addRestaurantTable(RestaurantTable restaurantTable) {
		return restaurantTableRepository.save(restaurantTable);
	}
	
	
	// 刪除餐廳桌位
	public void deleteRestaurantTable(RestaurantTableId restaurantTableId) {
		restaurantTableRepository.deleteById(restaurantTableId);
	}
	
	
	// 更新餐廳桌位
	public RestaurantTable updateRestaurantTable(RestaurantTable restaurantTable) {
		Optional<RestaurantTable> optional = restaurantTableRepository.findById(restaurantTable.getId());
		System.out.println(666);
		if(optional.isPresent()) {
			System.out.println(000000);
			System.out.println("test here");
			return restaurantTableRepository.save(restaurantTable);
		}
		return null;
	}
	
	
	// 查詢餐廳桌位byId
	public RestaurantTable findRestaurantTableById(RestaurantTableId restaurantTableId) {
		return restaurantTableRepository.findById(restaurantTableId).orElse(null);
	}

	
	// 查詢餐廳(id)所有桌位
	public List<RestaurantTable> findAllRestaurantTable(Integer restaurantId) {
		return restaurantTableRepository.findRestaurantTableByRestaurantId(restaurantId);
	}
	
	
	
	
	
}
