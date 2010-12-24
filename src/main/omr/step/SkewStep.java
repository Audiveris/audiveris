//----------------------------------------------------------------------------//
//                                                                            //
//                              S k e w S t e p                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Sheet;
import omr.sheet.SkewBuilder;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code SkewStep} determine the average skew of the picture, and
 * deskews it if needed
 *
 * @author Hervé Bitteur
 */
public class SkewStep
    extends AbstractStep
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SkewStep object.
     */
    public SkewStep ()
    {
        super(
            Steps.SKEW,
            Level.SHEET_LEVEL,
            Mandatory.MANDATORY,
            Redoable.NON_REDOABLE,
            SKEW_TAB,
            "Detect & remove all Staff Lines");
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void doit (Collection<SystemInfo> unused,
                      Sheet                  sheet)
        throws StepException
    {
        sheet.setSkewBuilder(new SkewBuilder(sheet));
        sheet.setSkew(sheet.getSkewBuilder().buildInfo());
        sheet.getBench()
             .recordSkew(sheet.getSkew().angle());

        //        // If rotated, rescale the sheet
        //        if (sheet.getPicture()
        //                 .isRotated()) {
        //            Steps.rebuildFrom(Steps.valueOf(Steps.SCALE), sheet, null, true);
        //        }
    }
}
