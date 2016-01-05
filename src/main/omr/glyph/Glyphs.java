//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          G l y p h s                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.image.Table;

import omr.util.IdUtil;
import omr.util.Predicate;

import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code Glyphs} gathers static methods operating on a collection of glyphs.
 *
 * @author Hervé Bitteur
 */
public abstract class Glyphs
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Glyphs.class);

    /** To compare glyphs according to their id.
     * This comparator, which requires that all handled glyphs have an ID, can be used for a set.
     */
    public static final Comparator<Glyph> byId = new Comparator<Glyph>()
    {
        @Override
        public int compare (Glyph g1,
                            Glyph g2)
        {
            return IdUtil.compare(g1.getId(), g2.getId());
        }
    };

    /** To compare glyphs according to their left abscissa. */
    public static final Comparator<Glyph> byAbscissa = new Comparator<Glyph>()
    {
        @Override
        public int compare (Glyph g1,
                            Glyph g2)
        {
            if (g1 == g2) {
                return 0;
            }

            return Integer.compare(g1.getLeft(), g2.getLeft());
        }
    };

    /** To compare glyphs according to their left abscissa (then top ordinate, then id).
     * This comparator, which requires that all handled glyphs have an ID, can be used for a set.
     */
    public static final Comparator<Glyph> byFullAbscissa = new Comparator<Glyph>()
    {
        @Override
        public int compare (Glyph g1,
                            Glyph g2)
        {
            if (g1 == g2) {
                return 0;
            }

            // Abscissa
            int dx = g1.getLeft() - g2.getLeft();

            if (dx != 0) {
                return dx;
            }

            // Ordinate
            int dy = g1.getTop() - g2.getTop();

            if (dy != 0) {
                return dy;
            }

            // Finally, use id ...
            return IdUtil.compare(g1.getId(), g2.getId());
        }
    };

    /** To compare glyphs according to their top ordinate. */
    public static final Comparator<Glyph> byOrdinate = new Comparator<Glyph>()
    {
        @Override
        public int compare (Glyph g1,
                            Glyph g2)
        {
            if (g1 == g2) {
                return 0;
            }

            return Integer.compare(g1.getTop(), g2.getTop());
        }
    };

    /** To compare glyphs according to their decreasing bottom ordinate. */
    public static final Comparator<Glyph> byReverseBottom = new Comparator<Glyph>()
    {
        @Override
        public int compare (Glyph g1,
                            Glyph g2)
        {
            if (g1 == g2) {
                return 0;
            }

            return Integer.compare(g2.getTop() + g2.getHeight(), g1.getTop() + g1.getHeight());
        }
    };

    //~ Constructors -------------------------------------------------------------------------------
    // Class is not meant to be instantiated.
    private Glyphs ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // buildLinks //
    //------------//
    /**
     * Build the graph of acceptable links within the provided collection of glyphs.
     *
     * @param glyphs the provided glyphs
     * @param maxGap maximum acceptable gap between glyphs
     * @return the populated graph
     */
    public static SimpleGraph<Glyph, GlyphLink> buildLinks (Collection<Glyph> glyphs,
                                                            double maxGap)
    {
        final int gapInt = (int) Math.ceil(maxGap);
        final List<Glyph> sortedGlyphs = new ArrayList<Glyph>(glyphs);
        Collections.sort(sortedGlyphs, byAbscissa);

        /** Graph of glyph instances, linked by their distance. */
        SimpleGraph<Glyph, GlyphLink> graph = new SimpleGraph<Glyph, GlyphLink>(GlyphLink.class);

        // Populate graph with all glyphs as vertices
        for (Glyph glyph : sortedGlyphs) {
            graph.addVertex(glyph);
        }

        // Populate edges (glyph to glyph distances) when applicable
        for (int i = 0; i < sortedGlyphs.size(); i++) {
            final Glyph glyph = sortedGlyphs.get(i);
            final Rectangle fatBox = glyph.getBounds();
            fatBox.grow(gapInt, gapInt);

            final int xBreak = fatBox.x + fatBox.width; // Glyphs are sorted by abscissa
            GlyphDistances glyphDistances = null; // Glyph-centered distance table

            for (Glyph other : sortedGlyphs.subList(i + 1, sortedGlyphs.size())) {
                Rectangle otherBox = other.getBounds();

                // Rough filtering, using fat box intersection
                if (!fatBox.intersects(otherBox)) {
                    continue;
                } else if (otherBox.x > xBreak) {
                    break;
                }

                // We now need the glyph distance table, if not yet computed
                if (glyphDistances == null) {
                    glyphDistances = new GlyphDistances(glyph, fatBox);
                }

                // Precise distance from glyph to other
                double dist = glyphDistances.distanceTo(other);

                if (dist <= maxGap) {
                    graph.addEdge(glyph, other, new GlyphLink.Nearby(dist));
                }
            }
        }

        return graph;
    }

    //-----------------------//
    // containedActualGlyphs //
    //-----------------------//
    /**
     * Look up in a collection of weak glyph instances for <b>all</b> actual glyph
     * instances contained in a provided rectangle.
     *
     * @param collection the collection of weak glyph instances to be browsed
     * @param rect       the coordinates rectangle
     * @return the actual glyph instances found, which may be an empty list
     */
    public static Set<Glyph> containedActualGlyphs (Collection<? extends WeakGlyph> collection,
                                                    Rectangle rect)
    {
        Set<Glyph> set = new LinkedHashSet<Glyph>();

        for (WeakGlyph weak : collection) {
            final Glyph glyph = weak.get();

            if ((glyph != null) && rect.contains(glyph.getBounds())) {
                set.add(glyph);
            }
        }

        return set;
    }

    //-----------------//
    // containedGlyphs //
    //-----------------//
    /**
     * Look up in a collection of glyph instances for <b>all</b> glyph
     * instances contained in a provided rectangle.
     *
     * @param collection the collection of glyph instances to be browsed
     * @param rect       the coordinates rectangle
     * @return the glyph instances found, which may be an empty list
     */
    public static Set<Glyph> containedGlyphs (Collection<? extends Glyph> collection,
                                              Rectangle rect)
    {
        Set<Glyph> set = new LinkedHashSet<Glyph>();

        for (Glyph glyph : collection) {
            if (rect.contains(glyph.getBounds())) {
                set.add(glyph);
            }
        }

        return set;
    }

    //-----------------//
    // containedGlyphs //
    //-----------------//
    /**
     * Look up in a collection of glyph instances for <b>all</b> glyph
     * instances contained in a provided polygon.
     *
     * @param collection the collection of glyph instances to be browsed
     * @param polygon    the containing polygon
     * @return the glyph instances found, which may be an empty list
     */
    public static Set<Glyph> containedGlyphs (Collection<? extends Glyph> collection,
                                              Polygon polygon)
    {
        Set<Glyph> set = new LinkedHashSet<Glyph>();

        for (Glyph glyph : collection) {
            if (polygon.contains(glyph.getBounds())) {
                set.add(glyph);
            }
        }

        return set;
    }

    //-----------------//
    // containingGlyph //
    //-----------------//
    /**
     * Look up in a collection of glyph instances for the <b>first</b> glyph
     * instance which contains the provided point.
     *
     * @param collection the collection of glyph instances to be browsed
     * @param point      the provided point
     * @return the glyph found, or null
     */
    public static Glyph containingGlyph (Collection<? extends Glyph> collection,
                                         Point point)
    {
        for (Glyph glyph : collection) {
            if (glyph.contains(point)) {
                return glyph;
            }
        }

        return null;
    }

    //------------------//
    // containingGlyphs //
    //------------------//
    /**
     * Look up in a collection of glyph instances for <b>all</b> glyph
     * instances which contain the provided point.
     *
     * @param collection the collection of glyph instances to be browsed
     * @param point      the provided point
     * @return the glyph instances found, which may be empty
     */
    public static Set<Glyph> containingGlyphs (Collection<? extends Glyph> collection,
                                               Point point)
    {
        Set<Glyph> set = new LinkedHashSet<Glyph>();

        for (Glyph glyph : collection) {
            if (glyph.contains(point)) {
                set.add(glyph);
            }
        }

        return set;
    }

    //----------//
    // contains //
    //----------//
    /**
     * Check whether a collection of glyph instances contains at least
     * one glyph for which the provided predicate holds true.
     *
     * @param glyphs    the glyph collection to check
     * @param predicate the predicate to be used
     * @return true if there is at least one matching glyph
     */
    public static boolean contains (Collection<Glyph> glyphs,
                                    Predicate<Glyph> predicate)
    {
        return firstOf(glyphs, predicate) != null;
    }

    //------------//
    // containsId //
    //------------//
    public static boolean containsId (Collection<Glyph> glyphs,
                                      String id)
    {
        for (Glyph glyph : glyphs) {
            if (glyph.getId().equals(id)) {
                return true;
            }
        }

        return false;
    }

    //---------//
    // firstOf //
    //---------//
    /**
     * Report the first glyph, if any, for which the provided predicate holds true.
     *
     * @param glyphs    the glyph collection to check
     * @param predicate the glyph predicate
     * @return the first matching glyph found if any, null otherwise
     */
    public static Glyph firstOf (Collection<Glyph> glyphs,
                                 Predicate<Glyph> predicate)
    {
        for (Glyph glyph : glyphs) {
            if (predicate.check(glyph)) {
                return glyph;
            }
        }

        return null;
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Return the bounding box of a collection of glyph instances.
     *
     * @param glyphs the provided collection of glyph instances
     * @return the bounding contour
     */
    public static Rectangle getBounds (Collection<? extends Glyph> glyphs)
    {
        Rectangle box = null;

        for (Glyph glyph : glyphs) {
            if (box == null) {
                box = new Rectangle(glyph.getBounds());
            } else {
                box.add(glyph.getBounds());
            }
        }

        return box;
    }

    //-----//
    // ids //
    //-----//
    /**
     * Build a string with just the IDs of the glyph collection,
     * introduced by the provided label.
     *
     * @param label  the string that introduces the list of IDs
     * @param glyphs the collection of glyph instances
     * @return the string built
     */
    public static String ids (String label,
                              Collection<? extends Glyph> glyphs)
    {
        if (glyphs == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(label).append("[");

        for (Glyph glyph : glyphs) {
            sb.append("#").append(glyph.getId());
        }

        sb.append("]");

        return sb.toString();
    }

    //-----//
    // ids //
    //-----//
    /**
     * Build a string with just the IDs of the glyph array, introduced
     * by the provided label.
     *
     * @param label  the string that introduces the list of IDs
     * @param glyphs the array of glyph instances
     * @return the string built
     */
    public static String ids (String label,
                              Glyph... glyphs)
    {
        return ids(label, Arrays.asList(glyphs));
    }

    //-----//
    // ids //
    //-----//
    /**
     * Build a string with just the IDs of the glyph collection,
     * introduced by the label "glyphs".
     *
     * @param glyphs the collection of glyph instances
     * @return the string built
     */
    public static String ids (Collection<? extends Glyph> glyphs)
    {
        return ids("glyphs", glyphs);
    }

    //-----//
    // ids //
    //-----//
    /**
     * Build a string with just the IDs of the glyph array, introduced
     * by the label "glyphs".
     *
     * @param glyphs the array of glyph instances
     * @return the string built
     */
    public static String ids (Glyph... glyphs)
    {
        return ids("glyphs", glyphs);
    }

    //-----------//
    // intersect //
    //-----------//
    /**
     * Report whether the two provided glyphs intersect.
     *
     * @param one a glyph
     * @param two another glyph
     * @param fat true for touch detection
     * @return true if overlap, false otherwise
     */
    public static boolean intersect (Glyph one,
                                     Glyph two,
                                     boolean fat)
    {
        // Very rough test
        final Rectangle oneBox = one.getBounds();
        final Rectangle twoBox = two.getBounds();

        if (fat) {
            oneBox.grow(1, 1);
            twoBox.grow(1, 1);
        }

        Rectangle clip = twoBox.intersection(oneBox);

        if (clip.isEmpty()) {
            return false;
        }

        // More precise test
        Table.UnsignedByte table = new Table.UnsignedByte(clip.width, clip.height);
        one.fillTable(table, clip.getLocation(), fat);

        return two.intersects(table, clip.getLocation());
    }

    //-------------------------//
    // intersectedActualGlyphs //
    //-------------------------//
    /**
     * Look up in a collection of weak glyph instances for <b>all</b> actual glyph
     * instances intersected by a provided rectangle.
     *
     * @param collection the collection of weak glyph instances to be browsed
     * @param rect       the coordinates rectangle
     * @return the actual glyph instances found, which may be an empty list
     */
    public static Set<Glyph> intersectedActualGlyphs (Collection<? extends WeakGlyph> collection,
                                                      Rectangle rect)
    {
        Set<Glyph> set = new LinkedHashSet<Glyph>();

        for (WeakGlyph weak : collection) {
            final Glyph glyph = weak.get();

            if ((glyph != null) && rect.intersects(glyph.getBounds())) {
                set.add(glyph);
            }
        }

        return set;
    }

    //-------------------//
    // intersectedGlyphs //
    //-------------------//
    /**
     * Look up in a collection of glyph instances for <b>all</b> glyph
     * instances intersected by a provided rectangle.
     *
     * @param collection the collection of glyph instances to be browsed
     * @param rect       the coordinates rectangle
     * @return the glyph instances found, which may be an empty list
     */
    public static Set<Glyph> intersectedGlyphs (Collection<? extends Glyph> collection,
                                                Rectangle rect)
    {
        Set<Glyph> set = new LinkedHashSet<Glyph>();

        for (Glyph glyph : collection) {
            if (rect.intersects(glyph.getBounds())) {
                set.add(glyph);
            }
        }

        return set;
    }

    //-------------------//
    // intersectedGlyphs //
    //-------------------//
    /**
     * Look up in a collection of glyph instances for <b>all</b> glyph
     * instances intersected by a provided area.
     *
     * @param collection the collection of glyph instances to be browsed
     * @param area       the intersecting area
     * @return the glyph instances found, which may be an empty list
     */
    public static Set<Glyph> intersectedGlyphs (Collection<? extends Glyph> collection,
                                                Area area)
    {
        Set<Glyph> set = new LinkedHashSet<Glyph>();

        for (Glyph glyph : collection) {
            if (area.intersects(glyph.getBounds())) {
                set.add(glyph);
            }
        }

        return set;
    }

    //--------------//
    // lookupGlyphs //
    //--------------//
    /**
     * Look up in a collection of glyph instances for <b>all</b> glyph
     * instances compatible with a provided predicate.
     *
     * @param collection the collection of glyph instances to be browsed
     * @param predicate  the predicate to apply to each candidate (a null
     *                   predicate will accept all candidates)
     * @return the glyph instances found, which may be an empty list
     */
    public static Set<Glyph> lookupGlyphs (Collection<? extends Glyph> collection,
                                           Predicate<Glyph> predicate)
    {
        Set<Glyph> set = new LinkedHashSet<Glyph>();

        for (Glyph glyph : collection) {
            if ((predicate == null) || predicate.check(glyph)) {
                set.add(glyph);
            }
        }

        return set;
    }

    //-------//
    // purge //
    //-------//
    /**
     * Purge a collection of glyph instances of those which match the given predicate.
     *
     * @param glyphs    the glyph collection to purge
     * @param predicate the predicate to detect glyph instances to purge
     */
    public static void purge (Collection<Glyph> glyphs,
                              Predicate<Glyph> predicate)
    {
        if (predicate == null) {
            return;
        }

        for (Iterator<Glyph> it = glyphs.iterator(); it.hasNext();) {
            Glyph glyph = it.next();

            if (predicate.check(glyph)) {
                it.remove();
            }
        }
    }

    //----------//
    // weightOf //
    //----------//
    /**
     * Report the total weight of the collection
     *
     * @param glyphs the provided collection of glyph instances
     * @return the cumulated weight
     */
    public static int weightOf (Collection<Glyph> glyphs)
    {
        int total = 0;

        if (glyphs != null) {
            for (Glyph glyph : glyphs) {
                total += glyph.getWeight();
            }
        }

        return total;
    }
}
//    //-------------//
//    // LinkAdapter //
//    //-------------//
//    /**
//     * Adapter used by buildLinks() method, to provide the maximum acceptable distance.
//     */
//    public static interface LinkAdapter
//    {
//        //~ Methods --------------------------------------------------------------------------------
//
//        /**
//         * Report the maximum acceptable distance around the provided glyph.
//         *
//         * @param glyph the glyph at hand
//         * @return the maximum distance to create a link
//         */
//        double getAcceptableDistance (Glyph glyph);
//    }
//
//    //--------------//
//    // asciiDrawing //
//    //--------------//
//    /**
//     * Report a basic representation of the glyph, using ascii chars.
//     *
//     * @param glyph the glyph at hand
//     * @return an ASCII representation
//     */
//    public static String asciiDrawing (Glyph glyph)
//    {
//        StringBuilder sb = new StringBuilder();
//
//        sb.append(String.format("%s%n", glyph));
//
//        // Determine the bounding box
//        Rectangle box = glyph.getBounds();
//
//        if (box == null) {
//            return sb.toString(); // Safer
//        }
//
//        // Allocate the drawing table
//        char[][] table = BasicSection.allocateTable(box);
//
//        // Register each section in turn
//        for (Section section : glyph.getMembers()) {
//            section.fillTable(table, box);
//        }
//
//        // Draw the result
//        sb.append(BasicSection.drawingOfTable(table, box));
//
//        return sb.toString();
//    }
