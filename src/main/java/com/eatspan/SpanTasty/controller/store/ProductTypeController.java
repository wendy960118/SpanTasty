package com.eatspan.SpanTasty.controller.store;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.eatspan.SpanTasty.entity.store.ProductType;
import com.eatspan.SpanTasty.service.store.ProductTypeService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/productType")
public class ProductTypeController {
	
	@Autowired
	private ProductTypeService productTypeService;
	
//	@PostMapping("/add")
//	public ResponseEntity<?> addProductType(@RequestBody ProductType addProductType){
//		ProductType productType = productTypeService.addProductType(addProductType);
//		if(productType != null) {
//			return new ResponseEntity<>(productType, HttpStatus.CREATED);
//		}
//		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//	}
	
	@PostMapping("/add")
	public String addProductType(@RequestParam String productTypeName,Model model) {
		ProductType productType = productTypeService.addProductType(productTypeName);
		List<ProductType> productTypes = productTypeService.findAllProductType();
		model.addAttribute("productTypes",productTypes);
		return "spantasty/store/product/searchAllProductType";
	}
	
//	@DeleteMapping("/del/{id}")
//	public ResponseEntity<?> deleteProductType(@PathVariable("id") Integer productTypeId){
//		if(productTypeService.findProductTypeById(productTypeId)==null) {
//			return new ResponseEntity<>(HttpStatus.NOT_FOUND); //不存在，返回404
//		}
//		productTypeService.deleteProductType(productTypeId);
//		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//	}
	
	@DeleteMapping("/del/{id}")
	public String deleteProductType(@PathVariable("id") Integer productTypeId) {
		productTypeService.deleteProductType(productTypeId);
		return "redirect:/productType/findAll";
	}
	
	
	@PutMapping("/update/{id}")
	public String updateProductType(@PathVariable("id") Integer productTypeId,
									@RequestParam String productTypeName,Model model) {
		productTypeService.updateProductType(productTypeId, productTypeName);
		return "redirect:/productType/findAll";
	}
	
//	@PutMapping("/update")
//	public ResponseEntity<?> updateProductType(@RequestBody ProductType updateProductType){
//		ProductType productType = productTypeService.updateProductType(updateProductType);
//		if(productType != null) {
//			return new ResponseEntity<>(productType, HttpStatus.OK);
//		}
//		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//	}
	
	@GetMapping("/find/{id}")
	public ResponseEntity<?> findProductTypeById(@PathVariable("id") Integer productTypeId){
		ProductType productType = productTypeService.findProductTypeById(productTypeId);
		if(productType != null) {
			return new ResponseEntity<>(productType, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND); 
	}
	
//	@GetMapping("/store/findAllProduct")
//	public String findAllProduct(Model model) {
//		List<Product> products = productService.findAllProduct();
//		model.addAttribute("products", products);
//		return "store/product/findAllProductPage";
//	}
	
	
	@GetMapping("/findAll")
	public String finaAllProductType(Model model){
		List<ProductType> productTypes = productTypeService.findAllProductType();
		model.addAttribute("productTypes",productTypes);
		return "spantasty/store/product/searchAllProductType";
	}
}
