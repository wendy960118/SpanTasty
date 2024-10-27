package com.eatspan.SpanTasty.dto.order;



import org.springframework.data.domain.Page;

import com.eatspan.SpanTasty.entity.order.TogoEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class TogoDto {
	Page<TogoEntity> togoList;
	int selectedId;
}
