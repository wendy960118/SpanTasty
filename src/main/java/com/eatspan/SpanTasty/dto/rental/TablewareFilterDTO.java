package com.eatspan.SpanTasty.dto.rental;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
@Data
public class TablewareFilterDTO {

	@JsonProperty("restaurantId")
	private Integer restaurantId;

}