package com.eatspan.SpanTasty.service.discount;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eatspan.SpanTasty.dto.discount.CouponDTO;
import com.eatspan.SpanTasty.dto.discount.CouponDistributeDTO;
import com.eatspan.SpanTasty.dto.discount.TagDTO;
import com.eatspan.SpanTasty.entity.discount.Coupon;
import com.eatspan.SpanTasty.entity.discount.CouponMember;
import com.eatspan.SpanTasty.entity.discount.CouponMemberId;
import com.eatspan.SpanTasty.entity.discount.Tag;
import com.eatspan.SpanTasty.entity.discount.TagId;
import com.eatspan.SpanTasty.repository.discount.CouponMemberRepository;
import com.eatspan.SpanTasty.repository.discount.CouponRepository;
import com.eatspan.SpanTasty.repository.order.FoodKindRepository;
import com.eatspan.SpanTasty.repository.store.ProductTypeRepository;
import com.eatspan.SpanTasty.utils.discount.DateUtils;

@Service
public class CouponService {
	
	
	@Autowired
	private CouponRepository CouponRepo;
	
	@Autowired
	private CouponMemberRepository couponMemberRepo;
	
	@Autowired
	private FoodKindRepository foodKindRepo;
	
	@Autowired
	private ProductTypeRepository productTypeRepo;

	
	//convert distributeDTO(excute)
	public CouponDistributeDTO converCouponDistributeDTO(CouponMember couponMember,String distributeStatus,String distributeFailReason ) {
		CouponDistributeDTO couponDistributeDTO = new CouponDistributeDTO();	
		couponDistributeDTO.setCouponId(couponMember.getCouponMemberId().getCouponId());
		couponDistributeDTO.setMemberId(couponMember.getCouponMemberId().getMemberId());
		couponDistributeDTO.setReceivedAmount(couponMember.getTotalAmount());
		couponDistributeDTO.setDistributeStatus(distributeStatus);
		couponDistributeDTO.setDistributeFailReason(distributeFailReason);
		return couponDistributeDTO;
	}
	
	//convert distributeDTO(pre excute)
	public CouponDistributeDTO convertCouponDistributeDTO(Coupon coupon) {
		return new CouponDistributeDTO(
				coupon.getCouponId(),
				coupon.getCouponCode()+coupon.getCouponDescription(),//selectOption
				coupon.getReceivedAmount(),
				coupon.getMaxCoupon()
		);
	}
	
	
	//convert distributeDTOs(pre excute)
	public List<CouponDistributeDTO> convertCouponDistributeDTO(List<Coupon> couponBeans){
		return couponBeans.stream()
				.map(couponBean -> convertCouponDistributeDTO(couponBean))
				.collect(Collectors.toList());
	}
	
	//convert couponDTO
	public CouponDTO convertCouponDTO(Coupon coupon) {
		List<TagDTO> tagDTOs = coupon.getTags().stream()
				.map(tagBean -> new TagDTO(tagBean.getTagId().getTagName(), tagBean.getTagType()))
				.collect(Collectors.toList());

		return new CouponDTO(
				coupon.getCouponId(),
				coupon.getCouponCode(),
				coupon.getCouponDescription(),
				coupon.getCouponStartDate(),
				coupon.getCouponEndDate(),
				coupon.getMaxCoupon(),
				coupon.getPerMaxCoupon(),
				coupon.getCouponStatus(),
				coupon.getRulesDescription(),
				coupon.getDiscountType(),
				coupon.getDiscount(),
				coupon.getMinOrderDiscount(),
				coupon.getMaxDiscount(),
				tagDTOs,
//				coupon.getMembers().size()//receivedAmount
				coupon.getCouponMember().stream()
										.mapToInt(CouponMember::getTotalAmount)
										.sum()
		);
	}
	
	//convert couponDTOs
	public List<CouponDTO> convertCouponDTO(List<Coupon> coupons) {
	    return coupons.stream()
	            .map(coupon -> convertCouponDTO(coupon))
	            .collect(Collectors.toList());
	}
	
	//coupon add tags (before CRUD)	
	public void addTags(Coupon coupon, String[] tags, String tagType) {
	    if (tags != null) {
	        for (String tag : tags) {
	            Tag tagBean = new Tag();
	            tagBean.setTagType(tagType);
	            
	            if (coupon.getCouponId() != null) {
	            	tagBean.setTagId(new TagId(coupon.getCouponId(),tag));//修改透過coupon的ID
	            }else {
	            	tagBean.setTagId(new TagId(tag));//新增的COUPON沒有ID，透過關聯coupon identity新增
	            }
	            
	            tagBean.setCoupon(coupon);
	            coupon.getTags().add(tagBean);
	        }
	    }
	}
	
	//getAll Coupon
	public List<CouponDTO> getAllCoupon(){
		List<Coupon> coupons = CouponRepo.findAll();
		return convertCouponDTO(coupons);
	}
	
	//getById Coupon
	public CouponDTO getCouponById(Integer couponId) {
		Coupon coupon = CouponRepo.findByCouponId(couponId);
		return convertCouponDTO(coupon);
	}
	
	//getAll tagsOption(這裡有增加非資料庫的標籤"全品項")
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
	
	//insert Coupon
	public void insertCoupon(Coupon couponBean,String[] productTags, String[] togoTags) {
		addTags(couponBean,productTags,"product");
		addTags(couponBean,togoTags,"togo");
		CouponRepo.save(couponBean);
	}
	
	//delete
	public void deleteCouponById(Integer couponId) {
		CouponRepo.deleteById(couponId);
	}
	
	//update
	public void updateCoupon(Coupon couponBean, String[] productTags, String[] togoTags) {
		addTags(couponBean,productTags,"product");
		addTags(couponBean,togoTags,"togo");
		CouponRepo.save(couponBean);
	}
	
	//search coupon
	public List<CouponDTO> searchCoupons(String keyWord){
		LocalDate date = DateUtils.getLocalDateFromString(keyWord);
		Integer intKeyWord = null;
		try {
			intKeyWord = Integer.parseInt(keyWord);
		} catch (Exception e) {
			intKeyWord = null;
		}
		System.out.println(date);
		List<Coupon> searchCoupons = CouponRepo.searchCoupons(intKeyWord,keyWord,date);
		return convertCouponDTO(searchCoupons);
	}
	
	//distrubute option
	public List<CouponDistributeDTO> getDistributeOption(String memberIds){
		String[] arrayMemberIds=memberIds.split(",");
		int distributeAmount=arrayMemberIds.length;
		
		List<Coupon> allCoupon = CouponRepo.findAll();
		List<Coupon> couponOption = new ArrayList<Coupon>();
		for (Coupon couponBean : allCoupon) {
			Integer maxAmount = couponBean.getMaxCoupon();
			Integer receivedAmount = couponBean.getCouponMember().stream()
									.mapToInt(CouponMember::getTotalAmount)
									.sum();				
			
			//null表示無發放限制
			if(maxAmount==null) {    
				couponOption.add(couponBean);
			}else {
				int usageAmount = maxAmount-receivedAmount;
				if(usageAmount>= distributeAmount){
					couponOption.add(couponBean);
				}
			}
		}
		return convertCouponDistributeDTO(couponOption);
	}
	
	//發放優惠券
	public List<CouponDistributeDTO> distributeExcute(String memberIds,Integer couponId,Integer perMaxCoupon) {
		List<CouponMember> couponMembers=new ArrayList<>();
		List<CouponDistributeDTO> couponDistributeDTOs = new ArrayList<>();
		
		//memberIds="1,2,3,..."
		String[] arrayMemberIds =  memberIds.split(",");
		
		for (String memberIdStr : arrayMemberIds) {
			int memberId = Integer.parseInt(memberIdStr);
			CouponMemberId couponMemberId = new CouponMemberId(couponId,memberId);
			Optional<CouponMember> optional = couponMemberRepo.findById(couponMemberId);
			
			if(perMaxCoupon==null) {
				if(optional.isEmpty()) {
					//資料庫
					CouponMember couponMember = new CouponMember(couponMemberId,1,1);
					couponMembers.add(couponMember);
					
					//傳回前端
					CouponDistributeDTO converCouponDistributeDTO = converCouponDistributeDTO(couponMember, "成功", null);
					couponDistributeDTOs.add(converCouponDistributeDTO);
				}else {
					//資料庫
					CouponMember couponMember = optional.get();
					couponMember.setTotalAmount(couponMember.getTotalAmount()+1);
					couponMember.setUsageAmount(couponMember.getUsageAmount()+1);
					couponMembers.add(couponMember);
					
					//傳回前端
					CouponDistributeDTO converCouponDistributeDTO = converCouponDistributeDTO(couponMember, "成功", null);
					couponDistributeDTOs.add(converCouponDistributeDTO);
				}
				couponMemberRepo.saveAll(couponMembers);//資料庫
				return couponDistributeDTOs;//傳回前端
			}else {
				if(optional.isEmpty()) {
					//資料庫
					CouponMember couponMember = new CouponMember(couponMemberId,1,1);
					couponMembers.add(couponMember);
					
					//傳回前端
					CouponDistributeDTO converCouponDistributeDTO = converCouponDistributeDTO(couponMember, "成功", null);
					couponDistributeDTOs.add(converCouponDistributeDTO);					
				}else if(optional.get().getTotalAmount()<perMaxCoupon){
					//資料庫
					CouponMember couponMember = optional.get();
					couponMember.setTotalAmount(couponMember.getTotalAmount()+1);
					couponMember.setUsageAmount(couponMember.getUsageAmount()+1);
					couponMembers.add(couponMember);
					
					//傳回前端
					CouponDistributeDTO converCouponDistributeDTO = converCouponDistributeDTO(couponMember, "失敗", "個人領取數量已達上限");
					couponDistributeDTOs.add(converCouponDistributeDTO);
				}else {
					CouponMember couponMember = optional.get();
					//傳回前端
					CouponDistributeDTO converCouponDistributeDTO = converCouponDistributeDTO(couponMember, "失敗", "個人領取數量已達上限");
					couponDistributeDTOs.add(converCouponDistributeDTO);
				}
				couponMemberRepo.saveAll(couponMembers);//資料庫
				return couponDistributeDTOs;//傳回前端
			}
		}
		return couponDistributeDTOs;
	}
	
	public List<CouponDistributeDTO> distributeExcute2(String memberIds, Integer couponId, Integer perMaxCoupon) {
	    List<CouponMember> couponMembersToSave = new ArrayList<>();
	    List<CouponDistributeDTO> results = new ArrayList<>();

	    Arrays.stream(memberIds.split(","))
	          .mapToInt(Integer::parseInt)
	          .mapToObj(memberId -> new CouponMemberId(couponId, memberId))
	          .forEach(couponMemberId -> {
	              Optional<CouponMember> optionalCouponMember = couponMemberRepo.findById(couponMemberId);
	              
	              CouponMember couponMember = optionalCouponMember.orElseGet(() -> new CouponMember(couponMemberId, 0, 0));
	              String status = "成功";
	              String message = null;
	              
	              System.out.println("==="+couponMember.getTotalAmount()+" "+perMaxCoupon);
	              if (perMaxCoupon == null || couponMember.getTotalAmount() < perMaxCoupon) {
	                  couponMember.incrementAmounts();
	                  couponMembersToSave.add(couponMember);
	              } else {
	                  status = "失敗";
	                  message = "個人領取數量已達上限";
	              }

	              results.add(converCouponDistributeDTO(couponMember, status, message));
	          });

	    if (!couponMembersToSave.isEmpty()) {
	        couponMemberRepo.saveAll(couponMembersToSave);
	    }

	    return results;
	}
}