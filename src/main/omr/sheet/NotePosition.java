//----------------------------------------------------------------------------//
//                                                                            //
//                          N o t e P o s i t i o n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.grid.StaffInfo;

/**
 * Class {@code NotePosition} handles the precise position of a note-like
 * entity, with respect to its related staff.
 *
 * @author Hervé Bitteur
 */
public class NotePosition
{
    //~ Instance fields --------------------------------------------------------

    /** The related staff */
    private final StaffInfo staff;

    /** The precise pitch position wrt the staff */
    private final double pitchPosition;

    /** The closest ledger if any */
    private final Ledger ledger;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // NotePosition //
    //--------------//
    /**
     * Creates a new NotePosition object.
     * @param staff the related staff
     * @param pitchPosition the precise pitch position
     * @param ledger the closest ledger if any
     */
    public NotePosition (StaffInfo staff,
                         double    pitchPosition,
                         Ledger    ledger)
    {
        this.staff = staff;
        this.ledger = ledger;
        this.pitchPosition = pitchPosition;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getLedger //
    //------------//
    /**
     * @return the ledger
     */
    public Ledger getLedger ()
    {
        return ledger;
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

        if (ledger != null) {
            sb.append(" ledger#")
              .append(ledger.getStick().getId());
        }

        sb.append("}");

        return sb.toString();
    }
}
