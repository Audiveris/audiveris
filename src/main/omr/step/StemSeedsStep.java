//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S t e m S e e d s S t e p                                   //
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
 * Class {@code StemSeedsStep} implements <b>STEM_SEEDS</b> step, which retrieves all
 * vertical sticks that may constitute <i>seeds</i> of future stems.
 *
 * @author Hervé Bitteur
 */
public class StemSeedsStep
        extends AbstractSystemStep
{
    //~ Constructors -------------------------------------------------------------------------------

    //---------------//
    // StemSeedsStep //
    //---------------//
    /**
     * Creates a new StemSeedsStep object.
     */
    public StemSeedsStep ()
    {
        super(
                Steps.STEM_SEEDS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve stem seeds & vertical endings");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        // We need a system of this sheet (any one)
        SystemInfo aSystem = sheet.getSystems().get(0);

        // Add stem checkboard
        aSystem.verticalsBuilder.addCheckBoard();
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.verticalsBuilder.buildVerticals(); // -> Stem seeds
    }

    //----------//
    // doProlog //
    //----------//
    @Override
    protected void doProlog (Collection<SystemInfo> systems,
                             Sheet sheet)
            throws StepException
    {
        sheet.getStemScaler().retrieveStem();
    }
}
