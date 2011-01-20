//----------------------------------------------------------------------------//
//                                                                            //
//                          M e a s u r e R a n g e                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.score.entity.Measure;
import omr.score.entity.Page;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;

import omr.util.TreeNode;

import java.util.ListIterator;

/**
 * Class {@code MeasureRange} encapsulates a range of measures, to ease the
 * playing or the exporting of just a range of measures.
 *
 * @author Hervé Bitteur
 */
public class MeasureRange
{
    //~ Instance fields --------------------------------------------------------

    /** Related score */
    private final Score score;

    /** Score-based index of first measure of the range */
    private final int firstIndex;

    /** Score-based index of last measure of the range */
    private final int lastIndex;

    /** Cached data */
    private boolean boundsComputed = false;
    private Page        firstPage;
    private ScoreSystem firstSystem;
    private Measure     firstMeasure;
    private Page        lastPage;
    private ScoreSystem lastSystem;
    private Measure     lastMeasure;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // MeasureRange //
    //--------------//
    /**
     * Creates a new MeasureRange object.
     *
     * @param score the related score instance
     * @param firstIndex score-based index of first measure
     * @param lastIndex score-based index of last measure, cannot be less than
     * firstIndex
     */
    public MeasureRange (Score score,
                         int   firstIndex,
                         int   lastIndex)
    {
        this.score = score;
        this.firstIndex = firstIndex;
        this.lastIndex = lastIndex;
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getFirstIndex //
    //---------------//
    /**
     * Report the index of first measure
     *
     * @return score-based index of first measure
     */
    public int getFirstIndex ()
    {
        return firstIndex;
    }

    //-----------------//
    // getFirstMeasure //
    //-----------------//
    public Measure getFirstMeasure ()
    {
        checkBounds();

        return firstMeasure;
    }

    //----------------//
    // getFirstSystem //
    //----------------//
    public ScoreSystem getFirstSystem ()
    {
        checkBounds();

        return firstSystem;
    }

    //--------------//
    // getLastIndex //
    //--------------//
    /**
     * Report the index of last measure
     *
     * @return score-based index of last measure
     */
    public int getLastIndex ()
    {
        return lastIndex;
    }

    //----------------//
    // getLastMeasure //
    //----------------//
    public Measure getLastMeasure ()
    {
        checkBounds();

        return lastMeasure;
    }

    //---------------//
    // getLastSystem //
    //---------------//
    public ScoreSystem getLastSystem ()
    {
        checkBounds();

        return lastSystem;
    }

    //----------//
    // contains //
    //----------//
    /**
     * Checks whether the provided index is within the range of measure indices
     *
     * @param index the measure index to check
     * @return true if index is within the range, false otherwise
     */
    public boolean contains (int index)
    {
        return (index >= firstIndex) && (index <= lastIndex);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("measures[")
          .append(firstIndex)
          .append("..")
          .append(lastIndex)
          .append("]");

        return sb.toString();
    }

    //-------------//
    // checkBounds //
    //-------------//
    private void checkBounds ()
    {
        if (!boundsComputed) {
            computeFirsts();
            computeLasts();
            boundsComputed = true;
        }
    }

    //---------------//
    // computeFirsts //
    //---------------//
    /**
     * Compute the first page/system/measure entities for firstIndex
     */
    private void computeFirsts ()
    {
        for (TreeNode pageNode : score.getPages()) {
            Page page = (Page) pageNode;
            int  offset = score.getMeasureOffset(page);

            for (TreeNode sn : page.getSystems()) {
                ScoreSystem system = (ScoreSystem) sn;
                SystemPart  part = system.getFirstPart();
                int         measureCount = part.getMeasures()
                                               .size();

                if (firstIndex < (offset + measureCount)) {
                    Measure measure = (Measure) part.getMeasures()
                                                    .get(firstIndex - offset);
                    firstPage = page;
                    firstSystem = system;
                    firstMeasure = measure;

                    return;
                } else {
                    offset += measureCount;
                }
            }
        }
    }

    //--------------//
    // computeLasts //
    //--------------//
    /**
     * Compute the last page/system/measure entities for lastIndex
     */
    private void computeLasts ()
    {
        for (ListIterator pageIt = score.getPages()
                                        .listIterator(score.getPages().size());
             pageIt.hasPrevious();) {
            Page page = (Page) pageIt.previous();
            int  offset = score.getMeasureOffset(page);

            for (ListIterator sit = page.getSystems()
                                        .listIterator(page.getSystems().size());
                 sit.hasPrevious();) {
                ScoreSystem system = (ScoreSystem) sit.previous();

                for (ListIterator pit = system.getParts()
                                              .listIterator(
                    system.getParts().size()); pit.hasPrevious();) {
                    SystemPart part = (SystemPart) pit.previous();

                    for (ListIterator mit = part.getMeasures()
                                                .listIterator(
                        part.getMeasures().size()); mit.hasPrevious();) {
                        Measure measure = (Measure) mit.previous();

                        if (measure.getIdValue() <= (lastIndex - offset)) {
                            lastPage = page;
                            lastSystem = system;
                            lastMeasure = measure;

                            return;
                        }
                    }
                }
            }
        }
    }
}
