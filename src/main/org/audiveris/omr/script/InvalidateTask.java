//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   I n v a l i d a t e T a s k                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.script;

import org.audiveris.omr.OMR;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.ui.ViewParameters;

import javax.swing.SwingUtilities;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Class {@code InvalidateTask} flags a sheet as invalid.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class InvalidateTask
        extends SheetTask
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a task to invalidate a sheet
     *
     * @param sheet the sheet to invalidate
     */
    public InvalidateTask (Sheet sheet)
    {
        super(sheet);
    }

    /** No-arg constructor for JAXB only. */
    private InvalidateTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (final Sheet sheet)
    {
        sheet.getStub().invalidate();

        if (OMR.gui != null) {
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    StubsController controller = StubsController.getInstance();

                    if (ViewParameters.getInstance().isInvalidSheetDisplay() == false) {
                        controller.removeAssembly(sheet.getStub());
                    } else {
                        controller.callAboutStub(sheet.getStub());
                    }
                }
            });
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" invalidate");

        return sb.toString();
    }
}
