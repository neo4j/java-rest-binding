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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.neo4j.helpers.collection.MapUtil.map;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.entity.RestRelationship;
import org.neo4j.rest.graphdb.index.RestIndex;
import org.neo4j.rest.graphdb.index.RestIndexManager;
import org.neo4j.rest.graphdb.util.TestHelper;

public class RestAPITest extends RestTestBase {
	
	private RestAPI restAPI;
	
	@Before
	public void init(){
		this.restAPI = ((RestGraphDatabase)getRestGraphDb()).getRestAPI();
	}

    @Test
    public void testUserAgent() throws Exception {
        restAPI.createNode(map());
        assertTrue(getUserAgent().matches("neo4j-rest-graphdb/[\\d.]+"));
    }
    @Test
    public void testOverrideUserAgent() throws Exception {
        System.setProperty(UserAgent.NEO4J_DRIVER_PROPERTY,"foo/bar");
        new RestAPIFacade(restAPI.getBaseUri()).createNode(map());
        assertTrue(getUserAgent().matches("foo/bar"));
        System.setProperty(UserAgent.NEO4J_DRIVER_PROPERTY,"");
    }

    @Test
    public void testCreateNodeWithParams() {
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("name", "test");
        Node node = this.restAPI.createNode(props);
        Assert.assertEquals( node, getRestGraphDb().getNodeById( node.getId() ));
        Assert.assertEquals( "test", getRestGraphDb().getNodeById( node.getId()).getProperty("name") );
    }

    @Test
    public void testGetSingleRelationshipShouldReturnNullIfThereIsNone() throws Exception {
        assertNull(getRestGraphDb().getReferenceNode().getSingleRelationship(DynamicRelationshipType.withName("foo"),Direction.OUTGOING));
    }
    @Test
    public void testHasSingleRelationshipShouldReturnFalseIfThereIsNone() throws Exception {
        assertEquals(false,getRestGraphDb().getReferenceNode().hasRelationship(DynamicRelationshipType.withName("foo"),Direction.OUTGOING));
    }

	@Test
    public void testCreateRelationshipWithParams() {
        Node refNode = getRestGraphDb().getReferenceNode();
        Node node = getRestGraphDb().createNode();
        Map<String, Object> props = new HashMap<String, Object>();
		props.put("name", "test");
        Relationship rel = this.restAPI.createRelationship(refNode, node, Type.TEST, props );
        Relationship foundRelationship = TestHelper.firstRelationshipBetween( refNode.getRelationships( Type.TEST, Direction.OUTGOING ), refNode, node );
        Assert.assertNotNull( "found relationship", foundRelationship );
        Assert.assertEquals( "same relationship", rel, foundRelationship );
        Assert.assertThat( refNode.getRelationships( Type.TEST, Direction.OUTGOING ), new IsRelationshipToNodeMatcher( refNode, node ) );
        Assert.assertThat( refNode.getRelationships( Direction.OUTGOING ), new IsRelationshipToNodeMatcher( refNode, node ) );
        Assert.assertThat( refNode.getRelationships( Direction.BOTH ), new IsRelationshipToNodeMatcher( refNode, node ) );
        Assert.assertThat( refNode.getRelationships( Type.TEST ), new IsRelationshipToNodeMatcher( refNode, node ) );
        Assert.assertEquals( "test", rel.getProperty("name") );
    }
	
	@Test (expected = IllegalArgumentException.class)
	public void testForNotCreatedIndex() {
		this.restAPI.getIndex("i do not exist");
	}
	
	@Test
	public void testIndexForNodes(){
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
   	    Index<Node> testIndex = index.forNodes("indexName");
   	    assertTrue(index.existsForNodes("indexName"));
	}
	
	@Test
	public void testGetIndexForNodes(){
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
    	Index<Node> testIndex = index.forNodes("indexName");
    	Assert.assertEquals(testIndex.getName(), this.restAPI.getIndex("indexName").getName());    	
	}
	
	@Test
	public void testCreateRestAPIIndexForNodes(){		
		this.restAPI.createIndex(Node.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
  	    assertTrue(index.existsForNodes("indexName"));
	}
	
	
	@Test 
	public void testForDoubleCreatedIndexForNodesWithSameParams() {
		this.restAPI.createIndex(Node.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);		
		this.restAPI.createIndex(Node.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
	}
	
	@Test 
	public void testForDoubleCreatedIndexForNodesWithSameParamsWithoutFullText() {
		this.restAPI.createIndex(Node.class, "indexName", LuceneIndexImplementation.EXACT_CONFIG);		
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
   	    Index<Node> testIndex = index.forNodes("indexName");   		
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testForDoubleCreatedIndexForNodesWithEmptyParams() {
		this.restAPI.createIndex(Node.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
   	    Index<Node> testIndex = index.forNodes("indexName", new HashMap<String, String>());   		
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testForDoubleCreatedIndexForNodesWithEmptyParamsReversed() {
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
   	    Index<Node> testIndex = index.forNodes("indexName", new HashMap<String, String>());   
		this.restAPI.createIndex(Node.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);   		
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testForDoubleCreatedIndexForNodesWithDifferentParamsViaREST() {
		this.restAPI.createIndex(Node.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);		
		this.restAPI.createIndex(Node.class, "indexName", LuceneIndexImplementation.EXACT_CONFIG);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testForDoubleCreatedIndexForNodesWithDifferentParams() {
		this.restAPI.createIndex(Node.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
		HashMap<String, String> config = new HashMap<String, String>();
		config.put("test", "value");
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
   	    Index<Node> testIndex = index.forNodes("indexName", config);   
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testForDoubleCreatedIndexForNodesWithDifferentParamsReversed() {		
		HashMap<String, String> config = new HashMap<String, String>();
		config.put("test", "value");
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
   	    Index<Node> testIndex = index.forNodes("indexName", config);  
   	    this.restAPI.createIndex(Node.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
	}
	
	@Test
	public void testGetIndexByIndexForNodesCreationViaRestAPI(){
		IndexManager index = getRestGraphDb().index();
    	Index<Node> testIndex = index.forNodes("indexName");
    	Assert.assertEquals(testIndex.getName(), this.restAPI.getIndex("indexName").getName());
	}
	
	@Test
	public void testCreateRestAPIIndexForRelationship(){
		Node refNode = getRestGraphDb().getReferenceNode();
	    Node node = getRestGraphDb().createNode();
	    Map<String, Object> props = new HashMap<String, Object>();
		props.put("name", "test");
	    Relationship rel = this.restAPI.createRelationship(refNode, node, Type.TEST, props );
		this.restAPI.createIndex(Relationship.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
		IndexManager index = getRestGraphDb().index();
  	    assertTrue(index.existsForRelationships("indexName"));
	}
	
	@Test
	public void testIndexForRelationships(){
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
   	    Index<Relationship> testIndex = index.forRelationships("indexName");
   	    assertTrue(index.existsForRelationships("indexName"));
	}
	
	@Test
	public void testGetIndexForRelationships(){
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
    	Index<Relationship> testIndex = index.forRelationships("indexName");
    	Assert.assertEquals(testIndex.getName(), this.restAPI.getIndex("indexName").getName());    	
	}
	
	@Test
	public void testCreateRestAPIIndexForRelationships(){		
		this.restAPI.createIndex(Relationship.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
  	    assertTrue(index.existsForRelationships("indexName"));
	}
	
	
	@Test 
	public void testForDoubleCreatedIndexForRelationshipsWithSameParams() {
		this.restAPI.createIndex(Relationship.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);		
		this.restAPI.createIndex(Relationship.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
	}
	
	@Test 
	public void testForDoubleCreatedIndexForRelationshipsWithSameParamsWithoutFullText() {
		this.restAPI.createIndex(Relationship.class, "indexName", LuceneIndexImplementation.EXACT_CONFIG);		
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
   	    Index<Relationship> testIndex = index.forRelationships("indexName");   		
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testForDoubleCreatedIndexForRelationshipsWithEmptyParams() {
		this.restAPI.createIndex(Relationship.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
   	    Index<Relationship> testIndex = index.forRelationships("indexName", new HashMap<String, String>());   		
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testForDoubleCreatedIndexForRelationshipsWithEmptyParamsReversed() {
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
   	    Index<Relationship> testIndex = index.forRelationships("indexName", new HashMap<String, String>());   
		this.restAPI.createIndex(Relationship.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);   		
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testForDoubleCreatedIndexForRelationshipsWithDifferentParamsViaREST() {
		this.restAPI.createIndex(Relationship.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);		
		this.restAPI.createIndex(Relationship.class, "indexName", LuceneIndexImplementation.EXACT_CONFIG);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testForDoubleCreatedIndexForRelationshipsWithDifferentParams() {
		this.restAPI.createIndex(Relationship.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
		HashMap<String, String> config = new HashMap<String, String>();
		config.put("test", "value");
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
   	    Index<Relationship> testIndex = index.forRelationships("indexName", config);   
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testForDoubleCreatedIndexForRelationshipsWithDifferentParamsReversed() {		
		HashMap<String, String> config = new HashMap<String, String>();
		config.put("test", "value");
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
   	    Index<Relationship> testIndex = index.forRelationships("indexName", config);  
   	    this.restAPI.createIndex(Relationship.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
	}
	
	@Test
	public void testGetIndexByIndexForRelationshipsCreationViaRestAPI(){
		IndexManager index = getRestGraphDb().index();
    	Index<Relationship> testIndex = index.forRelationships("indexName");
    	Assert.assertEquals(testIndex.getName(), this.restAPI.getIndex("indexName").getName());
	}
	
	@Test
	public void testCreateIndexWithSameNameButDifferentType(){
		this.restAPI.createIndex(Relationship.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
		this.restAPI.createIndex(Node.class, "indexName", LuceneIndexImplementation.FULLTEXT_CONFIG);
		RestIndexManager index = (RestIndexManager) getRestGraphDb().index();
		assertTrue(index.existsForNodes("indexName"));
		assertTrue(index.existsForRelationships("indexName"));
	}

    @Test
    public void testCreateNodeUniquely() {
        final RestIndex<Node> index = restAPI.createIndex(Node.class, "unique-node", LuceneIndexImplementation.EXACT_CONFIG);
        final RestNode node1 = restAPI.getOrCreateNode(index, "uid", "42", map("name", "Michael"));
        final RestNode node2 = restAPI.getOrCreateNode(index, "uid", "42", map("name", "Michael2"));
        assertEquals(node1,node2);
        assertEquals("Michael",node1.getProperty("name"));
        assertEquals("Michael",node2.getProperty("name"));
        final RestNode node3 = restAPI.getOrCreateNode(index, "uid", "41", map("name", "Emil"));
        assertEquals(false, node1.equals(node3));
    }
    @Test
    public void testCreateRelationshipUniquely() {
        final RestIndex<Relationship> index = restAPI.createIndex(Relationship.class, "unique-rel", LuceneIndexImplementation.EXACT_CONFIG);
        final RestNode michael = restAPI.createNode(map("name", "Michael"));
        final RestNode david = restAPI.createNode(map("name","David"));
        final RestNode peter = restAPI.createNode(map("name","Peter"));


        final RestRelationship rel1 = restAPI.getOrCreateRelationship(index, "uid", "42", michael, david, "KNOWS", map("at", "Neo4j"));
        final RestRelationship rel2 = restAPI.getOrCreateRelationship(index, "uid", "42", michael, david, "KNOWS", map("at", "Neo4j"));
        assertEquals(rel1,rel2);
        assertEquals("Neo4j",rel1.getProperty("at"));
        assertEquals("Neo4j",rel2.getProperty("at"));
        final RestRelationship rel3 = restAPI.getOrCreateRelationship(index, "uid", "41", michael, david, "KNOWS", map("at", "Neo4j"));
        assertEquals(false, rel3.equals(rel1));
    }
}
