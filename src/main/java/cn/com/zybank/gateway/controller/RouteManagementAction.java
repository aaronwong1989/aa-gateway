package cn.com.zybank.gateway.controller;

import cn.com.zybank.gateway.entity.MongoRouteDefinition;
import cn.com.zybank.gateway.repository.MongoRouteDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Route管理的控制器
 *
 * @author Aaron
 */
@RestController
public class RouteManagementAction {

  final MongoRouteDefinitionRepository mongoRouteDefinitionRepository;

  @Autowired
  public RouteManagementAction(MongoRouteDefinitionRepository mongoRouteDefinitionRepository) {
    this.mongoRouteDefinitionRepository = mongoRouteDefinitionRepository;
  }

  @PostMapping("/{routeId}")
  public Mono<MongoRouteDefinition> save(@PathVariable String routeId) {
    return Mono.empty();
  }
}
