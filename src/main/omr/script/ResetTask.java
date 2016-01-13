//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R e s e t T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.sheet.Sheet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ResetTask} resets a sheet to initial state.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "reset")
public class ResetTask
        extends SheetTask
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a task to reset a sheet
     *
     * @param sheet the sheet to reset
     */
    public ResetTask (Sheet sheet)
    {
        super(sheet);
    }

    /** No-arg constructor for JAXB only. */
    private ResetTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
    {
        sheet.reset();
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" reset");

        return sb.toString();
    }
}
