package foodprint.rest;

import foodprint.data.utils.ActivityJsonMapper;
import foodprint.data.utils.KeyUtils;
import foodprint.data.entity.Activity;
import foodprint.data.entity.Rank;
import foodprint.data.entity.Restaurant;
import foodprint.data.utils.RestaurantJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by bernard on 22/12/14.
 */
@RestController
@RequestMapping("/feed")
public class FeedController {

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private ActivityJsonMapper activityMapper;

    @Autowired
    private RestaurantJsonMapper restaurantMapper;

    @RequestMapping("/{userId}/timeline")
    public List<Activity> timeline(@PathVariable Long userId) {
        try(Jedis jedis = jedisPool.getResource()) {
            List<String> activityKeys = jedis.zrevrange(KeyUtils.timelineOf(userId), 0, -1).stream()
                    .map(x -> KeyUtils.activity(Long.valueOf(x)))
                    .collect(Collectors.toList());
            return getActivities(jedis, activityKeys);
        }
    }

    @RequestMapping("/{userId}/activities")
    public List<Activity> activities(@PathVariable Long userId) {
        try(Jedis jedis = jedisPool.getResource()) {
            List<String> activityKeys = jedis.zrevrange(KeyUtils.activitiesOf(userId), 0, -1).stream()
                    .map(x -> KeyUtils.activity(Long.valueOf(x)))
                    .collect(Collectors.toList());
            return getActivities(jedis, activityKeys);
        }
    }

    @RequestMapping("/{userId}/ranking")
    public List<Rank> ranking(@PathVariable Long userId) {
        try(Jedis jedis = jedisPool.getResource()) {
            Set<Tuple> rankingWithScore = jedis.zrevrangeWithScores(KeyUtils.rankingOf(userId), 0, -1);
            rankingWithScore.forEach(x -> System.out.println("restaurantId: " + x.getElement() + "\tscore: " + x.getScore()));
            List<String> restaurantKeys = rankingWithScore.stream()
                    .map(t -> KeyUtils.restaurant(Long.valueOf(t.getElement())))
                    .collect(Collectors.toList());
            List<Integer> scores = rankingWithScore.stream()
                    .map(t -> (int)t.getScore())
                    .collect(Collectors.toList());
            List<Restaurant> restaurants = getRestaurants(jedis, restaurantKeys);
            List<Rank> ranks = new ArrayList<>(rankingWithScore.size());
            for (int i = 0; i < rankingWithScore.size(); i++) {
                Rank r = new Rank(restaurants.get(i), scores.get(i));
                ranks.add(r);
            }
            return ranks;
        }
    }

    @RequestMapping("/{userId}/recommendation/{restaurantId}")
    public List<Activity> recommendation(@PathVariable Long userId, @PathVariable Long restaurantId) {
        try(Jedis jedis = jedisPool.getResource()) {
            String recommendationKey = KeyUtils.recommendationOf(restaurantId);
            String activitiesKey = KeyUtils.activitiesOf(userId);
            String viewKey = KeyUtils.viewOf(restaurantId, userId);
            jedis.zinterstore(viewKey, recommendationKey, activitiesKey);
            List<String> interKeys = jedis.zrevrange(viewKey, 0, -1).stream()
                    .map(x -> KeyUtils.activity(Long.valueOf(x)))
                    .collect(Collectors.toList());
            return getActivities(jedis, interKeys);
        }
    }

    @RequestMapping("/post")
    public Activity post(Activity activity) {
        try(Jedis jedis = jedisPool.getResource()) {
            Long epochMillis = Instant.now().toEpochMilli();
            activity.setId(jedis.incr(KeyUtils.activityCount()));
            activity.setTimeStamp(epochMillis);
            String jsonActivity = activityMapper.serialize(activity);

            List<Long> followerId = jedis.smembers(KeyUtils.followersOf(activity.getUserId())).parallelStream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            Pipeline pipeline = jedis.pipelined();

            jedis.set(KeyUtils.activity(activity.getId()), jsonActivity);
            if(activity.getDishes() != null) {
                activity.getDishes().stream()
                        .forEach(x -> pipeline.zincrby(KeyUtils.dishesOf(activity.getRestaurantId()), 1.0d, x));
            }

            pipeline.zadd(KeyUtils.activitiesOf(activity.getUserId()), epochMillis, activity.getId().toString());
            pipeline.zadd(KeyUtils.timelineOf(activity.getUserId()), epochMillis, activity.getId().toString());
            followerId.forEach(x -> pipeline.zadd(KeyUtils.timelineOf(x), epochMillis, activity.getId().toString()));
            followerId.forEach(x -> pipeline.zincrby(KeyUtils.rankingOf(x), activity.getScore(), activity.getRestaurantId().toString()));
            pipeline.zincrby(KeyUtils.rankingOf(activity.getUserId()), activity.getScore(), activity.getRestaurantId().toString());
            pipeline.zadd(KeyUtils.recommendationOf(activity.getRestaurantId()), epochMillis, activity.getId().toString());

            pipeline.sync();

            return activity;
        }
    }

    private List<Activity> getActivities(Jedis jedis, List<String> activityKeys) {
        Pipeline pipeline = jedis.pipelined();
        List<Response<String>> responses = activityKeys.stream()
                .map(pipeline::get)
                .collect(Collectors.toList());
        pipeline.sync();
        return responses.stream()
                .map(x -> activityMapper.parse(x.get()))
                .collect(Collectors.toList());
    }


    private List<Restaurant> getRestaurants(Jedis jedis, List<String> restaurantKeys) {
        Pipeline pipeline = jedis.pipelined();
        List<Response<String>> responses = restaurantKeys.stream()
                .map(pipeline::get)
                .collect(Collectors.toList());
        pipeline.sync();
        return responses.stream()
                .map(x -> restaurantMapper.parse(x.get()))
                .collect(Collectors.toList());
    }

}
