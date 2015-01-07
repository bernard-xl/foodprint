package foodprint.data.response;

import foodprint.data.entity.FoodPrint;
import foodprint.data.entity.User;

import java.util.List;

/**
 * Created by bernard on 6/1/15.
 */
public class UserProfile {

    private User user;

    private List<FoodPrint> recentFoodPrints;

    public UserProfile() {
    }

    public UserProfile(User user, List<FoodPrint> recentFoodPrints) {
        this.user = user;
        this.recentFoodPrints = recentFoodPrints;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<FoodPrint> getRecentFoodPrints() {
        return recentFoodPrints;
    }

    public void setRecentFoodPrints(List<FoodPrint> recentFoodPrints) {
        this.recentFoodPrints = recentFoodPrints;
    }
}
