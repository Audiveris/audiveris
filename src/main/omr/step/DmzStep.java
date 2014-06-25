//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          D m z S t e p                                         //
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
 * Class {@code DmzStep} implements <b>DMZ</b> step, which handles the beginning of
 * every staff in a system.
 *
 * @author Hervé Bitteur
 */
public class DmzStep
        extends AbstractSystemStep
{
    //~ Constructors -------------------------------------------------------------------------------

    //---------//
    // DmzStep //
    //---------//
    /**
     * Creates a new DmzStep object.
     */
    public DmzStep ()
    {
        super(
                Steps.DMZ,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve staves DMZ");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.dmzBuilder.processDmz(); // -> Staff clef + key + time
    }
}
