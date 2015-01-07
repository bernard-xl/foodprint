package foodprint.data.response;

import foodprint.data.entity.Restaurant;
import foodprint.data.entity.User;

import java.util.List;

/**
 * Created by bernard on 6/1/15.
 */
public class RestaurantProfile {

    private Restaurant restaurant;

    private Double totalRating;

    private Double ratingCount;

    private List<User> recentVisitors;

    private List<String> dishes;

    public RestaurantProfile() {
    }

    public RestaurantProfile(Restaurant restaurant, Double totalRating, Double ratingCount, List<User> recentVisitors, List<String> dishes) {
        this.restaurant = restaurant;
        this.totalRating = totalRating;
        this.ratingCount = ratingCount;
        this.recentVisitors = recentVisitors;
        this.dishes = dishes;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public Double getTotalRating() {
        return totalRating;
    }

    public void setTotalRating(Double totalRating) {
        this.totalRating = totalRating;
    }

    public Double getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Double ratingCount) {
        this.ratingCount = ratingCount;
    }

    public List<User> getRecentVisitors() {
        return recentVisitors;
    }

    public void setRecentVisitors(List<User> recentVisitors) {
        this.recentVisitors = recentVisitors;
    }
}
