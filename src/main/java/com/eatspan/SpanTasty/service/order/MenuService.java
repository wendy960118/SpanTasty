package com.eatspan.SpanTasty.service.order;

import java.util.List;

import com.eatspan.SpanTasty.entity.order.MenuEntity;

public interface MenuService {
	// 查詢
	public List<MenuEntity> getAllFoods();
	public MenuEntity getMenuById(Integer foodId);
	public List<MenuEntity> getFoodsByKind(Integer foodKindId);
	public List<MenuEntity> getFoodsByFoodName(String foodName);
	public List<MenuEntity> getFoodsByFoodStatus(Integer foodStatus);
	public List<MenuEntity> getFoodsByFoodKindIdAndFoodStatus(Integer foodKindId, Integer foodStatus);
	//新增
	public MenuEntity addFood(MenuEntity newFood);
	//刪除
	public void deleteFoodById(Integer foodId);
	//更新
	public MenuEntity updateFoodById(Integer foodId, MenuEntity updateFood);
	
}
