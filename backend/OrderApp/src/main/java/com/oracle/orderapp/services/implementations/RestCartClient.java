package com.oracle.orderapp.services.implementations;
import com.oracle.orderapp.dtos.clients.*;import com.oracle.orderapp.services.abstractions.CartClient;
import org.springframework.beans.factory.annotation.Qualifier;import org.springframework.stereotype.Component;import org.springframework.web.client.*;
@Component public class RestCartClient implements CartClient {
 private final RestClient c; public RestCartClient(@Qualifier("cartRestClient") RestClient c){this.c=c;}
 public CartResponse get(Integer id){try{return c.get().uri("/grocers/api/carts/{id}",id).retrieve().body(CartResponse.class);}catch(RestClientException e){throw RestClientSupport.translate("cart",e);}}
 public CartResponse checkout(Integer id,CartTransitionRequest r){return post(id,"checkout",r);}
 public CartResponse restore(Integer id,CartTransitionRequest r){return post(id,"restore",r);}
 private CartResponse post(Integer id,String action,CartTransitionRequest r){try{return c.post().uri("/grocers/api/carts/{id}/{action}",id,action).body(r).retrieve().body(CartResponse.class);}catch(RestClientException e){throw RestClientSupport.translate("cart",e);}}
}
