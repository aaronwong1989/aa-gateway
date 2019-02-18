package cn.com.zybank.gateway.handler;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import cn.com.zybank.gateway.entity.MongoRouteDefinition;
import cn.com.zybank.gateway.repository.MongoRouteDefinitionRepository;
import cn.com.zybank.gateway.service.MongoRouteDefinitionLocator;
import java.net.URI;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.CompositeRouteDefinitionLocator;
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

  /**
   * 用于写缓存,并通知gateway更新路由信息
   */
  private final MongoRouteDefinitionLocator routerWriter;
  /**
   * 更新mongoDB
   */
  private final MongoRouteDefinitionRepository mongoJpa;

  @Resource
  private CompositeRouteDefinitionLocator routeDefinitionLocator;

  @Autowired
  public RouteManagementHandler(MongoRouteDefinitionLocator routerWriter,
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
    return ok().contentType(APPLICATION_JSON).body(saved, MongoRouteDefinition.class);
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
    return ok().contentType(APPLICATION_JSON).body(deleted, RouteDefinition.class);
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
    return ok().contentType(APPLICATION_JSON).body(findOne, MongoRouteDefinition.class);
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
    return ok().contentType(APPLICATION_JSON).body(findMany, MongoRouteDefinition.class);
  }

  /**
   * 获取所有路由信息，包括:<br /> 1. properties静态路由 <br />  2. 服务发现路由 <br /> 3. 动态配置路由 <br />
   */
  public Mono<ServerResponse> queryALL(ServerRequest request) {
    return ok().contentType(APPLICATION_JSON)
        .body(routeDefinitionLocator.getRouteDefinitions(), RouteDefinition.class);
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
