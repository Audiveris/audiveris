//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S h e e t T a s k                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
        index = sheet.getStub().getNumber();
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
