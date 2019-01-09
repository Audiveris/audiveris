//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                          C h o r d A r p e g g i a t o R e l a t i o n                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import org.audiveris.omr.sig.inter.ArpeggiatoInter;
import org.audiveris.omr.sig.inter.Inter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ChordArpeggiatoRelation} represents a relation between a (head) chord
 * and an arpeggiato.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "chord-arpeggiato")
public class ChordArpeggiatoRelation
        extends Support
{

    private static final Constants constants = new Constants();

    /**
     * Creates a new {@code ArpeggiatoChordRelation} object.
     *
     * @param grade relation quality
     */
    public ChordArpeggiatoRelation (double grade)
    {
        super(grade);
    }

    /**
     * Creates a new {@code ArpeggiatoChordRelation} object.
     */
    public ChordArpeggiatoRelation ()
    {
        super();
    }

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final ArpeggiatoInter arpeggiato = (ArpeggiatoInter) e.getEdgeTarget();
        arpeggiato.checkAbnormal();
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
        return true;
    }

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final ArpeggiatoInter arpeggiato = (ArpeggiatoInter) e.getEdgeTarget();

        if (!arpeggiato.isRemoved()) {
            arpeggiato.checkAbnormal();
        }
    }

    @Override
    protected double getSourceCoeff ()
    {
        return constants.arpeggiatoSupportCoeff.getValue();
    }

    //----------------//
    // getXGapMaximum //
    //----------------//
    public static Scale.Fraction getXGapMaximum (boolean manual)
    {
        return manual ? constants.xGapMaxManual : constants.xGapMax;
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio arpeggiatoSupportCoeff = new Constant.Ratio(
                0.5,
                "Supporting coeff for (source) arpeggiato");

        private final Scale.Fraction xGapMax = new Scale.Fraction(
                1.5,
                "Maximum horizontal gap between arpeggiato & chord");

        private final Scale.Fraction xGapMaxManual = new Scale.Fraction(
                2.5,
                "Maximum manual horizontal gap between arpeggiato & chord");
    }
}
