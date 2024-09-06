# This scripts performs the operation of fetching exchange names from a table in postgres db
# using that exchanges in turn to create exchange in rabbitmq with the same name.
# Then binds exchanges to a queue specfied in the config file

### Prerequisite: Run the command "pip install -r requirements.txt"

import json
import psycopg2
import pika
import urllib.parse
import ssl

# reading configuration file 
with open("subscription-monitoring-config.json") as file:
 config=json.load(file)

postgresDatabaseName = config["dataBaseName"]
postgresUser = config["dataBaseUser"]
postgresPassword = config["dataBasePassword"]
postgresHost = config["dataBaseHost"]
postgresPort = config["dataBasePort"]
vhost = config["vhost"]
dataBrokerUserName = config['dataBrokerUserName']
dataBrokerPassword = config['dataBrokerPassword']
dataBrokerHost = config["dataBrokerHost"]
dataBrokerPort = config["dataBrokerPort"]
dataBrokerQueue = config["dataBrokerQueue"]
encoded_pass=urllib.parse.quote(dataBrokerPassword)

#connecting with postrges database
conn = psycopg2.connect(host=postgresHost, database=postgresDatabaseName, user=postgresUser, password=postgresPassword,port=postgresPort)
cur = conn.cursor()

# get rows from column "exchange_name" from "adaptors_details" table
cur.execute("SELECT exchange_name FROM adaptors_details;")
rows = cur.fetchall()
count = len(rows)
conn.close()

print(f'Total number of exchanges found: {count}')
print(f'Binding {count} exchanges to queue "{dataBrokerQueue}"............\n')



for exchange in rows:
    modified_exchange_list = str(exchange).replace('(', '').replace(')', '').replace(',', '').replace("'", '')
    dataBrokerExchange = modified_exchange_list

    context = ssl.SSLContext(ssl.PROTOCOL_TLSv1_2)
    ssl_options=pika.SSLOptions(context)
    credentials = pika.PlainCredentials(dataBrokerUserName,dataBrokerPassword)
    connection = pika.BlockingConnection(pika.URLParameters(f'amqps://{dataBrokerUserName}:{encoded_pass}@{dataBrokerHost}:{dataBrokerPort}/{vhost}'))
    channel= connection.channel()
    # channel.exchange_declare(dataBrokerExchange, durable=True, exchange_type="topic")
    # channel.queue_declare(dataBrokerQueue)
    channel.queue_bind(exchange=dataBrokerExchange, queue=dataBrokerQueue, routing_key=f"{dataBrokerExchange}/.*")
    print(f'"{dataBrokerExchange}" is bound with queue "{dataBrokerQueue}"\n')
    connection.close()
print("✅ Executed script successfully ")



    
