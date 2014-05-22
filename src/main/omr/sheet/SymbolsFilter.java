//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l s F i l t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.image.ImageUtil;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;
import omr.lag.SectionsBuilder;

import omr.run.Orientation;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.sheet.ui.ImageView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.ScrollImageView;

import omr.ui.BoardsPane;
import omr.ui.util.ItemRenderer;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class {@code SymbolsFilter} prepares an image with staff lines sections removed and
 * with all (good) inter instances erased, to allow the retrieval of symbols.
 *
 * @author Hervé Bitteur
 */
public class SymbolsFilter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SymbolsFilter.class);

    /** Orientation chosen for symbol runs. */
    public static final Orientation SYMBOL_ORIENTATION = Orientation.VERTICAL;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** Symbols lag. */
    private final Lag symLag;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SymbolsFilter object.
     *
     * @param sheet the related sheet
     */
    public SymbolsFilter (Sheet sheet)
    {
        this.sheet = sheet;

        // Create the spotLag
        symLag = new BasicLag(Lags.SYMBOL_LAG, SYMBOL_ORIENTATION);
        sheet.setLag(Lags.SYMBOL_LAG, symLag);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Start from the staff-free image, remove all good inters, and build the runs and
     * sections that compose symbols glyphs put in SYMBOL layer.
     * <p>
     * For not so good inters (they have already survived the first RESOLUTION step) we put them
     * aside as optional glyphs that can take part of the symbols glyphs clustering and thus
     * compete for valuable compounds.
     */
    public void process ()
    {
        logger.debug("SymbolsFilter running...");

        Picture picture = sheet.getPicture();
        ByteProcessor buf = picture.getSource(Picture.SourceKey.STAFF_LINE_FREE);
        BufferedImage img = buf.getBufferedImage();
        ByteProcessor buffer = new ByteProcessor(img);

        // Erase good shapes of each system
        Graphics2D g = img.createGraphics();
        SymbolsEraser eraser = new SymbolsEraser(buffer, g, sheet);
        Map<SystemInfo, List<Glyph>> optionalMap = eraser.eraseShapes(
                Arrays.asList(
                        Shape.THICK_BARLINE,
                        Shape.THIN_BARLINE,
                        Shape.THIN_CONNECTION,
                        Shape.THICK_CONNECTION,
                        Shape.STEM,
                        Shape.WHOLE_NOTE,
                        Shape.NOTEHEAD_BLACK,
                        Shape.NOTEHEAD_VOID,
                        Shape.BEAM,
                        Shape.BEAM_HOOK,
                        Shape.SLUR,
                        Shape.CRESCENDO,
                        Shape.DECRESCENDO,
                        Shape.ENDING,
                        Shape.LEDGER,
                        Shape.WHOLE_NOTE_SMALL,
                        Shape.NOTEHEAD_BLACK_SMALL,
                        Shape.NOTEHEAD_VOID_SMALL,
                        Shape.BEAM_SMALL,
                        Shape.BEAM_HOOK_SMALL));

        // Keep a copy on disk?
        if (constants.keepSymbolsBuffer.isSet()) {
            ImageUtil.saveOnDisk(img, sheet.getPage().getId() + ".sym");
        }

        // Dispatch optional glyphs
        if (optionalMap != null) {
            dispatchOptionals(optionalMap);

            // Display for visual check?
            if (constants.showSymbols.isSet() && (Main.getGui() != null)) {
                sheet.getAssembly().addViewTab(
                        "Symbols",
                        new ScrollImageView(sheet, new MyView(img, optionalMap)),
                        new BoardsPane(new PixelBoard(sheet)));
            }
        }

        buildSymbolsGlyphs(buffer);
    }

    //--------------------//
    // buildSymbolsGlyphs //
    //--------------------//
    /**
     * Build the symbols glyphs from the provided buffer.
     * <p>
     * Optional glyphs (corresponding to weak inters) are provided separately per system
     *
     * @param buffer image with symbols pixels
     */
    private void buildSymbolsGlyphs (ByteProcessor buffer)
    {
        // Runs
        RunsTable runTable = new RunsTableFactory(SYMBOL_ORIENTATION, buffer, 0).createTable(
                "symbols");

        // Sections
        SectionsBuilder sectionsBuilder = new SectionsBuilder(symLag, new JunctionRatioPolicy());
        List<Section> sections = sectionsBuilder.createSections(runTable, false);

        // Glyphs
        GlyphNest nest = sheet.getNest();
        List<Glyph> glyphs = nest.retrieveGlyphs(
                sections,
                GlyphLayer.SYMBOL,
                true,
                Glyph.Linking.NO_LINK);
        logger.debug("Symbol glyphs: {}", glyphs.size());

        // Dispatch each glyph to its relevant system(s)
        dispatchPageSymbols(glyphs);
    }

    //-------------------//
    // dispatchOptionals //
    //-------------------//
    private void dispatchOptionals (Map<SystemInfo, List<Glyph>> optionalMap)
    {
        for (Entry<SystemInfo, List<Glyph>> entry : optionalMap.entrySet()) {
            entry.getKey().setOptionalGlyphs(entry.getValue());
        }
    }

    //---------------------//
    // dispatchPageSymbols //
    //---------------------//
    /**
     * Dispatch page symbols according to their containing system(s).
     *
     * @param glyphs the glyphs to dispatch
     */
    private void dispatchPageSymbols (List<Glyph> glyphs)
    {
        List<SystemInfo> relevants = new ArrayList<SystemInfo>();
        SystemManager systemManager = sheet.getSystemManager();

        for (Glyph glyph : glyphs) {
            Point center = glyph.getCentroid();
            systemManager.getSystemsOf(center, relevants);

            for (SystemInfo system : relevants) {
                system.registerGlyph(glyph);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Boolean showSymbols = new Constant.Boolean(
                true,
                "Should we display the symbols buffer?");

        final Constant.Boolean keepSymbolsBuffer = new Constant.Boolean(
                true,
                "Should we store skeleton images on disk?");
    }

    //--------//
    // MyView //
    //--------//
    /**
     * View dedicated to symbols.
     */
    private class MyView
            extends ImageView
    {
        //~ Instance fields ------------------------------------------------------------------------

        // All optional glyphs. */
        private final Set<Glyph> optionals;

        //~ Constructors ---------------------------------------------------------------------------
        public MyView (BufferedImage image,
                       Map<SystemInfo, List<Glyph>> optionalMap)
        {
            super(image);

            optionals = new HashSet<Glyph>();

            for (List<Glyph> glyphs : optionalMap.values()) {
                optionals.addAll(glyphs);
            }
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected void renderItems (Graphics2D g)
        {
            final Rectangle clip = g.getClipBounds();

            // Good inters are erased and not taken into account for symbols
            // Weak ones are temporarily erased and used as optional glyphs for symbols
            for (Glyph glyph : optionals) {
                if (glyph.getBounds().intersects(clip)) {
                    for (Section section : glyph.getMembers()) {
                        section.render(g, false, Color.GREEN);
                    }
                }
            }

            // Global sheet renderers?
            for (ItemRenderer renderer : sheet.getItemRenderers()) {
                renderer.renderItems(g);
            }
        }
    }
}
