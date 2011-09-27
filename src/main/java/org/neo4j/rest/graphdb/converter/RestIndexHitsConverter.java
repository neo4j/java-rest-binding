/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb.converter;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.rest.graphdb.RequestResult;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.index.SimpleIndexHits;

import java.util.Collection;

/**
 * @author mh
 * @since 22.09.11
 */
public class RestIndexHitsConverter<S extends PropertyContainer> implements RestResultConverter {
    private final RestAPI restAPI;
    private final Class<S> entityType;

    public RestIndexHitsConverter(RestAPI restAPI,Class<S> entityType) {
        this.restAPI = restAPI;
        this.entityType = entityType;
    }

    public IndexHits<S> convertFromRepresentation(RequestResult response) {
        Collection hits = (Collection) response.toEntity();
        return new SimpleIndexHits<S>(hits, hits.size(), entityType, restAPI);
    }

}
