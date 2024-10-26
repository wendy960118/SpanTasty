package com.eatspan.SpanTasty.service.reservation;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.eatspan.SpanTasty.repository.reservation.TableTypeRepository;
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
	private TableTypeRepository tableTypeRepository;
	
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
//	public Reserve addReserve(ReserveDTO reserveDTO) {
//		
//	    Reserve reserve = new Reserve();
//	    
//	    reserve.setReserveSeat(reserveDTO.getReserveSeat());
//	    reserve.setReserveTime(reserveDTO.getCheckDate().atTime(reserveDTO.getStartTime()));
//	    // 設定member外鍵關聯
//	    Member member = memberService.findMemberById(reserveDTO.getMemberId()).get();
//	    reserve.setMember(member);
//	    // 設定restaurant外鍵關聯
//	    Restaurant restaurant = restaurantService.findRestaurantById(reserveDTO.getRestaurantId());
//	    reserve.setRestaurant(restaurant);
//	    // 根據reserveSeat取得訂位桌子種類
//	    String tableTypeId = getTableTypeIdByReserveSeat(reserve.getReserveSeat());
//	    // 設定tableType外鍵關聯
//	    TableType tableType = tableTypeService.findTableTypeById(tableTypeId);
//	    reserve.setTableType(tableType);
//	    reserve.setReserveNote(reserveDTO.getReserveNote());
//		reserve.setFinishedTime(reserve.getReserveTime().plusMinutes(reserve.getRestaurant().getEattime()));
//		
//		return reserveRepository.save(reserve);
//	}
	
	
	// 新增訂位
	public Reserve addReserve(ReserveDTO reserveDTO) {
	    
	    Reserve reserve = new Reserve();
	    
	    reserve.setReserveSeat(reserveDTO.getReserveSeat());
	    reserve.setReserveTime(reserveDTO.getCheckDate().atTime(reserveDTO.getStartTime()));
	    reserve.setReserveNote(reserveDTO.getReserveNote());
	    
	    // 設定member外鍵關聯
	    Member member = memberService.findMemberById(reserveDTO.getMemberId()).get();
	    reserve.setMember(member);
	    
	    // 設定restaurant外鍵關聯
	    Restaurant restaurant = restaurantService.findRestaurantById(reserveDTO.getRestaurantId());
	    reserve.setRestaurant(restaurant);
	    
	    reserve.setFinishedTime(reserve.getReserveTime().plusMinutes(reserve.getRestaurant().getEattime()));
	    
	    
	    // 根據reserveSeat取得訂位桌子種類
	    String tableTypeId = getTableTypeIdByReserveSeat(reserve.getReserveSeat());
	    
	    // 取得該餐廳想預定的桌子種類所有桌子(只有可預訂的)
	    List<RestaurantTable> availableTables = restaurantTableService.findTableIdByRestaurantAndTypeAndStatus(reserveDTO.getRestaurantId(), tableTypeId);
	    
	    // 該訂位時段所有訂位訂單
	    List<Reserve> reservations = reserveRepository.getReservationsInTimeSlot(
	        reserveDTO.getRestaurantId(), 
	        reserveDTO.getCheckDate(), 
	        reserveDTO.getStartTime(), 
	        reserveDTO.getStartTime().plusMinutes(reserve.getRestaurant().getEattime())
	    );

	    // 收集所有已被預訂的桌子
	    Set<RestaurantTableId> reservedTableIds = new HashSet<>();
	    for (Reserve reservation : reservations) {
	        for (RestaurantTable restaurantTable : reservation.getRestaurantTables()) {
	            reservedTableIds.add(restaurantTable.getId());
	        }
	    }
	    
	    // 過濾掉已被預訂的桌子
	    List<RestaurantTable> finalAvailableTables = availableTables.stream()
	        .filter(table -> !reservedTableIds.contains(table.getId()))
	        .collect(Collectors.toList());

	    // 如果沒有可用的桌子，可以處理錯誤或返回特定的響應
	    if (finalAvailableTables.isEmpty()) {
	    }
	    
	    // 選擇第一張可用桌子（或根據需求選擇）
	    RestaurantTable selectedTable = finalAvailableTables.get(0);
	    
	    System.out.println(selectedTable.getId().getRestaurantId());
	    System.out.println(selectedTable.getId().getTableTypeId());
	    System.out.println(selectedTable.getId().getTableId());
	    
	    reserve.setRestaurantTables(Collections.singletonList(selectedTable)); // 設定只選擇一張桌子
	    selectedTable.setReserves(Collections.singletonList(reserve));
	    
	    List<RestaurantTable> restaurantTables = reserve.getRestaurantTables();
	    for(RestaurantTable rTable : restaurantTables) {
	    	System.out.println(rTable.getId().getRestaurantId());
	    	System.out.println(rTable.getId().getTableTypeId());
	    	System.out.println(rTable.getId().getTableId());
	    	System.out.println(rTable.getReserves());
	    }
	    return reserveRepository.save(reserve);
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
	public Reserve updateReservebyDto(ReserveDTO reserveDTO) {
		
//	    Reserve reserve = findReserveById(reserveDTO.getReserveId());
//	    
//	    reserve.setReserveSeat(reserveDTO.getReserveSeat());
//	    reserve.setReserveTime(reserveDTO.getCheckDate().atTime(reserveDTO.getStartTime()));
//	    
//	    // 設定member外鍵關聯
//	    Member member = memberService.findMemberById(reserveDTO.getMemberId()).get();
//	    reserve.setMember(member);
//	    // 設定restaurant外鍵關聯
//	    Restaurant restaurant = restaurantService.findRestaurantById(reserveDTO.getRestaurantId());
//	    reserve.setRestaurant(restaurant);
//	    // 根據reserveSeat取得訂位桌子種類
//	    String tableTypeId = getTableTypeIdByReserveSeat(reserve.getReserveSeat());
//	    // 設定tableType外鍵關聯
//	    TableType tableType = tableTypeService.findTableTypeById(tableTypeId);
//	    reserve.setTableType(tableType);
//	    reserve.setReserveNote(reserveDTO.getReserveNote());
//	    reserve.setReserveStatus(reserveDTO.getReserveStatus());
//		
//	    reserve.setFinishedTime(reserve.getReserveTime().plusMinutes(reserve.getRestaurant().getEattime()));
//		return reserveRepository.save(reserve);
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
	public List<Reserve> findReserveByCriteria(String memberName, String phone, Integer restaurantId, LocalDate reserveTimeStart, LocalDate reserveTimeEnd){
		return reserveRepository.findReserveByCriteria(memberName, phone, restaurantId, reserveTimeStart, reserveTimeEnd);
	}
	
	
	
	
	
	
//	// 查詢餐廳該天所有時段特定桌位種類的預約狀況
//	public List<ReserveCheckDTO> getReserveCheck(Integer restaurantId, String tableTypeId, LocalDate checkDate) {
//		
//		Restaurant restaurant = restaurantRepository.findById(restaurantId)
//				.orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
//		
//		// 根據餐廳的營業時間與每個時間段的用餐限制，計算出時間段
//		List<TimeSlotDTO> timeSlots = generateTimeSlots(restaurant);
//		List<ReserveCheckDTO> reserveChecks = new ArrayList<>();
//		
//		for (TimeSlotDTO timeSlot : timeSlots) {
//			// 使用自定義的查詢方法，查詢每個時間段的預訂數量與總桌數
//			Integer reservedTableCount = reserveRepository.countReservationsInTimeSlot(restaurantId, tableTypeId, checkDate, timeSlot.getSlotStar(), timeSlot.getSlotEnd());
//			Integer totalTableCount = reserveRepository.countAvailableTables(restaurantId, tableTypeId);
//			if(totalTableCount==null) totalTableCount=0;
//			// 設定開放訂位的桌數比例
//			totalTableCount = (Integer) (totalTableCount * restaurant.getReservePercent() / 100);
//			ReserveCheckDTO bean = new ReserveCheckDTO(timeSlot.getSlotStar(), timeSlot.getSlotEnd(), totalTableCount, reservedTableCount);
//			reserveChecks.add(bean);
//		}
//		
//		return reserveChecks;
//	}
	
	// 查詢餐廳該天所有時段特定桌位種類的預約狀況
    public List<ReserveCheckDTO> getReserveCheck(Integer restaurantId, String tableTypeId, LocalDate checkDate) {
    	
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));

        // 根據餐廳的營業時間與每個時間段的用餐限制，計算出時間段
        List<TimeSlotDTO> timeSlots = generateTimeSlots(restaurant);
        List<ReserveCheckDTO> reserveChecks = new ArrayList<>();

        for (TimeSlotDTO timeSlot : timeSlots) {
            // 使用自定義的查詢方法，查詢每個時間段的預訂數量與總桌數
//            Integer reservedTableCount = reserveRepository.countReservationsInTimeSlot(restaurantId, tableTypeId, checkDate, timeSlot.getSlotStar(), timeSlot.getSlotEnd());
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
		
    	List<TableType> tableTypes = tableTypeRepository.findAll();
    	
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
