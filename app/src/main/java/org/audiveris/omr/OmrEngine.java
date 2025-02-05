//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        O m r E n g i n e                                       //
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
package org.audiveris.omr;

import org.audiveris.omr.sheet.Book;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Interface <code>OmrEngine</code> defines the API of an OMR engine.
 * <p>
 * OMR deals with instances of {@link Book} class.
 * <p>
 * A Book instance can be obtained from:
 * <ul>
 * <li>An input image file, via {@link #loadInput(java.nio.file.Path)},</li>
 * <li>A book file, via {@link #loadBook(java.nio.file.Path)}.</li>
 * </ul>
 * <p>
 * Subsequent actions are performed directly on a Book instance.
 *
 * @author Hervé Bitteur
 */
public interface OmrEngine
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Insert the provided book in the collection of book instances.
     *
     * @param book the book to insert
     */
    void addBook (Book book);

    /**
     * Report all books handled.
     *
     * @return the non-mutable collection of all books handled by the OMR service
     */
    Collection<Book> getAllBooks ();

    /**
     * Report the book, if any, for the provided path.
     *
     * @param bookPath key book path
     * @return the corresponding book, if any
     */
    Book getBook (Path bookPath);

    /**
     * Build a book out of a book file, which has previously been saved.
     *
     * @param bookPath path to the book file
     * @return the allocated book
     */
    Book loadBook (Path bookPath);

    /**
     * Build a book out of an input file.
     *
     * @param inputPath path to the input file, which may contain several images
     * @return the allocated book
     */
    Book loadInput (Path inputPath);

    /**
     * Remove the provided book from OMR service.
     *
     * @param book        the book to remove
     * @param sheetNumber the current sheet number in book, if any.
     *                    sheet number is inserted in history, to enable reopening on same sheet
     * @return true if book is actually removed
     */
    boolean removeBook (Book book,
                        Integer sheetNumber);

    /**
     * Update book key in engine if needed.
     *
     * @param book        the book to rename
     * @param oldBookPath the previous book path or null
     */
    public void renameBook (Book book,
                            Path oldBookPath);
}
