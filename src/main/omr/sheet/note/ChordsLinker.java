//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C h o r d s L i n k e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.note;

import omr.sheet.SystemInfo;
import omr.sheet.beam.BeamGroup;
import omr.sheet.rhythm.MeasureStack;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractBeamInter;
import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.Inter;
import omr.sig.inter.StemInter;
import omr.sig.relation.BeamStemRelation;
import omr.sig.relation.HeadStemRelation;
import omr.sig.relation.Relation;

import omr.util.Navigable;

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
                    AbstractHeadInter head = (AbstractHeadInter) sig.getOppositeInter(stem, hs);
                    AbstractChordInter chord = head.getChord();
                    chord.addBeam(beam);
                    beam.addChord(chord);
                }
            }
        }
    }
}
