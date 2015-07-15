//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        M a r k e d R u n                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

/**
 * Class {@code MarkedRun} is a run with a mark.
 * This is meant to be a temporary structure used for building glyph(s) out of runs.
 *
 * @author Hervé Bitteur
 */
public class MarkedRun
        extends Run
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Assigned mark. */
    private int mark;

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
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
