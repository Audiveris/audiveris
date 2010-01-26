//----------------------------------------------------------------------------//
//                                                                            //
//                          O m r F r a m e V i e w                           //
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

import com.xenoage.zong.player.gui.BasicFrameView;

import org.jdesktop.application.ResourceMap;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.WindowConstants;

/**
 * Class <code>OmrFrameView</code> is the specific Midi view for Audiveris
 *
 * @author Herv&eacute Bitteur
 */
public class OmrFrameView
    extends BasicFrameView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(OmrFrameView.class);

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // OmrFrameView //
    //--------------//
    /**
     * Creates a new OmrFrameView object.
     */
    public OmrFrameView ()
    {
        frame.setName("midiFrame");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(380, 115));

        // Resource injection
        ResourceMap resource = MainGui.getInstance()
                                      .getContext()
                                      .getResourceMap(getClass());
        resource.injectComponents(frame);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // getButtonSequence //
    //-------------------//
    /**
     * This is the proper sequence of buttons for Audiveris use
     * @return the sequence of button names
     */
    @Override
    protected List<String> getButtonSequence ()
    {
        return Arrays.asList(
            PLAY,
            PAUSE,
            STOP,
            VOLUME,
            VOLUME_SLIDER,
            AUDIO,
            INFO);
    }

    //----------//
    // loadIcon //
    //----------//
    /**
     * Do not load icons here, let the SAF injection work
     * @param filename not used
     * @return null
     */
    @Override
    protected ImageIcon loadIcon (String filename)
    {
        return null;
    }
}
