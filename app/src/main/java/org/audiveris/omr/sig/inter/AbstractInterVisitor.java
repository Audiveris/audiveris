//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A b s t r a c t I n t e r V i s i t o r                            //
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
 * Class <code>AbstractInterVisitor</code> is a nearly void implementation of
 * {@link InterVisitor}, meant to ease the coding of concrete sub-classes.
 * <p>
 * Some default re-directions are implemented here.
 * <p>
 * Methods are listed by class alphabetical order for easier manual browsing.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractInterVisitor
        implements InterVisitor
{
    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public void visit (AbstractBeamInter inter)
    {
    }

    @Override
    public void visit (AbstractChordInter inter)
    {
    }

    @Override
    public void visit (AbstractFlagInter inter)
    {
    }

    @Override
    public void visit (AbstractNumberInter inter)
    {
    }

    @Override
    public void visit (AlterInter inter)
    {
        visit((AbstractPitchedInter) inter);
    }

    @Override
    public void visit (ArpeggiatoInter inter)
    {
    }

    @Override
    public void visit (AugmentationDotInter inter)
    {
        visit((Inter) inter);
    }

    @Override
    public void visit (BarConnectorInter inter)
    {
    }

    @Override
    public void visit (BarlineInter inter)
    {
    }

    @Override
    public void visit (BeamGroupInter inter)
    {
    }

    @Override
    public void visit (BeatUnitInter inter)
    {
        visit((Inter) inter);
    }

    @Override
    public void visit (BraceInter inter)
    {
    }

    @Override
    public void visit (BracketConnectorInter inter)
    {
    }

    @Override
    public void visit (BracketInter inter)
    {
    }

    @Override
    public void visit (ClefInter inter)
    {
    }

    @Override
    public void visit (EndingInter inter)
    {
    }

    @Override
    public void visit (GraceChordInter inter)
    {
        visit((AbstractChordInter) inter);
    }

    @Override
    public void visit (HeadInter inter)
    {
    }

    @Override
    public void visit (Inter inter)
    {
    }

    @Override
    public void visit (KeyAlterInter inter)
    {
    }

    @Override
    public void visit (KeyInter inter)
    {
    }

    @Override
    public void visit (LedgerInter inter)
    {
    }

    @Override
    public void visit (MultipleRestInter inter)
    {
    }

    @Override
    public void visit (MetronomeInter inter)
    {
    }

    @Override
    public void visit (OctaveShiftInter inter)
    {
    }

    @Override
    public void visit (RestInter inter)
    {
        visit((AbstractNoteInter) inter);
    }

    @Override
    public void visit (SentenceInter inter)
    {
    }

    @Override
    public void visit (SlurInter inter)
    {
    }

    @Override
    public void visit (StaffBarlineInter inter)
    {
    }

    @Override
    public void visit (StemInter inter)
    {
    }

    @Override
    public void visit (TimeCustomInter inter)
    {
    }

    @Override
    public void visit (TimePairInter inter)
    {
    }

    @Override
    public void visit (TimeWholeInter inter)
    {
    }

    @Override
    public void visit (VerticalSerifInter inter)
    {
    }

    @Override
    public void visit (WedgeInter inter)
    {
    }

    @Override
    public void visit (WordInter inter)
    {
    }
}
