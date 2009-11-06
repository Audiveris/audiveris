//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e D e p e n d e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.log.Logger;

import omr.selection.MouseMovement;
import omr.selection.SheetEvent;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetDependent;

import omr.util.Implement;

import org.bushe.swing.event.EventSubscriber;

/**
 * Class <code>ScoreDependent</code> handles the dependency on score
 * availability
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class ScoreDependent
    extends SheetDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreDependent.class);

    //~ Instance fields --------------------------------------------------------

    /** Indicates whether there is a current score */
    protected boolean scoreAvailable = false;

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
        firePropertyChange("scoreAvailable", oldValue, this.scoreAvailable);
    }

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

    //---------//
    // onEvent //
    //---------//
    /**
     * Notification of sheet selection (and thus related score if any)
     *
     * @param event the notified sheet event
     */
    @Implement(EventSubscriber.class)
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
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
    }
}
