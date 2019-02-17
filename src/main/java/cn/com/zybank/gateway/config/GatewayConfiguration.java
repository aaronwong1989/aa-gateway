package cn.com.zybank.gateway.config;

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

import cn.com.zybank.gateway.handler.RouteManagementHandler;
import cn.com.zybank.gateway.service.GatewayRouteDefinitionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @author Aaron
 */
@Configuration
public class GatewayConfiguration {

  private final GatewayRouteDefinitionService routerWriter;

  public GatewayConfiguration(
      GatewayRouteDefinitionService routerWriter) {
    this.routerWriter = routerWriter;
  }

  @Bean
  public CommandLineRunner getRoutes() {
    // 初始化时调用一次加载路由信息,避免增加路由动作屏蔽掉第一次数据库加载
    return (String... args) -> {
      routerWriter.getRouteDefinitions().log().subscribe();
    };
  }

  @Bean
  public RouterFunction<ServerResponse> routeCity(RouteManagementHandler handler) {
    return RouterFunctions
        .route(POST("/gw-mgr/routes").and(accept(MediaType.APPLICATION_JSON)), handler::save)
        .andRoute(GET("/gw-mgr/routes").and(accept(MediaType.APPLICATION_JSON)), handler::queryAll)
        .andRoute(GET("/gw-mgr/routes/{routeId}").and(accept(MediaType.APPLICATION_JSON)),handler::queryOne)
        .andRoute(DELETE("/gw-mgr/routes/{routeId}").and(accept(MediaType.APPLICATION_JSON)),handler::delete)
        ;
  }
}
