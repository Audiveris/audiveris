//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        O m r E n g i n e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.sheet.Book;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface {@code OmrEngine} defines the API of an OMR engine.
 * <p>
 * Apart from the session-related methods {@link #initialize()} and {@link #terminate()}, OMR deals
 * with instances of {@link Book} class.
 * <p>
 * A Book instance can be obtained from:<ul>
 * <li>An input image file, via {@link #loadInput(java.nio.file.Path)},</li>
 * <li>A script file, via {@link #loadScript(java.nio.file.Path)},</li>
 * <li>A project file, via {@link #loadProject(java.nio.file.Path)}.</li>
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
     * Report the list of all books handled.
     *
     * @return the non-mutable list of all books handled by the OMR service
     */
    List<Book> getAllBooks ();

    /**
     * Needed to be called first to initialize OMR internals.
     */
    void initialize ();

    /**
     * Build a book out of an input file.
     *
     * @param path path to the input file, which may contain several images
     * @return the allocated book
     */
    Book loadInput (Path path);

    /**
     * Build a book out of a project file, which has previously been saved.
     *
     * @param path path to the input project file
     * @return the allocated book
     */
    Book loadProject (Path path);

    /**
     * Build a book out of a script file.
     *
     * @param path path to the input script file
     * @return the allocated book
     */
    Book loadScript (Path path);

    /**
     * Remove the provided book from OMR service.
     *
     * @param book the book to remove
     * @return true if book is actually removed
     */
    boolean removeBook (Book book);

    /**
     * Close the service, and release all OMR data.
     */
    void terminate ();
}
