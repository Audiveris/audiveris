//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e D e p e n d e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.selection.MouseMovement;
import omr.selection.SheetEvent;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetDependent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(
            ScoreDependent.class);

    /** Is a Score available. */
    protected static final String SCORE_AVAILABLE = "scoreAvailable";

    /** Is the Score idle. (available, but not being processed by a step) */
    protected static final String SCORE_IDLE = "scoreIdle";

    //~ Instance fields --------------------------------------------------------
    //
    /** Indicates whether there is a current score. */
    protected boolean scoreAvailable = false;

    /** Indicates whether there the current score is non busy. */
    protected boolean scoreIdle = false;

    //~ Constructors -----------------------------------------------------------
    //
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
    //
    //------------------//
    // isScoreAvailable //
    //------------------//
    /**
     * Getter for scoreAvailable property
     *
     * @return the current property value
     */
    public boolean isScoreAvailable ()
    {
        return scoreAvailable;
    }

    //-------------//
    // isScoreIdle //
    //-------------//
    /**
     * Getter for scoreIdle property
     *
     * @return the current property value
     */
    public boolean isScoreIdle ()
    {
        return scoreIdle;
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Notification of sheet selection (and thus related score if any).
     *
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

            // This updates sheetAvailable
            super.onEvent(event);

            Sheet sheet = event.getData();

            // Update scoreAvailable
            setScoreAvailable((sheet != null) && (sheet.getScore() != null));

            // Update scoreIdle
            if (isScoreAvailable()) {
                setScoreIdle(sheet.getScore().isIdle());
            } else {
                setScoreIdle(false);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //-------------------//
    // setScoreAvailable //
    //-------------------//
    /**
     * Setter for scoreAvailable property
     *
     * @param scoreAvailable the new property value
     */
    public void setScoreAvailable (boolean scoreAvailable)
    {
        boolean oldValue = this.scoreAvailable;
        this.scoreAvailable = scoreAvailable;
        firePropertyChange(SCORE_AVAILABLE, oldValue, this.scoreAvailable);
    }

    //--------------//
    // setScoreIdle //
    //--------------//
    /**
     * Setter for scoreIdle property
     *
     * @param scoreIdle the new property value
     */
    public void setScoreIdle (boolean scoreIdle)
    {
        boolean oldValue = this.scoreIdle;
        this.scoreIdle = scoreIdle;
        firePropertyChange(SCORE_IDLE, oldValue, this.scoreIdle);
    }
}
