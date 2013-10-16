//----------------------------------------------------------------------------//
//                                                                            //
//                            L e d g e r s S t e p                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import java.util.Collection;
import omr.sheet.HorizontalsFilter;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

/**
 * Class {@code LedgersStep} implements LEDGERS step.
 *
 * @author Hervé Bitteur
 */
public class LedgersStep
        extends AbstractSystemStep
{
    //~ Constructors -----------------------------------------------------------

    //-------------//
    // LedgersStep //
    //-------------//
    /**
     * Creates a new LedgersStep object.
     */
    public LedgersStep ()
    {
        super(
                Steps.LEDGERS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve ledgers & horizontal endings");
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        // We need a system of this sheet (any one)
        SystemInfo aSystem = sheet.getSystems()
                .get(0);

        // Retrieve long horizontal runs outside staves for ledgers/endings
        new HorizontalsFilter(sheet).process();
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * {@inheritDoc}
     * Retrieve horizontal sticks out of full filtered image.
     */
    @Override
    protected void doProlog (Collection<SystemInfo> systems,
                             Sheet sheet)
            throws StepException
    {
        // Retrieve long horizontal runs outside staves for ledgers/endings
        new HorizontalsFilter(sheet).process();
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.horizontalsBuilder.buildLedgers(); // -> Ledgers
    }
}
