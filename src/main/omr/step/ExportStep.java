//----------------------------------------------------------------------------//
//                                                                            //
//                            E x p o r t S t e p                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.score.ScoresManager;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code ExportStep} exports the whole score
 *
 * @author Hervé Bitteur
 */
public class ExportStep
        extends AbstractStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            ExportStep.class);

    //~ Constructors -----------------------------------------------------------
    //------------//
    // ExportStep //
    //------------//
    /**
     * Creates a new ExportStep object.
     */
    public ExportStep ()
    {
        super(
                Steps.EXPORT,
                Level.SCORE_LEVEL,
                Mandatory.OPTIONAL,
                DATA_TAB,
                "Export the score to MusicXML file");
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> systems,
                      Sheet sheet)
            throws StepException
    {
        ScoresManager.getInstance()
                .export(sheet.getScore(), null, null);
    }
}
