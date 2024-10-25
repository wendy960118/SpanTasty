package com.eatspan.SpanTasty.dto.reservation;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class ReserveCheckDTO {
	
	
	@JsonFormat(pattern = "HH:mm")
	private LocalTime startTime;
	
	@JsonFormat(pattern = "HH:mm")
	private LocalTime endTime;
	
	// 不開放組合桌位時 => 可預訂桌位數
	// 開放組合桌位時 => 可預訂總人數
	private Integer totalTableNumber;
	
	// 不開放組合桌位時 => 已被預訂桌位數
	// 開放組合桌位時 => 預訂人數
	private Integer reservedTableNumber;
	
	
}
