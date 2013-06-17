//----------------------------------------------------------------------------//
//                                                                            //
//                             M e a s u r e I d                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.score.Score;

import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ListIterator;

/**
 * Class {@code MeasureId} is a non-mutable class meant to handle the
 * specificities of a measure id, since ids are recorded as {@link PageBased}
 * instances:<ol>
 * <li>Initial ids assigned are page-based and start from 1</li>
 * <li>Final ids, assigned by {@link omr.score.MeasureFixer}, are page-based,
 * start from 1 (or 0 for a pickup measure), and have a special value (Xn) for
 * second half repeats.</li>
 * <li>Ids, as displayed in score view or exported in MusicXML, combine
 * the page-based ids to provide score-based ids.</li>
 * <li>Ids, as used by {@link MeasureRange}, are {@link ScoreBased}
 * instances.</li>
 * </ol>
 */
public abstract class MeasureId
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            MeasureId.class);

    /** Char prefix for a second half id */
    protected static final char SH_CHAR = 'X';

    /** String prefix for a second half id */
    protected static final String SH_STRING = Character.toString(SH_CHAR);

    //~ Instance fields --------------------------------------------------------
    /** Underlying numeric value */
    protected final int value;

    /** Flag for second half */
    protected final boolean secondHalf;

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // MeasureId //
    //-----------//
    /**
     * Creates a new MeasureId object.
     *
     * @param value      The underlying numeric value
     * @param secondHalf True if this is a second repeat half
     */
    private MeasureId (int value,
                       boolean secondHalf)
    {
        this.value = value;
        this.secondHalf = secondHalf;
    }

    //~ Methods ----------------------------------------------------------------
    //------------------//
    // createScoreBased //
    //------------------//
    /**
     * Creates a ScoreBased measure id object.
     *
     * @param strId the score-based id (as visible by the user)
     */
    public static ScoreBased createScoreBased (Score score,
                                               String strId)
    {
        // Check syntax
        String str = strId.trim()
                .toUpperCase();

        if ((str == null) || (str.length() == 0)) {
            throw new IllegalArgumentException("Null or empty Id string");
        }

        char initial = str.charAt(0);
        boolean secondHalf = initial == SH_CHAR;

        int value = secondHalf ? Integer.parseInt(str.substring(1))
                : Integer.parseInt(str);

        return new ScoreBased(score, value, secondHalf);
    }

    //-----------------//
    // retrieveMeasure //
    //-----------------//
    /**
     * Report the measure in the provided score, for which the id matches the
     * provided score-based id
     *
     * @param score        the related score
     * @param scoreBasedId the score-based id to search for
     * @return the measure found, or null if not found
     */
    public static Measure retrieveMeasure (Score score,
                                           ScoreBased scoreBasedId)
    {
        int scoreValue = scoreBasedId.value;
        String strId = scoreBasedId.toString();

        Page page = retrievePage(score, scoreValue);

        if (page != null) {
            int pageIdOffset = score.getMeasureIdOffset(page);
            int pageValue = scoreValue - pageIdOffset;

            for (TreeNode sn : page.getSystems()) {
                ScoreSystem system = (ScoreSystem) sn;
                SystemPart part = system.getFirstPart();

                if (pageValue <= part.getLastMeasure()
                        .getIdValue()) {
                    for (TreeNode mn : part.getMeasures()) {
                        Measure measure = (Measure) mn;

                        if ((pageValue == measure.getIdValue())
                            && strId.equals(measure.getScoreId())) {
                            return measure;
                        }
                    }
                }
            }
        }

        return null;
    }

    //--------------//
    // retrievePage //
    //--------------//
    /**
     * Report the page that contains the measure with provided score-based id
     * value
     *
     * @param score             the score at hand
     * @param scoreBasedIdValue the numeric value of score-based measure id
     * @return the containing page, or null if not found
     */
    public static Page retrievePage (Score score,
                                     int scoreBasedIdValue)
    {
        for (ListIterator<TreeNode> pageIt = score.getPages()
                .listIterator(
                score.getPages().size()); pageIt.hasPrevious();) {
            Page page = (Page) pageIt.previous();

            if (scoreBasedIdValue >= score.getMeasureIdOffset(page)) {
                return page;
            }
        }

        return null;
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------------//
    // MeasureRange // =========================================================
    //--------------//
    /**
     * Class {@code MeasureRange} handles a range of measures score-based ids,
     * to ease the playing or the exporting of just a range of measures.
     */
    public static class MeasureRange
    {
        //~ Instance fields ----------------------------------------------------

        /** Related score */
        private final Score score;

        /** Score-based id of first measure of the range */
        private final ScoreBased firstId;

        /** Score-based id of last measure of the range */
        private final ScoreBased lastId;

        //~ Constructors -------------------------------------------------------
        /**
         * Create a MeasureRange instance from score-based ids
         *
         * @param firstScoreId score-based id of the first measure of the range
         * @param lastScoreId  score-based id of the last measure of the range
         */
        public MeasureRange (ScoreBased firstScoreId,
                             ScoreBased lastScoreId)
        {
            if (firstScoreId.score != lastScoreId.score) {
                throw new IllegalArgumentException("Ids from different scores");
            }

            this.score = firstScoreId.score;
            this.firstId = firstScoreId;
            this.lastId = lastScoreId;
        }

        /**
         * Create a MeasureRange instance from score-based ids provided as
         * strings
         *
         * @param score        the related score
         * @param firstScoreId score-based id of the first measure of the range
         * @param lastScoreId  score-based id of the last measure of the range
         */
        public MeasureRange (Score score,
                             String firstScoreId,
                             String lastScoreId)
        {
            this(
                    createScoreBased(score, firstScoreId),
                    createScoreBased(score, lastScoreId));
        }

        //~ Methods ------------------------------------------------------------
        //----------//
        // contains //
        //----------//
        /**
         * Checks whether the provided page-based measure id is within the
         * measure range
         *
         * @param pageBasedId the page-based measure id to check
         * @return true if id is within the range, false otherwise
         */
        public boolean contains (PageBased pageBasedId)
        {
            ScoreBased scoreBasedId = new ScoreBased(pageBasedId);

            return (scoreBasedId.compareTo(firstId) >= 0)
                   && (scoreBasedId.compareTo(lastId) <= 0);
        }

        //--------//
        // equals //
        //--------//
        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof MeasureRange)) {
                return false;
            } else {
                MeasureRange that = (MeasureRange) obj;

                return (this.score == that.score)
                       && this.firstId.equals(that.firstId)
                       && this.lastId.equals(that.lastId);
            }
        }

        //------------//
        // getFirstId //
        //------------//
        /**
         * Report the id of the first measure in range
         *
         * @return the score-based id of first measure
         */
        public ScoreBased getFirstId ()
        {
            return firstId;
        }

        //---------------//
        // getFirstIndex //
        //---------------//
        /**
         * Report the score-based index of the first measure of the range
         *
         * @return the index (score-based) of the first measure
         */
        public int getFirstIndex ()
        {
            Measure measure = retrieveMeasure(score, firstId);

            return measure.getPageId()
                    .getScoreIndex();
        }

        //-----------//
        // getLastId //
        //-----------//
        /**
         * Report the id of the last measure in range
         *
         * @return the score-based id of last measure
         */
        public ScoreBased getLastId ()
        {
            return lastId;
        }

        //----------//
        // hashCode //
        //----------//
        @Override
        public int hashCode ()
        {
            int hash = 3;
            hash = (89 * hash) + ((score != null) ? score.hashCode() : 0);
            hash = (89 * hash) + ((firstId != null) ? firstId.hashCode() : 0);
            hash = (89 * hash) + ((lastId != null) ? lastId.hashCode() : 0);

            return hash;
        }

        //----------//
        // toString //
        //----------//
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("ids[")
                    .append(firstId)
                    .append("..")
                    .append(lastId)
                    .append("]");

            return sb.toString();
        }
    }

    //-----------//
    // PageBased // ============================================================
    //-----------//
    /**
     * A page-based measure id
     */
    public static class PageBased
            extends MeasureId
    {
        //~ Instance fields ----------------------------------------------------

        /** The related measure */
        protected final Measure measure;

        //~ Constructors -------------------------------------------------------
        public PageBased (Measure measure,
                          int value,
                          boolean secondHalf)
        {
            super(value, secondHalf);
            this.measure = measure;
        }

        public PageBased (Measure measure,
                          PageBased other)
        {
            this(measure, other.value, other.secondHalf);
        }

        //~ Methods ------------------------------------------------------------
        //--------//
        // equals //
        //--------//
        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof PageBased)) {
                return false;
            }

            PageBased that = (PageBased) obj;

            return (this.measure == that.measure)
                   && (this.value == that.value)
                   && (this.secondHalf == that.secondHalf);
        }

        //---------------//
        // getScoreIndex //
        //---------------//
        /**
         * Report the score-based index of this measure
         *
         * @return the score-based measure index
         */
        public int getScoreIndex ()
        {
            Page page = measure.getPage();
            Score score = page.getScore();
            int offset = score.getMeasureOffset(page);

            for (TreeNode sn : page.getSystems()) {
                ScoreSystem system = (ScoreSystem) sn;
                SystemPart part = system.getFirstPart();
                int measureCount = part.getMeasures()
                        .size();

                if (value < (offset + measureCount)) {
                    return measure.getChildIndex() + offset;
                } else {
                    offset += measureCount;
                }
            }

            // This should not happen
            logger.error(
                    "Cannot retrieve score index of page-based measure id {}",
                    this);

            return 0; // To keep the compiler happy
        }

        //----------//
        // hashCode //
        //----------//
        @Override
        public int hashCode ()
        {
            int hash = 5;

            return hash;
        }

        //---------------//
        // toScoreString //
        //---------------//
        /**
         * Present the score-based display (even though the stored
         * value is page-based)
         *
         * @return [X]absId
         */
        public String toScoreString ()
        {
            Page page = measure.getPage();

            int pageMeasureIdOffset = page.getScore()
                    .getMeasureIdOffset(page);

            if (secondHalf) {
                return SH_STRING + (pageMeasureIdOffset + value);
            } else {
                return Integer.toString(pageMeasureIdOffset + value);
            }
        }

        //----------//
        // toString //
        //----------//
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("*");

            if (secondHalf) {
                sb.append(SH_STRING);
            }

            sb.append(value);

            return sb.toString();
        }
    }

    //------------//
    // ScoreBased // ===========================================================
    //------------//
    /**
     * A score-based measure id
     */
    public static class ScoreBased
            extends MeasureId
            implements Comparable<ScoreBased>
    {
        //~ Instance fields ----------------------------------------------------

        /** The containing score */
        private final Score score;

        //~ Constructors -------------------------------------------------------
        public ScoreBased (Score score,
                           int value,
                           boolean secondHalf)
        {
            super(value, secondHalf);
            this.score = score;
        }

        public ScoreBased (PageBased pageBasedId)
        {
            this(
                    pageBasedId.measure.getScore(),
                    pageBasedId.value,
                    pageBasedId.secondHalf);
        }

        //~ Methods ------------------------------------------------------------
        //-----------//
        // compareTo //
        //-----------//
        @Override
        public int compareTo (ScoreBased that)
        {
            if (this.score != that.score) {
                throw new IllegalArgumentException(
                        "Cannot compare ids from different scores");
            }

            int deltaValue = this.value - that.value;

            if (deltaValue != 0) {
                return Integer.signum(deltaValue);
            }

            if (secondHalf && !that.secondHalf) {
                return 1;
            }

            if (!secondHalf && that.secondHalf) {
                return -1;
            }

            return 0;
        }

        //--------//
        // equals //
        //--------//
        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof ScoreBased)) {
                return false;
            }

            ScoreBased that = (ScoreBased) obj;

            return (this.value == that.value)
                   && (this.secondHalf == that.secondHalf);
        }

        //----------//
        // hashCode //
        //----------//
        @Override
        public int hashCode ()
        {
            int hash = 7;

            return hash;
        }

        //----------//
        // toString //
        //----------//
        /**
         * Present the score-based display
         *
         * @return [X]absId
         */
        @Override
        public String toString ()
        {
            return (secondHalf ? SH_STRING : "") + value;
        }
    }
}
