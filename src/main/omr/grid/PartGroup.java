//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        P a r t G r o u p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.sheet.Staff;

/**
 * Class {@code PartGroup} describes a group of parts.
 *
 * @author Hervé Bitteur
 */
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
    /** Group level. */
    private final int number;

    /** Symbol used. */
    private final Symbol symbol;

    /** Use bar line connections?. */
    private final boolean barline;

    /** First staff in group. */
    private final Staff firstStaff;

    /** Last staff in group. */
    private Staff lastStaff;

    /** Name. */
    private String name;

    /** Abbreviation. */
    private String abbreviation;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Build a {@code PartGroup} object.
     *
     * @param number     group level
     * @param symbol     symbol used
     * @param barline    use bar line connections?
     * @param firstStaff first staff in group
     */
    public PartGroup (int number,
                      Symbol symbol,
                      boolean barline,
                      Staff firstStaff)
    {
        this.number = number;
        this.symbol = symbol;
        this.barline = barline;
        this.firstStaff = firstStaff;

        lastStaff = firstStaff; // Initially
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
     * @return the firstStaff
     */
    public Staff getFirstStaff ()
    {
        return firstStaff;
    }

    /**
     * @return the lastStaff
     */
    public Staff getLastStaff ()
    {
        return lastStaff;
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
     * @param lastStaff the lastStaff to set
     */
    public void setLastStaff (Staff lastStaff)
    {
        this.lastStaff = lastStaff;
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

        sb.append(" staves:").append(firstStaff.getId());

        if (lastStaff != firstStaff) {
            sb.append("-").append(lastStaff.getId());
        }

        sb.append("}");

        return sb.toString();
    }
}
