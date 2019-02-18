package cn.com.zybank.gateway.config;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

import cn.com.zybank.gateway.handler.RouteManagementHandler;
import cn.com.zybank.gateway.service.MongoRouteDefinitionLocator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @author Aaron
 */
@Configuration
public class GatewayConfiguration {

  private final MongoRouteDefinitionLocator routerWriter;

  public GatewayConfiguration(
      MongoRouteDefinitionLocator routerWriter) {
    this.routerWriter = routerWriter;
  }

  @Bean
  public CommandLineRunner getRoutesBeforeStartup() {
    // 初始化时调用一次加载路由信息,避免增加路由动作屏蔽掉第一次数据库加载
    return (String... args) -> {
      routerWriter.getRouteDefinitions().log().subscribe();
    };
  }

  @Bean
  public RouterFunction<ServerResponse> gwRoute(RouteManagementHandler handler) {
    return RouterFunctions
        .route(POST("/gw-mgr/routes").and(accept(APPLICATION_JSON)), handler::save)
        .andRoute(GET("/gw-mgr/routes").and(accept(APPLICATION_JSON)), handler::queryAll)
        .andRoute(GET("/gw-mgr/routes/{routeId}").and(accept(APPLICATION_JSON)),handler::queryOne)
        .andRoute(DELETE("/gw-mgr/routes/{routeId}").and(accept(APPLICATION_JSON)),handler::delete)
        .andRoute(GET("/gw-mgr/routes-all").and(accept(APPLICATION_JSON)), handler::queryALL)
        ;
  }

  @Bean
  public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("path_route_gitee", r -> r.path("/gitee").uri("https://gitee.com/eblog"))
        .build();
  }
}
