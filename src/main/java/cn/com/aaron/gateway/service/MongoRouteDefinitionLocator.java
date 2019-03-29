package cn.com.aaron.gateway.service;

import cn.com.aaron.gateway.config.GatewayRoutesRefresher;
import cn.com.aaron.gateway.repository.MongoRouteDefinitionRepository;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
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
public class MongoRouteDefinitionLocator implements RouteDefinitionRepository {

  private final Map<String, RouteDefinition> routes = new ConcurrentHashMap<>(256);

  private final GatewayRoutesRefresher refresher;
  private final MongoRouteDefinitionRepository mongoJpa;

  @Autowired
  public MongoRouteDefinitionLocator(
      MongoRouteDefinitionRepository mongoJpa,
      GatewayRoutesRefresher refresher) {
    this.mongoJpa = mongoJpa;
    this.refresher = refresher;
  }

  /**
   * 获取自定义路由信息
   * <br /> 系统会在触发refresher.refreshRoutes()时自动调用该方法更新路由信息表
   */
  @Override
  public Flux<RouteDefinition> getRouteDefinitions() {
    //判断本地缓存是否为空，不为空直接返回
    if (routes.size() > 0) {
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
  public Mono<Void> saveRoute(Mono<RouteDefinition> route) {
    return route.flatMap(routeDefinition -> {
      routes.put(routeDefinition.getId(), routeDefinition);
      refresher.refreshRoutes();
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
  public Mono<RouteDefinition> deleteBy(Mono<String> routeId) {
    return routeId.flatMap(id -> {
      if (routes.containsKey(id)) {
        RouteDefinition tmp = routes.get(id);
        routes.remove(id);
        refresher.refreshRoutes();
        return Mono.just(tmp);
      }
      return Mono.empty();
    });
  }

  @Override
  public Mono<Void> delete(Mono<String> routeId) {
    // do nothing 不响应端点的该方法
    return Mono.empty();
  }

  @Override
  public Mono<Void> save(Mono<RouteDefinition> route) {
    // do nothing 不响应端点的该方法
    return Mono.empty();
  }

  /**
   * 判断指定routeId的路由信息是否存在于缓存中
   */
  public boolean isNotCached(String routeId) {
    return !routes.containsKey(routeId);
  }

  /**
   * 从缓存中获取路由信息
   */
  private Flux<RouteDefinition> getRoutes() {
    return Flux.fromIterable(routes.values());
  }
}
