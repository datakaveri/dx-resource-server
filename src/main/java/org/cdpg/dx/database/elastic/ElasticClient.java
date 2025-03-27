package iudx.resource.server.database.elastic;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

public class ElasticClient {

  private final ElasticsearchAsyncClient client;
  private final RestClient rsClient;

  public ElasticClient(
          String databaseIp,
          int databasePort,
          String databaseUser,
          String databasePassword) {

    CredentialsProvider credentials = new BasicCredentialsProvider();
    credentials.setCredentials(
            AuthScope.ANY, new UsernamePasswordCredentials(databaseUser, databasePassword));

    this.rsClient =
            RestClient.builder(new HttpHost(databaseIp, databasePort))
                    .setHttpClientConfigCallback(
                            httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentials))
                    .build();

    // Enable Jackson to allow comments in the JSON response
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS); // This allows JSON comments

    // Handle special characters, null fields, and unknown fields
    objectMapper.configure(
            JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true); // Allow unquoted field names
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // Skip null fields
    objectMapper.configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Ignore unknown properties

    // Create the JacksonJsonpMapper with the modified ObjectMapper
    JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);
    ElasticsearchTransport transport = new RestClientTransport(rsClient, jsonpMapper);
    this.client = new ElasticsearchAsyncClient(transport);
  }

  // to get the client for external use
  public ElasticsearchAsyncClient getClient() {
    return client;
  }

  public void close() throws IOException {
    rsClient.close();
  }
}