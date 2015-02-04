//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  E x p o r t S h e e t S t e p                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.BookManager;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import static omr.step.Step.DATA_TAB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code ExportSheetStep}
 *
 * @author Hervé Bitteur
 */
public class ExportSheetStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ExportSheetStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ExportStep object.
     */
    public ExportSheetStep ()
    {
        super(
                Steps.EXPORT_SHEET,
                Level.SHEET_LEVEL,
                Mandatory.OPTIONAL,
                DATA_TAB,
                "Export the current sheet to MusicXML file");
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
        BookManager.getInstance().export(sheet, null, null, ExportStep.useCompression());
    }
}
