//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C h o r d s L i n k e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.List;

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

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ChordsLinker.class);

    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** System sig. */
    @Navigable(false)
    private final SIGraph sig;

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

    //-----------------//
    // checkBeamChords //
    //-----------------//
    /**
     * Now that chords are known, re-check the connections between beams and chords.
     * <p>
     * This is meant to detect false beam-stem connections.
     * Conflicting chords are detected via their overlapping ratio in abscissa.
     * <p>
     * Which conflicting chord must be disconnected from the beam?
     * Not a beam side chord.
     * The one with weaker connection.
     * <p>
     * TODO: Perhaps choose the one in opposite direction WRT chord majority?
     */
    public void checkBeamChords ()
    {
        final double maxRatio = constants.maxAbscissaOverlapRatio.getValue();

        for (Inter inter : system.getSig().inters(BeamInter.class)) {
            if (inter.isVip()) {
                logger.info("VIP checkBeamChords for {}", inter);
            }

            final BeamInter beam = (BeamInter) inter;
            final List<AbstractChordInter> chords = beam.getChords(); // Sorted by center abscissa

            AbstractChordInter prevChord = null;

            for (AbstractChordInter chord : chords) {
                final Rectangle bounds = chord.getBounds();

                if (prevChord != null) {
                    final int overlap = GeoUtil.xOverlap(prevChord.getBounds(), bounds);
                    final double ratio = (double) overlap / bounds.width;

                    if (ratio > maxRatio) {
                        logger.info("{} Overlapping {} {} vs {}", beam, ratio, prevChord, chord);

                        StemInter prevStem = prevChord.getStem();
                        BeamStemRelation prevRel = (BeamStemRelation) sig.getRelation(
                                beam,
                                prevStem,
                                BeamStemRelation.class);

                        StemInter stem = chord.getStem();
                        BeamStemRelation rel = (BeamStemRelation) sig.getRelation(
                                beam,
                                stem,
                                BeamStemRelation.class);

                        final BeamStemRelation guiltyRel;

                        if (prevRel.getBeamPortion() == BeamPortion.LEFT) {
                            guiltyRel = rel; // Keep beam side chord
                        } else if (rel.getBeamPortion() == BeamPortion.RIGHT) {
                            guiltyRel = prevRel; // Keep beam side chord
                        } else {
                            // Use connection grade
                            guiltyRel = (prevRel.getGrade() < rel.getGrade()) ? prevRel : rel;
                        }

                        sig.removeEdge(guiltyRel);

                        AbstractChordInter guiltyChord = (guiltyRel == prevRel) ? prevChord : chord;
                        logger.info("{} disconnected from {}", guiltyChord, beam);

                        // Adjust which is the "current" chord
                        if (guiltyRel == rel) {
                            chord = prevChord;
                        }
                    }
                }

                prevChord = chord;
            }
        }
    }

    //------------//
    // linkChords //
    //------------//
    /**
     * Allocate beam groups per measure.
     */
    public void linkChords ()
    {
        for (MeasureStack stack : system.getStacks()) {
            for (Measure measure : stack.getMeasures()) {
                BeamGroupInter.populateMeasure(measure, true); // True for checkGroupSplit
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio maxAbscissaOverlapRatio = new Constant.Ratio(
                0.25,
                "Maximum abscissa relative overlap ratio between chords of a beam");
    }
}
