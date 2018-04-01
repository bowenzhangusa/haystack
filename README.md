run this to start the server
 mvn -q -nsu compile exec:exec -Dcheckstyle.skip=true -Dforcenpn=true -DmainClass="com.haystack.server.HaystackServer" -Dargs="8080"
