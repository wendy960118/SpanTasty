package com.eatspan.SpanTasty.service.rental;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eatspan.SpanTasty.entity.rental.Tableware;
import com.eatspan.SpanTasty.entity.rental.TablewareStock;
import com.eatspan.SpanTasty.repository.rental.TablewareRepository;
import com.eatspan.SpanTasty.repository.rental.TablewareStockRepository;


@Service
public class TablewareStockService {
	
	
	@Autowired
	private TablewareStockRepository tablewareStockRepository;
	@Autowired
	private TablewareRepository tablewareRepository;

	
	//新增餐具庫存
	public TablewareStock addStock(TablewareStock tablewareStock) {
		return tablewareStockRepository.save(tablewareStock);
	}

	
	//更改庫存
	public TablewareStock updateStock(Integer tablewareId, Integer restaurantId, Integer stock) {
		TablewareStock tablewareStock = tablewareStockRepository.findById(tablewareId, restaurantId);
		if(tablewareStock != null) {
			tablewareStock.setStock(stock);
			return tablewareStock;
		}
		return null;
	}
	
	
	//查詢餐具(By餐具 餐廳編號(可擇一))
	public List<TablewareStock> findStocksByCriteria(Integer tablewareId, Integer restaurantId) {
		return tablewareStockRepository.findByCriteria(tablewareId, restaurantId);
	}
	
	
	//查詢所有庫存
	public List<TablewareStock> findAllStocks(){
		return tablewareStockRepository.findAll();
	}
	
	
	// 查詢庫存
	public TablewareStock findStockById(Integer tablewareId, Integer restaurantId) {
		TablewareStock tablewareStock = tablewareStockRepository.findById(tablewareId, restaurantId);
		if(tablewareStock != null) {
			return tablewareStock;
		}
		return null;
	}
	
	
	// 查詢餐具(By餐具編號)
	public List<TablewareStock> findStockByTablewareId(Integer tablewareId) {
		return tablewareStockRepository.findByTablewareId(tablewareId);
	}
	
	
	//
	public List<TablewareStock> findStockByRestaurantId(Integer restaurantId) {
		return tablewareStockRepository.findByRestaurantId(restaurantId);
	}
	
	
	// 新增庫存(By餐廳編號)
	public void addStockByRestaurantId(Integer restaurantId) {
		List<Tableware> tablewares = tablewareRepository.findAll();
		for(Tableware tableware : tablewares) {
			TablewareStock tablewareStock = new TablewareStock();
			tablewareStock.setTablewareId(tableware.getTablewareId());
			tablewareStock.setRestaurantId(restaurantId);
			tablewareStock.setStock(0);
			tablewareStockRepository.save(tablewareStock);
		}
	}

	
//	查詢所有庫存(Page)
//	public Page<TablewareStock> findAllStocksPage(Integer page){
//		Pageable pageable = PageRequest.of(page-1, 10, Sort.Direction.ASC, "restaurantId");
//		return tablewareStockRepository.findAll(pageable);
//	}
}
