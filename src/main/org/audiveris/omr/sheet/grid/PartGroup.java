//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        P a r t G r o u p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.grid;

import java.util.Comparator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code PartGroup} describes a group of parts.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class PartGroup
{

    /** To compare groups by their first staff ID. */
    public static final Comparator<PartGroup> byFirstId = new Comparator<PartGroup>()
    {
        @Override
        public int compare (PartGroup pg1,
                            PartGroup pg2)
        {
            return Integer.compare(pg1.firstStaffId, pg2.firstStaffId);
        }
    };

    public static enum Symbol
    {
        bracket,
        brace,
        square;
    }

    //
    // Persistent data
    //----------------
    //
    /** Group level. */
    @XmlAttribute
    private final int number;

    /** Symbol used. */
    @XmlAttribute
    private final Symbol symbol;

    /** Use bar line connections?. */
    @XmlAttribute
    private final boolean barline;

    /** ID of first staff in group. */
    @XmlAttribute(name = "first-staff")
    private final int firstStaffId;

    /** ID of last staff in group. */
    @XmlAttribute(name = "last-staff")
    private int lastStaffId;

    /** Name. */
    @XmlAttribute
    private String name;

    /** Abbreviation. */
    @XmlAttribute
    private String abbreviation;

    /**
     * Build a {@code PartGroup} object.
     *
     * @param number       group level
     * @param symbol       symbol used
     * @param barline      use barline connections?
     * @param firstStaffId ID of first staff in group
     */
    public PartGroup (int number,
                      Symbol symbol,
                      boolean barline,
                      int firstStaffId)
    {
        this.number = number;
        this.symbol = symbol;
        this.barline = barline;
        this.firstStaffId = firstStaffId;

        lastStaffId = firstStaffId; // Initially
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private PartGroup ()
    {
        this.number = 0;
        this.symbol = null;
        this.barline = false;
        this.firstStaffId = 0;
    }

    /**
     * @return the abbreviation
     */
    public String getAbbreviation ()
    {
        return abbreviation;
    }

    /**
     * @return the firstStaffId
     */
    public int getFirstStaffId ()
    {
        return firstStaffId;
    }

    /**
     * @return the lastStaffId
     */
    public int getLastStaffId ()
    {
        return lastStaffId;
    }

    /**
     * @return the name
     */
    public String getName ()
    {
        return name;
    }

    /**
     * @return the number
     */
    public int getNumber ()
    {
        return number;
    }

    /**
     * @return the symbol
     */
    public Symbol getSymbol ()
    {
        return symbol;
    }

    /**
     * @return the barline
     */
    public boolean isBarline ()
    {
        return barline;
    }

    /**
     * Report whether this group is based on a brace symbol.
     *
     * @return true if brace-based
     */
    public boolean isBrace ()
    {
        return symbol == Symbol.brace;
    }

    /**
     * @param abbreviation the abbreviation to set
     */
    public void setAbbreviation (String abbreviation)
    {
        this.abbreviation = abbreviation;
    }

    /**
     * @param lastStaffId ID of the lastStaff
     */
    public void setLastStaffId (int lastStaffId)
    {
        this.lastStaffId = lastStaffId;
    }

    /**
     * @param name the name to set
     */
    public void setName (String name)
    {
        this.name = name;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append(symbol);

        sb.append(" number:").append(number);

        if (name != null) {
            sb.append(" name:").append(name);
        }

        if (abbreviation != null) {
            sb.append(" abbr:").append(abbreviation);
        }

        sb.append(" barline:").append(barline);

        sb.append(" staves:").append(firstStaffId);

        if (lastStaffId != firstStaffId) {
            sb.append("-").append(lastStaffId);
        }

        sb.append("}");

        return sb.toString();
    }
}
