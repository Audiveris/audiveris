//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h C h a i n T e s t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphComposition.Linking;

import omr.lag.BasicLag;
import omr.lag.Lag;
import omr.lag.Section;

import omr.run.Orientation;
import omr.run.Run;
import omr.run.RunsTable;

import omr.score.common.PixelRectangle;

import omr.util.BaseTestCase;
import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author Etiolles
 */
public class GlyphChainTest
    extends BaseTestCase
{
    //~ Instance fields --------------------------------------------------------

    Lag           vLag;
    RunsTable     vTable;
    private Glyph first;
    private Glyph second;
    private Glyph third;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GlyphChainTest object.
     */
    public GlyphChainTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Test of addAllItems method, of class BasicGlyphChain.
     */
    @Test
    public void testAddAllItems ()
    {
        Collection<?extends Glyph> glyphs = Arrays.asList(first, second);
        GlyphChain                 instance = createEmptyInstance();
        instance.addAllItems(glyphs);
        assertEquals(2, instance.getItemCount());
    }

    /**
     * Test of addAllItems method, of class BasicGlyphChain.
     */
    @Test
    public void testAddAllItemsNull ()
    {
        try {
            GlyphChain instance = createInstance();
            instance.addAllItems(null);
            fail(
                "IllegalArgumentException should be raised" +
                " when attempting to add a null collection of items");
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

//    /**
//     * Test of direct section addition, of class BasicGlyphChain.
//     */
//    @Test
//    public void testAddDirectSection ()
//    {
//        GlyphChain instance = createInstance();
//        instance.addItem(second);
//
//        Run r4 = new Run(25, 10, 127);
//        vTable.getSequence(70)
//              .add(r4);
//
//        Section s4 = vLag.createSection(40, r4);
//        s4.append(r4);
//
//        int            weightBefore = instance.getWeight();
//        PixelRectangle boundsBefore = instance.getBounds();
//        second.addSection(s4, Linking.LINK_BACK);
//
//        int            weightAfter = instance.getWeight();
//        PixelRectangle boundsAfter = instance.getBounds();
//        assertFalse("Unmodified weight", weightAfter == weightBefore);
//        assertFalse("Unmodified bounds", boundsAfter.equals(boundsBefore));
//
//        int expectedWeight = weightBefore + s4.getWeight();
//        assertEquals(expectedWeight, weightAfter);
//
//        PixelRectangle expectedBounds = first.getBounds()
//                                             .union(second.getBounds());
//        assertEquals(expectedBounds, boundsAfter);
//    }

    /**
     * Test of addItem method, of class BasicGlyphChain.
     */
    @Test
    public void testAddItem ()
    {
        Glyph      glyph = second;
        GlyphChain instance = createInstance();
        boolean    expResult = true;
        boolean    result = instance.addItem(glyph);
        assertEquals(expResult, result);
        assertEquals(2, instance.getItemCount());
    }

    /**
     * Test of addItem method, of class BasicGlyphChain.
     */
    @Test
    public void testAddItemEmpty ()
    {
        GlyphChain instance = createEmptyInstance();
        boolean    expResult = true;
        boolean    result = instance.addItem(first);
        assertEquals(expResult, result);
    }

    /**
     * Test of addItem method, of class BasicGlyphChain.
     */
    @Test
    public void testAddItemNull ()
    {
        try {
            GlyphChain instance = createInstance();
            instance.addItem(null);
            fail(
                "IllegalArgumentException should be raised" +
                " when attempting to add a null item");
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    /**
     * Test of addItem method, of class BasicGlyphChain.
     */
    @Test
    public void testAddSharedItem ()
    {
        GlyphChain instance = createInstance(); // First
        GlyphChain otherInstance = createOtherInstance(); // Third
        assertFalse(instance.getItems().contains(third));
        assertTrue(otherInstance.getItems().contains(third));

        instance.addItem(third); // Third is added to instance

        assertEquals(2, instance.getItemCount());
        assertEquals(1, otherInstance.getItemCount());
        
        assertFalse(otherInstance.getItems().contains(first));
        assertTrue(otherInstance.getItems().contains(third));
        
        assertTrue(instance.getItems().contains(first));
        assertTrue(instance.getItems().contains(third));
    }

    /**
     * Test of getFirstItem method, of class BasicGlyphChain.
     */
    @Test
    public void testGetFirstItem ()
    {
        GlyphChain instance = createInstance();
        Glyph      expResult = first;
        Glyph      result = instance.getFirstItem();
        assertEquals(expResult, result);
    }

    /**
     * Test of getItemAfter method, of class BasicGlyphChain.
     */
    @Test
    public void testGetItemAfter ()
    {
        Glyph      start = first;
        GlyphChain instance = createInstance();
        instance.addItem(second);

        Glyph expResult = second;
        Glyph result = instance.getItemAfter(start);
        assertEquals(expResult, result);
    }

    /**
     * Test of getItemAfter method, of class BasicGlyphChain.
     */
    @Test
    public void testGetItemAfterNull ()
    {
        GlyphChain instance = createInstance();
        Glyph      expResult = first;
        Glyph      result = instance.getItemAfter(null);
        assertEquals(expResult, result);
    }

    /**
     * Test of getItemAfter method, of class BasicGlyphChain.
     */
    @Test
    public void testGetItemAfterSeed ()
    {
        GlyphChain instance = createInstance();
        Glyph      expResult = null;
        Glyph      result = instance.getItemAfter(first);
        assertEquals(expResult, result);
    }

    /**
     * Test of getItemBefore method, of class BasicGlyphChain.
     */
    @Test
    public void testGetItemBefore ()
    {
        Glyph      stop = second;
        GlyphChain instance = createInstance();
        instance.addItem(second);

        Glyph expResult = first;
        Glyph result = instance.getItemBefore(stop);
        assertEquals(expResult, result);
    }

    /**
     * Test of getItemBefore method, of class BasicGlyphChain.
     */
    @Test
    public void testGetItemBeforeNull ()
    {
        GlyphChain instance = createInstance();
        Glyph      expResult = first;
        Glyph      result = instance.getItemBefore(null);
        assertEquals(expResult, result);
    }

    /**
     * Test of getItemCount method, of class BasicGlyphChain.
     */
    @Test
    public void testGetItemCount ()
    {
        GlyphChain instance = createInstance();
        int        expResult = 1;
        int        result = instance.getItemCount();
        assertEquals(expResult, result);
    }

    /**
     * Test of getItemCount method, of class BasicGlyphChain.
     */
    @Test
    public void testGetItemCountEmpty ()
    {
        GlyphChain instance = createEmptyInstance();
        int        expResult = 0;
        int        result = instance.getItemCount();
        assertEquals(expResult, result);
    }

    /**
     * Test of getItems method, of class BasicGlyphChain.
     */
    @Test
    public void testGetItems ()
    {
        GlyphChain instance = createInstance();
        instance.addItem(second);

        SortedSet expResult = Glyphs.sortedSet(first, second);
        SortedSet result = instance.getItems();
        assertEquals(expResult, result);
    }

    /**
     * Test of getItems method, of class BasicGlyphChain.
     */
    @Test
    public void testGetItemsEmpty ()
    {
        GlyphChain instance = createEmptyInstance();
        SortedSet  result = instance.getItems();
        assertEquals(0, result.size());
    }

    /**
     * Test of getLastItem method, of class GlyphChain.
     */
    @Test
    public void testGetLastItem ()
    {
        GlyphChain instance = createInstance();
        instance.addItem(second);

        Glyph expResult = second;
        Glyph result = instance.getLastItem();
        assertEquals(expResult, result);
    }

    /**
     * Test of getMembers method, of class BasicGlyphChain.
     */
    @Test
    public void testGetMembers ()
    {
        BasicGlyphChain instance = (BasicGlyphChain) createInstance();
        SortedSet<Section>  expResult = first.getMembers();
        SortedSet<Section>  result = instance.getMembers();
        assertEquals(expResult, result);
    }

    /**
     * Test of getMembers method, of class BasicGlyphChain.
     */
    @Test
    public void testGetMembers2 ()
    {
        BasicGlyphChain instance = (BasicGlyphChain) createInstance();
        instance.addItem(second);

        SortedSet expResult = new TreeSet<>();
        expResult.addAll(first.getMembers());
        expResult.addAll(second.getMembers());

        SortedSet result = instance.getMembers();
        assertEquals(expResult, result);
    }

    /**
     * Test of removeItem method, of class BasicGlyphChain.
     */
    @Test
    public void testRemoveItem ()
    {
        GlyphChain instance = createInstance();
        instance.addItem(second);

        boolean expResult = true;
        boolean result = instance.removeItem(first);
        assertEquals(expResult, result);
    }

    /**
     * Test of removeItem method, of class BasicGlyphChain.
     */
    @Test
    public void testRemoveNullItem ()
    {
        try {
            GlyphChain instance = createInstance();
            instance.removeItem(null);
            fail(
                "IllegalArgumentException should be raised" +
                " when attempting to remove a null item");
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    /**
     * Test of removeItem method, of class BasicGlyphChain.
     */
    @Test
    public void testRemoveSingleItem ()
    {
        GlyphChain instance = createInstance();
        instance.removeItem(first);
        assertEquals(0, instance.getItemCount());
    }

    /**
     * Test of setItems method, of class BasicGlyphChain.
     */
    @Test
    public void testSetItems ()
    {
        Collection<?extends Glyph> items = Arrays.asList(first, second);
        GlyphChain                 instance = createInstance();
        instance.setItems(items);
        assertEquals(2, instance.getItemCount());
    }

    /**
     * Test of setItems method, of class BasicGlyphChain.
     */
    @Test
    public void testSetNoItems ()
    {
        Collection<?extends Glyph> items = Collections.emptyList();
        GlyphChain                 instance = createInstance();
        instance.setItems(items);
        assertEquals(0, instance.getItemCount());
    }

    /**
     * Test of setItems method, of class BasicGlyphChain.
     */
    @Test
    public void testSetNullItems ()
    {
        try {
            Collection<?extends Glyph> items = null;
            GlyphChain                 instance = createInstance();
            instance.setItems(items);
            fail(
                "IllegalArgumentException should be raised" +
                " when attempting to set a null collection of items");
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    /**
     * Test of setItems method, of class BasicGlyphChain.
     */
    @Test
    public void testSetSharedItems ()
    {
        GlyphChain instance = createInstance(); // first
        instance.addItem(second);                // + second

        GlyphChain otherInstance = createOtherInstance(); // third => first, second

        assertTrue(instance.getItems().contains(first));
        assertTrue(instance.getItems().contains(second));
        assertTrue(otherInstance.getItems().contains(third));

        otherInstance.setItems(Arrays.asList(first, second));

        assertEquals(2, instance.getItemCount());
        assertEquals(2, otherInstance.getItemCount());

        assertTrue(instance.getItems().contains(first));
        assertTrue(instance.getItems().contains(second));
        
        assertTrue(otherInstance.getItems().contains(first));
        assertTrue(otherInstance.getItems().contains(second));

        assertFalse(instance.getItems().contains(third));        
        assertFalse(otherInstance.getItems().contains(third));
    }

    //-------//
    // setUp //
    //-------//
    @Override
    protected void setUp ()
    {
        vLag = new BasicLag("My Vertical Lag", Orientation.VERTICAL);
        vTable = new RunsTable(
            "Vert Runs",
            Orientation.VERTICAL,
            new Dimension(100, 200)); // Absolute
        vLag.setRuns(vTable);

        Run r1 = new Run(10, 5, 127);
        vTable.getSequence(50)
              .add(r1);

        Section s1 = vLag.createSection(20, r1);
        s1.append(r1);

        first = new BasicGlyph(20);
        first.setId(1);
        first.addSection(s1, Linking.LINK_BACK);

        Run r2 = new Run(20, 30, 127);
        vTable.getSequence(60)
              .add(r2);

        Section s2 = vLag.createSection(30, r2);
        s2.append(r2);
        second = new BasicGlyph(30);
        second.setId(2);
        second.addSection(s2, Linking.LINK_BACK);

        Run r3 = new Run(22, 20, 127);
        vTable.getSequence(62)
              .add(r3);

        Section s3 = vLag.createSection(35, r3);
        s3.append(r3);
        third = new BasicGlyph(30);
        third.setId(3);
        third.addSection(s3, Linking.LINK_BACK);
    }

    //---------------------//
    // createEmptyInstance //
    //---------------------//
    private GlyphChain createEmptyInstance ()
    {
        List<Glyph> empty = Collections.emptyList();
        BasicGlyphChain gc = new BasicGlyphChain(empty);

        return gc;
    }

    //----------------//
    // createInstance //
    //----------------//
    private GlyphChain createInstance ()
    {
        BasicGlyphChain gc = new BasicGlyphChain(Arrays.asList(first));

        return gc;
    }

    //---------------------//
    // createOtherInstance //
    //---------------------//
    private GlyphChain createOtherInstance ()
    {
        BasicGlyphChain gc = new BasicGlyphChain(Arrays.asList(third));

        return gc;
    }
}
