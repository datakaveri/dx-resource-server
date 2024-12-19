package iudx.resource.server.databroker;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * The Data Broker Service.
 *
 * <h1>Data Broker Service</h1>
 *
 * <p>The Data Broker Service in the IUDX Resource Server defines the operations to be performed
 * with the IUDX Data Broker server.
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-05-31
 */
@VertxGen
@ProxyGen
public interface DataBrokerService {

  @GenIgnore
  static DataBrokerService createProxy(Vertx vertx, String address) {
    return new DataBrokerServiceVertxEBProxy(vertx, address);
  }

  /**
   * The registerAdaptor implements the adaptor registration operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService registerAdaptor(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateAdaptor implements the adaptor update operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService updateAdaptor(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteAdaptor implements the adaptor delete operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService deleteAdaptor(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listAdaptor implements the adaptor list operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService listAdaptor(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The registerStreamingSubscription implements the registration of streaming subscription
   * operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService registerStreamingSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateStreamingSubscription implements the updation of streaming subscription operation
   * with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService updateStreamingSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The appendStreamingSubscription implements the updation (adding new) routingkeys to streaming
   * subscription operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService appendStreamingSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteStreamingSubscription implements the deletion of streaming subscription operation
   * with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService deleteStreamingSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listStreamingSubscription implements the listing of streaming subscription operation with
   * the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService listStreamingSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createExchange implements the creation of exchange operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService createExchange(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateExchange implements the updation of exchange operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService updateExchange(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteExchange implements the deletion of exchange operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService deleteExchange(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listExchange implements the listing of exchange operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService listExchangeSubscribers(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createQueue implements the creation of queue operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService createQueue(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateQueue implements the updation of queue operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService updateQueue(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteQueue implements the deletion of queue operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService deleteQueue(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The bindQueue implements the binding of queue operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService bindQueue(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The unbindQueue implements the unbinding of queue operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService unbindQueue(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createvHost implements the creation of vHost operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService createvHost(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updatevHost implements the updation of vHost operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService updatevHost(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deletevHost implements the deletion of vHost operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService deletevHost(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listvHost implements the listing of vHost operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService listvHost(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listExchangeSubscribers implements the listing of all bindings of a queue operation with
   * the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService listQueueSubscribers(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The publishFromAdaptor implements the publish from adaptor operation with the data broker.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DataBrokerService which is a Service
   */
  @Fluent
  DataBrokerService publishFromAdaptor(
          JsonArray request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DataBrokerService resetPassword(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DataBrokerService getExchange(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DataBrokerService publishHeartbeat(
      JsonObject request, String vhost, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DataBrokerService publishMessage(
      JsonObject body,
      String toExchange,
      String routingKey,
      Handler<AsyncResult<JsonObject>> handler);
}
