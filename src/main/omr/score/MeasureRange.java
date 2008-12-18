//----------------------------------------------------------------------------//
//                                                                            //
//                          M e a s u r e R a n g e                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;

import omr.util.TreeNode;

import java.util.ListIterator;

/**
 * Class <code>MeasureRange</code> encapsulates a range of measures, to ease the
 * playing or the exporting of just a range of measures
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class MeasureRange
{
    //~ Instance fields --------------------------------------------------------

    /** Related score */
    private final Score score;

    /** Id of first measure of the range */
    private final int firstId;

    /** Id of last measure of the range */
    private final int lastId;

    /** Cached data */
    private boolean boundsComputed;
    private ScoreSystem firstSystem;
    private ScoreSystem lastSystem;
    private Measure     firstMeasure;
    private Measure     lastMeasure;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // MeasureRange //
    //--------------//
    /**
     * Creates a new MeasureRange object.
     *
     * @param score the related score instance
     * @param firstId id of first measure
     * @param lastId id of last measure, which cannot be less than firstId
     */
    public MeasureRange (Score score,
                         int   firstId,
                         int   lastId)
    {
        this.score = score;
        this.firstId = firstId;
        this.lastId = lastId;
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // getFirstId //
    //------------//
    /**
     * Report the id of first measure
     *
     * @return id of first measure
     */
    public int getFirstId ()
    {
        return firstId;
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

    //-----------//
    // getLastId //
    //-----------//
    /**
     * Report the id of last measure
     *
     * @return id of last measure
     */
    public int getLastId ()
    {
        return lastId;
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
     * Checks whether the provided id is within the range of measure ids
     *
     * @param id the measure id to check
     * @return true if id is within the range, false otherwise
     */
    public boolean contains (int id)
    {
        return (id >= firstId) && (id <= lastId);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("measures[")
          .append(firstId)
          .append("..")
          .append(lastId)
          .append("]");

        return sb.toString();
    }

    //-------------//
    // checkBounds //
    //-------------//
    private final void checkBounds ()
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
    private void computeFirsts ()
    {
        for (TreeNode sn : score.getSystems()) {
            ScoreSystem system = (ScoreSystem) sn;

            for (TreeNode pn : system.getParts()) {
                SystemPart part = (SystemPart) pn;

                for (TreeNode mn : part.getMeasures()) {
                    Measure measure = (Measure) mn;

                    if (measure.getId() >= firstId) {
                        firstSystem = system;
                        firstMeasure = measure;

                        return;
                    }
                }
            }
        }
    }

    //--------------//
    // computeLasts //
    //--------------//
    private void computeLasts ()
    {
        for (ListIterator sit = score.getSystems()
                                     .listIterator(score.getSystems().size());
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

                    if (measure.getId() <= lastId) {
                        lastSystem = system;
                        lastMeasure = measure;

                        return;
                    }
                }
            }
        }
    }
}
