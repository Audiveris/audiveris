//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I n t e r V i s i t o r                                   //
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
package org.audiveris.omr.sig.inter;

/**
 * Interface {@code InterVisitor} is used to visit any instance of shape interpretation.
 * <p>
 * Listed by alphabetic order for easier manual browsing.
 *
 * @author Hervé Bitteur
 */
public interface InterVisitor
{
    //~ Methods ------------------------------------------------------------------------------------

    void visit (AbstractBeamInter inter); // BeamHook, Beam, SmallBeam

    void visit (AbstractChordInter inter); // HeadChord, SmallChord, RestChord

    void visit (AbstractFlagInter inter); // Flag, SmallFlag

    void visit (AlterInter inter);

    void visit (ArpeggiatoInter inter);

    void visit (ArticulationInter inter);

    void visit (AugmentationDotInter inter);

    void visit (BarConnectorInter inter);

    void visit (BarlineInter inter);

    void visit (BeamHookInter inter);

    void visit (BeamInter inter);

    void visit (BraceInter inter);

    void visit (BracketConnectorInter inter);

    void visit (BracketInter inter);

    void visit (BreathMarkInter inter);

    void visit (CaesuraInter inter);

    void visit (ChordNameInter inter);

    void visit (ClefInter inter);

    void visit (DynamicsInter inter);

    void visit (EndingInter inter);

    void visit (FermataArcInter inter);

    void visit (FermataDotInter inter);

    void visit (FermataInter inter);

    void visit (FingeringInter inter);

    void visit (FlagInter inter);

    void visit (FretInter inter);

    void visit (HeadChordInter inter);

    void visit (HeadInter inter);

    void visit (Inter inter); // Pedal, TimeNumber, Rest, Alter, RepeatDot, Articulation
    // AugmentationDot, BreathMark, Caesura, Dynamics
    // FermataArc, FermataDot, Fermata, Fingering, Fret, Marker, Ornament, Plucking, Segment
    // Tuplet

    void visit (KeyAlterInter inter);

    void visit (KeyInter inter);

    void visit (LedgerInter inter);

    void visit (LyricItemInter inter);

    void visit (LyricLineInter inter);

    void visit (MarkerInter inter);

    void visit (OrnamentInter inter);

    void visit (PedalInter inter);

    void visit (RepeatDotInter inter);

    void visit (PluckingInter inter);

    void visit (RestChordInter inter);

    void visit (RestInter inter);

    void visit (SegmentInter inter);

    void visit (SentenceInter inter); // Sentence, LyricLine

    void visit (SlurInter inter);

    void visit (SmallBeamInter inter);

    void visit (SmallChordInter inter);

    void visit (SmallFlagInter inter);

    void visit (StaffBarlineInter inter);

    void visit (StemInter inter);

    void visit (TimeNumberInter inter);

    void visit (TimePairInter inter);

    void visit (TimeWholeInter inter);

    void visit (TupletInter inter);

    void visit (WedgeInter inter);

    void visit (WordInter inter); // Word, ChordName, LyricItem
}
