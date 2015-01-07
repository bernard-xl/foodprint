package foodprint.controller;

import foodprint.data.entity.FoodPrint;
import foodprint.data.entity.User;
import foodprint.data.response.UserProfile;
import foodprint.data.utils.JsonObjectMapper;
import foodprint.data.utils.RedisKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Created by bernard on 6/1/15.
 */
@RestController
@RequestMapping("/people")
public class UserController {

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private JsonObjectMapper<User> userMapper;

    @Autowired
    private JsonObjectMapper<FoodPrint> foodPrintMapper;

    @RequestMapping("/register")
    public User register(User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            user.setId(jedis.incr(RedisKeys.userCount()));
            String jsonUser = userMapper.serialize(user);
            jedis.set(RedisKeys.user(user.getId()), jsonUser);
            return user;
        }
    }

    @RequestMapping("/update")
    public User update(User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (!jedis.exists(RedisKeys.user(user.getId())))
                throw new IllegalArgumentException("Specified user doesn't exist");
            String jsonUser = userMapper.serialize(user);
            jedis.set(RedisKeys.user(user.getId()), jsonUser);
            return user;
        }
    }

    @RequestMapping("/list")
    public List<User> list() {
        try (Jedis jedis = jedisPool.getResource()) {
            long userCount = Long.valueOf(jedis.get(RedisKeys.userCount()));
            List<String> userKeys = LongStream.iterate(1, x -> x + 1)
                    .mapToObj(RedisKeys::user)
                    .limit(userCount)
                    .collect(Collectors.toList());
            return getUsers(jedis, userKeys);
        }
    }

    @RequestMapping("/{userId}/follows")
    public Long follows(@PathVariable Long userId, Long followingId) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (!jedis.exists(RedisKeys.user(userId)) || !jedis.exists(RedisKeys.user(followingId)))
                throw new IllegalArgumentException("Specified user doesn't exist");

            if (jedis.sismember(RedisKeys.followingOf(userId), followingId.toString()))
                return jedis.scard(RedisKeys.followingOf(userId));

            Set<Tuple> hisVisited = jedis.zrangeWithScores(RedisKeys.visitedOf(followingId), 0, -1);

            Pipeline pipeline = jedis.pipelined();
            pipeline.sadd(RedisKeys.followingOf(userId), followingId.toString());
            pipeline.sadd(RedisKeys.followersOf(followingId), userId.toString());

            String timelineOfKey = RedisKeys.timelineOf(userId);
            String ratingOfKey = RedisKeys.ratingOf(userId);
            String ratingCountOfKey = RedisKeys.ratingCountOf(userId);
            String rankingKey = RedisKeys.rankingOf(userId);

            pipeline.zunionstore(timelineOfKey, timelineOfKey, RedisKeys.selfTimelineOf(followingId));
            pipeline.zunionstore(ratingOfKey, ratingOfKey, RedisKeys.selfRatingOf(followingId));
            pipeline.zunionstore(ratingCountOfKey, ratingCountOfKey, RedisKeys.selfRatingCountOf(followingId));
            pipeline.zunionstore(rankingKey, rankingKey, RedisKeys.visitedOf(followingId));

            String followingIdString = followingId.toString();
            hisVisited.forEach(x -> {
                String key = RedisKeys.visitorsBy(Long.valueOf(x.getElement()), userId);
                pipeline.zincrby(key, x.getScore(), followingIdString);
            });

            pipeline.sync();

            return jedis.scard(RedisKeys.followingOf(userId));
        }
    }

    @RequestMapping("/{userId}/profile")
    public UserProfile profile(@PathVariable Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (!jedis.exists(RedisKeys.user(userId)))
                throw new IllegalArgumentException("Specified user doesn't exist");

            String jsonUser = jedis.get(RedisKeys.user(userId));
            List<String> userFoodPrintKeys = jedis.zrevrange(RedisKeys.selfTimelineOf(userId), 0, -1).stream()
                    .map(x -> RedisKeys.foodPrint(Long.valueOf(x)))
                    .collect(Collectors.toList());

            User user = userMapper.parse(jsonUser);
            List<FoodPrint> recentFoodPrints = getFoodPrints(jedis, userFoodPrintKeys);

            return new UserProfile(user, recentFoodPrints);
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
}
