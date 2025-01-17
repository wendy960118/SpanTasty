package com.eatspan.SpanTasty.controller.order;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.eatspan.SpanTasty.dto.order.AddMenuDto;
import com.eatspan.SpanTasty.entity.order.FoodKindEntity;
import com.eatspan.SpanTasty.entity.order.MenuEntity;
import com.eatspan.SpanTasty.service.order.FoodKindService;
import com.eatspan.SpanTasty.service.order.MenuService;

@Controller
@RequestMapping("/order")
@PropertySource("upload.properties")
public class MenuController {
	
	@Autowired
	private MenuService menuService;
	
	@Autowired
	private FoodKindService foodKindService;
	
	@Value("${upload.orderPath}")
	private String uploadPath;
	
//	// 有參數：模糊查詢，無參數：查詢全部
//	@GetMapping("/menu")
//	public ResponseEntity<List<MenuEntity>> getFoods(@RequestParam(required = false) String foodName){
//		if (foodName != null) {
//			List<MenuEntity> foods = menuService.getFoodsByFoodName(foodName);
//			if (foods.isEmpty()) {
////				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT");
//				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//			}
//			return ResponseEntity.ok(foods); 
//		}
//		return ResponseEntity.ok(menuService.getAllFoods());
//	}
	
	@GetMapping("/menu/get")
	public String getMenuPage(Model model) {
		List<FoodKindEntity> foodKinds = foodKindService.getAllFoodKind();
		model.addAttribute("foodKinds", foodKinds);
		return "spantasty/order/getMenu";
	}
	
	@GetMapping("/menu/add")
	public String addMenuPage(Model model) {
		List<FoodKindEntity> foodKinds = foodKindService.getAllFoodKind();
		model.addAttribute("foodKinds", foodKinds);
		return "spantasty/order/addMenu";
	}
	
	@GetMapping("/menu")
	public String getAllMenu(Model model) {
		List<MenuEntity> foodList = menuService.getAllFoods();
		List<FoodKindEntity> foodKindList = foodKindService.getAllFoodKind();
		model.addAttribute("foodList", foodList);
		model.addAttribute("foodKindList", foodKindList);
		return "spantasty/order/getAllMenu";
	}
	
//	@GetMapping("/menu")
//	public ResponseEntity<List<MenuEntity>> getAllFoods() {
//		return ResponseEntity.ok(menuService.getAllFoods());
//	}
	
	@GetMapping("/menu/name")
	@ResponseBody
	public ResponseEntity<List<MenuEntity>> getFoodsByFoodName(@RequestParam String foodName) {
		return ResponseEntity.ok(menuService.getFoodsByFoodName(foodName));
	}
	
	@GetMapping("/menu/{foodId}")
	@ResponseBody
	public ResponseEntity<MenuEntity> getFoodById(@PathVariable Integer foodId) {
		return ResponseEntity.ok(menuService.getMenuById(foodId));
	}
	
	@GetMapping("/menu/kinds/{foodKindId}")
	@ResponseBody
	public ResponseEntity<List<MenuEntity>> getFoodsByKind(@PathVariable Integer foodKindId) {
		return ResponseEntity.ok(menuService.getFoodsByKind(foodKindId));
	}
	
	@GetMapping("/menu/{foodStatus}/kinds/{foodKindId}")
	@ResponseBody
	public ResponseEntity<List<MenuEntity>> getFoodsByKindAndStatus(@PathVariable Integer foodKindId, @PathVariable Integer foodStatus) {
		return ResponseEntity.ok(menuService.getFoodsByFoodKindIdAndFoodStatus(foodKindId, 1));
	}
	
	@GetMapping("/menu/kinds/all")
	@ResponseBody
	public ResponseEntity<List<MenuEntity>> getAllFoods() {
	    return ResponseEntity.ok(menuService.getAllFoods());
	}
	
	@PostMapping("/menu")
	@ResponseBody
	public ResponseEntity<List<MenuEntity>> addFood(@ModelAttribute AddMenuDto newFood)
			throws IllegalStateException, IOException {
		MenuEntity menu = new MenuEntity();
		menu.setFoodName(newFood.getFoodName());
		menu.setFoodPrice(newFood.getFoodPrice());
		menu.setFoodKindId(newFood.getFoodKindId());
		menu.setFoodStock(newFood.getFoodStock());
		menu.setFoodDescription(newFood.getFoodDescription());
		menu.setFoodStatus(newFood.getFoodStatus());
		
		// 建立資料夾
        File fileSaveDirectory = new File(uploadPath);
        if (!fileSaveDirectory.exists()) {
            fileSaveDirectory.mkdirs();
        }
        MultipartFile file = newFood.getFoodPicture();
        if (file != null && !file.isEmpty()) {
        	String fileName = file.getOriginalFilename();
        	String filePath = uploadPath + File.separator + fileName;
        	File fileToSave = new File(filePath);
        	file.transferTo(fileToSave);
        	menu.setFoodPicture("/SpanTasty/upload/order/" + fileName);
        }
		menuService.addFood(menu);
		return ResponseEntity.ok(menuService.getAllFoods());
	}
	
	@DeleteMapping("/menu/{foodId}")
	@ResponseBody
	public ResponseEntity<List<MenuEntity>> deleteFoodById(@PathVariable Integer foodId) {
		menuService.deleteFoodById(foodId);
		return ResponseEntity.ok(menuService.getAllFoods());
	}
	
	@PutMapping("/menu/{foodId}")
	@ResponseBody
	public ResponseEntity<MenuEntity> updateFoodById(
			@PathVariable Integer foodId,
			@ModelAttribute AddMenuDto updateFood)
					throws IllegalStateException, IOException {
		MenuEntity menu = new MenuEntity();
		menu.setFoodName(updateFood.getFoodName());
		menu.setFoodPrice(updateFood.getFoodPrice());
		menu.setFoodKindId(updateFood.getFoodKindId());
		menu.setFoodStock(updateFood.getFoodStock());
		menu.setFoodDescription(updateFood.getFoodDescription());
		menu.setFoodStatus(updateFood.getFoodStatus());
		
		// 建立資料夾
        File fileSaveDirectory = new File(uploadPath);
        if (!fileSaveDirectory.exists()) {
            fileSaveDirectory.mkdirs();
        }
        MultipartFile file = updateFood.getFoodPicture();
        if (file != null && !file.isEmpty()) {
        	String fileName = file.getOriginalFilename();
        	String filePath = uploadPath + File.separator + fileName;
        	File fileToSave = new File(filePath);
        	file.transferTo(fileToSave);
        	menu.setFoodPicture("/SpanTasty/upload/order/" + fileName);
        }
        menuService.updateFoodById(foodId, menu);
		return ResponseEntity.ok(menuService.getMenuById(foodId));
	}
	
}





