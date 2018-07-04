//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          P a g e R e f                                         //
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
package org.audiveris.omr.score;

import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.util.Jaxb;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code PageRef} represents a page reference within a sheet stub.
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
    @XmlAttribute(name = "id")
    private final int id;

    @XmlAttribute(name = "movement-start")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private final boolean movementStart;

    @XmlAttribute(name = "delta-measure-id")
    private Integer deltaMeasureId;

    // Transient data
    //---------------
    //
    private int sheetNumber;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PageRef} object.
     *
     * @param sheetNumber    sheet number within book
     * @param id             page id within sheet
     * @param movementStart  is page a movement start
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
     * @return the id
     */
    public int getId ()
    {
        return id;
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

    /**
     * @param deltaMeasureId the deltaMeasureId to set
     */
    public void setDeltaMeasureId (Integer deltaMeasureId)
    {
        this.deltaMeasureId = deltaMeasureId;
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
