package com.eatspan.SpanTasty.utils.reservation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class LogAspect {

    private static final String DIRECTORY_PATH = "C:/SpringBoot/export";
    private static final String LOG_FILE_PATH = DIRECTORY_PATH + "/reserve-aoplog.csv";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    @Pointcut(value = "execution(* com.eatspan.SpanTasty.controller.reservation..*(..))")
    private void pointcut1() {}

    @Around(value = "pointcut1()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        ensureDirectoryExists(DIRECTORY_PATH); // 確保目錄存在，否則建立
        addHeaderIfNeeded(); // 如果檔案是空的，新增標題列

        LocalDateTime startTime = LocalDateTime.now(); // 記錄開始時間
        String formattedStartTime = startTime.format(DATE_FORMATTER);
        String methodName = joinPoint.getSignature().getName();
        String requestUri = ""; // 初始化請求 URI 變數

        if (RequestContextHolder.getRequestAttributes() != null) {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            requestUri = request.getRequestURI(); // 獲取請求 URI
        }

        Object result = null;
        String status = "Success";
        try {
            result = joinPoint.proceed(); // 執行原始方法
        } catch (Throwable e) {
            status = "Error: " + e.toString();
            throw e; // 再次拋出例外
        } finally {
            // 結束後統一記錄時間和狀態
            LocalDateTime endTime = LocalDateTime.now(); // 記錄結束時間
            long executionTime = Duration.between(startTime, endTime).toMillis(); // 計算執行時間
            logToCsv(formattedStartTime, methodName, requestUri, status, String.valueOf(executionTime)); // 記錄到 CSV
        }
        return result;
    }

    private void logToCsv(String timestamp, String method, String requestUri, String status, String executionTime) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {
            writer.write(String.format("%s,%s,%s,%s,%s", timestamp, method, requestUri, status, executionTime)); // 記錄請求 URI
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ensureDirectoryExists(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path); // 如果資料夾不存在則自動建立
        }
    }

    private void addHeaderIfNeeded() throws IOException {
        Path path = Paths.get(LOG_FILE_PATH);
        if (Files.notExists(path) || Files.size(path) == 0) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {
                writer.write("開始時間,方法名稱,請求URI,結果,執行時間(ms)"); // 添加請求 URI 標題
                writer.newLine();
            }
        }
    }
}
