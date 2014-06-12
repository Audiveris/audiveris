//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         K e y s S t e p                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.SystemInfo;

/**
 * Class {@code KeysStep} implements <b>KEYS</b> step, which handles the beginning of
 * each staff (DMZ).
 *
 * @author Hervé Bitteur
 */
public class KeysStep
        extends AbstractSystemStep
{
    //~ Constructors -------------------------------------------------------------------------------

    //----------//
    // KeysStep //
    //----------//
    /**
     * Creates a new KeysStep object.
     */
    public KeysStep ()
    {
        super(
                Steps.KEYS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve staff keys");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.keysBuilder.buildKeys(); // -> Staff key
    }
}
