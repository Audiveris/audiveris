//----------------------------------------------------------------------------//
//                                                                            //
//                                G l y p h s                                 //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import java.util.*;

/**
 * Class <code>Glyphs</code> handles features related to a collection of glyphs
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Glyphs
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Glyphs.class);

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
     * Check whether a collection of glyphs contains at least one manually
     * assigned shape
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

    //-------------------//
    // purgeManualShapes //
    //-------------------//
    /**
     * Purge a collection of glyphs from manually assigned shapes
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

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the glyph
     * collection, introduced by the provided label
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
     * Convenient method, to build a string with just the ids of the glyph
     * array, introduced by the provided label
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
     * Convenient method, to build a string with just the ids of the glyph
     * collection, introduced by the label "glyphs"
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
     * Convenient method, to build a string with just the ids of the glyph
     * array, introduced by the label "glyphs"
     *
     * @param glyphs the array of glyphs
     * @return the string built
     */
    public static String toString (Glyph... glyphs)
    {
        return toString("glyphs", glyphs);
    }
}
