package com.eatspan.SpanTasty.controller.store;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eatspan.SpanTasty.dto.discount.PointMemberDTO;
import com.eatspan.SpanTasty.entity.account.Member;
import com.eatspan.SpanTasty.entity.store.Product;
import com.eatspan.SpanTasty.entity.store.ProductType;
import com.eatspan.SpanTasty.entity.store.ShoppingItem;
import com.eatspan.SpanTasty.entity.store.ShoppingItemId;
import com.eatspan.SpanTasty.entity.store.ShoppingOrder;
import com.eatspan.SpanTasty.service.account.MemberService;
import com.eatspan.SpanTasty.service.discount.PointService;
import com.eatspan.SpanTasty.service.store.ProductService;
import com.eatspan.SpanTasty.service.store.ProductTypeService;
import com.eatspan.SpanTasty.service.store.ShoppingItemService;
import com.eatspan.SpanTasty.service.store.ShoppingOrderService;
import com.eatspan.SpanTasty.utils.account.JwtUtil;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/StarCups")
public class StarCupsStoreController {

	@Autowired
	private ShoppingItemService shoppingItemService;

	@Autowired
	private ShoppingOrderService shoppingOrderService;

	@Autowired
	private ProductService productService;

	@Autowired
	private ProductTypeService productTypeService;
	
	@Autowired
	private MemberService memberService;
	
	@Autowired
	private HttpSession session;
	
	//點數
	@Autowired
	private PointService pointService;

	
	@GetMapping("/allProduct")
	public String findAllProduct(@RequestHeader(value = "Authorization", required = false) String token,
	        @RequestParam(value = "page", defaultValue = "1") int page, Model model) {

	    List<ProductType> productTypes = productTypeService.findAllProductType();
	    model.addAttribute("productTypes", productTypes);

	    List<Product> products = productService.findProductsByPage(page, 12);
	    model.addAttribute("products", products);

	    int totalProducts = productService.countAllProducts();
	    int totalPages = (int) Math.ceil((double) totalProducts / 12);
	    model.addAttribute("totalPages", totalPages);
	    model.addAttribute("currentPage", page);

	    model.addAttribute("totalProducts", totalProducts);


	    for (ProductType productType : productTypes) {
	        int count = productType.getProducts().size(); 
	        model.addAttribute("productCount_" + productType.getProductTypeId(), count);
	    }

	    return "starcups/store/allProduct";
	}

	@GetMapping("/cartCount")
	public ResponseEntity<Integer> getCartCount(HttpSession session) {
	    Integer shoppingId = (Integer) session.getAttribute("shoppingId");
	    if (shoppingId == null) {
	        return ResponseEntity.ok(0);
	    }

	    List<ShoppingItem> items = shoppingItemService.findShoppingItemById(shoppingId);
	    System.out.println("Items: " + items); // 打印所有 ShoppingItem
	    int totalCount = items.stream().mapToInt(ShoppingItem::getShoppingItemQuantity).sum();
	    System.out.println("Total Count: " + totalCount); // 打印計算的總數量
	    return ResponseEntity.ok(totalCount);
	}


	
	
	

	@PostMapping("/addPost")
	@ResponseBody
	public ResponseEntity<?> addOrder(@RequestHeader(value = "Authorization") String token,
	                                   @RequestParam Integer productId,
	                                   @RequestParam Integer shoppingItemQuantity) {
	    try {
	        // 解析 JWT token 取得 claims
	        Map<String, Object> claims = JwtUtil.parseToken(token);
	        Integer memberId = (Integer) claims.get("memberId"); // 獲取會員 ID

	        // 確認會員是否有狀態為3的訂單
	        ShoppingOrder existingOrder = shoppingOrderService.findOrderByMemberIdAndStatus(memberId, 3);
	        if (existingOrder == null) {
	            // 沒有狀態為3的訂單，則新建訂單
	            ShoppingOrder order = shoppingOrderService.addShoppingOrder(memberId, productId, shoppingItemQuantity);
	            session.setAttribute("shoppingId", order.getShoppingId());
	            return ResponseEntity.status(HttpStatus.OK).body(order);
	        } else {
	            // 有狀態為3的訂單，將商品明細加入該訂單
	            shoppingItemService.addShoppingItemToExistingOrder(existingOrder.getShoppingId(), productId, shoppingItemQuantity);
	            // 將該訂單的 shoppingId 放入 session
	            session.setAttribute("shoppingId", existingOrder.getShoppingId());
	            return ResponseEntity.status(HttpStatus.OK).body(existingOrder);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("發生錯誤：" + e.getMessage());
	    }
	}


	
//	@PostMapping("/addPost")
//	@ResponseBody
//	public ResponseEntity<?> addOrder(@RequestHeader(value = "Authorization") String token,
//	                                   @RequestParam Integer productId,
//	                                   @RequestParam Integer shoppingItemQuantity) {
//	    try {
//	        // 解析 JWT token 取得 claims
//	        Map<String, Object> claims = JwtUtil.parseToken(token);
//	        Integer memberId = (Integer) claims.get("memberId"); // 獲取會員 ID
//
//	        Integer shoppingId = (Integer) session.getAttribute("shoppingId");
//	        ShoppingOrder order;
//	        System.out.println("shoppingId:"+shoppingId+1);
//	        if (shoppingId == null) {
//	            order = shoppingOrderService.addShoppingOrder(memberId, productId, shoppingItemQuantity);
//	            session.setAttribute("shoppingId", order.getShoppingId()); 
//	        } else {
//	            shoppingItemService.addShoppingItemToExistingOrder(shoppingId, productId, shoppingItemQuantity);
//	            order = shoppingOrderService.findShoppingOrderById(shoppingId); 
//	        }
//	        
//	        return ResponseEntity.status(HttpStatus.OK).body(order);
//	    } catch (Exception e) {
//	        // 日誌記錄錯誤
//	        e.printStackTrace();
//	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("發生錯誤：" + e.getMessage());
//	    }
//	}

	@PostMapping("/cartDetail")
	public String toShoppingItem( @RequestParam String token, Model model) {
	    try {
	        // 解析 JWT token 取得 claims
	    	System.out.println(token);
	    	
	        Map<String, Object> claims = JwtUtil.parseToken(token);
	        Integer memberId = (Integer) claims.get("memberId"); // 獲取會員 ID
	        // 確認會員是否有狀態為3的訂單
	        ShoppingOrder existingOrder = shoppingOrderService.findOrderByMemberIdAndStatus(memberId, 3);
	        if (existingOrder == null) {
	            // 沒有狀態為3的訂單，則返回錯誤頁面
	            return "starcups/store/page505"; // 錯誤頁面
	        } else {
	            // 有狀態為3的訂單，獲取該訂單的明細
	            List<ShoppingItem> items = shoppingItemService.findShoppingItemById(existingOrder.getShoppingId());
	            Integer totalAmount = shoppingOrderService.calculateTotalAmount(existingOrder.getShoppingId());

	            model.addAttribute("shopping", existingOrder);
	            model.addAttribute("items", items);
	            model.addAttribute("totalAmount", totalAmount);
	            
	            return "starcups/store/cartPage"; // 返回訂單明細頁面
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return "starcups/store/page505"; // 出現錯誤時返回錯誤頁面
	    }
	}

	

//	@PostMapping("/cartDetail")
//	public String toShoppingItem(Model model) {
//	    Integer shoppingId = (Integer) session.getAttribute("shoppingId");
//
//	    if (shoppingId == null) {
//	        return "starcups/store/page505"; // 如果沒有訂單，顯示錯誤頁面
//	    }
//
//	    ShoppingOrder shopping = shoppingOrderService.findShoppingOrderById(shoppingId);
//	    model.addAttribute("shopping", shopping);
//	    List<ShoppingItem> items = shoppingItemService.findShoppingItemById(shoppingId);
//	    model.addAttribute("items", items);
//	    List<Product> productList = productService.findAllProduct();
//	    model.addAttribute("productList", productList);
//	    Integer totalAmount = shoppingOrderService.calculateTotalAmount(shoppingId);
//	    model.addAttribute("totalAmount", totalAmount);
//	    
//	    return "starcups/store/cartPage"; // 返回購物車頁面
//	}

	
	@PostMapping("/updateItem")
	@ResponseBody
	public ResponseEntity<?> updateShoppingItem(
			@RequestHeader(value = "Authorization") String token,
			@RequestParam Integer shoppingId,
			@RequestParam Integer productId,
			@RequestParam Integer shoppingItemQuantity) {
		
		try {
			// 解析 JWT token 取得 claims
			Map<String, Object> claims = JwtUtil.parseToken(token);
			Integer memberId = (Integer) claims.get("memberId"); 
			System.out.println("updateItem=HIHI");
			ShoppingItemId shoppingItemId = new ShoppingItemId(shoppingId, productId);
			ShoppingItem existingItem = shoppingItemService.findShoppingItemById(shoppingItemId);
			System.out.println("update");
			if (existingItem != null) {
				existingItem.setShoppingItemQuantity(shoppingItemQuantity);
				
				Integer productPrice = shoppingItemService.getProductPriceById(productId);
				Integer totalPrice = productPrice != null ? productPrice * shoppingItemQuantity : 0;
				existingItem.setShoppingItemPrice(totalPrice);
				shoppingItemService.updateShoppingItem(existingItem);
				
				ShoppingOrder shopping = shoppingOrderService.findShoppingOrderById(shoppingId);
				shopping.setShoppingTotal(shoppingOrderService.calculateTotalAmount(shoppingId));
				shoppingOrderService.updateShoppingOrder(shopping);
				
				return ResponseEntity.status(HttpStatus.OK).body(existingItem);
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("購物項目未找到");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("發生錯誤：" + e.getMessage());
		}
	}

	
	@GetMapping("/productsByProductType/{productTypeId}")
	public String searchProductsByProductType(@PathVariable Integer productTypeId,
	        @RequestParam(value = "page", defaultValue = "1") int page, Model model) {

	    // 獲取所有商品類型
	    List<ProductType> productTypes = productTypeService.findAllProductType();
	    model.addAttribute("productTypes", productTypes);

	    // 獲取指定類型的商品
	    List<Product> allProducts = productService.findProductsByProductType(productTypeId);

	    // 過濾狀態為 1 的商品
	    List<Product> availableProducts = allProducts.stream()
	            .filter(product -> product.getProductStatus() == 1)
	            .collect(Collectors.toList());

	    // 總商品數（狀態為 1）
	    int totalProducts = availableProducts.size();
	    
	    // 計算總頁數
	    int totalPages = (int) Math.ceil((double) totalProducts / 12);
	    
	    // 獲取當前頁面的商品
	    int fromIndex = (page - 1) * 12;
	    int toIndex = Math.min(fromIndex + 12, totalProducts);
	    List<Product> products = availableProducts.subList(fromIndex, toIndex);

	    model.addAttribute("totalPages", totalPages);
	    model.addAttribute("currentPage", page);
	    model.addAttribute("products", products);
	    model.addAttribute("totalProducts", totalProducts); // 更新顯示的商品數量

	    System.out.println("ProductTypeId: " + productTypeId);
	    
	    return "starcups/store/allProduct"; 
	}
	
	
	@PostMapping("/checkOut")
	public String checkOut(Model model) {
	    Integer shoppingId = (Integer) session.getAttribute("shoppingId");
	    
	    if (shoppingId == null) {
	        // 假設有一個方法可以獲取當前用戶的 memberId
	        Integer memberId = (Integer) session.getAttribute("memberId");
	        
	        // 根據 memberId 和商品狀態為 3 獲取訂單
	        ShoppingOrder shopping = shoppingOrderService.findOrderByMemberIdAndStatus(memberId, 3);
	        
	    }
	    
	    // 繼續原有的邏輯
	    ShoppingOrder shopping = shoppingOrderService.findShoppingOrderById(shoppingId);
	    model.addAttribute("shopping", shopping);
	    
	    List<ShoppingItem> items = shoppingItemService.findShoppingItemById(shoppingId);
	    model.addAttribute("items", items);
	    
	    List<Product> productList = productService.findAllProduct();
	    model.addAttribute("productList", productList);
	    
	    Integer totalAmount = shoppingOrderService.calculateTotalAmount(shoppingId);
	    model.addAttribute("totalAmount", totalAmount);
	    
	    Member members = memberService.findMemberByShoppingId(shoppingId);
	    model.addAttribute("members", members);
	    
	    // 點數 
	    Integer memberId = shopping.getMemberId();
	    PointMemberDTO pointMember = pointService.getPointMember(memberId);
	    if (pointMember == null) {
	        pointMember = new PointMemberDTO();
	    }    
	    model.addAttribute("pointMember", pointMember);
	    
	    Integer pointMemberTotalPoint = pointService.getPointMemberExpiryPoint(memberId);
	    model.addAttribute("pointMemberTotalPoint", pointMemberTotalPoint);
	    
	    return "starcups/store/checkOut";
	}



//	@PostMapping("/checkOut")
//	public String checkOut(Model model) {
//		Integer shoppingId = (Integer) session.getAttribute("shoppingId");
//		ShoppingOrder shopping = shoppingOrderService.findShoppingOrderById(shoppingId);
//		
//		model.addAttribute("shopping", shopping);
//		List<ShoppingItem> items = shoppingItemService.findShoppingItemById(shoppingId);
//		model.addAttribute("items", items);
//		List<Product> productList = productService.findAllProduct();
//		model.addAttribute("productList", productList);
//		Integer totalAmount = shoppingOrderService.calculateTotalAmount(shoppingId);
//		model.addAttribute("totalAmount", totalAmount);
//		Member members = memberService.findMemberByShoppingId(shoppingId);
//		model.addAttribute("members",members);
//		//點數 
//		Integer memberId = shopping.getMemberId();
//		PointMemberDTO pointMember = pointService.getPointMember(memberId);
//		if(pointMember==null) {
//			pointMember=new PointMemberDTO();
//		}	
//		model.addAttribute("pointMember",pointMember);
//		Integer pointMemberTotalPoint = pointService.getPointMemberExpiryPoint(memberId);
//		model.addAttribute("pointMemberTotalPoint",pointMemberTotalPoint);
//		return "starcups/store/checkOut";
//	}

	
	
	@GetMapping("/findItem")
	@ResponseBody
	public List<ShoppingItem> findItemAjax(@RequestParam Integer shoppingId) {
		return shoppingItemService.findShoppingItemById(shoppingId);
	}
	

    
	@PostMapping("/ecpayCheckout")
	@ResponseBody
	public String ecpayCheckout(Model model) {
	    Integer shoppingId = (Integer) session.getAttribute("shoppingId");
	    ShoppingOrder shopping = shoppingOrderService.findShoppingOrderById(shoppingId);
	    if (shopping.getShoppingStatus() == 2) {
	        return "已完成結帳，無法重複結帳";
	    }
	    
	    String aioCheckOutALLForm = shoppingOrderService.ecpayCheckout(shoppingId);

	    model.addAttribute("aioCheckOutALLForm", aioCheckOutALLForm);
	    
	    
	    return aioCheckOutALLForm;
	}

	@PostMapping("/orderConfirmP")
		public String checkOutFinishP(Model model) {
		
		Integer shoppingId = (Integer) session.getAttribute("shoppingId");
		ShoppingOrder shopping = shoppingOrderService.findShoppingOrderById(shoppingId);
		
		
		System.out.println("shoppingId "+shoppingId );
		
		model.addAttribute("shopping", shopping);
		List<ShoppingItem> items = shoppingItemService.findShoppingItemById(shoppingId);
		model.addAttribute("items", items);
		List<Product> productList = productService.findAllProduct();
		model.addAttribute("productList", productList);
		Integer totalAmount = shoppingOrderService.calculateTotalAmount(shoppingId);
		model.addAttribute("totalAmount", totalAmount);
	    Integer discountAmount = shopping.getDiscountAmount();
	    if (discountAmount == null) {
	        discountAmount = 0;
	    }
	    model.addAttribute("discountAmount", discountAmount);
		
	    shopping.setShoppingStatus(1);
	    shoppingOrderService.updateShoppingOrder(shopping);
	    
	    for (ShoppingItem item : items) {
	        Product product = item.getProduct();
	        if (product != null && product.getProductStock() > 0) {
	            product.setProductStock(product.getProductStock() - item.getShoppingItemQuantity());
	            productService.updateProduct(product); 
	        }
	    }
	    
	    
        try {
        	shoppingOrderService.sendMail(shopping);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
	    
	    session.removeAttribute("shoppingId");
	    
		return "starcups/store/OrderConfirm";
	}
	
	
	@GetMapping("/orderConfirm")
	public String checkOutFinish(Model model) {
	    Integer shoppingId = (Integer) session.getAttribute("shoppingId");
	    ShoppingOrder shopping = shoppingOrderService.findShoppingOrderById(shoppingId);
	    
	    System.out.println("shoppingId " + shoppingId);
	    
	    model.addAttribute("shopping", shopping);
	    List<ShoppingItem> items = shoppingItemService.findShoppingItemById(shoppingId);
	    model.addAttribute("items", items);
	    List<Product> productList = productService.findAllProduct();
	    model.addAttribute("productList", productList);
	    Integer totalAmount = shoppingOrderService.calculateTotalAmount(shoppingId);
	    model.addAttribute("totalAmount", totalAmount);
	    
	    Integer discountAmount = shopping.getDiscountAmount();
	    if (discountAmount == null) {
	        discountAmount = 0;
	    }
	    model.addAttribute("discountAmount", discountAmount);
	    
	    shopping.setShoppingStatus(1);
	    shoppingOrderService.updateShoppingOrder(shopping);
	    
	    for (ShoppingItem item : items) {
	        Product product = item.getProduct();
	        if (product != null && product.getProductStock() > 0) {
	            product.setProductStock(product.getProductStock() - item.getShoppingItemQuantity());
	            productService.updateProduct(product); 
	        }
	    }
	    
	    // 發送郵件
	    try {
	        shoppingOrderService.sendMail(shopping);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    
	    session.removeAttribute("shoppingId");
	    
	    return "starcups/store/OrderConfirm";
	}

	
	
	
	@GetMapping("/orderHistory")
	public String orderHistory (Model model) {
	    return "starcups/store/OrderHistory";
	}

    @GetMapping("/orderHistoryTurn")
    public ResponseEntity<?> orderHistoryTurn(@RequestHeader("Authorization") String token) {
    	
	    // 解析 JWT token 取得 claims
	    Map<String, Object> claims = JwtUtil.parseToken(token);

	    // 取得會員 ID
	    Integer memberId = (Integer) claims.get("memberId");
	    System.out.println(memberId);
	    if (memberId == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("無法從 Token 中取得會員 ID");
	    
	    Member member = memberService.findMemberById(memberId).orElse(null);
        if (member == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Member not found");
        
	    List<ShoppingOrder> shoppings = shoppingOrderService. findOrdersByMemberId(memberId);
    	
    	return ResponseEntity.ok(shoppings);
    }
    

    @GetMapping("/orderDetail/{id}")
    public ResponseEntity<?> orderDetail(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        // 解析 JWT token 取得 claims
        Map<String, Object> claims = JwtUtil.parseToken(token);
        Integer memberId = (Integer) claims.get("memberId");

        List<ShoppingItem> items = shoppingItemService.findShoppingItemById(id);

        return ResponseEntity.ok(items); // 返回所有商品明細
    }

    
    @GetMapping("/recipientInfo/{id}")
    public ResponseEntity<?> recipientInfo(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        Map<String, Object> claims = JwtUtil.parseToken(token);
        Integer memberId = (Integer) claims.get("memberId");

        Member member = memberService.findMemberById(memberId).orElse(null);

        return ResponseEntity.ok(member);
    }

}
