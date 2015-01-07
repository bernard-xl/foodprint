package foodprint.data.utils;

/**
 * Created by bernard on 6/1/15.
 */
public class RedisKeys {

    private static final String FORMAT = "%s:%d";
    private static final String FORMAT2 = "%s:%d:%d";

    public static String userCount() {
        return "userCount";
    }

    public static String user(long userId) {
        return String.format(FORMAT, "user", userId);
    }

    public static String followingOf(long userId) {
        return String.format(FORMAT, "followingOf", userId);
    }

    public static String followersOf(long userId) {
        return String.format(FORMAT, "followersOf", userId);
    }

    public static String timelineOf(long userId) {
        return String.format(FORMAT, "timelineOf", userId);
    }

    public static String selfTimelineOf(long userId) {
        return String.format(FORMAT, "selfTimelineOf", userId);
    }

    public static String rankingOf(long userId) {
        return String.format(FORMAT, "rankingOf", userId);
    }

    public static String visitedOf(long userId) {
        return String.format(FORMAT, "visitedOf", userId);
    }

    public static String ratingOf(long userId) {
        return String.format(FORMAT, "ratingOf", userId);
    }

    public static String selfRatingOf(long userId) {
        return String.format(FORMAT, "selfRatingOf", userId);
    }

    public static String ratingCountOf(long userId) {
        return String.format(FORMAT, "ratingCountOf", userId);
    }

    public static String selfRatingCountOf(long userId) {
        return String.format(FORMAT, "selfRatingCountOf", userId);
    }

    public static String foodPrintCount() {
        return "printCount";
    }

    public static String foodPrint(long foodPrintId) {
        return String.format(FORMAT, "foodPrint", foodPrintId);
    }

    public static String restaurantCount() {
        return "restaurantCount";
    }

    public static String restaurant(long restaurantId) {
        return String.format(FORMAT, "restaurant", restaurantId);
    }

    public static String visitorsBy(long restaurantId, long userId) {
        return String.format(FORMAT2, "restaurantVisitorsBy", restaurantId, userId);
    }

    public static String selfVisitorsOf(long restaurantId) {
        return String.format(FORMAT, "visitorsOf", restaurantId);
    }

    public static String dishesOf(long restaurantId) {
        return String.format(FORMAT, "dishesOf", restaurantId);
    }
}
