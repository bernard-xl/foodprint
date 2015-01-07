package foodprint.controller;

import foodprint.data.entity.Restaurant;
import foodprint.data.entity.User;
import foodprint.data.response.RestaurantProfile;
import foodprint.data.utils.JsonObjectMapper;
import foodprint.data.utils.RedisKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Created by bernard on 6/1/15.
 */
@RestController
@RequestMapping("/restaurant")
public class RestaurantController {

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private JsonObjectMapper<Restaurant> restaurantMapper;

    @Autowired
    private JsonObjectMapper<User> userMapper;

    @RequestMapping("/register")
    public Restaurant register(Restaurant restaurant) {
        try (Jedis jedis = jedisPool.getResource()) {
            restaurant.setId(jedis.incr(RedisKeys.restaurantCount()));
            String jsonRestaurant = restaurantMapper.serialize(restaurant);
            jedis.set(RedisKeys.restaurant(restaurant.getId()), jsonRestaurant);
            return restaurant;
        }
    }

    @RequestMapping("/update")
    public Restaurant update(Restaurant restaurant) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (!jedis.exists(RedisKeys.restaurant(restaurant.getId())))
                throw new IllegalArgumentException("Specified restaurant doesn't exists.");
            String jsonRestaurant = restaurantMapper.serialize(restaurant);
            jedis.set(RedisKeys.restaurant(restaurant.getId()), jsonRestaurant);
            return restaurant;
        }
    }

    @RequestMapping("/list")
    public List<Restaurant> list() {
        try (Jedis jedis = jedisPool.getResource()) {
            long restaurantCount = Long.valueOf(jedis.get(RedisKeys.restaurantCount()));
            List<String> restaurantKeys = LongStream.iterate(1, x -> x + 1)
                    .limit(restaurantCount)
                    .mapToObj(RedisKeys::restaurant)
                    .collect(Collectors.toList());
            return getRestaurants(jedis, restaurantKeys);
        }
    }

    @RequestMapping("/{restaurantId}/profile")
    public RestaurantProfile profile(@PathVariable Long restaurantId, @RequestParam Long viewUserId) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (!jedis.exists(RedisKeys.restaurant(restaurantId)) || !jedis.exists(RedisKeys.user(viewUserId)))
                throw new IllegalArgumentException("Specified restaurant/user doesn't exists.");

            String jsonRestaurant = jedis.get(RedisKeys.restaurant(restaurantId));
            List<String> dishes = jedis.zrevrange(RedisKeys.dishesOf(restaurantId), 0, -1).stream().collect(Collectors.toList());
            Double totalRating = jedis.zscore(RedisKeys.ratingOf(viewUserId), restaurantId.toString());
            Double ratingCount = jedis.zscore(RedisKeys.ratingCountOf(viewUserId), restaurantId.toString());

            String viewKey = RedisKeys.visitorsBy(restaurantId, viewUserId);
            List<String> visitorsKey = jedis.zrevrange(viewKey, 0, -1).stream()
                    .map(x -> RedisKeys.user(Long.valueOf(x)))
                    .collect(Collectors.toList());
            List<User> recentVisitors = getUsers(jedis, visitorsKey);
            Restaurant restaurant = restaurantMapper.parse(jsonRestaurant);

            return new RestaurantProfile(restaurant, totalRating, ratingCount, recentVisitors, dishes);
        }
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
