//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S e c t i o n T a l l y                                    //
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A easy way to access all sections that begin at a given position.
 *
 * @param <S> precise section type
 */
public class SectionTally<S extends Section>
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final List<S> list;

    private final int[] starts;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Build a tally on a sorted list of sections
     *
     * @param posCount maximum position value +1 (typically sheet height or width)
     * @param list     the list of sections (assumed to be ordered by full position!)
     */
    public SectionTally (int posCount,
                         List<S> list)
    {
        this.list = list;
        // Register sections by their starting pos
        // 'starts' is a vector parallel to sheet positions (+1 additional cell)
        // Vector at index p gives the index in 'list' of the first section starting at 'p' (or -1).
        // And similarly at index 'p+1', either -1 (for no section) or index for row 'p+1'.
        // Hence, all sections starting  at 'p' are in [starts[p]..starts[p+1][ sublist
        starts = new int[posCount + 1];
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
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // getSubList //
    //------------//
    /**
     * Report the sections that start at provided position value
     *
     * @param pos provided position value
     * @return the sublist of sections starting at this position, perhaps empty
     */
    public List<S> getSubList (int pos)
    {
        final int iStart = starts[pos];

        if (iStart == -1) {
            return Collections.emptyList();
        }

        int iNextStart = list.size();

        for (int j = pos + 1; j < starts.length; j++) {
            iNextStart = starts[j];

            if (iNextStart != -1) {
                break;
            }
        }

        return list.subList(iStart, iNextStart);
    }
}
