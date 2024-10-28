package com.eatspan.SpanTasty.controller.reservation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.eatspan.SpanTasty.dto.reservation.ReserveDTO;
import com.eatspan.SpanTasty.entity.account.Member;
import com.eatspan.SpanTasty.entity.reservation.Reserve;
import com.eatspan.SpanTasty.entity.reservation.Restaurant;
import com.eatspan.SpanTasty.service.account.MemberService;
import com.eatspan.SpanTasty.service.reservation.ReserveService;
import com.eatspan.SpanTasty.service.reservation.RestaurantService;
import com.eatspan.SpanTasty.utils.account.JwtUtil;

@Controller
@RequestMapping("/StarCups")
public class StarCupsReservationController {
	
	
	@Autowired
	private ReserveService reserveService;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private MemberService memberService;
	
	
	
	
	// 導向到訂位頁面
    @GetMapping("/reserve")
    public String showReserve(Model model, @RequestParam(required = false) Integer restaurantId) {
    	
        List<Restaurant> restaurants = restaurantService.findAllRestaurants()
                .stream()
                .filter(restaurant -> restaurant.getRestaurantStatus() == 1)
                .collect(Collectors.toList());
        
        Integer reserveMin = 2;
        Integer reserveMax = 10;
        if(restaurantId!=null) {
        	Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
        	reserveMin = restaurant.getReserveMin();
        	reserveMax = restaurant.getReserveMax();
        }
        
        model.addAttribute("selectedRestaurantId", restaurantId==null? "":restaurantId);
        model.addAttribute("reserveMin", reserveMin);
        model.addAttribute("reserveMax", reserveMax);
        model.addAttribute("restaurants", restaurants);
        return "starcups/reservation/reservePage";
    }
    
    // 導向到所有餐廳頁面(營業中)
    @GetMapping("/restaurant")
    public String showAllRestaurant(Model model, @RequestParam(defaultValue = "0") Integer page) {
    	Page<Restaurant> restaurantsPage = restaurantService.findAllActiveRestaurantsPage(page+1, 4);
    	model.addAttribute("restaurantsPage", restaurantsPage);
    	return "starcups/reservation/allRestaurantPage";
    }
    
    // ajax查詢所有餐廳(營業中)
    @GetMapping("/api/restaurants")
    public ResponseEntity<?> getAllRestaurantApi(Model model, @RequestParam(defaultValue = "0") Integer page) {
    	
        List<Restaurant> restaurants = restaurantService.findAllRestaurants()
                .stream()
                .filter(restaurant -> restaurant.getRestaurantStatus() == 1)
                .collect(Collectors.toList());
    	
    	return ResponseEntity.ok(restaurants);
    }
	
    // 導向到單一餐廳頁面
    @GetMapping("/restaurant/{id}")
    public String getRestaurant(Model model, @PathVariable(name = "id") Integer restaurantId) {
        Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
        model.addAttribute("restaurant", restaurant);
        return "starcups/reservation/restaurantPage";
    }
    
    // ajax查詢單一餐廳
    @GetMapping("/api/restaurant/{id}")
    public ResponseEntity<?> getRestaurantApi(@PathVariable(name = "id") Integer restaurantId) {
    	Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
    	return ResponseEntity.ok(restaurant);
    }
    
    
    
    
    
    // 導向到訂位紀錄頁面
    @GetMapping("/reserveRecord")
    public String reserveRecord() {
    	return "starcups/reservation/reserveRecordPage";
    }
    
    // 導向到訂位紀錄頁面
    @GetMapping("/get/reserveRecord")
    public ResponseEntity<?> getReserveRecord(@RequestHeader("Authorization") String token) {
    	
	    // 解析 JWT token 取得 claims
	    Map<String, Object> claims = JwtUtil.parseToken(token);

	    // 取得會員 ID
	    Integer memberId = (Integer) claims.get("memberId");
	    if (memberId == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("無法從 Token 中取得會員 ID");
	    
	    Member member = memberService.findMemberById(memberId).orElse(null);
        if (member == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Member not found");
        
        List<Reserve> reserves = reserveService.findByMember(member);
    	
    	return ResponseEntity.ok(reserves);
    }
    
    
    
    @PostMapping("/reserve/add")
    public ResponseEntity<String> addReserve(@RequestHeader("Authorization") String token,
    										 @RequestBody ReserveDTO reserveDTO) {
        try {
        	
    	    // 解析 JWT token 取得 claims 再取得會員 ID
    	    Map<String, Object> claims = JwtUtil.parseToken(token);
    	    Integer memberId = (Integer) claims.get("memberId");
    	    reserveDTO.setMemberId(memberId);
    	    
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
        	
            // 寄訂位成功信
            reserveService.sendMail(reserve);
            
            // 回傳成功訊息和狀態碼201 (Created)
            return ResponseEntity.status(HttpStatus.CREATED).body("Reserve added successfully");

        } catch (Exception e) {
            // 捕捉到例外情況時，回傳錯誤訊息和狀態碼500 (Internal Server Error)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    
    
    
	
	
}
