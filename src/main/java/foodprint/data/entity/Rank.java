package foodprint.data.entity;

/**
 * Created by bernard on 22/12/14.
 */
public class Rank {

    private Restaurant restaurant;

    private Integer score;

    public Rank() {
    }

    public Rank(Restaurant restaurant, Integer score) {
        this.restaurant = restaurant;
        this.score = score;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
