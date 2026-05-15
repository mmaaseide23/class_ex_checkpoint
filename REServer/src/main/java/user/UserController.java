package user;

import io.javalin.http.Context;
import io.javalin.openapi.*;

import java.util.List;
import java.util.Optional;

public class UserController {

    private static final int MAX_POSTCODE_PREFERENCES = 5;
    private static final int HTML_LISTING_THRESHOLD = 1000;

    private final UserDAO users;
    private final NotificationService notifications;

    public UserController(UserDAO users) {
        this.users = users;
        this.notifications = new NotificationService(users);
    }

    @OpenApi(
        path = "/user",
        methods = HttpMethod.POST,
        summary = "Register a new user",
        operationId = "registerUser",
        tags = {"User"},
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = User.class)),
        responses = {
            @OpenApiResponse(status = "201", content = @OpenApiContent(from = User.class), description = "User created"),
            @OpenApiResponse(status = "400", description = "Validation error or duplicate email")
        }
    )
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

    @OpenApi(
        path = "/user/{id}",
        methods = HttpMethod.GET,
        summary = "Get a user by ID",
        operationId = "getUser",
        tags = {"User"},
        pathParams = @OpenApiParam(name = "id", type = Integer.class, description = "User ID"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class), description = "User found"),
            @OpenApiResponse(status = "404", description = "User not found")
        }
    )
    public void getUser(Context ctx, String id) {
        Optional<User> user = users.getUserById(Integer.parseInt(id));
        if (user.isPresent()) {
            ctx.status(200).json(user.get());
        } else {
            ctx.status(404).json("User not found");
        }
    }

    @OpenApi(
        path = "/user/{id}/preference",
        methods = HttpMethod.POST,
        summary = "Add a preference for a user",
        operationId = "addPreference",
        tags = {"User Preference"},
        pathParams = @OpenApiParam(name = "id", type = Integer.class, description = "User ID"),
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = UserPreference.class)),
        responses = {
            @OpenApiResponse(status = "201", content = @OpenApiContent(from = UserPreference.class), description = "Preference added"),
            @OpenApiResponse(status = "400", description = "Validation error or max postcode limit reached")
        }
    )
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

    @OpenApi(
        path = "/user/{id}/preference",
        methods = HttpMethod.GET,
        summary = "Get all preferences for a user",
        operationId = "getPreferences",
        tags = {"User Preference"},
        pathParams = @OpenApiParam(name = "id", type = Integer.class, description = "User ID"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserPreference[].class), description = "List of preferences")
        }
    )
    public void getPreferences(Context ctx, String userId) {
        List<UserPreference> prefs = users.getPreferences(Integer.parseInt(userId));
        ctx.status(200).json(prefs);
    }

    @OpenApi(
        path = "/user/{userId}/preference/{id}",
        methods = HttpMethod.DELETE,
        summary = "Delete a preference by ID",
        operationId = "deletePreference",
        tags = {"User Preference"},
        pathParams = {
            @OpenApiParam(name = "userId", type = Integer.class, description = "User ID"),
            @OpenApiParam(name = "id", type = Integer.class, description = "Preference ID")
        },
        responses = {
            @OpenApiResponse(status = "200", description = "Preference deleted"),
            @OpenApiResponse(status = "404", description = "Preference not found")
        }
    )
    public void deletePreference(Context ctx, String prefId) {
        boolean deleted = users.deletePreference(Integer.parseInt(prefId));
        if (deleted) {
            ctx.status(200).json("Preference deleted");
        } else {
            ctx.status(404).json("Preference not found");
        }
    }

    @OpenApi(
        path = "/user/notify",
        methods = HttpMethod.GET,
        summary = "Get notifications for all users with matching listings",
        operationId = "notifyUsers",
        tags = {"Notification"},
        responses = {
            @OpenApiResponse(status = "200", description = "Notifications as HTML or plain text")
        }
    )
    public void notify(Context ctx) {
        List<UserNotification> toNotify = notifications.collectNotifications();

        int totalListings = 0;
        for (UserNotification n : toNotify) {
            totalListings += n.listings.size();
        }

        if (totalListings > HTML_LISTING_THRESHOLD) {
            ctx.contentType("text/plain").result(NotificationRenderer.renderPlainText(toNotify));
        } else {
            ctx.html(NotificationRenderer.renderHtml(toNotify));
        }
    }
}
