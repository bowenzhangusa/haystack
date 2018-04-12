CURRENT_DIR = $(shell pwd)
CURRENT_HOST=$(shell hostname --ip-address)
CURRENT_HOSTNAME=$(shell hostname)

# this is used by java app
export HAYSTACK_HOME = $(CURRENT_DIR)

# Hosts we use for project deployment.
# this is unix5.andrew.cmu.edu
HOST_1 ?= 128.2.13.144
# unix4.andrew.cmu.edu
HOST_2 ?= 128.2.13.137
# unix7.andrew.cmu.edu
HOST_3 ?= 128.2.13.138

# a port where java app will accept http requests
APP_PORT ?= 7799

# change this to "mvn" if you have maven installed globally
MAVEN ?= ./maven/bin/mvn

# set this to 1 to enable java app printing logs to stdout
DEBUG ?= 0

# ports to use for redis
# replace these ports if they are already taken;
# then run "make redis-start" on each node, "make redis-cluster-configure" on one node,
# and replace ports in "config.xml" (clearly, it's easier to just keep these ports...)
REDIS_MASTER_PORT ?= 7790
REDIS_SLAVE_PORT ?= 7791
# a command to run redis-cli tool on current host at master instance
REDIS_CLI_MASTER=./redis/src/redis-cli -h $(CURRENT_HOST) -p $(REDIS_MASTER_PORT)
# a command to run redis-cli tool on current host at slave instance
REDIS_CLI_SLAVE=./redis/src/redis-cli -h $(CURRENT_HOST) -p $(REDIS_SLAVE_PORT)
# a command to run redis-cli tool on current host at master instance as a cluster
REDIS_CLI_CLUSTER=./redis/src/redis-cli -c -h $(CURRENT_HOST) -p $(REDIS_MASTER_PORT)

# these variables will be used to fill cassandra.yaml config file for this host
# (see task "cassandra-configure" and files in "setup" directory)
export CASSANDRA_DATA_DIR ?= $(CURRENT_DIR)/cassandra/data-$(CURRENT_HOSTNAME)
export CASSANDRA_CONFIG_DIR ?= $(CURRENT_DIR)/cassandra/conf-$(CURRENT_HOSTNAME)
export CASSANDRA_CONFIG_TEMPLATE ?= $(CURRENT_DIR)/setup/cassandra-template.yaml
export CASSANDRA_CONFIG ?= $(CASSANDRA_CONFIG_DIR)/cassandra.yaml
export CASSANDRA_ENV ?= $(CASSANDRA_CONFIG_DIR)/setup/cassandra-env.sh
# replace these ports if they are already taken;
# then run "make cassandra-start" on each node, and replace ports in "config.xml"
export CASSANDRA_RPC_PORT ?= 7792
export CASSANDRA_STORAGE_PORT ?= 7793
export CASSANDRA_TRANSPORT_PORT ?= 7794
export JMX_PORT = 7795
export CASSANDRA_COMMITLOG_DIR ?= $(CASSANDRA_DATA_DIR)/commitlog
export CASSANDRA_CACHES_DIR ?= $(CASSANDRA_DATA_DIR)/caches
export CASSANDRA_HINTS_DIR ?= $(CASSANDRA_DATA_DIR)/hints
export CASSANDRA_SEEDS ?= $(HOST_1),$(HOST_2)
export CASSANDRA_LISTEN_ADDRESS ?= $(CURRENT_HOST)

# this is needed for cassandra to read the config from the host-specific directory
export CASSANDRA_CONF ?= $(CASSANDRA_CONFIG_DIR)

.PHONY: test install redis-start redis-cli-master redis-cli-slave configure-redis-cluster redis-reset cassandra-start app-start

# runs tests
test:
	$(MAVEN) -Dcheckstyle.skip=true test

# starts redis, cassandra and java app on current host
start: redis-start cassandra-start app-start

# starts java application server
app-start:
	@# helps if app was already running
	-@pkill -f haystack &> /dev/null
ifeq (1, $(DEBUG))
	@# start with logs enabled
	@$(MAVEN) -q -nsu compile exec:exec -Dcheckstyle.skip=true \
	-DmainClass="com.haystack.server.HaystackServer" -Dport="$(APP_PORT)"
else
	@# start as daemon with logs disabled
	@$(MAVEN) -q -nsu compile exec:exec -Dcheckstyle.skip=true \
		-DmainClass="com.haystack.server.HaystackServer" -Dport="$(APP_PORT)" &>/dev/null &
	@echo Haystack app started on port $(APP_PORT)
endif

# starts redis instances on current host (must be repeated on all hosts)
redis-start:
	@# this helps if redis was already running
	-@pkill redis &> /dev/null
	@# a hack to wait for redis instances to be killed
	@sleep 1
	@echo starting master on current host
	@./redis/src/redis-server --protected-mode no --port $(REDIS_MASTER_PORT) \
	--cluster-enabled yes \
	--dbfilename dump-master-$(CURRENT_HOSTNAME).rdb \
	--cluster-config-file nodes-master-$(CURRENT_HOSTNAME).conf \
	--cluster-node-timeout 15000 >/dev/null &
	@echo Redis master started at $(CURRENT_HOST):$(REDIS_MASTER_PORT)
	@echo starting slave on current host
	@./redis/src/redis-server --protected-mode no --port $(REDIS_SLAVE_PORT) \
	--cluster-enabled yes \
	--dbfilename dump-slave-$(CURRENT_HOSTNAME).rdb \
	--cluster-config-file nodes-slave-$(CURRENT_HOSTNAME).conf \
	--cluster-node-timeout 15000 >/dev/null &
	@echo Redis slave started at $(CURRENT_HOST):$(REDIS_SLAVE_PORT)

# enters redis-cli on current host in cluster mode
redis-cli-cluster:
	$(REDIS_CLI_CLUSTER)

# enters redis-cli on current host in master mode
redis-cli-master:
	$(REDIS_CLI_MASTER)

# enters redis-cli on current host in slave mode
redis-cli-slave:
	$(REDIS_CLI_SLAVE)

# resets redis flushing all data and clearing cluster state. used for cluster reconfiguration
redis-reset:
	@# this is needed for redis to be able to reconfigure a cluster
	@# (all the information about previous cluster configuration must be wiped)
	$(REDIS_CLI_MASTER) FLUSHALL
	$(REDIS_CLI_MASTER) cluster reset hard
	$(REDIS_CLI_SLAVE) FLUSHALL
	$(REDIS_CLI_SLAVE) cluster reset hard
	@rm nodes-slave-$(CURRENT_HOSTNAME).conf &> /dev/null; true
	@rm nodes-master-$(CURRENT_HOSTNAME).conf &> /dev/null; true

# shows nodes in redis cluster
redis-cluster-show-nodes:
	$(REDIS_CLI_CLUSTER) cluster nodes

# (re)configures redis cluster
redis-cluster-configure: redis-reset
	./redis/src/redis-trib.rb create \
	$(HOST_1):$(REDIS_MASTER_PORT) \
	$(HOST_2):$(REDIS_MASTER_PORT) \
	$(HOST_3):$(REDIS_MASTER_PORT)

	@# host 2 becames replicated on host 1 (meaning host 1 is a slave of host 2)
	./redis/src/redis-trib.rb add-node --slave $(HOST_1):$(REDIS_SLAVE_PORT) $(HOST_2):$(REDIS_MASTER_PORT)
	@# host 3 becames replicated on host 2
	./redis/src/redis-trib.rb add-node --slave $(HOST_2):$(REDIS_SLAVE_PORT) $(HOST_3):$(REDIS_MASTER_PORT)
	@# host 1 becames replicated on host 3
	./redis/src/redis-trib.rb add-node --slave $(HOST_3):$(REDIS_SLAVE_PORT) $(HOST_1):$(REDIS_MASTER_PORT)

# configures cassandra instance on current host, initializing config files
cassandra-configure:
	@echo creating host-specific config and data directories
	@mkdir -p $(CASSANDRA_DATA_DIR) \
		$(CASSANDRA_COMMITLOG_DIR) \
		$(CASSANDRA_CACHES_DIR) \
		$(CASSANDRA_HINTS_DIR)
	@echo copying default configs to a host-specific directory
	@cp -r -n $(CURRENT_DIR)/cassandra/conf $(CASSANDRA_CONFIG_DIR)
	@cp $(CURRENT_DIR)/setup/cassandra-env.sh $(CASSANDRA_CONFIG_DIR)/cassandra-env.sh
	@echo filling config file
	@envsubst < $(CASSANDRA_CONFIG_TEMPLATE) > $(CASSANDRA_CONFIG)
	@echo done

# starts cassandra instance on current host
cassandra-start: cassandra-configure
	@echo starting cassandra...
	@# this helps if cassandra was already running
	-@pkill -f cassandra &> /dev/null
	@# a hack to wait for cassandra to be killed
	@sleep 1
	@./cassandra/bin/cassandra -f &>/dev/null &
	@echo cassandra started

