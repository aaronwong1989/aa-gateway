package cn.com.aaron.gateway.repository;

import cn.com.aaron.gateway.entity.MongoRouteDefinition;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * 数据库操作JPA Repository
 * @author Aaron
 */
public interface MongoRouteDefinitionRepository extends ReactiveMongoRepository<MongoRouteDefinition, String> {

}
