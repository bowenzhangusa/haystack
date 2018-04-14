## About
This project is implemented with Java, Cassandra, Redis and NGINX.
See architecture diagram (architecture.png) for overall project structure.

The project is focused on providing fault-tolerant, robust service, by running multiple instances of every component.
It does not specifically optimize for minimal storage overhead per file, but relies on Cassandra to store files efficiently. 

## Infrastructure setup
All of the below is already done on andrew machines at `/afs/andrew.cmu.edu/usr2/asergeye/haystack/project`.
Only follow the instructions below if you're installing it someplace else.

The infrastructure setup is only semi-automated and does not handle errors and edge cases well.

The application uses ports in range of 7790-7799. If they are busy or need to be changed, refer to the comments in Makefile.
We used 3 andrew machines for deployment: unix5, unix7, and unix8.
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
Download and unpack cassandra into the project directory (on one node, if working with andrew machines) using Makefile command:
```
make cassandra-install
```

Then run this on each node simultaneously (within ~1 minute, so that nodes could start communicating with each other ASAP - otherwise cassandra will complain and die):
```
make cassandra-start
```

After cassandra has been started on each node, run the following on one node to create keyspace and table:
```
make cassandra-initialize
```
For some reason this command does not return to the shell, so upon seeing the "Cassandra keyspace and table initialized" message, press Ctrl+C.


### NGINX
We run NGINX only on 1 machine.
To install it, run
```
make nginx-install
```
This will download and compile it.
Then run
```
make nginx-start
```
This will configure and run nginx on a default port (in our case, 7798).

In our configuration, nginx uses all 3 hosts for high-availability and load-balancing (see setup/nginx-template.conf and Makefile for details).

Optionally, to monitor the nginx logs use
```
tailf nginx/error.log
tailf nginx/access.log
``` 

### Maven

To build and run the java app on andrew machines, a newer version of maven needs to be installed in project directory:
```
wget http://apache.claz.org/maven/maven-3/3.5.3/binaries/apache-maven-3.5.3-bin.tar.gz
tar zxvf apache-maven-3.5.3-bin.tar.gz
mv apache-maven-3.5.3 maven
```

If you already have newer maven installed globally, change the path to its binary in Makefile.
 
### Java application setup
Specify redis and cassandra connection options "config.xml".

Then run
```
make app-start
```
This will start the application at port 7799 by default.
If application logs are needed in console for debugging, run
```
DEBUG=1 make app-start
```

## Starting the application
After infrastructure components has been setup, to run the whole system we need to execute the following on each node:
```
make start
```
This will run cassandra, redis and java app on current host.

The on one of the hosts, additionally run
```
make nginx-start
```
This will run nginx as a load-balancer on a current server.
Further requests to the application should be issued here to be served by nginx.

These commands need to be repeated daily because of regular reboots of andrew machines.

## Tests:
```
make test
```

## Project structure
* `src` - java app source
* `setup` - directory with custom configuration files, used in Makefile for customizing the setup of cassandra
* `cassandra`, `redis` - these are expected to contain corresponding software. It is not bundled with the project (see the previous readme sections on how these directories are created)

## API usage

Requests should be issued to a host where nginx is running, on a default port 7798.
To upload a photo, send `POST` request in `multipart/form-data` format with a `file` field to the same host and port:

```
curl -v --request POST \
  --url http://unix5.andrew.cmu.edu:7798 \
  --header 'content-type: multipart/form-data' \
  --form 'file=@PATH-TO-FILE'
```
This will respond with JSON containing photo id.
Saved photo can be viewed in browser by url: `http://unix5.andrew.cmu.edu:7798/PHOTO-ID`