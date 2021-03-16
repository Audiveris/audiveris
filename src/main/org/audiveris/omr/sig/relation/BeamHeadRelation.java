//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B e a m H e a d R e l a t i o n                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BeamHeadRelation} is meant to boost cross support between beams and heads.
 * <p>
 * Use of BeamHeadRelation:
 * <ul>
 * <li>Small beams support small black heads,
 * <li>Standard beams support black heads (not void),
 * <li>Heads connected on a beam side are significantly supported.
 * </ul>
 * Use of Exclusion:
 * <ul>
 * <li>Small beams exclude standard heads,
 * <li>Standard beams exclude small heads.
 * </ul>
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "beam-head")
public class BeamHeadRelation
        extends Support
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BeamHeadRelation.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Is head connected on beam side?. */
    @XmlAttribute(name = "on-beam-side")
    private boolean onBeamSide;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BeamHeadRelation} object.
     *
     * @param grade      quality of relation
     * @param onBeamSide true for head on beam side
     */
    public BeamHeadRelation (double grade,
                             boolean onBeamSide)
    {
        super(grade);
        this.onBeamSide = onBeamSide;
    }

    /**
     * Creates a new {@code BeamHeadRelation} object.
     *
     * @param grade quality of relation
     */
    public BeamHeadRelation (double grade)
    {
        this(grade, false);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BeamHeadRelation ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        return false;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        return false;
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return onBeamSide ? constants.headSideSupportCoeff.getValue()
                : constants.headSupportCoeff.getValue();
    }

    //--------------//
    // isOnBeamSide //
    //--------------//
    /**
     * Report whether head is connected on beam side.
     *
     * @return the onBeamSide
     */
    public boolean isOnBeamSide ()
    {
        return onBeamSide;
    }

    //---------------//
    // setOnBeamSide //
    //---------------//
    /**
     * Set if head is connected on beam side.
     *
     * @param onBeamSide the onBeamSide to set
     */
    public void setOnBeamSide (boolean onBeamSide)
    {
        this.onBeamSide = onBeamSide;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio headSupportCoeff = new Constant.Ratio(
                0.75,
                "Supporting coeff for (target) head");

        private final Constant.Ratio headSideSupportCoeff = new Constant.Ratio(
                5.0,
                "Supporting coeff for side (target) head");
    }
}
