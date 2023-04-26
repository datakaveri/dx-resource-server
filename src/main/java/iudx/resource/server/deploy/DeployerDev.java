package iudx.resource.server.deploy;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.Option;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Deploys non-clustered vert.x instance of the server. As a JAR, the application requires 1 runtime
 * argument:
 *
 * <ul>
 *   <li>--config/-c : path to the config file
 * </ul>
 * <p></p>
 * e.g. <i>java -jar ./fatjar.jar -c configs/config.json</i>
 */
public class DeployerDev {
  private static final Logger LOGGER = LogManager.getLogger(DeployerDev.class);

  public static void recursiveDeploy(Vertx vertx, JsonObject configs, int i) {
    if (i >= configs.getJsonArray("modules").size()) {
      LOGGER.info("Deployed all");
      return;
    }
    JsonObject moduleConfigurations = getConfigForModule(i, configs);
    moduleConfigurations.put("host", configs.getString("host"));
    String moduleName = moduleConfigurations.getString("id");
    int numInstances = moduleConfigurations.getInteger("verticleInstances");
    DeploymentOptions deploymentOptions =
        new DeploymentOptions().setInstances(numInstances).setConfig(moduleConfigurations);

    boolean isWorkerVerticle = moduleConfigurations.getBoolean("isWorkerVerticle");
    if (isWorkerVerticle) {
      LOGGER.info("worker verticle : " + moduleConfigurations.getString("id"));
      deploymentOptions.setWorkerPoolName(moduleConfigurations.getString("threadPoolName"));
      deploymentOptions.setWorkerPoolSize(moduleConfigurations.getInteger("threadPoolSize"));
      deploymentOptions.setWorker(true);
      deploymentOptions.setMaxWorkerExecuteTime(30L);
      deploymentOptions.setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES);
    }

    vertx.deployVerticle(
        moduleName,
        deploymentOptions,
        ar -> {
          if (ar.succeeded()) {
            LOGGER.info("Deployed " + moduleName);
            recursiveDeploy(vertx, configs, i + 1);
          } else {
            LOGGER.fatal("Failed to deploy " + moduleName + " cause:", ar.cause());
          }
        });
  }

  private static JsonObject getConfigForModule(int moduleIndex, JsonObject configurations) {
    JsonObject commonConfigs = configurations.getJsonObject("commonConfig");
    JsonObject config = configurations.getJsonArray("modules").getJsonObject(moduleIndex);
    return config.mergeIn(commonConfigs, true);
  }

  public static void deploy(String configPath) {
    EventBusOptions ebOptions = new EventBusOptions();
    VertxOptions options = new VertxOptions().setEventBusOptions(ebOptions);

    String config;
    try {
      config = new String(Files.readAllBytes(Paths.get(configPath)), StandardCharsets.UTF_8);
    } catch (Exception e) {
      LOGGER.fatal("Couldn't read configuration file");
      return;
    }
    if (config.length() < 1) {
      LOGGER.fatal("Couldn't read configuration file");
      return;
    }
    JsonObject configuration = new JsonObject(config);
    Vertx vertx = Vertx.vertx(options);
    recursiveDeploy(vertx, configuration, 0);
  }

  public static void main(String[] args) {
    CLI cli =
        CLI.create("IUDX Cat")
            .setSummary("A CLI to deploy the resource server")
            .addOption(
                new Option()
                    .setLongName("help")
                    .setShortName("h")
                    .setFlag(true)
                    .setDescription("display help"))
            .addOption(
                new Option()
                    .setLongName("config")
                    .setShortName("c")
                    .setRequired(true)
                    .setDescription("configuration file"));

    StringBuilder usageString = new StringBuilder();
    cli.usage(usageString);
    CommandLine commandLine = cli.parse(Arrays.asList(args), false);
    if (commandLine.isValid() && !commandLine.isFlagEnabled("help")) {
      String configPath = commandLine.getOptionValue("config");
      deploy(configPath);
    } else {
      LOGGER.info(usageString);
    }
  }
}
