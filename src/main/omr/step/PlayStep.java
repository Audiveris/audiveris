//----------------------------------------------------------------------------//
//                                                                            //
//                              P l a y S t e p                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.log.Logger;

import omr.score.midi.MidiActions;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code PlayStep} plays the whole score
 *
 * @author Hervé Bitteur
 */
public class PlayStep
    extends AbstractStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(PlayStep.class);

    //~ Constructors -----------------------------------------------------------

    //----------//
    // PlayStep //
    //----------//
    /**
     * Creates a new PlayStep object.
     */
    public PlayStep ()
    {
        super(
            Steps.PLAY,
            Level.SCORE_LEVEL,
            Mandatory.OPTIONAL,
            Redoable.REDOABLE,
            GLYPHS_TAB,
            "Play the whole score");
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> systems,
                      Sheet                  sheet)
        throws StepException
    {
        new MidiActions.PlayTask(sheet.getScore(), null).execute();
    }
}
