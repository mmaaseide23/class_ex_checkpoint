package user;

import java.util.List;

public class NotificationService {

    private final UserDAO users;

    public NotificationService(UserDAO users) {
        this.users = users;
    }

    public List<UserNotification> collectNotifications() {
        return users.getNotificationsForAllUsers();
    }
}