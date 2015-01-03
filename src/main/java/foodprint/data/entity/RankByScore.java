package foodprint.data.entity;

import java.util.List;

/**
 * Created by bernard on 22/12/14.
 */
public class RankByScore {

    private Restaurant restaurant;

    private List<User> visitors;

    private Double score;

    public RankByScore() {
    }

    public RankByScore(Restaurant restaurant, Double score, List<User> visitors) {
        this.restaurant = restaurant;
        this.score = score;
        this.visitors = visitors;

    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public List<User> getVisitors() {
        return visitors;
    }

    public void setVisitors(List<User> visitors) {
        this.visitors = visitors;
    }
}
