//----------------------------------------------------------------------------//
//                                                                            //
//                             L i n e s S t e p                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.LinesBuilder;
import omr.sheet.Sheet;
import omr.sheet.StavesBuilder;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code LinesStep} detects and removes all Staff Lines
 *
 * @author Hervé Bitteur
 */
public class LinesStep
    extends AbstractStep
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LinesStep object.
     */
    public LinesStep ()
    {
        super(
            Steps.LINES,
            Level.SHEET_LEVEL,
            Mandatory.MANDATORY,
            Redoable.NON_REDOABLE,
            LINES_TAB,
            "Retrieve and erase staff lines");
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void doit (Collection<SystemInfo> unused,
                      Sheet                  sheet)
        throws StepException
    {
        StavesBuilder builder = new LinesBuilder(sheet);
        sheet.setStavesBuilder(builder);
        builder.buildInfo();
    }
}
