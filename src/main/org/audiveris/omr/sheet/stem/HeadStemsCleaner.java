//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 H e a d S t e m s C l e a n e r                                //
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
package org.audiveris.omr.sheet.stem;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.Exclusion;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import static org.audiveris.omr.util.HorizontalSide.LEFT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>HeadStemsCleaner</code> checks and cleans up an ensemble of stems around a head.
 *
 * @author Hervé Bitteur
 */
public class HeadStemsCleaner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(HeadStemsCleaner.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final HeadInter head;

    private final SIGraph sig;

    private final List<HeadStemRelation> rels = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a <code>HeadStemsCleaner</code> object.
     *
     * @param head the underlying head
     */
    public HeadStemsCleaner (HeadInter head)
    {
        this.head = head;

        sig = head.getSig();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // check //
    //-------//
    /**
     * Check the collection of stems around this head.
     *
     * @param stems the stems to check and clean
     */
    public void check (List<Inter> stems)
    {
        // Retrieve stem relations
        rels.clear();

        for (Inter stem : stems) {
            HeadStemRelation rel = (HeadStemRelation) sig.getRelation(
                    head,
                    stem,
                    HeadStemRelation.class);

            if (rel != null) {
                rels.add(rel);
            }
        }

        if (head.isVip() && (rels.size() > 1)) {
            logger.info("VIP {} with multiple stems {}", head, stems);
        }

        // Get down to a maximum of 2 stems
        while (rels.size() > 2) {
            // Discard the relation with smallest contribution
            HeadStemRelation discarded = discardWorstContribution(rels);

            if (discarded != null) {
                sig.removeEdge(discarded);
                rels.remove(discarded);
            }
        }

        // Do we still have a conflict to solve?
        if (rels.size() == 2) {
            // If not canonical, try to discard one of the stem links
            if (!isCanonicalShare()) {
                // Discard the relation with smallest contribution
                HeadStemRelation discarded = discardWorstContribution(rels);

                if (discarded != null) {
                    sig.removeEdge(discarded);
                    rels.remove(discarded);
                }
            }
        }
    }

    //--------------------------//
    // discardWorstContribution //
    //--------------------------//
    private HeadStemRelation discardWorstContribution (List<HeadStemRelation> relations)
    {
        double worstContrib = Double.MAX_VALUE;
        HeadStemRelation worstRel = null;

        for (HeadStemRelation rel : relations) {
            final StemInter stem = (StemInter) sig.getEdgeTarget(rel);
            final double ratio = rel.getTargetRatio();
            final double contrib = stem.getGrade() * (ratio - 1);

            if (worstContrib > contrib) {
                worstContrib = contrib;
                worstRel = rel;
            }
        }

        return worstRel;
    }

    //-----------------//
    // discardLargeGap //
    //-----------------//
    /**
     * Discard the stem link with largest significant y gap, if any.
     *
     * @param relations the set of head-stem relations to purge
     * @return the link to stem with largest significant y gap,or null
     */
    private HeadStemRelation discardLargeGap (List<HeadStemRelation> relations)
    {
        double worstGap = 0;
        HeadStemRelation worstRel = null;

        for (HeadStemRelation rel : relations) {
            double yGap = rel.getDy();

            if (worstGap < yGap) {
                worstGap = yGap;
                worstRel = rel;
            }
        }

        if (worstGap > constants.yGapTiny.getValue()) {
            if (head.isVip()) {
                logger.info("VIP {} discarding gap {}", head, sig.getEdgeTarget(worstRel));
            }

            return worstRel;
        } else {
            return null;
        }
    }

    //-------------------//
    // discardWeakerStem //
    //-------------------//
    /**
     * Discard the link to the stem with lower intrinsic grade.
     *
     * @return the link to discard
     */
    private HeadStemRelation discardWeakerStem ()
    {
        double worstGrade = Double.MAX_VALUE;
        HeadStemRelation worstRel = null;
        StemInter worstStem = null;

        for (HeadStemRelation rel : rels) {
            StemInter stem = (StemInter) sig.getEdgeTarget(rel);
            double grade = stem.getGrade();

            if (grade < worstGrade) {
                worstGrade = grade;
                worstStem = stem;
                worstRel = rel;
            }
        }

        if (worstRel != null) {
            if (head.isVip()) {
                logger.info("VIP {} discarding weaker {}", head, sig.getEdgeTarget(worstRel));
            }

            // If this false relation is really invading, use exclusion
            if (worstRel.isInvading()) {
                if (worstStem.isVip() || head.isVip()) {
                    logger.info("VIP invasion between {} & {}", head, worstStem);
                }

                sig.insertExclusion(head, worstStem, Exclusion.ExclusionCause.OVERLAP);
            }
        }

        return worstRel;
    }

    //------------------//
    // isCanonicalShare //
    //------------------//
    /**
     * Check whether this is the canonical "shared" configuration.
     * <p>
     * For this test, we cannot trust stem extensions and must stay with physical stem
     * limits.
     *
     * @return true if canonical
     */
    private boolean isCanonicalShare ()
    {
        StemInter leftStem = null;
        StemInter rightStem = null;

        for (HeadStemRelation rel : rels) {
            if (rel.getDy() > constants.yGapTiny.getValue()) {
                return false;
            }

            if (rel.getHeadSide() == LEFT) {
                leftStem = (StemInter) sig.getOppositeInter(head, rel);
            } else {
                rightStem = (StemInter) sig.getOppositeInter(head, rel);
            }
        }

        if ((leftStem == null) || (rightStem == null)) {
            return false;
        }

        return HeadStemRelation.isCanonicalShare(leftStem, head, rightStem);
    }

    //------------//
    // preferBeam //
    //------------//
    /**
     * Give preference to a stem linked to a beam, if any.
     *
     * @return stem link to be discarded if such was found
     */
    private HeadStemRelation preferBeam ()
    {
        final List<HeadStemRelation> toBeam = new ArrayList<>();
        final List<HeadStemRelation> notToBeam = new ArrayList<>();

        for (HeadStemRelation rel : rels) {
            StemInter stem = (StemInter) sig.getEdgeTarget(rel);

            if (stem.getBeams().isEmpty()) {
                notToBeam.add(rel);
            } else {
                toBeam.add(rel);
            }
        }

        if (toBeam.isEmpty() || notToBeam.isEmpty()) {
            return null;
        }

        // Remove one relation out of 'notToBeam' collection
        HeadStemRelation discarded;

        if (notToBeam.size() > 1) {
            discarded = discardLargeGap(notToBeam);

            if (discarded == null) {
                discarded = discardWeakerStem();
            }
        } else {
            discarded = notToBeam.get(0);
        }

        return discarded;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction yGapTiny = new Scale.Fraction(
                0.2, // 0.1,
                "Maximum vertical tiny gap between stem & head");
    }
}
