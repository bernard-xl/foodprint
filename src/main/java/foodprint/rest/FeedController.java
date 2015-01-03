package foodprint.rest;

import foodprint.data.entity.*;
import foodprint.data.utils.ActivityJsonMapper;
import foodprint.data.utils.KeyUtils;
import foodprint.data.utils.RestaurantJsonMapper;
import foodprint.data.utils.UserJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.*;

import java.time.Instant;
import java.util.*;
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

    @Autowired
    private UserJsonMapper userMapper;

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

    @RequestMapping("/{userId}/popularity")
    public List<RankByPopularity> popularity(@PathVariable Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Tuple> rankingWithScore = jedis.zrevrangeWithScores(KeyUtils.rankingOf(userId), 0, -1);
            Set<Tuple> popularityWithScore = jedis.zrevrangeWithScores(KeyUtils.popularityOf(userId), 0, -1);
            List<String> restaurantKeys = popularityWithScore.stream()
                    .map(t -> KeyUtils.restaurant(Long.valueOf(t.getElement())))
                    .collect(Collectors.toList());
            List<Double> popularity = popularityWithScore.stream()
                    .map(t -> t.getScore())
                    .collect(Collectors.toList());
            Map<Long, Double> ranking = new HashMap<>();
            rankingWithScore.forEach(t -> ranking.put(Long.valueOf(t.getElement()), t.getScore()));

            List<Restaurant> restaurants = getRestaurants(jedis, restaurantKeys);
            List<RankByPopularity> ranks = new ArrayList<>(popularityWithScore.size());

            for (int i = 0; i < popularityWithScore.size(); i++) {
                Restaurant restaurant = restaurants.get(i);
                Double averageScore = ranking.get(restaurant.getId()) / popularity.get(i);

                RankByPopularity r = new RankByPopularity(restaurant, averageScore, popularity.get(i).intValue());
                ranks.add(r);
            }
            return ranks;
        }
    }

    @RequestMapping("/{userId}/ranking")
    public List<RankByScore> ranking(@PathVariable Long userId) {
        try(Jedis jedis = jedisPool.getResource()) {
            Set<Tuple> rankingWithScore = jedis.zrevrangeWithScores(KeyUtils.rankingOf(userId), 0, -1);
            Set<Tuple> popularityWithScore = jedis.zrevrangeWithScores(KeyUtils.popularityOf(userId), 0, -1);
            List<String> restaurantKeys = rankingWithScore.stream()
                    .map(t -> KeyUtils.restaurant(Long.valueOf(t.getElement())))
                    .collect(Collectors.toList());
            List<Double> scores = rankingWithScore.stream()
                    .map(t -> t.getScore())
                    .collect(Collectors.toList());
            Map<Long, Double> popularity = new HashMap<>();
            popularityWithScore.forEach(t -> popularity.put(Long.valueOf(t.getElement()), t.getScore()));

            List<Restaurant> restaurants = getRestaurants(jedis, restaurantKeys);
            List<RankByScore> ranks = new ArrayList<>(rankingWithScore.size());

            for (int i = 0; i < rankingWithScore.size(); i++) {
                Restaurant restaurant = restaurants.get(i);
                Double averageScore = scores.get(i) / popularity.get(restaurant.getId());

                String visitorKey = KeyUtils.visitorsOf(restaurant.getId());
                String followingKey = KeyUtils.followingOf(userId);
                String viewKey = KeyUtils.viewOf(restaurant.getId(), userId);
                jedis.zinterstore(viewKey, visitorKey, followingKey);

                List<String> visitorKeys = jedis.zrevrange(viewKey, 0, -1).stream()
                        .map(x -> KeyUtils.user(Long.valueOf(x)))
                        .collect(Collectors.toList());

                RankByScore r = new RankByScore(restaurant, averageScore, getUsers(jedis, visitorKeys));
                ranks.add(r);
            }

            Comparator<RankByScore> comparator = (e1, e2) -> Double.compare(e2.getScore(), e1.getScore());
            return ranks.stream().sorted(comparator).collect(Collectors.toList());
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
            pipeline.zadd(KeyUtils.recommendationOf(activity.getRestaurantId()), epochMillis, activity.getId().toString());
            pipeline.zadd(KeyUtils.visitorsOf(activity.getRestaurantId()), epochMillis, activity.getUserId().toString());

            pipeline.zincrby(KeyUtils.rankingOf(activity.getUserId()), activity.getScore(), activity.getRestaurantId().toString());
            pipeline.zincrby(KeyUtils.popularityOf(activity.getUserId()), 1.0, activity.getRestaurantId().toString());
            pipeline.zincrby(KeyUtils.visitingOf(activity.getUserId()), 1.0, activity.getRestaurantId().toString());

            followerId.forEach(x -> pipeline.zadd(KeyUtils.timelineOf(x), epochMillis, activity.getId().toString()));
            followerId.forEach(x -> pipeline.zincrby(KeyUtils.rankingOf(x), activity.getScore(), activity.getRestaurantId().toString()));
            followerId.forEach(x -> pipeline.zincrby(KeyUtils.popularityOf(x), 1.0, activity.getRestaurantId().toString()));

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

    private List<User> getUsers(Jedis jedis, List<String> userKeys) {
        Pipeline pipeline = jedis.pipelined();
        List<Response<String>> responses = userKeys.stream()
                .map(pipeline::get)
                .collect(Collectors.toList());
        pipeline.sync();
        return responses.stream()
                .map(x -> userMapper.parse(x.get()))
                .collect(Collectors.toList());
    }

}
