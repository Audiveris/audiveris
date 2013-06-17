//----------------------------------------------------------------------------//
//                                                                            //
//                                G l y p h s                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.BasicAlignment;
import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.math.PointsCollector;

import omr.run.Orientation;

import omr.sheet.Scale;

import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code Glyphs} is a collection of static convenient methods,
 * providing features related to a collection of glyphs.
 *
 * @author Hervé Bitteur
 */
public class Glyphs
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Glyphs.class);

    /** Predicate to check for a manual shape */
    public static final Predicate<Glyph> manualPredicate = new Predicate<Glyph>()
    {
        @Override
        public boolean check (Glyph glyph)
        {
            return glyph.isManualShape();
        }
    };

    /** Predicate to check for a barline shape */
    public static final Predicate<Glyph> barPredicate = new Predicate<Glyph>()
    {
        @Override
        public boolean check (Glyph glyph)
        {
            return glyph.isBar();
        }
    };

    /** A immutable empty set of glyphs */
    public static final Set<Glyph> NO_GLYPHS = Collections.emptySet();

    //~ Methods ----------------------------------------------------------------
    //----------//
    // contains //
    //----------//
    /**
     * Check whether a collection of glyphs contains at least one glyph
     * for which the provided predicate holds true.
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

    //-----------------//
    // containsBarline //
    //-----------------//
    /**
     * Check whether the collection of glyphs contains at least one
     * barline.
     *
     * @param glyphs the collection to check
     * @return true if one or several glyphs are barlines components
     */
    public static boolean containsBarline (Collection<Glyph> glyphs)
    {
        return firstOf(glyphs, barPredicate) != null;
    }

    //------------//
    // containsId //
    //------------//
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

    //----------------//
    // containsManual //
    //----------------//
    /**
     * Check whether a collection of glyphs contains at least one glyph
     * with a manually assigned shape.
     *
     * @param glyphs the glyph collection to check
     * @return true if there is at least one manually assigned shape
     */
    public static boolean containsManual (Collection<Glyph> glyphs)
    {
        return contains(glyphs, manualPredicate);
    }

    //---------//
    // firstOf //
    //---------//
    /**
     * Report the first glyph, if any, for which the provided predicate
     * holds true.
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
     * Return the display bounding box of a collection of glyphs.
     *
     * @param glyphs the provided collection of glyphs
     * @return the bounding contour
     */
    public static Rectangle getBounds (Collection<Glyph> glyphs)
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

    //----------------------------//
    // getReverseLengthComparator //
    //----------------------------//
    /**
     * For comparing glyph instances on decreasing length.
     *
     * @param orientation the desired orientation reference
     * @return the comparator
     */
    public static Comparator<Glyph> getReverseLengthComparator (
            final Orientation orientation)
    {
        return new Comparator<Glyph>()
        {
            @Override
            public int compare (Glyph s1,
                                Glyph s2)
            {
                return s2.getLength(orientation)
                       - s1.getLength(orientation);
            }
        };
    }

    //----------------//
    // getThicknessAt //
    //----------------//
    /**
     * Report the resulting thickness of the collection of sticks at
     * the provided coordinate.
     *
     * @param coord       the desired coordinate
     * @param orientation the desired orientation reference
     * @param glyphs      glyphs contributing to the resulting thickness
     * @return the thickness measured, expressed in number of pixels.
     */
    public static double getThicknessAt (double coord,
                                         Orientation orientation,
                                         Glyph... glyphs)
    {
        return getThicknessAt(coord, orientation, null, glyphs);
    }

    //----------------//
    // getThicknessAt //
    //----------------//
    /**
     * Report the resulting thickness of the collection of sticks at
     * the provided coordinate.
     *
     * @param coord       the desired coordinate
     * @param orientation the desired orientation reference
     * @param section     section contributing to the resulting thickness
     * @param glyphs      glyphs contributing to the resulting thickness
     * @return the thickness measured, expressed in number of pixels.
     */
    public static double getThicknessAt (double coord,
                                         Orientation orientation,
                                         Section section,
                                         Glyph... glyphs)
    {
        if (glyphs.length == 0) {
            if (section == null) {
                return 0;
            } else {
                return section.getMeanThickness(orientation);
            }
        }

        // Retrieve global bounds
        Rectangle absBox = null;

        if (section != null) {
            absBox = section.getBounds();
        }

        for (Glyph g : glyphs) {
            if (absBox == null) {
                absBox = g.getBounds();
            } else {
                absBox.add(g.getBounds());
            }
        }

        Rectangle oBox = orientation.oriented(absBox);
        int intCoord = (int) Math.floor(coord);

        if ((intCoord < oBox.x) || (intCoord >= (oBox.x + oBox.width))) {
            return 0;
        }

        // Use a large-enough collector
        final Rectangle oRoi = new Rectangle(intCoord, oBox.y, 0, oBox.height);
        final Scale scale = new Scale(glyphs[0].getInterline());
        final int probeHalfWidth = scale.toPixels(
                BasicAlignment.getProbeWidth()) / 2;
        oRoi.grow(probeHalfWidth, 0);

        PointsCollector collector = new PointsCollector(
                orientation.absolute(oRoi));

        // Collect sections contribution
        for (Glyph g : glyphs) {
            for (Section sct : g.getMembers()) {
                sct.cumulate(collector);
            }
        }

        // Contributing section, if any
        if (section != null) {
            section.cumulate(collector);
        }

        // Case of no pixels found
        if (collector.getSize() == 0) {
            return 0;
        }

        // Analyze range of Y values
        int minVal = Integer.MAX_VALUE;
        int maxVal = Integer.MIN_VALUE;
        int[] vals = (orientation == Orientation.HORIZONTAL)
                ? collector.getYValues() : collector.getXValues();

        for (int i = 0, iBreak = collector.getSize(); i < iBreak; i++) {
            int val = vals[i];
            minVal = Math.min(minVal, val);
            maxVal = Math.max(maxVal, val);
        }

        return maxVal - minVal + 1;
    }

    //----------//
    // glyphsOf //
    //----------//
    /**
     * Report the set of glyphs that are pointed back by the provided
     * collection of sections.
     *
     * @param sections the provided sections
     * @return the set of active containing glyphs
     */
    public static Set<Glyph> glyphsOf (Collection<Section> sections)
    {
        Set<Glyph> glyphs = new LinkedHashSet<>();

        for (Section section : sections) {
            Glyph glyph = section.getGlyph();

            if (glyph != null) {
                glyphs.add(glyph);
            }
        }

        return glyphs;
    }

    //--------------//
    // lookupGlyphs //
    //--------------//
    /**
     * Look up in a collection of glyphs for <b>all</b> glyphs
     * contained in a provided rectangle.
     *
     * @param collection the collection of glyphs to be browsed
     * @param rect       the coordinates rectangle
     * @return the glyphs found, which may be an empty list
     */
    public static Set<Glyph> lookupGlyphs (
            Collection<? extends Glyph> collection,
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

    //--------------//
    // lookupGlyphs //
    //--------------//
    /**
     * Look up in a collection of glyphs for <b>all</b> glyphs
     * contained in a provided polygon.
     *
     * @param collection the collection of glyphs to be browsed
     * @param polygon    the containing polygon
     * @return the glyphs found, which may be an empty list
     */
    public static Set<Glyph> lookupGlyphs (
            Collection<? extends Glyph> collection,
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

    //--------------//
    // lookupGlyphs //
    //--------------//
    /**
     * Look up in a collection of glyphs for <b>all</b> glyphs
     * compatible with a provided predicate.
     *
     * @param collection the collection of glyphs to be browsed
     * @param predicate  the predicate to apply to each candidate (a null
     *                   predicate will accept all candidates)
     * @return the glyphs found, which may be an empty list
     */
    public static Set<Glyph> lookupGlyphs (
            Collection<? extends Glyph> collection,
            Predicate<Glyph> predicate)
    {
        Set<Glyph> set = new LinkedHashSet<>();

        for (Glyph glyph : collection) {
            if ((predicate == null) || predicate.check(glyph)) {
                set.add(glyph);
            }
        }

        return set;
    }

    //-------------------------//
    // lookupIntersectedGlyphs //
    //-------------------------//
    /**
     * Look up in a collection of glyphs for <b>all</b> glyphs
     * intersected by a provided rectangle.
     *
     * @param collection the collection of glyphs to be browsed
     * @param rect       the coordinates rectangle
     * @return the glyphs found, which may be an empty list
     */
    public static Set<Glyph> lookupIntersectedGlyphs (
            Collection<? extends Glyph> collection,
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

    //-------//
    // purge //
    //-------//
    /**
     * Purge a collection of glyphs of those which match the given
     * predicate.
     *
     * @param glyphs    the glyph collection to purge
     * @param predicate the predicate to detect glyphs to purge
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

    //--------------//
    // purgeManuals //
    //--------------//
    /**
     * Purge a collection of glyphs of those which exhibit a manually
     * assigned shape.
     *
     * @param glyphs the glyph collection to purge
     */
    public static void purgeManuals (Collection<Glyph> glyphs)
    {
        for (Iterator<Glyph> it = glyphs.iterator(); it.hasNext();) {
            Glyph glyph = it.next();

            if (glyph.isManualShape()) {
                it.remove();
            }
        }
    }

    //------------//
    // sectionsOf //
    //------------//
    /**
     * Report the set of sections contained by the provided collection
     * of glyphs.
     *
     * @param glyphs the provided glyphs
     * @return the set of all member sections
     */
    public static Set<Section> sectionsOf (Collection<Glyph> glyphs)
    {
        Set<Section> sections = new TreeSet<>();

        for (Glyph glyph : glyphs) {
            sections.addAll(glyph.getMembers());
        }

        return sections;
    }

    //----------//
    // shapesOf //
    //----------//
    /**
     * Report the set of shapes that appear in at least one of the
     * provided glyphs.
     *
     * @param glyphs the provided collection of glyphs
     * @return the shapes assigned among these glyphs
     */
    public static Set<Shape> shapesOf (Collection<Glyph> glyphs)
    {
        EnumSet<Shape> shapes = EnumSet.noneOf(Shape.class);

        if (glyphs != null) {
            for (Glyph glyph : glyphs) {
                if (glyph.getShape() != null) {
                    shapes.add(glyph.getShape());
                }
            }
        }

        return shapes;
    }

    //-----------//
    // sortedSet //
    //-----------//
    /**
     * Build a mutable set with the provided glyphs.
     *
     * @param glyphs the provided glyphs
     * @return a mutable sorted set composed of these glyphs
     */
    public static SortedSet<Glyph> sortedSet (Glyph... glyphs)
    {
        SortedSet<Glyph> set = new TreeSet<>(Glyph.byAbscissa);

        if (glyphs.length > 0) {
            set.addAll(Arrays.asList(glyphs));
        }

        return set;
    }

    //-----------//
    // sortedSet //
    //-----------//
    /**
     * Build a mutable set with the provided glyphs.
     *
     * @param glyphs the provided glyphs
     * @return a mutable sorted set composed of these glyphs
     */
    public static SortedSet<Glyph> sortedSet (Collection<Glyph> glyphs)
    {
        SortedSet<Glyph> set = new TreeSet<>(Glyph.byAbscissa);
        set.addAll(glyphs);

        return set;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Build a string with just the ids of the glyph collection,
     * introduced by the provided label.
     *
     * @param label  the string that introduces the list of IDs
     * @param glyphs the collection of glyphs
     * @return the string built
     */
    public static String toString (String label,
                                   Collection<? extends Glyph> glyphs)
    {
        if (glyphs == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(label)
                .append("[");

        for (Glyph glyph : glyphs) {
            sb.append("#")
                    .append(glyph.getId());
        }

        sb.append("]");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Build a string with just the ids of the glyph array, introduced
     * by the provided label.
     *
     * @param label  the string that introduces the list of IDs
     * @param glyphs the array of glyphs
     * @return the string built
     */
    public static String toString (String label,
                                   Glyph... glyphs)
    {
        return toString(label, Arrays.asList(glyphs));
    }

    //----------//
    // toString //
    //----------//
    /**
     * Build a string with just the ids of the glyph collection,
     * introduced by the label "glyphs".
     *
     * @param glyphs the collection of glyphs
     * @return the string built
     */
    public static String toString (Collection<? extends Glyph> glyphs)
    {
        return toString("glyphs", glyphs);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Build a string with just the ids of the glyph array, introduced
     * by the label "glyphs".
     *
     * @param glyphs the array of glyphs
     * @return the string built
     */
    public static String toString (Glyph... glyphs)
    {
        return toString("glyphs", glyphs);
    }

    private Glyphs ()
    {
    }
}
