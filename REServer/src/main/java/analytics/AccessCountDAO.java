package analytics;

import app.DatabaseConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AccessCountDAO {

    private MongoCollection<Document> collection() {
        return DatabaseConfig.getDatabase().getCollection("access_counts");
    }

    public void increment(String accessType, String accessValue) {
        collection().findOneAndUpdate(
            Filters.and(Filters.eq("access_type", accessType), Filters.eq("access_value", accessValue)),
            Updates.combine(
                Updates.inc("count", 1),
                Updates.set("last_accessed", LocalDateTime.now().toString())
            ),
            new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
        );
    }

    public List<AccessCount> getTopByType(String accessType, int limit) {
        List<AccessCount> results = new ArrayList<>();
        for (Document doc : collection().find(Filters.eq("access_type", accessType))
                .sort(Sorts.descending("count")).limit(limit)) {
            results.add(fromDoc(doc));
        }
        return results;
    }

    public List<AccessCount> getAll() {
        List<AccessCount> results = new ArrayList<>();
        for (Document doc : collection().find().sort(Sorts.descending("count"))) {
            results.add(fromDoc(doc));
        }
        return results;
    }

    private AccessCount fromDoc(Document doc) {
        AccessCount ac = new AccessCount();
        ac.accessType = doc.getString("access_type");
        ac.accessValue = doc.getString("access_value");
        ac.count = doc.getInteger("count", 0);
        String la = doc.getString("last_accessed");
        if (la != null) {
            ac.lastAccessed = LocalDateTime.parse(la);
        }
        return ac;
    }
}
