//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          P a r t R e f                                         //
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

import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
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
     * <p>
     * Deprecated, replaced by <code>staffConfigs</code>.
     */
    @Deprecated
    @XmlElement(name = "line-count")
    private List<Integer> OLD_lineCounts;

    /**
     * The configuration for each staff in this part.
     */
    @XmlElement(name = "staff-configuration")
    private final List<StaffConfig> staffConfigs = new ArrayList<>();

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
    public SystemRef systemRef;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Zero-arg constructor needed for JAXB.
     */
    @SuppressWarnings(value = "unused")
    private PartRef ()
    {
    }

    /**
     * Creates a PartRef instance.
     *
     * @param systemRef soft reference of containing system
     * @param staves    list of staves in part
     */
    public PartRef (SystemRef systemRef,
                    List<Staff> staves)
    {
        this.systemRef = systemRef;
        computeStaffConfigs(staves);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------------//
    // afterUnmarshal //
    //----------------//
    @SuppressWarnings(value = "unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        systemRef = (SystemRef) parent;

        if (OLD_lineCounts != null) {
            for (int count : OLD_lineCounts) {
                staffConfigs.add(new StaffConfig(count, false));
            }

            OLD_lineCounts = null;
        }
    }

    //---------------------//
    // computeStaffConfigs //
    //---------------------//
    /**
     * Compute the staff configurations, according to the provided list of staves.
     *
     * @param staves the sequence of staves in part
     */
    public final void computeStaffConfigs (List<Staff> staves)
    {
        staffConfigs.clear();

        for (Staff staff : staves) {
            staffConfigs.add(staff.getStaffConfig());
        }
    }

    public int getIndex ()
    {
        return systemRef.getParts().indexOf(this);
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
        final SystemInfo systemInfo = systemRef.getRealSystem(); // Perhaps loading...
        return systemInfo.getParts().get(getIndex());
    }

    public List<StaffConfig> getStaffConfigs ()
    {
        return staffConfigs;
    }

    public int getStaffCount ()
    {
        return staffConfigs.size();
    }

    public SystemRef getSystem ()
    {
        return systemRef;
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
        this.systemRef = system;
    }

    /**
     * Report a fully qualified string of this partRef.
     *
     * @return fully qualified string
     */
    public String toQualifiedString ()
    {
        return new StringBuilder() //
                .append("Sheet#").append(systemRef.getPage().getSheetNumber()) //
                .append(",Page#").append(systemRef.getPage().getId()) //
                .append(",System#").append(systemRef.getId()) //
                .append(',').append(this).toString();
    }

    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('#').append(getIndex() + 1).append('{');
        sb.append("configs:[").append(StaffConfig.toCsvString(staffConfigs)).append(']');

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
}
