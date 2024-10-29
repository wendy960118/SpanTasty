package com.eatspan.SpanTasty.controller.order;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.eatspan.SpanTasty.dto.order.FoodKindDto;
import com.eatspan.SpanTasty.entity.order.FoodKindEntity;
import com.eatspan.SpanTasty.service.order.FoodKindService;

@RestController
public class FoodKindController {
	
	@Autowired
	private FoodKindService foodKindService;
	
	//查詢全部
	@GetMapping("/order/kinds")
	public ResponseEntity<List<FoodKindEntity>> getAllFoodKind() {
		return ResponseEntity.ok(foodKindService.getAllFoodKind());
		
	}
	
	//新增
	@PostMapping("/order/kinds")
	public ResponseEntity<List<FoodKindEntity>> addFoodKind(@RequestBody FoodKindDto foodKindDto) {
		foodKindService.addFoodKind(foodKindDto.getFoodKindName());
		return ResponseEntity.ok(foodKindService.getAllFoodKind());
	}
	
	
}


