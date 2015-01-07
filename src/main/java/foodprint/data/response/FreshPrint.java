package foodprint.data.response;

import foodprint.data.entity.FoodPrint;
import foodprint.data.entity.Restaurant;
import foodprint.data.entity.User;

/**
 * Created by bernard on 6/1/15.
 */
public class FreshPrint {

    private FoodPrint foodPrint;

    private Restaurant restaurant;

    private User user;

    public FreshPrint() {
    }

    public FreshPrint(FoodPrint foodPrint, Restaurant restaurant, User user) {
        this.foodPrint = foodPrint;
        this.restaurant = restaurant;
        this.user = user;
    }

    public FoodPrint getFoodPrint() {
        return foodPrint;
    }

    public void setFoodPrint(FoodPrint foodPrint) {
        this.foodPrint = foodPrint;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
