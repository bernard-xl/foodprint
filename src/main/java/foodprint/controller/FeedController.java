package foodprint.controller;

import foodprint.data.entity.FoodPrint;
import foodprint.data.entity.Restaurant;
import foodprint.data.entity.User;
import foodprint.data.response.FreshPrint;
import foodprint.data.response.RestaurantProfile;
import foodprint.data.utils.JsonObjectMapper;
import foodprint.data.utils.RedisKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by bernard on 6/1/15.
 */
@RestController
@RequestMapping("/feed")
public class FeedController {

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private JsonObjectMapper<FoodPrint> foodPrintMapper;

    @Autowired
    private JsonObjectMapper<Restaurant> restaurantMapper;

    @Autowired
    private JsonObjectMapper<User> userMapper;

    @RequestMapping("/{userId}/freshPrints")
    public List<FreshPrint> freshPrints(@PathVariable Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> foodPrintKeys = jedis.zrevrange(RedisKeys.timelineOf(userId), 0, -1).stream()
                    .map(x -> RedisKeys.foodPrint(Long.valueOf(x)))
                    .collect(Collectors.toList());

            List<FoodPrint> foodPrints = getFoodPrints(jedis, foodPrintKeys);
            List<User> users = getUsersFromFoodPrints(jedis, foodPrints);
            List<Restaurant> restaurants = getRestaurantFromFoodPrints(jedis, foodPrints);

            List<FreshPrint> freshPrints = new ArrayList<>(foodPrints.size());
            Iterator<FoodPrint> foodPrintsIter = foodPrints.iterator();
            Iterator<User> usersIter = users.iterator();
            Iterator<Restaurant> restaurantIter = restaurants.iterator();

            for (int i = 0; i < foodPrints.size(); i++) {
                freshPrints.add(new FreshPrint(foodPrintsIter.next(), restaurantIter.next(), usersIter.next()));
            }

            return freshPrints;
        }
    }

    @RequestMapping("/{userId}/popularRestaurants")
    public List<RestaurantProfile> popularRestaurants(@PathVariable Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> restaurantKeys = jedis.zrevrange(RedisKeys.rankingOf(userId), 0, -1).stream()
                    .map(x -> RedisKeys.restaurant(Long.valueOf(x)))
                    .collect(Collectors.toList());

            List<Restaurant> restaurants = getRestaurants(jedis, restaurantKeys);
            List<Double> totalRatings = getTotalRatingFromRestaurants(jedis, restaurants, userId);
            List<Double> ratingCounts = getRatingCountsFromRestaurants(jedis, restaurants, userId);
            List<List<User>> recentVisitors = getRecentVisitorsFromRestaurants(jedis, restaurants, userId);
            List<List<String>> dishes = getDishesFromRestaurants(jedis, restaurants);

            List<RestaurantProfile> profiles = new ArrayList<>(restaurants.size());
            Iterator<Restaurant> restaurantIter = restaurants.iterator();
            Iterator<Double> totalRatingsIter = totalRatings.iterator();
            Iterator<Double> ratingCountsIter = ratingCounts.iterator();
            Iterator<List<User>> recentVisitorsIter = recentVisitors.iterator();
            Iterator<List<String>> dishesIter = dishes.iterator();

            for (int i = 0; i < restaurants.size(); i++) {
                profiles.add(new RestaurantProfile(restaurantIter.next(), totalRatingsIter.next(),
                        ratingCountsIter.next(), recentVisitorsIter.next(), dishesIter.next()));
            }

            return profiles;
        }
    }

    @RequestMapping(value = "/newPrint")
    public FoodPrint newPrint(FoodPrint foodPrint) {
        try (Jedis jedis = jedisPool.getResource()) {
            Long epochMilli = Instant.now().toEpochMilli();
            foodPrint.setId(jedis.incr(RedisKeys.foodPrintCount()));
            foodPrint.setTimeStamp(epochMilli);

            String foodPrintIdString = foodPrint.getId().toString();
            String restaurantIdString = foodPrint.getRestaurantId().toString();
            String userIdString = foodPrint.getUserId().toString();
            Long userId = foodPrint.getUserId();
            Double rating = foodPrint.getRating();
            Long restaurantId = foodPrint.getRestaurantId();

            List<Long> followerIds = jedis.smembers(RedisKeys.followersOf(foodPrint.getUserId())).stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            Boolean isFirstTimeVisit = jedis.zscore(RedisKeys.visitedOf(userId), restaurantIdString) == null;
            if (isFirstTimeVisit)
                jedis.zincrby(RedisKeys.rankingOf(userId), 1.0, restaurantIdString);

            Pipeline pipeline = jedis.pipelined();

            String jsonFoodPrint = foodPrintMapper.serialize(foodPrint);
            pipeline.set(RedisKeys.foodPrint(foodPrint.getId()), jsonFoodPrint);

            pipeline.zadd(RedisKeys.selfVisitorsOf(restaurantId), epochMilli, userIdString);
            pipeline.zadd(RedisKeys.visitedOf(userId), epochMilli, restaurantIdString);

            if (foodPrint.getDishes() != null) {
                String dishesOfKey = RedisKeys.dishesOf(foodPrint.getRestaurantId());
                foodPrint.getDishes().forEach(x -> pipeline.zincrby(dishesOfKey, 1.0, x));
            }

            pipeline.sadd(RedisKeys.visitedOf(userId), restaurantIdString);
            pipeline.zadd(RedisKeys.selfTimelineOf(userId), epochMilli, foodPrintIdString);
            pipeline.zadd(RedisKeys.timelineOf(userId), epochMilli, foodPrintIdString);
            pipeline.zincrby(RedisKeys.selfRatingOf(userId), rating, restaurantIdString);
            pipeline.zincrby(RedisKeys.ratingOf(userId), rating, restaurantIdString);
            pipeline.zincrby(RedisKeys.selfRatingCountOf(userId), 1.0, restaurantIdString);
            pipeline.zincrby(RedisKeys.ratingCountOf(userId), 1.0, restaurantIdString);

            followerIds.forEach(fid -> {
                pipeline.zadd(RedisKeys.visitorsBy(restaurantId, fid), epochMilli, userIdString);
                pipeline.zadd(RedisKeys.timelineOf(fid), epochMilli, foodPrintIdString);
                pipeline.zincrby(RedisKeys.ratingOf(fid), rating, restaurantIdString);
                pipeline.zincrby(RedisKeys.ratingCountOf(fid), 1.0, restaurantIdString);
                if (isFirstTimeVisit)
                    pipeline.zincrby(RedisKeys.rankingOf(fid), 1.0, restaurantIdString);
            });

            pipeline.sync();
            return foodPrint;
        }
    }

    private List<List<String>> getDishesFromRestaurants(Jedis jedis, List<Restaurant> restaurants) {
        Pipeline pipeline = jedis.pipelined();

        List<Response<Set<String>>> responses = restaurants.stream()
                .map(x -> pipeline.zrevrange(RedisKeys.dishesOf(x.getId()), 0, -1))
                .collect(Collectors.toList());
        pipeline.sync();
        return responses.stream()
                .map(x -> new ArrayList<String>(x.get()))
                .collect(Collectors.toList());

    }

    private List<List<User>> getRecentVisitorsFromRestaurants(Jedis jedis, List<Restaurant> restaurants, Long userId) {
        Pipeline pipeline = jedis.pipelined();

        List<Response<Set<String>>> responses1 = restaurants.stream()
                .map(x -> pipeline.zrevrange(RedisKeys.visitorsBy(x.getId(), userId), 0, -1))
                .collect(Collectors.toList());
        pipeline.sync();

        List<Set<String>> recentVisitorIds = responses1.stream()
                .map(x -> x.get())
                .collect(Collectors.toList());

        List<List<Response<String>>> responses2 = new ArrayList<>(responses1.size());
        for (Set<String> keys : recentVisitorIds) {
            responses2.add(keys.stream()
                    .map(x -> pipeline.get(RedisKeys.user(Long.valueOf(x))))
                    .collect(Collectors.toList()));
        }
        pipeline.sync();

        List<List<User>> responses3 = new ArrayList<>(responses2.size());
        for (List<Response<String>> res : responses2) {
            responses3.add(res.stream()
                    .map(x -> userMapper.parse(x.get()))
                    .collect(Collectors.toList()));
        }

        return responses3;
    }

    private List<Double> getRatingCountsFromRestaurants(Jedis jedis, List<Restaurant> restaurants, Long userId) {
        Pipeline pipeline = jedis.pipelined();
        List<Response<Double>> responses = restaurants.stream()
                .map(x -> pipeline.zscore(RedisKeys.ratingCountOf(userId), x.getId().toString()))
                .collect(Collectors.toList());
        pipeline.sync();
        return responses.stream()
                .map(x -> x.get())
                .collect(Collectors.toList());
    }

    private List<Double> getTotalRatingFromRestaurants(Jedis jedis, List<Restaurant> restaurants, Long userId) {
        Pipeline pipeline = jedis.pipelined();
        List<Response<Double>> responses = restaurants.stream()
                .map(x -> pipeline.zscore(RedisKeys.ratingOf(userId), x.getId().toString()))
                .collect(Collectors.toList());
        pipeline.sync();
        return responses.stream()
                .map(x -> x.get())
                .collect(Collectors.toList());
    }

    private List<User> getUsersFromFoodPrints(Jedis jedis, List<FoodPrint> foodPrints) {
        Pipeline pipeline = jedis.pipelined();
        List<Response<String>> responses = foodPrints.stream()
                .map(x -> pipeline.get(RedisKeys.user(x.getUserId())))
                .collect(Collectors.toList());
        pipeline.sync();
        return responses.stream()
                .map(x -> userMapper.parse(x.get()))
                .collect(Collectors.toList());
    }

    private List<Restaurant> getRestaurantFromFoodPrints(Jedis jedis, List<FoodPrint> foodPrints) {
        Pipeline pipeline = jedis.pipelined();
        List<Response<String>> responses = foodPrints.stream()
                .map(x -> pipeline.get(RedisKeys.restaurant(x.getRestaurantId())))
                .collect(Collectors.toList());
        pipeline.sync();
        return responses.stream()
                .map(x -> restaurantMapper.parse(x.get()))
                .collect(Collectors.toList());
    }

    private List<FoodPrint> getFoodPrints(Jedis jedis, List<String> foodPrintKeys) {
        Pipeline pipeline = jedis.pipelined();
        List<Response<String>> responses = foodPrintKeys.stream()
                .map(pipeline::get)
                .collect(Collectors.toList());
        pipeline.sync();
        return responses.stream()
                .map(x -> foodPrintMapper.parse(x.get()))
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
