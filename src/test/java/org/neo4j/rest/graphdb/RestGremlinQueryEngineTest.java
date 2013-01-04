/**
 * Copyright (c) 2002-2013 "Neo Technology,"
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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.rest.graphdb.query.RestGremlinQueryEngine;

import java.util.ArrayList;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class RestGremlinQueryEngineTest extends RestTestBase {
    private RestGremlinQueryEngine queryEngine;
    private RestAPI restAPI;
    private MatrixDataGraph embeddedMatrixdata;
    private MatrixDataGraph restMatrixData;
    
    @Before
    public void init() throws Exception {
        embeddedMatrixdata = new MatrixDataGraph(getGraphDatabase()).createNodespace();
        restMatrixData = new MatrixDataGraph(getRestGraphDb());
        this.restAPI = ((RestGraphDatabase)getRestGraphDb()).getRestAPI();
        queryEngine = new RestGremlinQueryEngine(restAPI);      
    }
    
    
    @Test
    public void testGetReferenceNode(){
        final String queryString = "g.v(0)";
        final Node result = (Node) queryEngine.query(queryString, null).to(Node.class).single();
        assertEquals(embeddedMatrixdata.getGraphDatabase().getReferenceNode(), result);

    }
    
    @Test
    public void testGetNeoNodeByReferenceNode(){
        final String queryString = "g.v(0).out('NEO_NODE')";
        final Node result = (Node) queryEngine.query(queryString, null).to(Node.class).single();
        assertEquals(embeddedMatrixdata.getNeoNode(), result);

    }
    
    @Test
    public void testGetSingleProperty(){
        final String queryString = "g.v(neo).name";
        final String result = (String) queryEngine.query(queryString, MapUtil.map("neo",getNeoId())).to(String.class).single();
        assertEquals(embeddedMatrixdata.getNeoNode().getProperty("name"), result);

    }
    
    @Test
    public void testGetMultipleResults(){        
        final String queryString = "g.v(neo).out('KNOWS').name";
        final Collection<Object> result =  IteratorUtil.asCollection(queryEngine.query(queryString, MapUtil.map("neo",getNeoId())));        
        assertEquals(createCollectionWithMultipleProperties("Trinity","Morpheus") ,result); 
    }
    
    @Test
    public void testGetMultipleResultsAsNodes(){        
        final String queryString = "g.v(neo).out('KNOWS')";
        final Collection<Node> result =  IteratorUtil.asCollection(queryEngine.query(queryString, MapUtil.map("neo",getNeoId())).to(Node.class));        
        assertEquals(createCollectionWithMultipleProperties(embeddedMatrixdata.getGraphDatabase().getNodeById(getTrinityId()),embeddedMatrixdata.getGraphDatabase().getNodeById(getMorpheusId())) ,result); 
    }
    
    @Test
    public void testGetMultipleResultsWithMultipleStartNodes(){        
        final String queryString = "[g.v(morpheus), g.v(trinity)]._().out('KNOWS').name";
        final Collection<Object> result =  IteratorUtil.asCollection(queryEngine.query(queryString, MapUtil.map("morpheus", getMorpheusId(), "trinity", getTrinityId())));        
        assertEquals(createCollectionWithMultipleProperties("Trinity","Cypher","Cypher") ,result);        
    }
    
    @Test
    public void testGetMultipleIDResults(){        
        final String queryString = "g.v(neo).out('KNOWS').id";
        final Collection<Object> result =  IteratorUtil.asCollection(queryEngine.query(queryString, MapUtil.map("neo",getNeoId())));        
        assertEquals(createCollectionWithMultipleProperties((int)getTrinityId(),(int)getMorpheusId()) ,result); 
    }
    
    
    @Test
    public void testFilter(){        
        final String queryString = "g.v(morpheus).out('KNOWS').filter{it.type.equals(\"villain\")}.name";
        final Collection<Object> result =  IteratorUtil.asCollection(queryEngine.query(queryString, MapUtil.map("morpheus",getMorpheusId())));        
        assertEquals(createCollectionWithMultipleProperties("Cypher") ,result); 
       
    }    
  
    /**
    @Test
    public void testIfThenElse(){        
        final String queryString = "g.v(neo).out('KNOWS').ifThenElse{it.name.equals(\"Trinity\")}{it.name}{it.out('KNOWS').name}";
        final Collection<Object> result =  IteratorUtil.asCollection(queryEngine.query(queryString, MapUtil.map("neo",getNeoId())));        
        //assertEquals(createCollectionWithMultipleProperties((int)getTrinityId(),(int)getMorpheusId()) ,result); 
        System.out.println(result);
    }*/
    

    @Ignore("figure out new syntax")
    @Test
    public void testQueryList2(){
        final String queryString = "[g.v(neo),g.v(trinity)]._().type.as('person.type').name.as('person.name').table(new Table()).cap >> 1";       
        final Collection<Object> result = IteratorUtil.asCollection(queryEngine.query(queryString, MapUtil.map("neo", getNeoId(), "trinity",getTrinityId())));     
        assertEquals(asList(MapUtil.map("person.type","hero", "person.name", "Thomas Anderson"), MapUtil.map("person.type","hero", "person.name", "Trinity")), result);

    }    
    
    
    @Test
    @Ignore("figure out new syntax")
    public void testQueryList() throws Exception {
        final String queryString = "t = new Table(); [g.v(neo),g.v(trinity)].each{ n -> n.as('person.name').as('person.type').table(t,['person.name','person.type']){ it.type }{ it.name } >> -1}; t;" ;
        final Collection<Object> result = IteratorUtil.asCollection(queryEngine.query(queryString, MapUtil.map("neo", getNeoId(), "trinity",getTrinityId())));       
        assertEquals(asList(MapUtil.map("person.type","hero", "person.name", "Thomas Anderson"), MapUtil.map("person.type","hero", "person.name", "Trinity")), result);
    }

    
   public ArrayList<Object> createCollectionWithMultipleProperties(Object... params){
       ArrayList<Object> list = new ArrayList<Object>();
       for(Object param : params){
           list.add(param);
       }
       return list;
   }
    
   
    
    public long getNeoId(){
        return  embeddedMatrixdata.getNeoNode().getId();
    }
    
    public long getTrinityId(){
        IndexManager index = getRestGraphDb().index();             
        Index<Node> goodGuys = index.forNodes("heroes");
        IndexHits<Node> hits = goodGuys.get( "name", "Trinity" );
        Node trinity = hits.getSingle();
        return trinity.getId();
    }
    
    public long getMorpheusId(){
        IndexManager index = getRestGraphDb().index();             
        Index<Node> goodGuys = index.forNodes("heroes");
        IndexHits<Node> hits = goodGuys.get( "name", "Morpheus" );
        Node morpheus = hits.getSingle();
        return morpheus.getId();
    }
   
}
