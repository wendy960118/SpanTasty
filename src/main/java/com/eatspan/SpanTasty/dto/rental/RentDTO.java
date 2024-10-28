package com.eatspan.SpanTasty.dto.rental;

import java.util.Date;


import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class RentDTO {

    private Integer rentId;
    
	@DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date rentDate;
	
    private Integer rentStatus;
    
    private Integer rentDeposit;
    
    private String restaurantName;

    
    public RentDTO(Integer rentId, Date rentDate, Integer rentStatus, Integer rentDeposit, String restaurantName) {
        this.rentId = rentId;
        this.rentDate = rentDate;
        this.rentStatus = rentStatus;
        this.rentDeposit = rentDeposit;
        this.restaurantName = restaurantName;
    }
}
