package services;

import database.DatabaseHelper;
import models.AnalyticsData;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnalyticsService {
    public List<AnalyticsData> getAnalyticsData(LocalDate fromDate, LocalDate toDate, List<String> categories) {
        List<AnalyticsData> analyticsData = new ArrayList<>();

        boolean selectAll = categories.contains("ALL");
        String categoryFilter = selectAll ? "" :
                "AND c.category_name IN (" + String.join(",", categories.stream().map(c -> "?").toArray(String[]::new)) + ")";

        String query = "SELECT p.item_number, p.label, c.category_name, p.price, " +
                "SUM(s.quantity) AS total_quantity, " +
                "ROUND(SUM(s.quantity * p.price), 2) AS total_sales " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "JOIN Category c ON p.category_id = c.id " +
                "WHERE (s.from_date >= ? AND s.to_date <= ?) " +
                categoryFilter +
                "GROUP BY p.item_number, p.label, c.category_name, p.price " +
                "ORDER BY total_quantity DESC";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, java.sql.Date.valueOf(fromDate));
            pstmt.setDate(2, java.sql.Date.valueOf(toDate));

            int paramIndex = 3;
            if (!selectAll) {
                for (String category : categories) {
                    pstmt.setString(paramIndex++, category);
                }
            }

            System.out.println("Executing Query: " + query);
            System.out.println("With Parameters: " + categories);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                analyticsData.add(new AnalyticsData(
                        rs.getInt("item_number"),
                        rs.getString("label"),
                        rs.getString("category_name"),
                        rs.getDouble("price"),
                        rs.getInt("total_quantity"),
                        rs.getDouble("total_sales")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching analytics data: " + e.getMessage());
        }
        return analyticsData;
    }

    public List<AnalyticsData> getWeeklyAnalyticsData(LocalDate fromDate, LocalDate toDate, List<String> categories, List<String> weekRanges) {
        List<AnalyticsData> analyticsDataList = new ArrayList<>();

        boolean selectAll = categories.contains("ALL");
        String categoryFilter = selectAll ? "" :
                "AND c.category_name IN (" + String.join(",", categories.stream().map(c -> "?").toArray(String[]::new)) + ")";

        String query = "SELECT p.item_number, p.label, c.category_name, p.price, s.quantity, s.from_date, s.to_date " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "JOIN Category c ON p.category_id = c.id " +
                "WHERE (s.from_date >= ? AND s.to_date <= ?) " + categoryFilter +
                "ORDER BY p.item_number, s.from_date";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, java.sql.Date.valueOf(fromDate));
            pstmt.setDate(2, java.sql.Date.valueOf(toDate));

            int paramIndex = 3;
            if (!selectAll) {
                for (String category : categories) {
                    pstmt.setString(paramIndex++, category);
                }
            }

            ResultSet rs = pstmt.executeQuery();

            Map<Integer, AnalyticsData> dataMap = new HashMap<>();

            while (rs.next()) {
                int itemNumber = rs.getInt("item_number");
                String label = rs.getString("label");
                String category = rs.getString("category_name");
                double price = rs.getDouble("price");
                int quantity = rs.getInt("quantity");
                LocalDate saleStartDate = rs.getDate("from_date").toLocalDate();
                LocalDate saleEndDate = rs.getDate("to_date").toLocalDate();

                // Get week-wise breakdown
                Map<String, Integer> weeklySales = calculateWeeklySales(saleStartDate, saleEndDate, quantity, weekRanges);

                // If item exists, merge data
                if (dataMap.containsKey(itemNumber)) {
                    dataMap.get(itemNumber).mergeWeeklySales(weeklySales);
                } else {
                    AnalyticsData analyticsData = new AnalyticsData(itemNumber, label, category, price, 0, 0.0);
                    analyticsData.setWeeklySales(weeklySales);
                    dataMap.put(itemNumber, analyticsData);
                }
            }

            analyticsDataList.addAll(dataMap.values());

        } catch (SQLException e) {
            System.err.println("Error fetching weekly analytics data: " + e.getMessage());
        }
        return analyticsDataList;
    }

    private Map<String, Integer> calculateWeeklySales(LocalDate saleStart, LocalDate saleEnd, int totalQuantity, List<String> weekRanges) {
        Map<String, Integer> weeklySales = new HashMap<>();

        // Split total quantity evenly across weeks (simple distribution)
        long totalDays = ChronoUnit.DAYS.between(saleStart, saleEnd) + 1;
        int perDayQty = (totalDays > 0) ? totalQuantity / (int) totalDays : 0;

        for (String week : weekRanges) {
            String[] dates = week.split(" to ");
            LocalDate weekStart = LocalDate.parse(dates[0]);
            LocalDate weekEnd = LocalDate.parse(dates[1]);

            if (!(saleEnd.isBefore(weekStart) || saleStart.isAfter(weekEnd))) {
                long overlapDays = ChronoUnit.DAYS.between(
                        saleStart.isBefore(weekStart) ? weekStart : saleStart,
                        saleEnd.isAfter(weekEnd) ? weekEnd : saleEnd
                ) + 1;

                weeklySales.put(week, (int) (overlapDays * perDayQty));
            } else {
                weeklySales.put(week, 0);
            }
        }

        return weeklySales;
    }






}
