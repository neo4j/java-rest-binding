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
package org.neo4j.rest.graphdb.entity;

import static java.util.Arrays.asList;

import java.net.URI;
import java.util.*;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.helpers.collection.CombiningIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.traversal.RestDirection;
import org.neo4j.rest.graphdb.util.ResourceIterableWrapper;

public class RestNode extends RestEntity implements Node {
    public RestNode( URI uri, RestAPI restApi ) {
        super( uri, restApi );
    }

    public RestNode( String uri, RestAPI restApi ) {
        super( uri, restApi );
    }

    public RestNode( Map<?, ?> data, RestAPI restApi ) {
        super( data, restApi );
    }    
  
    public Relationship createRelationshipTo( Node toNode, RelationshipType type ) {
    	 return this.restApi.createRelationship(this, toNode, type, null);
    }

    public Iterable<Relationship> getRelationships() {
        return restApi.getRelationships(this, "relationships/all");
    }

    public Iterable<Relationship> getRelationships( RelationshipType... types ) {
        String path = getStructuralData().get( "all_relationships" ) + "/";
        int counter = 0;
        for ( RelationshipType type : types ) {
            if ( counter++ > 0 ) {
                path += "&";
            }
            path += type.name();
        }
        return restApi.getRelationships(this, path);
    }


    public Iterable<Relationship> getRelationships( Direction direction ) {
        return restApi.getRelationships(this, "relationships/" + RestDirection.from(direction).shortName);
    }

    public Iterable<Relationship> getRelationships( RelationshipType type,
                                                    Direction direction ) {
        String relationshipsKey = RestDirection.from( direction ).longName + "_relationships";
        Object relationship = getStructuralData().get( relationshipsKey );
        return restApi.getRelationships(this, relationship + "/" + type.name());
    }

    public Relationship getSingleRelationship( RelationshipType type,
                                               Direction direction ) {
        return IteratorUtil.singleOrNull( getRelationships( type, direction ) );
    }

    public boolean hasRelationship() {
        return getRelationships().iterator().hasNext();
    }

    public boolean hasRelationship( RelationshipType... types ) {
        return getRelationships( types ).iterator().hasNext();
    }

    public boolean hasRelationship( Direction direction ) {
        return getRelationships( direction ).iterator().hasNext();
    }

    public boolean hasRelationship( RelationshipType type, Direction direction ) {
        return getRelationships( type, direction ).iterator().hasNext();
    }

    public Traverser traverse( Order order, StopEvaluator stopEvaluator,
                               ReturnableEvaluator returnableEvaluator, Object... rels ) {
        throw new UnsupportedOperationException();
    }

    public Traverser traverse( Order order, StopEvaluator stopEvaluator,
                               ReturnableEvaluator returnableEvaluator, RelationshipType type, Direction direction ) {
        throw new UnsupportedOperationException();
    }

    public Traverser traverse( Order order, StopEvaluator stopEvaluator,
                               ReturnableEvaluator returnableEvaluator, RelationshipType type, Direction direction,
                               RelationshipType secondType, Direction secondDirection ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Relationship> getRelationships(final Direction direction, RelationshipType... types) {
        return new CombiningIterable<Relationship>(new IterableWrapper<Iterable<Relationship>, RelationshipType>(asList(types)) {
            @Override
            protected Iterable<Relationship> underlyingObjectToObject(RelationshipType relationshipType) {
                return getRelationships(relationshipType,direction);
            }
        });
    }

    @Override
    public boolean hasRelationship(Direction direction, RelationshipType... types) {
        for (RelationshipType relationshipType : types) {
            if (hasRelationship(relationshipType,direction)) return true;
        }
        return false;
    }

    private final Set<String> labels=new HashSet<String>();

    @Override
    public void addLabel(Label label) {
        restApi.addLabel(labelsPath(),label.name());
        this.labels.add(label.name());
    }

    @Override
    public void removeLabel(Label label) {
        restApi.removeLabel(labelsPath(),label.name());
        this.labels.remove(label.name());
    }

    @Override
    public boolean hasLabel(Label label) {
        updateLabels();
        return this.labels.contains(label.name());
    }

    private void updateLabels() {
        if (hasToUpdateProperties()) {
            Collection<String> labels=restApi.getLabels(labelsPath());
            this.labels.clear();
            this.labels.addAll(labels);
        }
    }

    @Override
    public ResourceIterable<Label> getLabels() {
        updateLabels();
        return new ResourceIterableWrapper<Label,String>(labels) {
            @Override
            protected Label underlyingObjectToObject(String s) {
                return DynamicLabel.label(s);
            }
        };
    }

    public String labelsPath() {
        Object path = getStructuralData().get("labels");
        return path==null ? null : path.toString();
    }
}
