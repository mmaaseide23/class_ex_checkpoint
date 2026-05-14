package user;

import io.javalin.http.Context;

import java.util.List;
import java.util.Optional;

public class UserController {

    private static final int MAX_POSTCODE_PREFERENCES = 5;

    private final UserDAO users;

    public UserController(UserDAO users) {
        this.users = users;
    }

    public void register(Context ctx) {
        User user = ctx.bodyValidator(User.class).get();

        if (user.firstName == null || user.firstName.isBlank()) {
            ctx.status(400).json("First name is required");
            return;
        }
        if (user.lastName == null || user.lastName.isBlank()) {
            ctx.status(400).json("Last name is required");
            return;
        }
        if (user.email == null || user.email.isBlank()) {
            ctx.status(400).json("Email is required");
            return;
        }

        User created = users.createUser(user);
        if (created != null) {
            ctx.status(201).json(created);
        } else {
            ctx.status(400).json("Failed to create user. Email may already exist.");
        }
    }

    public void getUser(Context ctx, String id) {
        Optional<User> user = users.getUserById(Integer.parseInt(id));
        if (user.isPresent()) {
            ctx.status(200).json(user.get());
        } else {
            ctx.status(404).json("User not found");
        }
    }

    public void addPreference(Context ctx, String userId) {
        UserPreference pref = ctx.bodyValidator(UserPreference.class).get();

        if (pref.preferenceType == null || pref.preferenceType.isBlank()) {
            ctx.status(400).json("preferenceType is required");
            return;
        }
        if (pref.preferenceValue == null || pref.preferenceValue.isBlank()) {
            ctx.status(400).json("preferenceValue is required");
            return;
        }

        int uid = Integer.parseInt(userId);

        if (pref.preferenceType.equals("postcode")) {
            int count = users.countPreferencesByType(uid, "postcode");
            if (count >= MAX_POSTCODE_PREFERENCES) {
                ctx.status(400).json("Maximum of 5 postcode preferences reached");
                return;
            }
        }

        UserPreference created = users.addPreference(uid, pref);
        if (created != null) {
            ctx.status(201).json(created);
        } else {
            ctx.status(400).json("Failed to add preference");
        }
    }

    public void getPreferences(Context ctx, String userId) {
        List<UserPreference> prefs = users.getPreferences(Integer.parseInt(userId));
        ctx.status(200).json(prefs);
    }

    public void deletePreference(Context ctx, String prefId) {
        boolean deleted = users.deletePreference(Integer.parseInt(prefId));
        if (deleted) {
            ctx.status(200).json("Preference deleted");
        } else {
            ctx.status(404).json("Preference not found");
        }
    }

}
