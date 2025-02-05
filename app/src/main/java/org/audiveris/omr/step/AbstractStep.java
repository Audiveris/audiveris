//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A b s t r a c t S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.sig.ui.UITask.OpKind;
import org.audiveris.omr.sig.ui.UITaskList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Abstract class <code>AbstractStep</code> provides a convenient basis for any {@link OmrStep}
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
     * Creates a new <code>AbstractStep</code> object.
     */
    public AbstractStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------------//
    // clearErrors //
    //-------------//
    /**
     * Clear the errors window in editor (No longer used).
     *
     * @param step  step concerned
     * @param sheet related sheet
     */
    public void clearErrors (OmrStep step,
                             Sheet sheet)
    {
        if (OMR.gui != null) {
            sheet.getErrorsEditor().clearStep(step);
        }
    }

    //-----------//
    // displayUI //
    //-----------//
    /**
     * Update UI at step completion.
     *
     * @param step  step just completed
     * @param sheet related sheet
     */
    public void displayUI (OmrStep step,
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

    /**
     * Apply the provided UI sequence to this step.
     *
     * @param seq    the sequence of UI tasks
     * @param opKind which operation is done on seq
     */
    public void impact (UITaskList seq,
                        OpKind opKind)
    {
        // No-op by default
    }

    /**
     * Report whether the provided class impacts this step.
     *
     * @param classe the class to check
     * @return true if step is impacted
     */
    public boolean isImpactedBy (Class<?> classe)
    {
        return false; // By default
    }

    /**
     * Report whether the collection of classes contains the provided class, or a super
     * type of the provided class.
     *
     * @param classe  the class to check
     * @param classes the collection of classes
     * @return true if compatible class found
     */
    protected boolean isImpactedBy (Class<?> classe,
                                    Collection<Class<?>> classes)
    {
        for (Class<?> cl : classes) {
            if (cl.isAssignableFrom(classe)) {
                return true;
            }
        }

        return false;
    }
}
