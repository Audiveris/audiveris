//----------------------------------------------------------------------------//
//                                                                            //
//                         M i d i W r i t e T a s k                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.score.ScoresManager;

import omr.sheet.Sheet;

import java.io.File;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code MidiWriteTask} writes the Midi file of a score
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class MidiWriteTask
    extends ScriptTask
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

    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
        throws Exception
    {
        ScoresManager.getInstance()
                     .midiWrite(
            sheet.getScore(),
            (path != null) ? new File(path) : null);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return " write " + path + super.internalsString();
    }
}
