//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      L o g i c a l P a r t                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.score;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code LogicalPart} describes a part at score or page level.
 * It can be "instantiated" in one or several SystemInfo by a {@link org.audiveris.omr.sheet.Part}.
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
    //
    // Persistent data
    //----------------
    //
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
    @XmlAttribute
    private String abbreviation;

    /** Instrument MIDI program, if any. */
    @XmlAttribute(name = "midi-program")
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
    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof LogicalPart)) {
            return false;
        }

        LogicalPart that = (LogicalPart) obj;

        if ((id != that.id) || (staffCount != that.staffCount)) {
            return false;
        }

        return Objects.deepEquals(midiProgram, that.midiProgram)
               && Objects.deepEquals(name, that.name)
               && Objects.deepEquals(abbreviation, that.abbreviation);
    }

    //-------//
    // setId //
    //-------//
    /**
     * Set the id of this part.
     *
     * @param id the new part id
     */
    public final void setId (int id)
    {
        if (this.id != id) {
            this.id = id;
        }
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

    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (23 * hash) + this.id;
        hash = (23 * hash) + this.staffCount;
        hash = (23 * hash) + Objects.hashCode(this.name);
        hash = (23 * hash) + Objects.hashCode(this.abbreviation);
        hash = (23 * hash) + Objects.hashCode(this.midiProgram);

        return hash;
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

    //-------------//
    // swapVoiceId //
    //-------------//
    /**
     * Change in the provided page, the ID of voice for the specified newId
     *
     * @param page  the page to update
     * @param oldId old ID
     * @param newId new ID to be used for this voice in all measures of page for this logical part
     */
    public void swapVoiceId (Page page,
                             int oldId,
                             int newId)
    {
        for (SystemInfo system : page.getSystems()) {
            Part part = system.getPartById(this.id);

            if (part != null) {
                part.swapVoiceId(oldId, newId);
            }
        }
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
