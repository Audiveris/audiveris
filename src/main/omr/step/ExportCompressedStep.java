//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             E x p o r t C o m p r e s s e d S t e p                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.score.ScoresManager;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import static omr.step.Step.DATA_TAB;

import java.util.Collection;

/**
 * Class {@code ExportCompressedStep} exports the whole score in compressed form.
 *
 * @author Hervé Bitteur
 */
public class ExportCompressedStep
        extends AbstractStep
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ExportCompressedStep object.
     */
    public ExportCompressedStep ()
    {
        super(
                Steps.EXPORT_COMPRESSED,
                Level.SCORE_LEVEL,
                Mandatory.OPTIONAL,
                DATA_TAB,
                "Export the score to compressed MusicXML file");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> systems,
                      Sheet sheet)
            throws StepException
    {
        ScoresManager.getInstance().export(sheet.getScore(), null, null, true);
    }
}
