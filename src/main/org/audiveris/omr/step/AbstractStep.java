//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A b s t r a c t S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.step;

import org.audiveris.omr.OMR;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.ui.SheetTab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class {@code AbstractStep} provides a convenient basis for any {@link Step}
 * implementation.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AbstractStep} object.
     */
    public AbstractStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // clearErrors //
    //-------------//
    public void clearErrors (Step step,
                             Sheet sheet)
    {
        if (OMR.gui != null) {
            sheet.getErrorsEditor().clearStep(step);
        }
    }

    //-----------//
    // displayUI //
    //-----------//
    public void displayUI (Step step,
                           Sheet sheet)
    {
        // Void by default
    }

    //------//
    // doit //
    //------//
    /**
     * Actually perform the step.
     * This method must be defined for any concrete Step.
     *
     * @param sheet the related sheet
     * @throws StepException raised if processing failed
     */
    public abstract void doit (Sheet sheet)
            throws StepException;

    //-------------//
    // getSheetTab //
    //-------------//
    /**
     * Report the related assembly view tab, selected when step completes
     *
     * @return the related tab
     */
    public SheetTab getSheetTab ()
    {
        return SheetTab.DATA_TAB;
    }
}
