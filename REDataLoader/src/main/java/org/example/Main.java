package org.example;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;

public class Main {

    private static final CSVFormat CSV_FORMAT = CSVFormat.Builder.create(CSVFormat.RFC4180)
        .setHeader()
        .setSkipHeaderRecord(true)
        .setAllowDuplicateHeaderNames(false)
        .build();

    static final private String PATH_TO_FILE =
        "/Users/michaelmaaseide/Documents/Courses/CS4530/REServer/data/nsw_property_data.csv";

    static final private String DB_URL = "jdbc:postgresql://localhost:5432/realestate";

    private static final String INSERT_SQL =
        "INSERT INTO properties (property_id, download_date, council_name, purchase_price, " +
        "address, post_code, property_type, strata_lot_number, property_name, area, " +
        "area_type, contract_date, settlement_date, zoning, nature_of_property, " +
        "primary_purpose, legal_description) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public static void main(String[] args) {
        System.out.println("Starting data load...");

        final Path csvFilePath = Paths.get(PATH_TO_FILE);

        try (
            CSVParser parser = CSVParser.parse(csvFilePath, StandardCharsets.UTF_8, CSV_FORMAT);
            Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)
        ) {
            conn.setAutoCommit(false);

            System.out.println("File opened, connected to database");
            System.out.println("Headers: " + parser.getHeaderNames());

            int count = 0;
            int batchSize = 5000;

            for (final CSVRecord record : parser) {
                stmt.setObject(1, parseLongOrNull(record.get("property_id")));
                stmt.setDate(2, parseDate(record.get("download_date")));
                stmt.setString(3, record.get("council_name"));
                stmt.setObject(4, parseLongOrNull(record.get("purchase_price")));
                stmt.setString(5, record.get("address"));
                stmt.setString(6, record.get("post_code"));
                stmt.setString(7, record.get("property_type"));
                stmt.setString(8, record.get("strata_lot_number"));
                stmt.setString(9, record.get("property_name"));
                stmt.setObject(10, parseDoubleOrNull(record.get("area")));
                stmt.setString(11, record.get("area_type"));
                stmt.setDate(12, parseDate(record.get("contract_date")));
                stmt.setDate(13, parseDate(record.get("settlement_date")));
                stmt.setString(14, record.get("zoning"));
                stmt.setString(15, record.get("nature_of_property"));
                stmt.setString(16, record.get("primary_purpose"));
                stmt.setString(17, record.get("legal_description"));

                stmt.addBatch();
                count++;

                if (count % batchSize == 0) {
                    stmt.executeBatch();
                    conn.commit();
                    System.out.println("Loaded " + count + " records...");
                }
            }

            stmt.executeBatch();
            conn.commit();

            System.out.println("Done! Total records loaded: " + count);

        } catch (IOException e) {
            System.out.println("Failed to open CSV file: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return Date.valueOf(value.trim());
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return Long.parseLong(value.trim());
    }

    private static Double parseDoubleOrNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return Double.parseDouble(value.trim());
    }
}
