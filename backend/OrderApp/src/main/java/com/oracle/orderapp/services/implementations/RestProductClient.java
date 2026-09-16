package com.oracle.orderapp.services.implementations;
import com.oracle.orderapp.dtos.clients.*;import com.oracle.orderapp.services.abstractions.ProductClient;
import org.springframework.beans.factory.annotation.Qualifier;import org.springframework.stereotype.Component;import org.springframework.web.client.*;
@Component public class RestProductClient implements ProductClient {
 private final RestClient c; public RestProductClient(@Qualifier("productRestClient") RestClient c){this.c=c;}
 public InventoryResponse decrement(InventoryDecrementRequest r){return post("decrements",r);}
 public InventoryResponse restore(InventoryRestoreRequest r){return post("restores",r);}
 private InventoryResponse post(String action,Object r){try{return c.post().uri("/grocers/api/products/inventory/{action}",action).body(r).retrieve().body(InventoryResponse.class);}catch(RestClientException e){throw RestClientSupport.translate("products",e);}}
}
