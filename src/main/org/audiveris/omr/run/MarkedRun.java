//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        M a r k e d R u n                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.run;

/**
 * Class {@code MarkedRun} is a run with a mark.
 * This is meant to be a temporary structure used for building glyph(s) out of runs.
 *
 * @author Hervé Bitteur
 */
public class MarkedRun
        extends Run
{

    /** Assigned mark. */
    private int mark;

    /**
     * Creates a new {@code MarkedRun} object.
     *
     * @param run  a standard run
     * @param mark the assigned mark
     */
    public MarkedRun (Run run,
                      int mark)
    {
        this(run.getStart(), run.getLength(), mark);
    }

    /**
     * Creates a new {@code MarkedRun} object.
     *
     * @param start  the coordinate of start for a run (y for vertical run)
     * @param length the length of the run in pixels
     * @param mark   the assigned mark
     */
    public MarkedRun (int start,
                      int length,
                      int mark)
    {
        super(start, length);
        this.mark = mark;
    }

    public int getMark ()
    {
        return mark;
    }

    public void setMark (int mark)
    {
        this.mark = mark;
    }

    @Override
    public void setStart (int start)
    {
        this.start = start;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MarkedRun{");
        sb.append(start).append("/").append(length);
        sb.append(" mark=").append(mark);
        sb.append("}");

        return sb.toString();
    }
}
