//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    V o i c e D i s t a n c e                                   //
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.RestChordInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code VoiceDistance} provides a kind of distance between two chords,
 * to evaluate if they could to belong to the same voice.
 * <p>
 * This abstract class provides two concrete implementations:
 * <ul>
 * <li><b>Separated</b> for parts made of a single staff or of several staves clearly
 * separated.
 * <li><b>Merged</b> for parts made of two staves nearly merged.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public abstract class VoiceDistance
{

    private static final Logger logger = LoggerFactory.getLogger(VoiceDistance.class);

    /** Threshold for no voice link. */
    public static final int NO_LINK = 40;

    public static final int INCOMPATIBLE = 10_000; // Forbidden

    protected final Scale scale;

    /**
     * Report the distance between two chords, assumed to be in different time slots.
     *
     * @param left    chord on the left
     * @param right   chord on the right
     * @param details (optional output) if not null, to be populated by distance details
     * @return the evaluated voice-wise 'distance' between these two chords
     */
    public abstract int getDistance (AbstractChordInter left,
                                     AbstractChordInter right,
                                     StringBuilder details);

    protected VoiceDistance (Scale scale)
    {
        this.scale = scale;
    }

    //-----------//
    // Separated //
    //-----------//
    /**
     * This distance is meant for the standard case, where a part contains one or
     * several staves with sufficient vertical space between the part staves.
     * <p>
     * This is the typical case for 1-staff part, for a grand piano staff (2 staves)
     * and for organ (2 or 3 staves).
     */
    public static class Separated
            extends VoiceDistance
    {

        private static final int NOT_A_REST = 5;

        private static final int NEW_IN_STAFF = 10; //25;

        private static final int STAFF_DIFF = 20; //30;

        private static final int STEM_1_DIFF = 3;

        private static final int STEM_2_DIFF = 6;

        public Separated (Scale scale)
        {
            super(scale);
        }

        @Override
        public int getDistance (AbstractChordInter left,
                                AbstractChordInter right,
                                StringBuilder details)
        {
            // Different assigned voices?
            if ((right.getVoice() != null) && (left.getVoice() != null)
                        && (right.getVoice() != left.getVoice())) {
                return INCOMPATIBLE;
            }

            // Different staves? (beware: some chords embrace two staves, hence we use topStaff)
            int dStaff = 0;

            if (right.getTopStaff() != left.getTopStaff()) {
                // Different (top) staves, but are they in same part?
                if (right.getPart() != left.getPart()) {
                    return INCOMPATIBLE;
                } else {
                    // Map with a stem imposes to be on same staff
                    if ((left instanceof RestChordInter) || (right instanceof RestChordInter)) {
                        return INCOMPATIBLE;
                    }

                    dStaff = STAFF_DIFF;
                }
            }

            // Penalty for a chord which originated in a different staff
            int nis = (left.getVoice().getStartingStaff() != right.getTopStaff()) ? NEW_IN_STAFF
                    : 0;

            // A rest is a placeholder, hence bonus for rest (implemented by penalty on non-rest)
            int nar = 0;

            if (left instanceof HeadChordInter) {
                nar += NOT_A_REST;
            }

            // Pitch difference (even in different staves)
            int p1 = left.getLeadingNote().getAbsolutePitch();
            int p2 = right.getLeadingNote().getAbsolutePitch();
            int dp = Math.abs(p2 - p1);

            // Stem direction difference
            int dif = Math.abs(right.getStemDir() - left.getStemDir());
            int dStem = (dif == 2) ? STEM_2_DIFF : ((dif == 1) ? STEM_1_DIFF : 0);

            final int d = dStem + dStaff + nis + nar + dp;

            if (left.isVip() && right.isVip()) {
                logger.info(
                        "VIP VoiceDistance.Separated ch#{} ch#{} {} {}",
                        left.getId(),
                        right.getId(),
                        d,
                        detailsOf(dStaff, dStem, nis, nar, dp));
            }

            if (details != null) {
                details.append(detailsOf(dStaff, dStem, nis, nar, dp));
            }

            return d;
        }

        private String detailsOf (int dStaff,
                                  int dStem,
                                  int nis,
                                  int nar,
                                  int dp)
        {
            return String.format(
                    "dStaff=%d dStem=%d nis=%d nar=%d dPitch=%d",
                    dStaff,
                    dStem,
                    nis,
                    nar,
                    dp);
        }
    }

    //--------//
    // Merged //
    //--------//
    /**
     * This specific distance was initially defined for lute scores, where the
     * instrument is noted on two staves vertically very close to each other.
     * <p>
     * Whereas in standard piano grand staff, the C4 ledger line is just <b>logically</b> shared by
     * the upper and the lower staves, here this line is really <b>physically</b> shared, resulting
     * into a kind of <i>merged</i> grand staff of 5 + 1 + 5 = 11 lines.
     * <p>
     * The staff on which a note lies is rather secondary.
     * Instead, chords with upward stem are considered as part of <i>high</i> voices, and chords
     * with downward stem as part of <i>low</i> voices.
     * <p>
     * On the diagram below, notice how voices are rather decoupled from containing staves.
     * <p>
     * <img src="doc-files/MergedGrandStaff.png" alt="MergedGrandStaff diagram">
     */
    public static class Merged
            extends VoiceDistance
    {

        private static final int NOT_A_REST = 5;

        private static final int NEW_IN_STAFF = 2;

        private static final int STAFF_DIFF = 2;

        private static final int STEM_1_DIFF = 10;

        private static final int STEM_2_DIFF = 100;

        public Merged (Scale scale)
        {
            super(scale);
        }

        @Override
        public int getDistance (AbstractChordInter left,
                                AbstractChordInter right,
                                StringBuilder details)
        {
            // Different assigned voices?
            if ((right.getVoice() != null) && (left.getVoice() != null)
                        && (right.getVoice() != left.getVoice())) {
                return INCOMPATIBLE;
            }

            // Not is same part?
            if (right.getPart() != left.getPart()) {
                return INCOMPATIBLE;
            }

            // Stem direction difference
            int dif = Math.abs(right.getStemDir() - left.getStemDir());
            int dStem = (dif == 2) ? STEM_2_DIFF : ((dif == 1) ? STEM_1_DIFF : 0);

            // Different staves?
            int dStaff = (right.getTopStaff() != left.getTopStaff()) ? STAFF_DIFF : 0;

            // Penalty for a chord which originated in a different staff
            int nis = (left.getVoice().getStartingStaff() != right.getTopStaff()) ? NEW_IN_STAFF
                    : 0;

            // A rest is a placeholder, hence bonus for rest (implemented by penalty on non-rest)
            int nar = (left instanceof HeadChordInter) ? NOT_A_REST : 0;

            // Pitch difference
            int dy = Math.abs(right.getHeadLocation().y - left.getHeadLocation().y) / scale
                    .getInterline();

            final int d = dStem + dStaff + nis + nar + dy;

            if (left.isVip() && right.isVip()) {
                logger.info(
                        "VIP VoiceDistance.Merged ch#{} ch#{} {} {}",
                        left.getId(),
                        right.getId(),
                        d,
                        detailsOf(dStaff, dStem, nis, nar, dy));
            }

            if (details != null) {
                details.append(detailsOf(dStaff, dStem, nis, nar, dy));
            }

            return d;
        }

        private String detailsOf (int dStaff,
                                  int dStem,
                                  int nis,
                                  int nar,
                                  int dy)
        {
            return String.format(
                    "dStaff=%d dStem=%d nis=%d nar=%d dy=%d",
                    dStaff,
                    dStem,
                    nis,
                    nar,
                    dy);
        }
    }
}
