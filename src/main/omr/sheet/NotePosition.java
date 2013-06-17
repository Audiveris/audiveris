//----------------------------------------------------------------------------//
//                                                                            //
//                          N o t e P o s i t i o n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.grid.StaffInfo;
import omr.grid.StaffInfo.IndexedLedger;

/**
 * Class {@code NotePosition} handles the precise position of a
 * note-like entity, with respect to its related staff.
 *
 * @author Hervé Bitteur
 */
public class NotePosition
{
    //~ Instance fields --------------------------------------------------------

    /** The related staff. */
    private final StaffInfo staff;

    /** The precise pitch position wrt the staff. */
    private final double pitchPosition;

    /** The closest ledger if any. */
    private final IndexedLedger indexedLedger;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // NotePosition //
    //--------------//
    /**
     * Creates a new NotePosition object.
     *
     * @param staff         the related staff
     * @param pitchPosition the precise pitch position
     * @param indexedLedger the closest ledger if any
     */
    public NotePosition (StaffInfo staff,
                         double pitchPosition,
                         IndexedLedger indexedLedger)
    {
        this.staff = staff;
        this.indexedLedger = indexedLedger;
        this.pitchPosition = pitchPosition;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getLedger //
    //-----------//
    /**
     * @return the ledger
     */
    public IndexedLedger getLedger ()
    {
        return indexedLedger;
    }

    //------------------//
    // getPitchPosition //
    //------------------//
    /**
     * @return the pitchPosition
     */
    public double getPitchPosition ()
    {
        return pitchPosition;
    }

    //----------//
    // getStaff //
    //----------//
    /**
     * @return the staff
     */
    public StaffInfo getStaff ()
    {
        return staff;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        sb.append(" staff#")
                .append(staff.getId());

        sb.append(" pitch:")
                .append((float) pitchPosition);

        if (indexedLedger != null) {
            sb.append(" ledger#")
                    .append(indexedLedger.glyph.getId());
        }

        sb.append("}");

        return sb.toString();
    }
}
