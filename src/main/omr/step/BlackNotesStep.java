//----------------------------------------------------------------------------//
//                                                                            //
//                         B l a c k N o t e s S t e p                        //
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
 * Class {@code BlackNotesStep} implements BLACK_NOTES step.
 *
 * @author Hervé Bitteur
 */
public class BlackNotesStep
        extends AbstractSystemStep
{
    //~ Constructors -----------------------------------------------------------

    //----------------//
    // BlackNotesStep //
    //----------------//
    /**
     * Creates a new BlackNotesStep object.
     */
    public BlackNotesStep ()
    {
        super(
                Steps.BLACK_NOTES,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve black note heads");
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.blackNotesBuilder.buildBlackHeads(); // -> Black heads
    }
}
