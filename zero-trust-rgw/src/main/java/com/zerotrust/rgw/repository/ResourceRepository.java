package com.zerotrust.rgw.repository;

import com.zerotrust.rgw.entity.Resource;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ResourceRepository extends ReactiveCrudRepository<Resource, Long> {
    Mono<Resource> findByResourceId(String resourceId);
}