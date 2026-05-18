package user;

import app.DatabaseConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class UserDAO {

    private MongoDatabase db() {
        return DatabaseConfig.getDatabase();
    }

    private MongoCollection<Document> users() {
        return db().getCollection("users");
    }

    private MongoCollection<Document> preferences() {
        return db().getCollection("user_preferences");
    }

    public User createUser(User user) {
        Document doc = new Document()
            .append("first_name", user.firstName)
            .append("last_name", user.lastName)
            .append("email", user.email)
            .append("phone", user.phone)
            .append("created_date", LocalDate.now().toString());

        Document existing = users().find(Filters.eq("email", user.email)).first();
        if (existing != null) {
            return null;
        }

        users().insertOne(doc);
        user.id = doc.getObjectId("_id").hashCode() & 0x7FFFFFFF;
        user.createdDate = LocalDate.now();
        return user;
    }

    public Optional<User> getUserById(int id) {
        for (Document doc : users().find()) {
            int docId = doc.getObjectId("_id").hashCode() & 0x7FFFFFFF;
            if (docId == id) {
                return Optional.of(fromDoc(doc));
            }
        }
        return Optional.empty();
    }

    public UserPreference addPreference(int userId, UserPreference pref) {
        Document doc = new Document()
            .append("user_id", userId)
            .append("preference_type", pref.preferenceType)
            .append("preference_value", pref.preferenceValue);
        preferences().insertOne(doc);
        pref.id = doc.getObjectId("_id").hashCode() & 0x7FFFFFFF;
        pref.userId = userId;
        return pref;
    }

    public List<UserPreference> getPreferences(int userId) {
        List<UserPreference> results = new ArrayList<>();
        for (Document doc : preferences().find(Filters.eq("user_id", userId))) {
            results.add(prefFromDoc(doc));
        }
        return results;
    }

    public boolean deletePreference(int prefId) {
        for (Document doc : preferences().find()) {
            int docId = doc.getObjectId("_id").hashCode() & 0x7FFFFFFF;
            if (docId == prefId) {
                DeleteResult result = preferences().deleteOne(Filters.eq("_id", doc.getObjectId("_id")));
                return result.getDeletedCount() > 0;
            }
        }
        return false;
    }

    public int countPreferencesByType(int userId, String type) {
        return (int) preferences().countDocuments(
            Filters.and(Filters.eq("user_id", userId), Filters.eq("preference_type", type))
        );
    }

    public List<UserNotification> getNotificationsForAllUsers() {
        MongoCollection<Document> salesCol = db().getCollection("sales");
        MongoCollection<Document> listingsCol = db().getCollection("listings");

        Map<Integer, UserNotification> byUser = new LinkedHashMap<>();

        for (Document userDoc : users().find()) {
            int uid = userDoc.getObjectId("_id").hashCode() & 0x7FFFFFFF;

            Set<String> postcodes = new HashSet<>();
            for (Document prefDoc : preferences().find(
                    Filters.and(Filters.eq("user_id", uid), Filters.eq("preference_type", "postcode")))) {
                postcodes.add(prefDoc.getString("preference_value"));
            }
            if (postcodes.isEmpty()) continue;

            Set<Long> propertyIds = new HashSet<>();
            for (String pc : postcodes) {
                int pcInt = safeParseInt(pc);
                for (Document saleDoc : salesCol.find(Filters.or(
                        Filters.eq("post_code", pc),
                        Filters.eq("post_code", pcInt)
                ))) {
                    Object pid = saleDoc.get("property_id");
                    if (pid instanceof Long) propertyIds.add((Long) pid);
                    else if (pid instanceof Integer) propertyIds.add(((Integer) pid).longValue());
                }
            }
            if (propertyIds.isEmpty()) continue;

            for (Document listingDoc : listingsCol.find(Filters.in("property_id", propertyIds))) {
                UserNotification n = byUser.get(uid);
                if (n == null) {
                    n = new UserNotification();
                    n.userId = uid;
                    n.firstName = userDoc.getString("first_name");
                    n.lastName = userDoc.getString("last_name");
                    n.email = userDoc.getString("email");
                    byUser.put(uid, n);
                }
                UserNotification.MatchingListing ml = new UserNotification.MatchingListing();
                Object pid = listingDoc.get("property_id");
                ml.propertyId = pid instanceof Long ? (Long) pid : ((Integer) pid).longValue();
                Object price = listingDoc.get("price");
                ml.price = price instanceof Long ? (Long) price : ((Integer) price).longValue();
                n.listings.add(ml);
            }
        }
        return new ArrayList<>(byUser.values());
    }

    private User fromDoc(Document doc) {
        User u = new User();
        u.id = doc.getObjectId("_id").hashCode() & 0x7FFFFFFF;
        u.firstName = doc.getString("first_name");
        u.lastName = doc.getString("last_name");
        u.email = doc.getString("email");
        u.phone = doc.getString("phone");
        String cd = doc.getString("created_date");
        if (cd != null) u.createdDate = LocalDate.parse(cd);
        return u;
    }

    private int safeParseInt(String val) {
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return -1; }
    }

    private UserPreference prefFromDoc(Document doc) {
        UserPreference p = new UserPreference();
        p.id = doc.getObjectId("_id").hashCode() & 0x7FFFFFFF;
        p.userId = doc.getInteger("user_id");
        p.preferenceType = doc.getString("preference_type");
        p.preferenceValue = doc.getString("preference_value");
        return p;
    }
}
