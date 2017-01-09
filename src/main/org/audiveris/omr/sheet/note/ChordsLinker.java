//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C h o r d s L i n k e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.sheet.note;

import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.beam.BeamGroup;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Class {@code ChordsLinker} works at system level to handle relations between chords
 * and other entities.
 * <p>
 * These relationships can be addressed only when ALL system chord candidates have been retrieved.
 *
 * @author Hervé Bitteur
 */
public class ChordsLinker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ChordsLinker.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** System SIG. */
    private final SIGraph sig;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ChordsLinker} object.
     *
     * @param system the dedicated system
     */
    public ChordsLinker (SystemInfo system)
    {
        this.system = system;
        sig = system.getSig();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // linkChords //
    //------------//
    public void linkChords ()
    {
        // Handle beam relationships
        linkBeams();

        // Allocate beam groups
        for (MeasureStack stack : system.getMeasureStacks()) {
            BeamGroup.populate(stack);
        }
    }

    //-----------//
    // linkBeams //
    //-----------//
    private void linkBeams ()
    {
        List<Inter> beams = sig.inters(AbstractBeamInter.class);

        for (Inter inter : beams) {
            AbstractBeamInter beam = (AbstractBeamInter) inter;
            Set<Relation> bsRels = sig.getRelations(beam, BeamStemRelation.class);

            for (Relation bs : bsRels) {
                StemInter stem = (StemInter) sig.getOppositeInter(beam, bs);
                Set<Relation> hsRels = sig.getRelations(stem, HeadStemRelation.class);

                for (Relation hs : hsRels) {
                    HeadInter head = (HeadInter) sig.getOppositeInter(stem, hs);
                    AbstractChordInter chord = head.getChord();
                    chord.addBeam(beam);
                    beam.addChord(chord);
                }
            }
        }
    }
}
