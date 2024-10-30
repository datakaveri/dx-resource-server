<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Modules

This document contains the information of the configurations to setup various services and dependencies in order to
bring up the DX Resource Server.
Please find the example configuration
file [here](https://github.com/datakaveri/iudx-resource-server/blob/master/example-configs/configs/config-dev.json).
While running the server, make a copy of sample configs directory and add appropriate values to all files.

```console
 cp -r example-configs/* .
```

```
# configs directory after generation of configs files
configs/
├── config-dev.json
└── config-test.json
├── keystore.jks
└── keystore.p12
```

## Other Configuration

| Key Name   | Value Datatype | Value Example   | Description                                                 |
|:-----------|:--------------:|:----------------|:------------------------------------------------------------|
| version    |     Float      | 1.0             | config version                                              |
| zookeepers |     Array      | zookeeper       | zookeeper configuration to deploy clustered vert.x instance |
| clusterId  |     String     | iudx-rs-cluster | cluster id to deploy clustered vert.x instance              |

## CommonConfig

| Key Name            | Datatype | Example                         | Description                                                                                                                             |
|:--------------------|:--------:|:--------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------|
| dxApiBasePath       |  String  | /auth/v1                        | API base path for DX AAA server. Reference : [link](https://swagger.io/docs/specification/2-0/api-host-and-base-path/)                  |
| dxAuthBasePath      |  String  | /ngsi-ld/v1                     | API base path for DX rs-proxy-sever. Reference : [link](https://swagger.io/docs/specification/2-0/api-host-and-base-path/)              |
| dxCatalogueBasePath |  String  | /iudx/cat/v1                    | API base path for DX Catalogue server. Reference : [link](https://swagger.io/docs/specification/2-0/api-host-and-base-path/)            |
| catServerHost       |  String  | api.cat-test.iudx.io            | Host name of DX Catalogue server for fetching the information of resources, resource groups                                             |
| catServerPort       | integer  | 443                             | Port number to access HTTPS APIs of Catalogue Server                                                                                    |
| timeLimit           |  String  | "test,2020-10-22T00:00:00Z,365" | Contains three comma-separated parts: deployment type (e.g., test or production), a date-time stamp, and the maximum allowed query days |
| timeLimitForAsync   | integer  | 365                             | No of time limit (in days) for asynchronous search queries.                                                                             |

## Api Server Verticle

| Key Name          | Value Datatype | Value Example | Description                                                              |
|:------------------|:--------------:|:--------------|:-------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations |
| verticleInstances |    integer     | 8             | Number of instances required for verticles                               |
| httpPort          |    integer     | 8443          | Port for running the instance DX Resource Server                         |
| ssl               |    boolean     | true          | Enable or Disable secure sockets                                         |

## Database Verticle

| Key Name          | Value Datatype | Value Example | Description                                                              |
|:------------------|:--------------:|:--------------|:-------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations |
| verticleInstances |    integer     | 1             | Number of instances required for verticles                               |
| tenantPrefix      |     String     | iudx          | To identify indexes                                                      |
| databaseIP        |     String     | localhost     | Ip Address                                                               |
| databasePort      |    integer     | 24034         | Port Number                                                              |
| dbUser            |     String     | xyz-user      | Elastic User                                                             |
| dbPassword        |     String     | xyz-password  | Elastic User Password                                                    |                                                                          |

## DataBroker Verticle

| Key Name                 | Value Datatype | Value Example    | Description                                                                                            |
|:-------------------------|:--------------:|:-----------------|:-------------------------------------------------------------------------------------------------------|
| isWorkerVerticle         |    boolean     | false            | To check if worker verticle needs to be deployed for blocking operations                               |
| verticleInstances        |    integer     | 1                | Number of instances required for verticles                                                             |
| dataBrokerIP             |     String     | localhost        | IP address of the data broker                                                                          |
| dataBrokerPort           |    integer     | 2587             | Port Number of the data broker                                                                         |
| prodVhost                |     String     | vHost            | RMQ vhost                                                                                              |
| internalVhost            |     String     | vHost-INTERNAL   | RMQ vhost internal                                                                                     |
| externalVhost            |     String     | vHost-EXTERNAL   | RMQ vhost external                                                                                     |
| dataBrokerUserName       |     String     | rmqUserName      | User name for RMQ                                                                                      |
| dataBrokerPassword       |     String     | rmqUserPassword  | Password for RMQ                                                                                       |
| dataBrokerManagementPort |    integer     | 28051            | Port on which RMQ Management plugin is running                                                         |
| connectionTimeout        |    integer     | 6000             | Setting connection timeout as part of RabbitMQ config options to set up webclient                      |
| requestedHeartbeat       |    integer     | 60               | Defines after what period of time the peer TCP connection should be considered unreachable by RabbitMQ |
| handshakeTimeout         |    integer     | 6000             | To increase or decrease the default connection time out                                                |
| requestedChannelMax      |    integer     | 5                | Tells no more that 5 (or given number) could be opened up on a connection at the same time             |
| networkRecoveryInterval  |    integer     | 500              | Interval to restart the connection between rabbitmq node and clients                                   |
| automaticRecoveryEnabled |    boolean     | true             | Automatic Recovery for connection failure                                                              |
| postgresDatabaseIP       |     String     | localhost        | IP address of Postgres                                                                                 |
| postgresDatabasePort     |    integer     | 5432             | Port Number of Postgres                                                                                |
| postgresDatabaseName     |     String     | postgres         | Schema Name                                                                                            |
| postgresDatabaseUserName |     String     | postgresUser     | User Name of Postgres                                                                                  |
| postgresDatabasePassword |     String     | postgresPassword | Password of Postgres                                                                                   |                                 
| postgrespoolSize         |    integer     | 25               | Number of connections                                                                                  |
| brokerAmqpIp             |     String     | localhost        | AMQ IP of data broker                                                                                  |                                                                                                        |
| brokerAmqpPort           |    integer     | 23456            | AMQ Port of data broker                                                                                |                                                                                                        |

## Authentication Verticle

| Key Name          | Value Datatype | Value Example | Description                                                                 |
|:------------------|:--------------:|:--------------|:----------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for AuthenticationVerticle |
| verticleInstances |    integer     | 1             | Number of instances required for AuthenticationVerticle                     |
| audience          |     String     | rs.iudx.io    | Audience                                                                    |
| authServerHost    |     String     | abc.iudx.io   | Hostname of the authentication server                                       |
| jwtIgnoreExpiry   |    boolean     | false         | To ignore JWT Expiry                                                        |
| enableLimits      |    boolean     | false         | To enable Limits (data limits)                                              |

## Metering Verticle

| Key Name          | Value Datatype | Value Example | Description                                                              |
|:------------------|:--------------:|:--------------|:-------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations |
| verticleInstances |    integer     | 1             | Number of instances required for verticles                               |

## Latest Verticle

| Key Name                | Value Datatype | Value Example | Description                                                              |
|:------------------------|:---------------|:--------------|:-------------------------------------------------------------------------|
| isWorkerVerticle        | boolean        | false         | To check if worker verticle needs to be deployed for blocking operations |
| tenantPrefix            | String         | iudx          | Tenant prefix for LatestVerticle                                         |
| verticleInstances       | integer        | 2             | Number of instances required for verticles                               |
| redisMode               | String         | STANDALONE    | Mode in which Redis needs to run                                         |
| redisUsername           | String         | redisUserName | Username for Redis connection                                            |
| redisPassword           | String         | redisPassword | Password for Redis connection                                            |
| redisMaxPoolSize        | integer        | 30            | Maximum number of connections in pool                                    |
| redisMaxPoolWaiting     | integer        | 200           | Maximum number of waiting connections                                    |
| redisMaxWaitingHandlers | integer        | 1024          | Maximum number of waiting handlers                                       |
| redisPoolRecycleTimeout | integer        | 1500          | Time in milliseconds after which connection is recycled                  |
| redisHost               | String         | localhost     | Hostname or IP of Redis instance                                         |
| redisPort               | integer        | 1234          | Port number on which Redis is running                                    |

## Postgres Verticle

| Key Name          | Value Datatype | Value Example        | Description                                    |
|:------------------|:---------------|:---------------------|:-----------------------------------------------|
| isWorkerVerticle  | boolean        | false                | Indicates if the verticle is a worker verticle |
| verticleInstances | Integer        | 1                    | Number of instances required for verticles     |
| databaseIp        | String         | localhost            | Postgres database IP address                   |
| databasePort      | integer        | 5432                 | Postgres database port number                  |
| databaseName      | String         | abc_iudx             | Postgres database name (Schema)                |
| databaseUserName  | String         | postgresUserName     | Postgres database username                     |
| databasePassword  | String         | postgresUserPassword | Postgres database password                     |
| poolSize          | integer        | 25                   | Connection pool size for Postgres database     |

## Cache Verticle

| Key Name          | Value Datatype | Value Example | Description                                    |
|:------------------|:---------------|:--------------|:-----------------------------------------------|
| isWorkerVerticle  | boolean        | false         | Indicates if the verticle is a worker verticle |
| verticleInstances | integer        | 1             | Number of instances for this verticle          |

## Async Verticle

| Key Name          | Value Datatype | Value Example        | Description                                    |
|:------------------|:---------------|:---------------------|:-----------------------------------------------|
| isWorkerVerticle  | boolean        | false                | Indicates if the verticle is a worker verticle |
| tenantPrefix      | String         | iudx                 | Tenant prefix for ES index                     |
| threadPoolName    | String         | async-query-pool     | Name of the thread pool                        |
| threadPoolSize    | integer        | 20                   | Size of the thread pool                        |
| verticleInstances | integer        | 20                   | Number of instances for this verticle          |
| databaseIP        | String         | localhost            | IP address of ES database                      |
| databasePort      | integer        | 998                  | Port number of ES database                     |
| dbUser            | String         | xyz-user             | ES username                                    |
| dbPassword        | String         | xyz-password         | ES password                                    |
| filePath          | String         | /home/xyz/Downloads/ | File path for async queries                    |
| bucketName        | String         | abc-xyz              | S3 bucket name for async queries               |

## Encryption Verticle

| Key Name          | Value Datatype | Value Example | Description                                    |
|:------------------|:---------------|:--------------|:-----------------------------------------------|
| isWorkerVerticle  | boolean        | false         | Indicates if the verticle is a worker verticle |
| verticleInstances | integer        | 1             | Number of instances for this verticle          |
