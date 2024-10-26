package com.eatspan.SpanTasty.controller.discount;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eatspan.SpanTasty.dto.discount.PointMemberDTO;
import com.eatspan.SpanTasty.service.discount.PointService;

@Controller
public class StarCupsCheckOutController {
	
	@Autowired
	private PointService pointService;
	
	
	@GetMapping("/StarCups/togo/checkout")
	@ResponseBody
	public Map<String,Object> checkout(@RequestParam Integer memberId){
		System.out.println(memberId);
		PointMemberDTO pointMember = pointService.getPointMember(memberId);
		Integer pointMemberTotalPoint = pointService.getPointMemberExpiryPoint(memberId);
		Map<String,Object> resultMap = new HashMap<>();
		resultMap.put("pointMember", pointMember);
		resultMap.put("pointMemberTotalPoint", pointMemberTotalPoint);
		return resultMap;
	}
}
