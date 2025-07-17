package iudx.resource.server.deploy;

import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.zookeeper.ZookeeperDiscoveryProperties;
import com.hazelcast.zookeeper.ZookeeperDiscoveryStrategyFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
// JVM metrics imports
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.Option;
import io.vertx.core.cli.TypedOption;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

public class Deployer {
  private static final Logger logger = LogManager.getLogger(Deployer.class);
  private static ClusterManager mgr;
  private static Vertx vertx;

  /**
   * Recursively deploy all modules.
   *
   * @param vertx the vert.x instance
   * @param configs the JSON configuration
   * @param i for recursive base case
   */
  public static void recursiveDeploy(Vertx vertx, JsonObject configs, int i) {
    if (i >= configs.getJsonArray("modules").size()) {
      logger.info("Deployed all");
      return;
    }
    JsonObject moduleConfigurations = getConfigForModule(i, configs);
    String moduleName = moduleConfigurations.getString("id");
    int numInstances = moduleConfigurations.getInteger("verticleInstances");

    DeploymentOptions deploymentOptions =
        new DeploymentOptions().setInstances(numInstances).setConfig(moduleConfigurations);

    boolean isWorkerVerticle = moduleConfigurations.getBoolean("isWorkerVerticle");
    if (isWorkerVerticle) {
      logger.info("worker verticle : " + moduleConfigurations.getString("id"));
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
            logger.info("Deployed " + moduleName);
            recursiveDeploy(vertx, configs, i + 1);
          } else {
            logger.fatal("Failed to deploy " + moduleName + " cause:", ar.cause());
          }
        });
  }

  /**
   * Recursively deploy modules/verticles (if they exist) present in the `modules` list.
   *
   * @param vertx the vert.x instance
   * @param configs the JSON configuration
   * @param modules the list of modules to deploy
   */
  public static void recursiveDeploy(Vertx vertx, JsonObject configs, List<String> modules) {
    if (modules.isEmpty()) {
      logger.info("Deployed requested verticles");
      return;
    }

    JsonArray configuredModules = configs.getJsonArray("modules");

    String moduleName = modules.get(0);
    JsonObject config =
        configuredModules.stream()
            .map(obj -> (JsonObject) obj)
            .filter(obj -> obj.getString("id").equals(moduleName))
            .findFirst()
            .orElse(new JsonObject());

    if (config.isEmpty()) {
      logger.fatal("Failed to deploy " + moduleName + " cause: Not Found");
      return;
    }
    // get common configs and add this to config object
    JsonObject commonConfigs = configs.getJsonObject("commonConfig");
    config.mergeIn(commonConfigs, true);
    int numInstances = config.getInteger("verticleInstances");
    DeploymentOptions deploymentOptions =
        new DeploymentOptions().setInstances(numInstances).setConfig(config);
    boolean isWorkerVerticle = config.getBoolean("isWorkerVerticle");
    if (isWorkerVerticle) {
      logger.info("worker verticle : " + config.getString("id"));
      deploymentOptions.setWorkerPoolName(config.getString("threadPoolName"));
      deploymentOptions.setWorkerPoolSize(config.getInteger("threadPoolSize"));
      deploymentOptions.setWorker(true);
      deploymentOptions.setMaxWorkerExecuteTime(30L);
      deploymentOptions.setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES);
    }

    vertx.deployVerticle(
        moduleName,
        deploymentOptions,
        ar -> {
          if (ar.succeeded()) {
            logger.info("Deployed " + moduleName);
            modules.remove(0);
            recursiveDeploy(vertx, configs, modules);
          } else {
            logger.fatal("Failed to deploy " + moduleName + " cause:", ar.cause());
          }
        });
  }

  private static JsonObject getConfigForModule(int moduleIndex, JsonObject configurations) {
    JsonObject commonConfigs = configurations.getJsonObject("commonConfig");
    JsonObject config = configurations.getJsonArray("modules").getJsonObject(moduleIndex);
    return config.mergeIn(commonConfigs, true);
  }

  public static ClusterManager getClusterManager(
      String host, List<String> zookeepers, String clusterId) {
    Config config = new Config();
    config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
    config.getNetworkConfig().setPublicAddress(host);
    config.setProperty("hazelcast.discovery.enabled", "true");
    config.setProperty("hazelcast.logging.type", "log4j2");
    DiscoveryStrategyConfig discoveryStrategyConfig =
        new DiscoveryStrategyConfig(new ZookeeperDiscoveryStrategyFactory());
    discoveryStrategyConfig.addProperty(
        ZookeeperDiscoveryProperties.ZOOKEEPER_URL.key(), String.join(",", zookeepers));
    discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.GROUP.key(), clusterId);
    config
        .getNetworkConfig()
        .getJoin()
        .getDiscoveryConfig()
        .addDiscoveryStrategyConfig(discoveryStrategyConfig);

    return new HazelcastClusterManager(config);
  }

  public static MetricsOptions getMetricsOptions() {
    return new MicrometerMetricsOptions()
        .setPrometheusOptions(
            new VertxPrometheusOptions()
                .setEnabled(true)
                .setStartEmbeddedServer(true)
                .setEmbeddedServerOptions(new HttpServerOptions().setPort(9000)))
        // .setPublishQuantiles(true))
        .setLabels(
            EnumSet.of(Label.EB_ADDRESS, Label.EB_FAILURE, Label.HTTP_CODE, Label.HTTP_METHOD))
        .setEnabled(true);
  }

  public static void setJvmMetrics() {
    MeterRegistry registry = BackendRegistries.getDefaultNow();
    logger.debug(registry);
    new ClassLoaderMetrics().bindTo(registry);
    new JvmMemoryMetrics().bindTo(registry);
    new JvmGcMetrics().bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);
  }

  /**
   * Deploy clustered vert.x instance.
   *
   * @param configPath the path for JSON config file
   * @param host String
   * @param modules list of modules to deploy. If list is empty, all modules are deployed
   */
  public static void deploy(String configPath, String host, List<String> modules) {
    String config;
    try {
      config = new String(Files.readAllBytes(Paths.get(configPath)), StandardCharsets.UTF_8);
    } catch (Exception e) {
      logger.fatal("Couldn't read configuration file", e);
      return;
    }
    if (config.length() < 1) {
      logger.fatal("Configuration file is empty");
      return;
    }
    JsonObject configuration = new JsonObject(config);
    List<String> zookeepers = configuration.getJsonArray("zookeepers").getList();
    String clusterId = configuration.getString("clusterId");
    mgr = getClusterManager(host, zookeepers, clusterId);
    EventBusOptions ebOptions = new EventBusOptions().setClusterPublicHost(host);
    VertxOptions options =
        new VertxOptions()
            .setClusterManager(mgr)
            .setEventBusOptions(ebOptions)
            .setMetricsOptions(getMetricsOptions());
    logger.debug("metrics-options" + options.getMetricsOptions());
    Vertx.clusteredVertx(
        options,
        res -> {
          if (res.succeeded()) {
            vertx = res.result();
            logger.debug(vertx.isMetricsEnabled());
            setJvmMetrics();
            if (modules.isEmpty()) {
              recursiveDeploy(vertx, configuration, 0);
            } else {
              recursiveDeploy(vertx, configuration, modules);
            }
          } else {
            logger.fatal("Could not join cluster", res.cause());
          }
        });
  }

  public static void gracefulShutdown() {
    Logger logger1 = LogManager.getLogger(Deployer.class);
    logger1.info("Shutting down the application");

    if (vertx == null) {
      logger1.warn("Vert.x instance is null. Skipping undeployment and shutdown.");
      return;
    }

    Set<String> deployIdSet = vertx.deploymentIDs();
    CountDownLatch latchVerticles = new CountDownLatch(deployIdSet.size());
    CountDownLatch latchVertx = new CountDownLatch(1);

    logger1.debug("Number of verticles being undeployed: " + deployIdSet.size());

    for (String deploymentId : deployIdSet) {
      vertx.undeploy(
          deploymentId,
          handler -> {
            if (handler.succeeded()) {
              logger1.debug(deploymentId + " verticle successfully undeployed");
            } else {
              logger1.warn(deploymentId + " undeploy failed!");
            }
            latchVerticles.countDown();
          });
    }

    try {
      latchVerticles.await(5, TimeUnit.SECONDS);
      logger1.info("All verticles undeployed");

      if (mgr != null) {
        Promise<Void> promise = Promise.promise();
        mgr.leave(promise);
        logger1.info("Vert.x left cluster successfully");
      }
    } catch (Exception e) {
      logger1.error("Exception while undeploying or leaving cluster", e);
    }

    try {
      vertx.close(
          handler -> {
            if (handler.succeeded()) {
              logger1.info("Vert.x closed successfully");
            } else {
              logger1.warn("Vert.x did not close properly: " + handler.cause());
            }
            latchVertx.countDown();
          });
      latchVertx.await(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      logger1.error("Exception while closing Vert.x", e);
    }

    try {
      if (LogManager.getContext() instanceof LoggerContext) {
        logger1.debug("Shutting down log4j2");
        LogManager.shutdown((LoggerContext) LogManager.getContext());
      } else {
        logger1.warn("Unable to shutdown log4j2");
      }
    } catch (Exception e) {
      logger1.error("Exception while shutting down log4j2", e);
    }
  }

  public static void main(String[] args) {
    CLI cli =
        CLI.create("IUDX Rs")
            .setSummary("A CLI to deploy the resource")
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
                    .setDescription("configuration file"))
            .addOption(
                new Option()
                    .setLongName("host")
                    .setShortName("i")
                    .setRequired(true)
                    .setDescription("public host"))
            .addOption(
                new TypedOption<String>()
                    .setType(String.class)
                    .setLongName("modules")
                    .setShortName("m")
                    .setRequired(false)
                    .setDefaultValue("all")
                    .setParsedAsList(true)
                    .setDescription(
                        "comma separated list of verticle names to deploy. "
                            + "If omitted, or if `all` is passed, all verticles are deployed"));

    StringBuilder usageString = new StringBuilder();
    cli.usage(usageString);
    CommandLine commandLine = cli.parse(Arrays.asList(args), false);
    if (commandLine.isValid() && !commandLine.isFlagEnabled("help")) {
      String configPath = commandLine.getOptionValue("config");
      String host = commandLine.getOptionValue("host");
      List<String> passedModules = commandLine.getOptionValues("modules");
      List<String> modules = passedModules.stream().distinct().collect(Collectors.toList());

      /* `all` is also passed by default if no -m option given.*/
      if (modules.contains("all")) {
        deploy(configPath, host, List.of());
      } else {
        deploy(configPath, host, modules);
      }
      Runtime.getRuntime().addShutdownHook(new Thread(() -> gracefulShutdown()));
    } else {
      logger.info(usageString);
    }
  }
}
