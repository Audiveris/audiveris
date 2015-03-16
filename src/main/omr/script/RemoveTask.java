//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R e m o v e T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.sheet.Sheet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Class {@code RemoveTask} removes a page from its containing score
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class RemoveTask
        extends SheetTask
{
    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a task to remove a page from its score
     *
     * @param sheet the sheet/page to remove
     */
    public RemoveTask (Sheet sheet)
    {
        super(sheet);
    }

    /** No-arg constructor for JAXB only. */
    private RemoveTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
    {
        sheet.remove(false);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" remove");

        return sb.toString();

    }
}
