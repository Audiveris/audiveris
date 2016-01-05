//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      L o g i c a l P a r t                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Class {@code LogicalPart} describes a part at score or page level.
 * It is "instantiated" in each SystemInfo by a {@link omr.sheet.Part}.
 * <p>
 * There is an intermediate LogicalPart instance at Page level, which abstracts the system parts at
 * page level, and which is then used when abstracting the part information from pages to score.</p>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class LogicalPart
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            LogicalPart.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * Distinguished id for this logical part.
     * (the same id is used by the corresponding physical Part in each System)
     */
    @XmlAttribute
    private int id;

    /** Count of staves. */
    @XmlAttribute(name = "staff-count")
    private final int staffCount;

    /** Name for this part. */
    @XmlAttribute
    private String name;

    /** Abbreviation for this part, if any. */
    @XmlElement
    private String abbreviation;

    /** Instrument MIDI program, if any. */
    @XmlElement(name = "midi-program")
    private Integer midiProgram;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new instance of ScorePart
     *
     * @param id         the id for this part
     * @param staffCount the count of staves within this part
     */
    public LogicalPart (int id,
                        int staffCount)
    {
        setId(id);
        this.staffCount = staffCount;
    }

    /** Meant for XML binder only */
    private LogicalPart ()
    {
        setId(0);
        staffCount = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        return "P" + getId();
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
        sb.append("LogicalPart{");

        sb.append("id=").append(getId());

        if (name != null) {
            sb.append(" name=").append(name);
        }

        if (abbreviation != null) {
            sb.append(" abrv=").append(abbreviation);
        }

        sb.append(" staffCount:").append(staffCount);

        sb.append("}");

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        // Default Part names
        private final Constant.String defaultSingleStaffPartName = new Constant.String(
                "Voice",
                "Default name for a part with one staff");

        private final Constant.String defaultDoubleStaffPartName = new Constant.String(
                "Piano",
                "Default name for a part with two staves");

        private final Constant.String defaultPartName = new Constant.String(
                "NoName",
                "Default name for a part with more than two staves");

        // Default Midi program numbers
        private final Constant.Integer defaultSingleStaffPartProgram = new Constant.Integer(
                "MidiProgram",
                54,
                "Default program number for a part with one staff");

        private final Constant.Integer defaultDoubleStaffPartProgram = new Constant.Integer(
                "MidiProgram",
                1,
                "Default program number for a part with two staves");

        private final Constant.Integer defaultPartProgram = new Constant.Integer(
                "MidiProgram",
                1,
                "Default program number for a part with more than two staves");
    }
}
