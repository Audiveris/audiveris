//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S y s t e m R e f                                       //
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
package org.audiveris.omr.score;

import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.util.Navigable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * Class <code>SystemRef</code> represents, within a <code>SheetStub</code>, a soft
 * reference to a system.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(value = XmlAccessType.NONE)
public class SystemRef
{
    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /**
     * The sequence of parts in this system.
     */
    @XmlElement(name = "part")
    private final List<PartRef> parts = new ArrayList<>();

    // Transient data
    //---------------

    /** Containing page. */
    @Navigable(value = false)
    public PageRef pageRef;

    //~ Constructors -------------------------------------------------------------------------------

    //~ Methods ------------------------------------------------------------------------------------

    //----------------//
    // afterUnmarshal //
    //----------------//
    @SuppressWarnings(value = "unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        pageRef = (PageRef) parent;
    }

    //-------//
    // getId //
    //-------//
    public int getId ()
    {
        return 1 + getIndex();
    }

    //----------//
    // getIndex //
    //----------//
    public int getIndex ()
    {
        return pageRef.getSystems().indexOf(this);
    }

    //---------//
    // getPage //
    //---------//
    public PageRef getPage ()
    {
        return pageRef;
    }

    //----------//
    // getParts //
    //----------//
    public List<PartRef> getParts ()
    {
        return parts;
    }

    //---------------//
    // getRealSystem //
    //---------------//
    public SystemInfo getRealSystem ()
    {
        final Page page = pageRef.getRealPage(); // Avoid loading!
        return page.getSystems().get(getIndex());
    }

    //---------//
    // setPage //
    //---------//
    public void setPage (PageRef page)
    {
        this.pageRef = page;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return new StringBuilder(getClass().getSimpleName()).append('{') //
                .append(parts.stream().map(p -> p.toString()).collect(Collectors.joining(","))) //
                .append('}').toString();
    }
}
