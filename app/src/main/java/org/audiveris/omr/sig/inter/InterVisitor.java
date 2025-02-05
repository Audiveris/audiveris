//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I n t e r V i s i t o r                                   //
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
package org.audiveris.omr.sig.inter;

/**
 * Interface <code>InterVisitor</code> is used to visit any instance of shape interpretation.
 * <p>
 * Implementation root {@link AbstractInterVisitor} implements some default re-directions.
 * <p>
 * Methods are listed by class alphabetical order for easier manual browsing.
 *
 * @author Hervé Bitteur
 */
public interface InterVisitor
{
    //~ Methods ------------------------------------------------------------------------------------

    void visit (AbstractBeamInter inter);

    void visit (AbstractChordInter inter);

    void visit (AbstractFlagInter inter);

    void visit (AbstractNumberInter inter);

    void visit (AlterInter inter);

    void visit (ArpeggiatoInter inter);

    void visit (AugmentationDotInter inter);

    void visit (BarConnectorInter inter);

    void visit (BarlineInter inter);

    void visit (BeamGroupInter inter);

    void visit (BeatUnitInter inter);

    void visit (BraceInter inter);

    void visit (BracketConnectorInter inter);

    void visit (BracketInter inter);

    void visit (ClefInter inter);

    void visit (EndingInter inter);

    void visit (GraceChordInter inter);

    void visit (HeadInter inter);

    void visit (Inter inter);

    void visit (KeyAlterInter inter);

    void visit (KeyInter inter);

    void visit (LedgerInter inter);

    void visit (MetronomeInter inter);

    void visit (MultipleRestInter inter);

    void visit (OctaveShiftInter inter);

    void visit (RestInter inter);

    void visit (SentenceInter inter);

    void visit (SlurInter inter);

    void visit (StaffBarlineInter inter);

    void visit (StemInter inter);

    void visit (TimeCustomInter inter);

    void visit (TimePairInter inter);

    void visit (TimeWholeInter inter);

    void visit (VerticalSerifInter inter);

    void visit (WedgeInter inter);

    void visit (WordInter inter);
}
