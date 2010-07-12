//----------------------------------------------------------------------------//
//                                                                            //
//                    O m r F r a m e C o n t r o l l e r                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.midi;

import omr.log.Logger;

import omr.ui.MainGui;

import com.xenoage.util.exceptions.InvalidFormatException;
import com.xenoage.zong.core.music.MP;
import com.xenoage.zong.musicxml.types.MxlScorePartwise;
import com.xenoage.zong.player.gui.BasicFrameController;

/**
 * Class <code>OmrFrameController</code> is the Midi controller specific for
 * Audiveris
 *
 * @author Hervé Bitteur
 */
public class OmrFrameController
    extends BasicFrameController
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MidiAgent.class);

    //~ Instance fields --------------------------------------------------------

    // Are we currently playing?
    private boolean playing = false;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new OmrFrameController object.
     * @param view related view
     */
    public OmrFrameController (OmrFrameView view)
    {
        super(view);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // isPlaying //
    //-----------//
    public boolean isPlaying ()
    {
        return playing;
    }

    //--------------//
    // loadDocument //
    //--------------//
    @Override
    public void loadDocument (MxlScorePartwise doc)
        throws InvalidFormatException
    {
        controller.loadScore(doc, true); // true for ignoreErrors
        view.setScoreInfo(controller.getScore());
    }

    //------//
    // play //
    //------//
    @Override
    public void play ()
    {
        // Display the Midi Frame at its usual location
        MainGui.getInstance()
               .show(getFrameView().getFrame());

        // And play
        playing = true;
        super.play();
    }

    //-----------------//
    // playbackStopped //
    //-----------------//
    @Override
    public void playbackStopped (MP position)
    {
        playing = false;
    }
}
