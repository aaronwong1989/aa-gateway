package cn.com.zybank.gateway.dao.repository;

import cn.com.zybank.gateway.dao.entity.MongoRouteDefinition;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * 数据库操作JPA Repository
 * @author Aaron
 */
public interface MongoRouteDefinitionJpaRepository extends ReactiveMongoRepository<MongoRouteDefinition, String> {

}
