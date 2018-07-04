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
import org.audiveris.omr.sig.inter.ArpeggiatoInter;
import org.audiveris.omr.sig.inter.Inter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ChordArpeggiatoRelation}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "chord-arpeggiato")
public class ChordArpeggiatoRelation
        extends AbstractSupport
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
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
        arpeggiato.checkAbnormal();
    }

    @Override
    protected double getSourceCoeff ()
    {
        return constants.arpeggiatoSupportCoeff.getValue();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio arpeggiatoSupportCoeff = new Constant.Ratio(
                0.5,
                "Supporting coeff for (source) arpeggiato");
    }
}
