//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             B o o k                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
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
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;

import javax.swing.JFrame;

/**
 * Interface {@code Book} is the root class for handling a physical set of image input
 * files, resulting in one or several logical MusicXML scores.
 * <p>
 * A book instance generally corresponds to an input file containing one or several images, each
 * image resulting in a separate {@link Sheet} instance.
 * <p>
 * A sheet generally contains one or several systems. An indented system (prefixed by part names)
 * usually indicates the beginning of a movement. Such indented system may appear in the middle of a
 * sheet, thus (logical) movement frontiers do not always match (physical) sheet frontiers.
 * <p>
 * A (super-) book may also contain (sub-) books to recursively gather a sequence of input files.
 * <p>
 * <img src="doc-files/Book.png" />
 *
 * @author Hervé Bitteur
 */
public interface Book
{
    //~ Methods ------------------------------------------------------------------------------------

    // -------------
    // --- Admin ---
    // -------------
    //
    /**
     * Include a (sub) book into this (super) one.
     *
     * @param book the sub book to include
     */
    void includeBook (Book book);

    /**
     * Report the offset of this book, with respect to a containing super-book.
     *
     * @return the offset (in terms of number of sheets)
     */
    int getOffset ();

    /**
     * Assign this book offset (WRT containing super-book)
     *
     * @param offset the offset to set
     */
    void setOffset (int offset);

    /**
     * Report the path name of the book image(s) input.
     *
     * @return the image input path
     */
    Path getInputPath ();

    /**
     * Report the radix of the file that corresponds to the book.
     * It is based on the simple file name of the book, with no path and no extension.
     *
     * @return the book input file radix
     */
    String getRadix ();

    /**
     * Report the proper prefix to use when logging a user message related to this book
     *
     * @return the proper prefix
     */
    String getLogPrefix ();

    /**
     * Delete this book instance, as well as its related resources.
     */
    void close ();

    /**
     * Report whether this book is being closed
     *
     * @return the closing flag
     */
    boolean isClosing ();

    /**
     *
     * @param closing the closing to set
     */
    void setClosing (boolean closing);

    // --------------
    // --- Sheets ---
    // --------------
    //
    /**
     * Create as many sheets as there are images in the input image file.
     * A created sheet is nearly empty, the related image will have to be loaded later.
     *
     * @param sheetIds set of sheet IDs (1-based) explicitly included, null for all
     */
    void createSheets (SortedSet<Integer> sheetIds);

    /**
     * Actually load the image that corresponds to the specified sheet id.
     *
     * @param id specified sheet id
     * @return the loaded sheet image
     */
    BufferedImage loadSheetImage (int id);

    /**
     * Report whether this book contains several sheets.
     *
     * @return true for several sheets
     */
    boolean isMultiSheet ();

    /**
     * Report the sheet with provided id (counted from 1).
     *
     * @param sheetId the desired value for sheet id
     * @return the proper sheet, or null if not found
     */
    Sheet getSheet (int sheetId);

    /**
     * Report the sheets contained in this book.
     *
     * @return the immutable list of sheets
     */
    List<Sheet> getSheets ();

    /**
     * Remove the specified sheet from the containing book.
     * <p>
     * Typically, when the sheet carries no music information, it can be removed from the book
     * (without changing the IDs of the sibling sheets in the book)
     *
     * @param sheet the sheet to remove
     * @return true if actually removed
     */
    boolean removeSheet (Sheet sheet);

    // ---------------------
    // --- Transcription ---
    // ---------------------
    //
    /**
     * Convenient method to perform all needed transcription steps on (all or some sheets
     * of) this book.
     *
     * @return true if OK
     * @param sheetIds specific set of sheet IDs if any, null for all
     */
    boolean transcribe (SortedSet<Integer> sheetIds);

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
     * Determine scores by gathering and connecting sheets pages.
     */
    void buildScores ();

    /**
     * Report the scores (movements) detected in this book.
     *
     * @return the immutable list of scores
     */
    List<Score> getScores ();

    // ------------------
    // --- Parameters ---
    // ------------------
    //
    /**
     * Report the binarization filter defined at book level.
     *
     * @return the filter param
     */
    Param<FilterDescriptor> getFilterParam ();

    /**
     * Report the OCR language(s) specification defined at book level
     *
     * @return the OCR language(s) spec
     */
    Param<String> getLanguageParam ();

    // -----------------
    // --- Artifacts ---
    // -----------------
    //
    /**
     * Report the related book bench.
     *
     * @return the related bench
     */
    BookBench getBench ();

    /**
     * Create a dedicated frame, where book hierarchy can be browsed interactively.
     *
     * @return the created frame
     */
    JFrame getBrowserFrame ();

    /**
     * Report the path, if any and sans extension, where book is to be exported.
     *
     * @return the book export path sans extension, or null
     */
    Path getExportPath ();

    /**
     * Remember where the book is to be exported.
     *
     * @param exportPath the book export path (without .xml or .mxl extension)
     */
    void setExportPath (Path exportPath);

    /**
     * Export this book scores using MusicXML format.
     */
    void export ();

    /**
     * Delete the exported MusicXML, if any.
     */
    void deleteExport ();

    /**
     * Report the path, if any and sans extension, where book is to be printed.
     *
     * @return the print path, or null
     */
    Path getPrintPath ();

    /**
     * Remember to which path book print data is to be written.
     *
     * @param printPath the print path
     */
    void setPrintPath (Path printPath);

    /**
     * Print this book in PDF format.
     */
    void print ();

    /**
     * Report the script of actions performed on this book.
     *
     * @return the book script
     */
    Script getScript ();

    /**
     * Report the file, if any, where the book script should be written.
     *
     * @return the related script file or null
     */
    File getScriptFile ();

    /**
     * Remember the file where the book script is written.
     *
     * @param scriptFile the related script file
     */
    void setScriptFile (File scriptFile);
}
