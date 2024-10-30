package com.eatspan.SpanTasty.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;

//專門用來接收ajax傳來的json物件
@Data
@AllArgsConstructor
public class RestaurantTableDTO {
	
	private Integer restaurantId;
	private String tableTypeId;
	private Integer tableId;
	private Integer tableStatus;
	
}
