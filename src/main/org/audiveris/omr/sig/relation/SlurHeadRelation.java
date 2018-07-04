//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S l u r H e a d R e l a t i o n                                //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SlurHeadRelation} represents a link between a slur and one of the two
 * embraced note heads.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "slur-head")
public class SlurHeadRelation
        extends AbstractSupport
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SlurHeadRelation.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Left or right side of the slur. */
    @XmlAttribute(name = "side")
    private HorizontalSide side;

    // Transient data
    //---------------
    //
    /** Euclidean distance from slur end to chord middle vertical. */
    private double euclidean;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SlurNoteRelation} object.
     *
     * @param side the left or right side of the slur
     */
    public SlurHeadRelation (HorizontalSide side)
    {
        this.side = side;
    }

    /**
     * No-arg constructor meant for JAXB and user allocation.
     */
    public SlurHeadRelation ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // added //
    //-------//
    /**
     * Populate side if needed.
     *
     * @param e edge change event
     */
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final SlurInter slur = (SlurInter) e.getEdgeSource();

        if (side == null) {
            final Inter head = e.getEdgeTarget();
            side = (slur.getCenter().x < head.getCenter().x) ? RIGHT : LEFT;
        }

        if (isManual() || slur.isManual()) {
            final SIGraph sig = slur.getSig();
            final Step latestStep = sig.getSystem().getSheet().getStub().getLatestStep();

            if (latestStep.compareTo(Step.LINKS) >= 0) {
                // Check for a tie
                List<Inter> systemHeadChords = sig.inters(HeadChordInter.class); // Costly...
                slur.checkStaffTie(systemHeadChords);
            }
        }

        slur.checkAbnormal();
    }

    //--------------//
    // getEuclidean //
    //--------------//
    /**
     * @return the euclidean distance
     */
    public double getEuclidean ()
    {
        return euclidean;
    }

    //---------//
    // getSide //
    //---------//
    /**
     * @return the side
     */
    public HorizontalSide getSide ()
    {
        return side;
    }

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

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final SlurInter slur = (SlurInter) e.getEdgeSource();
        slur.checkAbnormal();
    }

    //--------------//
    // setEuclidean //
    //--------------//
    /**
     * @param euclidean the euclidean distance to set
     */
    public void setEuclidean (double euclidean)
    {
        this.euclidean = euclidean;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return super.toString() + "/" + side;
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        return constants.slurSupportCoeff.getValue();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio slurSupportCoeff = new Constant.Ratio(
                5,
                "Value for (source) slur coeff in support formula");
    }
}
