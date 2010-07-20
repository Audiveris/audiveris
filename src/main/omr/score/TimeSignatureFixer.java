//----------------------------------------------------------------------------//
//                                                                            //
//                    T i m e S i g n a t u r e F i x e r                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.log.Logger;

import omr.math.Rational;

import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.entity.TimeSignature;
import omr.score.entity.Voice;
import omr.score.visitor.AbstractScoreVisitor;

import omr.util.TreeNode;

import java.util.*;
import omr.glyph.Shape;

/**
 * Class <code>TimeSignatureFixer</code> can visit the score hierarchy to
 * check whether each of the time signatures are consistent with most of
 * measures intrinsic time signature.
 *
 * @author Hervé Bitteur
 */
public class TimeSignatureFixer
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        TimeSignatureFixer.class);

    /** Used to sort integers by decreasing value */
    protected static final Comparator<Integer> reverseIntComparator = new Comparator<Integer>() {
        public int compare (Integer e1,
                            Integer e2)
        {
            return e2 - e1;
        }
    };


    //~ Constructors -----------------------------------------------------------

    //--------------------//
    // TimeSignatureFixer //
    //--------------------//
    /**
     * Creates a new TimeSignatureFixer object.
     */
    public TimeSignatureFixer ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // visit Score //
    //-------------//
    /**
     * Score hierarchy entry point
     *
     * @param score visit the score to export
     * @return false, since no further processing is required after this node
     */
    @Override
    public boolean visit (Score score)
    {
        // We cannot rely on standard browsing part by part, since we need to 
        // address all measures (of same Id) in a row, regardless of their 
        // containing part
        ScoreSystem system = score.getFirstSystem();
        SystemPart  part = system.getFirstPart();
        Measure     measure = part.getFirstMeasure();

        // Measure that starts a range of measures with an explicit time sig
        Measure startMeasure = null;

        // Measure that ends the range
        // Right before another time sig, or last measure of the score
        Measure stopMeasure = null;

        while (measure != null) {
            if (hasTimeSig(measure)) {
                if (startMeasure != null) {
                    // Complete the ongoing time sig analysis
                    checkTimeSigs(startMeasure, stopMeasure);
                }

                // Start a new analysis
                startMeasure = measure;
            }

            stopMeasure = measure;
            measure = measure.getFollowing();
        }

        if (startMeasure != null) {
            // Complete the ongoing time sig analysis
            checkTimeSigs(startMeasure, stopMeasure);
        }

        // Don't go the standard way (part per part)
        return false;
    }

    //---------------//
    // checkTimeSigs //
    //---------------//
    /**
     * Perform the analysis on the provided range of measures, retrieving the
     * most significant intrinsic time sig as determined by measures chords.
     * Based on this "intrinsic" time information, modify the explicit time
     * signatures accordingly.
     *
     * @param startMeasure beginning of the measure range
     * @param stopMeasure end of the measure range
     */
    private void checkTimeSigs (Measure startMeasure,
                                Measure stopMeasure)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "checkTimeSigs on measure range " + startMeasure.getId() +
                ".." + stopMeasure.getId());
        }

        // Retrieve the best possible time signature(s)
        SortedMap<Integer, Rational> bestSigs = retrieveBestSigs(
            startMeasure,
            stopMeasure);

        if (!bestSigs.isEmpty()) {
            Rational bestRational = bestSigs.get(bestSigs.firstKey());

            if (logger.isFineEnabled()) {
                logger.fine("Best sig: " + bestRational);
            }

            for (Staff.SystemIterator sit = new Staff.SystemIterator(
                startMeasure); sit.hasNext();) {
                Staff         staff = sit.next();
                Measure       measure = sit.getMeasure();
                TimeSignature sig = measure.getTimeSignature(staff);

                if (sig != null) {
                    Rational rational = sig.getRational();

                    if (!rational.equals(bestRational)) {
                        logger.info(
                            "Measure#" + measure.getId() + " " +
                            staff.getContextString() + "T" + staff.getId() +
                            " " + rational + "->" + bestRational);
                        sig.modify(null, bestRational);
                    }
                }
            }
        } else {
            if (logger.isFineEnabled()) {
                logger.fine("No best sig!");
            }
        }
    }

    //------------//
    // hasTimeSig //
    //------------//
    /**
     * Check whether the provided measure contains at least one explicit time
     * signature
     *
     * @param measure the provided measure (in fact we care only about the
     * measure id, regardless of the part)
     * @return true if a time sig exists in some staff of the measure
     */
    private boolean hasTimeSig (Measure measure)
    {
        for (Staff.SystemIterator sit = new Staff.SystemIterator(measure);
             sit.hasNext();) {
            Staff         staff = sit.next();
            TimeSignature sig = sit.getMeasure()
                                   .getTimeSignature(staff);

            if (sig != null) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Measure#" + measure.getId() + " " +
                        staff.getContextString() + "T" + staff.getId() + " " +
                        sig);
                }

                return true;
            }
        }

        return false;
    }

    //------------------//
    // retrieveBestSigs //
    //------------------//
    /**
     * By inspecting each voice in the provided measure range, determine the
     * best intrinsic time signatures
     *
     * @param startMeasure beginning of the measure range
     * @param stopMeasure end of the measure range
     * @return a map, sorted by decreasing count, of possible time signatures
     */
    private SortedMap<Integer, Rational> retrieveBestSigs (Measure startMeasure,
                                                           Measure stopMeasure)
    {
        // Retrieve the significant measure informations
        Map<Rational, Integer> sigs = new LinkedHashMap<Rational, Integer>();
        Measure                m = startMeasure;
        int                    mIndex = m.getParent()
                                         .getChildren()
                                         .indexOf(m);

        // Loop on measure range
        while (true) {
            // Retrieve info
            if (logger.isFineEnabled()) {
                logger.fine("Checking measure#" + m.getId());
            }

            ScoreSystem system = m.getSystem();

            for (TreeNode pNode : system.getParts()) {
                SystemPart part = (SystemPart) pNode;
                Measure    measure = (Measure) part.getMeasures()
                                                   .get(mIndex);

                for (Voice voice : measure.getVoices()) {
                    Rational rational = voice.getInferredTimeSignature();

                    if (logger.isFineEnabled()) {
                        logger.fine("Voice#" + voice.getId() + ": " + rational);
                    }

                    if (rational != null) {
                        // Update histogram
                        Integer sum = sigs.get(rational);

                        if (sum == null) {
                            sum = 1;
                        } else {
                            sum += 1;
                        }

                        sigs.put(rational, sum);
                    }
                }
            }

            // Are we through?
            if (m == stopMeasure) {
                break;
            } else {
                // Move to next measure
                m = m.getFollowing();
                mIndex = m.getParent()
                          .getChildren()
                          .indexOf(m);
            }
        }

        // Sort info by decreasing counts
        SortedMap<Integer, Rational> bestSigs = new TreeMap<Integer, Rational>(
            reverseIntComparator);

        for (Map.Entry<Rational, Integer> entry : sigs.entrySet()) {
            bestSigs.put(entry.getValue(), entry.getKey());
        }

        return bestSigs;
    }
}
