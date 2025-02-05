//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      L o g i c a l P a r t                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Class <code>LogicalPart</code> describes a part at score level.
 * <p>
 * It can be "instantiated" in one or several SystemInfo by a (physical)
 * {@link Part} instance.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class LogicalPart
        implements Cloneable
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LogicalPart.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /**
     * This is the distinguished integer id for this logical part.
     * <p>
     * The same id is used by the corresponding physical part in each system where this part
     * appears.
     */
    @XmlAttribute(name = "id")
    private int id;

    /**
     * This is the count of staves in this logical part.
     * Deprecated since the introduction of the more precise lineCounts field.
     * <p>
     * The count of staves must be the same for a given logical part and for the corresponding
     * physical part in each system.
     * <p>
     * Typical examples of count values are:
     * <ul>
     * <li>1 for a singer part
     * <li>2 for a piano part
     * <li>3 for an organ part
     * </ul>
     */
    @XmlAttribute(name = "staff-count")
    @Deprecated
    private final int staffCount;

    /**
     * The number of lines for each staff in this part.
     * <p>
     * Deprecated, replaced by <code>staffConfigs</code>.
     */
    @Deprecated
    @XmlElement(name = "line-count")
    private List<Integer> OLD_lineCounts;

    /**
     * The number of lines, perhaps small annotated, for each staff in this part.
     */
    @XmlElement(name = "staff-configuration")
    private List<StaffConfig> staffConfigs = new ArrayList<>();

    /**
     * This is the name, often an instrument name, for this logical part.
     * <p>
     * It is generally located in the left margin of the very first physical part and the OCR can
     * often recognize the text.
     */
    @XmlAttribute(name = "name")
    private String name;

    /**
     * This is the abbreviation, if any, used for this part name on the following systems.
     * <p>
     * It is also located in the left margin of the system, but the OCR often cannot find
     * the abbreviation in its dictionary.
     */
    @XmlAttribute(name = "abbreviation")
    private String abbreviation;

    /**
     * This is the instrument MIDI program, if any.
     * <p>
     * Audiveris uses heuristics to assign realistic MIDI programs in some frequent cases
     * like "singer part + piano part".
     */
    @XmlAttribute(name = "midi-program")
    private Integer midiProgram;

    //~ Constructors -------------------------------------------------------------------------------

    /** Meant for XML binder only */
    private LogicalPart ()
    {
        setId(0);
        staffCount = 0;
    }

    /**
     * Creates a new instance of ScorePart
     *
     * @param id           the id for this part
     * @param staffCount   the count of staves within this part
     * @param staffConfigs the configuration for each staff in part
     */
    public LogicalPart (int id,
                        int staffCount,
                        List<StaffConfig> staffConfigs)
    {
        setId(id);
        this.staffCount = staffCount;
        setStaffConfigs(staffConfigs);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------------//
    // afterUnmarshal //
    //----------------//
    @SuppressWarnings(value = "unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        if (OLD_lineCounts != null) {
            for (int count : OLD_lineCounts) {
                staffConfigs.add(new StaffConfig(count, false));
            }

            OLD_lineCounts = null;
        }
    }

    //------//
    // copy //
    //------//
    public LogicalPart copy ()
    {
        try {
            final LogicalPart that = (LogicalPart) super.clone();
            that.staffConfigs = new ArrayList<>(staffConfigs);

            return that;
        } catch (CloneNotSupportedException ignored) {
            logger.error("Could not clone LogicalPart");
            return null;
        }
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LogicalPart that) {
            if ((id != that.id) || (staffCount != that.staffCount)) {
                return false;
            }

            return Objects.deepEquals(name, that.name) //
                    && Objects.deepEquals(abbreviation, that.abbreviation) //
                    && Objects.deepEquals(staffConfigs, that.staffConfigs) //
                    && Objects.deepEquals(midiProgram, that.midiProgram);
        }

        return false;
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
    /**
     * Report the default name for this part, based on staff count.
     *
     * @return inferred name
     */
    public String getDefaultName ()
    {
        return switch (staffCount) {
            case 1 -> constants.defaultSingleStaffPartName.getValue();
            case 2 -> constants.defaultDoubleStaffPartName.getValue();
            default -> constants.defaultPartName.getValue();
        };
    }

    //-------------------//
    // getDefaultProgram //
    //-------------------//
    /**
     * Report the default midi program, based on staff count.
     *
     * @return inferred program
     */
    public Integer getDefaultProgram ()
    {
        logger.debug("Part #{} count={}", getId(), staffCount);

        return switch (staffCount) {
            case 1 -> constants.defaultSingleStaffPartProgram.getValue();
            case 2 -> constants.defaultDoubleStaffPartProgram.getValue();
            default -> constants.defaultPartProgram.getValue();
        };
    }

    //-------------//
    // getFullName //
    //-------------//
    /**
     * Report the full name formatted as: name + " [" + abbreviation + "]".
     *
     * @return the full name for this logical part
     */
    public String getFullName ()
    {
        if (name == null)
            return null;

        if (abbreviation == null)
            return name;

        return name + " [" + abbreviation + "]";
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the id of this logical part
     *
     * @return the part id
     */
    public int getId ()
    {
        return id;
    }

    //----------//
    // getIndex //
    //----------//
    /**
     * Report the index of this logical part within the provided score.
     *
     * @param score the containing score
     * @return logical part index
     */
    public int getIndex (Score score)
    {
        return score.getLogicalParts().indexOf(this);
    }

    //----------------//
    // getMidiProgram //
    //----------------//
    /**
     * Report part midi program number, if any.
     *
     * @return program number or null
     */
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

    //-----------------//
    // getStaffConfigs //
    //-----------------//
    /**
     * Report configuration for each staff in this part.
     *
     * @return the staves configurations
     */
    public List<StaffConfig> getStaffConfigs ()
    {
        return Collections.unmodifiableList(staffConfigs);
    }

    //---------------//
    // getStaffCount //
    //---------------//
    /**
     * Report the count of staves in this logical part.
     *
     * @return the staffCount
     */
    public int getStaffCount ()
    {
        return staffCount;
    }

    //----------//
    // hashCode //
    //----------//
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
        this.id = id;
    }

    //----------------//
    // setMidiProgram //
    //----------------//
    /**
     * Assign the part midi program.
     *
     * @param midiProgram the midi program number for this part
     */
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

    //-----------------//
    // setStaffConfigs //
    //-----------------//
    public final void setStaffConfigs (List<StaffConfig> staffConfigs)
    {
        this.staffConfigs.clear();
        this.staffConfigs.addAll(staffConfigs);
    }

    //-------------//
    // swapVoiceId //
    //-------------//
    /**
     * Change, in the provided page, the ID of voice for the specified newId
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
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('#').append(id).append('{');

        if (name != null) {
            sb.append("name:").append(name);
        }

        if (abbreviation != null) {
            sb.append(" abrv:").append(abbreviation);
        }

        if (midiProgram != null) {
            sb.append(" midi:").append(midiProgram);
        }

        sb.append(" configs:[").append(StaffConfig.toCsvString(staffConfigs)).append(']');

        sb.append('}');

        return sb.toString();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //---------//
    // valueOf //
    //---------//
    /**
     * Report, among the collection of LogicalPart instances, the one that matches on full name.
     *
     * @param fullName the full name to match: name [abbrev]
     * @param logicals collection of LogicalParts to browse
     * @return the proper LogicalPart found or null
     */
    public static LogicalPart valueOf (String fullName,
                                       List<LogicalPart> logicals)
    {
        // Extract name from full name
        final int bracket = fullName.indexOf('[');
        final String name = ((bracket != -1) ? fullName.substring(0, bracket) : fullName).trim();

        for (LogicalPart log : logicals) {
            if (log.name.equalsIgnoreCase(name))
                return log;
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
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
