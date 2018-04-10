## Infrastructure preparation
All of the below is already done on andrew machines at `/afs/andrew.cmu.edu/usr2/asergeye/haystack/project`.
Only run the following if you're installing it someplace else.

The infrastructure setup is only semi-automated and does not handle errors and edge cases well.

### Redis
Redis cluster setup is based on this [tutorial](https://linode.com/docs/applications/big-data/how-to-install-and-configure-a-redis-cluster-on-ubuntu-1604/).

Execute the following in the project directory:
```
wget http://download.redis.io/redis-stable.tar.gz
tar xvzf redis-stable.tar.gz
mv redis-stable redis
cd redis
make all
```

Install redis gem for ruby so we can setup redis-cluster with `redis-trib`:
`gem install redis`

On andrew machines, ruby version is too old, so [RVM](https://rvm.io/) can be installed to run the newest ruby:
```
\curl -sSL https://get.rvm.io | bash
rvm install 2.5
gem install redis
```

Repeat the above steps on all machines (we use 3 andrew machines: unix4, unix5, and unix7).
If the you use different machines, edit the Makefile with corresponding hostnames.

Then on each machine, run ```make redis-start``` (see Makefile for detailed info).
Then on one of those machines, run `make redis-cluster-configure`.
Then run `make redis-cluster-show-nodes` on the same machine to make sure there are 3 connected master nodes and 2 slaves. If the output is not as expected, check if ports are busy or some other error happened.

If you need to reconfigure the servers, make sure to first execute `make redis-reset` on each node.

After setting the cluster up, modify `./config.xml` to point to any of the redis servers.

### Cassandra
TODO

### NGINX
TODO
 
## Project installation instructions
Java 8, maven, redis and cassandra need to be installed beforehand.
Specify redis and cassandra connection options in `src/main/java/com/haystack/Config.java`.
After installing cassandra, make sure to run `nodetool enablethrift` to make its API accessible from java.

Then run
`run-server.sh $port_number`

## Tests:
`make test`

Tests do not work on andrew machines (yet) due to incompatible maven version.

## Daily reboots on andrew machines

To make sure all the software runs after server reboots, ssh to every machine (in our case, unix5.andrew.cmu.edu, unix4.andrew.cmu.edu, unix7.andrew.cmu.edu) and run the following:

`make redis-start`

TODO: make the same for cassandra and nginx
