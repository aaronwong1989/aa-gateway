package cn.com.zybank.gateway.repository;

import cn.com.zybank.gateway.GatewayRoutesRefresher;
import cn.com.zybank.gateway.entity.MongoRouteDefinition;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 使用Mongo保存自定义路由配置（代替默认的InMemoryRouteDefinitionRepository） <br /> 首次调用会把自定义路由配置信息加载到缓存中，以后的每次调用都从缓存返回
 *
 * @author Aaron
 */
@Component
@Slf4j
public class MongoRouteDefinitionRepository implements RouteDefinitionRepository {

  private final Map<String, RouteDefinition> routes = new ConcurrentHashMap<>(256);

  private final GatewayRoutesRefresher refresher;
  private final MongoRouteDefinitionJpaRepository mongoJpa;

  @Autowired
  public MongoRouteDefinitionRepository(
      MongoRouteDefinitionJpaRepository mongoJpa,
      GatewayRoutesRefresher refresher) {
    this.mongoJpa = mongoJpa;
    this.refresher = refresher;
  }


  /**
   * 获取自定义路由信息 <br /> 系统会在触发refresher.refreshRoutes()时自动调用改方法更新路由信息表
   */
  @Override
  public Flux<RouteDefinition> getRouteDefinitions() {
    //判断本地缓存是否为空，不为空直接返回
    if (routes.size() > 0) {
      if (log.isDebugEnabled()) {
        log.debug("getRouteDefinitions from ram");
      }
      return getRoutes();
    }
    if (log.isDebugEnabled()) {
      log.debug("getRouteDefinitions from db");
    }
    return mongoJpa.findAll().flatMap(define -> {
      RouteDefinition routeDefinition = new RouteDefinition();
      routeDefinition.setId(define.getRouteId());
      routeDefinition.setFilters(define.getFilters());
      routeDefinition.setPredicates(define.getPredicates());
      routeDefinition.setUri(URI.create(define.getUri()));
      routeDefinition.setOrder(define.getOrder());
      routes.put(routeDefinition.getId(), routeDefinition);
      return Mono.just(routeDefinition);
    });
  }

  /**
   * 新增路由信息
   *
   * @param route 路由定义对象
   * @return reactor.core.publisher.Mono<java.lang.Void>
   * @author aaron
   */
  @Override
  public Mono<Void> save(Mono<RouteDefinition> route) {
    return route.flatMap(routeDefinition -> {
      MongoRouteDefinition mongoRouteDefinition = new MongoRouteDefinition();
      mongoRouteDefinition.setRouteId(routeDefinition.getId());
      mongoRouteDefinition.setPredicates(routeDefinition.getPredicates());
      mongoRouteDefinition.setFilters(routeDefinition.getFilters());
      mongoRouteDefinition.setUri(routeDefinition.getUri().toString());
      mongoRouteDefinition.setOrder(routeDefinition.getOrder());

      routes.put(routeDefinition.getId(), routeDefinition);
      refresher.refreshRoutes();

      //TODO 不调用.subscribe()数据库中就没有, Why?
      mongoJpa.save(mongoRouteDefinition).log().subscribe();

      return Mono.empty();
    });

  }

  /**
   * 删除路由信息
   *
   * @param routeId 路由id
   * @return reactor.core.publisher.Mono<java.lang.Void>
   * @author aaron
   */
  @Override
  public Mono<Void> delete(Mono<String> routeId) {
    return routeId.flatMap(id -> {
      if (routes.containsKey(id)) {
        routes.remove(id);
        if (log.isDebugEnabled()) {
          log.debug("delete RouteDefinitions from db");
        }
        mongoJpa.deleteById(routeId);
        refresher.refreshRoutes();
        return Mono.empty();
      }
      return Mono
          .defer(() -> Mono.error(new NotFoundException("RouteDefinition not found: " + routeId)));
    });
  }


  /**
   * 从缓存中获取路由信息
   */
  private Flux<RouteDefinition> getRoutes() {
    return Flux.fromIterable(routes.values());
  }

}
