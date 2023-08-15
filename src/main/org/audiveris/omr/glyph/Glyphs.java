//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          G l y p h s                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.glyph;

import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.Table;

import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Class <code>Glyphs</code> gathers static methods operating on a collection of glyphs.
 *
 * @author Hervé Bitteur
 */
public abstract class Glyphs
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Glyphs.class);

    /**
     * To compare glyphs according to their id.
     * This comparator, which requires that all handled glyphs have an ID, can be used for a set.
     */
    public static final Comparator<Glyph> byId = (g1,
                                                  g2) -> Integer.compare(g1.getId(), g2.getId());

    /** To compare glyphs according to their left abscissa. */
    public static final Comparator<Glyph> byAbscissa = (g1,
                                                        g2) -> (g1 == g2) ? 0
                                                                : Integer.compare(
                                                                        g1.getLeft(),
                                                                        g2.getLeft());

    /**
     * To compare glyphs according to their left abscissa (then top ordinate, then id).
     * This comparator, which requires that all handled glyphs have an ID, can be used for a
     * set.
     */
    public static final Comparator<Glyph> byFullAbscissa = (g1,
                                                            g2) ->
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
        return Integer.compare(g1.getId(), g2.getId());
    };

    /** To compare glyphs according to their top ordinate. */
    public static final Comparator<Glyph> byOrdinate = (g1,
                                                        g2) -> (g1 == g2) ? 0
                                                                : Integer.compare(
                                                                        g1.getTop(),
                                                                        g2.getTop());

    /**
     * To compare glyphs according to their top ordinate (then left abscissa, then id).
     * This comparator, which requires that all handled glyphs have an ID, can be used for a set.
     */
    public static final Comparator<Glyph> byFullOrdinate = (g1,
                                                            g2) ->
    {
        if (g1 == g2) {
            return 0;
        }

        // Ordinate
        int dy = g1.getTop() - g2.getTop();

        if (dy != 0) {
            return dy;
        }

        // Abscissa
        int dx = g1.getLeft() - g2.getLeft();

        if (dx != 0) {
            return dx;
        }

        // Finally, use id ...
        return Integer.compare(g1.getId(), g2.getId());
    };

    /** To compare glyphs according to their decreasing bottom ordinate. */
    public static final Comparator<Glyph> byReverseBottom = (g1,
                                                             g2) -> (g1 == g2) ? 0
                                                                     : Integer.compare(
                                                                             g2.getTop() + g2
                                                                                     .getHeight(),
                                                                             g1.getTop() + g1
                                                                                     .getHeight());

    /** To compare glyphs according to their (increasing) weight. */
    public static final Comparator<Glyph> byWeight = (g1,
                                                      g2) -> (g1 == g2) ? 0
                                                              : Integer.compare(
                                                                      g1.getWeight(),
                                                                      g2.getWeight());

    /** To compare glyphs according to their (increasing) width. */
    public static final Comparator<Glyph> byWidth = (g1,
                                                     g2) -> (g1 == g2) ? 0
                                                             : Integer.compare(
                                                                     g1.getWidth(),
                                                                     g2.getWidth());

    /** To compare glyphs according to their decreasing weight. */
    public static final Comparator<Glyph> byReverseWeight = (g1,
                                                             g2) -> (g1 == g2) ? 0
                                                                     : Integer.compare(
                                                                             g2.getWeight(),
                                                                             g1.getWeight());

    //~ Constructors -------------------------------------------------------------------------------

    // Class is not meant to be instantiated.
    private Glyphs ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

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
        final List<Glyph> sortedGlyphs = new ArrayList<>(glyphs);
        Collections.sort(sortedGlyphs, byAbscissa);

        /** Graph of glyph instances, linked by their distance. */
        SimpleGraph<Glyph, GlyphLink> graph = new SimpleGraph<>(GlyphLink.class);

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

                if ((dist <= maxGap) && !glyph.equals(other)) {
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
        Set<Glyph> set = new LinkedHashSet<>();

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
     * instances contained in a provided polygon.
     *
     * @param collection the collection of glyph instances to be browsed
     * @param polygon    the containing polygon
     * @return the glyph instances found, which may be an empty list
     */
    public static Set<Glyph> containedGlyphs (Collection<? extends Glyph> collection,
                                              Polygon polygon)
    {
        Set<Glyph> set = new LinkedHashSet<>();

        for (Glyph glyph : collection) {
            if (polygon.contains(glyph.getBounds())) {
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
        Set<Glyph> set = new LinkedHashSet<>();

        for (Glyph glyph : collection) {
            if (rect.contains(glyph.getBounds())) {
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
        Set<Glyph> set = new LinkedHashSet<>();

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
    /**
     * Tell whether the provided ID appears in the glyph collection.
     *
     * @param glyphs glyph collection
     * @param id     provided ID
     * @return true if so
     */
    public static boolean containsId (Collection<Glyph> glyphs,
                                      int id)
    {
        for (Glyph glyph : glyphs) {
            if (glyph.getId() == id) {
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
            if (predicate.test(glyph)) {
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
     * introduced by the label "glyphs".
     *
     * @param glyphs the collection of glyph instances
     * @return the string built
     */
    public static String ids (Collection<? extends Glyph> glyphs)
    {
        return Entities.ids("glyphs", glyphs);
    }

    //-----//
    // ids //
    //-----//
    /**
     * Report a string of glyph IDs.
     *
     * @param label  a string to introduce the list of IDs
     * @param glyphs collection of glyphs
     * @return string of IDs
     */
    public static String ids (String label,
                              Collection<? extends Glyph> glyphs)
    {
        return Entities.ids(label, glyphs);
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
        Set<Glyph> set = new LinkedHashSet<>();

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
     * instances intersected by a provided area.
     *
     * @param collection the collection of glyph instances to be browsed
     * @param area       the intersecting area
     * @return the glyph instances found, which may be an empty list
     */
    public static Set<Glyph> intersectedGlyphs (Collection<? extends Glyph> collection,
                                                Area area)
    {
        Set<Glyph> set = new LinkedHashSet<>();

        if (collection != null) {
            for (Glyph glyph : collection) {
                if (area.intersects(glyph.getBounds())) {
                    set.add(glyph);
                }
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
        Set<Glyph> set = new LinkedHashSet<>();

        for (Glyph glyph : collection) {
            if (rect.intersects(glyph.getBounds())) {
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
        Set<Glyph> set = new LinkedHashSet<>();

        for (Glyph glyph : collection) {
            if ((predicate == null) || predicate.test(glyph)) {
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

            if (predicate.test(glyph)) {
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
