package com.eatspan.SpanTasty.controller.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eatspan.SpanTasty.dto.reservation.ReserveCheckDTO;
import com.eatspan.SpanTasty.dto.reservation.ReserveDTO;
import com.eatspan.SpanTasty.entity.account.Member;
import com.eatspan.SpanTasty.entity.reservation.Reserve;
import com.eatspan.SpanTasty.entity.reservation.Restaurant;
import com.eatspan.SpanTasty.entity.reservation.RestaurantTable;
import com.eatspan.SpanTasty.entity.reservation.RestaurantTableId;
import com.eatspan.SpanTasty.entity.reservation.TableType;
import com.eatspan.SpanTasty.service.account.MemberService;
import com.eatspan.SpanTasty.service.reservation.ReserveService;
import com.eatspan.SpanTasty.service.reservation.RestaurantService;
import com.eatspan.SpanTasty.service.reservation.RestaurantTableService;
import com.eatspan.SpanTasty.service.reservation.TableTypeService;

@Controller
@RequestMapping("/StarCups")
public class StarCupsController {
	
	
	@Autowired
	private ReserveService reserveService;
	@Autowired
	private RestaurantTableService restaurantTableService;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private TableTypeService tableTypeService;
	@Autowired
	private MemberService memberService;
	
	
	
	// 導向到訂位頁面
    @GetMapping("/reserve")
    public String showReserve(Model model) {
        List<Restaurant> restaurants = restaurantService.findAllRestaurants();
        model.addAttribute("restaurants", restaurants);
        return "starcups/reservation/reservePage";
    }
    
    // 導向到所有餐廳頁面
    @GetMapping("/restaurant")
    public String showAllRestaurant(Model model, @RequestParam(defaultValue = "0") Integer page) {
    	Page<Restaurant> restaurantsPage = restaurantService.findAllRestaurantsPage(page+1, 4);
    	model.addAttribute("restaurantsPage", restaurantsPage);
    	return "starcups/reservation/allRestaurantPage";
    }
	
    
    @GetMapping("/restaurant/{id}")
    public String getRestaurant(Model model, @PathVariable(name = "id") Integer restaurantId) {
        Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
        model.addAttribute("restaurant", restaurant);
        return "starcups/reservation/restaurantPage";
    }
    
    
    
    @PostMapping("/reserve/add")
    public ResponseEntity<String> addReserve(@RequestBody ReserveDTO reserveDTO) {
        
        try {
            Reserve reserve = new Reserve();
            
            System.out.println(reserveDTO.getCheckDate());
            System.out.println(reserveDTO.getStartTime());
            
            reserve.setReserveSeat(reserveDTO.getReserveSeat());
            reserve.setReserveTime(reserveDTO.getCheckDate().atTime(reserveDTO.getStartTime()));
            
            // token取得memberId
            // 設定member外鍵關聯
            Member member = memberService.findMemberById(reserveDTO.getMemberId()).orElse(null);
            if (member == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Member not found");
            
            // 設定restaurant外鍵關聯
            Restaurant restaurant = restaurantService.findRestaurantById(reserveDTO.getRestaurantId());
            if (restaurant == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Restaurant not found");
            reserve.setRestaurant(restaurant);
            
            // 根據reserveSeat取得訂位桌子種類
            String tableTypeId = reserveService.getTableTypeIdByReserveSeat(reserve.getReserveSeat());
            // 設定tableType外鍵關聯
            TableType tableType = tableTypeService.findTableTypeById(tableTypeId);
            if (tableType == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TableType not found");
            reserve.setTableType(tableType);
            
            // 保存訂位
            reserveService.addReserve(reserve);
            
            // 回傳成功訊息和狀態碼201 (Created)
            return ResponseEntity.status(HttpStatus.CREATED).body("Reserve added successfully");

        } catch (Exception e) {
            // 捕捉到例外情況時，回傳錯誤訊息和狀態碼500 (Internal Server Error)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    
    
    
	
	
}