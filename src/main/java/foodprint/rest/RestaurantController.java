package foodprint.rest;

import foodprint.data.utils.KeyUtils;
import foodprint.data.entity.Restaurant;
import foodprint.data.utils.RestaurantJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Created by bernard on 21/12/14.
 */
@RestController
@RequestMapping("/restaurant")
public class RestaurantController {

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private RestaurantJsonMapper restaurantMapper;

    @RequestMapping("/register")
    public Restaurant register(Restaurant restaurant) {
        try(Jedis jedis = jedisPool.getResource()) {
            restaurant.setId(jedis.incr(KeyUtils.restaurantCount()));
            String jsonRestaurant = restaurantMapper.serialize(restaurant);
            jedis.set(KeyUtils.restaurant(restaurant.getId()), jsonRestaurant);
            return restaurant;
        }
    }

    @RequestMapping("/list")
    public List<Restaurant> list() {
        try(Jedis jedis = jedisPool.getResource()) {
            long restaurantCount = Long.valueOf(jedis.get(KeyUtils.restaurantCount()));
            List<String> restaurantKeys = LongStream.iterate(1, x -> x + 1)
                    .limit(restaurantCount)
                    .mapToObj(KeyUtils::restaurant)
                    .collect(Collectors.toList());
            return getRestaurants(jedis, restaurantKeys);
        }
    }

    @RequestMapping("/{restaurantId}/profile")
    public Restaurant profile(@PathVariable Long restaurantId) {
        try(Jedis jedis = jedisPool.getResource()) {
            String jsonRestaurant = jedis.get(KeyUtils.restaurant(restaurantId));
            if(jsonRestaurant == null)
                throw new IllegalArgumentException("specified restaurant doesn't exist.");
            return restaurantMapper.parse(jsonRestaurant);
        }
    }

    @RequestMapping("/{restaurantId}/dishes")
    public List<String> dishes(@PathVariable Long restaurantId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> dishes = jedis.zrange(KeyUtils.dishesOf(restaurantId), 0, -1);
            return dishes.stream().collect(Collectors.toList());
        }
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
