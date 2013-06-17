//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e P a r t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ScorePart} defines a part at score level. It is
 * instantiated in each System by a SystemPart.
 *
 * <p>There is an intermediate ScorePart instance at Page level, which records
 * the merge of system parts at page level, and which is then used when merging
 * the part information from pages to score.</p>
 *
 * @author Hervé Bitteur
 */
public class ScorePart
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            ScorePart.class);

    //~ Instance fields --------------------------------------------------------
    /**
     * Distinguished id for this part (the same id is used by the corresponding
     * SystemPart in each System)
     */
    private int id;

    /** Count of staves */
    private final int staffCount;

    /** Name for this part */
    private String name;

    /** Abbreviation for this part, if any */
    private String abbreviation;

    /** Instrument MIDI program, if any */
    private Integer midiProgram;

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // ScorePart //
    //-----------//
    /**
     * Creates a new instance of ScorePart
     *
     * @param id         the id for this part
     * @param staffCount the count of staves within this part
     */
    public ScorePart (int id,
                      int staffCount)
    {
        setId(id);
        this.staffCount = staffCount;
    }

    /** Meant for XML binder only */
    private ScorePart ()
    {
        setId(0);
        staffCount = 0;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // getAbbreviation //
    //-----------------//
    /**
     * @return the abbreviation
     */
    public String getAbbreviation ()
    {
        return abbreviation;
    }

    //----------------//
    // getDefaultName //
    //----------------//
    public String getDefaultName ()
    {
        switch (staffCount) {
        case 1:
            return constants.defaultSingleStaffPartName.getValue();

        case 2:
            return constants.defaultDoubleStaffPartName.getValue();

        default:
            return constants.defaultPartName.getValue();
        }
    }

    //-------------------//
    // getDefaultProgram //
    //-------------------//
    public Integer getDefaultProgram ()
    {
        logger.debug("Part #{} count={}", getId(), staffCount);

        switch (staffCount) {
        case 1:
            return constants.defaultSingleStaffPartProgram.getValue();

        case 2:
            return constants.defaultDoubleStaffPartProgram.getValue();

        default:
            return constants.defaultPartProgram.getValue();
        }
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
    // getMidiProgram //
    //----------------//
    public Integer getMidiProgram ()
    {
        return midiProgram;
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

    //---------------//
    // getStaffCount //
    //---------------//
    /**
     * @return the staffCount
     */
    public int getStaffCount ()
    {
        return staffCount;
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
        return staffCount > 1;
    }

    //-----------------//
    // setAbbreviation //
    //-----------------//
    /**
     * @param abbreviation the abbreviation to set
     */
    public void setAbbreviation (String abbreviation)
    {
        this.abbreviation = abbreviation;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Set the id of this part
     *
     * @param id the distinguished part id
     */
    public final void setId (int id)
    {
        this.id = id;
    }

    //----------------//
    // setMidiProgram //
    //----------------//
    public void setMidiProgram (Integer midiProgram)
    {
        this.midiProgram = midiProgram;
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{ScorePart");

        sb.append(" id=")
                .append(id);

        if (name != null) {
            sb.append(" name=")
                    .append(name);
        }

        if (abbreviation != null) {
            sb.append(" abrv=")
                    .append(abbreviation);
        }

        sb.append(" staffCount:")
                .append(staffCount);

        sb.append("}");

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
        Constant.String defaultSingleStaffPartName = new Constant.String(
                "Voice",
                "Default name for a part with one staff");

        Constant.String defaultDoubleStaffPartName = new Constant.String(
                "Piano",
                "Default name for a part with two staves");

        Constant.String defaultPartName = new Constant.String(
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
