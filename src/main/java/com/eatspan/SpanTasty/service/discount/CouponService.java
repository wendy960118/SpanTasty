package com.eatspan.SpanTasty.service.discount;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.MinguoChronology;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.metrics.StartupStep.Tags;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.eatspan.SpanTasty.config.MailConfig;
import com.eatspan.SpanTasty.dto.discount.CouponDTO;
import com.eatspan.SpanTasty.dto.discount.CouponDistributeDTO;
import com.eatspan.SpanTasty.dto.discount.TagDTO;
import com.eatspan.SpanTasty.dto.discount.couponOptionDTO;
import com.eatspan.SpanTasty.dto.order.MenuDTO;
import com.eatspan.SpanTasty.entity.account.Member;
import com.eatspan.SpanTasty.entity.discount.Coupon;
import com.eatspan.SpanTasty.entity.discount.CouponMember;
import com.eatspan.SpanTasty.entity.discount.CouponMemberId;
import com.eatspan.SpanTasty.entity.discount.CouponSchedule;
import com.eatspan.SpanTasty.entity.discount.Tag;
import com.eatspan.SpanTasty.entity.discount.TagId;
import com.eatspan.SpanTasty.entity.order.TogoItemEntity;
import com.eatspan.SpanTasty.entity.store.ProductType;
import com.eatspan.SpanTasty.entity.store.ShoppingItem;
import com.eatspan.SpanTasty.entity.store.ShoppingOrder;
import com.eatspan.SpanTasty.repository.account.MemberRepository;
import com.eatspan.SpanTasty.repository.discount.CouponMemberRepository;
import com.eatspan.SpanTasty.repository.discount.CouponRepository;
import com.eatspan.SpanTasty.repository.discount.CouponScheduleRepository;
import com.eatspan.SpanTasty.repository.order.FoodKindRepository;
import com.eatspan.SpanTasty.repository.store.ProductTypeRepository;
import com.eatspan.SpanTasty.repository.store.ShoppingItemRepository;
import com.eatspan.SpanTasty.repository.store.ShoppingOrderRepository;
import com.eatspan.SpanTasty.service.account.MemberService;
import com.eatspan.SpanTasty.service.store.ShoppingOrderService;
import com.eatspan.SpanTasty.utils.discount.DateUtils;

import freemarker.core.ParseException;
import freemarker.core.ReturnInstruction.Return;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class CouponService {

	@Autowired
	private CouponRepository CouponRepo;

	@Autowired
	private CouponMemberRepository couponMemberRepo;

	@Autowired
	private CouponScheduleRepository couponScheduleRepo;

	@Autowired
	private FoodKindRepository foodKindRepo;

	@Autowired
	private ProductTypeRepository productTypeRepo;

	@Autowired
	private MemberRepository memberRepo;

	@Autowired
	private ShoppingOrderService shoppingOrderService;

	@Autowired
	private MemberService memberService;

	@Autowired
	private MailConfig mailConfig;// javaMail要注入----------------------------

	@Autowired
	private JavaMailSender mailSender;// javaMail要注入----------------------------

	@Autowired
	private freemarker.template.Configuration freemarkerConfig; // javaMail要注入----------------------------

	// mail------------------------------------------------------------------------------------
	// 範例 //傳入參數在自行設置
	public void sendMail(String memberName, String memberEmail) throws MessagingException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
		// 設置mail
		helper.setFrom(mailConfig.getUserName());// 誰寄信(application設定的信箱,mail.properties的userName)
		helper.setTo(memberEmail);// 誰收信
		helper.setSubject("【☕週年靜加碼】starcups 咖啡不限金額9折");// 主旨
		// helpr.setText();不要模板或圖片資源的話在這裡設置內文，下面只要mail.send

		// 設置模板
		// 設置model
		Map<String, Object> model = new HashMap<String, Object>();
		// 透過modal傳入的物件("參數名","東西")
		model.put("userName", memberName);
		// get模板，並將modal傳入模板
		String templateString = FreeMarkerTemplateUtils
				.processTemplateIntoString(freemarkerConfig.getTemplate("couponMail.html"), model);

		// 設置mail內文
		helper.setText(templateString, true);

		// 設置資源，順序要在內文之後
		FileSystemResource file = new FileSystemResource(
				new File("src/main/resources/static/images/mail/logo-starcups.png"));
		helper.addInline("logo", file);

		mailSender.send(mimeMessage);
	}
	// -------------------------------------------------------------------------------------------

	// convert distributeDTO(excute)
	public CouponDistributeDTO converCouponDistributeDTO(CouponMember couponMember, String distributeStatus,
			String distributeFailReason) {
		CouponDistributeDTO couponDistributeDTO = new CouponDistributeDTO();
		couponDistributeDTO.setCouponId(couponMember.getCouponMemberId().getCouponId());
		couponDistributeDTO.setMemberId(couponMember.getCouponMemberId().getMemberId());
		couponDistributeDTO.setReceivedAmount(couponMember.getTotalAmount());
		couponDistributeDTO.setDistributeStatus(distributeStatus);
		couponDistributeDTO.setDistributeFailReason(distributeFailReason);
		return couponDistributeDTO;
	}

	// convert distributeDTO(pre excute)
	public CouponDistributeDTO convertCouponDistributeDTO(Coupon coupon) {
		return new CouponDistributeDTO(coupon.getCouponId(), coupon.getCouponCode() + coupon.getCouponDescription(), // selectOption
				coupon.getReceivedAmount(), coupon.getMaxCoupon());
	}

	// convert distributeDTOs(pre excute)
	public List<CouponDistributeDTO> convertCouponDistributeDTO(List<Coupon> couponBeans) {
		return couponBeans.stream().map(couponBean -> convertCouponDistributeDTO(couponBean))
				.collect(Collectors.toList());
	}

	// convert couponDTO
	public CouponDTO convertCouponDTO(Coupon coupon) {
		List<TagDTO> tagDTOs = coupon.getTags().stream()
				.map(tagBean -> new TagDTO(tagBean.getTagId().getTagName(), tagBean.getTagId().getTagType()))
				.collect(Collectors.toList());

		return new CouponDTO(coupon.getCouponId(), coupon.getCouponCode(), coupon.getCouponDescription(),
				coupon.getCouponStartDate(), coupon.getCouponEndDate(), coupon.getMaxCoupon(), coupon.getPerMaxCoupon(),
				coupon.getCouponStatus(), coupon.getRulesDescription(), coupon.getDiscountType(), coupon.getDiscount(),
				coupon.getMinOrderDiscount(), coupon.getMaxDiscount(), tagDTOs,
//				coupon.getMembers().size()//receivedAmount
				coupon.getCouponMember().stream().mapToInt(CouponMember::getTotalAmount).sum());
	}

	// convert couponDTOs
	public List<CouponDTO> convertCouponDTO(List<Coupon> coupons) {
		return coupons.stream().map(coupon -> convertCouponDTO(coupon)).collect(Collectors.toList());
	}

	// coupon add tags (before CRUD)
	public void addTags(Coupon coupon, String[] tags, String tagType) {
		if (tags != null) {
			for (String tag : tags) {
				Tag tagBean = new Tag();
//	            tagBean.setTagType(tagType);

				if (coupon.getCouponId() != null) {
					tagBean.setTagId(new TagId(coupon.getCouponId(), tag, tagType));// 修改透過coupon的ID
				} else {
					tagBean.setTagId(new TagId(tag, tagType));// 新增的COUPON沒有ID，透過關聯coupon identity新增

				}

				tagBean.setCoupon(coupon);
				List<Tag> tagsList = new ArrayList<Tag>();
//	            coupon.setTags(tagsList);
				coupon.getTags().add(tagBean);
			}
		}
	}

	// getAll Coupon
	public List<CouponDTO> getAllCoupon() {
		List<Coupon> coupons = CouponRepo.findAll();
		return convertCouponDTO(coupons);
	}

	// getById Coupon
	public CouponDTO getCouponById(Integer couponId) {
		Coupon coupon = CouponRepo.findByCouponId(couponId);
		return convertCouponDTO(coupon);
	}

	// getAll tagsOption(這裡有增加非資料庫的標籤"全品項")
	public Map<String, List<String>> getTagOptions() {

		Map<String, List<String>> tagOptions = new HashMap<>();

		// product
		List<String> productList = productTypeRepo.findProductTypeName();
		productList.add("商城(全品項)");
		tagOptions.put("product", productList);

		// togo
		List<String> togoList = foodKindRepo.findFoodKindName();
		togoList.add("訂餐(全品項)");
		tagOptions.put("togo", togoList);

		return tagOptions;

	}

	// insert Coupon
	public void insertCoupon(Coupon couponBean, String[] productTags, String[] togoTags) {
		addTags(couponBean, productTags, "product");
		addTags(couponBean, togoTags, "togo");
		System.out.println(couponBean);
		CouponRepo.save(couponBean);
	}

	// delete
	public void deleteCouponById(Integer couponId) {
		CouponRepo.deleteById(couponId);
	}

	// update
	public void updateCoupon(Coupon couponBean, String[] productTags, String[] togoTags) {
		addTags(couponBean, productTags, "product");
		addTags(couponBean, togoTags, "togo");
		CouponRepo.save(couponBean);
	}

	// search coupon
	public List<CouponDTO> searchCoupons(String keyWord) {
		LocalDate date = DateUtils.getLocalDateFromString(keyWord);
		Integer intKeyWord = null;
		try {
			intKeyWord = Integer.parseInt(keyWord);
		} catch (Exception e) {
			intKeyWord = null;
		}
		System.out.println(date);
		List<Coupon> searchCoupons = CouponRepo.searchCoupons(intKeyWord, keyWord, date);
		return convertCouponDTO(searchCoupons);
	}

	// distrubute option
	public List<CouponDistributeDTO> getDistributeOption(String memberIds) {
		String[] arrayMemberIds = memberIds.split(",");
		int distributeAmount = arrayMemberIds.length;

		List<Coupon> allCoupon = CouponRepo.findAll();
		List<Coupon> couponOption = new ArrayList<Coupon>();
		for (Coupon couponBean : allCoupon) {
			Integer maxAmount = couponBean.getMaxCoupon();
			Integer receivedAmount = couponBean.getCouponMember().stream().mapToInt(CouponMember::getTotalAmount).sum();
			if(couponBean.getCouponStatus().equals("上架")) {
				// null表示無發放限制
				if (maxAmount == null) {
					couponOption.add(couponBean);
				} else {
					int usageAmount = maxAmount - receivedAmount;
					if (usageAmount >= distributeAmount) {
						couponOption.add(couponBean);
					}
				}
				
			}
		}
		return convertCouponDistributeDTO(couponOption);
	}

	// 發放優惠券
	@Transactional
	public List<CouponDistributeDTO> distributeExcute2(String memberIds, Integer couponId, Integer perMaxCoupon,
			LocalDateTime scheduleTime, String scheduleName) {
//	    List<CouponMember> couponMembersToSave = new ArrayList<>();
		List<CouponSchedule> schedulesToSave = new ArrayList<>();
		List<CouponDistributeDTO> results = new ArrayList<>();

		Arrays.stream(memberIds.split(",")).mapToInt(Integer::parseInt)
				.mapToObj(memberId -> new CouponMemberId(couponId, memberId)).forEach(couponMemberId -> {
					Optional<CouponMember> optionalCouponMember = couponMemberRepo.findById(couponMemberId);

					CouponMember couponMember = optionalCouponMember
							.orElseGet(() -> new CouponMember(couponMemberId, 0, 0));
//	              String status = "成功";
					String status = "排程成功";
					String message = null;

					System.out.println("===" + couponMember.getTotalAmount() + " " + perMaxCoupon);
					if (perMaxCoupon == null || couponMember.getTotalAmount() < perMaxCoupon) {
//	                  couponMember.incrementAmounts();
//	                  couponMembersToSave.add(couponMember);
						CouponSchedule schedule = new CouponSchedule();
						schedule.setCouponMember(couponMember);
						schedule.setScheduleTime(scheduleTime);
						schedule.setScheduleName(scheduleName);
						schedule.setStatus("pending");
						schedulesToSave.add(schedule);
					} else {
//	                  status = "失敗";
						status = "排程失敗";
						message = "個人領取數量已達上限";
					}

					results.add(converCouponDistributeDTO(couponMember, status, message));
				});

//	    if (!couponMembersToSave.isEmpty()) {
//	        couponMemberRepo.saveAll(couponMembersToSave);
//	    }
		if (!schedulesToSave.isEmpty()) {
			couponScheduleRepo.saveAll(schedulesToSave);
		}

		return results;
	}

	// 會員領取優惠券判斷能不能領
	public Boolean canGetCoupon(Integer memberId, String couponCode) {
		Coupon coupon = CouponRepo.findByCouponCode(couponCode);
		if (coupon == null) {
			return false;
		}

		CouponMember couponMember = couponMemberRepo.findByMemberIdAndCouponId(memberId, coupon.getCouponId());

		// 最大優惠券數量
		Integer couponMaxAmount = coupon.getMaxCoupon();
		if (couponMaxAmount != null) {
			int couponReceivedAmount = coupon.getCouponMember().stream().mapToInt(CouponMember::getTotalAmount).sum();
			if (couponReceivedAmount >= couponMaxAmount) {
				return false;
			}
		}

		// 每人限制數量
		Integer couponPerMaxAmount = coupon.getPerMaxCoupon();
		if (couponPerMaxAmount != null && couponMember != null) {
			int perAmount = couponMember.getTotalAmount();
			if (perAmount >= couponPerMaxAmount) {
				return false;
			}
		}
		return true;
	}

	// 會員領取優惠券
	public void getCoupon(Integer memberId, String couponCode) {
		Coupon coupon = CouponRepo.findByCouponCode(couponCode);
		CouponMemberId couponMemberId = new CouponMemberId(coupon.getCouponId(), memberId);
		Optional<CouponMember> optional = couponMemberRepo.findById(couponMemberId);
		CouponMember couponMember = optional.orElseGet(() -> new CouponMember(couponMemberId, 0, 0));
		couponMember.incrementAmounts();
		couponMemberRepo.save(couponMember);

	}

	private List<String> getCartItemNames(List<?> cartItems) {

		if (cartItems.isEmpty()) {
			throw new RuntimeException("此方法不接受空List");
		}

		if (cartItems.get(0) instanceof ShoppingItem) {
			return cartItems.stream()
					.map(obj -> ((ShoppingItem)obj).getProduct().getProductType().getProductTypeName())
					.collect(Collectors.toList());
		}

		if (cartItems.get(0) instanceof MenuDTO) {
			return cartItems.stream()
					.map(obj -> ((MenuDTO)obj).getFoodKindName())
					.collect(Collectors.toList());
		}
		
		throw new RuntimeException("你傳入的型別不對喔");
	}

	// 結帳判斷優惠券
	public List<CouponMember> couponCanUse(List<?> shoppingItems, Integer totalAmount, Integer memberId) {
		// 購物車明細的種類 productNames
//		List<String> productNames = shoppingItems.stream()
//				.map(shoppingItem->shoppingItem.getProduct().getProductType().getProductTypeName())
//				.collect(Collectors.toList());
		List<String> productNames = getCartItemNames(shoppingItems);

		// 取得會員擁有優惠券 coupons (可使用數量>0)
		List<CouponMember> couponMembers = couponMemberRepo.findByMemberId(memberId).stream()
				.filter(couponMember -> couponMember.getUsageAmount() > 0).collect(Collectors.toList());

		// 判斷每個優惠券是否可以使用
		couponMembers.stream().forEach(couponMember -> {
			Coupon coupon = couponMember.getCoupon();

			// 1.低消判斷(null表示無限制)
			// 2.下架不能用
			// 3.過期
			Integer minOrderDiscount = coupon.getMinOrderDiscount() == null ? 0 : coupon.getMinOrderDiscount();
			if ((totalAmount <= minOrderDiscount && minOrderDiscount != null) || coupon.getCouponStatus() == "下架"
					|| coupon.getCouponEndDate().isBefore(LocalDate.now())) {
				couponMember.setCanUse(false);
				return;
			}

			// 去得優惠券的tags
			List<String> couponTagNames = coupon.getTags().stream()
					.filter(tag -> "product".equals(tag.getTagId().getTagType()))
					.map(tag -> tag.getTagId().getTagName()).collect(Collectors.toList());

			// 有標籤>是否包含一種購物車明細的種類>true 可折扣
			boolean canUseCoupon = couponTagNames.stream().anyMatch(productNames::contains);
			if (canUseCoupon) {
				couponMember.setCanUse(true);
				return;
			}
			// 無標籤>無折扣限制
			if (coupon.getTags().isEmpty()) {
				couponMember.setCanUse(true);
				return;
			}
			couponMember.setCanUse(false);
		});

		List<CouponMember> couponMemberCanUse = couponMembers.stream()
				.filter(couponMember -> couponMember.getCanUse() == true).collect(Collectors.toList());

		return couponMembers;
	}
	
	public List<CouponMember> couponCanUseTogo(List<?> shoppingItems, Integer totalAmount, Integer memberId) {
		// 購物車明細的種類 productNames
//		List<String> productNames = shoppingItems.stream()
//				.map(shoppingItem->shoppingItem.getProduct().getProductType().getProductTypeName())
//				.collect(Collectors.toList());
		List<String> productNames = getCartItemNames(shoppingItems);

		// 取得會員擁有優惠券 coupons (可使用數量>0)
		List<CouponMember> couponMembers = couponMemberRepo.findByMemberId(memberId).stream()
				.filter(couponMember -> couponMember.getUsageAmount() > 0).collect(Collectors.toList());

		// 判斷每個優惠券是否可以使用
		couponMembers.stream().forEach(couponMember -> {
			Coupon coupon = couponMember.getCoupon();

			// 1.低消判斷(null表示無限制)
			// 2.下架不能用
			// 3.過期
			Integer minOrderDiscount = coupon.getMinOrderDiscount() == null ? 0 : coupon.getMinOrderDiscount();
			if ((totalAmount <= minOrderDiscount && minOrderDiscount != null) || coupon.getCouponStatus() == "下架"
					|| coupon.getCouponEndDate().isBefore(LocalDate.now())) {
				couponMember.setCanUse(false);
				return;
			}

			// 去得優惠券的tags
			List<String> couponTagNames = coupon.getTags().stream()
					.filter(tag -> "togo".equals(tag.getTagId().getTagType()))
					.map(tag -> tag.getTagId().getTagName()).collect(Collectors.toList());

			// 有標籤>是否包含一種購物車明細的種類>true 可折扣
			boolean canUseCoupon = couponTagNames.stream().anyMatch(productNames::contains);
			if (canUseCoupon) {
				couponMember.setCanUse(true);
				return;
			}
			// 無標籤>無折扣限制
			if (coupon.getTags().isEmpty()) {
				couponMember.setCanUse(true);
				return;
			}
			couponMember.setCanUse(false);
		});

		List<CouponMember> couponMemberCanUse = couponMembers.stream()
				.filter(couponMember -> couponMember.getCanUse() == true).collect(Collectors.toList());

		return couponMembers;
	}
	

	public Integer coculateCouponDiscount(String couponCode, Integer shoppingItemsAmount) {
		Coupon coupon = CouponRepo.findByCouponCode(couponCode);
		Integer couponDiscount;
		switch (coupon.getDiscountType()) {
		case "百分比":
			Integer discountPercentage = coupon.getDiscount();
			couponDiscount = shoppingItemsAmount * (100 - discountPercentage) / 100;
			// 最大折扣限制 null表示無限制
			Integer maxDiscount = coupon.getMaxDiscount();
			if (maxDiscount != null) {
				return Math.min(couponDiscount, maxDiscount);
			}

			return couponDiscount;
		case "固定金額":
			return coupon.getDiscount();
		default:
			return 0;
		}
	}

	public void updateDiscountAmount(Integer shoppingId, Integer totalDiscount) {

		ShoppingOrder shoppingOrder = shoppingOrderService.findShoppingOrderById(shoppingId);
		shoppingOrder.setDiscountAmount(totalDiscount);
		shoppingOrder.setFinalTotal(shoppingOrder.getShoppingTotal() - totalDiscount);
		shoppingOrderService.updateShoppingOrder(shoppingOrder);
	}

	public void useCoupon(Integer couponId, Integer memberId) {
		CouponMember couponMember = couponMemberRepo.findByMemberIdAndCouponId(memberId, couponId);
		couponMember.setUsageAmount(couponMember.getUsageAmount() - 1);
		couponMemberRepo.save(couponMember);
	}

	public List<couponOptionDTO> getMemberOption() {
		List<Member> members = memberService.findAllMembers();
		return members.stream()
				.map(member -> new couponOptionDTO(member.getMemberId(),
						member.getMemberId() + "," + member.getMemberName() + "," + member.getPhone()))
				.collect(Collectors.toList());
	}
}
