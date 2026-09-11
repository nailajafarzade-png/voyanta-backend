package com.voyanta.homepage.service;


import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.homepage.dto.response.HomepageStatsResponse;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.enums.PlanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomepageStatsService {

    private static final String CACHE_KEY = "homepage:stats";

    private final TravelPlanRepository travelPlanRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final VoyantaProperties properties;

    public HomepageStatsResponse getStats() {
        HomepageStatsResponse cached = (HomepageStatsResponse) redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        // DB-də heç bir plan olmasa belə count() sadəcə 0 qaytarır, exception atmır —
        // "boş data 500 verməsin" tələbi buna görə əlavə qorumaya ehtiyac duymur.
        long plansCreated = travelPlanRepository.countByStatus(PlanStatus.READY);
        HomepageStatsResponse stats = new HomepageStatsResponse(plansCreated, properties.getHomepage().getAvgRating());

        redisTemplate.opsForValue().set(CACHE_KEY, stats, properties.getHomepage().getStatsCacheTtl());
        return stats;
    }
}