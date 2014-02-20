//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B i n a r y S t e p                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Picture;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code BinaryStep} implements <b>BINARY</b> step, which binarizes the initial
 * sheet image, using proper filter, to come up with a black & white image.
 *
 * @author Hervé Bitteur
 */
public class BinaryStep
        extends AbstractStep
{
    //~ Constructors -------------------------------------------------------------------------------

    //------------//
    // BinaryStep //
    //------------//
    /**
     * Creates a new BinaryStep object.
     */
    public BinaryStep ()
    {
        super(
                Steps.BINARY,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                BINARY_TAB,
                "Binarize the initial sheet image");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> unused,
                      Sheet sheet)
            throws StepException
    {
        // Trigger the binarization, and cache the resulting source
        sheet.getPicture().getSource(Picture.SourceKey.BINARY);
    }
}
