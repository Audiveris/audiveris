//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S c o r e P a g e R e f                                    //
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

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class <code>ScorePageRef</code> is used within a score to formalize a link to a page.
 * <p>
 * While a score (and its containing book) always remain in memory,
 * a dependent page (and its containing sheet) can be swapped out.
 * <p>
 * The ScorePageRef instance contains just the information needed to reload the page on demand.
 *
 * @author Hervé Bitteur
 */
public class ScorePageRef
{
    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------
    //
    /**
     * This is the rank, counted from 1 within the containing book, of the sheet
     * containing the page.
     */
    @XmlAttribute(name = "sheet-number")
    public final int sheetNumber;

    /**
     * This is the page number within the containing sheet.
     * <ul>
     * <li>Value is 1 if the sheet contains just 1 page (which is the most frequent case by far)
     * <li>Value is in [1..n] range if there are n pages in the same sheet
     * </ul>
     */
    @XmlAttribute(name = "sheet-page-id")
    public final int sheetPageId;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>ScorePageRef</code> object.
     *
     * @param sheetNumber sheet number (counted from 1) within the containing book
     * @param sheetPageId page id (counted from 1) within the containing sheet
     */
    public ScorePageRef (int sheetNumber,
                         int sheetPageId)
    {
        this.sheetNumber = sheetNumber;
        this.sheetPageId = sheetPageId;
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private ScorePageRef ()
    {
        this.sheetNumber = 0;
        this.sheetPageId = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return new StringBuilder(getClass().getSimpleName()).append('{')
                .append("sheetNumber:").append(sheetNumber)
                .append(", sheetPageId:").append(sheetPageId)
                .append('}').toString();
    }
}
