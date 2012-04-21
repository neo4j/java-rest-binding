/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb;


import java.util.Map;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.rest.graphdb.index.RestIndexManager;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.neo4j.rest.graphdb.util.ResultConverter;


public class RestGraphDatabase extends AbstractRemoteDatabase {
    private RestAPI restAPI;
    private final RestCypherQueryEngine cypherQueryEngine;


    public RestGraphDatabase( RestAPI api){
    	this.restAPI = api;
        cypherQueryEngine = new RestCypherQueryEngine(restAPI);
    }
    
    public RestGraphDatabase( String uri ) {
        this( new ExecutingRestRequest( uri ));
    }

    public RestGraphDatabase( String uri, String user, String password ) {
        this(new ExecutingRestRequest( uri, user, password ));
    }
    
    public RestGraphDatabase( RestRequest restRequest){
    	this(new RestAPI(restRequest));
    } 
    
    
    public RestAPI getRestAPI(){
    	return this.restAPI;
    }
    
    
    @Override
    public RestIndexManager index() {
       return this.restAPI.index();
    }

    @Override
    public Node createNode() {
    	return this.restAPI.createNode(null);
    }
  
    @Override
    public Node getNodeById( long id ) {
    	return this.restAPI.getNodeById(id);
    }

    @Override
    public Node getReferenceNode() {
        return this.restAPI.getReferenceNode();
    }

    @Override
    public Iterable<Node> getAllNodes() {
        return cypherQueryEngine.query("start n=node(*) return n", null).to(Node.class);
    }

    public Iterable<Map<String, Object>> execute( String statement, Map<String, Object> params )
    {
        return cypherQueryEngine.query( statement, params );
    }


    @Override
    public Iterable<RelationshipType> getRelationshipTypes() {
        return cypherQueryEngine.query( "start n=node(*) match n-[r]->() return distinct type(r) as rel_type", null ).to(
                RelationshipType.class,//
                new ResultConverter<Map<String, Object>, RelationshipType>()
                {
            @Override
            public RelationshipType convert(Map<String, Object> row, Class<RelationshipType> type) {
                return DynamicRelationshipType.withName((String)row.get("rel_type"));
            }
        });
    }

    @Override
    public Relationship getRelationshipById( long id ) {
    	return this.restAPI.getRelationshipById(id);
    }    

  
    public RestRequest getRestRequest() {
        return this.restAPI.getRestRequest();
    }

    public long getPropertyRefetchTimeInMillis() {
        return this.restAPI.getPropertyRefetchTimeInMillis();
	}

    @Override
    public String getStoreDir() {
        return restAPI.getStoreDir();
    }
}

