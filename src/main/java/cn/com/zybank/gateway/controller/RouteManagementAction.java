package cn.com.zybank.gateway.controller;

import cn.com.zybank.gateway.entity.MongoRouteDefinition;
import cn.com.zybank.gateway.repository.GatewayRouteDefinitionRepository;
import cn.com.zybank.gateway.repository.MongoRouteDefinitionRepository;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Route管理的控制器
 *
 * @author Aaron
 */
@RestController
@RequestMapping("/gw-mgr/routes")
@Slf4j
public class RouteManagementAction {

  private final GatewayRouteDefinitionRepository routerWriter;
  private final MongoRouteDefinitionRepository mongoJpa;

  @Autowired
  public RouteManagementAction(GatewayRouteDefinitionRepository routerWriter,
      MongoRouteDefinitionRepository mongoJpa) {
    this.routerWriter = routerWriter;
    this.mongoJpa = mongoJpa;
  }

  /**
   * 保存路由信息
   */
  @PostMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<MongoRouteDefinition> save(
      @RequestBody Mono<MongoRouteDefinition> mongoRouteDefinition) {
    return mongoRouteDefinition.flatMap(mongoJpa::save).log()
        .doOnSuccess(mongoDef ->
            routerWriter.saveRoute(Mono.just(this.convert(mongoDef))).subscribe()
        );
  }

  /**
   * 删除制定routeId的路由信息
   */
  @DeleteMapping(path = "/{routeId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<RouteDefinition> delete(@PathVariable String routeId) {
    return routerWriter.deleteBy(Mono.just(routeId)).log().doOnSuccess(def -> {
          mongoJpa.deleteById(Mono.just(routeId)).subscribe();
        }
    );
  }


  /**
   * 从数据库查询查询路由信息并更新缓存
   */
  @GetMapping(path = "/{routeId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<MongoRouteDefinition> queryByWithUpdate(@PathVariable String routeId) {
    return mongoJpa.findById(Mono.just(routeId)).doOnSuccess(mongoDef -> {
      // 检查在缓存中是否存在,不存在的话加入缓存
      if (routerWriter.isNotCached(mongoDef.getRouteId())) {
        routerWriter.saveRoute(Mono.just(this.convert(mongoDef))).subscribe();
      }
    });
  }

  /**
   * 从数据库查询查询路由信息并更新缓存
   */
  @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
  public Flux<MongoRouteDefinition> queryAllWithUpdate() {
    return mongoJpa.findAll().doOnNext(mongoDef -> {
      // 检查每一个在缓存中是否存在,不存在的话加入缓存
      if (routerWriter.isNotCached(mongoDef.getRouteId())) {
        routerWriter.saveRoute(Mono.just(this.convert(mongoDef))).subscribe();
      }
    });
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
