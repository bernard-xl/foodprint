package foodprint.rest;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import foodprint.data.utils.JsonObjectMapper;
import foodprint.data.utils.KeyUtils;
import foodprint.data.entity.User;
import foodprint.data.utils.UserJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.security.Key;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Created by bernard on 21/12/14.
 */
@RestController
@RequestMapping("/people")
public class PeopleController {

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private UserJsonMapper userMapper;

    @RequestMapping("/register")
    public User register(User user) {
        try(Jedis jedis = jedisPool.getResource()) {
            user.setId(jedis.incr(KeyUtils.userCount()));
            String jsonUser = userMapper.serialize(user);
            jedis.set(KeyUtils.user(user.getId()), jsonUser);
            return user;
        }
    }

    @RequestMapping("/list")
    public List<User> list() {
        try(Jedis jedis = jedisPool.getResource()) {
            long userCount = Long.valueOf(jedis.get(KeyUtils.userCount()));
            List<String> userKeys = LongStream.iterate(1, x -> x + 1)
                    .mapToObj(KeyUtils::user)
                    .limit(userCount)
                    .collect(Collectors.toList());
            return getUsers(jedis, userKeys);
        }
    }

    @RequestMapping("/{userId}/follow")
    public Long follow(@PathVariable Long userId, @NotNull Long followingId) {
        try(Jedis jedis = jedisPool.getResource()) {
            if(!jedis.exists(KeyUtils.user(followingId)) || !jedis.exists(KeyUtils.user(userId)))
                throw new IllegalArgumentException("specified user doesn't exist.");
            Pipeline pipeline = jedis.pipelined();
            pipeline.sadd(KeyUtils.followingOf(userId), followingId.toString());
            pipeline.sadd(KeyUtils.followersOf(followingId), userId.toString());
            pipeline.zunionstore(KeyUtils.timelineOf(userId), KeyUtils.timelineOf(userId), KeyUtils.activitiesOf(followingId));
            pipeline.sync();
            return jedis.scard(KeyUtils.followingOf(userId));
        }
    }

    @RequestMapping("/{userId}/following")
    public List<User> following(@PathVariable Long userId) {
        try(Jedis jedis = jedisPool.getResource()) {
            String followingKey = KeyUtils.followingOf(userId);
            List<String> followingKeys = jedis.smembers(followingKey).parallelStream()
                    .map(x -> KeyUtils.user(Long.valueOf(x)))
                    .collect(Collectors.toList());
            return getUsers(jedis, followingKeys);
        }
    }

    @RequestMapping("/{userId}/followers")
    public List<User> followers(@PathVariable Long userId) {
        try(Jedis jedis = jedisPool.getResource()) {
            String followersKey = KeyUtils.followersOf(userId);
            List<String> followersKeys = jedis.smembers(followersKey).parallelStream()
                    .map(x -> KeyUtils.user(Long.valueOf(x)))
                    .collect(Collectors.toList());
            return getUsers(jedis, followersKeys);
        }
    }

    @RequestMapping("/{userId}/profile")
    public User profile(@PathVariable Long userId) throws IOException {
        try(Jedis jedis = jedisPool.getResource()) {
            String jsonUser = jedis.get(KeyUtils.user(userId));
            if(jsonUser == null)
                throw new IllegalArgumentException("specified user doesn't exist.");
            return userMapper.parse(jsonUser);
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

}
