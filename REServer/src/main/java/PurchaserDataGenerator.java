import app.DatabaseConfig;

import java.sql.*;
import java.util.*;
import java.util.UUID;

public class PurchaserDataGenerator {

  private static final String CREATE_PURCHASER_TABLE =
          "CREATE TABLE IF NOT EXISTS purchasers (" +
                  "purchaser_id SERIAL PRIMARY KEY, " +
                  "first_name VARCHAR(100), " +
                  "last_name VARCHAR(100), " +
                  "email VARCHAR(255) UNIQUE, " +
                  "phone VARCHAR(20), " +
                  "created_date DATE DEFAULT CURRENT_DATE" +
                  ")";

  private static final String CREATE_INTEREST_TABLE =
          "CREATE TABLE IF NOT EXISTS purchaser_interests (" +
                  "interest_id SERIAL PRIMARY KEY, " +
                  "purchaser_id INTEGER REFERENCES purchasers(purchaser_id), " +
                  "post_code VARCHAR(10), " +
                  "UNIQUE(purchaser_id, post_code)" +
                  ")";

  private static final String INSERT_PURCHASER =
          "INSERT INTO purchasers (first_name, last_name, email, phone) " +
                  "VALUES (?, ?, ?, ?) RETURNING purchaser_id";

  private static final String INSERT_INTEREST =
          "INSERT INTO purchaser_interests (purchaser_id, post_code) " +
                  "VALUES (?, ?) ON CONFLICT DO NOTHING";

  private static final String GET_POSTCODES =
          "SELECT DISTINCT post_code FROM properties WHERE post_code IS NOT NULL";

  // Sample first and last names for realistic data
  private static final String[] FIRST_NAMES = {
          "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
          "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
          "Thomas", "Sarah", "Christopher", "Karen", "Charles", "Nancy", "Daniel", "Lisa",
          "Matthew", "Betty", "Anthony", "Margaret", "Mark", "Sandra", "Donald", "Ashley",
          "Steven", "Kimberly", "Paul", "Emily", "Andrew", "Donna", "Joshua", "Michelle",
          "Kenneth", "Carol", "Kevin", "Amanda", "Brian", "Dorothy", "George", "Melissa",
          "Timothy", "Deborah", "Ronald", "Stephanie", "Edward", "Rebecca", "Jason", "Sharon",
          "Jeffrey", "Laura", "Ryan", "Cynthia", "Jacob", "Kathleen", "Gary", "Amy",
          "Nicholas", "Angela", "Eric", "Shirley", "Jonathan", "Anna", "Stephen", "Brenda",
          "Larry", "Pamela", "Justin", "Emma", "Scott", "Nicole", "Brandon", "Helen",
          "Benjamin", "Samantha", "Samuel", "Katherine", "Raymond", "Christine", "Gregory", "Debra",
          "Alexander", "Rachel", "Patrick", "Carolyn", "Frank", "Janet", "Jack", "Catherine",
          "Dennis", "Maria", "Jerry", "Heather", "Tyler", "Diane", "Aaron", "Ruth"
  };

  private static final String[] LAST_NAMES = {
          "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
          "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
          "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White",
          "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young",
          "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
          "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell",
          "Carter", "Roberts", "Gomez", "Phillips", "Evans", "Turner", "Diaz", "Parker",
          "Cruz", "Edwards", "Collins", "Reyes", "Stewart", "Morris", "Morales", "Murphy",
          "Cook", "Rogers", "Gutierrez", "Ortiz", "Morgan", "Cooper", "Peterson", "Bailey",
          "Reed", "Kelly", "Howard", "Ramos", "Kim", "Cox", "Ward", "Richardson",
          "Watson", "Brooks", "Chavez", "Wood", "James", "Bennett", "Gray", "Mendoza",
          "Ruiz", "Hughes", "Price", "Alvarez", "Castillo", "Sanders", "Patel", "Myers",
          "Long", "Ross", "Foster", "Jimenez", "Powell", "Jenkins", "Perry", "Russell"
  };

  public static void main(String[] args) {
    Random random = new Random();

    try (Connection conn = DatabaseConfig.getConnection()) {
      conn.setAutoCommit(false);

      System.out.println("Creating tables if they don't exist...");
      try (Statement stmt = conn.createStatement()) {
        stmt.execute(CREATE_PURCHASER_TABLE);
        stmt.execute(CREATE_INTEREST_TABLE);
      }

      System.out.println("Fetching available postcodes...");
      List<String> postcodes = new ArrayList<>();
      try (Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery(GET_POSTCODES)) {
        while (rs.next()) {
          postcodes.add(rs.getString("post_code"));
        }
      }

      if (postcodes.isEmpty()) {
        System.err.println("No postcodes found in properties table!");
        return;
      }

      System.out.println("Found " + postcodes.size() + " distinct postcodes");
      System.out.println("Generating 10,000 purchaser accounts...");

      try (PreparedStatement purchaserStmt = conn.prepareStatement(INSERT_PURCHASER);
           PreparedStatement interestStmt = conn.prepareStatement(INSERT_INTEREST)) {

        for (int i = 1; i <= 10000; i++) {
          // Generate random purchaser
          String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
          String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
          String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "." +
                  UUID.randomUUID().toString().substring(0, 8) + "@example.com";
          String phone = "04" + String.format("%08d", random.nextInt(100000000));

          purchaserStmt.setString(1, firstName);
          purchaserStmt.setString(2, lastName);
          purchaserStmt.setString(3, email);
          purchaserStmt.setString(4, phone);

          ResultSet rs = purchaserStmt.executeQuery();
          rs.next();
          int purchaserId = rs.getInt(1);
          rs.close();

          // Generate random number of postcode interests (0-5)
          int numInterests = random.nextInt(6); // 0 to 5
          Set<String> selectedPostcodes = new HashSet<>();

          for (int j = 0; j < numInterests; j++) {
            String postcode = postcodes.get(random.nextInt(postcodes.size()));
            if (selectedPostcodes.add(postcode)) {
              interestStmt.setInt(1, purchaserId);
              interestStmt.setString(2, postcode);
              interestStmt.addBatch();
            }
          }

          if (i % 100 == 0) {
            interestStmt.executeBatch();
            System.out.println("Generated " + i + " purchasers...");
          }
        }

        interestStmt.executeBatch();
      }

      conn.commit();

      // Print statistics
      System.out.println("\n=== Generation Complete ===");
      try (Statement stmt = conn.createStatement()) {
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM purchasers");
        rs.next();
        System.out.println("Total purchasers: " + rs.getInt(1));

        rs = stmt.executeQuery("SELECT COUNT(*) FROM purchaser_interests");
        rs.next();
        System.out.println("Total interests: " + rs.getInt(1));

        rs = stmt.executeQuery(
                "SELECT COUNT(DISTINCT purchaser_id) as count, " +
                        "COUNT(*) / COUNT(DISTINCT purchaser_id)::float as avg " +
                        "FROM purchaser_interests"
        );
        rs.next();
        System.out.println("Purchasers with interests: " + rs.getInt("count"));
        System.out.printf("Average interests per purchaser: %.2f\n", rs.getFloat("avg"));
      }

    } catch (SQLException e) {
      System.err.println("Database error: " + e.getMessage());
      e.printStackTrace();
    }
  }
}