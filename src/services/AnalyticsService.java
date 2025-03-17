package services;

import database.DatabaseHelper;
import models.AnalyticsData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsService {

    public List<AnalyticsData> getAnalyticsData(LocalDate fromDate, LocalDate toDate, List<String> categories) {
        List<AnalyticsData> analyticsData = new ArrayList<>();

        boolean selectAll = categories.contains("ALL");
        String categoryFilter = selectAll ? "" :
                "AND c.category_name IN (" +
                        String.join(",", categories.stream().map(c -> "?").toArray(String[]::new)) +
                        ")";

        // Updated ORDER BY clause: order by category name ascending and then by total_quantity descending.
        String query = "SELECT p.item_number, p.label, c.category_name, p.price AS cost, s.price AS retail, " +
                "SUM(s.quantity) AS total_quantity, " +
                "ROUND(SUM(s.quantity * p.price), 2) AS total_cost, " +
                "ROUND(SUM(s.quantity * s.price), 2) AS total_retail " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "JOIN Category c ON p.category_id = c.id " +
                "WHERE (s.from_date >= ? AND s.to_date <= ?) " +
                categoryFilter + " " +
                "GROUP BY p.item_number, p.label, c.category_name, p.price " +
                "ORDER BY c.category_name ASC, total_quantity DESC";

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
                        rs.getDouble("cost"),
                        rs.getDouble("retail"),
                        rs.getDouble("total_cost"),
                        rs.getDouble("total_retail"),
                        rs.getInt("total_quantity")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching analytics data: " + e.getMessage());
        }

        // Group the product-level rows by category and add a subtotal row for each category.
        List<AnalyticsData> finalData = new ArrayList<>();
        Map<String, List<AnalyticsData>> grouped = analyticsData.stream()
                .collect(Collectors.groupingBy(AnalyticsData::getCategory));

        // Sort the category names in ascending order.
        List<String> sortedCategories = new ArrayList<>(grouped.keySet());
        sortedCategories.sort(String::compareTo);

        for (String cat : sortedCategories) {
            List<AnalyticsData> group = grouped.get(cat);
            // Ensure each group is sorted by quantity descending.
            group.sort((a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()));
            // Add all product rows for this category.
            finalData.addAll(group);

            // Compute subtotals for the category.
            int subtotalQty = group.stream().mapToInt(AnalyticsData::getQuantity).sum();
            double subtotalTotalCost = group.stream().mapToDouble(AnalyticsData::getTotalCost).sum();
            double subtotalTotalRetail = group.stream().mapToDouble(AnalyticsData::getTotalRetail).sum();
            // Create a subtotal row (itemNumber = -1, label = "Sub Total").
            double roundedSubtotalTotalCost = new BigDecimal(subtotalTotalCost)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
            double roundedSubtotalTotalRetail = new BigDecimal(subtotalTotalRetail)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
            AnalyticsData subtotal = new AnalyticsData(-1, "Sub Total", cat, 0.0, 0.0, roundedSubtotalTotalCost, roundedSubtotalTotalRetail, subtotalQty);
            finalData.add(subtotal);
        }

        return finalData;
    }

    public List<AnalyticsData> getWeeklyAnalyticsData(LocalDate fromDate, LocalDate toDate, List<String> categories, List<String> weekRanges) {
        List<AnalyticsData> analyticsDataList = new ArrayList<>();

        boolean selectAll = categories.contains("ALL");
        String categoryFilter = selectAll ? "" :
                "AND c.category_name IN (" +
                        String.join(",", categories.stream().map(c -> "?").toArray(String[]::new)) +
                        ")";

        String query = "SELECT p.item_number, p.label, c.category_name, p.price as cost, s.price as retail, " +
                "s.quantity, s.from_date, s.to_date, " +
                "ROUND(SUM(s.quantity * p.price), 2) AS total_cost, " +
                "ROUND(SUM(s.quantity * s.price), 2) AS total_retail " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "JOIN Category c ON p.category_id = c.id " +
                "WHERE (s.from_date >= ? AND s.to_date <= ?) " + categoryFilter + " " +
                "GROUP BY p.item_number, p.label, c.category_name, p.price, s.quantity, s.from_date, s.to_date " +
                "ORDER BY s.from_date, p.category_id, total_quantity DESC";

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
                double cost = rs.getDouble("cost");
                double retail = rs.getDouble("retail");
                double total_cost = rs.getDouble("total_cost");
                double total_retail = rs.getDouble("total_retail");
                int quantity = rs.getInt("quantity");
                LocalDate saleStartDate = rs.getDate("from_date").toLocalDate();
                LocalDate saleEndDate = rs.getDate("to_date").toLocalDate();

                // Get week-wise breakdown
                Map<String, Integer> weeklySales = calculateWeeklySales(saleStartDate, saleEndDate, quantity, weekRanges);

                if (dataMap.containsKey(itemNumber)) {
                    dataMap.get(itemNumber).mergeWeeklySales(weeklySales);
                } else {
                    AnalyticsData analyticsData = new AnalyticsData(itemNumber, label, category, cost, retail, total_cost, total_retail, quantity);
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

    public Map<String, Integer> getCategoryQuantityDistribution(LocalDate fromDate, LocalDate toDate, String category) {
        Map<String, Integer> distribution = new HashMap<>();
        String query = "SELECT p.label, SUM(s.quantity) as totalQty " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "JOIN Category c ON p.category_id = c.id " +
                "WHERE s.from_date >= ? AND s.to_date <= ? AND c.category_name = ? " +
                "GROUP BY p.label";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, java.sql.Date.valueOf(fromDate));
            pstmt.setDate(2, java.sql.Date.valueOf(toDate));
            pstmt.setString(3, category);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String label = rs.getString("label");
                int totalQty = rs.getInt("totalQty");
                distribution.put(label, totalQty);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching category quantity distribution: " + e.getMessage());
        }
        return distribution;
    }

    public Map<String, Map<String, Integer>> getSalesTrendByLabel(LocalDate fromDate, LocalDate toDate, String category) {
        Map<String, Map<String, Integer>> trendData = new HashMap<>();
        String query = "SELECT p.label, s.from_date, SUM(s.quantity) as qty " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "JOIN Category c ON p.category_id = c.id " +
                "WHERE s.from_date >= ? AND s.to_date <= ? AND c.category_name = ? " +
                "GROUP BY p.label, s.from_date " +
                "ORDER BY p.label, s.from_date";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, java.sql.Date.valueOf(fromDate));
            pstmt.setDate(2, java.sql.Date.valueOf(toDate));
            pstmt.setString(3, category);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String label = rs.getString("label");
                LocalDate saleDate = rs.getDate("from_date").toLocalDate();
                int qty = rs.getInt("qty");
                String dateStr = saleDate.toString();
                Map<String, Integer> dateMap = trendData.getOrDefault(label, new HashMap<>());
                dateMap.put(dateStr, qty);
                trendData.put(label, dateMap);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching sales trend by label: " + e.getMessage());
        }
        return trendData;
    }

    public Map<String, Integer> getQuantityByCategory(LocalDate fromDate, LocalDate toDate, List<String> categories) {
        Map<String, Integer> distribution = new HashMap<>();
        boolean selectAll = categories.contains("ALL");
        String categoryFilter = "";
        if (!selectAll) {
            categoryFilter = "AND c.category_name IN (" +
                    String.join(",", categories.stream().map(c -> "?").toArray(String[]::new)) +
                    ")";
        }

        String query = "SELECT c.category_name, SUM(s.quantity) AS totalQty " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "JOIN Category c ON p.category_id = c.id " +
                "WHERE s.from_date >= ? AND s.to_date <= ? " +
                categoryFilter + " " +
                "GROUP BY c.category_name";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, java.sql.Date.valueOf(fromDate));
            pstmt.setDate(2, java.sql.Date.valueOf(toDate));

            int paramIndex = 3;
            if (!selectAll) {
                for (String cat : categories) {
                    pstmt.setString(paramIndex++, cat);
                }
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String categoryName = rs.getString("category_name");
                int totalQty = rs.getInt("totalQty");
                distribution.put(categoryName, totalQty);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching quantity by category: " + e.getMessage());
        }
        return distribution;
    }

}
