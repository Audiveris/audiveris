//----------------------------------------------------------------------------//
//                                                                            //
//                             S c a l e S t e p                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code ScaleStep} determines the general scale of the sheet, based on
 * the mean distance between staff lines
 *
 * @author Hervé Bitteur
 */
public class ScaleStep
    extends AbstractStep
{
    //~ Constructors -----------------------------------------------------------

    //-----------//
    // ScaleStep //
    //-----------//
    /**
     * Creates a new ScaleStep object.
     */
    public ScaleStep ()
    {
        super(
            Steps.SCALE,
            Level.SHEET_LEVEL,
            Mandatory.MANDATORY,
            Redoable.NON_REDOABLE,
            PICTURE_TAB,
            "Compute general scale");
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> unused,
                      Sheet                  sheet)
        throws StepException
    {
        Scale scale = new Scale(sheet);
        sheet.setScale(scale);
    }
}
