package foodprint.data.entity;

import java.util.List;

/**
 * Created by bernard on 5/1/15.
 */
public class RankByPopularity {

    private Restaurant restaurant;

    private Double score;

    private Integer popularity;

    public RankByPopularity() {
    }

    public RankByPopularity(Restaurant restaurant, Double score, Integer popularity) {
        this.restaurant = restaurant;
        this.score = score;
        this.popularity = popularity;
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

    public Integer getPopularity() {
        return popularity;
    }

    public void setPopularity(Integer popularity) {
        this.popularity = popularity;
    }

}
