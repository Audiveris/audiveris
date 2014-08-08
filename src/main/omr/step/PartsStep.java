//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        P a r t s S t e p                                       //
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import omr.score.PageReduction;
import omr.score.entity.Page;

/**
 * Class {@code PartsStep} defines the parts within a page, by reducing informations
 * from systems in the page.
 *
 * @author Hervé Bitteur
 */
public class PartsStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PartsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    //-----------//
    // PartsStep //
    //-----------//
    /**
     * Creates a new {@code PartsStep} object.
     */
    public PartsStep ()
    {
        super(
                Steps.PARTS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Connect the parts across all systems in a page");
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    protected void doit (Collection<SystemInfo> systems,
                         Sheet sheet)
            throws StepException
    {
          // Connect parts across systems
        Page page = sheet.getPage();
        new PageReduction(page).reduce();

        //TODO: Missing parts
    }
}
