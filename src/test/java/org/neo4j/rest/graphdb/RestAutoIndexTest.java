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

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.graphdb.index.RelationshipAutoIndexer;
import org.neo4j.rest.graphdb.util.StreamJsonHelper;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RestAutoIndexTest extends RestTestBase {

    @Test
    public void testEnableDisableAutoIndexerNode() {
        AutoIndexer<Node> indexer = getRestGraphDb().index().getNodeAutoIndexer();
        testEnableDisableAutoIndexer(indexer);
    }

    @Test
    public void testEnableDisableAutoIndexerRelationship() {
        RelationshipAutoIndexer indexer = getRestGraphDb().index().getRelationshipAutoIndexer();
        testEnableDisableAutoIndexer(indexer);
    }

    @Test
    public void testAddRemoveAutoIndexerPropertiesOnNodes() {
        AutoIndexer<Node> indexer = getRestGraphDb().index().getNodeAutoIndexer();
        testAddRemoveAutoIndexerProperties(indexer);
    }

    @Test
    public void testAddRemoveAutoIndexerPropertiesOnRelationships() {
        RelationshipAutoIndexer indexer = getRestGraphDb().index().getRelationshipAutoIndexer();
        testAddRemoveAutoIndexerProperties(indexer);
    }

    private void testAddRemoveAutoIndexerProperties(AutoIndexer<? extends PropertyContainer> indexer) {
        assertTrue(indexer.getAutoIndexedProperties().isEmpty());

        indexer.startAutoIndexingProperty("property1");
        assertTrue(indexer.getAutoIndexedProperties().size()==1);
        assertTrue(indexer.getAutoIndexedProperties().contains("property1"));

        indexer.startAutoIndexingProperty("property2");
        assertTrue(indexer.getAutoIndexedProperties().size() == 2);
        assertTrue(indexer.getAutoIndexedProperties().contains("property2"));

        indexer.stopAutoIndexingProperty("property2");
        assertTrue(indexer.getAutoIndexedProperties().size() == 1);
        assertFalse(indexer.getAutoIndexedProperties().contains("property2"));

        indexer.stopAutoIndexingProperty("property1");
        assertTrue(indexer.getAutoIndexedProperties().isEmpty());

        indexer.stopAutoIndexingProperty("propertyUnknown");
        assertTrue(indexer.getAutoIndexedProperties().isEmpty());

    }

    private void testEnableDisableAutoIndexer(AutoIndexer<? extends PropertyContainer> indexer) {
        assertFalse(indexer.isEnabled());
        indexer.setEnabled(true);
        assertTrue(indexer.isEnabled());
        indexer.setEnabled(false);
        assertFalse(indexer.isEnabled());
    }

}
