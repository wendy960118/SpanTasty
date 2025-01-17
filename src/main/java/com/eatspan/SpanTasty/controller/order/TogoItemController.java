package com.eatspan.SpanTasty.controller.order;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eatspan.SpanTasty.entity.order.FoodKindEntity;
import com.eatspan.SpanTasty.entity.order.MenuEntity;
import com.eatspan.SpanTasty.entity.order.TogoItemEntity;
import com.eatspan.SpanTasty.entity.order.TogoItemId;
import com.eatspan.SpanTasty.service.order.FoodKindService;
import com.eatspan.SpanTasty.service.order.MenuService;
import com.eatspan.SpanTasty.service.order.TogoItemService;

@Controller
@RequestMapping("/order")
public class TogoItemController {
	
	@Autowired
	private MenuService menuService;
	
	@Autowired
	private TogoItemService togoItemService;
	
	@Autowired
	private FoodKindService foodKindService;
	
	@GetMapping("/togo/{togoId}/items")
	public String getTogoItemPage(@PathVariable Integer togoId, Model model) {
		List<TogoItemEntity> togoItems = togoItemService.getAllTogoItemByTogoId(togoId);
		if (togoItems.isEmpty()) {
			model.addAttribute("message", "未找到對應的訂單項目");
		}else {
			List<FoodKindEntity> foodKindList = foodKindService.getAllFoodKind();
			List<MenuEntity> foodList = menuService.getAllFoods();
			model.addAttribute("foodKindList", foodKindList);
			model.addAttribute("foodList", foodList);
			model.addAttribute("togoItems", togoItems);
		}
		return "spantasty/order/getTogoItems";
	}
	
	//ajax togoId查詢 
	@GetMapping("/togo/items/{togoId}")
	@ResponseBody
	public ResponseEntity<List<TogoItemEntity>> getTogoItemsById(@PathVariable Integer togoId) {
		List<TogoItemEntity> togoItems = togoItemService.getAllTogoItemByTogoId(togoId);
		if (togoItems.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return ResponseEntity.ok(togoItems);
	}
	
	@PostMapping("/togo/{togoId}/items")
	@ResponseBody
	public ResponseEntity<List<TogoItemEntity>> addTogoItems(
			@RequestBody List<TogoItemEntity> newTogoItems,
			@PathVariable Integer togoId) {
		togoItemService.addTogoItems(togoId, newTogoItems);
		return ResponseEntity.ok(togoItemService.getAllTogoItemByTogoId(togoId));
	}
	
	@DeleteMapping("/togo/{togoId}/items/{foodId}")
	public ResponseEntity<List<TogoItemEntity>> deleteTogoItemByTogoIdFoodId(
			@PathVariable Integer togoId,
			@PathVariable Integer foodId) {
		TogoItemId togoItemId = new TogoItemId(togoId, foodId);
		togoItemService.deleteTogoItemById(togoItemId);
		return ResponseEntity.ok(togoItemService.getAllTogoItemByTogoId(togoId));
		
	}
	
	@PutMapping("/togo/{togoId}/items/{foodId}")
	public ResponseEntity<List<TogoItemEntity>> updateTogoItemByTogoIdFoodId(
			@PathVariable Integer togoId,
			@PathVariable Integer foodId,
			@RequestBody Integer newAmount){
		TogoItemId togoItemId = new TogoItemId(togoId, foodId);
		TogoItemEntity existingTogoItem = togoItemService.getTogoItemByTogoIdFoodId(togoItemId);
		// 若沒有這筆訂單明細回傳404
		if (existingTogoItem == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		togoItemService.updateTogoItemById(togoItemId, newAmount);
		return ResponseEntity.ok(togoItemService.getAllTogoItemByTogoId(togoId));
	}
}






