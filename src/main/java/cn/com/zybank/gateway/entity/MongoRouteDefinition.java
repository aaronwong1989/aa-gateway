package cn.com.zybank.gateway.entity;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 存储在mongodb中的自定义路由信息
 * @author aaron
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "gateway_routers")
public class MongoRouteDefinition {
  /** 路由id, mongodb中的 _id */
  @Id
  private String routeId;
  /** 路由谓词 */
  private List<PredicateDefinition> predicates = new ArrayList<>();
  /** 过滤器 */
  private List<FilterDefinition> filters = new ArrayList<>();
  /** 跳转地址uri */
  private String uri;
  /** 路由顺序 */
  private int order;
}