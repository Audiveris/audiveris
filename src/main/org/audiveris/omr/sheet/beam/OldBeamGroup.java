//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     O l d B e a m G r o u p                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sheet.beam;

import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code OldBeamGroup} represents a group of related beams.
 * <p>
 * This class is now deprecated (replaced by {@link BeamGroupInter} and is kept here only to allow
 * migration of old .omr files.
 *
 * @author Hervé Bitteur
 */
@Deprecated
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "beam-group")
public class OldBeamGroup
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(OldBeamGroup.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Id for debug mainly, unique within measure stack. */
    @XmlAttribute
    private final int id;

    /** Indicates a beam group that is linked to more than one staff. */
    @XmlAttribute(name = "multi-staff")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean multiStaff;

    /** Set of contained beams. */
    @XmlList
    @XmlIDREF
    @XmlValue
    private final LinkedHashSet<AbstractBeamInter> beams = new LinkedHashSet<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * No-arg constructor meant for JAXB.
     */
    private OldBeamGroup ()
    {
        this.id = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // afterReload //
    //-------------//
    /**
     * To be called right after unmarshalling.
     *
     * @param measure the containing measure
     * @param sig     the related sig
     */
    public void afterReload (Measure measure,
                             SIGraph sig)
    {
        try {
            final BeamGroupInter beamGroup = new BeamGroupInter();
            beamGroup.setMeasure(measure);
            beamGroup.setMultiStaff(multiStaff);
            sig.addVertex(beamGroup);

            for (AbstractBeamInter beam : beams) {
                beam.setGroup(beamGroup);
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //----------//
    // getBeams //
    //----------//
    /**
     * Report the beams that are part of this group.
     *
     * @return the collection of contained beams, in no particular order
     */
    public Set<AbstractBeamInter> getBeams ()
    {
        return beams;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the group id (unique within the measure, starting from 1).
     *
     * @return the group id
     */
    public int getId ()
    {
        return id;
    }

    //--------------//
    // isMultiStaff //
    //--------------//
    /**
     * Tell whether this beam group is linked to more than one staff.
     *
     * @return the multiStaff
     */
    public boolean isMultiStaff ()
    {
        return multiStaff;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{BeamGroup#").append(id).append(" beams[");

        if (beams != null) {
            for (AbstractBeamInter beam : beams) {
                sb.append(beam).append(" ");
            }
        }

        sb.append("]").append("}");

        return sb.toString();
    }
}
