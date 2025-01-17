package com.eatspan.SpanTasty.controller.store;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.eatspan.SpanTasty.entity.store.Product;
import com.eatspan.SpanTasty.entity.store.ShoppingItem;
import com.eatspan.SpanTasty.entity.store.ShoppingItemId;
import com.eatspan.SpanTasty.entity.store.ShoppingOrder;
import com.eatspan.SpanTasty.service.store.ProductService;
import com.eatspan.SpanTasty.service.store.ShoppingItemService;
import com.eatspan.SpanTasty.service.store.ShoppingOrderService;

@Controller
@RequestMapping("/shoppingItem")
public class ShoppingItemController {

    @Autowired
    private ShoppingItemService shoppingItemService;
    
    @Autowired
    private ShoppingOrderService shoppingOrderService;
    
    @Autowired
    private ProductService productService;
    

    //查詢單筆
    @GetMapping("/itemDetail/{id}")
    public String toShoppingItem(@PathVariable Integer id, Model model) {
        ShoppingOrder shopping = shoppingOrderService.findShoppingOrderById(id);
        model.addAttribute("shopping", shopping);
        List<ShoppingItem> items = shoppingItemService.findShoppingItemById(id);
        model.addAttribute("items",items);
        List<Product> productList = productService.findAllProduct();
        model.addAttribute("productList",productList);
        Integer totalAmount = shoppingOrderService.calculateTotalAmount(id);
        model.addAttribute("totalAmount",totalAmount);
        System.out.println();
        return "spantasty/store/shopping/shoppingItemDetail";
    }
    
//    // 顯示訂單明細
//    @GetMapping("/itemDetail/{id}")
//    public String toShoppingItem(@RequestParam Integer shoppingId, Model model) {
////    	ShoppingOrder shoppingOrder = shoppingOrderService.findShoppingOrderById(shoppingId);
//        List<ShoppingItem> items = shoppingItemService.findShoppingItemById(shoppingId);
//        model.addAttribute("items", items);
//        model.addAttribute("shopping", shoppingId);
//        model.addAttribute("productList", productService.findAllProduct()); 
//        return "store/shopping/shoppingItemDetail"; 
//    }
    
    
//    @PostMapping("/itemDetailsPost")
//    public String showShoppingItem(@RequestParam Integer shoppingId,
//    								@RequestParam Integer productId,Model model) {
//    	shoppingItemService.findAllShoppingItems(shoppingId,productId);
//    	
//    	return ""
//    }
    

    // 新增商品項目
//    @PostMapping("/addItem")
//    public String addShoppingItem(@ModelAttribute ShoppingItem shoppingItem) {
//        shoppingItemService.addShoppingItem(shoppingItem);
//        return "redirect:/shoppingItem/details?shoppingId=" + shoppingItem.getId().getShoppingId();
//    }
    
    @PostMapping("/addItem")
    public String addShoppingItem(@RequestParam Integer shoppingId,
                                   @RequestParam Integer productId,
                                   @RequestParam Integer shoppingItemQuantity,
                                   Model model) {

        ShoppingItem newItem = new ShoppingItem();
        newItem.setId(new ShoppingItemId(shoppingId, productId));
        newItem.setShoppingItemQuantity(shoppingItemQuantity);

        Integer productPrice = shoppingItemService.getProductPriceById(productId);
        Integer totalPrice = productPrice != null ? productPrice * shoppingItemQuantity : 0;
        newItem.setShoppingItemPrice(totalPrice);
        
        ShoppingOrder shopping = shoppingOrderService.findShoppingOrderById(shoppingId);
        shopping.setShoppingTotal(shoppingOrderService.calculateTotalAmount(shoppingId));
        shoppingItemService.addShoppingItem(newItem);
        shoppingOrderService.updateShoppingOrder(shopping);

        return "redirect:/shoppingItem/itemDetail/" + shoppingId;
    }

    


    // 刪除商品項目
    @DeleteMapping("/delItem")
    public String deleteShoppingItem(@RequestParam Integer shoppingId, @RequestParam Integer productId) {
        ShoppingItemId shoppingItemId = new ShoppingItemId(shoppingId, productId);
        ShoppingOrder shopping = shoppingOrderService.findShoppingOrderById(shoppingId);
        shopping.setShoppingTotal(shoppingOrderService.calculateTotalAmount(shoppingId));
        shoppingItemService.deleteShoppingItem(shoppingItemId);
        shoppingOrderService.updateShoppingOrder(shopping);
        return "redirect:/shoppingItem/itemDetail/" + shoppingId;
    }

    // 刪除所有商品項目
    @DeleteMapping("/deleteAll")
    public String deleteAllShoppingItems(@RequestParam Integer shoppingId) {
        shoppingItemService.deleteAllShoppingItems(shoppingId);
        return "redirect:/shoppingItem/details?shoppingId=" + shoppingId;
    }
    
    // 更新商品項目
    @PutMapping("/updateItem")
    public String updateShoppingItem(@RequestParam Integer shoppingId,
                                      @RequestParam Integer productId,
                                      @RequestParam Integer shoppingItemQuantity,
                                      Model model) {
        ShoppingItemId shoppingItemId = new ShoppingItemId(shoppingId, productId);
        
        ShoppingItem existingItem = shoppingItemService.findShoppingItemById(shoppingItemId);
        
        if (existingItem != null) {
            existingItem.setShoppingItemQuantity(shoppingItemQuantity);
            
            Integer productPrice = shoppingItemService.getProductPriceById(productId);
            
            Integer totalPrice = productPrice != null ? productPrice * shoppingItemQuantity : 0;
            existingItem.setShoppingItemPrice(totalPrice);
            shoppingItemService.updateShoppingItem(existingItem);
            
            ShoppingOrder shopping = shoppingOrderService.findShoppingOrderById(shoppingId);
            shopping.setShoppingTotal(shoppingOrderService.calculateTotalAmount(shoppingId));
            shoppingOrderService.updateShoppingOrder(shopping);
        }
        
        return "redirect:/shoppingItem/itemDetail/" + shoppingId; 
    }

}