//----------------------------------------------------------------------------//
//                                                                            //
//                              P l a y T a s k                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.score.midi.MidiActions;

import omr.sheet.Sheet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Class {@code PlayTask} plays the Midi sequence of a score
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class PlayTask
    extends ScriptTask
{
    //~ Constructors -----------------------------------------------------------

    //----------//
    // PlayTask //
    //----------//
    /** No-arg constructor needed by JAXB */
    private PlayTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
    {
        // We launch the playing and don't wait for its completion
        new MidiActions.PlayTask(sheet.getScore(), null).execute();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return " play" + super.internalsString();
    }

    //--------------//
    // isRecordable //
    //--------------//
    /**
     * PlayTask is not meant to be recorded each time the user plays the score.
     * However, one can always add a &lt;play/&gt; line, manually in a script.
     * @return false
     */
    @Override
    boolean isRecordable ()
    {
        return false;
    }
}
