package com.eatspan.SpanTasty.controller.order;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eatspan.SpanTasty.dto.order.TogoDto;
import com.eatspan.SpanTasty.dto.order.TogoHistoryDto;
import com.eatspan.SpanTasty.entity.order.FoodKindEntity;
import com.eatspan.SpanTasty.entity.order.TogoEntity;
import com.eatspan.SpanTasty.entity.reservation.Restaurant;
import com.eatspan.SpanTasty.service.order.FoodKindService;
import com.eatspan.SpanTasty.service.order.TogoService;
import com.eatspan.SpanTasty.service.reservation.RestaurantService;
import com.eatspan.SpanTasty.utils.order.PickupTime;

@Controller
@RequestMapping("/order")
public class TogoController {
	
	@Autowired
	private TogoService togoService;
	
	@Autowired
	private FoodKindService foodKindService;
	
	@Autowired
	private RestaurantService restaurantService;
	
	@Autowired
	private PickupTime pickupTime;
	
	//查詢
	// 有參數：模糊查詢，無參數：查詢全部
//	@GetMapping("/togo")
//	public ResponseEntity<List<TogoEntity>> getTogo(
//			@RequestParam(required = false) Integer memberId,
//			@RequestParam(required = false) String tgPhone) {
//		//有輸入memberId
//		if (memberId != null) {
//			List<TogoEntity> togo = togoService.getTogoByMemberId(memberId);
//			// 檢查memberId有沒有訂單資料，沒有:404，有:回傳訂單
//			if (togo.isEmpty()) {
//				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//			}
//			return ResponseEntity.ok(togo);
//		}
//		//有輸入tgPhone
//		if (tgPhone != null) {
//			List<TogoEntity> togo = togoService.getTogoByPhone(tgPhone);
//			// 檢查tgPhone有沒有訂單資料，沒有:404，有:回傳訂單
//			if (togo.isEmpty()) {
//				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//			}
//			return ResponseEntity.ok(togo);
//		}
//		return ResponseEntity.ok(togoService.getAllTogo());
//	}
	
	@GetMapping("/togo/get")
	public String getTogoPage(Model model) {
		List<FoodKindEntity> foodKindList = foodKindService.getAllFoodKind();
		model.addAttribute("foodKindList", foodKindList);
		return "spantasty/order/getTogo";
	}
	
	@GetMapping("/togo/add")
	public String addTogoPage(Model model) {
		List<Restaurant> restaurants = restaurantService.findAllRestaurants();
		model.addAttribute("restaurants", restaurants);
		return "spantasty/order/addTogo";
	}
	
	@GetMapping("/togo")
	public String getAllTogo(
			Model model,
			@RequestParam(defaultValue = "0") Integer page,
			@RequestParam(required = false) Integer restaurantId) {
		if (restaurantId == null) {
			Page<TogoEntity> togoList = togoService.getAllTogoPage(page+1, null);
			DateTimeFormatter createTimeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			DateTimeFormatter pickupTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
			for (TogoEntity togo : togoList) {
				togo.setCreateTimeformattedDate(togo.getTogoCreateTime().format(createTimeformatter)); // 將日期格式化為字串
				if (togo.getTogoPickupTime() != null) {
					togo.setPickupTimeformattedDate(togo.getTogoPickupTime().format(pickupTimeFormatter));
				}
			}
			
			TogoDto togoDto = new TogoDto();
			togoDto.setTogoList(togoList);

			model.addAttribute("togoList", togoDto);
			return "spantasty/order/getAllTogo";
		} else {
			Page<TogoEntity> togoList = togoService.getTogoByRestaurantId(page+1, null, restaurantId);
			DateTimeFormatter createTimeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			DateTimeFormatter pickupTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
			for (TogoEntity togo : togoList) {
				togo.setCreateTimeformattedDate(togo.getTogoCreateTime().format(createTimeformatter)); // 將日期格式化為字串
				if (togo.getTogoPickupTime() != null) {
					togo.setPickupTimeformattedDate(togo.getTogoPickupTime().format(pickupTimeFormatter));
				}
			}
			TogoDto togoDto = new TogoDto();
			togoDto.setTogoList(togoList);
			togoDto.setSelectedId(restaurantId);
			
			model.addAttribute("togoList", togoDto);
			
		}
		return "spantasty/order/getAllTogo";
		
	}
	
	@GetMapping("/{memberId}/togo")
	@ResponseBody
	public ResponseEntity<List<TogoHistoryDto>> getTogoByMemberId(@PathVariable Integer memberId) {
		List<TogoEntity> togoList = togoService.getTogoByMemberId(memberId);
		List<TogoHistoryDto> togoHistoryDtoList = new ArrayList<>();
		for (TogoEntity item : togoList) {
			TogoHistoryDto togoHistoryDto = new TogoHistoryDto();
			togoHistoryDto.setTogoId(item.getTogoId());
			togoHistoryDto.setTogoCreateDate(item.getTogoCreateTime());
			togoHistoryDto.setFinalTotal(item.getFinalTotal());
			togoHistoryDto.setTogoStatus(item.getTogoStatus());
			togoHistoryDtoList.add(togoHistoryDto);
		}
		return ResponseEntity.ok(togoHistoryDtoList);
	}
	
	@GetMapping("/togo/{togoId}")
	@ResponseBody
	public ResponseEntity<TogoEntity> getTogoById(@PathVariable Integer togoId) {
		TogoEntity togo = togoService.getTogoById(togoId);
		DateTimeFormatter createTimeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		togo.setCreateTimeformattedDate(togo.getTogoCreateTime().format(createTimeformatter));
		if (togo.getTogoPickupTime() != null) {
			DateTimeFormatter pickupTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
			togo.setPickupTimeformattedDate(togo.getTogoPickupTime().format(pickupTimeFormatter));
			
		}
		return ResponseEntity.ok(togo);
	}
	
	@GetMapping("/togo/phone")
	@ResponseBody
	public ResponseEntity<List<TogoEntity>> getTogoByPhone(@RequestParam String tgPhone) {
		List<TogoEntity> togoList = togoService.getTogoByPhone(tgPhone);
		DateTimeFormatter createTimeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		DateTimeFormatter pickupTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
		for (TogoEntity item : togoList) {
			item.setCreateTimeformattedDate(item.getTogoCreateTime().format(createTimeformatter));
			if (item.getTogoPickupTime() != null) {
				item.setPickupTimeformattedDate(item.getTogoPickupTime().format(pickupTimeFormatter));
			}
		}
		return ResponseEntity.ok(togoList);
	}
	
	@GetMapping("/togo/restaurant/{restaurantId}")
	@ResponseBody
	public ResponseEntity<Page<TogoEntity>> getTogoByRestaurant(
			@PathVariable Integer restaurantId,
			@RequestParam(defaultValue = "0") Integer page) {
		Page<TogoEntity> togoList = togoService.getTogoByRestaurantId(page+1, null, restaurantId);
		DateTimeFormatter createTimeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		DateTimeFormatter pickupTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
		for (TogoEntity item : togoList) {
			item.setCreateTimeformattedDate(item.getTogoCreateTime().format(createTimeformatter));
			if (item.getTogoPickupTime() != null) {
				item.setPickupTimeformattedDate(item.getTogoPickupTime().format(pickupTimeFormatter));
			}
		}
		return ResponseEntity.ok(togoList);
	}
	
	@PostMapping("/togo")
	@ResponseBody
	public ResponseEntity<TogoEntity> addTogo(@RequestBody TogoEntity newTogo) {
		TogoEntity savedTogo = togoService.addTogo(newTogo);
		return ResponseEntity.ok(togoService.getTogoById(savedTogo.getTogoId()));
	}
	
	//刪除&更新
	@PutMapping("/togo/{togoId}")
	@ResponseBody
	public ResponseEntity<TogoEntity> updateTogoById(@PathVariable Integer togoId, @RequestBody TogoEntity updateTogo) {
		TogoEntity togo = togoService.getTogoById(togoId);
		if (togo == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		togoService.updateTogoById(togoId, updateTogo);
		return ResponseEntity.ok(togoService.getTogoById(togoId));
	}
	
	//產生取餐時間
	@GetMapping("/togo/{restaurantId}/pickup")
	@ResponseBody
	public ResponseEntity<List<String>> getPickupOptions(@PathVariable Integer restaurantId) {
        LocalTime closingTime = restaurantService.getClosingTimeById(restaurantId); // 獲取餐廳的結束營業時間
        DateTimeFormatter pickupTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        List<LocalTime> pickupTimes = pickupTime.generatePickupTimes(closingTime);
        List<String> formattedPickupTimes = new ArrayList<>();
        for (LocalTime times : pickupTimes) {
        	formattedPickupTimes.add(times.format(pickupTimeFormatter));
        }
        return ResponseEntity.ok(formattedPickupTimes); // 獲取取餐時間選項並返回
    }
	
}





