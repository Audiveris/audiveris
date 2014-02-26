//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S l u r s S t e p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Sheet;
import omr.sheet.skeleton.SlursBuilder;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code SlursStep} retrieves all slurs of a sheet.
 *
 * @author Hervé Bitteur
 */
public class SlursStep
        extends AbstractStep
{
    //~ Constructors -------------------------------------------------------------------------------

    //-----------//
    // SlursStep //
    //-----------//
    /**
     * Creates a new SlursStep object.
     */
    public SlursStep ()
    {
        super(
                Steps.SLURS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                SKELETON_TAB,
                "Retrieve slurs & ties");
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
        new SlursBuilder(sheet).buildSlurs(); // -> slurs
    }
}
