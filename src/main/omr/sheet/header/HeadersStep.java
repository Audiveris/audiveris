//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      H e a d e r s S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.header;

import omr.sheet.SystemInfo;
import omr.sheet.header.HeaderBuilder;

import omr.step.AbstractSystemStep;
import omr.step.StepException;

/**
 * Class {@code HeadersStep} implements <b>HEADERS</b> step, which handles the beginning
 * of every staff in a system.
 *
 * @author Hervé Bitteur
 */
public class HeadersStep
        extends AbstractSystemStep<Void>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code HeadersStep} object.
     */
    public HeadersStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
            throws StepException
    {
        new HeaderBuilder(system).processHeader(); // -> Staff clef + key + time
    }
}
