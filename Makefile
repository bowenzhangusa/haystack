export HAYSTACK_HOME = $(shell pwd)

CURRENT_HOST=$(shell hostname --ip-address)
CURRENT_HOSTNAME=$(shell hostname)
CURRENT_USER=$(shell whoami)

# Hosts we use for project deployment.
# this is unix5.andrew.cmu.edu
HOST_1 ?= 128.2.13.144
# unix4.andrew.cmu.edu
HOST_2 ?= 128.2.13.137
# unix7.andrew.cmu.edu
HOST_3 ?= 128.2.13.138

# ports to use for redis
REDIS_MASTER_PORT ?= 7790
REDIS_SLAVE_PORT ?= 7791
# a command to run redis-cli tool on current host at master instance
REDIS_CLI_MASTER=./redis/src/redis-cli -h $(CURRENT_HOST) -p $(REDIS_MASTER_PORT)
# a command to run redis-cli tool on current host at slave instance
REDIS_CLI_SLAVE=./redis/src/redis-cli -h $(CURRENT_HOST) -p $(REDIS_SLAVE_PORT)
# a command to run redis-cli tool on current host at master instance as a cluster
REDIS_CLI_CLUSTER=./redis/src/redis-cli -c -h $(CURRENT_HOST) -p $(REDIS_MASTER_PORT)

.PHONY: test install redis-start redis-cli-master redis-cli-slave configure-redis-cluster redis-reset

# runs tests
test:
	mvn -Dcheckstyle.skip=true test

# starts redis instances on current host (must be repeated on all hosts)
redis-start:
	@# this helps if redis was already running
	@pkill redis &> /dev/null
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
