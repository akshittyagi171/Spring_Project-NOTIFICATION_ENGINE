package com.notificationengine.notificationservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class RedisTest {
    @Autowired
    private RedisTemplate redisTemplate;


    @Test
    @Disabled
    void testSendMail() {
        redisTemplate.opsForValue().set("email","gmail@email.com");
        Object email = redisTemplate.opsForValue().get("email");
        System.out.println(email.toString());
        int a = 1;
    }
}
