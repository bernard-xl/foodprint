package foodprint.data.entity;

import java.time.Instant;
import java.util.List;

/**
 * Created by bernard on 21/12/14.
 */
public class Activity {

    private Long id;

    private Long userId;

    private Long restaurantId;

    private Integer score;

    private String comment;

    private List<String> dishes;

    private Long timeStamp;

    public Activity() {
    }

    public Activity(Long userId, Long restaurantId, Integer score, String comment) {
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.score = score;
        this.comment = comment;
        this.timeStamp = Instant.now().getEpochSecond();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getDishes() {
        return dishes;
    }

    public void setDishes(List<String> dishes) {
        this.dishes = dishes;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
