//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S h e e t T a s k                                        //
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

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code SheetTask} is a {@link ScriptTask} that applies to a given sheet.
 *
 * @author Hervé Bitteur
 */
public abstract class SheetTask
        extends ScriptTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Sheet index */
    @XmlAttribute(name = "sheet")
    protected Integer index;

    /** The related sheet */
    protected Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SheetTask object.
     *
     * @param sheet the related sheet
     */
    protected SheetTask (Sheet sheet)
    {
        index = sheet.getNumber();
    }

    /** No-arg constructor for JAXB only. */
    protected SheetTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // getSheetIndex //
    //---------------//
    /**
     * Report the id of the index/sheet if any
     *
     * @return the sheet index (counted from 1) or null if none
     */
    public Integer getSheetIndex ()
    {
        return index;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (index != null) {
            sb.append(" sheet#").append(index);
        }

        return sb.toString();
    }
}
