//----------------------------------------------------------------------------//
//                                                                            //
//                              S e c t i o n s                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import java.util.*;

/**
 * Class <code>Sections</code> handles features related to a collection of
 * sections
 *
 * @author Hervé Bitteur
 */
public class Sections
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Sections.class);

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getContourBox //
    //---------------//
    /**
     * Return the display bounding box of a collection of sections.
     *
     * @param sections the provided collection of sections
     * @return the bounding contour
     */
    public static PixelRectangle getContourBox (Collection<Section> sections)
    {
        PixelRectangle box = null;

        for (Section section : sections) {
            if (box == null) {
                box = new PixelRectangle(section.getContourBox());
            } else {
                box.add(section.getContourBox());
            }
        }

        return box;
    }

    //----------//
    // contains //
    //----------//
    public static boolean contains (Collection<?extends Section> sections,
                                    Section                      section)
    {
        return sections.contains(section);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the section
     * collection, introduced by the provided label
     *
     * @param label the string that introduces the list of IDs
     * @param sections the collection of sections
     * @return the string built
     */
    public static String toString (String                       label,
                                   Collection<?extends Section> sections)
    {
        if (sections == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(label)
          .append("[");

        for (Section section : sections) {
            sb.append("#")
              .append(section.getId());
        }

        sb.append("]");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the section
     * array, introduced by the provided label
     *
     * @param label the string that introduces the list of IDs
     * @param sections the array of sections
     * @return the string built
     */
    public static String toString (String     label,
                                   Section... sections)
    {
        return toString(label, Arrays.asList(sections));
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the section
     * collection, introduced by the label "sections"
     *
     * @param sections the collection of sections
     * @return the string built
     */
    public static String toString (Collection<?extends Section> sections)
    {
        return toString("sections", sections);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the section
     * array, introduced by the label "sections"
     *
     * @param sections the array of sections
     * @return the string built
     */
    public static String toString (Section... sections)
    {
        return toString("sections", sections);
    }
}
