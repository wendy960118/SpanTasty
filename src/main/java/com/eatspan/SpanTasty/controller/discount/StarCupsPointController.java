package com.eatspan.SpanTasty.controller.discount;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eatspan.SpanTasty.dto.discount.PointMemberDTO;
import com.eatspan.SpanTasty.entity.discount.Point;
import com.eatspan.SpanTasty.service.discount.PointService;


@Controller
@RequestMapping("/StarCups")
public class StarCupsPointController {
	
	@Autowired
	private PointService pointService;
	
	//導向頁面
	@GetMapping("/point")
	public String goToPoint() {
		return "/starcups/discount/point";
	}
	
	//
	@GetMapping("/point/show")
	@ResponseBody
	public Map<String, Object> showPoint(@RequestParam Integer memberId) {
		System.out.println(memberId);
		PointMemberDTO pointMember = pointService.getPointMember(memberId);//彙總點數
		Integer pointMemberTotalPoint = pointService.getPointMemberExpiryPoint(memberId);//未含過期總點數
		Page<Point> starCupsPoint = pointService.StarCupsPoint(memberId,0);//點數紀錄
		
		Map<String, Object> JsonMap = new HashMap<String,Object>();		
		JsonMap.put("pointMember", pointMember);
		JsonMap.put("pointMemberTotalPoint", pointMemberTotalPoint);
		JsonMap.put("pointsById", starCupsPoint);
		
		return JsonMap;
	}
	
	@GetMapping("/point/showAll")
	@ResponseBody
	public Page<Point> showAllPointPage(@RequestParam Integer memberId,@RequestParam(value = "p") Integer pageNumber) {
		Page<Point> starCupsPoint = pointService.StarCupsPoint(memberId,pageNumber);//點數紀錄				
		return starCupsPoint;
	}
	
	@GetMapping("/point/showGet")
	@ResponseBody
	public Page<Point> showGetPointPage(@RequestParam Integer memberId,@RequestParam(value = "p") Integer pageNumber) {
		Page<Point> starCupsPoint = pointService.StarCupsPointGet(memberId,pageNumber);//點數紀錄				
		return starCupsPoint;
	}
	
	

}
