package iudx.resource.server.configuration;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Configuration {

  private static FileSystem fileSystem;
  private static final Logger LOGGER = LogManager.getLogger(Configuration.class);
  private static final String CONFIG_PATH = "./configs/config-test.json";

  /**
   * This is to read the config.json file from fileSystem to load configuration.
   * 
   * @param moduleIndex
   * @param vertx
   * @return module JsonObject
   */
  public JsonObject configLoader(int moduleIndex, Vertx vertx) {

    fileSystem = vertx.fileSystem();
    JsonObject moduleConf = null;

    /* configuration setup */
    File file = new File(CONFIG_PATH);

    if (file.exists()) {
      Buffer buff = fileSystem.readFileBlocking(CONFIG_PATH);
      JsonArray conf = buff.toJsonObject().getJsonArray("modules");
      moduleConf = conf.getJsonObject(moduleIndex);
      moduleConf.put("host", buff.toJsonObject().getString("host"));

    } else {
      LOGGER.fatal("Couldn't read configuration file; Path: " + CONFIG_PATH);
    }

    return moduleConf;
  }

  public JsonObject configLoader(String moduleName, Vertx vertx)
  {
    fileSystem = vertx.fileSystem();
    JsonObject moduleConf = new JsonObject();

    /* configuration setup */
    File file = new File(CONFIG_PATH);

    if (file.exists()) {
      Buffer buff = fileSystem.readFileBlocking(CONFIG_PATH);
      JsonArray allVerticles = buff.toJsonObject().getJsonArray("modules");

      List<JsonObject> requiredVerticle = allVerticles.stream()
              .map(JsonObject::mapFrom)
              .filter(jsonObject -> jsonObject.getString("id").contains(moduleName))
              .toList();

      /* adding the first verticle considering there would be one matching verticle with the given module name*/
      moduleConf = requiredVerticle.getFirst();
    } else {
      LOGGER.fatal("Couldn't read configuration file; Path: " + CONFIG_PATH);
    }

    return moduleConf;
  }

}