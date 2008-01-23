//----------------------------------------------------------------------------//
//                                                                            //
//                              P l a y T a s k                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.script;

import omr.score.midi.MidiActions;

import omr.sheet.Sheet;

import omr.step.StepException;

import javax.xml.bind.annotation.*;

/**
 * Class <code>PlayTask</code> is a script task which plays the Midi sequence
 * of a score
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
public class PlayTask
    extends Task
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

    //-----//
    // run //
    //-----//
    @Override
    public void run (Sheet sheet)
        throws StepException
    {
        MidiActions.play(sheet.getScore(), null);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return " play";
    }
}
