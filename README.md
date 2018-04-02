## Installation instructions
Java 8, maven, redis and cassandra need to be installed beforehand.
Specify redis and cassandra connection options in `src/main/java/com/haystack/Config.java`.
After installing cassandra, make sure to run `nodetool enablethrift` to make its API accessible from java.

Then run
`make install`
`run-server.sh $port_number`

## Tests:
`make test`
