package script;

import app.DatabaseConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SyntheticSalesScript {

    public static void main(String[] args) {
        MongoDatabase db = DatabaseConfig.getDatabase();
        MongoCollection<Document> listings = db.getCollection("listings");
        MongoCollection<Document> sales = db.getCollection("sales");

        listings.drop();
        System.out.println("Cleared existing listings");

        List<Document> saleDocs = new ArrayList<>();
        for (Document doc : sales.find().limit(1100)) {
            Object price = doc.get("purchase_price");
            long p = 0;
            if (price instanceof Long) p = (Long) price;
            else if (price instanceof Integer) p = ((Integer) price).longValue();
            if (p > 0) saleDocs.add(doc);
            if (saleDocs.size() >= 1000) break;
        }

        System.out.println("Fetched " + saleDocs.size() + " sales");

        String today = LocalDate.now().toString();
        int count = 0;
        for (Document saleDoc : saleDocs) {
            Object pid = saleDoc.get("property_id");
            long propertyId = pid instanceof Long ? (Long) pid : ((Integer) pid).longValue();
            Object price = saleDoc.get("purchase_price");
            long salePrice = price instanceof Long ? (Long) price : ((Integer) price).longValue();

            Document listing = new Document()
                .append("property_id", propertyId)
                .append("listing_date", today)
                .append("price", Math.round(salePrice * 1.20));
            listings.insertOne(listing);
            count++;
        }

        System.out.println("Done! Created: " + count);
    }
}
