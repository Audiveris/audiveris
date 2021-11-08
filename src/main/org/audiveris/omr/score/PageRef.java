//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          P a g e R e f                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.util.Jaxb;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>PageRef</code> represents a page reference within a <code>SheetStub</code>.
 * <p>
 * The hierarchy of Book, SheetStub and PageRef instances always remains in memory,
 * but Sheet and Page instances can be swapped out.
 * <p>
 * To this end, a PageRef instance provides enough information to:
 * <ul>
 * <li>Process the following pages in the same score, without having to load this page
 * <li>Load this page on demand.
 * </ul>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class PageRef
        implements Comparable<PageRef>
{
    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------
    //
    /**
     * This is the page number within the containing sheet.
     * <ul>
     * <li>Value is 1 if the sheet contains just 1 page (which is the most frequent case)
     * <li>Value is in [1..n] range if there are n pages in the same sheet
     * </ul>
     */
    @XmlAttribute(name = "id")
    private final int id;

    /**
     * This boolean indicates that the page starts a new movement, generally because its
     * first system is indented with respect to the other systems in the same sheet.
     * <p>
     * This attribute is present only if its boolean value is 'true'.
     */
    @XmlAttribute(name = "movement-start")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private final boolean movementStart;

    /**
     * This integer value provides the increment on measure IDs brought by the page.
     * <p>
     * This would be the exact count of measures in the page, if there were no pickup measures,
     * alternate endings or cautionary measures.
     * <p>
     * Knowing this value allows to assign measure IDs in the following page,
     * without having to load this one.
     */
    @XmlAttribute(name = "delta-measure-id")
    private Integer deltaMeasureId;

    /**
     * This time rational value corresponds to the last active time signature in the page.
     * <p>
     * For example, if the last active time signature is a COMMON time signature, this element will
     * provide a 4/4 value.
     * <p>
     * Knowing this value allows to process rhythm and check measures in the following page,
     * without having to load this one.
     * <p>
     */
    @XmlElement(name = "last-time-rational")
    private TimeRational lastTimeRational;

    // Transient data
    //---------------
    //
    private int sheetNumber;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>PageRef</code> object.
     *
     * @param sheetNumber    sheet number within book
     * @param id             page id within sheet
     * @param movementStart  is page a movement start?
     * @param deltaMeasureId increase of measure IDs within the page, or null
     */
    public PageRef (int sheetNumber,
                    int id,
                    boolean movementStart,
                    Integer deltaMeasureId)
    {
        this.sheetNumber = sheetNumber;
        this.id = id;
        this.deltaMeasureId = deltaMeasureId;
        this.movementStart = movementStart;
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private PageRef ()
    {
        id = 0;
        movementStart = false;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // compareTo //
    //-----------//
    @Override
    public int compareTo (PageRef that)
    {
        // This is a total ordering
        if (this.sheetNumber != that.sheetNumber) {
            return Integer.compare(this.sheetNumber, that.sheetNumber);
        }

        return Integer.compare(this.id, that.id);
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (!(obj instanceof PageRef)) {
            return false;
        }

        return compareTo((PageRef) obj) == 0;
    }

    /**
     * @return the deltaMeasureId
     */
    public Integer getDeltaMeasureId ()
    {
        return deltaMeasureId;
    }

    /**
     * @param deltaMeasureId the deltaMeasureId to set
     */
    public void setDeltaMeasureId (Integer deltaMeasureId)
    {
        this.deltaMeasureId = deltaMeasureId;
    }

    /**
     * @return the id
     */
    public int getId ()
    {
        return id;
    }

    /**
     * @return the last time rational value in page
     */
    public TimeRational getLastTimeRational ()
    {
        return lastTimeRational;
    }

    /**
     * @param lastTimeRational the lastTimeRational value to set
     */
    public void setLastTimeRational (TimeRational lastTimeRational)
    {
        this.lastTimeRational = lastTimeRational;
    }

    /**
     * @return the sheetNumber
     */
    public int getSheetNumber ()
    {
        return sheetNumber;
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (97 * hash) + this.id;
        hash = (97 * hash) + this.sheetNumber;

        return hash;
    }

    //-----------------//
    // isMovementStart //
    //-----------------//
    /**
     * @return the movementStart
     */
    public boolean isMovementStart ()
    {
        return movementStart;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("PageRef{");
        sb.append("sheetNumber:").append(sheetNumber);
        sb.append(" id:").append(id);

        if (isMovementStart()) {
            sb.append(" movementStart");
        }

        if (deltaMeasureId != null) {
            sb.append(" deltaMeasureId:").append(deltaMeasureId);
        }

        if (lastTimeRational != null) {
            sb.append(" lastTimeRational:").append(lastTimeRational);
        }

        sb.append('}');

        return sb.toString();
    }

    //-----------------//
    // beforeUnmarshal //
    //-----------------//
    @SuppressWarnings("unused")
    private void beforeUnmarshal (Unmarshaller u,
                                  Object parent)
    {
        SheetStub stub = (SheetStub) parent;
        sheetNumber = stub.getNumber();
    }
}
