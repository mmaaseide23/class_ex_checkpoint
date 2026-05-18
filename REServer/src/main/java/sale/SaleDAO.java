package sale;

import app.DatabaseConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaleDAO {

    private MongoCollection<Document> collection() {
        return DatabaseConfig.getDatabase().getCollection("sales");
    }

    private Sale fromDoc(Document doc) {
        Sale s = new Sale();
        s.propertyId = doc.containsKey("property_id") ? toLong(doc.get("property_id")) : 0L;
        s.councilName = toStr(doc.get("council_name"));
        s.purchasePrice = doc.containsKey("purchase_price") ? toLong(doc.get("purchase_price")) : 0L;
        s.address = toStr(doc.get("address"));
        s.postCode = toStr(doc.get("post_code"));
        s.propertyType = toStr(doc.get("property_type"));
        s.strataLotNumber = toStr(doc.get("strata_lot_number"));
        s.propertyName = toStr(doc.get("property_name"));
        s.area = doc.containsKey("area") && doc.get("area") != null ? toDouble(doc.get("area")) : 0.0;
        s.areaType = toStr(doc.get("area_type"));
        s.zoning = toStr(doc.get("zoning"));
        s.natureOfProperty = toStr(doc.get("nature_of_property"));
        s.primaryPurpose = toStr(doc.get("primary_purpose"));
        s.legalDescription = toStr(doc.get("legal_description"));
        s.downloadDate = parseDate(toStr(doc.get("download_date")));
        s.contractDate = parseDate(toStr(doc.get("contract_date")));
        s.settlementDate = parseDate(toStr(doc.get("settlement_date")));
        return s;
    }

    public boolean newSale(Sale sale) {
        Document doc = new Document()
            .append("property_id", sale.propertyId)
            .append("post_code", sale.postCode)
            .append("purchase_price", sale.purchasePrice);
        collection().insertOne(doc);
        return true;
    }

    public Optional<Sale> getSaleByPropertyId(String propertyId) {
        Document doc = collection().find(Filters.eq("property_id", Long.parseLong(propertyId))).first();
        if (doc != null) {
            return Optional.of(fromDoc(doc));
        }
        return Optional.empty();
    }

    public List<Sale> getSalesByPostCode(String postCode) {
        List<Sale> results = new ArrayList<>();
        // post_code may be stored as int (from CSV) or string (from API)
        for (Document doc : collection().find(Filters.or(
                Filters.eq("post_code", postCode),
                Filters.eq("post_code", safeParseInt(postCode))
        )).limit(100)) {
            results.add(fromDoc(doc));
        }
        return results;
    }

    public List<Sale> getAllSales() {
        List<Sale> results = new ArrayList<>();
        for (Document doc : collection().find().limit(100)) {
            results.add(fromDoc(doc));
        }
        return results;
    }

    public List<Sale> getSalesByPriceRange(long minPrice, long maxPrice) {
        List<Sale> results = new ArrayList<>();
        for (Document doc : collection().find(Filters.and(
                Filters.gte("purchase_price", minPrice),
                Filters.lte("purchase_price", maxPrice)
        )).limit(100)) {
            results.add(fromDoc(doc));
        }
        return results;
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.isEmpty()) return null;
        try { return LocalDate.parse(val); }
        catch (Exception e) { return null; }
    }

    private long toLong(Object val) {
        if (val instanceof Long) return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof Double) return ((Double) val).longValue();
        if (val instanceof String s && !s.isEmpty()) {
            try { return Long.parseLong(s); }
            catch (NumberFormatException e) { return 0L; }
        }
        return 0L;
    }

    private double toDouble(Object val) {
        if (val instanceof Double) return (Double) val;
        if (val instanceof Integer) return ((Integer) val).doubleValue();
        if (val instanceof Long) return ((Long) val).doubleValue();
        if (val instanceof String s && !s.isEmpty()) {
            try { return Double.parseDouble(s); }
            catch (NumberFormatException e) { return 0.0; }
        }
        return 0.0;
    }

    private String toStr(Object val) {
        if (val == null) return null;
        return val.toString();
    }

    private int safeParseInt(String val) {
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return -1; }
    }
}
