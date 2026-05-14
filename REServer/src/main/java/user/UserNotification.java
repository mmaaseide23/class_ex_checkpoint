package user;

import java.util.ArrayList;
import java.util.List;

public class UserNotification {
    public int userId;
    public String firstName;
    public String lastName;
    public String email;
    public List<MatchingListing> listings = new ArrayList<>();

    public static class MatchingListing {
        public long propertyId;
        public long price;
    }
}