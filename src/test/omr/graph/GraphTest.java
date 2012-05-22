//----------------------------------------------------------------------------//
//                                                                            //
//                             G r a p h T e s t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
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
 * @author Hervé Bitteur
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
    // testAddTargetNoTarget //
    //-----------------------//
    //@Test
    public void testAddTargetNoTarget ()
    {
        MyVertex v10 = new MyVertex();
        MyVertex v20 = null;

        try {
            v10.addTarget(v20);
            fail(
                "Exception should be raised" +
                " when edge is allocated to a null target");
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
            v1.addTarget(v);
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
        v2.removeTarget(v3, true);

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

        Collection<MyVertex> ref = new ArrayList<>();
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
        v1.addTarget(v2);
        v1.addTarget(v2);
        
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
            v2.removeTarget(v3, true);
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
        v1.addTarget(v2);
        v1.addTarget(v3);
        v2.addTarget(v3);
        v3.addTarget(v1);
    }

    private void createVertices ()
    {
        v1 = graph.createVertex();
        v2 = graph.createVertex();
        v3 = graph.createVertex();
    }

    //~ Inner Classes ----------------------------------------------------------

    static class MyDigraph
        extends BasicDigraph<MyDigraph, MyVertex>
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
        extends BasicVertex<MyDigraph, MyVertex>
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            return super.toString() + "}";
        }
    }
}
