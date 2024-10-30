package com.eatspan.SpanTasty.utils.order;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PickupTime {
	// 獲取取餐時間選項
    public List<LocalTime> generatePickupTimes(LocalTime closingTime) {
        List<LocalTime> pickupTimes = new ArrayList<>();
        LocalTime currentTime = LocalTime.now();

        // 設置取餐時間的開始時間為當前時間加上 15 分鐘後的最近的整十分鐘
        LocalTime startTime = currentTime.plusMinutes(15); // 當前時間加上 15 分鐘
        startTime = startTime.plusMinutes((10 - (startTime.getMinute() % 10)) % 10); // 無條件進位到最近的整十分鐘

        // 生成取餐時間選項，直到結束營業時間
        while (!startTime.isAfter(closingTime)) {
            pickupTimes.add(startTime); // 將時間轉為字符串並添加到列表
            startTime = startTime.plusMinutes(10); // 每次增加十分鐘
        }

        return pickupTimes; // 返回生成的取餐時間選項
    }
}
