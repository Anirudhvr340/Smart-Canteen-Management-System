package com.scms.service;

import com.scms.model.enums.OrderStatus;
import com.scms.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepo;

    public double getTodayRevenue() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Double r = orderRepo.sumRevenueByDateRange(start, end);
        return r != null ? r : 0.0;
    }

    public double getMonthRevenue() {
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        Double r = orderRepo.sumRevenueByDateRange(start, end);
        return r != null ? r : 0.0;
    }

    public long getTodayOrderCount() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        Long c = orderRepo.countByDateRange(start, start.plusDays(1));
        return c != null ? c : 0;
    }

    public long getTodayCancelCount() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        Long c = orderRepo.countByStatusAndDateRange(OrderStatus.CANCELLED, start, start.plusDays(1));
        return c != null ? c : 0;
    }

    public Map<String, Double> getLast7DaysRevenue() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime s = day.atStartOfDay();
            Double r = orderRepo.sumRevenueByDateRange(s, s.plusDays(1));
            result.put(day.toString(), r != null ? r : 0.0);
        }
        return result;
    }

    public Map<String, Long> getOrdersByStatus() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values())
            m.put(s.name(), orderRepo.countByStatus(s));
        return m;
    }
}
