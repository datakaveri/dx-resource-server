<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Setup and Installation Guide
This document contains the installation and configuration information required to deploy the Data Exchange (DX) Resource Server.

## Configuration
In order to connect the DX Resource Server with PostgreSQL, RabbitMQ, Redis, DX Catalogue Server, DX AAA Server, etc please refer [Configurations](./Configurations.md). It contains appropriate information which shall be updated as per the deployment.

## Dependencies
In this section we explain about the dependencies and their scope. It is expected that the dependencies are met before starting the deployment of DX Resource Server.

### External Dependencies
| Software Name  | Purpose                                                                                                                                | 
|:---------------|:---------------------------------------------------------------------------------------------------------------------------------------|
| PostgreSQL     | For storing information related subscription, ingestion, unique-attributes and async, and accessing audit table to give count for apis |
| RabbitMQ       | To publish auditing related data to auditing server via RabbitMQ exchange and also to create queues and exchanges.                     |
| Redis          | Used as a cache for serving latest data.                                                                                               |
| AWS S3         | To store files for async search.                                                                                                       |
| Elastic Search | Used for serve data for the different types of queries.                                                                                |


### Internal Dependencies
| Software Name                                               | Purpose                                                                  | 
|:------------------------------------------------------------|:-------------------------------------------------------------------------|
| DX Authentication Authorization and Accounting (AAA) Server | Used to download certificate for JWT token decoding and to get user info |
| DX Catalogue Server                                         | Used to fetch the list of resource and provider related information      |
| DX Auditing Server                                          | Used to fetch audit data for different uses.                             |

## Prerequisites

#### AWS S3 Account
- Make sure that AWS S3 account is configured properly and bucket [name](./Configurations.md) should be existed to save file in AWS S3 bucket.
- Download the credentials `AWS_ACCESS_KEY_ID` , `AWS_SECRET_ACCESS_KEY` and add them to the environment variables

#### RabbitMQ
- To setup RabbitMQ refer the setup and installation instructions available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker)
- After deployment of RabbitMQ, we need to ensure that there are certain prerequisites met. Incase if it is not met, please login to RabbitMQ management portal using the RabbitMQ management UI and create a the following

##### Create vHost

| Type  | Name          | Details                    |   
|-------|---------------|----------------------------|
| vHost | IUDX-INTERNAL | Create a vHost in RabbitMQ |
| vHost | IUDX          | Create a vHost in RabbitMQ |


##### Create Exchange

| Exchange Name | Type of exchange | features | Details                                                                              |   
|---------------|------------------|----------|--------------------------------------------------------------------------------------|
| auditing      | direct           | durable  | Create an exchange in vHost IUDX-INTERNAL to allow audit information to be published |  
| async-query   | direct           | durable  | Create an exchange in vHost IUDX-INTERNAL to handle async search request             |

##### Create Queue and Bind to Exchange
| Exchange Name                 | Queue Name               | vHost         | routing key |  
|-------------------------------|--------------------------|---------------|-------------|
| auditing                      | auditing-messages        | IUDX-INTERNAL | #           |
| <rg-id>                       | database                 | IUDX          | <rg-id>/.*  |
| <rg-id>                       | redis-latest             | IUDX          | <rg-id>/.*  |
| <rg-id>                       | subscriptions-monitoring | IUDX          | <rg-id>/.*  |
| latest-data-unique-attributes | rs-unique-attributes     | IUDX-INTERNAL | #           |
| invalid-sub                   | rs-invalid-sub           | IUDX-INTERNAL | #           |
| async-query                   | rs-async-query           | IUDX-INTERNAL | #           |

**Note** 
- 'latest-data-unique-attributes' and 'invalid-sub' should be available to bind 'rs-unique-attributes' and 'rs-invalid-sub'.
- The 'rs-unique-attributes' queue is used to retrieve the latest data with unique attributes.
- The 'rs-invalid-sub' queue provides details about revoked tokens.

##### User and Permissions
Create a DX RS user using the RabbitMQ management UI and set write permission. This user will be used by DX Resource server to publish audit data

| API                         | Body           | Details                                                |   
|-----------------------------|----------------|--------------------------------------------------------|
| /api/users/user/permissions | As shown below | Set permission for a user to publish audit information | 


Body for the API request

```
 "permissions": [
        {
          "vhost": "IUDX-INTERNAL",
          "permission": {
            "configure": "^$",
            "write": "^auditing$",
            "read": "^$"
          }
        }
]
```
#### ElasticSearch
- To setup ElasticSearch refer setup and installation instructions available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/elk)

#### Postgres SQL
- To setup PostgreSQL refer setup and installation instructions available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres)
- **Note** 
  - PostgreSQL database should be configured with a RBAC user having CRUD privileges
  - Any Schema changes will be available [here](https://github.com/datakaveri/iudx-resource-server/blob/master/src/main/resources/db/migration/) and use flyway migration to update/create the postgres table.

| Table Name           | Purpose                                                      | 
|----------------------|--------------------------------------------------------------|
| revoked_tokens       | To verify token status either revoked or not                 |
| unique_attributes    | To fetch unique attributes                                   | 
| subscriptions        | To store the subscription related information for a resource | 
| s3_upload_url        | To store async request related information                   |
| adaptors_details     | To store adaptor(exchange) related information               |
| subscription_users   | To store user credential of RMQ                              |
| auditing_rs          | To fetch auditing related information                        |

#### Auditing
- Auditing is done using the DX Auditing Server which uses Immudb and Postgres for storing the audit logs
- To setup immuclient for immudb please refer [immudb setup guide](https://github.com/datakaveri/iudx-deployment/tree/master/docs/immudb-setup)
- The schema for auditing table in PostgreSQL is present here - [postgres auditing table schema](https://github.com/datakaveri/iudx-resource-server/blob/master/src/main/resources/db/migration/V4_0__Add-tables-in-postgres-for-metering.sql)
- The schema for Immudb table, index for the table is present here - [immudb schema in DX Auditing Server](https://github.com/datakaveri/auditing-server/tree/main/src/main/resources/immudb/migration)

| Table Name  | Purpose                      | DB                 | 
|-------------|------------------------------|--------------------|
| auditing_rs | To store audited information | Immudb, PostgreSQL |


### Database Migration using Flyway
- Database flyway migrations help in updating the schema, permissions, grants, triggers etc., with the latest version
- Each flyway schema file is versioned with the format `V<majorVersion>_<minorVersion>__<description>.sql`, ex : `V1_1__init-tables.sql`
- Schemas for PostgreSQL tables are present here - [Flyway schema](https://github.com/datakaveri/iudx-resource-server/tree/master/src/main/resources/db/migration)
- Values like DB URL, database user credentials, user and schema name should be populated in flyway.conf
- The following commands shall be executed
    - ``` mvn flyway:info -Dflyway.configFiles=flyway.conf``` To get the flyway schema history table
    - ``` mvn clean flyway:migrate -Dflyway.configFiles=flyway.conf ``` To migrate flyway schema
    - ``` mvn flyway:repair ``` To resolve some migration errors during flyway migration
- Please find the reference to Flyway migration [here](https://documentation.red-gate.com/fd/migrations-184127470.html)

## Installation Steps
### Maven
1. Install java 11 and maven
2. Use the maven exec plugin based starter to start the server. Goto the root folder where the pom.xml file is present and run the below command.
   `mvn clean compile exec:java@resource-server`

### JAR
1. Install java 11 and maven
2. Set Environment variables
```
export RS_URL=https://<rs-domain-name>
export LOG_LEVEL=INFO
```
3. Use maven to package the application as a JAR. Goto the root folder where the pom.xml file is present and run the below command.
   `mvn clean package -Dmaven.test.skip=true`
4. 2 JAR files would be generated in the `target/` directory
    - `iudx.resource.server-cluster-0.0.1-SNAPSHOT-fat.jar` - clustered vert.x containing micrometer metrics
    - `iudx.resource.server-dev-0.0.1-SNAPSHOT-fat.jar` - non-clustered vert.x and does not contain micrometer metrics

#### Running the clustered JAR
**Note**: The clustered JAR requires Zookeeper to be installed. Refer [here](https://zookeeper.apache.org/doc/r3.3.3/zookeeperStarted.html) to learn more about how to set up Zookeeper. Additionally, the `zookeepers` key in the config being used needs to be updated with the IP address/domain of the system running Zookeeper.
The JAR requires 3 runtime arguments when running:

* --config/-c : path to the config file
* --host/-i : the hostname for clustering
* --modules/-m : comma separated list of module names to deploy

e.g. `java -jar target/iudx.resource.server-cluster-0.0.1-SNAPSHOT-fat.jar  --host $(hostname) -c configs/config.json -m iudx.resource.server.database.archives.DatabaseVerticle,iudx.resource.server.authenticator.AuthenticationVerticle
,iudx.resource.server.metering.MeteringVerticle,iudx.resource.server.database.postgres.PostgresVerticle`

Use the `--help/-h` argument for more information. You may additionally append an `RS_JAVA_OPTS` environment
variable containing any Java options to pass to the application.

e.g.
```
$ export RS_JAVA_OPTS="-Xmx4096m"
$ java $RS_JAVA_OPTS -jar target/iudx.resource.server-cluster-0.0.1-SNAPSHOT-fat.jar ...

```

#### Running the non-clustered JAR
The JAR requires 1 runtime argument when running

* --config/-c : path to the config file

e.g. `java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4j2LogDelegateFactory -jar target/iudx.resource.server-dev-0.0.1-SNAPSHOT-fat.jar -c configs/config.json`

Use the `--help/-h` argument for more information. You may additionally append an `RS_JAVA_OPTS` environment variable containing any Java options to pass to the application.

e.g.
```
$ export RS_JAVA_OPTS="-Xmx1024m"
$ java $RS_JAVA_OPTS -jar target/iudx.resource.server-dev-0.0.1-SNAPSHOT-fat.jar ...
```

### Docker
1. Install docker and docker-compose
2. Clone this repo
3. Build the images
   ` ./docker/build.sh`
4. Modify the `docker-compose.yml` file to map the config file
5. Start the server in production (prod) or development (dev) mode using docker-compose
   ` docker-compose up prod `

## Logging and Monitoring
### Log4j 2
- For asynchronous logging, logging messages to the console in a specific format, Apache's log4j 2 is used
- For log formatting, adding appenders, adding custom logs, setting log levels, log4j2.xml could be updated : [link](https://github.com/datakaveri/iudx-resource-server/blob/master/src/main/resources/log4j2.xml)
- Please find the reference to log4j 2 : [here](https://logging.apache.org/log4j/2.x/manual/index.html)

### Micrometer
- Micrometer is used for observability of the application
- Reference link: [vertx-micrometer-metrics](https://vertx.io/docs/vertx-micrometer-metrics/java/)
- The metrics from micrometer is stored in Prometheus which can be used to alert, observe,
  take steps towards the current state of the application
- The data sent to Prometheus can then be visualised in Grafana
- Reference link: [vertx-prometheus-grafana](https://how-to.vertx.io/metrics-prometheus-grafana-howto/)
- DX Deployment repository references for [Prometheus](https://github.com/datakaveri/iudx-deployment/tree/master/K8s-deployment/K8s-cluster/addons/mon-stack/prometheus), [Loki](https://github.com/datakaveri/iudx-deployment/tree/master/K8s-deployment/K8s-cluster/addons/mon-stack/loki), [Grafana](https://github.com/datakaveri/iudx-deployment/tree/master/K8s-deployment/K8s-cluster/addons/mon-stack/grafana)

## Testing
### Unit Testing
1. Run the server through either docker, maven or redeployer
2. Run the unit tests and generate a surefire report
   `mvn clean test-compile surefire:test surefire-report:report`
3. Jacoco reports are stored in `./target/`

### Integration Testing

Integration tests are through Postman/Newman whose script can be found from [here](https://github.com/datakaveri/iudx-resource-server/tree/master/src/test/resources).
1. Install prerequisites
- [postman](https://www.postman.com/) + [newman](https://www.npmjs.com/package/newman)
- [newman reporter-htmlextra](https://www.npmjs.com/package/newman-reporter-htmlextra)
2. Example Postman environment can be found [here](https://github.com/datakaveri/iudx-resource-server/blob/master/src/test/resources/)
- Please find the README to setup postman environment file [here](https://github.com/datakaveri/iudx-resource-server/blob/main/src/test/resources/README.md)
3. Run the server through either docker, maven or redeployer
4. Run the integration tests and generate the newman report
   `newman run <postman-collection-path> -e <postman-environment> --insecure -r htmlextra --reporter-htmlextra-export .`
5. Command to store report in `target/newman`:  `newman run <postman-collection-path> -e <postman-environment> --insecure -r htmlextra --reporter-htmlextra-export ./target/newman/report.html`

### Performance Testing
- JMeter is for used performance testing, load testing of the application
- Please find the reference to JMeter : [here](https://jmeter.apache.org/usermanual/get-started.html)
- Command to generate HTML report at `target/jmeter`
```
rm -r -f target/jmeter && jmeter -n -t jmeter/<file-name>.jmx -l target/jmeter/sample-reports.csv -e -o target/jmeter/
```

### Security Testing
- For security testing, Zed Attack Proxy(ZAP) Scanning is done to discover security risks, vulnerabilities to help us address them
- A report is generated to show vulnerabilities as high risk, medium risk, low risk and false positive
- Please find the reference to ZAP : [here](https://www.zaproxy.org/getting-started/)