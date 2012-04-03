//----------------------------------------------------------------------------//
//                                                                            //
//                              M i d i S t e p                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.log.Logger;

import omr.score.ScoresManager;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code MidiStep} midis the whole score
 *
 * @author Hervé Bitteur
 */
public class MidiStep
    extends AbstractStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MidiStep.class);

    //~ Constructors -----------------------------------------------------------

    //----------//
    // MidiStep //
    //----------//
    /**
     * Creates a new MidiStep object.
     */
    public MidiStep ()
    {
        super(
            Steps.MIDI,
            Level.SCORE_LEVEL,
            Mandatory.OPTIONAL,
            Redoable.REDOABLE,
            DATA_TAB,
            "Write the output MIDI file");
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
        try {
            ScoresManager.getInstance()
                         .midiWrite(sheet.getScore(), null);
        } catch (Exception ex) {
            logger.warning("Midi write failed", ex);
        }
    }
}
