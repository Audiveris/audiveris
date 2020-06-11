//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A b s t r a c t I n t e r V i s i t o r                            //
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
package org.audiveris.omr.sig.inter;

/**
 * Class {@code AbstractInterVisitor} is a void implementation for {@link InterVisitor}
 * meant to ease the coding of concrete sub-classes.
 * <p>
 * For easier browsing, keep visit() methods sorted by alphabetical order of Inter class.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractInterVisitor
        implements InterVisitor
{

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
    public void visit (AlterInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (ArpeggiatoInter inter)
    {
    }

    @Override
    public void visit (ArticulationInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (AugmentationDotInter inter)
    {
        visit((Inter) inter); // Redirection by default
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
    public void visit (BeamHookInter inter)
    {
        visit((AbstractBeamInter) inter); // Redirection by default
    }

    @Override
    public void visit (BeamInter inter)
    {
        visit((AbstractBeamInter) inter); // Redirection by default
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
    public void visit (BreathMarkInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (CaesuraInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (ChordNameInter inter)
    {
        visit((WordInter) inter); // Redirection by default
    }

    @Override
    public void visit (ClefInter inter)
    {
    }

    @Override
    public void visit (DynamicsInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (EndingInter inter)
    {
    }

    @Override
    public void visit (FermataArcInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (FermataDotInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (FermataInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (FingeringInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (FlagInter inter)
    {
        visit((AbstractFlagInter) inter); // Redirection by default
    }

    @Override
    public void visit (FretInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (HeadChordInter inter)
    {
        visit((AbstractChordInter) inter); // Redirection by default
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
    public void visit (LyricItemInter inter)
    {
        visit((WordInter) inter); // Redirection by default
    }

    @Override
    public void visit (LyricLineInter inter)
    {
        visit((SentenceInter) inter); // Redirection by default
    }

    @Override
    public void visit (MarkerInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (OrnamentInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (PedalInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (RepeatDotInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (PluckingInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (RestChordInter inter)
    {
        visit((AbstractChordInter) inter); // Redirection by default
    }

    @Override
    public void visit (RestInter inter)
    {
        visit((Inter) inter); // Redirection by default
    }

    @Override
    public void visit (SegmentInter inter)
    {
        visit((Inter) inter); // Redirection by default
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
    public void visit (SmallBeamInter inter)
    {
        visit((AbstractBeamInter) inter); // Redirection by default
    }

    @Override
    public void visit (SmallChordInter inter)
    {
        visit((AbstractChordInter) inter); // Redirection by default
    }

    @Override
    public void visit (SmallFlagInter inter)
    {
        visit((AbstractFlagInter) inter); // Redirection by default
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
    public void visit (TimeNumberInter inter)
    {
        visit((Inter) inter); // Redirection by default
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
    public void visit (TupletInter inter)
    {
        visit((Inter) inter); // Redirection by default
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
