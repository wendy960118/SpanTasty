package com.eatspan.SpanTasty.controller.reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;

import com.eatspan.SpanTasty.dto.reservation.ExportReserveDTO;
import com.eatspan.SpanTasty.dto.reservation.ReserveCheckDTO;
import com.eatspan.SpanTasty.dto.reservation.ReserveDTO;
import com.eatspan.SpanTasty.entity.reservation.Reserve;
import com.eatspan.SpanTasty.entity.reservation.Restaurant;
import com.eatspan.SpanTasty.entity.reservation.TableType;
import com.eatspan.SpanTasty.service.reservation.ReserveService;
import com.eatspan.SpanTasty.service.reservation.RestaurantService;
import com.eatspan.SpanTasty.service.reservation.TableTypeService;

@Controller
@RequestMapping("/reserve")
public class ReserveController {
	
	
	@Autowired
	private ReserveService reserveService;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private TableTypeService tableTypeService;
	
	
	
	// 導向到查詢訂單頁面
    @GetMapping("/getAll")
    public String showReserve(Model model) {
        List<TableType> tableTypes = tableTypeService.findAllTableType();
        List<Restaurant> restaurants = restaurantService.findAllRestaurants();
        model.addAttribute("tableTypes", tableTypes);
        model.addAttribute("restaurants", restaurants);
        return "spantasty/reservation/getAllReserve";
    }
	
    // 導向到訂位統計頁面
    @GetMapping("/reserveStatic")
    public String showReserveStatic(Model model) {
    	List<Restaurant> restaurants = restaurantService.findAllRestaurants();
    	model.addAttribute("restaurants", restaurants);
    	return "spantasty/reservation/reserveStatic";
    }
    
    
    // ajax 查詢訂位訂單
    @GetMapping("/getList")
    public ResponseEntity<?> getReserveList(@RequestParam(required = false) String memberName,
								            @RequestParam(required = false) String phone,
								            @RequestParam(required = false) Integer restaurantId,
								            @RequestParam(required = false) String tableTypeId,
								            @RequestParam(required = false) LocalDate reserveTimeStart,
								            @RequestParam(required = false) LocalDate reserveTimeEnd) {
    	
    	List<Reserve> reserveByCriteria = reserveService.findReserveByCriteria(memberName, phone, restaurantId, tableTypeId, reserveTimeStart, reserveTimeEnd);
    	return ResponseEntity.ok(reserveByCriteria);
    }
    
    
    // 匯出 JSON 訂位訂單
    @GetMapping("/export")
    public ResponseEntity<?> exportReserveList(@RequestParam(required = false) String memberName,
                                                 @RequestParam(required = false) String phone,
                                                 @RequestParam(required = false) Integer restaurantId,
                                                 @RequestParam(required = false) String tableTypeId,
                                                 @RequestParam(required = false) LocalDate reserveTimeStart,
                                                 @RequestParam(required = false) LocalDate reserveTimeEnd) {
        
        List<Reserve> reserveByCriteria = reserveService.findReserveByCriteria(memberName, phone, restaurantId, tableTypeId, reserveTimeStart, reserveTimeEnd);
        
        List<ExportReserveDTO> exportReserveDTOs = reserveByCriteria.stream()
                .map(ExportReserveDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(exportReserveDTOs);
    }

    // ajax查詢可訂位時段
    @GetMapping("/getReserveCheck")
    public ResponseEntity<?> getReserveCheck(@RequestParam Integer restaurantId,
									    	 @RequestParam Integer reserveSeat,
									    	 @RequestParam LocalDate checkDate) {
    	
    	Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
    	Integer reserveCombinable = restaurant.getReserveCombinable();
    	
    	if(reserveCombinable==1) {
    		// 單一桌位可預訂時間
    		String tableTypeId = reserveService.getTableTypeIdByReserveSeat(reserveSeat);
    		List<ReserveCheckDTO> reserveCheck = reserveService.getReserveCheck(restaurantId, tableTypeId, checkDate);
    		return ResponseEntity.ok(reserveCheck);
    	}else if (reserveCombinable==2) {
    		// 組合桌位可預訂時間
    		List<ReserveCheckDTO> combinedReserveCheck = reserveService.getCombinedReserveCheck(restaurantId, reserveSeat, checkDate);
    		return ResponseEntity.ok(combinedReserveCheck);
		}
    	
    	return ResponseEntity.status(HttpStatus.NOT_FOUND).body("收尋可訂位時間時出現錯誤");
    }
    
    
    // 刪除訂位Ajax
	@DeleteMapping("/del")
	public ResponseEntity<?> delReserve(@RequestParam Integer reserveId) {
		reserveService.deleteReserve(reserveId);
		return ResponseEntity.ok("delete ok");
	}
    
	
	// 新增訂位的ajax
	@PostMapping("/add")
	public ResponseEntity<?> addReserve(@RequestBody ReserveDTO reserveDTO) {
		String validateResult = reserveService.validateAddReserveDto(reserveDTO);
		if(validateResult!=null) {
			return ResponseEntity.badRequest().body(validateResult);
		}
		
	    // 新增訂位紀錄(單一桌位訂位)
	    Reserve reserve = reserveService.addReserve(reserveDTO);
	    
	    // 新增訂位紀錄(組合桌位訂位)
	    if(reserve==null && restaurantService.findRestaurantById(reserveDTO.getRestaurantId()).getReserveCombinable()==2) {
	    	//新增組合桌位的訂位訂單
	    	reserve = reserveService.addCombinedReserve(reserveDTO);
	    }
	    
	    if(reserve==null) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("無法找到足夠的桌位以完成訂單");
	    }
	    
		return ResponseEntity.ok("reserve add success");
	}
	
	
	
    // ajax 查詢訂位訂單
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getReserve(@PathVariable Integer id) {
    	Reserve reserve = reserveService.findReserveById(id);
    	return ResponseEntity.ok(reserve);
    }
	
	// 修改訂位的ajax
	@PutMapping("/set")
	public ResponseEntity<?> updateReserve(@RequestBody ReserveDTO reserveDTO) {
		
	
		String validateResult = reserveService.validateSetReserveDto(reserveDTO);
		if(validateResult!=null) {
			return ResponseEntity.badRequest().body(validateResult);
		}
		
		// 修改訂位紀錄(單一桌位訂位)
	    Reserve reserve = reserveService.updateReservebyDto(reserveDTO);
		
	    // 修改訂位紀錄(組合桌位訂位)
	    if(reserve==null && restaurantService.findRestaurantById(reserveDTO.getRestaurantId()).getReserveCombinable()==2) {
	    	//修改組合桌位的訂位訂單
	    	reserve = reserveService.updateCombinedReservebyDto(reserveDTO);
	    }
	    
	    if(reserve==null) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("無法找到足夠的桌位以完成訂單");
	    }
		
	    return ResponseEntity.ok("Reserve update ok");
	}
	
	// 修改訂位狀態的ajax
	@PutMapping("/setStatus")
	public ResponseEntity<?> updateReserveStatus(@RequestBody Reserve reserveStatus){
		Reserve reserve = reserveService.findReserveById(reserveStatus.getReserveId());
		reserve.setReserveStatus(reserveStatus.getReserveStatus());
		reserveService.updateReserveByEntity(reserve);
		return ResponseEntity.ok("ReserveStatus update ok");
	}
	
	
	
	
    // ajax 查詢所有餐廳特定時間內訂位訂單數量
    @GetMapping("/getReserveSum")
    public ResponseEntity<?> countReserveSum(@RequestParam(required = false) LocalDate slotEndDate,
    								   	   @RequestParam(required = false) LocalDate slotStartDate) {
    	Map<String, Integer> reserveSum = reserveService.getReserveSum(slotStartDate, slotEndDate);
    	return ResponseEntity.ok(reserveSum);
    }
    
    
    // ajax 查詢所有訂位訂單數量byMonth
    @GetMapping("/getReserveMonth")
    public ResponseEntity<?> countReserveMonth(@RequestParam(required = false) Integer year) {
    	List<Integer> reserveSum = reserveService.getReserveInMonth(year);
    	return ResponseEntity.ok(reserveSum);
    }
    
    
    // ajax 查詢所有訂位訂單數量bySeat
    @GetMapping("/getReserveSeat")
    public ResponseEntity<?> countReserveBySeat(@RequestParam Integer restaurantId) {
    	Map<String, Integer> reserveBySeat = reserveService.getReserveBySeat(restaurantId);
    	return ResponseEntity.ok(reserveBySeat);
    }
    
    
    // ajax 查詢所有訂位訂單數量byWeekDay
    @GetMapping("/getReserveWeekDay")
    public ResponseEntity<?> countReserveByWeekDay(@RequestParam Integer restaurantId) {
    	Map<String, Integer> reserveBySeat = reserveService.getReserveByWeekDay(restaurantId);
    	return ResponseEntity.ok(reserveBySeat);
    }
    
    
	
	
}
