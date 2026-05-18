package listing;

import app.DatabaseConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ListingDAO {

    private MongoCollection<Document> collection() {
        return DatabaseConfig.getDatabase().getCollection("listings");
    }

    private Listing fromDoc(Document doc) {
        Listing l = new Listing();
        l.propertyId = doc.containsKey("property_id") ? toLong(doc.get("property_id")) : 0L;
        l.price = doc.containsKey("price") ? toLong(doc.get("price")) : 0L;
        String ld = doc.getString("listing_date");
        l.listingDate = ld != null ? LocalDate.parse(ld) : null;
        return l;
    }

    public boolean newListing(Listing listing) {
        Document existing = collection().find(Filters.and(
            Filters.eq("property_id", listing.propertyId),
            Filters.eq("listing_date", listing.listingDate.toString()),
            Filters.eq("price", listing.price)
        )).first();

        if (existing != null) {
            return true;
        }

        Document doc = new Document()
            .append("property_id", listing.propertyId)
            .append("listing_date", listing.listingDate.toString())
            .append("price", listing.price);
        collection().insertOne(doc);
        return true;
    }

    public List<Listing> getListingsByPropertyId(String propertyId) {
        List<Listing> results = new ArrayList<>();
        for (Document doc : collection().find(Filters.eq("property_id", Long.parseLong(propertyId)))
                .sort(Sorts.ascending("listing_date"))) {
            results.add(fromDoc(doc));
        }
        return results;
    }

    public List<Listing> getAllListings() {
        List<Listing> results = new ArrayList<>();
        for (Document doc : collection().find().limit(100)) {
            results.add(fromDoc(doc));
        }
        return results;
    }

    private long toLong(Object val) {
        if (val instanceof Long) return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof Double) return ((Double) val).longValue();
        if (val instanceof String) return Long.parseLong((String) val);
        return 0L;
    }
}
