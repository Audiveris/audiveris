//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B e a m s S t e p                                       //
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
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code BeamsStep} implements <b>BEAMS</b> step, which uses the spots produced
 * by an image closing operation to retrieve all possible beam interpretations.
 *
 * @author Hervé Bitteur
 */
public class BeamsStep
        extends AbstractSystemStep
{
    //~ Constructors -------------------------------------------------------------------------------

    //-----------//
    // BeamsStep //
    //-----------//
    /**
     * Creates a new BeamsStep object.
     */
    public BeamsStep ()
    {
        super(
                Steps.BEAMS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve beams");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.beamsBuilder.buildBeams(); // -> Beams
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * {@inheritDoc}
     * Perform a closing operation on the whole image with a disk
     * shape as the structure element to point out concentrations of
     * foreground pixels (for beams, for black heads).
     */
    @Override
    protected void doProlog (Collection<SystemInfo> systems,
                             Sheet sheet)
            throws StepException
    {
        // Retrieve significant spots for the whole sheet
        sheet.getSpotsBuilder().buildPageSpots();
    }
}
