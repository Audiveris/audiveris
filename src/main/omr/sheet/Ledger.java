//----------------------------------------------------------------------------//
//                                                                            //
//                                L e d g e r                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

/**
 * Class {@code Ledger} is a physical {@link Dash} which is logically
 * a Ledger (to represents portions of virtual staff lines)
 *
 * @author Hervé Bitteur
 */
@Deprecated
public class Ledger
        extends Dash
{
    //~ Instance fields --------------------------------------------------------

    /** Precise line index outside of staff nearby. */
    private final int lineIndex;

    //~ Constructors -----------------------------------------------------------
    //--------//
    // Ledger //
    //--------//
    /**
     * Create a Ledger, from its underlying horizontal stick
     *
     * @param stick     the related retrieved stick
     * @param staff     the staff nearby
     * @param lineIndex the precise line index wrt staff
     *                  ( -1, -2, ... above staff and +1, +2, ... below staff)
     */
    public Ledger (Glyph stick,
                   StaffInfo staff,
                   int lineIndex)
    {
        super(stick, staff);
        this.lineIndex = lineIndex;
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // getLineIndex //
    //--------------//
    /**
     * @return the precise line index for this ledger
     */
    public int getLineIndex ()
    {
        return lineIndex;
    }

    //------------------//
    // getPitchPosition //
    //------------------//
    /**
     * Report the pitch position of this ledger WRT the related staff
     *
     * @return the pitch position
     */
    public int getPitchPosition ()
    {
        //        // Safer, for the time being...
        //        if (getStaff()
        //                .getLines()
        //                .size() != 5) {
        //            throw new RuntimeException("Only 5-line staves are supported");
        //        }
        if (lineIndex > 0) {
            return 4 + (2 * lineIndex);
        } else {
            return -4 + (2 * lineIndex);
        }
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());
        sb.append(" index:")
                .append(lineIndex);
        sb.append(" pitch:")
                .append(getPitchPosition());

        return sb.toString();
    }
}
