//-----------------------------------------------------------------------//
//                                                                       //
//                           G r a p h T e s t                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.graph;

import omr.util.BaseTestCase;

//import org.testng.annotations.*;
import static junit.framework.Assert.*;
import junit.framework.*;

import java.util.*;

/**
 * Class <code>GraphTest</code> performs very basic tests on a graph and
 * vertices in combination.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class GraphTest
    extends BaseTestCase
{
    static class MyDigraph
        extends Digraph<MyDigraph, MyVertex>
    {
        @Override
            public String toString ()
        {
            return super.toString() + "}";
        }
    }

    static class MyVertex
        extends Vertex<MyDigraph, MyVertex>
    {
        @Override
            public String toString ()
        {
            return super.toString() + "}";
        }
    }

    //~ Instance variables ------------------------------------------------

    private MyDigraph graph;

    private MyVertex v1;
    private MyVertex v2;
    private MyVertex v3;

    //~ Constructors ------------------------------------------------------

    //~ Methods -----------------------------------------------------------

    //-------//
    // setUp //
    //-------//
    //@Configuration(beforeTestMethod = true)
    protected void setUp ()
    {
        graph = new MyDigraph();
    }

    //------------------------//
    // testSetNullVertexClass //
    //------------------------//
    //@Test
    public void testSetNullVertexClass()
    {
        try {
            graph.setVertexClass(null);
            fail("Exception should be raised" +
                 " when setting a null vertex class");
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    //---------------------//
    // testGraphAllocation //
    //---------------------//
    //@Test
    public void testGraphAllocation()
    {
        assertNotNull("Graph was not allocated.", graph);
        assertEquals("Graph should have no vertex.",
                     0, graph.getVertexCount());
    }

    //--------------------------//
    // testNullVertexAllocation //
    //--------------------------//
    //@Test
    public void testNullVertexAllocation()
    {
        try {
            MyVertex v = graph.createVertex();
            fail("Exception should be raised"+
                 " when attempting to createx a vertex"+
                 " with no vertex class defined");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    //----------------------//
    // testVertexAllocation //
    //----------------------//
    //@Test
    public void testVertexAllocation()
    {
        // Allocate some vertices
        createVertices();
        assertEquals("Graph should contain exactly 3 vertices.",
                     3, graph.getVertexCount());
    }

    //-----------------//
    // testGetVertices //
    //-----------------//
    //@Test
    public void testGetVertices()
    {
        createVertices();
        Collection<MyVertex> ref = new ArrayList<MyVertex>();
        ref.add(v1);
        ref.add(v3);
        ref.add(v2);
        System.out.println("\ntestGetVertices:");
        System.out.println("ref=" + ref);
        System.out.println("vertices=" + graph.getVertices());
        assertTrue("Non correct collection of vertices",
                   ref.containsAll(graph.getVertices()));
        assertTrue("Non correct collection of vertices",
                   graph.getVertices().containsAll(ref));
    }

    //---------------------//
    // testGetLastVertexId //
    //---------------------//
    //@Test
    public void testGetLastVertexId()
    {
        createVertices();
        assertEquals("Last vertex id should be 3.",
                     3, graph.getLastVertexId());
    }

    //-------------------//
    // testGetVertexById //
    //-------------------//
    //@Test
    public void testGetVertexById()
    {
        createVertices();
        MyVertex v = graph.getVertexById(2);
        assertSame("Vertex of id 2 should be v2.",
                   v2, graph.getVertexById(2));
    }

    //------------------------//
    // testGetVertexByWrongId //
    //------------------------//
    //@Test
    public void testGetVertexByWrongId()
    {
        createVertices();
        assertNull("No vertex should be found with wrong id.",
                   graph.getVertexById(123));
    }

    //------------------//
    // testMultipleEdge //
    //------------------//
    //@Test
    public void testMultipleEdge()
    {
        createVertices();
        MyVertex.addEdge(v1, v2);
        MyVertex.addEdge(v1, v2);

        graph.dump("\ntestMultipleEdge. attempt of multiple edges:");
        assertEquals("There should be just one target.",
                     1, v1.getOutDegree());
        assertEquals("There should be just one source.",
                     1, v2.getInDegree());
    }

    //--------------------//
    // testEdgeAllocation //
    //--------------------//
    //@Test
    public void testEdgeAllocation()
    {
        createVertices();

        // Link with edges
        createEdges();

        graph.dump("\nDump of whole graph:");
        assertEquals("v1 should have 2 targets : v2 & v3.",
                     2, v1.getOutDegree());
        assertEquals(v1.getTargets().get(0).getId(), v2.getId());
        assertEquals(v1.getTargets().get(1), v3);
        assertSame(v2.getSources().get(0), v1);
        assertEquals(1, v2.getOutDegree());
        assertEquals(1, v3.getOutDegree());
        assertEquals("Vertex v3 should have 2 sources.",
                     2, v3.getInDegree());
    }

    //---------------------//
    // testCrossGraphEdges //
    //---------------------//
    //@Test
    public void testCrossGraphEdges()
    {
        createVertices();

        MyDigraph g2 = new MyDigraph();
        g2.setVertexClass(MyVertex.class);
        MyVertex v = g2.createVertex();

        try {
            MyVertex.addEdge(v1, v);
            fail("Exception should be raised"+
                 " when edge is allocated across graphs");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    //-----------------------//
    // testAddEdgeNoVertices //
    //-----------------------//
    //@Test
    public void testAddEdgeNoVertices()
    {
        MyVertex v1 = null;
        MyVertex v2 = null;
        try {
            MyVertex.addEdge(v1, v2);
            fail("Exception should be raised"+
                 " when edge is allocated between null vertices");
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    //-----------------//
    // testEdgeRemoval //
    //-----------------//
    //@Test
    public void testEdgeRemoval()
    {
        createVertices();
        createEdges();

        // Remove an edge
        MyVertex.removeEdge(v2, v3);

        graph.dump("\nDump after removal of edge from v2 to v3:");
        assertEquals("Vertex v2 should have no more targets.",
                     0, v2.getOutDegree());
        assertEquals("Vertex v3 should have one source.",
                     1, v3.getInDegree());
    }

    //-------------------//
    // testNoEdgeRemoval //
    //-------------------//
    //@Test
    public void testNoEdgeRemoval()
    {
        createVertices();

        try {
            // Remove a non-existing edge
            MyVertex.removeEdge(v2, v3);
            fail("Exception should be raised"+
                 " when attempting to remove a non-existent edge");
        } catch(Exception expected) {
            checkException(expected);
        }
    }

    //----------------------//
    // testRemoveNullVertex //
    //----------------------//
    //@Test
    public void testRemoveNullVertex()
    {
        createVertices();
        try {
            graph.removeVertex(null);
            fail("Exception should be raised"+
                 " when removing a null vertex");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    //-------------------//
    // testVertexRemoval //
    //-------------------//
    //@Test
    public void testVertexRemoval()
    {
        createVertices();
        createEdges();

        // Remove some vertices
        v2.delete();
        graph.dump("\nDump after deletion of vertex v2:");

        // Remove all vertices
        v1.delete();
        v3.delete();
        graph.dump("\nDump after deletion of all vertices:");
        assertEquals("Graph should now be totally empty.",
                     0, graph.getVertexCount());
    }

    //---------------//
    // testAddVertex //
    //---------------//
    //@Test
    public void testAddVertex()
    {
        MyVertex v = new MyVertex();
        graph.addVertex(v);
        assertEquals("Graph should contain just one vertex.",
                     1, graph.getVertexCount());
        assertSame("Retrieving Vertex just added.",
                   v, graph.getVertexById(v.getId()));
    }

    //-------------------//
    // testAddNullVertex //
    //-------------------//
    //@Test
    public void testAddNullVertex()
    {
        try {
            graph.addVertex(null);
            fail("Exception should be raised"+
                 " when adding a null vertex");
        } catch (Exception expected){
            checkException(expected);
        }
    }

    //~ Methods private ---------------------------------------------------

    private void createVertices()
    {
        graph.setVertexClass(MyVertex.class);
        v1 = graph.createVertex();
        v2 = graph.createVertex();
        v3 = graph.createVertex();
    }

    private void createEdges()
    {
        MyVertex.addEdge(v1, v2);
        MyVertex.addEdge(v1, v3);
        MyVertex.addEdge(v2, v3);
        MyVertex.addEdge(v3, v1);
    }
}
