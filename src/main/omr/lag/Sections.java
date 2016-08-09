//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S e c t i o n s                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.lag;

import omr.run.Orientation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code Sections} handles features related to a collection of sections.
 *
 * @author Hervé Bitteur
 */
public class Sections
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Sections.class);

    //~ Constructors -------------------------------------------------------------------------------
    private Sections ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // byReverseLength //
    //-----------------//
    /**
     * Return a comparator for comparing Section instances on their decreasing length,
     * using the provided orientation.
     *
     * @param orientation the provided orientation
     * @return the properly oriented length comparator
     */
    public static Comparator<Section> byReverseLength (final Orientation orientation)
    {
        return new Comparator<Section>()
        {
            @Override
            public int compare (Section s1,
                                Section s2)
            {
                return Integer.signum(s2.getLength(orientation) - s1.getLength(orientation));
            }
        };
    }

    //-----------------//
    // indexByPosition //
    //-----------------//
    /**
     * Build an index of provided sections on their starting position.
     * index[pos] contains the index in list of first section starting at this pos value.
     *
     * @param posCount number of position values (typically max pos +1)
     * @param list     the list of sections to index, assumed to be ordered by full position
     * @return the index array
     */
    public static int[] indexByPosition (int posCount,
                                         List<? extends Section> list)
    {
        ///watch.start("populate starts");
        // Register sections by their starting pos
        // 'starts' is a vector parallel to sheet positions (+1 additional cell)
        // Vector at index p gives the index in 'list' of the first section starting at 'p' (or -1).
        // And similarly at index 'p+1', either -1 (for no section) or index for row 'p+1'.
        // Hence, all sections starting  at 'p' are in [starts[p]..starts[p+1][ sublist
        final int[] starts = new int[posCount + 1];
        Arrays.fill(starts, 0, starts.length, -1);

        int currentPos = -1;

        for (int i = 0, iBreak = list.size(); i < iBreak; i++) {
            final Section section = list.get(i);
            final int pos = section.getFirstPos();

            if (pos > currentPos) {
                starts[pos] = i;
                currentPos = pos;
            }
        }

        starts[starts.length - 1] = list.size(); // End mark

        return starts;
    }

    //---------------------//
    // intersectedSections //
    //---------------------//
    /**
     * Convenient method to look for sections that intersect the provided rectangle
     *
     * @param rect     provided rectangle
     * @param sections the collection of sections to browse
     * @return the set of intersecting sections
     */
    public static Set<Section> intersectedSections (Rectangle rect,
                                                    Collection<? extends Section> sections)
    {
        Set<Section> found = new LinkedHashSet<Section>();

        for (Section section : sections) {
            if (section.intersects(rect)) {
                found.add(section);
            }
        }

        return found;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the section collection,
     * introduced by the provided label.
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
        sb.append(label).append("[");

        for (Section section : sections) {
            sb.append("#").append(section.isVertical() ? "V" : "H").append(section.getId());
        }

        sb.append("]");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the section collection,
     * introduced by the label "sections".
     *
     * @param sections the collection of sections
     * @return the string built
     */
    public static String toString (Collection<? extends Section> sections)
    {
        return toString("sections", sections);
    }
}
//
//    //-------------------//
//    // containingSection //
//    //-------------------//
//    /**
//     * Report the first section that contains the provided point.
//     *
//     * @param x        abscissa of provided point
//     * @param y        ordinate of provided point
//     * @param sections the collection of sections to browse
//     * @return the section found or null
//     */
//    public static Section containingSection (int x,
//                                             int y,
//                                             Collection<? extends Section> sections)
//    {
//        for (Section section : sections) {
//            if (section.contains(x, y)) {
//                return section;
//            }
//        }
//
//        return null;
//    }
