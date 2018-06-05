//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S h e e t                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.classifier.AnnotationIndex;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.dynamic.FilamentIndex;
import org.audiveris.omr.glyph.ui.GlyphsController;
import org.audiveris.omr.glyph.ui.SymbolsEditor;
import org.audiveris.omr.lag.LagManager;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.sig.InterIndex;
import org.audiveris.omr.sig.ui.InterController;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.ui.ErrorsEditor;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.util.ItemRenderer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code Sheet} corresponds to one image in a book image file.
 * <p>
 * If a movement break occurs in the middle of a sheet, this sheet will contain at least two pages,
 * but in most cases there is exactly one {@link Page} instance per Sheet instance.
 *
 * Methods are organized as follows:
 * <dl>
 * <dt>Administration</dt>
 * <dd><ul>
 * <li>{@link #getId}</li>
 * <li>{@link #getStub}</li>
 * <li>{@link #afterReload}</li>
 * <li>{@link #getLagManager}</li>
 * <li>{@link #getFilamentIndex}</li>
 * <li>{@link #getGlyphIndex}</li>
 * <li>{@link #getInterIndex}</li>
 * <li>{@link #getPersistentIdGenerator}</li>
 * </ul></dd>
 *
 * <dt>Pages, Systems and Staves</dt>
 * <dd><ul>
 * <li>{@link #addPage}</li>
 * <li>{@link #getPages}</li>
 * <li>{@link #getStaffManager}</li>
 * <li>{@link #getSystemManager}</li>
 * <li>{@link #getSystems}</li>
 * </ul></dd>
 *
 * <dt>Symbols</dt>
 * <dd><ul>
 * <li>{@link #annotate}</li>
 * <li>{@link #sample}</li>
 * </ul></dd>
 *
 * <dt>Artifacts</dt>
 * <dd><ul>
 * <li>{@link #setImage}</li>
 * <li>{@link #hasPicture}</li>
 * <li>{@link #getPicture}</li>
 * <li>{@link #getHeight}</li>
 * <li>{@link #getWidth}</li>
 * <li>{@link #setScale}</li>
 * <li>{@link #getScale}</li>
 * <li>{@link #getInterline}</li>
 * <li>{@link #setSkew}</li>
 * <li>{@link #getSkew}</li>
 * <li>{@link #store}</li>
 * <li>{@link #print}</li>
 * <li>{@link #export}</li>
 * <li>{@link #getSheetDelta}</li>
 * </ul></dd>
 *
 * <dt>UI</dt>
 * <dd><ul>
 * <li>{@link #getSymbolsEditor}</li>
 * <li>{@link #displayDataTab}</li>
 * <li>{@link #displayMainTabs}</li>
 * <li>{@link #getErrorsEditor}</li>
 * <li>{@link #getGlyphsController}</li>
 * <li>{@link #getInterController}</li>
 * <li>{@link #getLocationService}</li>
 * <li>{@link #addItemRenderer}</li>
 * <li>{@link #renderItems}</li>
 * </ul></dd>
 * </dl>
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
     * Save sheet symbols annotations into the provided folder.
     *
     * @param sheetFolder target folder (perhaps in a zip file system)
     */
    void annotate (Path sheetFolder);

    /**
     * Save sheet symbols annotations.
     */
    void annotate ();

    /**
     * Display the ANNOTATION_TAB.
     */
    void displayAnnotationTab ();

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
     *
     * @param path sheet export path
     */
    void export (Path path);

    /**
     * Report the global index for annotations of this sheet
     *
     * @return the index for annotations
     */
    AnnotationIndex getAnnotationIndex ();

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
     * Report the global index for glyphs of this sheet
     *
     * @return the index for glyphs
     */
    GlyphIndex getGlyphIndex ();

    /**
     * In non batch mode, report the UI module for symbol assignment in this sheet
     *
     * @return the glyphs controller
     */
    GlyphsController getGlyphsController ();

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
     * In non batch mode, report the UI module for inter management in this sheet
     *
     * @return the inter controller
     */
    InterController getInterController ();

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
     * Access to the generator of persistent IDs for this sheet.
     *
     * @return the ID generator
     */
    AtomicInteger getPersistentIdGenerator ();

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
     * Save sheet samples into book repository.
     */
    void sample ();

    /**
     * Assign the related image to this sheet
     *
     * @param image the loaded image
     * @throws StepException if processing failed at this step
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
     * @param sheetFolder    path of sheet folder in (new) book file
     * @param oldSheetFolder path of sheet folder in old book file, if any
     */
    void store (Path sheetFolder,
                Path oldSheetFolder);
}
