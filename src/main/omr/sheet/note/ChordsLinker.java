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

import omr.sheet.Staff;
import omr.sheet.SystemInfo;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractBeamInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.ChordInter;
import omr.sig.inter.FermataInter;
import omr.sig.inter.Inter;
import omr.sig.inter.StemInter;
import omr.sig.relation.BeamStemRelation;
import omr.sig.relation.HeadStemRelation;
import omr.sig.relation.Relation;

import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

    /** Chords in system, organized by staff. */
    private final Map<Staff, List<ChordInter>> chordMap = new TreeMap<Staff, List<ChordInter>>(
            Staff.byId);

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
        // Sort retrieved chords by "closest" staff
        getChords();

        // Handle fermata relationships
        linkFermatas();

        // Handle beam relationships
        linkBeams();
    }

    //-----------//
    // getChords //
    //-----------//
    private void getChords ()
    {
        // Abscissa-ordered list of chords in each staff
        for (Staff staff : system.getStaves()) {
            List<ChordInter> chords = new ArrayList<ChordInter>();
            chordMap.put(staff, chords);

            List<Inter> chordInters = sig.inters(staff, ChordInter.class);
            Collections.sort(chordInters, Inter.byAbscissa);

            for (Inter inter : chordInters) {
                chords.add((ChordInter) inter);
            }
        }
    }

    //-----------//
    // linkBeams // TODO: probably useless when beams are no longer recorded in chords
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
                    ChordInter chord = head.getChord();
                    chord.addBeam(beam);
                    beam.addChord(chord);
                }
            }
        }
    }

    //--------------//
    // linkFermatas //
    //--------------//
    private void linkFermatas ()
    {
        List<Inter> fermatas = sig.inters(FermataInter.class);

        for (Inter inter : fermatas) {
            FermataInter fermata = (FermataInter) inter;

            // Look for a chord related to this fermata
            List<ChordInter> chords = chordMap.get(fermata.getStaff());

            if (!fermata.linkWithChords(chords)) {
                fermata.delete();
            }
        }
    }
}
