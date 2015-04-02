//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S h e e t                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import java.awt.Graphics2D;
import omr.glyph.GlyphNest;

import omr.image.FilterDescriptor;

import omr.lag.LagManager;

import omr.math.Population;

import omr.score.entity.Page;

import omr.selection.SelectionService;

import omr.sheet.stem.StemScale;
import omr.sheet.ui.SheetAssembly;

import omr.sig.InterManager;

import omr.step.Step;
import omr.step.StepException;

import omr.util.LiveParam;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import omr.glyph.ui.SymbolsController;
import omr.glyph.ui.SymbolsEditor;
import omr.ui.ErrorsEditor;
import omr.ui.util.ItemRenderer;

/**
 * Interface {@code Sheet} corresponds to an image in a book image file.
 * <p>
 * If a movement break occurs in the middle of a sheet, this sheet will contain several pages, but
 * in most cases there is exactly one {@link Page} instance per Sheet instance.
 *
 * @author Hervé Bitteur
 */
public interface Sheet
{
    //~ Methods ------------------------------------------------------------------------------------

    // -------------
    // --- Admin ---
    // -------------
    //
    /**
     * Report the distinguished name for this sheet.
     *
     * @return sheet name
     */
    String getId ();

    /**
     * Report the proper prefix to use when logging a message
     *
     * @return the proper prefix
     */
    String getLogPrefix ();

    /**
     * Report the containing book.
     *
     * @return containing book
     */
    Book getBook ();

    /**
     * @return the sheet index (1-based) in containing book
     */
    int getIndex ();

    /**
     * Assign the related image to this sheet
     *
     * @param image the loaded image
     * @throws StepException
     */
    void setImage (BufferedImage image) throws StepException;

    /**
     * Close this sheet, and remove it from the containing book.
     *
     * @param bookIsClosing true if book is being closed
     */
    void close (boolean bookIsClosing);

    // -------------
    // --- Pages ---
    // -------------
    //
    /**
     * Add a related page to this sheet.
     *
     * @param page the detected page
     */
    void addPage (Page page);

    /**
     * Report the collections of pages found in this sheet (generally just one).
     *
     * @return the list of page(s)
     */
    List<Page> getPages ();

    // ---------------------
    // --- Transcription ---
    // ---------------------
    //
    /**
     * Perform a step, including intermediate ones if any, with online progress monitor.
     * If any step throws {@link StepException} the processing is stopped.
     *
     * @param target  the targeted step
     * @param systems the impacted systems (null for all of them)
     * @return true if OK
     */
    boolean doStep (Step target,
                    Collection<SystemInfo> systems);

    /**
     * Report whether the specified step has been performed on this sheet
     *
     * @param step the step to check
     * @return true if already performed
     */
    boolean isDone (Step step);

    /**
     * Make sure the provided step has been reached on this sheet
     *
     * @param step the step to check
     * @return true if OK
     */
    boolean ensureStep (Step step);

    /**
     * Retrieve the step being processed "as we speak"
     *
     * @return the current step
     */
    Step getCurrentStep ();

    /**
     * Report the latest step done so far on this sheet.
     *
     * @return the latest step done, or null
     */
    Step getLatestStep ();

    // ------------------
    // --- Parameters ---
    // ------------------
    //
    /**
     * Report the binarization filter defined at sheet level.
     *
     * @return the filter param
     */
    LiveParam<FilterDescriptor> getFilterParam ();

    /**
     * Report the OCR language(s) specification defined at sheet level
     *
     * @return the OCR language(s) spec
     */
    LiveParam<String> getLanguageParam ();

    // ------------------
    // --- Companions ---
    // ------------------
    //
    /**
     * Access to the system manager for this sheet
     *
     * @return the SystemManager instance
     */
    SystemManager getSystemManager ();

    /**
     * Access to the staff manager for this sheet
     *
     * @return the staff Manager
     */
    StaffManager getStaffManager ();

    /**
     * Access to the lag manager for this sheet
     *
     * @return the lag Manager
     */
    LagManager getLagManager ();

    /**
     * Access to the Inter manager for this sheet
     *
     * @return the sheet Inter's manager
     */
    InterManager getInterManager ();

    /**
     * Report the global nest for glyphs of this sheet, or null
     *
     * @return the nest for glyphs, perhaps null
     */
    GlyphNest getGlyphNest ();

    // -----------------
    // --- Artifacts ---
    // -----------------
    //
    /**
     * Report the related sheet bench
     *
     * @return the related bench
     */
    SheetBench getBench ();

    /**
     * Report the picture of this sheet, that is the image to be processed.
     *
     * @return the related picture
     */
    Picture getPicture ();

    /**
     * Report the picture width in pixels
     *
     * @return the picture width
     */
    int getWidth ();

    /**
     * Report the picture height in pixels
     *
     * @return the picture height
     */
    int getHeight ();

    /**
     * Report the computed scale of this sheet.
     * This drives several processing thresholds.
     *
     * @return the sheet scale
     */
    Scale getScale ();

    /**
     * Link scale information to this sheet
     *
     * @param scale the computed sheet global scale
     */
    void setScale (Scale scale);

    /**
     * Convenient method to report the key scaling information of the sheet
     *
     * @return the scale interline value
     */
    int getInterline ();

    /**
     * Remember the stem scale observed
     *
     * @param stemScale the stem scaling data
     */
    void setStemScale (StemScale stemScale);

    /**
     * Report the most frequent stem thickness within the sheet
     *
     * @return the main Stem thickness
     */
    int getMainStem ();

    /**
     * Report the maximum stem thickness within the sheet
     *
     * @return the maximum Stem thickness
     */
    int getMaxStem ();

    /**
     * Report the skew information for this sheet.
     *
     * @return the skew information
     */
    Skew getSkew ();

    /**
     * Link skew information to this sheet
     *
     * @param skew the skew information
     */
    void setSkew (Skew skew);

    /**
     * Report the population of vertical gaps observed between items of a beam group
     *
     * @return the beamGaps
     */
    Population getBeamGaps ();

    /**
     * Remember the population of beam gaps
     *
     * @param beamGaps the beamGaps to set
     */
    void setBeamGaps (Population beamGaps);

    /**
     * Convenient way to get an unmodifiable view on sheet systems.
     *
     * @return a view on systems list
     */
    List<SystemInfo> getSystems ();

    /**
     * @return the sheetDelta
     */
    SheetDiff getSheetDelta ();

    /**
     * Export a single sheet in MusicXML.
     * <p>
     * The output is structured differently according to whether the sheet contains one or several
     * pages.<ul>
     * <li>A single-page sheet results in one score output.</li>
     * <li>A multi-page sheet results in one opus output (if useOpus is set) or a folder of scores
     * (is useOpus is not set).</li>
     * </ul>
     */
    void export ();

    /**
     * Delete the sheet exported MusicXML, if any.
     */
    void deleteExport ();

    /**
     * Print the sheet physical appearance using PDF format.
     */
    void print ();

    // ----------
    // --- UI ---
    // ----------
    //
    /**
     * In non batch mode, give access to sheet location service.
     *
     * @return the selection service dedicated to location in sheet (null in batch mode)
     */
    SelectionService getLocationService ();

    /**
     * In non batch mode, report the related SheetAssembly for GUI
     *
     * @return the sheet UI assembly, or null in batch mode
     */
    SheetAssembly getAssembly ();

    /**
     * In non batch mode, report the editor dealing with detected errors in this sheet
     *
     * @return the errors editor, or null
     */
    ErrorsEditor getErrorsEditor ();

    /**
     * In non batch mode, report the editor dealing with symbols recognition in this sheet
     *
     * @return the symbols editor, or null
     */
    SymbolsEditor getSymbolsEditor ();

    /**
     * In non batch mode, report the UI module for symbol assignment in this sheet
     *
     * @return the symbols controller
     */
    SymbolsController getSymbolsController ();

    /**
     * In non batch mode, register a class instance to render items on top of UI views.
     *
     * @param renderer an item renderer
     * @return true if renderer was added, false in batch
     */
    boolean addItemRenderer (ItemRenderer renderer);

    /**
     * In non batch mode, apply the registered item renderings on the provided graphics.
     *
     * @param g the graphics context
     */
    void renderItems (Graphics2D g);

}
