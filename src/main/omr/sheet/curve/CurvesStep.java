//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       C u r v e s S t e p                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.AbstractStep;
import omr.step.StepException;

import java.util.Collection;

/**
 * Class {@code CurvesStep} retrieves all curves (slurs, wedges, endings) of a sheet.
 *
 * @author Hervé Bitteur
 */
public class CurvesStep
        extends AbstractStep
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SlursStep object.
     */
    public CurvesStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // doit //
    //------//
    @Override
    protected void doit (Collection<SystemInfo> systems,
                         Sheet sheet)
            throws StepException
    {
        new Curves(sheet).buildCurves();
    }
}
