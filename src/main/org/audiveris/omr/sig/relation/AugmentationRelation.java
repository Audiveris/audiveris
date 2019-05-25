//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A u g m e n t a t i o n R e l a t i o n                            //
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
package org.audiveris.omr.sig.relation;

import java.util.List;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.ui.InterController;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code AugmentationRelation} represents the relation between an augmentation
 * dot and the related note (head or rest) instance.
 * <p>
 * NOTA: An augmentation can be considered as linked to at most one note (<b>single target</b>)
 * since the case of pair of mirrored heads (which can indeed be targeted by the same dot)
 * is addressed specifically within {@link InterController#link} method.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "augmentation")
public class AugmentationRelation
        extends AbstractConnection
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AugmentationRelation.class);

    private static final double[] OUT_WEIGHTS = new double[]{
        constants.xOutWeight.getValue(),
        constants.yWeight.getValue()};

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final AugmentationDotInter dot = (AugmentationDotInter) e.getEdgeSource();

        dot.checkAbnormal();

        // Update related chord dot count?
        if (isManual() || dot.isManual()) {
            updateChordDotCount(e.getEdgeTarget());
        }
    }

    @Override
    public Object clone ()
            throws CloneNotSupportedException
    {
        return super.clone(); //To change body of generated methods, choose Tools | Templates.
    }

    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum (boolean manual)
    {
        return manual ? constants.xOutGapMaxManual : constants.xOutGapMax;
    }

    //-------------------//
    // getXOutGapMinimum //
    //-------------------//
    public static Scale.Fraction getXOutGapMinimum (boolean manual)
    {
        return manual ? constants.xOutGapMinManual : constants.xOutGapMin;
    }

    //----------------//
    // getYGapMaximum //
    //----------------//
    public static Scale.Fraction getYGapMaximum (boolean manual)
    {
        return manual ? constants.yGapMaxManual : constants.yGapMax;
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        return true;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        return true; // See explanation in class header
    }

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final AugmentationDotInter dot = (AugmentationDotInter) e.getEdgeSource();

        if (!dot.isRemoved()) {
            dot.checkAbnormal();
        }

        final Inter target = e.getEdgeTarget();

        if (!target.isRemoved()) {
            updateChordDotCount(target);
        }
    }

    //---------------//
    // getOutWeights //
    //---------------//
    @Override
    protected double[] getOutWeights ()
    {
        return OUT_WEIGHTS;
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    /**
     * @return the coefficient used to compute source support ratio
     */
    @Override
    protected double getSourceCoeff ()
    {
        return constants.dotSupportCoeff.getValue();
    }

    @Override
    protected Scale.Fraction getXOutGapMax (boolean manual)
    {
        return getXOutGapMaximum(manual);
    }

    @Override
    protected Scale.Fraction getYGapMax (boolean manual)
    {
        return getYGapMaximum(manual);
    }

    //---------------------//
    // updateChordDotCount //
    //---------------------//
    private void updateChordDotCount (Inter target)
    {
        AbstractNoteInter note = null;

        if (target instanceof AugmentationDotInter) {
            AugmentationDotInter first = (AugmentationDotInter) target;
            List<AbstractNoteInter> notes = first.getAugmentedNotes();

            if (!notes.isEmpty()) {
                note = notes.get(0);
            }
        } else {
            note = (AbstractNoteInter) target;
        }

        if (note != null) {
            AbstractChordInter chord = note.getChord();
            chord.countDots();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio dotSupportCoeff = new Constant.Ratio(
                0.5,
                "Supporting coeff for (source) dot");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                1.3,
                "Maximum horizontal gap between dot center & note/rest reference point");

        private final Scale.Fraction xOutGapMaxManual = new Scale.Fraction(
                2.0,
                "Maximum manual horizontal gap between dot center & note/rest reference point");

        private final Scale.Fraction xOutGapMin = new Scale.Fraction(
                0.25,
                "Minimum horizontal gap between dot center & note/rest reference point");

        private final Scale.Fraction xOutGapMinManual = new Scale.Fraction(
                0.1,
                "Minimum manual horizontal gap between dot center & note/rest reference point");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                0.8,
                "Maximum vertical gap between dot center & note/rest reference point");

        private final Scale.Fraction yGapMaxManual = new Scale.Fraction(
                1.2,
                "Maximum manual vertical gap between dot center & note/rest reference point");

        private final Constant.Ratio xOutWeight = new Constant.Ratio(
                0,
                "Relative impact weight for xOutGap");

        private final Constant.Ratio yWeight = new Constant.Ratio(
                1,
                "Relative impact weight for yGap");
    }
}
