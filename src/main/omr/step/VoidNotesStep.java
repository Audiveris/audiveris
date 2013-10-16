//----------------------------------------------------------------------------//
//                                                                            //
//                          V o i d N o t e s S t e p                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.SystemInfo;

/**
 * Class {@code VoidNotesStep} implements VOID_NOTES step.
 *
 * @author Hervé Bitteur
 */
public class VoidNotesStep
        extends AbstractSystemStep
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // VoidNotesStep //
    //---------------//
    /**
     * Creates a new VoidNotesStep object.
     */
    public VoidNotesStep ()
    {
         super(
                Steps.VOID_NOTES,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve void note heads & whole notes");
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.voidNotesBuilder.buildVoidHeads(); // -> Void heads
    }
}
