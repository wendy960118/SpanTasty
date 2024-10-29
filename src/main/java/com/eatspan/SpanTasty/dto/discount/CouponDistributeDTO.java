package com.eatspan.SpanTasty.dto.discount;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CouponDistributeDTO {
	
	private Integer couponId;
	@JsonIgnore
	private String selectOption;
	private Integer memberId;
	@JsonIgnore
	private String excuteStatus;
	@JsonIgnore
	private Integer maxCoupon;
	private Integer receivedAmount;
	private String distributeStatus;//for發放
	private String distributeFailReason;//for發放
	
	public CouponDistributeDTO(int couponId, String selectOption, Integer maxCoupon, Integer receivedAmount) {
		this.couponId = couponId;
		this.selectOption = selectOption;
		this.maxCoupon = maxCoupon;
		this.receivedAmount = receivedAmount;
	}
}
