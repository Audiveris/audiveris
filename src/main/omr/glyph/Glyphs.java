//----------------------------------------------------------------------------//
//                                                                            //
//                                G l y p h s                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.Main;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import java.awt.Point;
import java.util.*;

/**
 * Class <code>Glyphs</code> is just a collection of static convenient methods,
 * providing features related to a collection of glyphs.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Glyphs
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Glyphs.class);

    /** For comparing glyphs according to their decreasing weight */
    public static final Comparator<Glyph> reverseWeightComparator = new Comparator<Glyph>() {
        public int compare (Glyph o1,
                            Glyph o2)
        {
            return o2.getWeight() - o1.getWeight();
        }
    };

    /** For comparing glyphs according to their id */
    public static final Comparator<Glyph> idComparator = new Comparator<Glyph>() {
        public int compare (Glyph o1,
                            Glyph o2)
        {
            return o1.getId() - o2.getId();
        }
    };

    /** For comparing glyphs according to their abscissa, then ordinate, then id */
    public static final Comparator<Glyph> globalComparator = new Comparator<Glyph>() {
        public int compare (Glyph o1,
                            Glyph o2)
        {
            if (o1 == o2) {
                return 0;
            }

            if (o1.getContourBox() == null) {
                Main.dumping.dump(o1);

                logger.warning(
                    "Glyph w/ no contourbox " + o1,
                    new Throwable("Bingo"));
            }

            Point ref = o1.getContourBox()
                          .getLocation();
            Point otherRef = o2.getContourBox()
                               .getLocation();

            // Are x values different?
            int dx = ref.x - otherRef.x;

            if (dx != 0) {
                return dx;
            }

            // Vertically aligned, so use ordinates
            int dy = ref.y - otherRef.y;

            if (dy != 0) {
                return dy;
            }

            // Finally, use id ...
            return o1.getId() - o2.getId();
        }
    };


    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getContourBox //
    //---------------//
    /**
     * Return the display bounding box of a collection of glyphs.
     *
     * @param glyphs the provided collection of glyphs
     * @return the bounding contour
     */
    public static PixelRectangle getContourBox (Collection<Glyph> glyphs)
    {
        PixelRectangle box = null;

        for (Glyph glyph : glyphs) {
            if (box == null) {
                box = new PixelRectangle(glyph.getContourBox());
            } else {
                box.add(glyph.getContourBox());
            }
        }

        return box;
    }

    //---------------------//
    // containsManualShape //
    //---------------------//
    /**
     * Check whether a collection of glyphs contains at least one glyph with a
     * manually assigned shape
     * @param glyphs the glyph collection to check
     * @return true if there is at least one manually assigned shape
     */
    public static boolean containsManualShape (Collection<Glyph> glyphs)
    {
        for (Glyph glyph : glyphs) {
            if (glyph.isManualShape()) {
                return true;
            }
        }

        return false;
    }

    //----------//
    // glyphsOf //
    //----------//
    /**
     * Report the set of glyphs that are pointed back by the provided collection
     * of sections
     * @param sections the provided sections
     * @return the set of active containing glyphs
     */
    public static Set<Glyph> glyphsOf (Collection<GlyphSection> sections)
    {
        Set<Glyph> glyphs = new LinkedHashSet<Glyph>();

        for (GlyphSection section : sections) {
            Glyph glyph = section.getGlyph();

            if (glyph != null) {
                glyphs.add(glyph);
            }
        }

        return glyphs;
    }

    //-------------------//
    // purgeManualShapes //
    //-------------------//
    /**
     * Purge a collection of glyphs of those which exhibit a manually assigned
     * shape
     * @param glyphs the glyph collection to purge
     */
    public static void purgeManualShapes (Collection<Glyph> glyphs)
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
     * Report the set of sections contained by the provided collection of glyphs
     * @param glyphs the provided glyphs
     * @return the set of all member sections
     */
    public static Set<GlyphSection> sectionsOf (Collection<Glyph> glyphs)
    {
        Set<GlyphSection> sections = new TreeSet<GlyphSection>();

        for (Glyph glyph : glyphs) {
            sections.addAll(glyph.getMembers());
        }

        return sections;
    }

    //----------//
    // shapesOf //
    //----------//
    /**
     * Report the set of shapes that appear in at least one of the provided
     * glyphs
     * @param glyphs the provided collection of glyphs
     * @return the shapes assigned among these glyphs
     */
    public static Set<Shape> shapesOf (Collection<Glyph> glyphs)
    {
        Set<Shape> shapes = new HashSet<Shape>();

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
     * Build a mutable set with the provided glyphs
     * @param glyphs the provided glyphs
     * @return a mutable sorted set composed of these glyphs
     */
    public static SortedSet<Glyph> sortedSet (Glyph... glyphs)
    {
        SortedSet<Glyph> set = new TreeSet<Glyph>(Glyphs.globalComparator);

        for (Glyph glyph : glyphs) {
            set.add(glyph);
        }

        return set;
    }

    //-----------//
    // sortedSet //
    //-----------//
    /**
     * Build a mutable set with the provided glyphs
     * @param glyphs the provided glyphs
     * @return a mutable sorted set composed of these glyphs
     */
    public static SortedSet<Glyph> sortedSet (Collection<Glyph> glyphs)
    {
        SortedSet<Glyph> set = new TreeSet<Glyph>(Glyphs.globalComparator);
        set.addAll(glyphs);

        return set;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Build a string with just the ids of the glyph collection, introduced by
     * the provided label
     *
     * @param label the string that introduces the list of IDs
     * @param glyphs the collection of glyphs
     * @return the string built
     */
    public static String toString (String                     label,
                                   Collection<?extends Glyph> glyphs)
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
     * Build a string with just the ids of the glyph array, introduced by the
     * provided label
     *
     * @param label the string that introduces the list of IDs
     * @param glyphs the array of glyphs
     * @return the string built
     */
    public static String toString (String   label,
                                   Glyph... glyphs)
    {
        return toString(label, Arrays.asList(glyphs));
    }

    //----------//
    // toString //
    //----------//
    /**
     * Build a string with just the ids of the glyph collection, introduced by
     * the label "glyphs"
     *
     * @param glyphs the collection of glyphs
     * @return the string built
     */
    public static String toString (Collection<?extends Glyph> glyphs)
    {
        return toString("glyphs", glyphs);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Build a string with just the ids of the glyph array, introduced by the
     * label "glyphs"
     *
     * @param glyphs the array of glyphs
     * @return the string built
     */
    public static String toString (Glyph... glyphs)
    {
        return toString("glyphs", glyphs);
    }
}
