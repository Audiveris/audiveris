//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e P a r t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>ScorePart</code> defines a part at score level. It is
 * instantiated in each System by a SystemPart.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScorePart
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScorePart.class);

    //~ Instance fields --------------------------------------------------------

    /**
     * Distinguished id for this part (the same id is used by the corresponding
     * SystemPart in each System)
     */
    private final int id;

    /** Name for this part */
    private String name;

    /** Typical ordinate for displaying this part in the score display */
    private final int displayOrdinate;

    /** Instrument MIDI program, if any */
    private Integer midiProgram;

    /** List of staff ids */
    private List<Integer> ids = new ArrayList<Integer>();

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // ScorePart //
    //-----------//
    /**
     * Creates a new instance of ScorePart, built from a SystemPart
     *
     * @param systemPart the concrete SystemPart
     * @param id the id for this part
     * @param displayOrdinate the ordinate offset of this part wrt system
     */
    public ScorePart (SystemPart systemPart,
                      int        id,
                      int        displayOrdinate)
    {
        this.id = id;
        this.displayOrdinate = displayOrdinate;

        for (TreeNode node : systemPart.getStaves()) {
            Staff staff = (Staff) node;
            ids.add(staff.getId());
        }
    }

    /** Meant for XML binder only */
    private ScorePart ()
    {
        id = 0;
        displayOrdinate = 0;
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getDefaultName //
    //----------------//
    public String getDefaultName ()
    {
        switch (getStaffIds()
                    .size()) {
        case 1 :
            return constants.defaultSingleStaffPartName.getValue();

        case 2 :
            return constants.defaultDoubleStaffPartName.getValue();

        default :
            return constants.defaultPartName.getValue();
        }
    }

    //-------------------//
    // getDefaultProgram //
    //-------------------//
    public Integer getDefaultProgram ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("Part #" + getId() + " size=" + getStaffIds().size());
        }

        switch (getStaffIds()
                    .size()) {
        case 1 :
            return constants.defaultSingleStaffPartProgram.getValue();

        case 2 :
            return constants.defaultDoubleStaffPartProgram.getValue();

        default :
            return constants.defaultPartProgram.getValue();
        }
    }

    //--------------------//
    // getDisplayOrdinate //
    //--------------------//
    public int getDisplayOrdinate ()
    {
        return displayOrdinate;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the id of this part
     *
     * @return the part id
     */
    public int getId ()
    {
        return id;
    }

    //----------------//
    // setMidiProgram //
    //----------------//
    public void setMidiProgram (Integer midiProgram)
    {
        this.midiProgram = midiProgram;
    }

    //----------------//
    // getMidiProgram //
    //----------------//
    public Integer getMidiProgram ()
    {
        return midiProgram;
    }

    //--------------//
    // isMultiStaff //
    //--------------//
    /**
     * Report whether there are more than a single staff in this part
     *
     * @return true if this part is multi-staff
     */
    public boolean isMultiStaff ()
    {
        return ids.size() > 1;
    }

    //---------//
    // setName //
    //---------//
    /**
     * Assign a name to this part
     *
     * @param name the new part name
     */
    public void setName (String name)
    {
        this.name = name;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the assigned name
     *
     * @return the part name
     */
    public String getName ()
    {
        return name;
    }

    //--------//
    // getPid //
    //--------//
    /**
     * Report a pid string, using format "Pn", where 'n' is the id
     *
     * @return the Pid
     */
    public String getPid ()
    {
        return "P" + id;
    }

    //-------------//
    // getStaffIds //
    //-------------//
    /**
     * Report the staff ids for this part
     *
     * @return the list of staff ids
     */
    public List<Integer> getStaffIds ()
    {
        return ids;
    }

    //
    //    //--------//
    //    // equals //
    //    //--------//
    //    /**
    //     * Check whether the list of ids are identical
    //     *
    //     * @param obj the object to compare to
    //     * @return true if equal
    //     */
    //    @Override
    //    public boolean equals (Object obj)
    //    {
    //        if (obj instanceof ScorePart) {
    //            ScorePart sp = (ScorePart) obj;
    //
    //            if (sp.ids.size() != ids.size()) {
    //                return false;
    //            }
    //
    //            for (int i = 0; i < ids.size(); i++) {
    //                if (!(sp.ids.get(i).equals(ids.get(i)))) {
    //                    return false;
    //                }
    //            }
    //
    //            return true;
    //        } else {
    //            return false;
    //        }
    //    }
    //
    //    //----------//
    //    // hashCode //
    //    //----------//
    //    /**
    //     * To please FindBugs, because of overriding of equals method
    //     * @return nothing
    //     */
    //    @Override
    //    public int hashCode ()
    //    {
    //        assert false : "hashCode not designed";
    //
    //        return 42; // any arbitrary constant will do
    //    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Part");

        sb.append(" id=")
          .append(id);

        if (name != null) {
            sb.append(" name=")
              .append(name);
        }

        sb.append(" [");

        for (Integer i : ids) {
            sb.append(i + " ");
        }

        sb.append("]}");

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        // Default Part names
        Constant.String  defaultSingleStaffPartName = new Constant.String(
            "Voice",
            "Default name for a part with one staff");
        Constant.String  defaultDoubleStaffPartName = new Constant.String(
            "Piano",
            "Default name for a part with two staves");
        Constant.String  defaultPartName = new Constant.String(
            "NoName",
            "Default name for a part with more than two staves");

        // Default Midi program numbers
        Constant.Integer defaultSingleStaffPartProgram = new Constant.Integer(
            "MidiProgram",
            54,
            "Default program number for a part with one staff");
        Constant.Integer defaultDoubleStaffPartProgram = new Constant.Integer(
            "MidiProgram",
            1,
            "Default program number for a part with two staves");
        Constant.Integer defaultPartProgram = new Constant.Integer(
            "MidiProgram",
            1,
            "Default program number for a part with more than two staves");
    }
}
