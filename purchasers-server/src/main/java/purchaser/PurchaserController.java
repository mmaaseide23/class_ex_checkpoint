package purchaser;

import io.javalin.http.Context;

import java.util.List;
import java.util.Optional;

public class PurchaserController {

    private static final int MAX_POSTCODE_PREFERENCES = 5;

    private final PurchaserDAO purchasers;

    public PurchaserController(PurchaserDAO purchasers) {
        this.purchasers = purchasers;
    }

    public void register(Context ctx) {
        Purchaser p = ctx.bodyValidator(Purchaser.class).get();

        if (p.firstName == null || p.firstName.isBlank()) {
            ctx.status(400).json("First name is required");
            return;
        }
        if (p.lastName == null || p.lastName.isBlank()) {
            ctx.status(400).json("Last name is required");
            return;
        }
        if (p.email == null || p.email.isBlank()) {
            ctx.status(400).json("Email is required");
            return;
        }

        Purchaser created = purchasers.createPurchaser(p);
        if (created != null) {
            ctx.status(201).json(created);
        } else {
            ctx.status(400).json("Failed to create purchaser. Email may already exist.");
        }
    }

    public void getPurchaser(Context ctx, String id) {
        try {
            Optional<Purchaser> p = purchasers.getPurchaserById(Integer.parseInt(id));
            if (p.isPresent()) {
                ctx.status(200).json(p.get());
            } else {
                ctx.status(404).json("Purchaser not found");
            }
        } catch (NumberFormatException e) {
            ctx.status(400).json("Invalid purchaser ID: must be numeric");
        }
    }

    public void addPreference(Context ctx, String purchaserId) {
        PurchaserPreference pref = ctx.bodyValidator(PurchaserPreference.class).get();

        if (pref.preferenceType == null || pref.preferenceType.isBlank()) {
            ctx.status(400).json("preferenceType is required");
            return;
        }
        if (pref.preferenceValue == null || pref.preferenceValue.isBlank()) {
            ctx.status(400).json("preferenceValue is required");
            return;
        }

        int pid;
        try {
            pid = Integer.parseInt(purchaserId);
        } catch (NumberFormatException e) {
            ctx.status(400).json("Invalid purchaser ID: must be numeric");
            return;
        }

        if (pref.preferenceType.equals("postcode")) {
            int count = purchasers.countPreferencesByType(pid, "postcode");
            if (count >= MAX_POSTCODE_PREFERENCES) {
                ctx.status(400).json("Maximum of 5 postcode preferences reached");
                return;
            }
        }

        PurchaserPreference created = purchasers.addPreference(pid, pref);
        if (created != null) {
            ctx.status(201).json(created);
        } else {
            ctx.status(400).json("Failed to add preference");
        }
    }

    public void getPreferences(Context ctx, String purchaserId) {
        try {
            List<PurchaserPreference> prefs = purchasers.getPreferences(Integer.parseInt(purchaserId));
            ctx.status(200).json(prefs);
        } catch (NumberFormatException e) {
            ctx.status(400).json("Invalid purchaser ID: must be numeric");
        }
    }

    public void deletePreference(Context ctx, String prefId) {
        try {
            boolean deleted = purchasers.deletePreference(Integer.parseInt(prefId));
            if (deleted) {
                ctx.status(200).json("Preference deleted");
            } else {
                ctx.status(404).json("Preference not found");
            }
        } catch (NumberFormatException e) {
            ctx.status(400).json("Invalid preference ID: must be numeric");
        }
    }

    public void withPostcodePrefs(Context ctx) {
        ctx.status(200).json(purchasers.getPurchasersWithPostcodePreferences());
    }
}