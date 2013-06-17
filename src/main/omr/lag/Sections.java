//----------------------------------------------------------------------------//
//                                                                            //
//                              S e c t i o n s                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.run.Orientation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class {@code Sections} handles features related to a collection of
 * sections.
 *
 * @author Hervé Bitteur
 */
public class Sections
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Sections.class);

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getBounds //
    //-----------//
    /**
     * Return the display bounding box of a collection of sections.
     *
     * @param sections the provided collection of sections
     * @return the bounding contour
     */
    public static Rectangle getBounds (Collection<? extends Section> sections)
    {
        Rectangle box = null;

        for (Section section : sections) {
            if (box == null) {
                box = new Rectangle(section.getBounds());
            } else {
                box.add(section.getBounds());
            }
        }

        return box;
    }

    //----------------------------//
    // getReverseLengthComparator //
    //----------------------------//
    /**
     * Return a comparator for comparing Section instances on their
     * decreasing length, using the provided orientation.
     *
     * @param orientation the provided orientation
     */
    public static Comparator<Section> getReverseLengthComparator (
            final Orientation orientation)
    {
        return new Comparator<Section>()
        {
            @Override
            public int compare (Section s1,
                                Section s2)
            {
                return Integer.signum(
                        s2.getLength(orientation) - s1.getLength(orientation));
            }
        };
    }

    //---------------------------//
    // lookupIntersectedSections //
    //---------------------------//
    /**
     * Convenient method to look for sections that intersect the
     * provided rectangle
     *
     * @param rect     provided rectangle
     * @param sections the collection of sections to browse
     * @return the set of intersecting sections
     */
    public static Set<Section> lookupIntersectedSections (Rectangle rect,
                                                          Collection<? extends Section> sections)
    {
        Set<Section> found = new LinkedHashSet<>();

        for (Section section : sections) {
            if (section.intersects(rect)) {
                found.add(section);
            }
        }

        return found;
    }

    //----------------//
    // lookupSections //
    //----------------//
    /**
     * Convenient method to look for sections contained by the provided
     * rectangle
     *
     * @param rect     provided rectangle
     * @param sections the collection of sections to browse
     * @return the set of contained sections
     */
    public static Set<Section> lookupSections (Rectangle rect,
                                               Collection<? extends Section> sections)
    {
        Set<Section> found = new LinkedHashSet<>();

        for (Section section : sections) {
            if (rect.contains(section.getBounds())) {
                found.add(section);
            }
        }

        return found;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the
     * section collection, introduced by the provided label.
     *
     * @param label    the string that introduces the list of IDs
     * @param sections the collection of sections
     * @return the string built
     */
    public static String toString (String label,
                                   Collection<? extends Section> sections)
    {
        if (sections == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(label)
                .append("[");

        for (Section section : sections) {
            sb.append("#")
                    .append(section.isVertical() ? "V" : "H")
                    .append(section.getId());
        }

        sb.append("]");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the
     * section array, introduced by the provided label.
     *
     * @param label    the string that introduces the list of IDs
     * @param sections the array of sections
     * @return the string built
     */
    public static String toString (String label,
                                   Section... sections)
    {
        return toString(label, Arrays.asList(sections));
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the
     * section collection, introduced by the label "sections".
     *
     * @param sections the collection of sections
     * @return the string built
     */
    public static String toString (Collection<? extends Section> sections)
    {
        return toString("sections", sections);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the
     * section array, introduced by the label "sections".
     *
     * @param sections the array of sections
     * @return the string built
     */
    public static String toString (Section... sections)
    {
        return toString("sections", sections);
    }

    private Sections ()
    {
    }
}
