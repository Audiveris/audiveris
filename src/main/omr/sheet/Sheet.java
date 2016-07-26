//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S h e e t                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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

import omr.sig.InterIndex;
import omr.sig.inter.Inter;
import omr.sig.relation.CrossExclusion;

import omr.step.StepException;

import omr.ui.ErrorsEditor;
import omr.ui.selection.SelectionService;
import omr.ui.util.ItemRenderer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code Sheet} corresponds to one image in a book image file.
 * <p>
 * If a movement break occurs in the middle of a sheet, this sheet will contain at least two pages,
 * but in most cases there is exactly one {@link Page} instance per Sheet instance.
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(BasicSheet.Adapter.class)
public interface Sheet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The radix used for folder of this sheet internals. */
    static final String INTERNALS_RADIX = "sheet#";

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * In non batch mode, register a class instance to render items on top of UI views.
     *
     * @param renderer an item renderer
     * @return true if renderer was added, false in batch
     */
    boolean addItemRenderer (ItemRenderer renderer);

    /**
     * Add a related page to this sheet.
     *
     * @param page the detected page
     */
    void addPage (Page page);

    /**
     * Complete sheet initialization, after reload.
     *
     * @param stub the sheet stub
     */
    void afterReload (SheetStub stub);

    /**
     * Delete the sheet exported MusicXML, if any.
     */
    void deleteExport ();

    /**
     * Display the DATA_TAB.
     */
    void displayDataTab ();

    /**
     * Display the main tabs related to this sheet.
     */
    void displayMainTabs ();

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
     * Report the cross-system exclusions.
     *
     * @return the map of cross-exclusions
     */
    Map<Inter, List<CrossExclusion>> getCrossExclusions ();

    /**
     * In non batch mode, report the editor dealing with detected errors in this sheet
     *
     * @return the errors editor, or null
     */
    ErrorsEditor getErrorsEditor ();

    /**
     * Report the global index for filaments of this sheet, or null
     *
     * @return the index for filaments, perhaps null
     */
    FilamentIndex getFilamentIndex ();

    /**
     * Report the global nest for glyphs of this sheet, or null
     *
     * @return the nest for glyphs, perhaps null
     */
    GlyphIndex getGlyphIndex ();

    /**
     * Report the picture height in pixels
     *
     * @return the picture height
     */
    int getHeight ();

    /**
     * Report the distinguished name for this sheet.
     *
     * @return sheet name
     */
    String getId ();

    /**
     * Access to the Inter index for this sheet
     *
     * @return the sheet Inter's index
     */
    InterIndex getInterIndex ();

    /**
     * Convenient method to report the key scaling information of the sheet
     *
     * @return the scale interline value
     */
    int getInterline ();

    /**
     * Access to the lag manager for this sheet
     *
     * @return the lag Manager
     */
    LagManager getLagManager ();

    /**
     * Access to the generator of persistent IDs for this sheet.
     *
     * @return the ID generator
     */
    AtomicInteger getPersistentIdGenerator ();

    /**
     * In non batch mode, give access to sheet location service.
     *
     * @return the selection service dedicated to location in sheet (null in batch mode)
     */
    SelectionService getLocationService ();

    /**
     * Report the collections of pages found in this sheet (generally just one).
     *
     * @return the list of page(s)
     */
    List<Page> getPages ();

    /**
     * Report the picture of this sheet, that provides sources and tables.
     *
     * @return the related picture
     */
    Picture getPicture ();

    /**
     * Report the computed scale of this sheet.
     * This drives several processing thresholds.
     *
     * @return the sheet scale
     */
    Scale getScale ();

    /**
     * Report the measured difference between entities and pixels.
     *
     * @return the sheetDelta
     */
    SheetDiff getSheetDelta ();

    /**
     * Report the skew information for this sheet.
     *
     * @return the skew information
     */
    Skew getSkew ();

    /**
     * Access to the staff manager for this sheet
     *
     * @return the staff Manager
     */
    StaffManager getStaffManager ();

    /**
     * Report the related sheet stub.
     *
     * @return the related stub (non null)
     */
    SheetStub getStub ();

    /**
     * In non batch mode, report the UI module for symbol assignment in this sheet
     *
     * @return the symbols controller
     */
    SymbolsController getSymbolsController ();

    /**
     * In non batch mode, report the editor dealing with symbols recognition in this sheet
     *
     * @return the symbols editor, or null
     */
    SymbolsEditor getSymbolsEditor ();

    /**
     * Access to the system manager for this sheet
     *
     * @return the SystemManager instance
     */
    SystemManager getSystemManager ();

    /**
     * Convenient way to get an unmodifiable view on sheet systems.
     *
     * @return a view on systems list
     */
    List<SystemInfo> getSystems ();

    /**
     * Report the picture width in pixels
     *
     * @return the picture width
     */
    int getWidth ();

    /**
     * Report whether the Picture instance exists in sheet.
     *
     * @return true if so
     */
    boolean hasPicture ();

    /**
     * Print the sheet physical appearance using PDF format.
     *
     * @param sheetPrintPath path of sheet print file
     */
    void print (Path sheetPrintPath);

    /**
     * In non batch mode, apply the registered item renderings on the provided graphics.
     *
     * @param g the graphics context
     */
    void renderItems (Graphics2D g);

    /**
     * Assign the related image to this sheet
     *
     * @param image the loaded image
     * @throws StepException
     */
    void setImage (BufferedImage image)
            throws StepException;

    /**
     * Remember scale information to this sheet
     *
     * @param scale the computed sheet global scale
     */
    void setScale (Scale scale);

    /**
     * Link skew information to this sheet
     *
     * @param skew the skew information
     */
    void setSkew (Skew skew);

    /**
     * Store sheet internals into book file system.
     *
     * @param sheetPath    path of sheet in new) book file
     * @param oldSheetPath path of sheet in old book file, if any
     */
    void store (Path sheetPath,
                Path oldSheetPath);
}
