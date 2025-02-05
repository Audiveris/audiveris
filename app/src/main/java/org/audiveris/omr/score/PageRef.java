//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          P a g e R e f                                         //
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

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>PageRef</code> represents, within a <code>SheetStub</code>, a soft reference
 * to a page.
 * <p>
 * The hierarchy of Book, SheetStub and PageRef (plus SystemRef and PartRef) instances always
 * remains in memory, but Sheet and Page instances can be swapped out.
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

    /**
     * This is the page rank within the containing sheet.
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
     * This is sometimes less than the raw measure count, because of special measures
     * (pickup, repeat, courtesy) that don't increment measure IDs.
     */
    @XmlAttribute(name = "delta-measure-id")
    private Integer deltaMeasureId;

    /**
     * This time rational value corresponds to the last effective time signature in the page.
     * <p>
     * For example, if the last active time signature is a COMMON time signature, this element will
     * provide a 4/4 value.
     */
    @XmlElement(name = "last-time-rational")
    private TimeRational lastTimeRational;

    /**
     * Information about the sequence of systems in this page.
     */
    @XmlElement(name = "system")
    private final List<SystemRef> systems = new ArrayList<>();

    // Transient data
    //---------------

    /**
     * The rank of its containing sheet within its book.
     *
     * @see #afterUnmarshal (Unmarshaller, Object )
     */
    private int sheetNumber;

    /** Containing sheet stub. */
    @Navigable(false)
    private SheetStub stub;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor needed for JAXB.
     */
    private PageRef ()
    {
        id = 0;
        movementStart = false;
    }

    /**
     * Creates a new <code>PageRef</code> object.
     *
     * @param stub          containing sheet stub
     * @param id            page id within sheet
     * @param movementStart is page a movement start?
     */
    public PageRef (SheetStub stub,
                    int id,
                    boolean movementStart)
    {
        this.stub = stub;
        this.id = id;
        this.movementStart = movementStart;

        sheetNumber = stub.getNumber();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // addSystem //
    //-----------//
    public void addSystem (SystemRef system)
    {
        systems.add(system);
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        stub = (SheetStub) parent;
        sheetNumber = stub.getNumber();
    }

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
        if (obj instanceof PageRef that) {
            return compareTo(that) == 0;
        }

        return false;
    }

    //-------------------//
    // getDeltaMeasureId //
    //-------------------//
    /**
     * Report the progression of measure IDs within this page.
     *
     * @return the deltaMeasureId
     */
    public Integer getDeltaMeasureId ()
    {
        return deltaMeasureId;
    }

    //-------//
    // getId //
    //-------//
    /**
     * @return the id
     */
    public int getId ()
    {
        return id;
    }

    //----------//
    // getIndex //
    //----------//
    /**
     * Report index of this page within the containing sheet stub.
     *
     * @return index in list of PageRef's
     */
    public int getIndex ()
    {
        return stub.getPageRefs().indexOf(this);
    }

    /**
     * @return the last time rational value in page
     */
    public TimeRational getLastTimeRational ()
    {
        return lastTimeRational;
    }

    //---------------//
    // getPageNumber //
    //---------------//
    public PageNumber getPageNumber ()
    {
        return new PageNumber(stub.getNumber(), 1 + stub.getPageRefs().indexOf(this));
    }

    //---------------------//
    // getPrecedingInScore //
    //---------------------//
    /**
     * Report the preceding PageRef of this one within the score.
     *
     * @return the preceding PageRef, or null if none
     */
    public PageRef getPrecedingInScore ()
    {
        final Score score = stub.getBook().getScore(this);
        if (score == null) {
            return null;
        }

        return score.getPrecedingPageRef(this);
    }

    //-------------//
    // getRealPage //
    //-------------//
    public Page getRealPage ()
    {
        final Sheet sheet = stub.getSheet(); // Avoid loading!
        return sheet.getPages().get(getIndex());
    }

    //----------------//
    // getSheetNumber //
    //----------------//
    /**
     * @return the sheetNumber
     */
    public int getSheetNumber ()
    {
        return sheetNumber;
    }

    //---------//
    // getStub //
    //---------//
    public SheetStub getStub ()
    {
        return stub;
    }

    //------------//
    // getSystems //
    //------------//
    /**
     * Report the systems in this page
     *
     * @return the (unmodifiable) list of systems
     */
    public List<SystemRef> getSystems ()
    {
        return Collections.unmodifiableList(systems);
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
     * Report whether this page is explicitly a movement start (due to system indentation).
     *
     * @return true if so
     */
    public boolean isMovementStart ()
    {
        return movementStart;
    }

    //--------------//
    // removeSystem //
    //--------------//
    /**
     * Remove the provided SystemRef
     *
     * @param systemRef the systemRef to remove
     * @return true if removed
     */
    public boolean removeSystem (SystemRef systemRef)
    {
        return systems.remove(systemRef);
    }

    //-------------------//
    // setDeltaMeasureId //
    //-------------------//
    /**
     * Set the increase on measure ID in this page.
     *
     * @param deltaMeasureId the delta Measure Id to set
     */
    public void setDeltaMeasureId (Integer deltaMeasureId)
    {
        this.deltaMeasureId = deltaMeasureId;
    }

    //---------------------//
    // setLastTimeRational //
    //---------------------//
    /**
     * Remember the time rational value at end of this page.
     *
     * @param lastTimeRational the last TimeRational value to set
     */
    public void setLastTimeRational (TimeRational lastTimeRational)
    {
        this.lastTimeRational = lastTimeRational;
    }

    //------------//
    // setSystems //
    //------------//
    /**
     * Redefines the sequence of systems in this page.
     *
     * @param systems the new sequence of systems
     */
    public final void setSystems (List<SystemRef> systems)
    {
        this.systems.clear();
        this.systems.addAll(systems);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('#').append(getId()).append('{');
        sb.append("sheetNumber:").append(sheetNumber);

        if (isMovementStart()) {
            sb.append(" movementStart");
        }

        if (deltaMeasureId != null) {
            sb.append(" deltaMeasureId:").append(deltaMeasureId);
        }

        if (lastTimeRational != null) {
            sb.append(" lastTimeRational:").append(lastTimeRational);
        }

        return sb.append('}').toString();
    }

    //----------------//
    // unremoveSystem //
    //----------------//
    /**
     * Un-remove the provided systemRef into this pageRef.
     *
     * @param index     the target index
     * @param systemRef the systemRef to re-insert
     * @see #removeSystem
     */
    public void unremoveSystem (int index,
                                SystemRef systemRef)
    {
        systems.add(index, systemRef);
    }
}
