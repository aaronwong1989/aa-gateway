package cn.com.zybank.gateway.repository;

import cn.com.zybank.gateway.entity.MongoRouteDefinition;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * 数据库操作JPA Repository
 * @author Aaron
 */
public interface MongoRouteDefinitionRepository extends ReactiveMongoRepository<MongoRouteDefinition, String> {

}
