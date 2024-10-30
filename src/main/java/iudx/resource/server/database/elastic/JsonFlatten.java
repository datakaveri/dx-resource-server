package iudx.resource.server.database.elastic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class JsonFlatten {
  private final Map<String, ValueNode> json = new LinkedHashMap<>();
  private final LinkedHashMap<String, Object> jsonObj = new LinkedHashMap<>();
  private final JsonNode root;

  public JsonFlatten(JsonNode node) {
    this.root = Objects.requireNonNull(node);
  }

  public static void flattenJson(JsonNode node, String parent, Map<String, ValueNode> map) {

    if (node == null) {
      return;
    }
    if (node instanceof ValueNode) {
      map.put(parent, (ValueNode) node);
    } else {
      String prefix = parent == null ? "" : parent + ".";
      if (node instanceof ArrayNode) {
        ArrayNode arrayNode = (ArrayNode) node;
        for (int i = 0; i < arrayNode.size(); i++) {
          flattenJson(arrayNode.get(i), prefix + i, map);
        }
      } else if (node instanceof ObjectNode) {
        ObjectNode objectNode = (ObjectNode) node;
        for (Iterator<Map.Entry<String, JsonNode>> it = objectNode.fields(); it.hasNext(); ) {
          Map.Entry<String, JsonNode> field = it.next();
          flattenJson(field.getValue(), prefix + field.getKey(), map);
        }
      } else {
        throw new RuntimeException("Unknown JSON node type: " + node.getNodeType());
      }
    }
  }

  public LinkedHashMap<String, Object> flatten() {
    flattenJson(root, null, json);
    for (Map.Entry<String, ValueNode> entry : json.entrySet()) {
      String key = entry.getKey();
      ValueNode valueNode = entry.getValue();
      if (valueNode.isInt()) {
        jsonObj.put(key, valueNode.asInt());
      } else if (valueNode.isLong()) {
        jsonObj.put(key, valueNode.asLong());
      } else if (valueNode.isFloat() || valueNode.isDouble()) {
        jsonObj.put(key, valueNode.asDouble());
      } else if (valueNode.isBoolean()) {
        jsonObj.put(key, valueNode.asBoolean());
      } else if (json.get(key).isTextual()) {
        jsonObj.put(key, json.get(key).asText());
      }
    }
    return jsonObj;
  }
}
