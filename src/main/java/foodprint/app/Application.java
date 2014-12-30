package foodprint.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by bernard on 21/12/14.
 */
@Configuration
@PropertySource("redis.properties")
@ComponentScan({"foodprint.data", "foodprint.rest"})
@EnableAutoConfiguration
public class Application {

    @Autowired
    private Environment environment;

    @Bean
    public JedisPool jedisPool(JedisPoolConfig poolConfig) {
        String redisAddress = environment.getRequiredProperty("redis.ip");
        Integer redisPort = Integer.valueOf(environment.getRequiredProperty("redis.port"));
        JedisPool pool = new JedisPool(poolConfig, redisAddress, redisPort);
        return pool;
    }

    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(Integer.valueOf(environment.getRequiredProperty("redis.maxTotal")));
        config.setMaxIdle(Integer.valueOf(environment.getRequiredProperty("redis.maxIdle")));
        config.setMaxWaitMillis(Long.valueOf(environment.getRequiredProperty("redis.maxWait")));
        config.setTestOnBorrow(Boolean.valueOf(environment.getRequiredProperty("redis.testOnBorrow")));
        config.setTestOnReturn(Boolean.valueOf(environment.getRequiredProperty("redis.testOnReturn")));
        return config;
    }

    public static void main(String ...args) {
        SpringApplication.run(Application.class, args);
    }
}
