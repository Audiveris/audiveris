//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             B o o k                                            //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.image.FilterParam;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.util.param.Param;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.locks.Lock;

import javax.swing.JFrame;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code Book} is the root class for handling a physical set of image input
 * files, resulting in one or several logical MusicXML scores.
 * <p>
 * A book instance generally corresponds to an input file containing one or several images, each
 * image resulting in a separate {@link Sheet} instance.
 * <p>
 * A sheet generally contains one or several systems.
 * An indented system (sometimes prefixed by part names) usually indicates a new movement.
 * Such indented system may appear in the middle of a sheet, thus (logical) movement frontiers do
 * not always match (physical) sheet frontiers.
 * <p>
 * A (super-) book may also contain (sub-) books to recursively gather a sequence of input files.
 * <p>
 * Methods are organized as follows:
 * <dl>
 * <dt>Administration</dt>
 * <dd><ul>
 * <li>{@link #getInputPath}</li>
 * <li>{@link #getAlias}</li>
 * <li>{@link #setAlias}</li>
 * <li>{@link #getRadix}</li>
 * <li>{@link #includeBook}</li>
 * <li>{@link #getOffset}</li>
 * <li>{@link #setOffset}</li>
 * <li>{@link #isDirty}</li>
 * <li>{@link #setDirty}</li>
 * <li>{@link #isModified}</li>
 * <li>{@link #setModified}</li>
 * <li>{@link #close}</li>
 * <li>{@link #isClosing}</li>
 * <li>{@link #setClosing}</li>
 * <li>{@link #getLock}</li>
 * </ul></dd>
 *
 * <dt>SheetStubs</dt>
 * <dd><ul>
 * <li>{@link #createStubs}</li>
 * <li>{@link #createStubsTabs}</li>
 * <li>{@link #loadSheetImage}</li>
 * <li>{@link #isMultiSheet}</li>
 * <li>{@link #getStub}</li>
 * <li>{@link #getStubs}</li>
 * <li>{@link #getFirstValidStub}</li>
 * <li>{@link #getValidStubs}</li>
 * <li>{@link #removeStub}</li>
 * <li>{@link #hideInvalidStubs}</li>
 * <li>{@link #swapAllSheets}</li>
 * </ul></dd>
 *
 * <dt>Parameters</dt>
 * <dd><ul>
 * <li>{@link #getBinarizationFilter}</li>
 * <li>{@link #getOcrLanguages}</li>
 * </ul></dd>
 *
 * <dt>Transcription</dt>
 * <dd><ul>
 * <li>{@link #reset}</li>
 * <li>{@link #resetToBinary}</li>
 * <li>{@link #transcribe}</li>
 * <li>{@link #reachBookStep}</li>
 * <li>{@link #updateScores}</li>
 * <li>{@link #reduceScores}</li>
 * <li>{@link #getScores}</li>
 * </ul></dd>
 *
 * <dt>Symbols</dt>
 * <dd><ul>
 * <li>{@link #annotate}</li>
 * <li>{@link #getSampleRepository}</li>
 * <li>{@link #getSpecificSampleRepository}</li>
 * <li>{@link #hasAllocatedRepository}</li>
 * <li>{@link #hasSpecificRepository}</li>
 * <li>{@link #sample}</li>
 * </ul></dd>
 *
 * <dt>Artifacts</dt>
 * <dd><ul>
 * <li>{@link #getBrowserFrame}</li>
 * <li>{@link #getExportPathSansExt}</li>
 * <li>{@link #setExportPathSansExt}</li>
 * <li>{@link #export}</li>
 * <li>{@link #getPrintPath}</li>
 * <li>{@link #setPrintPath}</li>
 * <li>{@link #print}</li>
 * <li>{@link #getBookPath}</li>
 * <li>{@link #store}</li>
 * <li>{@link #storeBookInfo}</li>
 * <li>{@link #openSheetFolder}</li>
 * </ul></dd>
 * </dl>
 * <p>
 * <img src="doc-files/Book-Detail.png" alt="Book detals UML">
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(BasicBook.Adapter.class)
public interface Book
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** File name for book internals in book file system: {@value}. */
    static final String BOOK_INTERNALS = "book.xml";

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Write the book symbol annotations.
     */
    void annotate ();

    /**
     * Delete this book instance, as well as its related resources.
     */
    void close ();

    /**
     * Create as many sheet stubs as there are images in the input image file.
     * A created stub is nearly empty, the related image will have to be loaded later.
     *
     * @param sheetNumbers set of sheet numbers (1-based) explicitly included, null for all
     */
    void createStubs (SortedSet<Integer> sheetNumbers);

    /**
     * Insert stubs assemblies in UI tabbed pane.
     * GUI will focus on first valid stub, unless a stub number is provided.
     *
     * @param focus the stub number to focus upon, or null
     */
    void createStubsTabs (Integer focus);

    /**
     * Export this book scores using MusicXML format.
     * <p>
     * Assuming 'BOOK' is the radix of book name, several outputs can be considered:
     * <ul>
     * <li>If we don't use opus and the book contains a single score, it is exported as "BOOK.ext"
     * where "ext" is either "mxl" or "xml" depending upon whether compression is used.</li>
     * <li>If we don't use opus and the book contains several scores, it is exported as several
     * "BOOK.mvt#.ext" files, where "#" stands for the movement number and "ext" is either "mxl" or
     * "xml" depending upon whether compression is used.</li>
     * <li>If we use opus, everything goes into "BOOK.opus.mxl" as a single container file.</li>
     * </ul>
     */
    void export ();

    /**
     * Report the book name alias if any.
     *
     * @return book alias or null
     */
    String getAlias ();

    /**
     * Report the binarization filter defined at book level.
     *
     * @return the filter parameter
     */
    FilterParam getBinarizationFilter ();

    /**
     * Report where the book is kept.
     *
     * @return the book path
     */
    Path getBookPath ();

    /**
     * Create a dedicated frame, where book hierarchy can be browsed interactively.
     *
     * @return the created frame
     */
    JFrame getBrowserFrame ();

    /**
     * Report the path (without extension) where book is to be exported.
     *
     * @return the book export path without extension, or null
     */
    Path getExportPathSansExt ();

    /**
     * Report the first non-discarded stub in this book
     *
     * @return the first non-discarded stub, or null
     */
    SheetStub getFirstValidStub ();

    /**
     * Report the path name of the book image(s) input.
     *
     * @return the image input path
     */
    Path getInputPath ();

    /**
     * Report the lock that protects book project file.
     *
     * @return book project lock
     */
    Lock getLock ();

    /**
     * Report the OCR language(s) specification defined at book level, if any.
     *
     * @return the OCR language(s) spec
     */
    Param<String> getOcrLanguages ();

    /**
     * Report the offset of this book, with respect to a containing super-book.
     *
     * @return the offset (in terms of number of sheets)
     */
    Integer getOffset ();

    /**
     * Report the path, if any, where book is to be printed.
     *
     * @return the print path, or null
     */
    Path getPrintPath ();

    /**
     * Report the processing switches defined at book level, if any.
     *
     * @return the processing switches
     */
    ProcessingSwitches getProcessingSwitches ();

    /**
     * Report the radix of the file that corresponds to the book.
     * It is based on the simple file name of the book, with no path and no extension.
     *
     * @return the book input file radix
     */
    String getRadix ();

    /**
     * Report the sample repository (specific or global) to populate for this book
     *
     * @return a specific book repository if possible, otherwise the global one
     */
    SampleRepository getSampleRepository ();

    /**
     * Report the score which contains the provided page.
     *
     * @param page provided page
     * @return containing score (can it be null?)
     */
    Score getScore (Page page);

    /**
     * Report the scores (movements) detected in this book.
     *
     * @return the immutable list of scores
     */
    List<Score> getScores ();

    /**
     * Report (after allocation if needed) the book <b>specific</b> sample repository
     *
     * @return the repository instance with material for this book only, or null
     */
    SampleRepository getSpecificSampleRepository ();

    /**
     * Report the sheet stub with provided id (counted from 1).
     *
     * @param sheetId the desired value for sheet id
     * @return the proper sheet stub, or null if not found
     */
    SheetStub getStub (int sheetId);

    /**
     * Report all the sheets stubs contained in this book.
     *
     * @return the immutable list of sheets stubs, list may be empty but is never null
     */
    List<SheetStub> getStubs ();

    /**
     * Report the non-discarded sheets stubs in this book.
     *
     * @return the immutable list of valid sheets stubs
     */
    List<SheetStub> getValidStubs ();

    /**
     * Tell whether the book has allocated a dedicated sample repository.
     *
     * @return true if allocated
     */
    boolean hasAllocatedRepository ();

    /**
     * Tell whether the book has an existing specific sample repository.
     *
     * @return true if specific repository exists
     */
    boolean hasSpecificRepository ();

    /**
     * Hide stub assemblies of invalid sheets.
     */
    void hideInvalidStubs ();

    /**
     * Include a (sub) book into this (super) book.
     *
     * @param book the sub book to include
     */
    void includeBook (Book book);

    /**
     * Report whether this book is closing
     *
     * @return the closing flag
     */
    boolean isClosing ();

    /**
     * Report whether the book scores need to be reduced.
     *
     * @return true if dirty
     */
    boolean isDirty ();

    /**
     * Report whether the book has been modified with respect to its book data.
     *
     * @return true if modified
     */
    boolean isModified ();

    /**
     * Report whether this book contains several sheets.
     *
     * @return true for several sheets
     */
    boolean isMultiSheet ();

    /**
     * Actually load the image that corresponds to the specified sheet id.
     *
     * @param id specified sheet id
     * @return the loaded sheet image
     */
    BufferedImage loadSheetImage (int id);

    /**
     * Open (in the book zipped file) the folder for provided sheet number
     *
     * @param number sheet number (1-based) within the book
     * @return the path to sheet folder
     * @throws IOException if anything goes wrong
     */
    Path openSheetFolder (int number)
            throws IOException;

    /**
     * Print this book in PDF format.
     */
    void print ();

    /**
     * Print annotations of this book in PDF format.
     */
    void printAnnotations (Path bookPrintPath);

    /**
     * Reach a specific step (and all needed intermediate steps) on all valid sheets
     * of this book.
     *
     * @param target   the targeted step
     * @param force    if true and step already reached, sheet is reset and processed until step
     * @param sheetIds IDs of selected valid sheets, or null for all valid sheets
     * @return true if OK on all sheet actions
     */
    boolean reachBookStep (Step target,
                           boolean force,
                           Set<Integer> sheetIds);

    /**
     * Determine the logical parts of each score.
     *
     * @return the count of modifications done
     */
    int reduceScores ();

    /**
     * Remove the specified sheet stub from the containing book.
     * <p>
     * Typically, when the sheet carries no music information, it can be removed from the book
     * (without changing the IDs of the sibling sheets in the book)
     *
     * @param stub the sheet stub to remove
     * @return true if actually removed
     */
    boolean removeStub (SheetStub stub);

    /**
     * Reset all valid sheets of this book to their initial state.
     */
    void reset ();

    /**
     * Reset all valid sheets of this book to their ANNOTATIONS step.
     */
    void resetToAnnotations ();

    /**
     * Reset all valid sheets of this book to their BINARY step.
     */
    void resetToBinary ();

    /**
     * Write the book symbol samples into its sample repository.
     */
    void sample ();

    /**
     * Set the book alias
     *
     * @param alias the book alias
     */
    void setAlias (String alias);

    /**
     * Flag this book as closing.
     *
     * @param closing the closing to set
     */
    void setClosing (boolean closing);

    /**
     * Set the dirty flag.
     *
     * @param dirty the new flag value
     */
    void setDirty (boolean dirty);

    /**
     * Remember the path (without extension) where the book is to be exported.
     *
     * @param exportPathSansExt the book export path (without extension)
     */
    void setExportPathSansExt (Path exportPathSansExt);

    /**
     * Set the modified flag.
     *
     * @param modified the new flag value
     */
    void setModified (boolean modified);

    /**
     * Assign this book offset (WRT containing super-book)
     *
     * @param offset the offset to set
     */
    void setOffset (Integer offset);

    /**
     * Remember to which path book print data is to be written.
     *
     * @param printPath the print path
     */
    void setPrintPath (Path printPath);

    /**
     * Store book to disk.
     *
     * @param bookPath   target path for storing the book
     * @param withBackup if true, rename beforehand any existing target as a backup
     */
    void store (Path bookPath,
                boolean withBackup);

    /**
     * Store book to disk, using its current book path.
     */
    void store ();

    /**
     * Store the book information (global info + stub steps) into book file system.
     *
     * @param root root path of book file system
     * @throws Exception if anything goes wrong
     */
    void storeBookInfo (Path root)
            throws Exception;

    /**
     * Swap all sheets, except the current one if any.
     */
    void swapAllSheets ();

    /**
     * Convenient method to perform all needed transcription steps on all valid sheets
     * of this book and building the book score(s).
     *
     * @return true if OK
     */
    boolean transcribe ();

    /** Update the gathering of sheet pages into scores.
     *
     * @param stub current stub
     */
    void updateScores (SheetStub stub);
}
