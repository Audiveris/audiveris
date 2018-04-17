//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B e a m S t e m R e l a t i o n                                //
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
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.beam.BeamGroup;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;
import static org.audiveris.omr.sig.relation.BeamPortion.*;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BeamStemRelation} implements the geographic link between a beam
 * (or beam hook) and a stem.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "beam-stem")
public class BeamStemRelation
        extends AbstractStemConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            BeamStemRelation.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Which portion of beam is used?. */
    @XmlAttribute(name = "beam-portion")
    private BeamPortion beamPortion;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BeamStemRelation} object.
     */
    public BeamStemRelation ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // getXInGapMaximum //
    //------------------//
    public static Scale.Fraction getXInGapMaximum (boolean manual)
    {
        return manual ? constants.xInGapMaxManual : constants.xInGapMax;
    }

    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum (boolean manual)
    {
        return manual ? constants.xOutGapMaxManual : constants.xOutGapMax;
    }

    //----------------//
    // getYGapMaximum //
    //----------------//
    public static Scale.Fraction getYGapMaximum (boolean manual)
    {
        return manual ? constants.yGapMaxManual : constants.yGapMax;
    }

    //-------//
    // added //
    //-------//
    /**
     * Populate beam portion if needed and update the chord(s) if any that use this stem.
     * <p>
     * In the rare case where a stem is shared by several chords, the beam connection applies to
     * all these chords.
     *
     * @param e edge change event
     */
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final AbstractBeamInter beam = (AbstractBeamInter) e.getEdgeSource();
        final StemInter stem = (StemInter) e.getEdgeTarget();

        if (beamPortion == null) {
            if (beam instanceof BeamHookInter) {
                beamPortion = (beam.getCenter().x < stem.getCenter().x) ? RIGHT : LEFT;
            } else {
                int xStem = stem.getCenter().x;
                Scale scale = stem.getSig().getSystem().getSheet().getScale();
                int maxDx = scale.toPixels(getXInGapMaximum(false));
                double left = beam.getMedian().getX1();
                double right = beam.getMedian().getX2();

                if (xStem < (left + maxDx)) {
                    beamPortion = LEFT;
                } else if (xStem > (right - maxDx)) {
                    beamPortion = RIGHT;
                } else {
                    beamPortion = CENTER;
                }
            }
        }

        for (HeadChordInter chord : stem.getChords()) {
            chord.invalidateCache();

            // Include in proper BeamGroup set within containing Measure?
            SystemInfo system = stem.getSig().getSystem();
            Staff staff1 = chord.getTopStaff();
            MeasureStack stack = system.getStackAt(stem.getCenter());

            if (stack != null) {
                Measure measure = stack.getMeasureAt(staff1);
                BeamGroup.includeBeam(beam, measure);
            }
        }

        beam.checkAbnormal();
    }

    //----------------//
    // getBeamPortion //
    //----------------//
    /**
     * @return the beamPortion
     */
    public BeamPortion getBeamPortion ()
    {
        return beamPortion;
    }

    //----------------//
    // getStemPortion //
    //----------------//
    @Override
    public StemPortion getStemPortion (Inter source,
                                       Line2D stemLine,
                                       Scale scale)
    {
        double midStem = (stemLine.getY1() + stemLine.getY2()) / 2;

        return (extensionPoint.getY() < midStem) ? StemPortion.STEM_TOP : StemPortion.STEM_BOTTOM;
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

    /**
     * @param beamPortion the beamPortion to set
     */
    public void setBeamPortion (BeamPortion beamPortion)
    {
        this.beamPortion = beamPortion;
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        return constants.beamSupportCoeff.getValue();
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return constants.stemSupportCoeff.getValue();
    }

    //--------------//
    // getXInGapMax //
    //--------------//
    @Override
    protected Scale.Fraction getXInGapMax (boolean manual)
    {
        return getXInGapMaximum(manual);
    }

    //---------------//
    // getXOutGapMax //
    //---------------//
    @Override
    protected Scale.Fraction getXOutGapMax (boolean manual)
    {
        return getXOutGapMaximum(manual);
    }

    //------------//
    // getYGapMax //
    //------------//
    @Override
    protected Scale.Fraction getYGapMax (boolean manual)
    {
        return getYGapMaximum(manual);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" ").append(beamPortion);

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

        private final Constant.Ratio beamSupportCoeff = new Constant.Ratio(
                4,
                "Value for source (beam) coeff in support formula");

        private final Constant.Ratio stemSupportCoeff = new Constant.Ratio(
                2,
                "Value for target (stem) coeff in support formula");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                0.8,
                "Maximum vertical gap between stem & beam");

        private final Scale.Fraction yGapMaxManual = new Scale.Fraction(
                1.2,
                "Maximum manual vertical gap between stem & beam");

        private final Scale.Fraction xInGapMax = new Scale.Fraction(
                0.5,
                "Maximum horizontal overlap between stem & beam");

        private final Scale.Fraction xInGapMaxManual = new Scale.Fraction(
                0.75,
                "Maximum manual horizontal overlap between stem & beam");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                0.1,
                "Maximum horizontal gap between stem & beam");

        private final Scale.Fraction xOutGapMaxManual = new Scale.Fraction(
                0.2,
                "Maximum manual horizontal gap between stem & beam");
    }
}
