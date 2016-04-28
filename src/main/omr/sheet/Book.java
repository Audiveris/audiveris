//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             B o o k                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.image.FilterDescriptor;

import omr.score.Score;

import omr.script.Script;

import omr.step.Step;

import omr.util.Param;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;

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
 * <li>{@link #includeBook}</li>
 * <li>{@link #getOffset}</li>
 * <li>{@link #setOffset}</li>
 * <li>{@link #getInputPath}</li>
 * <li>{@link #getRadix}</li>
 * <li>{@link #getLogPrefix}</li>
 * <li>{@link #close}</li>
 * <li>{@link #isClosing}</li>
 * <li>{@link #setClosing}</li>
 * </ul></dd>
 *
 * <dt>Sheets</dt>
 * <dd><ul>
 * <li>{@link #createStubs}</li>
 * <li>{@link #loadSheetImage}</li>
 * <li>{@link #isMultiSheet}</li>
 * <li>{@link #getStub}</li>
 * <li>{@link #getStubs}</li>
 * <li>{@link #removeStub}</li>
 * </ul></dd>
 *
 * <dt>Transcription</dt>
 * <dd><ul>
 * <li>{@link #transcribe}</li>
 * <li>{@link #doStep}</li>
 * <li>{@link #buildScores}</li>
 * <li>{@link #getScores}</li>
 * </ul></dd>
 *
 * <dt>Parameters</dt>
 * <dd><ul>
 * <li>{@link #getFilterParam}</li>
 * <li>{@link #getLanguageParam}</li>
 * </ul></dd>
 *
 * <dt>Artifacts</dt>
 * <dd><ul>
 * <li>{@link #getBrowserFrame}</li>
 * <li>{@link #getExportPathSansExt}</li>
 * <li>{@link #setExportPathSansExt}</li>
 * <li>{@link #export}</li>
 * <li>{@link #deleteExport}</li>
 * <li>{@link #getPrintPath}</li>
 * <li>{@link #setPrintPath}</li>
 * <li>{@link #print}</li>
 * <li>{@link #getScript}</li>
 * <li>{@link #getScriptPath}</li>
 * <li>{@link #setScriptPath}</li>
 * <li>{@link #getBookPath}</li>
 * <li>{@link #store}</li>
 * </ul></dd>
 * </dl>
 * <p>
 * <img src="doc-files/Book.png">
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
     * Determine scores by gathering and connecting sheets pages.
     */
    void buildScores ();

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
     */
    void createStubsTabs ();

    /**
     * Delete the exported MusicXML, if any.
     */
    void deleteExport ();

    /**
     * Display all stubs assemblies, including the invalid ones.
     */
    void displayAllStubs ();

    /**
     * Perform a specific step (and all needed intermediate steps) on all or some sheets
     * of this book.
     *
     * @param target   the targeted step
     * @param sheetIds specific set of sheet IDs if any, null for all
     * @return true if OK on all sheet actions
     */
    boolean doStep (Step target,
                    SortedSet<Integer> sheetIds);

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
     * Report the binarization filter defined at book level.
     *
     * @return the filter param
     */
    Param<FilterDescriptor> getFilterParam ();

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
     * Report the OCR language(s) specification defined at book level
     *
     * @return the OCR language(s) spec
     */
    Param<String> getLanguageParam ();

    /**
     * Report the proper prefix to use when logging a user message related to this book
     *
     * @return the proper prefix
     */
    String getLogPrefix ();

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
     * Report where the book book is kept.
     *
     * @return the book book path
     */
    Path getBookPath ();

    /**
     * Report the radix of the file that corresponds to the book.
     * It is based on the simple file name of the book, with no path and no extension.
     *
     * @return the book input file radix
     */
    String getRadix ();

    /**
     * Report the scores (movements) detected in this book.
     *
     * @return the immutable list of scores
     */
    List<Score> getScores ();

    /**
     * Report the script of actions performed on this book.
     *
     * @return the book script
     */
    Script getScript ();

    /**
     * Report the path, if any, where the book script should be written.
     *
     * @return the related script path or null
     */
    Path getScriptPath ();

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
     */
    Path openSheetFolder (int number);

    /**
     * Print this book in PDF format.
     */
    void print ();

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
     * Remember the path (without extension) where the book is to be exported.
     *
     * @param exportPathSansExt the book export path (without extension)
     */
    void setExportPathSansExt (Path exportPathSansExt);

    /**
     * Set the modified flag.
     *
     * @param val the new flag value
     */
    void setModified (boolean val);

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
     * Remember the path where the book script is written.
     *
     * @param scriptPath the related script path
     */
    void setScriptPath (Path scriptPath);

    /**
     * Store book book to disk.
     *
     * @param bookPath target path for storing the book
     * @param withBackup  if true, rename beforehand any existing target as a backup
     */
    void store (Path bookPath,
                boolean withBackup);

    /**
     * Store book book to disk, using its current book path.
     */
    void store ();

    /**
     * Swap all sheets, except the current one if any.
     */
    void swapAllSheets ();

    /**
     * Convenient method to perform all needed transcription steps on (all or some sheets
     * of) this book.
     *
     * @return true if OK
     * @param sheetIds specific set of sheet IDs if any, null for all
     */
    boolean transcribe (SortedSet<Integer> sheetIds);
}
