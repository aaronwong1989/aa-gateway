package cn.com.zybank.gateway.config;

import cn.com.zybank.gateway.repository.GatewayRouteDefinitionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Aaron
 */
@Configuration
public class GatewayConfiguration {

  private final GatewayRouteDefinitionRepository routerWriter;

  public GatewayConfiguration(
      GatewayRouteDefinitionRepository routerWriter) {
    this.routerWriter = routerWriter;
  }

  @Bean
  public CommandLineRunner getRoutes() {
    // 初始化时调用一次加载路由信息,避免增加路由动作屏蔽掉第一次数据库加载
    return (String... args) -> {
      routerWriter.getRouteDefinitions().log().subscribe();
    };
  }

}
