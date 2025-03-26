package services;

import database.DatabaseHelper;
import models.AnalyticsData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsService {

    public List<AnalyticsData> getAnalyticsData(LocalDate fromDate, LocalDate toDate,
                                                List<String> categories, List<String> subcategories) {

        List<AnalyticsData> analyticsData = new ArrayList<>();

        boolean selectAllCats = categories.contains("ALL");
        boolean selectAllSubcats = subcategories.contains("ALL");

        StringBuilder filterClause = new StringBuilder();
        List<String> queryParams = new ArrayList<>();

        if (!selectAllCats && !selectAllSubcats) {
            List<String> conditions = new ArrayList<>();
            for (String cat : categories) {
                for (String sub : subcategories) {
                    conditions.add("(p.category = ? AND p.subcategory = ?)");
                    queryParams.add(cat);
                    queryParams.add(sub);
                }
            }
            filterClause.append("AND (").append(String.join(" OR ", conditions)).append(")");
        } else if (!selectAllCats) {
            String placeholders = String.join(",", Collections.nCopies(categories.size(), "?"));
            filterClause.append("AND p.category IN (").append(placeholders).append(")");
            queryParams.addAll(categories);
        } else if (!selectAllSubcats) {
            String placeholders = String.join(",", Collections.nCopies(subcategories.size(), "?"));
            filterClause.append("AND p.subcategory IN (").append(placeholders).append(")");
            queryParams.addAll(subcategories);
        }

        String query = "SELECT p.item_number, p.label, p.category, p.subcategory, " +
                "p.price AS cost, s.price AS retail, " +
                "SUM(s.quantity) AS total_quantity, " +
                "ROUND(SUM(s.quantity * p.price), 2) AS total_cost, " +
                "ROUND(SUM(s.quantity * s.price), 2) AS total_retail " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "JOIN Category c on (c.category=p.category AND c.subcategory=p.subcategory) " +
                "WHERE (s.from_date >= ? AND s.to_date <= ?) " + filterClause + " " +
                "GROUP BY p.item_number " +
                "ORDER BY total_quantity DESC, p.label, p.category ASC, p.subcategory ASC";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, java.sql.Date.valueOf(fromDate));
            pstmt.setDate(2, java.sql.Date.valueOf(toDate));

            int paramIndex = 3;
            for (String param : queryParams) {
                pstmt.setString(paramIndex++, param);
            }

            System.out.println("Running SQL: " + query);
            System.out.println("With Params: " + queryParams);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                analyticsData.add(new AnalyticsData(
                        rs.getInt("item_number"),
                        rs.getString("label"),
                        rs.getString("category"),
                        rs.getString("subcategory"),
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


        List<AnalyticsData> finalData = new ArrayList<>();
        Map<String, List<AnalyticsData>> grouped = analyticsData.stream()
                .collect(Collectors.groupingBy(a -> a.getCategory() + " - " + a.getSubcategory()));

        for (String key : grouped.keySet().stream().sorted().toList()) {
            List<AnalyticsData> group = grouped.get(key);
            group.sort((a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()));
            finalData.addAll(group);

            int subtotalQty = group.stream().mapToInt(AnalyticsData::getQuantity).sum();
            double subtotalCost = group.stream().mapToDouble(AnalyticsData::getTotalCost).sum();
            double subtotalRetail = group.stream().mapToDouble(AnalyticsData::getTotalRetail).sum();

            finalData.add(new AnalyticsData(-1, "Sub Total",
                    group.get(0).getCategory(),
                    group.get(0).getSubcategory(),
                    0.0, 0.0,
                    BigDecimal.valueOf(subtotalCost).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                    BigDecimal.valueOf(subtotalRetail).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                    subtotalQty));
        }

        return finalData;
    }

    public static String buildQueryWithParams(String sql, List<Object> params) {
        for (Object param : params) {
            String value = (param instanceof String) ? "'" + param + "'" : String.valueOf(param);
            sql = sql.replaceFirst("\\?", value);
        }
        return sql;
    }

    public Map<String, Integer> getQuantityBySubcategory(LocalDate fromDate, LocalDate toDate, List<String> selectedPairs) {
        Map<String, Integer> distribution = new HashMap<>();

        boolean selectAll = selectedPairs.contains("ALL");
        StringBuilder whereClause = new StringBuilder();

        if (!selectAll && !selectedPairs.isEmpty()) {
            List<String> conditions = new ArrayList<>();
            for (int i = 0; i < selectedPairs.size(); i++) {
                conditions.add("(p.category = ? AND p.subcategory = ?)");
            }
            whereClause.append("AND (").append(String.join(" OR ", conditions)).append(")");
        }

        String query = "SELECT p.subcategory, SUM(s.quantity) AS totalQty " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "WHERE s.from_date >= ? AND s.to_date <= ? " +
                whereClause + " " +
                "GROUP BY p.subcategory";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, java.sql.Date.valueOf(fromDate));
            pstmt.setDate(2, java.sql.Date.valueOf(toDate));

            int paramIndex = 3;
            if (!selectAll && !selectedPairs.isEmpty()) {
                for (String pair : selectedPairs) {
                    String[] parts = pair.split(" - ");
                    pstmt.setString(paramIndex++, parts[0]); // category
                    pstmt.setString(paramIndex++, parts[1]); // subcategory
                }
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String subcategory = rs.getString("subcategory");
                int totalQty = rs.getInt("totalQty");
                distribution.put(subcategory, totalQty);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching subcategory quantity distribution: " + e.getMessage());
        }

        return distribution;
    }


    public Map<String, Map<String, Integer>> getSalesTrendByCategory(LocalDate fromDate, LocalDate toDate, String category) {
        Map<String, Map<String, Integer>> trendData = new HashMap<>();
        String query = "SELECT p.category, s.from_date, SUM(s.quantity) as qty " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "WHERE s.from_date >= ? AND s.to_date <= ? AND p.category = ? " +
                "GROUP BY p.category, s.from_date " +
                "ORDER BY p.category, s.from_date";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, java.sql.Date.valueOf(fromDate));
            pstmt.setDate(2, java.sql.Date.valueOf(toDate));
            pstmt.setString(3, category);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String cat = rs.getString("category");
                LocalDate date = rs.getDate("from_date").toLocalDate();
                int qty = rs.getInt("qty");

                trendData.computeIfAbsent(cat, k -> new HashMap<>())
                        .put(date.toString(), qty);
            }
        } catch (SQLException e) {
            System.err.println("Error in getSalesTrendByCategory: " + e.getMessage());
        }
        return trendData;
    }

    public Map<String, Map<String, Integer>> getSalesTrendBySubcategory(LocalDate fromDate, LocalDate toDate, String subcategory) {
        Map<String, Map<String, Integer>> trendData = new HashMap<>();
        String query = "SELECT p.subcategory, s.from_date, SUM(s.quantity) as qty " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "WHERE s.from_date >= ? AND s.to_date <= ? AND p.subcategory = ? " +
                "GROUP BY p.subcategory, s.from_date " +
                "ORDER BY p.subcategory, s.from_date";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, java.sql.Date.valueOf(fromDate));
            pstmt.setDate(2, java.sql.Date.valueOf(toDate));
            pstmt.setString(3, subcategory);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String subcat = rs.getString("subcategory");
                LocalDate date = rs.getDate("from_date").toLocalDate();
                int qty = rs.getInt("qty");

                trendData.computeIfAbsent(subcat, k -> new HashMap<>())
                        .put(date.toString(), qty);
            }
        } catch (SQLException e) {
            System.err.println("Error in getSalesTrendBySubcategory: " + e.getMessage());
        }
        return trendData;
    }


    public List<AnalyticsData> getWeeklyAnalyticsData(LocalDate fromDate, LocalDate toDate, List<String> categories, List<String> weekRanges) {
        List<AnalyticsData> analyticsDataList = new ArrayList<>();

        boolean selectAll = categories.contains("ALL");
        String categoryFilter = selectAll ? "" :
                "AND c.category IN (" +
                        String.join(",", categories.stream().map(c -> "?").toArray(String[]::new)) +
                        ")";

        String query = "SELECT p.item_number, p.label, c.category, p.price as cost, s.price as retail, " +
                "s.quantity, s.from_date, s.to_date, " +
                "ROUND(SUM(s.quantity * p.price), 2) AS total_cost, " +
                "ROUND(SUM(s.quantity * s.price), 2) AS total_retail " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "JOIN Category c ON p.category = c.category " +
                "WHERE (s.from_date >= ? AND s.to_date <= ?) " + categoryFilter + " " +
                "GROUP BY p.item_number, p.label, c.category, p.price, s.quantity, s.from_date, s.to_date " +
                "ORDER BY s.from_date, p.category, total_quantity DESC";

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
                String category = rs.getString("category");
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
                "JOIN Category c ON p.category = c.category " +
                "WHERE s.from_date >= ? AND s.to_date <= ? AND c.category = ? " +
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
                "JOIN Category c ON p.category = c.category " +
                "WHERE s.from_date >= ? AND s.to_date <= ? AND c.category = ? " +
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

    public Map<String, Integer> getQuantityByCategory(LocalDate fromDate, LocalDate toDate, List<String> selectedCategories) {
        Map<String, Integer> distribution = new HashMap<>();
        boolean selectAll = selectedCategories.contains("ALL");

        String categoryFilter = "";
        if (!selectAll) {
            categoryFilter = "AND p.category IN (" +
                    String.join(",", selectedCategories.stream().map(c -> "?").toArray(String[]::new)) +
                    ")";
        }

        String query = "SELECT p.category, SUM(s.quantity) AS totalQty " +
                "FROM Sales s " +
                "JOIN Product p ON s.item_number = p.item_number " +
                "WHERE s.from_date >= ? AND s.to_date <= ? " +
                categoryFilter + " GROUP BY p.category";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, java.sql.Date.valueOf(fromDate));
            pstmt.setDate(2, java.sql.Date.valueOf(toDate));

            int paramIndex = 3;
            if (!selectAll) {
                for (String cat : selectedCategories) {
                    pstmt.setString(paramIndex++, cat.split(" - ")[0]); // only category part
                }
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String category = rs.getString("category");
                int quantity = rs.getInt("totalQty");
                distribution.put(category, quantity);
            }

        } catch (SQLException e) {
            System.err.println("Error in getQuantityByCategory: " + e.getMessage());
        }

        return distribution;
    }


}
