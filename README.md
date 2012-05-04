The Java binding for the Neo4j Server REST API wraps the REST calls behind the well known
[GraphDatabaseService](http://api.neo4j.org/1.2/org/neo4j/graphdb/GraphDatabaseService.html) API.

Currently supports:
___________________
 * all the node and relationship operations
 * the new Index API
 * Basic Http Auth (Digest)
 * preliminary traversal support
 * cypher, gremlin support
 * support for batch API
 * preliminary support for arbitrary server plugins and extensions
 
Open issues:
____________
 * full traversal support

Usage:
------

Build it locally. Then use the maven / ivy dependency or copy the jar into your app.

    <dependency>
		<groupId>org.neo4j</groupId>
		<artifactId>neo4j-rest-graphdb</artifactId>
		<version>1.6.M02</version>
    </dependency>

    GraphDatabaseService gds = new RestGraphDatabase("http://localhost:7474/db/data");
    GraphDatabaseService gds = new RestGraphDatabase("http://localhost:7474/db/data",username,password);

    <bean id="graphDbService" class="org.neo4j.rest.graphdb.RestGraphDatabase" destroy-method="shutdown">
        <constructor-arg index="0" value="http://localhost:7474/db/data" />
    </bean>
    </pre>

**Please note: Transactions are not supported over this API.**

Unit Test:
----------
to start tests you will need the https://github.com/jexp/neo4j-clean-remote-db-addon to cleanup the database while


References / Community:
-----------------------

 * [Neo4j community site](http://neo4j.org)
 * [Neo4j REST API](http://docs.neo4j.org/chunked/milestone/rest-api.html)
 * [Neo4j Docs](http://docs.neo4j.org)
 * [Neo4j Mailing List](http://neo4j.org/forums)
