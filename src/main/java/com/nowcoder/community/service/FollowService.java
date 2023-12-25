package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {

    // RedisTemplate 用于操作 Redis
    @Autowired
    private RedisTemplate redisTemplate;

    // UserService 用于用户相关操作
    @Autowired
    private UserService userService;

    /**
     * 关注一个实体（用户、帖子等）
     * @param userId 用户的ID
     * @param entityType 实体类型
     * @param entityId 实体的ID
     */
    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                // 获取关注者和被关注者的键
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                operations.multi(); // 开始事务

                // 添加关注者和被关注者信息到 Redis
                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());

                return operations.exec(); // 执行事务
            }
        });
    }

    /**
     * 取消关注一个实体
     * @param userId 用户的ID
     * @param entityType 实体类型
     * @param entityId 实体的ID
     */
    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                // 获取关注者和被关注者的键
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                operations.multi(); // 开始事务

                // 从 Redis 中移除关注者和被关注者信息
                operations.opsForZSet().remove(followeeKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);

                return operations.exec(); // 执行事务
            }
        });
    }

    /**
     * 查询关注的实体的数量
     * @param userId 用户的ID
     * @param entityType 实体类型
     * @return 关注的数量
     */
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    /**
     * 查询实体的粉丝数量
     * @param entityType 实体类型
     * @param entityId 实体的ID
     * @return 粉丝数量
     */
    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    /**
     * 查询当前用户是否已关注该实体
     * @param userId 用户的ID
     * @param entityType 实体类型
     * @param entityId 实体的ID
     * @return 是否已关注
     */
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    /**
     * 查询某用户关注的人
     * @param userId 用户的ID
     * @param offset 分页的偏移量
     * @param limit 每页数量
     * @return 关注的用户列表，包含用户信息和关注时间
     */
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        // 通过用户ID和实体类型（这里固定为用户）获取关注者列表的Redis键
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);

        // 从Redis中获取一段范围内的关注者ID集合，这里使用反向范围查询来按时间倒序获取
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);

        // 如果没有关注者ID，返回null
        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();

            // 通过ID查找用户信息
            User user = userService.findUserById(targetId);
            map.put("user", user);

            // 获取关注的时间，并转换为Date对象
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime", new Date(score.longValue()));

            // 添加到列表中
            list.add(map);
        }

        return list;
    }

    /**
     * 查询某用户的粉丝
     * @param userId 用户的ID
     * @param offset 分页的偏移量
     * @param limit 每页数量
     * @return 粉丝列表，包含粉丝的用户信息和关注时间
     */
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        // 通过用户ID和实体类型（这里固定为用户）获取粉丝列表的Redis键
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);

        // 从Redis中获取一段范围内的粉丝ID集合，这里使用反向范围查询来按时间倒序获取
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);

        // 如果没有粉丝ID，返回null
        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();

            // 通过ID查找用户信息
            User user = userService.findUserById(targetId);
            map.put("user", user);

            // 获取关注的时间，并转换为Date对象
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime", new Date(score.longValue()));

            // 添加到列表中
            list.add(map);
        }
        return list;
    }

}
