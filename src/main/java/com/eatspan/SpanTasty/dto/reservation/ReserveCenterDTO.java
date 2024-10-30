package com.eatspan.SpanTasty.dto.reservation;

import java.util.List;

import com.eatspan.SpanTasty.entity.reservation.Reserve;
import com.eatspan.SpanTasty.entity.reservation.RestaurantTable;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class ReserveCenterDTO {
	
    private List<Reserve> reserves;
    private List<RestaurantTable> restaurantTables;

	
	
}
