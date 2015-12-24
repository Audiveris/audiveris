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

import omr.glyph.GlyphIndex;
import omr.glyph.dynamic.FilamentIndex;
import omr.glyph.ui.SymbolsController;
import omr.glyph.ui.SymbolsEditor;

import omr.lag.LagManager;

import omr.score.Page;

import omr.selection.SelectionService;

import omr.sheet.ui.SheetAssembly;

import omr.sig.InterIndex;

import omr.step.Step;
import omr.step.StepException;

import omr.ui.ErrorsEditor;
import omr.ui.util.ItemRenderer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code Sheet} corresponds to one image in a book image file.
 * <p>
 * If a movement break occurs in the middle of a sheet, this sheet will contain at least two pages,
 * but in most cases there is exactly one {@link Page} instance per Sheet instance.
 * <p>
 * Methods are organized as follows:
 * <dl>
 * <dt>Admin</dt>
 * <dd><ul>
 * <li>{@link #getId}</li>
 * <li>{@link #getLogPrefix}</li>
 * <li>{@link #getBook}</li>
 * <li>{@link #getNumber}</li>
 * <li>{@link #setImage}</li>
 * <li>{@link #close}</li>
 * </ul></dd>
 *
 * <dt>Pages</dt>
 * <dd><ul>
 * <li>{@link #addPage}</li>
 * <li>{@link #getPages}</li>
 * </ul></dd>
 *
 * <dt>Transcription</dt>
 * <dd><ul>
 * <li>{@link #transcribe}</li>
 * <li>{@link #doStep}</li>
 * <li>{@link #isDone}</li>
 * <li>{@link #ensureStep}</li>
 * <li>{@link #getCurrentStep}</li>
 * <li>{@link #getLatestStep}</li>
 * </ul></dd>
 *
 * <dt>Companions</dt>
 * <dd><ul>
 * <li>{@link #getSystemManager}</li>
 * <li>{@link #getStaffManager}</li>
 * <li>{@link #getLagManager}</li>
 * <li>{@link #getInterIndex}</li>
 * <li>{@link #getGlyphIndex}</li>
 * <li>{@link #getFilamentIndex}</li>
 * </ul></dd>
 *
 * <dt>Artifacts</dt>
 * <dd><ul>
 * <li>{@link #getPicture}</li>
 * <li>{@link #getWidth}</li>
 * <li>{@link #getHeight}</li>
 * <li>{@link #getScale}</li>
 * <li>{@link #setScale}</li>
 * <li>{@link #getInterline}</li>
 * <li>{@link #getSkew}</li>
 * <li>{@link #setSkew}</li>
 * <li>{@link #getSystems}</li>
 * <li>{@link #getSheetDelta}</li>
 * <li>{@link #export}</li>
 * <li>{@link #deleteExport}</li>
 * <li>{@link #print}</li>
 * <li>{@link #store}</li>
 * </ul></dd>
 *
 * <dt>UI</dt>
 * <dd><ul>
 * <li>{@link #getLocationService}</li>
 * <li>{@link #getAssembly}</li>
 * <li>{@link #getSymbolsEditor}</li>
 * <li>{@link #getErrorsEditor}</li>
 * <li>{@link #getSymbolsController}</li>
 * <li>{@link #addItemRenderer}</li>
 * <li>{@link #renderItems}</li>
 * </ul></dd>
 * </dl>
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(BasicSheet.Adapter.class)
public interface Sheet
        extends SheetStub
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The radix used for folder of this sheet internals. */
    static final String INTERNALS_RADIX = "sheet#";

    //~ Methods ------------------------------------------------------------------------------------
    // -------------
    // --- Admin ---
    // -------------
    //
    /**
     * Complete sheet initialization, after reload.
     *
     * @param stub the sheet stub
     */
    void afterReload (SheetStub stub);

    /**
     * Report the distinguished name for this sheet.
     *
     * @return sheet name
     */
    @Override
    String getId ();

    /**
     * Report the related sheet stub.
     *
     * @return the related stub (non null)
     */
    SheetStub getStub ();

    /**
     * Report the containing book.
     *
     * @return containing book
     */
    @Override
    Book getBook ();

    /**
     * Report the number for this sheet in containing book
     *
     * @return the sheet index number (1-based) in containing book
     */
    @Override
    int getNumber ();

    /**
     * Assign the related image to this sheet
     *
     * @param image the loaded image
     * @throws StepException
     */
    void setImage (BufferedImage image)
            throws StepException;

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
     * Convenient method to perform all needed transcription steps on this sheet.
     *
     * @return true if OK
     */
    boolean transcribe ();

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
    @Override
    boolean isDone (Step step);

    /**
     * Make sure the provided step has been reached on this sheet
     *
     * @param step the step to check
     * @return true if OK
     */
    @Override
    boolean ensureStep (Step step);

    /**
     * Report the step being processed, if any.
     *
     * @return the current step or null
     */
    @Override
    Step getCurrentStep ();

    /**
     * Report the latest step done so far on this sheet.
     *
     * @return the latest step done, or null
     */
    @Override
    Step getLatestStep ();

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
     * Access to the Inter index for this sheet
     *
     * @return the sheet Inter's index
     */
    InterIndex getInterIndex ();

    /**
     * Report the global nest for glyphs of this sheet, or null
     *
     * @return the nest for glyphs, perhaps null
     */
    GlyphIndex getGlyphIndex ();

    /**
     * Report the global index for filaments of this sheet, or null
     *
     * @return the index for filaments, perhaps null
     */
    FilamentIndex getFilamentIndex ();

    // -----------------
    // --- Artifacts ---
    // -----------------
    //
    /**
     * Report the picture of this sheet, that provides sources and tables.
     *
     * @return the related picture
     */
    Picture getPicture ();

    /**
     * Report whether the Picture instance exists in sheet.
     *
     * @return true if so
     */
    boolean hasPicture ();

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
     * Remember scale information to this sheet
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
     * Convenient way to get an unmodifiable view on sheet systems.
     *
     * @return a view on systems list
     */
    List<SystemInfo> getSystems ();

    /**
     * Report the measured difference between entities and pixels.
     *
     * @return the sheetDelta
     */
    SheetDiff getSheetDelta ();

    /**
     * Export a single sheet in MusicXML.
     * <p>
     * The output is structured differently according to whether the sheet contains one or several
     * pages.<ul>
     * <li>A single-page sheet results in one score output.</li>
     * <li>A multi-page sheet results in one opus output (if useOpus is set) or a series of scores
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
     *
     * @param sheetPrintPath path of sheet print file
     */
    void print (Path sheetPrintPath);

    /**
     * Store sheet internals into book project file system.
     *
     * @param sheetPath    path of sheet in new) project file
     * @param oldSheetPath path of sheet in old project file, if any
     */
    void store (Path sheetPath,
                Path oldSheetPath);

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
    @Override
    SheetAssembly getAssembly ();

    /**
     * In non batch mode, report the editor dealing with symbols recognition in this sheet
     *
     * @return the symbols editor, or null
     */
    SymbolsEditor getSymbolsEditor ();

    /**
     * In non batch mode, report the editor dealing with detected errors in this sheet
     *
     * @return the errors editor, or null
     */
    ErrorsEditor getErrorsEditor ();

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

    /**
     * Display the main tabs related to this sheet.
     */
    void displayMainTabs ();

    /**
     * Display the DATA_TAB.
     */
    void displayDataTab ();
}
