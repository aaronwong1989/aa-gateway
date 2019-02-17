package cn.com.zybank.gateway.handler;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import cn.com.zybank.gateway.entity.MongoRouteDefinition;
import cn.com.zybank.gateway.repository.MongoRouteDefinitionRepository;
import cn.com.zybank.gateway.service.GatewayRouteDefinitionService;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Route管理的控制器
 *
 * @author Aaron
 */
@Slf4j
@Component
public class RouteManagementHandler {

  private final GatewayRouteDefinitionService routerWriter;
  private final MongoRouteDefinitionRepository mongoJpa;

  @Autowired
  public RouteManagementHandler(GatewayRouteDefinitionService routerWriter,
      MongoRouteDefinitionRepository mongoJpa) {
    this.routerWriter = routerWriter;
    this.mongoJpa = mongoJpa;
  }
  /**
   * 保存路由信息
   */
  public Mono<ServerResponse> save(ServerRequest request) {
    Mono<MongoRouteDefinition> saved = request.bodyToMono(MongoRouteDefinition.class)
        .flatMap(mongoJpa::save)
        .doOnNext(
            def -> routerWriter.saveRoute(Mono.just(this.convert(def))).subscribe()
        );
    return ServerResponse.ok().contentType(APPLICATION_JSON).body(saved, MongoRouteDefinition.class);
  }

  /**
   * 删除制定routeId的路由信息
   */
  public Mono<ServerResponse> delete(ServerRequest request) {
    String routeId = request.pathVariable("routeId");
    Mono<RouteDefinition> deleted = routerWriter.deleteBy(Mono.just(routeId))
        .doOnNext(
            def -> mongoJpa.deleteById(Mono.just(routeId)).subscribe()
        );
    return ServerResponse.ok().contentType(APPLICATION_JSON).body(deleted, RouteDefinition.class);
  }


  /**
   * 从数据库查询查询路由信息并更新缓存
   */
  public Mono<ServerResponse> queryOne(ServerRequest request) {
    String routeId = request.pathVariable("routeId");
    Mono<MongoRouteDefinition> findOne = mongoJpa.findById(Mono.just(routeId))
        .doOnNext(mongoDef -> {
          if (routerWriter.isNotCached(mongoDef.getRouteId())) {
            routerWriter.saveRoute(Mono.just(this.convert(mongoDef))).subscribe();
          }
        });
    return ServerResponse.ok().contentType(APPLICATION_JSON).body(findOne, MongoRouteDefinition.class);
  }

  /**
   * 从数据库查询查询路由信息并更新缓存
   */
  public Mono<ServerResponse> queryAll(ServerRequest request) {
    Flux<MongoRouteDefinition> findMany = mongoJpa.findAll()
        .doOnNext(mongoDef -> {
          if (routerWriter.isNotCached(mongoDef.getRouteId())) {
            routerWriter.saveRoute(Mono.just(this.convert(mongoDef))).subscribe();
          }
        });
    return ServerResponse.ok().contentType(APPLICATION_JSON).body(findMany, MongoRouteDefinition.class);
  }

  /**
   * 转换对象格式
   */
  private RouteDefinition convert(MongoRouteDefinition mongoDef) {
    RouteDefinition routeDef = new RouteDefinition();
    routeDef.setId(mongoDef.getRouteId());
    routeDef.setPredicates(mongoDef.getPredicates());
    routeDef.setFilters(mongoDef.getFilters());
    routeDef.setUri(URI.create(mongoDef.getUri()));
    routeDef.setOrder(mongoDef.getOrder());
    return routeDef;
  }
}
