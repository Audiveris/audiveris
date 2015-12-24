//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    N o t e P o s i t i o n                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.note;

import omr.sheet.Staff;
import omr.sheet.Staff.IndexedLedger;

/**
 * Class {@code NotePosition} handles the precise position of a note-like entity,
 * with respect to its related staff.
 *
 * @author Hervé Bitteur
 */
public class NotePosition
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The related staff. */
    private final Staff staff;

    /** The precise pitch position wrt the staff. */
    private final double pitchPosition;

    /** The closest ledger if any. */
    private final IndexedLedger indexedLedger;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new NotePosition object.
     *
     * @param staff         the related staff
     * @param pitchPosition the precise pitch position
     * @param indexedLedger the closest ledger if any
     */
    public NotePosition (Staff staff,
                         double pitchPosition,
                         IndexedLedger indexedLedger)
    {
        this.staff = staff;
        this.indexedLedger = indexedLedger;
        this.pitchPosition = pitchPosition;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
    public Staff getStaff ()
    {
        return staff;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        sb.append(" staff#").append(staff.getId());

        sb.append(" pitch:").append(String.format("%.1f", pitchPosition));

        if (indexedLedger != null) {
            sb.append(" ledger#").append(indexedLedger.ledger.getId());
        }

        sb.append("}");

        return sb.toString();
    }
}
