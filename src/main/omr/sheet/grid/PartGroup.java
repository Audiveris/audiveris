//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        P a r t G r o u p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Class {@code PartGroup} describes a group of parts.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class PartGroup
{
    //~ Enumerations -------------------------------------------------------------------------------

    public static enum Symbol
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        bracket,
        brace;
    }

    //~ Instance fields ----------------------------------------------------------------------------
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
    @XmlElement
    private String name;

    /** Abbreviation. */
    @XmlElement
    private String abbreviation;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Build a {@code PartGroup} object.
     *
     * @param number       group level
     * @param symbol       symbol used
     * @param barline      use bar line connections?
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

    //~ Methods ------------------------------------------------------------------------------------
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
