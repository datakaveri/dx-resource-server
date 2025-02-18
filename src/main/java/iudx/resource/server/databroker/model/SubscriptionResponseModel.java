package iudx.resource.server.databroker.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import static iudx.resource.server.databroker.util.Constants.*;

@DataObject
public class SubscriptionResponseModel {

    private String userId;
    private String apiKey;
    private String queue;
    private String url;
    private int port;
    private String vHost;

    // Default Constructor (needed for Vert.x DataObject)
    public SubscriptionResponseModel() {}

    // Parameterized Constructor
    public SubscriptionResponseModel(String userId, String apiKey, String queue, String url, int port, String vHost) {
        this.userId = userId;
        this.apiKey = apiKey;
        this.queue = queue;
        this.url = url;
        this.port = port;
        this.vHost = vHost;
    }

    // Constructor to create from JSON
    public SubscriptionResponseModel(JsonObject json) {
        this.userId = json.getString(USER_NAME);
        this.apiKey = json.getString(APIKEY);
        this.queue = json.getString("id");
        this.url = json.getString(URL);
        this.port = json.getInteger(PORT); // Default value if missing
        this.vHost = json.getString(VHOST);
    }

    // Convert to JSON
    public JsonObject toJson() {
        return new JsonObject()
                .put(USER_NAME, userId)
                .put(APIKEY, apiKey)
                .put(ID, queue)
                .put(URL, url)
                .put(PORT, port)
                .put(VHOST, vHost);
    }

    // Getters
    public String getUserId() { return userId; }
    public String getApiKey() { return apiKey; }
    public String getQueue() { return queue; }
    public String getUrl() { return url; }
    public int getPort() { return port; }
    public String getVHost() { return vHost; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setQueue(String queue) { this.queue = queue; }
    public void setUrl(String url) { this.url = url; }
    public void setPort(int port) { this.port = port; }
    public void setVHost(String vHost) { this.vHost = vHost; }

    @Override
    public String toString() {
        return "SubscriptionResponseModel{" +
                "userId='" + userId + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", queue='" + queue + '\'' +
                ", url='" + url + '\'' +
                ", port=" + port +
                ", vHost='" + vHost + '\'' +
                '}';
    }
}
