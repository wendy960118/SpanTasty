package com.eatspan.SpanTasty.controller.rental;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.eatspan.SpanTasty.dto.rental.RentDetailDTO;
import com.eatspan.SpanTasty.dto.rental.RentItemDTO;
import com.eatspan.SpanTasty.dto.rental.RentKeywordDTO;
import com.eatspan.SpanTasty.dto.rental.RentRequestDTO;
import com.eatspan.SpanTasty.dto.rental.StockKeywordDTO;
import com.eatspan.SpanTasty.dto.rental.TablewareFilterDTO;
import com.eatspan.SpanTasty.entity.account.Member;
import com.eatspan.SpanTasty.entity.rental.Rent;
import com.eatspan.SpanTasty.entity.rental.RentItem;
import com.eatspan.SpanTasty.entity.reservation.Restaurant;
import com.eatspan.SpanTasty.entity.rental.Tableware;
import com.eatspan.SpanTasty.entity.rental.TablewareStock;
import com.eatspan.SpanTasty.service.account.MemberService;
import com.eatspan.SpanTasty.service.rental.RentItemService;
import com.eatspan.SpanTasty.service.rental.RentService;
import com.eatspan.SpanTasty.service.rental.TablewareService;
import com.eatspan.SpanTasty.service.rental.TablewareStockService;
import com.eatspan.SpanTasty.service.reservation.RestaurantService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@Controller
@RequestMapping("/rent")
public class RentController {
	
	@Autowired
	private RentService rentService;
	@Autowired
	private TablewareService tablewareService;
	@Autowired
	private RentItemService rentItemService;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private MemberService memberService;
	@Autowired
	private TablewareStockService tablewareStockService;
	
	
	// 查詢下拉式選單
	@GetMapping("/add")
	public String toAddAndSearch(Model model) {
		List<Restaurant> restaurants = restaurantService.findAllRestaurants();
		List<Member> members = memberService.findAllMembers();
		model.addAttribute("restaurants" ,restaurants);
		model.addAttribute("members" ,members);
		// 過濾掉 tablewareStatus == 2 的餐具
		List<Tableware> tablewares = tablewareService.findAllTablewares()
		        .stream()
		        .filter(tableware -> tableware.getTablewareStatus() != 2)
		        .collect(Collectors.toList());
		
		model.addAttribute("tablewares" ,tablewares);
		return "spantasty/rental/addRent";
	}
	
	
	// 篩選掉庫存為0的用具
	@PostMapping("/filter")
	@ResponseBody
	public List<Tableware> getTablewaresByFilter(@RequestBody TablewareFilterDTO tablewareFilterDTO) {
	    List<Tableware> availableTablewares = tablewareService.findAllTablewares()
	        .stream()
	        // 這裡不再需要過濾 tablewareStatus，因為在第一次已經過濾過了
	        .filter(tableware -> {
	            TablewareStock stock = tablewareStockService.findStockById(tableware.getTablewareId(), tablewareFilterDTO.getRestaurantId());
	            return stock != null && stock.getStock() > 0;  // 只過濾庫存
	        })
	        .collect(Collectors.toList());
	    
	    return availableTablewares; // 返回 JSON 給前端 AJAX
	}
	
	
	// 檢查庫存
	@PostMapping("/check")
	@ResponseBody
	public ResponseEntity<Integer> getRentStock(@RequestBody StockKeywordDTO stockKeywordDTO) {
		Integer tablewareId = stockKeywordDTO.getTablewareId();
		Integer restaurantId = stockKeywordDTO.getRestaurantId();
	    TablewareStock tablewareStock = tablewareStockService.findStockById(tablewareId, restaurantId);
	    if (tablewareStock != null) {
	        Integer stock = tablewareStock.getStock();
	        return ResponseEntity.ok(stock);
	    } else {
	        return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 如果未找到，返回 404
	    }
	}
	
	
	// 新增訂單 訂單明細
	@PostMapping("/addPost")
	public String addRentAndRentItems(
			@ModelAttribute Rent rent,
			@RequestParam Map<String, String> allParams, 
			Model model) {
		Date rentDate = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(rentDate);
		calendar.add(Calendar.DAY_OF_YEAR, 7);
		Date dueDate = calendar.getTime();
		rent.setRentDate(rentDate);
		rent.setDueDate(dueDate);
		rent.setRentStatus(1);
		rent.setRentMemo("未歸還");
		rentService.addRent(rent);
		
		int rentId = rent.getRentId();
		// 保存 RentItem 資料
		List<String> tablewareIds = new ArrayList<>();
		List<String> rentItemQuantities = new ArrayList<>();
		List<String> rentItemDeposits = new ArrayList<>();
		
		for (Map.Entry<String, String> entry : allParams.entrySet()) {
	        if (entry.getKey().startsWith("tablewareId")) {
	            tablewareIds.add(entry.getValue());
	        } else if (entry.getKey().startsWith("rentItemQuantity")) {
	            rentItemQuantities.add(entry.getValue());
	        } else if (entry.getKey().startsWith("rentItemDeposit")) {
	            rentItemDeposits.add(entry.getValue());
	        }
	    }
		List<RentItem> rentItems = new ArrayList<>();
		
		for (int i = 0; i < tablewareIds.size(); i++) {
	        Integer tablewareId = Integer.parseInt(tablewareIds.get(i));
	        Integer rentItemQuantity = Integer.parseInt(rentItemQuantities.get(i));
	        Integer rentItemDeposit = Integer.parseInt(rentItemDeposits.get(i));
	        
	        RentItem rentItem = new RentItem();
	        rentItem.setRentId(rentId);
	        rentItem.setTablewareId(tablewareId);
	        rentItem.setRentItemQuantity(rentItemQuantity);
	        rentItem.setRentItemDeposit(rentItemDeposit);
	        rentItem.setReturnMemo("未歸還");
	        rentItem.setReturnStatus(1);
	        rentItems.add(rentItem);
	    }
		
		for (RentItem rentItem : rentItems) {
	        rentItemService.addRentItem(rentItem);
	        TablewareStock tablewareStock = tablewareStockService.findStockById(rentItem.getTablewareId(), rent.getRestaurantId());
	        Integer stock = tablewareStock.getStock();
	        stock -= rentItem.getRentItemQuantity();
	        tablewareStock.setStock(stock);
	        tablewareStockService.addStock(tablewareStock);
	    }
		return "redirect:/rent/getAll";
	}
	
	
	//刪除訂單 訂單明細
	@DeleteMapping("/del/{id}")
	public String deleteRentAndRentItems(@PathVariable("id") Integer rentId, Model model) {
		List<RentItem> rentItems = rentItemService.findRentItemsByRentId(rentId);
		Rent rent = rentService.findRentById(rentId);
		Integer restaurantId = rent.getRestaurantId();
		for(RentItem rentItem: rentItems) {
			Integer tablewareId = rentItem.getTablewareId();
			TablewareStock tablewareStock = tablewareStockService.findStockById(tablewareId, restaurantId);
			Integer stock = tablewareStock.getStock();
			stock += rentItem.getRentItemQuantity();
			tablewareStock.setStock(stock);
			tablewareStockService.addStock(tablewareStock);
			
			rentItemService.deleteRentItem(rentItem);
		}
		rentService.deleteRent(rentId);
		return "redirect:/rent/getAll";
	}
	

	//查詢訂單(By訂單編號)
	@GetMapping("/set/{id}")
	public String toSetRent(@PathVariable("id") Integer rentId, @RequestParam("action") String action, Model model) {
		Rent rent = rentService.findRentById(rentId);
		model.addAttribute("rent", rent);
		if ("update".equals(action)) {
			List<Restaurant> restaurants = restaurantService.findAllRestaurants();
			model.addAttribute("restaurants" ,restaurants);
			List<Member> members = memberService.findAllMembers();
			model.addAttribute("members" ,members);
			return "spantasty/rental/setRent";
			
		} else if ("return".equals(action)) {
			List<Restaurant> restaurants = restaurantService.findAllRestaurants()
					.stream()
	                .filter(restaurant -> restaurant.getRestaurantStatus() == 1)
	                .collect(Collectors.toList());
			model.addAttribute("restaurants" ,restaurants);
			
			Date returnDate = new Date();
			rent.setReturnDate(returnDate);
			model.addAttribute("rent", rent);
			
			
			List<RentItem> rentItems = rentItemService.findRentItemsByRentId(rentId);
			List<RentItemDTO> rentItemsDTO = rentItems.stream()
				    .map(rentItem -> new RentItemDTO(
				        rentItem.getRentId(),
				        rentItem.getTablewareId(),
				        rentItem.getRentItemQuantity(),
				        rentItem.getRentItemDeposit(),
				        rentItem.getReturnMemo(),
				        rentItem.getReturnStatus(),
				        rentItem.getTableware().getTablewareName(),
				        0, // Initialize returnedQuantity for DTO
				        0  // Initialize damagedQuantity for DTO
				    )).collect(Collectors.toList());
			model.addAttribute("rentItemsDTO", rentItemsDTO);
			
			return "spantasty/rental/setRentReturn2";
			
		}else if("get".equals(action)){
			List<RentItem> rentItems = rentItemService.findRentItemsByRentId(rentId);
			model.addAttribute("rentItems",rentItems);
			return "spantasty/rental/getRentAndItems";
		}
		return null;
	}
	
	
	//更改訂單
	@PutMapping("/setPut1")
	protected String updateRent(@ModelAttribute Rent rent, Model model) {
		rentService.addRent(rent);
		return "redirect:/rent/getAll";
	}
	
	
	//歸還訂單
	@PutMapping("/setPut2")
	public String returnRent(@ModelAttribute Rent rent, Model model) {
		rent.setRentStatus(2);
		rent.setRentMemo("已歸還");
		List<RentItem> rentItems = rent.getRentItems();

		
		for(RentItem rentItem: rentItems) {
			String returnMemo = rentItem.getReturnMemo();
			
			// 解析 returnMemo 得到歸還數量和破損數量
	        String[] memoParts = returnMemo.replace("租借", "").replace("歸還", ",").replace("破損", ",").split(",");
	        int rentItemQuantity = Integer.parseInt(memoParts[0]); // 總數量
	        int returnedQuantity = Integer.parseInt(memoParts[1]); // 歸還數量
	        int damagedQuantity = Integer.parseInt(memoParts[2]); // 破損數量
			
			rentItem.setReturnMemo(returnMemo);
			if (rentItemQuantity == returnedQuantity && damagedQuantity > 0) {
			    rentItem.setReturnStatus(3); //完全歸還(有破損)
			} else if (rentItemQuantity == returnedQuantity) {
			    rentItem.setReturnStatus(2); //完全歸還
			} else if (rentItemQuantity > returnedQuantity && damagedQuantity > 0) {
			    rentItem.setReturnStatus(5); //未完全歸還(有破損)
			} else if (rentItemQuantity > returnedQuantity) {
			    rentItem.setReturnStatus(4); //未完全歸還
			}
			rentItemService.addRentItem(rentItem);
			// 更新庫存
	        int stockChange = returnedQuantity - damagedQuantity;
	        TablewareStock tablewareStock = tablewareStockService.findStockById(rentItem.getTablewareId(), rent.getReturnRestaurantId());
	        Integer stock = tablewareStock.getStock();
	        Integer newStock = stock + stockChange;
	        tablewareStock.setStock(newStock);
	        tablewareStockService.addStock(tablewareStock);
		}
		rentService.addRent(rent);
		return "redirect:/rent/getAll";
	}

	
	//查詢所有訂單
	@GetMapping("getAll")
	public String getAllRents(Model model, @RequestParam(value = "p", defaultValue = "1") Integer page) {
		List<Restaurant> restaurants = restaurantService.findAllRestaurants();
		List<Member> members = memberService.findAllMembers();
		model.addAttribute("restaurants" ,restaurants);
		model.addAttribute("members" ,members);
		Page<Rent> rentPages = rentService.findAllRentPages(page);
		model.addAttribute("rentPages",rentPages);
		
		Map<Integer, String> restaurantMap = restaurants.stream()
		        .collect(Collectors.toMap(Restaurant::getRestaurantId, Restaurant::getRestaurantName));
	    Map<Integer, String> memberMap = members.stream()
	        .collect(Collectors.toMap(Member::getMemberId, Member::getMemberName));

	    // 使用分頁中的租借訂單進行 DTO 轉換
	    List<RentDetailDTO> rentDetails = rentPages.getContent().stream().map(rent -> {
	        RentDetailDTO dto = new RentDetailDTO();
	        dto.setRentId(rent.getRentId());
	        dto.setRentDeposit(rent.getRentDeposit());
	        dto.setRentDate(rent.getRentDate());
	        dto.setDueDate(rent.getDueDate());
	        dto.setReturnDate(rent.getReturnDate());
	        dto.setRentStatus(rent.getRentStatus());
	        dto.setRentMemo(rent.getRentMemo());
	        dto.setRestaurantId(rent.getRestaurantId());
	        dto.setMemberId(rent.getMemberId());
	        dto.setReturnRestaurantId(rent.getReturnRestaurantId());

	        // 從 Map 中查找對應的餐廳名稱和成員名稱
	        dto.setRestaurantName(restaurantMap.get(rent.getRestaurantId()));
	        dto.setMemberName(memberMap.get(rent.getMemberId()));
	        dto.setReturnRestaurantName(restaurantMap.get(rent.getReturnRestaurantId()));

	        
	        return dto;
		}).collect(Collectors.toList());
		
		model.addAttribute("rents" ,rentDetails);
		return "spantasty/rental/getAllRents";
	}
	
	
	//查詢訂單(By多個條件)
	@ResponseBody
	@PostMapping("/get")
	public ResponseEntity<List<RentDetailDTO>> getRents(@RequestBody RentKeywordDTO rentKeywordDTO) {
		try {
			List<Restaurant> restaurants = restaurantService.findAllRestaurants();
			List<Member> members = memberService.findAllMembers();
			
			Integer rentId = (rentKeywordDTO.getRentId() != null && rentKeywordDTO.getRentId() != 0) ? rentKeywordDTO.getRentId() : null;
	        Integer memberId = (rentKeywordDTO.getMemberId() != null && rentKeywordDTO.getMemberId() != 0) ? rentKeywordDTO.getMemberId() : null;
	        Integer restaurantId = (rentKeywordDTO.getRestaurantId() != null && rentKeywordDTO.getRestaurantId() != 0) ? rentKeywordDTO.getRestaurantId() : null;
	        Integer rentStatus = (rentKeywordDTO.getRentStatus() != null && rentKeywordDTO.getRentStatus() != 0) ? rentKeywordDTO.getRentStatus() : null;
	        Date rentDateStart = (rentKeywordDTO.getRentDateStart() != null && !rentKeywordDTO.getRentDateStart().trim().isEmpty()) ? new SimpleDateFormat("yyyy-MM-dd").parse(rentKeywordDTO.getRentDateStart()) : null;
	        Date rentDateEnd = (rentKeywordDTO.getRentDateEnd() != null && !rentKeywordDTO.getRentDateEnd().trim().isEmpty()) ? new SimpleDateFormat("yyyy-MM-dd").parse(rentKeywordDTO.getRentDateEnd()) : null;
	        
			List<Rent> rents = rentService.findRentsByCriteria(rentId, memberId, restaurantId, rentStatus, rentDateStart, rentDateEnd);
			
			List<RentDetailDTO> rentDetails = rents.stream().map(rent -> {
				RentDetailDTO dto = new RentDetailDTO();
				dto.setRentId(rent.getRentId());
		        dto.setRentDeposit(rent.getRentDeposit());
		        dto.setRentDate(rent.getRentDate());
		        dto.setDueDate(rent.getDueDate());
		        dto.setReturnDate(rent.getReturnDate());
		        dto.setRentStatus(rent.getRentStatus());
		        dto.setRentMemo(rent.getRentMemo());
		        dto.setRestaurantId(rent.getRestaurantId());
		        dto.setMemberId(rent.getMemberId());
		        dto.setReturnRestaurantId(rent.getReturnRestaurantId());
		        
		        restaurants.stream()
		            .filter(r -> r.getRestaurantId().equals(rent.getRestaurantId()))
		            .findFirst()
		            .ifPresent(r -> dto.setRestaurantName(r.getRestaurantName()));

	        // 查找對應的成員名稱
		        members.stream()
		            .filter(m -> m.getMemberId().equals(rent.getMemberId()))
		            .findFirst()
		            .ifPresent(m -> dto.setMemberName(m.getMemberName()));
		        
		        restaurants.stream()
			        .filter(r -> r.getRestaurantId().equals(rent.getReturnRestaurantId()))
			        .findFirst()
			        .ifPresent(r -> dto.setReturnRestaurantName(r.getRestaurantName()));
		        
		        return dto;
			}).collect(Collectors.toList());
			
			return ResponseEntity.ok(rentDetails);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}
	
	
	//歸還訂單
	@PostMapping("/setPut3")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> returnRent2(@RequestBody RentRequestDTO rentRequestDTO, Model model) {
		Rent rent = rentRequestDTO.getRent();
		List<RentItemDTO> rentItemsDTO = rentRequestDTO.getRentItemsDTO();
		rent.setRentStatus(2);
		rent.setRentMemo("已歸還");
		
		int totalExpense = 0;
		
		List<RentItemDTO> damagedItems = new ArrayList<>();
	    List<RentItemDTO> unreturnedItems = new ArrayList<>();
		
		for(RentItemDTO rentItemDTO : rentItemsDTO) {
			Integer rentItemQuantity = rentItemDTO.getRentItemQuantity();
			Integer returnedQuantity = rentItemDTO.getReturnedQuantity();
			Integer damagedQuantity = rentItemDTO.getDamagedQuantity();
			Integer rentItemDeposit = rentItemDTO.getRentItemDeposit();
			
			Integer tablewareDeposit = tablewareService.findTablewareDeposit(rentItemDTO.getTablewareId());
			int unreturnedCount = rentItemQuantity - returnedQuantity;
	        int extraCharge = (unreturnedCount * tablewareDeposit * 2) + (damagedQuantity * tablewareDeposit * 2);
	        totalExpense += extraCharge;
			
	        // 將未歸還和損壞情況分開記錄
	        if (damagedQuantity > 0) {
	            RentItemDTO damagedItem = new RentItemDTO();
	            damagedItem.setRentItemQuantity(rentItemDTO.getRentItemQuantity());
	            damagedItem.setRentItemDeposit(rentItemDTO.getRentItemDeposit());
	            damagedItem.setTablewareName(rentItemDTO.getTablewareName());
	            damagedItem.setReturnedQuantity(returnedQuantity);
	            damagedItem.setDamagedQuantity(damagedQuantity);
	            damagedItems.add(damagedItem);
	        }

	        if (unreturnedCount > 0) {
	            RentItemDTO unreturnedItem = new RentItemDTO();
	            unreturnedItem.setTablewareName(rentItemDTO.getTablewareName());
	            unreturnedItem.setRentItemQuantity(rentItemDTO.getRentItemQuantity());
	            unreturnedItem.setReturnedQuantity(returnedQuantity);
	            unreturnedItem.setDamagedQuantity(0); // 不記錄損壞
	            unreturnedItem.setRentItemDeposit(rentItemDTO.getRentItemDeposit());
	            unreturnedItems.add(unreturnedItem);
	        }
	        
			Integer returnStatus = null;
			if (rentItemQuantity == returnedQuantity && damagedQuantity > 0) {
				returnStatus = 3; //完全歸還(有破損)
			} else if (rentItemQuantity == returnedQuantity) {
				returnStatus = 2; //完全歸還
			} else if (rentItemQuantity > returnedQuantity && damagedQuantity > 0) {
				returnStatus = 5; //未完全歸還(有破損)
			} else if (rentItemQuantity > returnedQuantity) {
				returnStatus = 4; //未完全歸還
			}
			
			// 組合 returnMemo
	        String returnMemo = "歸還" + returnedQuantity + "破損" + damagedQuantity;
	        
			// 更新庫存
	        int stockChange = returnedQuantity - damagedQuantity;
	        TablewareStock tablewareStock = tablewareStockService.findStockById(rentItemDTO.getTablewareId(), rent.getReturnRestaurantId());
	        Integer stock = tablewareStock.getStock();
	        Integer newStock = stock + stockChange;
	        tablewareStock.setStock(newStock);
	        tablewareStockService.addStock(tablewareStock);
	        
	        RentItem rentItem = new RentItem();
	        rentItem.setRentId(rentItemDTO.getRentId());
	        rentItem.setTablewareId(rentItemDTO.getTablewareId());
	        rentItem.setReturnMemo(returnMemo);
	        rentItem.setReturnStatus(returnStatus);
	        rentItem.setRentItemDeposit(rentItemDeposit);
	        rentItem.setRentItemQuantity(rentItemQuantity);
	        rentItemService.addRentItem(rentItem);
		}
		rentService.addRent(rent);
		System.out.println(unreturnedItems);
		System.out.println(damagedItems);
		Map<String, Object> response = new HashMap<>();
	    response.put("totalExpense", totalExpense);
	    response.put("damagedItems", damagedItems);
	    response.put("unreturnedItems", unreturnedItems);
	    
		
		return ResponseEntity.ok(response);
	}
	
	
	//
	@GetMapping("/expense")
	public String rentSummary(@RequestParam("totalExpense") Integer totalExpense,
	                          @RequestParam("damagedItems") String damagedItemsJson,
	                          @RequestParam("unreturnedItems") String unreturnedItemsJson,
	                          Model model) throws JsonMappingException, JsonProcessingException {
	    
	    ObjectMapper mapper = new ObjectMapper();
	    List<RentItemDTO> damagedItems = mapper.readValue(damagedItemsJson, new TypeReference<List<RentItemDTO>>(){});
	    List<RentItemDTO> unreturnedItems = mapper.readValue(unreturnedItemsJson, new TypeReference<List<RentItemDTO>>(){});
	    
	    model.addAttribute("totalExpense", totalExpense);
	    model.addAttribute("damagedItems", damagedItems);
	    model.addAttribute("unreturnedItems", unreturnedItems);
	    return "spantasty/rental/getReturnExpense";
	}
}
