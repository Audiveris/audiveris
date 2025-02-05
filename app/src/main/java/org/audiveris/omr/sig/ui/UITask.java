//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           U I T a s k                                          //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.score.Page;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.SIGraph;

/**
 * Class <code>UITask</code> is the basis for user interactive task
 *
 * @author Hervé Bitteur
 */
public abstract class UITask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** A name for task action. */
    protected final String actionName;

    /** Underlying sheet. */
    protected Sheet sheet;

    /** Underlying SIG, if any. */
    protected final SIGraph sig;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>UITask</code> object, with a page.
     *
     * @param page       the underlying page
     * @param actionName name for action
     */
    public UITask (Page page,
                   String actionName)
    {
        sig = null;
        sheet = page.getSheet();
        this.actionName = actionName;
    }

    /**
     * Creates a new <code>UITask</code> object, with a sheet.
     *
     * @param sheet      the underlying sheet
     * @param actionName name for action
     */
    public UITask (Sheet sheet,
                   String actionName)
    {
        sig = null;
        this.sheet = sheet;
        this.actionName = actionName;
    }

    /**
     * Creates a new <code>UITask</code> object, with a sig.
     *
     * @param sig        the underlying sig
     * @param actionName name for action
     */
    public UITask (SIGraph sig,
                   String actionName)
    {
        this.sig = sig;
        sheet = sig.getSystem().getSheet();
        this.actionName = actionName;
    }

    //~ Methods ------------------------------------------------------------------------------------

    public SIGraph getSig ()
    {
        return sig;
    }

    public abstract void performDo ();

    public abstract void performUndo ();

    //~ Inner Classes ------------------------------------------------------------------------------

    /** Operation kind performed on a UITask. */
    public static enum OpKind
    {
        DO,
        UNDO,
        REDO;
    }
}
