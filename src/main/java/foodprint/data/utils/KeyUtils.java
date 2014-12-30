package foodprint.data.utils;

/**
 * Created by bernard on 21/12/14.
 */
public class KeyUtils {

    private static final String FORMAT = "%s%s%d";
    private static final String FORMAT2 = "%s%s%d%s%d";
    private static final String SEPARATOR = ":";

    public static String userCount() {
        return "userCount";
    }

    public static String user(long userId) {
        return String.format(FORMAT, "user", SEPARATOR, userId);
    }

    public static String followingOf(long userId) {
        return String.format(FORMAT, "followingOf", SEPARATOR, userId);
    }

    public static String followersOf(long userId) {
        return String.format(FORMAT, "followersOf", SEPARATOR, userId);
    }

    public static String rankingOf(long userId) {
        return String.format(FORMAT, "rankingOf", SEPARATOR, userId);
    }

    public static String activitiesOf(long userId) {
        return String.format(FORMAT, "activitiesOf", SEPARATOR, userId);
    }

    public static String timelineOf(long userId) {
        return String.format(FORMAT, "timelineOf", SEPARATOR, userId);
    }

    public static String resCountOfUser(long userId) {
        return String.format(FORMAT, "resCountOfUser", SEPARATOR, userId);
    }

    public static String restaurantCount() {
        return "restaurantCount";
    }

    public static String restaurant(long restaurantId) {
        return String.format(FORMAT, "restaurant", SEPARATOR, restaurantId);
    }

    public static String viewOf(long restaurantId, long userId) {
        return String.format(FORMAT, "viewOf", SEPARATOR, restaurantId, SEPARATOR, userId);
    }

    public static String visitorsOf(long restaurantId) {
        return String.format(FORMAT, "visitorsOf", SEPARATOR, restaurantId);
    }

    public static String recommendationOf(long restaurantId) {
        return String.format(FORMAT, "recommendationOf", SEPARATOR, restaurantId);
    }

    public static String dishesOf(long restaurantId) {
        return String.format(FORMAT, "dishesOf", SEPARATOR, restaurantId);
    }

    public static String resCountOfRestaurant(long restaurantId) {
        return String.format(FORMAT, "resCountOfRestaurant", SEPARATOR, restaurantId);
    }

    public static String activityCount() {
        return "activityCount";
    }

    public static String activity(long activityId) {
        return String.format(FORMAT, "activity",  SEPARATOR, activityId);
    }
}
