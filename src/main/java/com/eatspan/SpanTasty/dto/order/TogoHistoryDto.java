package com.eatspan.SpanTasty.dto.order;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class TogoHistoryDto {
	
	private Integer togoId;
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC+8")
	private LocalDateTime togoCreateDate;
	private Integer finalTotal;
	private Integer togoStatus;
}
