//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S t a f f E d i t i o n T a s k                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.sig.ui.UITask;

/**
 * Class <code>StaffEditionTask</code> handles the user edition of a staff, regardless
 * of its mode (global or lines).
 *
 * @author Hervé Bitteur
 */
public class StaffEditionTask
        extends UITask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The editor used on staff. */
    private final StaffEditor editor;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a <code>StaffEditionTask</code> instance.
     *
     * @param editor the underlying staff editor
     */
    public StaffEditionTask (StaffEditor editor)
    {
        super(editor.getSystem().getSheet(), "staff-edition");
        this.editor = editor;
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public void performDo ()
    {
        editor.finalDoit();
    }

    @Override
    public void performUndo ()
    {
        editor.undo();
    }

    @Override
    public String toString ()
    {
        return new StringBuilder(getClass().getSimpleName()).append('{').append(editor).append('}')
                .toString();
    }
}
