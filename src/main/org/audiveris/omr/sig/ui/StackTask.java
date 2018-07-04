//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t a c k T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.sheet.rhythm.MeasureStack;

/**
 * Class {@code StackTask} implements the on demand re-processing of a stack.
 *
 * @author Hervé Bitteur
 */
public class StackTask
        extends UITask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Impacted stack. */
    private final MeasureStack stack;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StackTask} object.
     *
     * @param stack the impacted stack
     */
    public StackTask (MeasureStack stack)
    {
        super(stack.getSystem().getSig());
        this.stack = stack;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public MeasureStack getStack ()
    {
        return stack;
    }

    @Override
    public void performDo ()
    {
        // Void
    }

    @Override
    public void performUndo ()
    {
        // Void
    }

    @Override
    protected String actionName ()
    {
        return "reprocess";
    }
}
