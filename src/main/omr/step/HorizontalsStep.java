//----------------------------------------------------------------------------//
//                                                                            //
//                       H o r i z o n t a l s S t e p                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.HorizontalsBuilder;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code HorizontalsStep} retrieves the horizontal dashes (ledgers,
 * endings)
 *
 * @author Hervé Bitteur
 */
public class HorizontalsStep
    extends AbstractStep
{
    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // HorizontalsStep //
    //-----------------//
    /**
     * Creates a new HorizontalsStep object.
     */
    public HorizontalsStep ()
    {
        super(
            Steps.HORIZONTALS,
            Level.SHEET_LEVEL,
            Mandatory.MANDATORY,
            Redoable.NON_REDOABLE,
            HORIZONTALS_TAB,
            "Detect horizontal dashes");
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> unused,
                      Sheet                  sheet)
        throws StepException
    {
        sheet.setHorizontalsBuilder(new HorizontalsBuilder(sheet));
        sheet.setHorizontals(sheet.getHorizontalsBuilder().buildInfo());
    }
}
