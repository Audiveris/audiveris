//----------------------------------------------------------------------------//
//                                                                            //
//                         M i d i W r i t e T a s k                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.script;

import omr.log.Logger;

import omr.score.ScoreManager;

import omr.sheet.Sheet;

import omr.step.StepException;

import java.io.File;

import javax.xml.bind.annotation.*;

/**
 * Class <code>MidiWriteTask</code> is a script task which writes the Midi file
 * of a score
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
public class MidiWriteTask
    extends Task
{
    //~ Instance fields --------------------------------------------------------

    /** The file used for export */
    @XmlAttribute
    private String path;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // MidiWriteTask //
    //---------------//
    /**
     * Create a task to write the midi file of a sheet
     *
     * @param path the full path of the midi file
     */
    public MidiWriteTask (String path)
    {
        this.path = path;
    }

    //---------------//
    // MidiWriteTask //
    //---------------//
    /** No-arg constructor needed by JAXB */
    private MidiWriteTask ()
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
        try {
            ScoreManager.getInstance()
                        .midiWrite(sheet.getScore(), new File(path));
        } catch (Exception ex) {
            Logger.getLogger(MidiWriteTask.class.getName())
                  .warning("Midi write failed", ex);
        }
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return " write " + path;
    }
}
