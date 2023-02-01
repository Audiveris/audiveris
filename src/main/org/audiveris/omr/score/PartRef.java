//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          P a r t R e f                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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

import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.util.IntUtil;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>PartRef</code> represents, within a <code>SheetStub</code>, a soft reference
 * to a part in a system.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(value = XmlAccessType.NONE)
public class PartRef
{
    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /**
     * The number of lines for each staff in this part.
     */
    @XmlElement(name = "line-count")
    private final List<Integer> lineCounts = new ArrayList<>();

    /**
     * The part name, if any.
     */
    @XmlAttribute(name = "name")
    private String name;

    /**
     * The (manually?) assigned logical part ID.
     */
    @XmlAttribute(name = "logical-id")
    private Integer logicalId;

    /**
     * Indicates if the logical was manually mapped by the end-user.
     */
    @XmlAttribute(name = "manual")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean manual;

    // Transient data
    //---------------

    /** Containing system. */
    @Navigable(value = false)
    public SystemRef system;

    //~ Constructors -------------------------------------------------------------------------------

    //~ Methods ------------------------------------------------------------------------------------

    @SuppressWarnings(value = "unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        system = (SystemRef) parent;
    }

    public int getIndex ()
    {
        return system.getParts().indexOf(this);
    }

    public List<Integer> getLineCounts ()
    {
        return lineCounts;
    }

    public Integer getLogicalId ()
    {
        return logicalId;
    }

    public String getName ()
    {
        return name;
    }

    public Part getRealPart ()
    {
        final PageRef pageRef = system.getPage();
        final SheetStub stub = pageRef.getStub();
        final Sheet sheet = stub.getSheet(); // Avoid loading!
        final Page page = sheet.getPages().get(pageRef.getId() - 1);
        final SystemInfo systemInfo = page.getSystems().get(system.getId() - 1);
        final int partRefIndex = getIndex();
        final List<Part> parts = systemInfo.getParts();
        return parts.get(partRefIndex);
    }

    public int getStaffCount ()
    {
        return lineCounts.size();
    }

    public SystemRef getSystem ()
    {
        return system;
    }

    public boolean isManual ()
    {
        return manual;
    }

    public boolean setLogicalId (Integer logicalId)
    {
        if (this.logicalId == null || !this.logicalId.equals(logicalId)) {
            this.logicalId = logicalId;
            return true;
        } else {
            return false;
        }
    }

    public void setManual (boolean manual)
    {
        this.manual = manual;
    }

    public void setName (String name)
    {
        this.name = name;
    }

    public void setSystem (SystemRef system)
    {
        this.system = system;
    }

    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('#').append(getIndex() + 1).append('{');
        sb.append("lines:[").append(IntUtil.toCsvString(lineCounts)).append(']');

        if (name != null) {
            sb.append(" name:").append(name);
        }

        if (logicalId != null) {
            sb.append(" logicalId:").append(logicalId);
        }

        if (manual) {
            sb.append(" manual");
        }

        return sb.append('}').toString();
    }

    /**
     * Report a fully qualified string of this partRef.
     */
    public String toQualifiedString ()
    {
        // @formatter:off
        return new StringBuilder()
            .append("Sheet#").append(system.getPage().getSheetNumber())
            .append(",Page#").append(system.getPage().getId())
            .append(",System#").append(system.getId())
            .append(',').append(this).toString();
        // @formatter:on
    }
}
