package com.eatspan.SpanTasty.controller.reservation;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.eatspan.SpanTasty.dto.reservation.RestaurantDTO;
import com.eatspan.SpanTasty.entity.reservation.Restaurant;
import com.eatspan.SpanTasty.service.rental.TablewareStockService;
import com.eatspan.SpanTasty.service.reservation.RestaurantService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;


@Controller
@RequestMapping("/restaurant")
public class RestaurantController {
	
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private TablewareStockService tablewareStockService;
	
	
    @GetMapping("/getAll")
    public String getAllRestaurants(Model model, @RequestParam(defaultValue = "0") Integer page) {
        Page<Restaurant> restaurantsPage = restaurantService.findAllRestaurantsPage(page+1, null);
        model.addAttribute("restaurantsPage", restaurantsPage);
        return "spantasty/reservation/getAllRestaurant";
    }
    
    
    // ajax 查詢餐廳
    @GetMapping("/getapi/{id}")
    @ResponseBody
    public ResponseEntity<?> getRestaurant(@PathVariable(name = "id") Integer restaurantId) {
    	Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
    	return ResponseEntity.ok(restaurant);
    }
    
    
	
    // 查詢餐廳(byId)
    @GetMapping("/get/{id}")
    public String getRestaurantById(@PathVariable("id") Integer restaurantId, Model model) {
        Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
        model.addAttribute("restaurant", restaurant);
        return "spantasty/reservation/getRestaurant";
    }
    
    
    
    // 把頁面導向addRestaurant
    @GetMapping("/add")
    public String toAddRestaurant() {
        return "spantasty/reservation/addRestaurant";
    }
    
    
	
    // 新增餐廳
    @PostMapping("/addPost")
    public String addRestaurant(@ModelAttribute Restaurant addRestaurant, 
                                @RequestParam("rimg") MultipartFile file) throws IllegalStateException, IOException {
        
    	Restaurant restaurant = restaurantService.uploadFile(addRestaurant, file);
        restaurantService.addRestaurant(restaurant);
        // 增加餐廳時 增加餐廳用具預設庫存量
         tablewareStockService.addStockByRestaurantId(restaurant.getRestaurantId());
        return "redirect:/restaurant/getAll";
    }
    
    
    
    
    
    // 把頁面導向setRestaurant
    @GetMapping("/set/{id}")
    public String toSetRestaurant(@PathVariable("id") Integer restaurantId, Model model) {
        Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
        model.addAttribute("restaurant", restaurant);
        return "spantasty/reservation/setRestaurant";
    }
    
    
    
    // 修改餐廳
    @PutMapping("/setPut")
    public String updateRestaurant(@ModelAttribute Restaurant setRestaurant, 
    							   @RequestParam("rimg") MultipartFile file) throws IllegalStateException, IOException {
    	
    	Restaurant restaurant = restaurantService.uploadFile(setRestaurant, file);
        restaurantService.updateRestaurant(restaurant);
        return "redirect:/restaurant/getAll";
    }
    
    
    
    
    
    // 刪除餐廳
    @DeleteMapping("/del/{id}")
    public ResponseEntity<?> deleteRestaurant(@PathVariable("id") Integer restaurantId) {
        restaurantService.deleteRestaurant(restaurantId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    
    // 修改餐廳訂位規則
    @PutMapping("/set")
    public String updateRestaurantReserve(@ModelAttribute Restaurant restaurant){
        restaurantService.updateRestaurant(restaurant);
        Integer restaurantId = restaurant.getRestaurantId();
        return "redirect:/table/getAll?restaurantId="+restaurantId;
    }

	
    // 取得所有縣市
    @GetMapping("/city")
    public ResponseEntity<List<String>> getAllCities() {
        List<String> cities = restaurantService.getAllCities();
        return ResponseEntity.ok(cities);
    }

    // 根據縣市取得鄉鎮
    @GetMapping("/{city}/towns")
    public ResponseEntity<List<String>> getTownsByCity(@PathVariable String city) {
        List<String> towns = restaurantService.getTownsByCity(city);
        return ResponseEntity.ok(towns);
    }
    
    // 根據鄉鎮取得餐廳
    @GetMapping("/{town}/list")
    public ResponseEntity<List<RestaurantDTO>> getRestaurantsByTown(@PathVariable String town) {
        List<RestaurantDTO> restaurants = restaurantService.getRestaurantsByTown(town);
        return ResponseEntity.ok(restaurants);
    }
	
	
}
