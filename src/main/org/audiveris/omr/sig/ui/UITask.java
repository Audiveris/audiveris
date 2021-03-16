//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           U I T a s k                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
 * Class {@code UITask} is the basis for user interactive task
 *
 * @author Hervé Bitteur
 */
public abstract class UITask
{

    /** Operation kind performed on a UITask. */
    public static enum OpKind
    {
        DO,
        UNDO,
        REDO;
    }

    /** A name for task action. */
    protected final String actionName;

    /** Underlying sheet. */
    protected Sheet sheet;

    /** Underlying SIG. */
    protected final SIGraph sig;

    /**
     * Creates a new {@code UITask} object.
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

    /**
     * Creates a new {@code UITask} object.
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

    public SIGraph getSig ()
    {
        return sig;
    }

    public abstract void performDo ();

    public abstract void performUndo ();
}
