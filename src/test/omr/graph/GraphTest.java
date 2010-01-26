//----------------------------------------------------------------------------//
//                                                                            //
//                             G r a p h T e s t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.graph;

import omr.util.BaseTestCase;
import static junit.framework.Assert.*;

import java.util.*;

/**
 * Class <code>GraphTest</code> performs very basic tests on a graph and
 * vertices in combination.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GraphTest
    extends BaseTestCase
{
    //~ Instance fields --------------------------------------------------------

    private MyDigraph   graph;
    private MyVertex    v1;
    private MyVertex    v2;
    private MyVertex    v3;
    private MySignature s1;
    private MySignature s2;
    private MySignature s3;

    //~ Methods ----------------------------------------------------------------

    //-----------------------//
    // testAddEdgeNoVertices //
    //-----------------------//
    //@Test
    public void testAddEdgeNoVertices ()
    {
        MyVertex v10 = null;
        MyVertex v20 = null;

        try {
            MyVertex.addEdge(v10, v20);
            fail(
                "Exception should be raised" +
                " when edge is allocated between null vertices");
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    //-------------------//
    // testAddNullVertex //
    //-------------------//
    //@Test
    public void testAddNullVertex ()
    {
        try {
            graph.addVertex(null);
            fail("Exception should be raised" + " when adding a null vertex");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    //---------------//
    // testAddVertex //
    //---------------//
    //@Test
    public void testAddVertex ()
    {
        MyVertex v = new MyVertex();
        graph.addVertex(v);
        assertEquals(
            "Graph should contain just one vertex.",
            1,
            graph.getVertexCount());
        assertSame(
            "Retrieving Vertex just added.",
            v,
            graph.getVertexById(v.getId()));
    }

    //---------------------//
    // testCrossGraphEdges //
    //---------------------//
    //@Test
    public void testCrossGraphEdges ()
    {
        createVertices();

        MyDigraph g2 = new MyDigraph();

        MyVertex  v = g2.createVertex();

        try {
            MyVertex.addEdge(v1, v);
            fail(
                "Exception should be raised" +
                " when edge is allocated across graphs");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    //--------------------//
    // testEdgeAllocation //
    //--------------------//
    //@Test
    public void testEdgeAllocation ()
    {
        createVertices();

        // Link with edges
        createEdges();

        graph.dump("\nDump of whole graph:");
        assertEquals(
            "v1 should have 2 targets : v2 & v3.",
            2,
            v1.getOutDegree());
        assertEquals(v1.getTargets().get(0).getId(), v2.getId());
        assertEquals(v1.getTargets().get(1), v3);
        assertSame(v2.getSources().get(0), v1);
        assertEquals(1, v2.getOutDegree());
        assertEquals(1, v3.getOutDegree());
        assertEquals("Vertex v3 should have 2 sources.", 2, v3.getInDegree());
    }

    //-----------------//
    // testEdgeRemoval //
    //-----------------//
    //@Test
    public void testEdgeRemoval ()
    {
        createVertices();
        createEdges();

        // Remove an edge
        MyVertex.removeEdge(v2, v3);

        graph.dump("\nDump after removal of edge from v2 to v3:");
        assertEquals(
            "Vertex v2 should have no more targets.",
            0,
            v2.getOutDegree());
        assertEquals("Vertex v3 should have one source.", 1, v3.getInDegree());
    }

    //---------------------//
    // testGetLastVertexId //
    //---------------------//
    //@Test
    public void testGetLastVertexId ()
    {
        createVertices();
        assertEquals("Last vertex id should be 3.", 3, graph.getLastVertexId());
    }

    //-------------------//
    // testGetVertexById //
    //-------------------//
    //@Test
    public void testGetVertexById ()
    {
        createVertices();

        MyVertex v = graph.getVertexById(2);
        assertSame("Vertex of id 2 should be v2.", v2, graph.getVertexById(2));
    }

    //------------------------//
    // testGetVertexByWrongId //
    //------------------------//
    //@Test
    public void testGetVertexByWrongId ()
    {
        createVertices();
        assertNull(
            "No vertex should be found with wrong id.",
            graph.getVertexById(123));
    }

    //-----------------//
    // testGetVertices //
    //-----------------//
    //@Test
    public void testGetVertices ()
    {
        createVertices();

        Collection<MyVertex> ref = new ArrayList<MyVertex>();
        ref.add(v1);
        ref.add(v3);
        ref.add(v2);
        System.out.println("\ntestGetVertices:");
        System.out.println("ref=" + ref);
        System.out.println("vertices=" + graph.getVertices());
        assertTrue(
            "Non correct collection of vertices",
            ref.containsAll(graph.getVertices()));
        assertTrue(
            "Non correct collection of vertices",
            graph.getVertices().containsAll(ref));
    }

    //---------------------//
    // testGraphAllocation //
    //---------------------//
    //@Test
    public void testGraphAllocation ()
    {
        assertNotNull("Graph was not allocated.", graph);
        assertEquals("Graph should have no vertex.", 0, graph.getVertexCount());
    }

    //------------------//
    // testMultipleEdge //
    //------------------//
    //@Test
    public void testMultipleEdge ()
    {
        createVertices();
        MyVertex.addEdge(v1, v2);
        MyVertex.addEdge(v1, v2);

        graph.dump("\ntestMultipleEdge. attempt of multiple edges:");
        assertEquals("There should be just one target.", 1, v1.getOutDegree());
        assertEquals("There should be just one source.", 1, v2.getInDegree());
    }

    //-------------------//
    // testNoEdgeRemoval //
    //-------------------//
    //@Test
    public void testNoEdgeRemoval ()
    {
        createVertices();

        try {
            // Remove a non-existing edge
            MyVertex.removeEdge(v2, v3);
            fail(
                "Exception should be raised" +
                " when attempting to remove a non-existent edge");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    //----------------------//
    // testRemoveNullVertex //
    //----------------------//
    //@Test
    public void testRemoveNullVertex ()
    {
        createVertices();

        try {
            graph.removeVertex(null);
            fail("Exception should be raised" + " when removing a null vertex");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    //----------------------//
    // testVertexAllocation //
    //----------------------//
    //@Test
    public void testVertexAllocation ()
    {
        // Allocate some vertices
        createVertices();
        assertEquals(
            "Graph should contain exactly 3 vertices.",
            3,
            graph.getVertexCount());
    }

    //---------------------------//
    // testVertexDoubleSignature //
    //---------------------------//
    //@Test
    public void testVertexDoubleSignature ()
    {
        // Allocate some vertices
        createVertices();
        v1.setSignature();

        try {
            v1.setSignature();
            fail(
                "Exception should be raised" +
                " when changing vertex signature");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    //-------------------------//
    // testVertexNullSignature //
    //-------------------------//
    //@Test
    public void testVertexNullSignature ()
    {
        // Allocate some vertices
        createVertices();

        try {
            s1 = v1.getSignature();
            fail(
                "Exception should be raised" +
                " when getting a null vertex signature");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    //-------------------//
    // testVertexRemoval //
    //-------------------//
    //@Test
    public void testVertexRemoval ()
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
        assertEquals(
            "Graph should now be totally empty.",
            0,
            graph.getVertexCount());
    }

    //---------------------//
    // testVertexSignature //
    //---------------------//
    //@Test
    public void testVertexSignature ()
    {
        // Allocate some vertices
        createVertices();
        v1.setSignature();
        v2.setSignature();
        v3.setSignature();

        s1 = v1.getSignature();
        System.out.println(
            "v1 " + v1 + " @" + Integer.toHexString(v1.hashCode()));
        System.out.println("s1 " + s1);

        MyVertex v = graph.getVertexBySignature(s1);
        System.out.println("v " + v + " @" + Integer.toHexString(v.hashCode()));

        assertEquals("Should retrieve v1 via s1", v1, v);
        assertEquals(
            "Should retrieve v2 via s2",
            v2,
            graph.getVertexBySignature(v2.getSignature()));
        assertEquals(
            "Should retrieve v3 via s3",
            v3,
            graph.getVertexBySignature(v3.getSignature()));
    }

    //-------//
    // setUp //
    //-------//
    //@Configuration(beforeTestMethod = true)
    @Override
    protected void setUp ()
    {
        graph = new MyDigraph();
    }

    private void createEdges ()
    {
        MyVertex.addEdge(v1, v2);
        MyVertex.addEdge(v1, v3);
        MyVertex.addEdge(v2, v3);
        MyVertex.addEdge(v3, v1);
    }

    private void createVertices ()
    {
        v1 = graph.createVertex();
        v2 = graph.createVertex();
        v3 = graph.createVertex();
    }

    //~ Inner Classes ----------------------------------------------------------

    static class MyDigraph
        extends Digraph<MyDigraph, MyVertex, MySignature>
    {
        //~ Constructors -------------------------------------------------------

        public MyDigraph ()
        {
            super("MyDigraph", MyVertex.class);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            return super.toString() + "}";
        }
    }

    static class MySignature
    {
        //~ Instance fields ----------------------------------------------------

        final MyVertex vertex;

        //~ Constructors -------------------------------------------------------

        public MySignature (MyVertex vertex)
        {
            this.vertex = vertex;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            return "{Sig " + Integer.toHexString(vertex.hashCode()) + "}";
        }
    }

    static class MyVertex
        extends Vertex<MyDigraph, MyVertex, MySignature>
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            return super.toString() + "}";
        }

        @Override
        protected MySignature computeSignature ()
        {
            return new MySignature(this);
        }
    }
}
