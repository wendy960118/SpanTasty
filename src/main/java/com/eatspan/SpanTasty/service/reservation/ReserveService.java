package com.eatspan.SpanTasty.service.reservation;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.eatspan.SpanTasty.config.MailConfig;
import com.eatspan.SpanTasty.dto.reservation.ReserveCheckDTO;
import com.eatspan.SpanTasty.dto.reservation.ReserveDTO;
import com.eatspan.SpanTasty.dto.reservation.TimeSlotDTO;
import com.eatspan.SpanTasty.entity.account.Member;
import com.eatspan.SpanTasty.entity.reservation.Reserve;
import com.eatspan.SpanTasty.entity.reservation.Restaurant;
import com.eatspan.SpanTasty.entity.reservation.RestaurantTable;
import com.eatspan.SpanTasty.entity.reservation.RestaurantTableId;
import com.eatspan.SpanTasty.entity.reservation.TableType;
import com.eatspan.SpanTasty.repository.reservation.ReserveRepository;
import com.eatspan.SpanTasty.repository.reservation.RestaurantRepository;
import com.eatspan.SpanTasty.service.account.MemberService;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class ReserveService {
	
	@Autowired
	private ReserveRepository reserveRepository;
	@Autowired
	private RestaurantRepository restaurantRepository;
	
	@Autowired
	private MemberService memberService;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private TableTypeService tableTypeService;
	@Autowired
	private RestaurantTableService restaurantTableService;
	
	@Autowired
	private MailConfig mailConfig;// javaMail
	
	@Autowired
	private JavaMailSender mailSender;// javaMail
	
	@Autowired
	private freemarker.template.Configuration freemarkerConfig; // javaMail
	
	
	
	// 新增訂位
	public Reserve addReserve(ReserveDTO reserveDTO) {
	    
	    Reserve reserve = new Reserve();
		setReserveByReserveDTO(reserve, reserveDTO);
	    
	    // 根據reserveSeat取得訂位桌子種類
	    String tableTypeId = getTableTypeIdByReserveSeat(reserve.getReserveSeat());
	    
	    // 取得該餐廳想預定的桌子種類所有桌子(只有可預訂的)
	    List<RestaurantTable> availableTables = restaurantTableService.findTableByRestaurantAndTypeAndStatus(reserveDTO.getRestaurantId(), tableTypeId);
	    
	    // 該訂位時段所有訂位訂單
	    List<Reserve> reservations = reserveRepository.getReservationsInTimeSlot(
	        reserveDTO.getRestaurantId(), 
	        reserveDTO.getCheckDate(), 
	        reserveDTO.getStartTime(), 
	        reserveDTO.getStartTime().plusMinutes(reserve.getRestaurant().getEattime())
	    );

	    // 收集所有已被預訂的桌子
	    Set<RestaurantTableId> reservedTableIds = reservations.stream()
	            .flatMap(reservation -> reservation.getRestaurantTables().stream().map(RestaurantTable::getId))
	            .collect(Collectors.toSet());
	    
	    // 過濾掉已被預訂的桌子
	    List<RestaurantTable> finalAvailableTables = availableTables.stream()
	        .filter(table -> !reservedTableIds.contains(table.getId()))
	        .collect(Collectors.toList());

	    // 如果沒有可用的桌子，可以處理錯誤或返回特定的響應
	    if (finalAvailableTables.isEmpty()) {
	    	System.out.println("沒有符合的單一桌位，將進入組合桌位訂定");
	    	return null;
	    }
	    
	    // 選擇第一張可用桌子（或根據需求選擇）
	    RestaurantTable selectedTable = finalAvailableTables.get(0);
	    
	    reserve.setRestaurantTables(new ArrayList<>(List.of(selectedTable)));
	    selectedTable.setReserves(new ArrayList<>(List.of(reserve)));
	    reserve.setTableId(selectedTable.getId().getTableTypeId()+selectedTable.getId().getTableId());
	    return reserveRepository.save(reserve);
	}

	
//	// 新增訂位(組合)
	public Reserve addCombinedReserve(ReserveDTO reserveDTO) {
	    Reserve reserve = new Reserve();
	    setReserveByReserveDTO(reserve, reserveDTO);

	    // 將 remainingSeats 包裝在一個陣列中
	    final int[] remainingSeats = {reserveDTO.getReserveSeat()};

	    // 取得該餐廳所有可預訂的桌子，按座位數降序排列
	    List<RestaurantTable> availableTables = restaurantTableService.findTableByRestaurantAndStatus(reserveDTO.getRestaurantId())
	        .stream()
	        .sorted((t1, t2) -> Integer.compare(t2.getTableType().getTableTypeName(), t1.getTableType().getTableTypeName()))
	        .collect(Collectors.toList());

	    // 該訂位時段已預訂的桌子Id集合
	    Set<RestaurantTableId> reservedTableIds = reserveRepository.getReservationsInTimeSlot(
	            reserveDTO.getRestaurantId(),
	            reserveDTO.getCheckDate(),
	            reserveDTO.getStartTime(),
	            reserveDTO.getStartTime().plusMinutes(reserve.getRestaurant().getEattime()))
	        .stream()
	        .flatMap(reservation -> reservation.getRestaurantTables().stream().map(RestaurantTable::getId))
	        .collect(Collectors.toSet());

	    // 過濾掉已被預訂的桌子
	    List<RestaurantTable> filteredTables = availableTables.stream()
	        .filter(table -> !reservedTableIds.contains(table.getId()))
	        .collect(Collectors.toList());

	    List<RestaurantTable> selectedTables = new ArrayList<>();

	    // 循環選擇符合需求的桌子
	    while (remainingSeats[0] > 0 && !filteredTables.isEmpty()) {
	        // 找到剛好符合剩餘需求的桌子
	        Optional<RestaurantTable> exactMatchTable = filteredTables.stream()
	            .filter(table -> table.getTableType().getTableTypeName() == remainingSeats[0]||table.getTableType().getTableTypeName() == remainingSeats[0]+1)
	            .findFirst();

	        if (exactMatchTable.isPresent()) {
	            RestaurantTable table = exactMatchTable.get();
	            selectedTables.add(table);
	            remainingSeats[0] -= table.getTableType().getTableTypeName(); // 更新剩餘需求
	            filteredTables.remove(table); // 從可選桌子中移除該桌子
	            break; // 座位需求已滿足，結束迴圈
	        }

	        // 否則，選擇座位數大於需求或最大桌子
	        RestaurantTable bestFitTable = filteredTables.stream()
	            .filter(table -> table.getTableType().getTableTypeName() >= remainingSeats[0])
//		        .sorted((t1, t2) -> Integer.compare(t1.getTableType().getTableTypeName(), t2.getTableType().getTableTypeName()))
	            .findFirst()
	            .orElse(filteredTables.get(0)); // 若無符合大小，選擇最大桌子

	        selectedTables.add(bestFitTable);
	        remainingSeats[0] -= bestFitTable.getTableType().getTableTypeName(); // 更新剩餘需求
	        filteredTables.remove(bestFitTable);
	    }

	    // 無法滿足訂位需求
	    if (remainingSeats[0] > 0) {
	        System.out.println("無法找到足夠的桌位");
	        return null;
	    }

	    // 設定訂單的桌位資訊
	    reserve.setRestaurantTables(selectedTables);
	    selectedTables.forEach(table -> table.getReserves().add(reserve));

	    // 拼接所有桌子的ID以作為訂單的桌子ID
	    reserve.setTableId(selectedTables.stream()
	        .map(table -> table.getId().getTableTypeId() + table.getId().getTableId())
	        .collect(Collectors.joining(", ")));

	    return reserveRepository.save(reserve);
	}




	
	// 修改訂位
	@Transactional
	public Reserve updateReservebyDto(ReserveDTO reserveDTO) {
		
		Reserve reserve = findReserveById(reserveDTO.getReserveId());
	    
		setReserveByReserveDTO(reserve, reserveDTO);
		
	    // 根據reserveSeat取得訂位桌子種類
	    String tableTypeId = getTableTypeIdByReserveSeat(reserve.getReserveSeat());
	    
	    // 取得該餐廳想預定的桌子種類所有桌子(只有可預訂的)
	    List<RestaurantTable> availableTables = restaurantTableService.findTableByRestaurantAndTypeAndStatus(reserveDTO.getRestaurantId(), tableTypeId);
	    
	    // 該訂位時段所有訂位訂單
	    List<Reserve> reservations = reserveRepository.getReservationsInTimeSlot(
	        reserveDTO.getRestaurantId(), 
	        reserveDTO.getCheckDate(), 
	        reserveDTO.getStartTime(), 
	        reserveDTO.getStartTime().plusMinutes(reserve.getRestaurant().getEattime())
	    );
	    
	    // 收集所有已被預訂的桌子Id
	    Set<RestaurantTableId> reservedTableIds = reservations.stream()
            .flatMap(reservation -> reservation.getRestaurantTables().stream().map(RestaurantTable::getId))
            .collect(Collectors.toSet());
	    
	    // 過濾掉已被預訂的桌子
	    List<RestaurantTable> finalAvailableTables = availableTables.stream()
	        .filter(table -> !reservedTableIds.contains(table.getId()))
	        .collect(Collectors.toList());
	    
	    
	    // 如果沒有可用的桌子，可以處理錯誤或返回特定的響應
	    if (finalAvailableTables.isEmpty()) {
	    	System.out.println("沒有符合的單一桌位，將進入組合桌位訂定");
	    	return null;
	    }
	    
	    // 選擇第一張可用桌子（或根據需求選擇）
	    RestaurantTable selectedTable = finalAvailableTables.get(0);
	    reserve.setRestaurantTables(new ArrayList<>(List.of(selectedTable)));
	    selectedTable.setReserves(new ArrayList<>(List.of(reserve)));
	    reserve.setTableId(selectedTable.getId().getTableTypeId()+selectedTable.getId().getTableId());
	    return reserveRepository.save(reserve);
	}
	
	
	
	// 修改訂位
	@Transactional
	public Reserve updateCombinedReservebyDto(ReserveDTO reserveDTO) {
		
		Reserve reserve = findReserveById(reserveDTO.getReserveId());
	    setReserveByReserveDTO(reserve, reserveDTO);

	    // 將 remainingSeats 包裝在一個陣列中
	    final int[] remainingSeats = {reserveDTO.getReserveSeat()};

	    // 取得該餐廳所有可預訂的桌子，按座位數降序排列
	    List<RestaurantTable> availableTables = restaurantTableService.findTableByRestaurantAndStatus(reserveDTO.getRestaurantId())
	        .stream()
	        .sorted((t1, t2) -> Integer.compare(t2.getTableType().getTableTypeName(), t1.getTableType().getTableTypeName()))
	        .collect(Collectors.toList());
	    
	    // 該訂位時段已預訂的桌子Id集合
	    Set<RestaurantTableId> reservedTableIds = reserveRepository.getReservationsInTimeSlot(
	            reserveDTO.getRestaurantId(),
	            reserveDTO.getCheckDate(),
	            reserveDTO.getStartTime(),
	            reserveDTO.getStartTime().plusMinutes(reserve.getRestaurant().getEattime()))
	        .stream()
	        .filter(reservation -> !reservation.getReserveId().equals(reserveDTO.getReserveId())) // 排除修改的訂單
	        .flatMap(reservation -> reservation.getRestaurantTables().stream().map(RestaurantTable::getId))
	        .collect(Collectors.toSet());
	    
	    // 過濾掉已被預訂的桌子
	    List<RestaurantTable> filteredTables = availableTables.stream()
	        .filter(table -> !reservedTableIds.contains(table.getId()))
	        .collect(Collectors.toList());
	    
	    List<RestaurantTable> selectedTables = new ArrayList<>();

	    // 循環選擇符合需求的桌子
	    while (remainingSeats[0] > 0 && !filteredTables.isEmpty()) {
	        // 找到剛好符合剩餘需求的桌子
	        Optional<RestaurantTable> exactMatchTable = filteredTables.stream()
	            .filter(table -> table.getTableType().getTableTypeName() == remainingSeats[0] || table.getTableType().getTableTypeName() == remainingSeats[0]+1) 
	            .findFirst();

	        if (exactMatchTable.isPresent()) {
	            RestaurantTable table = exactMatchTable.get();
	            selectedTables.add(table);
	            remainingSeats[0] -= table.getTableType().getTableTypeName(); // 更新剩餘需求
	            filteredTables.remove(table); // 從可選桌子中移除該桌子
	            break; // 座位需求已滿足，結束迴圈
	        }

	        // 否則，選擇座位數大於需求或最大桌子
	        RestaurantTable bestFitTable = filteredTables.stream()
	            .filter(table -> table.getTableType().getTableTypeName() >= remainingSeats[0])
//		        .sorted((t1, t2) -> Integer.compare(t1.getTableType().getTableTypeName(), t2.getTableType().getTableTypeName()))
	            .findFirst()
	            .orElse(filteredTables.get(0)); // 若無符合大小，選擇最大桌子

	        selectedTables.add(bestFitTable);
	        remainingSeats[0] -= bestFitTable.getTableType().getTableTypeName(); // 更新剩餘需求
	        filteredTables.remove(bestFitTable);
	    }

	    // 無法滿足訂位需求
	    if (remainingSeats[0] > 0) {
	        System.out.println("無法找到足夠的桌位");
	        return null;
	    }

	    // 設定訂單的桌位資訊
	    reserve.setRestaurantTables(selectedTables);
	    selectedTables.forEach(table -> table.getReserves().add(reserve));

	    // 拼接所有桌子的ID以作為訂單的桌子ID
	    reserve.setTableId(selectedTables.stream()
	        .map(table -> table.getId().getTableTypeId() + table.getId().getTableId())
	        .collect(Collectors.joining(", ")));

	    return reserveRepository.save(reserve);
	}
	
	
	
	
	private Reserve setReserveByReserveDTO(Reserve reserve, ReserveDTO reserveDTO) {
		
	    reserve.setReserveSeat(reserveDTO.getReserveSeat());
	    reserve.setReserveTime(reserveDTO.getCheckDate().atTime(reserveDTO.getStartTime()));
	    reserve.setReserveNote(reserveDTO.getReserveNote());
	    reserve.setMember(memberService.findMemberById(reserveDTO.getMemberId()).get());
	    reserve.setRestaurant(restaurantService.findRestaurantById(reserveDTO.getRestaurantId()));
	    reserve.setFinishedTime(reserve.getReserveTime().plusMinutes(reserve.getRestaurant().getEattime()));
	    reserve.setReserveStatus(reserveDTO.getReserveStatus());
		
	    return reserve;
	}
	
	
	
	
	// 後台新增訂位檢查傳入參數
	public String validateAddReserveDto(ReserveDTO reserveDTO) {
		
		// 檢查是否有MemberId
		Integer memberId = reserveDTO.getMemberId();
		if(memberId==null || memberId <= 0) return "MemberId is not validate";
		// 檢查是否有RestaurantId
		Integer restaurantId = reserveDTO.getRestaurantId();
		if(restaurantId==null || restaurantId <= 0) return "RestaurantId is not validate";
		// 檢查是否有ReserveSeat
		Integer reserveSeat = reserveDTO.getReserveSeat();
		if(reserveSeat==null || reserveSeat <= 0) return "ReserveSeat is not validate";
		// 檢查是否有訂位日期
		LocalDate checkDate = reserveDTO.getCheckDate();
		if(checkDate==null) return "CheckDate is not validate";
		// 檢查是否有訂位時間
		LocalTime startTime = reserveDTO.getStartTime();
		if(startTime==null) return "StartTime is not validate";
		
		return null;
	}
	
	// 後台修改訂位檢查傳入參數
	public String validateSetReserveDto(ReserveDTO reserveDTO) {
		
		// 檢查是否有ReserveId
		Integer reserveId = reserveDTO.getReserveId();
		if(reserveId==null || reserveId <= 0) return "ReserveId is not validate";
		// 檢查是否有MemberId
		Integer memberId = reserveDTO.getMemberId();
		if(memberId==null || memberId <= 0) return "MemberId is not validate";
		// 檢查是否有RestaurantId
		Integer restaurantId = reserveDTO.getRestaurantId();
		if(restaurantId==null || restaurantId <= 0) return "RestaurantId is not validate";
		// 檢查是否有ReserveSeat
		Integer reserveSeat = reserveDTO.getReserveSeat();
		if(reserveSeat==null || reserveSeat <= 0) return "ReserveSeat is not validate";
		// 檢查是否有訂位日期
		LocalDate checkDate = reserveDTO.getCheckDate();
		if(checkDate==null) return "CheckDate is not validate";
		// 檢查是否有訂位時間
		LocalTime startTime = reserveDTO.getStartTime();
		if(startTime==null) return "StartTime is not validate";
		
		return null;
	}
	
	
	
	// 更新訂位
	@Transactional
	public Reserve updateReserveByEntity(Reserve reserve) {
		Optional<Reserve> optional = reserveRepository.findById(reserve.getReserveId());
		if(optional.isPresent()) {
			return reserveRepository.save(reserve);
		}
		return null;
	}
	
	
	
	// 刪除訂位
	public void deleteReserve(Integer reserveId) {
		reserveRepository.deleteById(reserveId);
	}
	
	
	// 查詢訂位byId
	public Reserve findReserveById(Integer reserveId) {
		return reserveRepository.findById(reserveId).orElse(null);
	}

	
	// 查詢所有訂位
	public List<Reserve> findAllReserves() {
		return reserveRepository.findAll();
	}
	
	
	// 查詢會員所有訂位
	public List<Reserve> findByMember(Member member) {
		return reserveRepository.findByMemberOrderByReserveTimeDesc(member);
	}
	
	
	
	// 查詢訂位by可變條件
	public List<Reserve> findReserveByCriteria(String memberName, String phone, Integer restaurantId, String tableTypeId, LocalDate reserveTimeStart, LocalDate reserveTimeEnd){
		return reserveRepository.findReserveByCriteria(memberName, phone, restaurantId, tableTypeId, reserveTimeStart, reserveTimeEnd);
	}
	
	
	// 查詢餐廳該天所有時段特定桌位種類的預約狀況
    public List<ReserveCheckDTO> getReserveCheck(Integer restaurantId, String tableTypeId, LocalDate checkDate) {
    	
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));

        // 根據餐廳的營業時間與每個時間段的用餐限制，計算出時間段
        List<TimeSlotDTO> timeSlots = generateTimeSlots(restaurant);
        List<ReserveCheckDTO> reserveChecks = new ArrayList<>();

        for (TimeSlotDTO timeSlot : timeSlots) {
            // 使用自定義的查詢方法，查詢每個時間段的預訂數量與總桌數
            List<Reserve> reservationsInTimeSlot = reserveRepository.getReservationsInTimeSlot(restaurantId, checkDate, timeSlot.getSlotStar(), timeSlot.getSlotEnd());
            
            // 計算該時間段已預訂的特定桌子種類的數量
            Integer reservedTableCount = (int) reservationsInTimeSlot.stream()
                .flatMap(reservation -> reservation.getRestaurantTables().stream())
                .filter(table -> table.getTableType().getTableTypeId().equals(tableTypeId))
                .count();
            
            Integer totalTableCount = reserveRepository.countAvailableTables(restaurantId, tableTypeId);
            if(totalTableCount==null) totalTableCount=0;
            // 設定開放訂位的桌數比例
            totalTableCount = (Integer) (totalTableCount * restaurant.getReservePercent() / 100);
            ReserveCheckDTO bean = new ReserveCheckDTO(timeSlot.getSlotStar(), timeSlot.getSlotEnd(), totalTableCount, reservedTableCount);
            reserveChecks.add(bean);
        }

        return reserveChecks;
    }
    
    
    // 查詢餐廳該天所有時段特定桌位種類的預約狀況
    public List<ReserveCheckDTO> getCombinedReserveCheck(Integer restaurantId, Integer reserveSeat, LocalDate checkDate) {
    	
    	Restaurant restaurant = restaurantRepository.findById(restaurantId)
    			.orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
    	
    	// 根據餐廳的營業時間與每個時間段的用餐限制，計算出時間段
    	List<TimeSlotDTO> timeSlots = generateTimeSlots(restaurant);
    	List<ReserveCheckDTO> reserveChecks = new ArrayList<>();
    	
    	for (TimeSlotDTO timeSlot : timeSlots) {
    		
    		// 取得該餐廳想預定的桌子種類所有桌子(只有可預訂的)
    		List<RestaurantTable> availableTables = restaurantTableService.findTableByRestaurantAndStatus(restaurantId);
    		
    		// 該訂位時段所有訂位訂單
    		List<Reserve> reservations = reserveRepository.getReservationsInTimeSlot(
    		    restaurantId,
    		    checkDate,
    		    timeSlot.getSlotStar(),
    		    timeSlot.getSlotEnd()
    		);

    		// 收集所有已被預訂的桌子Id
    		Set<RestaurantTableId> reservedTableIds = reservations.stream()
    		    .flatMap(reservation -> reservation.getRestaurantTables().stream().map(RestaurantTable::getId))
    		    .collect(Collectors.toSet());

    		// 過濾掉已被預訂的桌子
    		List<RestaurantTable> finalAvailableTables = availableTables.stream()
    		    .filter(table -> !reservedTableIds.contains(table.getId())) // 排除已被預訂的桌子
    		    .filter(table -> table.getTableType().getTableTypeName() < reserveSeat + 2) // 排除大於訂位人數+2的桌子
    		    .collect(Collectors.toList());
    		
    		
    		// 計算剩餘桌子的可容納人數
    		Integer totalCapacity = finalAvailableTables.stream()
    		    .mapToInt(table -> table.getTableType().getTableTypeName()) // 取得桌子座位數
    		    .sum();
    		
    		// 設定開放訂位的桌數比例
    		totalCapacity = (Integer) (totalCapacity * restaurant.getReservePercent() / 100);
    		ReserveCheckDTO bean = new ReserveCheckDTO(timeSlot.getSlotStar(), timeSlot.getSlotEnd(), totalCapacity, reserveSeat);
    		
    		reserveChecks.add(bean);
    	}
    	
    	return reserveChecks;
    }

    
    
    // 計算餐廳的所有可用時段
    private List<TimeSlotDTO> generateTimeSlots(Restaurant restaurant) {
        List<TimeSlotDTO> timeSlots = new ArrayList<>();
        LocalTime slotStart = restaurant.getRestaurantOpentime();
        LocalTime slotEnd;

        while (slotStart.isBefore(restaurant.getRestaurantClosetime())) {
            slotEnd = slotStart.plusMinutes(restaurant.getEattime());
            timeSlots.add(new TimeSlotDTO(slotStart, slotEnd));
            slotStart = slotStart.plusMinutes(restaurant.getReserveTimeScale());  // 訂位的時間區間
        }

        return timeSlots;
    }
    
    
    
    // 依照預定人數查詢訂位的桌位種類ID
    public String getTableTypeIdByReserveSeat(Integer reserveSeat) {
		
    	List<TableType> tableTypes = tableTypeService.findAllTableType();
    	
    	for(TableType tableType : tableTypes) {
    		if(reserveSeat == tableType.getTableTypeName()) {
    			return tableType.getTableTypeId();
    		}
    	}
    	
    	for(TableType tableType : tableTypes) {
    		if(reserveSeat+1 == tableType.getTableTypeName()) {
    			return tableType.getTableTypeId();
    		}
    	}
    	
    	return null;
	}
    
    
    
    
    // 訂位統計
    public Map<String, Integer> getReserveSum(LocalDate slotStartDate, LocalDate slotEndDate) {

        // 如果沒有傳入開始日期，則設置為當年的第一天
        if (slotStartDate == null) {
            Year currentYear = Year.now();
            slotStartDate = currentYear.atDay(1); // 當年1月1日
        }

        // 如果沒有傳入結束日期，則設置為當年的最後一天
        if (slotEndDate == null) {
            Year currentYear = Year.now();
            slotEndDate = currentYear.atMonth(12).atEndOfMonth(); // 當年12月31日
        }

        List<Restaurant> restaurants = restaurantRepository.findAll();
        Map<String, Integer> restaurantReserveMap = new HashMap<>();

        for (Restaurant restaurant : restaurants) {
            Integer restaurantId = restaurant.getRestaurantId();
            Integer countReservationsInDate = reserveRepository.countReservationsInDateSlot(restaurantId, slotStartDate, slotEndDate);
            restaurantReserveMap.put(restaurant.getRestaurantName(), countReservationsInDate);
        }

        return restaurantReserveMap;
    }

    
    // 訂位統計
    public List<Integer> getReserveInMonth(Integer year) {
    	if (year == null) year = 2024;
        List<Integer> reservationCounts = new ArrayList<>();
        // 獲取每個月份的訂位數量
        for (Month month : Month.values()) {
            // 計算每個月的第一天和最後一天
            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDate monthEnd = LocalDate.of(year, month, month.length(year % 4 == 0)); // 考慮閏年
            // 獲取該月的預訂數量
            Integer count = reserveRepository.countReservationsInMonth(monthStart, monthEnd);
            reservationCounts.add(count);
        }
        return reservationCounts;
    }
    
    
    // 訂位統計(人數統計)
    public Map<String, Integer> getReserveBySeat(Integer restaurantId) {

    	Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
    	Map<String, Integer> restaurantReserveMap = new LinkedHashMap<>();
    	for(int i = restaurant.getReserveMin() ; i<=restaurant.getReserveMax() ; i++) {
    		Integer countReservationsBySeat = reserveRepository.countReservationsBySeat(restaurantId, i);
    		restaurantReserveMap.put(i+"人", countReservationsBySeat);
    	}
        return restaurantReserveMap;
    }
    
    
    // 訂位統計(星期統計)
    public Map<String, Integer> getReserveByWeekDay(Integer restaurantId) {
    	
    	Map<String, Integer> restaurantReserveMap = new LinkedHashMap<>();
        // 將數字對應到中文星期
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        for (int i = 1; i <= 7; i++) {
            Integer countReservationsBySeat = reserveRepository.countReservationsByWeekDay(restaurantId, i);
            // 使用對應的中文星期
            restaurantReserveMap.put(weekDays[i - 1], countReservationsBySeat);
        }
    	return restaurantReserveMap;
    }
    
    
    
    
    
    
    // 寄發訂位成功通知信
	public void sendMail(Reserve reserve) throws MessagingException, TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper  helper = new MimeMessageHelper(mimeMessage,true);
		//設置mail
		helper.setFrom(mailConfig.getUserName3());//誰寄信(application設定的信箱)
		helper.setTo(reserve.getMember().getEmail());//誰收信
		helper.setSubject("【☕訂位成功通知】您在 starcups "+ reserve.getRestaurant().getRestaurantName() +" 的預訂已經完成");//主旨
		
		//設置模板
		//設置model
		Map<String, Object> model = new HashMap<String,Object>();
		//透過model傳入的物件("參數名","東西")
		model.put("reserve",reserve);
		//get模板，並將modal傳入模板
		String templateString = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfig.getTemplate("reserveMail.html"), model);
		
		//設置mail內文
		helper.setText(templateString,true);
		
		//設置資源，順序要在內文之後
		FileSystemResource file = new FileSystemResource(new File("src/main/resources/static/images/mail/logo-starcups.png"));
		helper.addInline("logo",file);
		
		mailSender.send(mimeMessage);	
	}
	
	

}
