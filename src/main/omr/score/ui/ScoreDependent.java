//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e D e p e n d e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.log.Logger;

import omr.selection.MouseMovement;
import omr.selection.SheetEvent;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetDependent;

import omr.step.Steps;

/**
 * Class {@code ScoreDependent} handles the dependency on score
 * availability
 *
 * @author Hervé Bitteur
 */
public abstract class ScoreDependent
    extends SheetDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreDependent.class);

    /** Is a Score available */
    protected static final String SCORE_AVAILABLE = "scoreAvailable";

    /** Is a Score merged (and ready for export, play, midi, etc) */
    protected static final String SCORE_MERGED = "scoreMerged";

    //~ Instance fields --------------------------------------------------------

    /** Indicates whether there is a current score */
    protected boolean scoreAvailable = false;

    /** Indicates whether the current score has been merged */
    protected boolean scoreMerged = false;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // ScoreDependent //
    //----------------//
    /**
     * Creates a new ScoreDependent object.
     */
    protected ScoreDependent ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // isScoreAvailable //
    //------------------//
    /**
     * Getter for scoreAvailable property
     * @return the current property value
     */
    public boolean isScoreAvailable ()
    {
        return scoreAvailable;
    }

    //---------------//
    // isScoreMerged //
    //---------------//
    /**
     * Getter for scoreMerged property
     * @return the current property value
     */
    public boolean isScoreMerged ()
    {
        return scoreMerged;
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Notification of sheet selection (and thus related score if any).
     * @param event the notified sheet event
     */
    @Override
    public void onEvent (SheetEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            super.onEvent(event);

            Sheet sheet = event.getData();
            setScoreAvailable((sheet != null) && (sheet.getScore() != null));
            setScoreMerged(
                (sheet != null) && sheet.isDone(Steps.valueOf(Steps.SCORE)));
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
    }

    //-------------------//
    // setScoreAvailable //
    //-------------------//
    /**
     * Setter for scoreAvailable property
     * @param scoreAvailable the new property value
     */
    public void setScoreAvailable (boolean scoreAvailable)
    {
        boolean oldValue = this.scoreAvailable;
        this.scoreAvailable = scoreAvailable;
        firePropertyChange(SCORE_AVAILABLE, oldValue, this.scoreAvailable);
    }

    //----------------//
    // setScoreMerged //
    //----------------//
    /**
     * Setter for scoreMerged property
     * @param scoreMerged the new property value
     */
    public void setScoreMerged (boolean scoreMerged)
    {
        boolean oldValue = this.scoreMerged;
        this.scoreMerged = scoreMerged;
        firePropertyChange(SCORE_MERGED, oldValue, this.scoreMerged);
    }
}
