package com.github.ontio;

import org.apache.ibatis.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.ontio.ExplorerRedisCache.SpringAccessor.getRedisTemplate;
/**
 * @author zhouq
 * @version 1.0
 * @date 2018/7/16
 */
@Component
public class ExplorerRedisCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(ExplorerRedisCache.class);

    // 读写锁
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    //private RedisTemplate<String, Object> redisTemplate = ApplicationContextProvider.getBean("redisTemplate");

    private String id = "defaultrediscacheid001";

    public ExplorerRedisCache(final String id) {

        logger.info("##init ExplorerRedisCache, Cache id:{}##", id);
        if (id == null) {
            throw new IllegalArgumentException("Cache instances require an ID");
        }
        this.id = id;
    }

    public ExplorerRedisCache() {
        logger.info("##init ExplorerRedisCache with default Cache id:{}##", this.id);
    }

    @Override
    public String getId() {
        logger.info("##get Redis Cache Id:{}##", this.id);
        return this.id;
    }

    @Override
    public void putObject(Object key, Object value) {
        logger.info("##putObject. key:{}, value:{}##", key, value);
        if (value != null) {
            // 向Redis中添加数据，有效时间是2天
            getRedisTemplate().opsForValue().set(key.toString(), value, 2, TimeUnit.DAYS);
        }
    }

    @Override
    public Object getObject(Object key) {
        logger.info("##getObject. key:{}##", key.toString());
        try {
            if (key != null) {
                Object obj = getRedisTemplate().opsForValue().get(key.toString());
       //         logger.info("##getRedisObject. value:{}", obj);
                return obj;
            }
        } catch (Exception e) {
            logger.error("redis error... ", e);
        }
        return null;
    }

    /**
     * 批量删除对应的value
     * @param keys
     */
    public void remove(final String... keys) {
        for (String key : keys) {
            removeObject(key);
        }
    }


    @Override
    public Object removeObject(Object key) {
        logger.info("##removeObject. key:{}##", key.toString());
        try {
            if (key != null) {
                getRedisTemplate().delete(key.toString());
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public void clear() {
        logger.info("clear Redis Cache,this.id:{}",this.id);
        try {
            Set<String> keys = getRedisTemplate().keys("*:" + this.id + "*");
            if (!CollectionUtils.isEmpty(keys)) {
                logger.info("keys:{}",keys);
                getRedisTemplate().delete(keys);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public int getSize() {
        logger.info("##get Redis Cache Size##");
        Long size = (Long) getRedisTemplate().execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.dbSize();
            }
        });
        return size.intValue();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        logger.info("##get Redis Cache ReadWriteLock##");
        return this.readWriteLock;
    }


    // RedisCache is instantiated by MyBatis, however we wish to use a Spring managed RedisTemplate.  To avoid race
    // conditions between Spring context initialization, and MyBatis, use getRedisTemplate() to access this.
    static final class SpringAccessor {
        private static RedisTemplate<String, Object> redisTemplate;

        static RedisTemplate<String, Object> getRedisTemplate() {
            // locally cache the RedisTemplate as that is not expected to change
            if (redisTemplate == null) {
                try {
                    redisTemplate = ApplicationContextProvider.getBean("redisTemplate");
                } catch (BeansException ex) {
                    logger.warn("##Spring Redis template is currently not available.");
                }
            }
            return redisTemplate;
        }
    }

}