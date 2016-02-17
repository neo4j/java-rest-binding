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

package org.neo4j.rest.graphdb.traversal;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.Predicate;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import java.util.*;

public class RestOldTraverserWrapper {
    private static class TraverserImpl implements org.neo4j.graphdb.Traverser, Iterator<Node> {
        private TraversalPosition currentPos;
        private Path nextPath;
        private Iterator<Path> iter;
        private int count;
        private Filter filter;

        public TraversalPosition currentPosition() {
            return currentPos;
        }

        public Collection<Node> getAllNodes() {
            List<Node> result = new ArrayList<Node>();
            for (Node node : this) {
                result.add(node);
            }
            return result;
        }

        public Iterator<Node> iterator() {
            return this;
        }

        public boolean hasNext() {
            while (iter.hasNext()) {
                Path path = iter.next();
                if (filter == null || filter.accept(path)) {
                    nextPath = path;
                    return true;
                }
            }
            nextPath = null;
            return false;
        }

        public Node next() {
            currentPos = new PositionImpl(this, nextPath);
            count++;
            return currentPos.currentNode();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class PositionImpl implements TraversalPosition {
        private final Path position;
        private final int count;

        PositionImpl(TraverserImpl traverser, Path position) {
            this.position = position;
            this.count = traverser.count;
        }

        public Node currentNode() {
            return position.endNode();
        }

        public int depth() {
            return position.length();
        }

        public boolean isStartNode() {
            return position.length() == 0;
        }

        public boolean notStartNode() {
            return !isStartNode();
        }

        public Relationship lastRelationshipTraversed() {
            return position.lastRelationship();
        }

        public Node previousNode() {
            return position.lastRelationship().getOtherNode(position.endNode());
        }

        public int returnedNodesCount() {
            return count;
        }

    }

    private static void assertNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException("Null " + message);
        }
    }

    private static final TraversalDescription BASE_DESCRIPTION =
            Traversal.description().uniqueness(Uniqueness.NODE_GLOBAL);


    /**
     * traverse with Max Depth and server side filter
     */
    public static org.neo4j.graphdb.Traverser traverse(Node node, Traverser.Order order,
                                                       Integer maxDepth,
                                                       String filterBody, String filterLanguage,
                                                       Object... rels) {

        assertNotNull(order, "order");
        assertNotNull(filterBody, "body");
        assertNotNull(filterLanguage, "language");
        assertNotNull(maxDepth, "maxDepth");

        RestTraversal rt = getRestTraversal(order, rels);

        rt.maxDepth(maxDepth);

        rt = fillFilter(filterBody, filterLanguage, rt);

        TraverserImpl result = new TraverserImpl();
        result.iter = rt.traverse(node).iterator();
        return result;
    }


    /**
     * traverse with server side prune and filter
     */
    public static org.neo4j.graphdb.Traverser traverse(Node node, Traverser.Order order,
                                                       String pruneBody, String pruneLanguage,
                                                       String filterBody, String filterLanguage,
                                                       Object... rels) {
        assertNotNull(order, "order");
        assertNotNull(pruneBody, "body");
        assertNotNull(pruneLanguage, "language");

        RestTraversal rt = getRestTraversal(order, rels);

        RestTraversalDescription.ScriptLanguage _pruneLanguage = RestTraversalDescription.ScriptLanguage.valueOf(pruneLanguage);
        rt.prune(_pruneLanguage, pruneBody);

        rt = fillFilter(filterBody, filterLanguage, rt);

        TraverserImpl result = new TraverserImpl();
        result.iter = rt.traverse(node).iterator();
        return result;
    }

    /**
     * traverse with Return Evaluator and server side prune
     */
    public static org.neo4j.graphdb.Traverser traverse(Node node, Traverser.Order order,
                                                       String pruneBody, String pruneLanguage,
                                                       ReturnableEvaluator returnableEvaluator,
                                                       Object... rels) {
        assertNotNull(order, "order");
        assertNotNull(pruneBody, "body");
        assertNotNull(pruneLanguage, "language");
        assertNotNull(returnableEvaluator, "returnable evaluator");

        RestTraversal rt = getRestTraversal(order, rels);

        RestTraversalDescription.ScriptLanguage _language = RestTraversalDescription.ScriptLanguage.valueOf(pruneLanguage);
        rt.prune(_language, pruneBody);

        TraverserImpl result = new TraverserImpl();
        result.filter = new Filter(result, returnableEvaluator);
        result.iter = rt.traverse(node).iterator();
        return result;
    }

    /**
     * traverse with Max Depth and Return Evaluator
     */
    public static org.neo4j.graphdb.Traverser traverse(Node node, Traverser.Order order,
                                                       Integer maxDepth,
                                                       ReturnableEvaluator returnableEvaluator,
                                                       Object... rels) {
        assertNotNull(order, "order");
        assertNotNull(maxDepth, "stop evaluator");
        assertNotNull(returnableEvaluator, "returnable evaluator");

        RestTraversal rt = getRestTraversal(order, rels);

        rt.maxDepth(maxDepth);

        // return all nodes incl. start node
        rt.filter(Traversal.returnAll());

        TraverserImpl result = new TraverserImpl();
        result.filter = new Filter(result, returnableEvaluator);
        result.iter = rt.traverse(node).iterator();
        return result;
    }


    public static org.neo4j.graphdb.Traverser traverse(Node node, Traverser.Order order,
                                                       StopEvaluator stopEvaluator,
                                                       ReturnableEvaluator returnableEvaluator,
                                                       Object... rels) {
        throw new UnsupportedOperationException();
    }


    private static RestTraversal fillFilter(String filterBody, String filterLanguage, RestTraversal rt) {
        /**
         * proudly presents this awesome builtin FILTER hack
         */
        if ("builtin".equalsIgnoreCase(filterLanguage)) {
            if ("all".equalsIgnoreCase(filterBody))
                rt.filter(Traversal.returnAll());
            else if ("all_but_start_node".equalsIgnoreCase(filterBody))
                rt.filter(Traversal.returnAllButStartNode());
        } else {
            RestTraversalDescription.ScriptLanguage _filterLanguage = RestTraversalDescription.ScriptLanguage.valueOf(filterLanguage);
            rt.filter(_filterLanguage, filterBody);
        }
        return rt;
    }


    private static RestTraversal getRestTraversal(Traverser.Order order, Object... rels) {
        Stack<Object[]> entries = calcRels(rels);

        RestTraversal rt = new RestTraversal();
        switch (order) {
            case BREADTH_FIRST:
                rt.breadthFirst();
                break;
            case DEPTH_FIRST:
                rt.depthFirst();
                break;
        }
        // writing down relations
        while (!entries.isEmpty()) {
            Object[] entry = entries.pop();
            rt.relationships((RelationshipType) entry[0], (Direction) entry[1]);
        }
        return rt;
    }


    private static Stack<Object[]> calcRels(Object... rels) {
        if (rels.length % 2 != 0 || rels.length == 0) {
            throw new IllegalArgumentException();
        }

        Stack<Object[]> entries = new Stack<Object[]>();
        for (int i = 0; i < rels.length; i += 2) {
            Object relType = rels[i];
            if (relType == null) {
                throw new IllegalArgumentException(
                        "Null relationship type at " + i);
            }
            if (!(relType instanceof RelationshipType)) {
                throw new IllegalArgumentException(
                        "Expected RelationshipType at var args pos " + i
                                + ", found " + relType);
            }
            Object direction = rels[i + 1];
            if (direction == null) {
                throw new IllegalArgumentException(
                        "Null direction at " + (i + 1));
            }
            if (!(direction instanceof Direction)) {
                throw new IllegalArgumentException(
                        "Expected Direction at var args pos " + (i + 1)
                                + ", found " + direction);
            }
            entries.push(new Object[]{relType, direction});
        }
        return entries;
    }

    private static class Filter implements Predicate<Path> {
        private final TraverserImpl traverser;
        private final ReturnableEvaluator evaluator;

        Filter(TraverserImpl traverser, ReturnableEvaluator returnableEvaluator) {
            this.traverser = traverser;
            this.evaluator = returnableEvaluator;
        }

        public boolean accept(Path position) {
            return evaluator.isReturnableNode(new PositionImpl(traverser,
                    position));
        }
    }
}