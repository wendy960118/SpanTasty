package com.eatspan.SpanTasty.dto.order;

import java.util.List;


import com.eatspan.SpanTasty.entity.order.TogoItemEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class TogoItemDTO {
	/*
	 * for 結帳優惠券使用
	 * 
	 * 
	 * */
	private List<MenuDTO> togoItems;
	private Integer totalAmount;
	private Integer memberId;
	private String type;
}
