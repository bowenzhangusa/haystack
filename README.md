## Infrastructure preparation
All of the below is already done on andrew machines at `/afs/andrew.cmu.edu/usr2/asergeye/haystack/project`.
Only follow the instructions below if you're installing it someplace else.

The infrastructure setup is only semi-automated and does not handle errors and edge cases well.

The application uses ports in range of 7790-7799. If they are busy or need to be changed, refer to the comments in Makefile.
We used 3 andrew machines for deployment: unix4, unix5, and unix7.
If you use different machines, edit the Makefile with corresponding hostnames.

### Redis
Redis cluster setup is based on this [tutorial](https://linode.com/docs/applications/big-data/how-to-install-and-configure-a-redis-cluster-on-ubuntu-1604/).

Execute the following in the project directory (on one node, if working with andrew machines):
```
wget http://download.redis.io/redis-stable.tar.gz
tar xvzf redis-stable.tar.gz
mv redis-stable redis
cd redis
make all
```

Install redis gem for ruby so we can setup redis-cluster with `redis-trib`:
```
gem install redis
```

On andrew machines, ruby version is too old, so [RVM](https://rvm.io/) can be installed to run the newest ruby:
```
curl -sSL https://get.rvm.io | bash
rvm install 2.5
gem install redis
```

Then on each machine, run 
```
make redis-start
``` 
(see Makefile for detailed info).
Then on one of those machines, run 
```
make redis-cluster-configure
```
To make sure it works, you can run `make redis-cluster-show-nodes` on the same machine, expecting to see 3 connected master nodes and 2 slaves. If the output is not as expected, check if ports are busy or some other error happened.

If you need to reconfigure the cluster later, make sure to first execute `make redis-reset` on each node, and then run `make redis-cluster-configure` on one of them.
After setting the cluster up, modify `./config.xml` to point to all of the redis servers.

### Cassandra
Download and unpack cassandra into the project directory (on one node, if working with andrew machines):
```
wget http://apache.claz.org/cassandra/3.11.2/apache-cassandra-3.11.2-bin.tar.gz
tar zxvf apache-cassandra-3.11.2-bin.tar.gz
mv apache-cassandra-3.11.2 cassandra
```

Run this on each node simultaneously (so that nodes could start communicating with each other ASAP, otherwise cassandra will complain and die):
```
make cassandra-start
```


### NGINX
TODO
 
## Project installation instructions
Java 8, maven, redis and cassandra need to be installed beforehand.
Specify redis and cassandra connection options "config.xml".

Then run
`run-server.sh $port_number`

## Tests:
`make test`

Tests do not work on andrew machines (yet) due to incompatible maven version.

## Daily reboots on andrew machines

To make sure all the software runs after server reboots, ssh to every machine (in our case, unix5.andrew.cmu.edu, unix4.andrew.cmu.edu, unix7.andrew.cmu.edu) and run the following:

`make start`

## Project structure
* `src` - java app source
* `setup` - directory with custom configuration files, used in Makefile for customizing the setup of cassandra
* `cassandra`, `redis` - these are expected to contain corresponding software. It is not bundled with the project (see the previous readme sections on how these directories are created)