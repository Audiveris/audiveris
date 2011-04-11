//----------------------------------------------------------------------------//
//                                                                            //
//                            F r a m e s S t e p                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.staff.FramesBuilder;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code FramesStep} determine the systems frames of the picture.
 * This is a temporary attempt, to be evaluated and refined is successful.
 *
 * @author Hervé Bitteur
 */
public class FramesStep
    extends AbstractStep
{
    //~ Constructors -----------------------------------------------------------

    //------------//
    // FramesStep //
    //------------//
    /**
     * Creates a new FramesStep object.
     */
    public FramesStep ()
    {
        super(
            Steps.FRAMES,
            Level.SHEET_LEVEL,
            Mandatory.MANDATORY,
            Redoable.NON_REDOABLE,
            FRAMES_TAB,
            "Retrieve all systems frames");
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
        new FramesBuilder(sheet).buildInfo();
    }
}
