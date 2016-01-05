//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   I n v a l i d a t e T a s k                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.OMR;

import omr.sheet.Sheet;
import omr.sheet.ui.StubsController;

import omr.ui.ViewParameters;

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
        sheet.invalidate();

        if (OMR.getGui() != null) {
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
                        controller.callAboutSheet(sheet.getStub());
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
