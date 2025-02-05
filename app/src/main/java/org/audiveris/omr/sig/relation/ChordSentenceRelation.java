//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            C h o r d S e n t e n c e R e l a t i o n                           //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.SentenceInter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>ChordSentenceRelation</code> represents a support relation between a chord
 * and a sentence.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "chord-sentence")
public class ChordSentenceRelation
        extends Support
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Methods ------------------------------------------------------------------------------------

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

    //------------//
    // getXGapMax //
    //------------//
    public static Scale.Fraction getXGapMax ()
    {
        return constants.xGapMax;
    }

    //---------//
    // removed //
    //---------//
    /**
     * {@inheritDoc}.
     * <p>
     * If the chord is being removed (and not the sentence), we try to find out a new chord
     * to be linked with the orphan sentence.
     *
     * @param e the relation event.
     */
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final AbstractChordInter chord = (AbstractChordInter) e.getEdgeSource();
        final SentenceInter sentence = (SentenceInter) e.getEdgeTarget();

        if (chord.isRemoved() && !sentence.isRemoved()) {
            final SystemInfo system = sentence.getSig().getSystem();
            sentence.link(system);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction xGapMax =
                new Scale.Fraction(1.0, "Maximum horizontal gap between chord & sentence");
    }
}
