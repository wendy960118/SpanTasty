package com.eatspan.SpanTasty.service.store;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.eatspan.SpanTasty.entity.store.Product;
import com.eatspan.SpanTasty.entity.store.ShoppingItem;
import com.eatspan.SpanTasty.entity.store.ShoppingItemId;
import com.eatspan.SpanTasty.entity.store.ShoppingOrder;
import com.eatspan.SpanTasty.repository.store.ProductRepository;
import com.eatspan.SpanTasty.repository.store.ShoppingItemRepository;
import com.eatspan.SpanTasty.repository.store.ShoppingOrderRepository;


@Service
public class ShoppingItemService {
	@Autowired
	private ShoppingItemRepository shoppingItemRepo;
	
	@Autowired
	private ShoppingOrderRepository shoppingOrderRepo;
	
    @Autowired
    @Lazy
    private ShoppingOrderService shoppingOrderService;
    
	@Autowired
	private ProductRepository productRepo;
	
//    @Autowired
//    public ShoppingItemService(ShoppingOrderService shoppingOrderService) {
//        this.shoppingOrderService = shoppingOrderService;
	
	
	public Integer getProductPriceById(Integer productId) {
	    Optional<Product> productOpt = productRepo.findById(productId);
	    return productOpt.map(Product::getProductPrice).orElse(null); 
	}

	
	public ShoppingItem addShoppingItem(ShoppingItem shoppingItem) {
		return shoppingItemRepo.save(shoppingItem);
	}
	
	public void addShoppingItemToExistingOrder(Integer shoppingId, Integer productId, Integer shoppingItemQuantity) {
	    // 實作將商品添加到已有訂單的邏輯
	    ShoppingItem newItem = new ShoppingItem();
	    newItem.setId(new ShoppingItemId(shoppingId, productId));
	    newItem.setShoppingItemQuantity(shoppingItemQuantity);

	    Integer productPrice = getProductPriceById(productId);
	    Integer totalPrice = productPrice != null ? productPrice * shoppingItemQuantity : 0;
	    newItem.setShoppingItemPrice(totalPrice);
	    
	    addShoppingItem(newItem);
	    ShoppingOrder shoppingOrder = shoppingOrderService.findShoppingOrderById(shoppingId);
	    shoppingOrder.setShoppingTotal(shoppingOrderService.calculateTotalAmount(shoppingId));
	    shoppingOrderService.updateShoppingOrder(shoppingOrder);
	}

	
//	public ShoppingItem addShoppingItem(ShoppingItem shoppingItem) {
//	    // 獲取產品
//		 Integer productId = shoppingItem.getId().getProductId(); // 從 ShoppingItemId 獲取 productId
//		 Product product = productRepo.findById(productId).orElse(null);
//	    if (product != null) {
//	        shoppingItem.setProduct(product);
//	    }
//
//	    // 獲取產品價格
//	    Integer productPrice = product != null ? product.getProductPrice() : null;
//	    Integer totalPrice = productPrice != null ? productPrice * shoppingItem.getShoppingItemQuantity() : 0;
//	    shoppingItem.setShoppingItemPrice(totalPrice);
//
//	    // 確保購物訂單已設置
//	    ShoppingOrder shoppingOrder = shoppingItem.getShoppingOrder();
//	    if (shoppingOrder != null && shoppingOrder.getShoppingId() != null) {
//	        shoppingOrder = shoppingOrderRepo.findById(shoppingOrder.getShoppingId()).orElse(null);
//	        if (shoppingOrder != null) {
//	            shoppingItem.setShoppingOrder(shoppingOrder);
//	        }
//	    }
//
//	    // 先查詢是否已存在的購物項目
//	    ShoppingItem existingItem = shoppingItemRepo.findById(shoppingItem.getId()).orElse(null);
//	    if (existingItem != null) {
//	        existingItem.setShoppingItemQuantity(existingItem.getShoppingItemQuantity() + shoppingItem.getShoppingItemQuantity());
//	        return shoppingItemRepo.save(existingItem);
//	    }
//
//	    // 保存購物項目
//	    ShoppingItem savedShoppingItem = shoppingItemRepo.save(shoppingItem);
//
//	    // 更新購物訂單的總額
//	    if (shoppingOrder != null) {
//	        shoppingOrder.setShoppingTotal(shoppingOrder.getShoppingTotal() + totalPrice);
//	        shoppingOrderRepo.save(shoppingOrder); // 保存更新後的 ShoppingOrder
//	    }
//
//	    return savedShoppingItem;
//	}

	
	
//	public ShoppingItem addShoppingItem(ShoppingItem shoppingItem) {
//	    Integer productPrice = getProductPriceById(shoppingItem.getId().getProductId());
//	    
//	    Integer totalPrice = productPrice != null ? productPrice * shoppingItem.getShoppingItemQuantity() : 0;
//	    shoppingItem.setShoppingItemPrice(totalPrice);
//
//	    Product product = productRepo.findById(shoppingItem.getId().getProductId()).orElse(null);
//	    shoppingItem.setProduct(product);
//	    
//	    
//	    ShoppingItem savedShoppingItem = shoppingItemRepo.save(shoppingItem);
//
//	    ShoppingOrder shoppingOrder = shoppingItem.getShoppingOrder(); // 假設已經設置了 shoppingOrder
//	    if (shoppingOrder != null) {
//	        shoppingOrder.setShoppingTotal(shoppingOrder.getShoppingTotal() + totalPrice);
//	        shoppingOrderRepo.save(shoppingOrder); // 保存更新後的 ShoppingOrder
//	    }
//
//	    return savedShoppingItem;
//	}
	
//	public ShoppingItem addShoppingItem(ShoppingItemId shoppingItemId, Integer shoppingItemQuantity, Integer ShoppingItemPrice, Product product, ShoppingOrder shoppingOrder) {
//	    ShoppingItem shoppingItem = new ShoppingItem();
//	    
//	    // 設置複合主键
//	    shoppingItem.setId(shoppingItemId);
//	    shoppingItem.setShoppingItemQuantity(shoppingItemQuantity);
//	    shoppingItem.setShoppingItemPrice(ShoppingItemPrice);
//	    
//	    // 設置與產品和訂單的關聯
//	    shoppingItem.setProduct(product);
//	    shoppingItem.setShoppingOrder(shoppingOrder);
//	    
//	    return shoppingItemRepo.save(shoppingItem); 
//	}

	public void deleteShoppingItem(ShoppingItemId shoppingItemId) {
        shoppingItemRepo.deleteById(shoppingItemId);
    }

	public boolean deleteAllShoppingItems(int shoppingId) {
        try {
            shoppingItemRepo.deleteAllByShoppingOrderId(shoppingId);
            return true;
        } catch (Exception e) {
            return false; 
        }
    }
	
    public ShoppingItem updateShoppingItem(ShoppingItem shoppingItem) {
        Optional<ShoppingItem> optional = shoppingItemRepo.findById(shoppingItem.getId());
        if (optional.isPresent()) {
            return shoppingItemRepo.save(shoppingItem);
        }
        return null;
    }
	
//    public ShoppingItem updateShoppingItem(ShoppingItemId shoppingItemId, Integer shoppingItemQuantity, Integer shoppingItemprice) {
//        Optional<ShoppingItem> optional = shoppingItemRepo.findById(shoppingItemId);
//        if (optional.isPresent()) {
//            ShoppingItem shoppingItem = optional.get();
//            if (shoppingItemQuantity != null) {
//                shoppingItem.setShoppingItemQuantity(shoppingItemQuantity);
//            }
//            if (shoppingItemprice != null) {
//                shoppingItem.setShoppingItemPrice(shoppingItemprice);
//            }
//            return shoppingItemRepo.save(shoppingItem);
//        }
//        return null;
//    }

    
    public List<ShoppingItem> findShoppingItemById(Integer shoppingId) {
        return shoppingItemRepo.findByShoppingOrderShoppingId(shoppingId);
    }
    
    public ShoppingItem findShoppingItemById(ShoppingItemId shoppingItemId) {
        Optional<ShoppingItem> optional = shoppingItemRepo.findById(shoppingItemId);
        return optional.orElse(null); // 如果找不到則返回 null
    }
//    public ShoppingItem findShoppingItemById(ShoppingItemId shoppingItemId) {
//        Optional<ShoppingItem> optional = shoppingItemRepo.findById(shoppingItemId);
//        
//        if(optional.isPresent()) {
//        	return optional.get();
//        }
//        return null;
//    }
    
//    public ShoppingItem findShoppingItemById(ShoppingItemId shoppingItemId) {
//        Optional<ShoppingItem> optional = shoppingItemRepo.findById(shoppingItemId);
//        
//        if(optional.isPresent()) {
//        	return optional.get();
//        }
//        return null;
//    }

    public List<ShoppingItem> findAllShoppingItems() {
        return shoppingItemRepo.findAll();
    }
    
	
}
