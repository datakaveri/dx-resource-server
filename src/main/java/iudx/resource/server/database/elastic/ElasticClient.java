package iudx.resource.server.database.elastic;


import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import iudx.resource.server.database.elastic.util.EsResponseFormatter;
import iudx.resource.server.database.elastic.util.EsResponseFormatterFactory;
import iudx.resource.server.database.elastic.util.ProgressListener;
import iudx.resource.server.database.util.ResponseBuilder;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import static iudx.resource.server.database.elastic.Constants.*;

public class ElasticClient {
  private static final Logger LOGGER =
      LogManager.getLogger(iudx.resource.server.database.elastic.ElasticClient.class);
  private final RestClient client;
  ElasticsearchClient esClient;
  ElasticsearchAsyncClient asyncClient;
  private ResponseBuilder responseBuilder;

  public ElasticClient(String databaseIp, int databasePort, String user, String password) {
    CredentialsProvider credentials = new BasicCredentialsProvider();
    credentials.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
    RestClientBuilder restClientBuilder =
        RestClient.builder(new HttpHost(databaseIp, databasePort))
            .setHttpClientConfigCallback(
                httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentials));
    client = restClientBuilder.build();

    ElasticsearchTransport transport = new RestClientTransport(client, new JacksonJsonpMapper());
    // And create the API client
    esClient = new ElasticsearchClient(transport);
    asyncClient = new ElasticsearchAsyncClient(transport);
  }

  public Future<JsonObject> asyncScroll(
      File file,
      String index,
      Query query,
      String[] source,
      String searchId,
      ProgressListener progressListener,
      String format,
      String filePath) {
    Promise<JsonObject> promise = Promise.promise();
    SearchRequest searchRequest =
        SearchRequest.of(
            e -> e.index(index).query(query).size(10000).scroll(scr -> scr.time("5m")));

    asyncClient
        .search(searchRequest, ObjectNode.class)
        .whenCompleteAsync(
            (response, ex) -> {
              String scrollId = null;
              try {
                // LOGGER.info("response : {}",response.toString());
                scrollId = response.scrollId();

                long totalHits = response.hits().total().value();
                LOGGER.debug("Total documents to be downloaded : " + totalHits);

                List<Hit<ObjectNode>> searchHits = response.hits().hits();
                LOGGER.debug("Total records : {}", searchHits.size());

                EsResponseFormatterFactory convertFactory =
                    new EsResponseFormatterFactory(format, file);
                EsResponseFormatter instance = convertFactory.createInstance();

                LOGGER.debug(file.getAbsolutePath());

                int totaldocsDownloaded = 0;
                Set<String> headers = null;

                if (format.equalsIgnoreCase("json")) {
                  instance.write(searchHits);
                } else {
                  headers = instance.writeToCsv(searchHits);
                }

                int totalIterations = totalHits < 10000 ? 1 : (int) Math.ceil(totalHits / 10000.0);
                double iterationCount = 0.0;
                double progress;
                boolean appendComma = false;
                while (searchHits != null && searchHits.size() > 0) {
                  long downloadedDocs = searchHits.size();
                  totaldocsDownloaded += downloadedDocs;

                  String downloadLogMessage = "downloaded {} docs of {} total [{} new]";
                  LOGGER.debug(downloadLogMessage, totaldocsDownloaded, totalHits, downloadedDocs);
                  iterationCount += 1;
                  progress = iterationCount / totalIterations;
                  // keeping progress at 90% of actual to update the last 10% after upload to
                  // external (s3)
                  double finalProgress = progress * 0.9;
                  Future.future(handler -> progressListener.updateProgress(finalProgress));

                  if (format.equalsIgnoreCase("json")) {
                    instance.append(searchHits, appendComma);
                  } else {
                    instance.append(searchHits, appendComma, headers);
                  }

                  ScrollRequest scrollRequest = nextScrollRequest(scrollId);
                  CompletableFuture<ScrollResponse<ObjectNode>> future =
                      asyncClient.scroll(scrollRequest, ObjectNode.class);
                  ScrollResponse<ObjectNode> scrollResponse = future.get();
                  scrollId = scrollResponse.scrollId();
                  searchHits = scrollResponse.hits().hits();
                  appendComma = true;
                }

                instance.finish();
                promise.complete();

              } catch (Exception exception) {
                promise.fail("failed for some exception");
                exception.printStackTrace();
              } finally {
                clearScrollRequest(scrollId);
              }
            });
    return promise.future();
  }

  private ScrollRequest nextScrollRequest(final String scrollId) {
    return ScrollRequest.of(
        scrollRequest -> scrollRequest.scrollId(scrollId).scroll(Time.of(t -> t.time("5m"))));
  }

  private void clearScrollRequest(String scrollId) {
    if (scrollId != null) {
      LOGGER.debug("Closing scroll request with id : {}", scrollId);
      final String finalScroll = scrollId;
      ClearScrollRequest clearScrollRequest = ClearScrollRequest.of(f -> f.scrollId(finalScroll));
      try {
        asyncClient.clearScroll(clearScrollRequest);
      } catch (Exception e) {
        LOGGER.error(e);
        LOGGER.error(e.getMessage());
      }
    }
  }

  public Future<JsonObject> asyncSearch(
      String index, Query query, int size, int from, SourceConfig sourceFilterConfig) {
    Promise<JsonObject> promise = Promise.promise();
    SearchRequest searchRequest =
        SearchRequest.of(
            e ->
                e.index(index)
                    .query(query)
                    .size(size)
                    .from(from)
                    .source(sourceFilterConfig)
                    .timeout("180s"));
    asyncClient
        .search(searchRequest, ObjectNode.class)
        .whenCompleteAsync(
            (response, exception) -> {
              if (exception != null) {
                LOGGER.error("async search query failed : {}", exception);
                promise.fail(exception);
                return;
              }
              JsonObject queryResult;
              try {
                JsonArray dbResponse = new JsonArray();
                if (response.hits().total().value() == 0) {
                  responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204);
                  responseBuilder.setMessage(EMPTY_RESPONSE);
                  promise.fail(responseBuilder.getResponse().toString());
                  return;
                }

                // TODO : explore client API docs to directly get response, avoid loop over response
                // to
                // create a seprate Json
                for (Hit<ObjectNode> esHitResponse : response.hits().hits()) {
                  queryResult = new JsonObject(esHitResponse.source().toString());
                  dbResponse.add(queryResult);
                }

                responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200);
                responseBuilder.setMessage(dbResponse);
                promise.complete(responseBuilder.getResponse());
              } catch (Exception ex) {
                LOGGER.error("Exception occurred while executing query: {}", ex);
                JsonObject dbException = new JsonObject(ex.getMessage());
                responseBuilder =
                    new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(dbException);
                promise.fail(responseBuilder.getResponse().toString());
              }
            });
    return promise.future();
  }

  public Future<JsonObject> asyncCount(String index, Query query) {
    Promise<JsonObject> promise = Promise.promise();
    CountRequest countRequest = CountRequest.of(e -> e.index(index).query(query));
    asyncClient
        .count(countRequest)
        .whenCompleteAsync(
            (response, exception) -> {
              if (exception != null) {
                LOGGER.error("async count query failed : {}", exception);
                promise.fail(exception);
                return;
              }
              try {

                long count = response.count();
                if (count == 0) {
                  responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204);
                  responseBuilder.setMessage(EMPTY_RESPONSE);
                  promise.fail(responseBuilder.getResponse().toString());
                  return;
                }
                responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200);
                responseBuilder.setCount(count);
                promise.complete(responseBuilder.getResponse());
              } catch (Exception ex) {
                LOGGER.error("Exception occurred while executing query: {}", ex);
                JsonObject dbException = new JsonObject(ex.getMessage());
                responseBuilder =
                    new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(dbException);
                promise.fail(responseBuilder.getResponse().toString());
              }
            });
    return promise.future();
  }
}
