//----------------------------------------------------------------------------//
//                                                                            //
//                             T e x t s S t e p                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.text.TextScanner;

import omr.log.Logger;

import omr.sheet.SystemInfo;

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
    private static final Logger logger = Logger.getLogger(TextsStep.class);

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
        new TextScanner(system).scanSystem();
    }
}
