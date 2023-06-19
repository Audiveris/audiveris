//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S t a f f C o n f i g                                     //
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
package org.audiveris.omr.score;

import org.audiveris.omr.util.Jaxb;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>StaffConfig</code> summarizes staff physical configuration, using line count,
 * and small annotation if any.
 *
 * @author Hervé Bitteur
 */

@XmlAccessorType(value = XmlAccessType.NONE)
public class StaffConfig
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Count of lines in staff. */
    @XmlAttribute(name = "line-count")
    public final int count;

    /** Is this a small staff?. */
    @XmlAttribute(name = "small")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    public final boolean isSmall;

    //~ Constructors -------------------------------------------------------------------------------

    public StaffConfig ()
    {
        this.count = 0;
        this.isSmall = false;
    }

    public StaffConfig (int count,
                        boolean isSmall)
    {
        this.count = count;
        this.isSmall = isSmall;
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof StaffConfig that) {
            return this.count == that.count && this.isSmall == that.isSmall;
        }
        return false;
    }

    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = 59 * hash + this.count;
        hash = 59 * hash + (this.isSmall ? 1 : 0);
        return hash;
    }

    @Override
    public String toString ()
    {
        return new StringBuilder().append(count).append(isSmall ? 's' : "").toString();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    public static StaffConfig decode (String str)
    {
        boolean isSmall = false;
        final int count;
        if (str.endsWith("s")) {
            isSmall = true;
            count = Integer.decode(str.substring(0, str.length() - 1));
        } else {
            count = Integer.decode(str);
        }
        return new StaffConfig(count, isSmall);
    }

    /**
     * Report a string formatted as comma-separated values from the provided collection.
     *
     * @param collection provided collection of StaffConfig
     * @return the CSV string
     */
    public static String toCsvString (Collection<StaffConfig> collection)
    {
        return new StringBuilder().append(
                collection.stream() //
                        .map(sc -> (sc == null) ? "null" : sc.toString()) //
                        .collect(Collectors.joining(","))).toString();
    }

    //~ Static fields/initializers -----------------------------------------------------------------

    //~ Inner Classes ------------------------------------------------------------------------------
}
