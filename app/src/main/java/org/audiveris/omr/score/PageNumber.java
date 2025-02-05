//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P a g e N u m b e r                                      //
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

import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.SheetStub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class <code>PageNumber</code> represents, within a <code>Score</code>, a soft reference
 * to a page.
 * <p>
 * While a score (and its containing book) always remain in memory,
 * a dependent physical page (and its containing physical sheet) can be swapped out.
 * <p>
 * The PageNumber instance contains just the information needed to reload the page on demand.
 *
 * @author Hervé Bitteur
 */
public class PageNumber
        implements Comparable<PageNumber>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PageNumber.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /**
     * This is the rank, counted from 1 within the containing book, of the containing sheet.
     */
    @XmlAttribute(name = "sheet-number")
    public final int sheetNumber;

    /**
     * This is the page rank within the containing sheet.
     * <ul>
     * <li>Value is 1 if the sheet contains just 1 page (which is the most frequent case by far)
     * <li>Value is in [1..n] range if there are n pages in the same sheet
     * </ul>
     */
    @XmlAttribute(name = "sheet-page-id")
    public final int sheetPageId;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor needed for JAXB.
     */
    private PageNumber ()
    {
        this.sheetNumber = 0;
        this.sheetPageId = 0;
    }

    /**
     * Creates a new <code>ScorePageRef</code> object.
     *
     * @param sheetNumber sheet number (counted from 1) within the containing book
     * @param sheetPageId page id (counted from 1) within the containing sheet
     */
    public PageNumber (int sheetNumber,
                       int sheetPageId)
    {
        this.sheetNumber = sheetNumber;
        this.sheetPageId = sheetPageId;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // compareTo //
    //-----------//
    @Override
    public int compareTo (PageNumber that)
    {
        if (sheetNumber != that.sheetNumber) {
            return Integer.compare(sheetNumber, that.sheetNumber);
        }

        return Integer.compare(sheetPageId, that.sheetPageId);
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof PageNumber that) {
            return (sheetNumber == that.sheetNumber) && (sheetPageId == that.sheetPageId);
        }

        return false;
    }

    //------------//
    // getPageRef //
    //------------//
    /**
     * Report the PageRef that corresponds to this PageNumber.
     *
     * @param book the containing book
     * @return the corresponding PageRef instance
     */
    public PageRef getPageRef (Book book)
    {
        final SheetStub stub = book.getStub(sheetNumber);
        final List<PageRef> pageRefs = stub.getPageRefs();

        if (pageRefs.isEmpty()) {
            logger.info("No page for sheet#{}", sheetNumber);
            return null;
        }

        return pageRefs.get(sheetPageId - 1);
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = 71 * hash + this.sheetNumber;
        hash = 71 * hash + this.sheetPageId;
        return hash;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        // @formatter:off
        return new StringBuilder(getClass().getSimpleName()).append('{')
                .append("sheetNumber:").append(sheetNumber)
                .append(", sheetPageId:").append(sheetPageId)
                .append('}').toString();
        // @formatter:on
    }
}
