//----------------------------------------------------------------------------//
//                                                                            //
//                             T e x t s S t e p                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.SystemInfo;

import omr.text.TextScanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code TextsStep} discovers text items in a system area.
 *
 * @author Hervé Bitteur
 */
public class TextsStep
        extends AbstractSystemStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            TextsStep.class);

    //~ Constructors -----------------------------------------------------------
    //
    //-----------//
    // TextsStep //
    //-----------//
    /**
     * Creates a TextsStep instance.
     */
    public TextsStep ()
    {
        super(
                Steps.TEXTS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve texts items in each system area");
    }

    //~ Methods ----------------------------------------------------------------
    //
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        clearSystemErrors(system);
        new TextScanner(system).scanSystem();
    }
}
