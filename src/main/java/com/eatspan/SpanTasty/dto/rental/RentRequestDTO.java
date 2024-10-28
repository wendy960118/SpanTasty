package com.eatspan.SpanTasty.dto.rental;

import java.util.List;

import com.eatspan.SpanTasty.entity.rental.Rent;

import lombok.Data;

@Data
public class RentRequestDTO {
	
	private Rent rent;
	
	private List<RentItemDTO> rentItemsDTO;

}
