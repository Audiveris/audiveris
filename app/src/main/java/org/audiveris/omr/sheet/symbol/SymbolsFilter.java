//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l s F i l t e r                                   //
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
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.GlyphGroup;
import static org.audiveris.omr.glyph.GlyphGroup.SYMBOL;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.image.ImageUtil;
import static org.audiveris.omr.image.PixelSource.BACKGROUND;
import static org.audiveris.omr.image.PixelSource.FOREGROUND;
import org.audiveris.omr.image.Template;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.PageCleaner;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;
import org.audiveris.omr.sheet.ui.ImageView;
import org.audiveris.omr.sheet.ui.PixelBoard;
import org.audiveris.omr.sheet.ui.ScrollImageView;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.util.ByteUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.process.ByteProcessor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class <code>SymbolsFilter</code> prepares a sheet image to allow the retrieval of symbols.
 * <ul>
 * <li>Staff lines are removed,
 * <li>All good inter instances are erased,
 * <li>All weak instances are also erased but kept apart as optional pixels (so that they can later
 * be considered by the symbols builder).
 * </ul>
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

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SymbolsFilter object.
     *
     * @param sheet the related sheet
     */
    public SymbolsFilter (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------

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
        final RunTableFactory runFactory = new RunTableFactory(SYMBOL_ORIENTATION);
        final RunTable runTable = runFactory.createTable(buffer);

        // Glyphs
        final List<Glyph> glyphs = GlyphFactory.buildGlyphs(runTable, new Point(0, 0), SYMBOL);
        logger.debug("Symbol glyphs: {}", glyphs.size());

        // Dispatch each glyph to its relevant system(s)
        dispatchPageSymbols(glyphs);
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
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        final List<SystemInfo> relevants = new ArrayList<>();
        final SystemManager systemManager = sheet.getSystemManager();

        for (Glyph glyph : glyphs) {
            final Glyph registeredGlyph = glyphIndex.registerOriginal(glyph);
            registeredGlyph.addGroup(GlyphGroup.SYMBOL);

            final Point center = registeredGlyph.getCentroid();
            systemManager.getSystemsOf(center, relevants);
            relevants.forEach(system -> system.addFreeGlyph(registeredGlyph));
        }
    }

    //---------//
    // process //
    //---------//
    /**
     * Start from the staff-free image, remove all good inters, and from the remaining
     * pixels build the symbols glyphs put in SYMBOL group.
     * <p>
     * For not good inters (some "weak" inters have already survived the first REDUCTION step)
     * we put them aside as optional glyphs that can take part of the symbols glyphs clustering and
     * thus compete for valuable compounds.
     * <p>
     * We also keep 1-letter words, since they might be symbols.
     *
     * @param optionalsMap (output) all weak glyphs gathered per system
     */
    public void process (Map<SystemInfo, List<Glyph>> optionalsMap)
    {
        logger.debug("SymbolsFilter running...");

        final ByteProcessor rawBuf = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);
        final BufferedImage img = rawBuf.getBufferedImage();
        final ByteProcessor buffer = new ByteProcessor(img);

        // Prepare the ground for symbols retrieval, noting optional (weak) glyphs per system
        final Graphics2D g = img.createGraphics();
        final SymbolsCleaner eraser = new SymbolsCleaner(buffer, g, sheet);
        eraser.eraseInters(optionalsMap);
        buffer.threshold(127);

        // Keep a copy on disk?
        if (constants.saveSymbolsBuffer.isSet()) {
            ImageUtil.saveOnDisk(img, sheet.getId(), "symbols");
        }

        // Display for visual check?
        if (constants.displaySymbols.isSet() && (OMR.gui != null)) {
            sheet.getStub().getAssembly().addViewTab(
                    "Symbols",
                    new ScrollImageView(sheet, new SymbolsView(img, optionalsMap)),
                    new BoardsPane(new PixelBoard(sheet)));
        }

        buildSymbolsGlyphs(buffer);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean displaySymbols =
                new Constant.Boolean(false, "Should we display the symbols image?");

        private final Constant.Boolean saveSymbolsBuffer =
                new Constant.Boolean(false, "Should we save symbols image on disk?");

        private final Scale.Fraction staffVerticalMargin =
                new Scale.Fraction(0.5, "Margin erased above & below staff header area");

        private final Constant.Integer maxSymbolLength = new Constant.Integer(
                "letter count",
                3,
                "Maximum number of chars for a word to be checked as a symbol");

        private final Constant.Ratio minHeadContextualGrade =
                new Constant.Ratio(0.6, "Minimum contextual grade to hide a head");

        private final Constant.Ratio minStemContextualGrade =
                new Constant.Ratio(0.7, "Minimum contextual grade to hide a stem");
    }

    //----------------//
    // SymbolsCleaner //
    //----------------//
    /**
     * Class <code>SymbolsCleaner</code> erases Inter instances to prepare symbols retrieval.
     * <ul>
     * <li>All the "strong" Inter instances are simply erased.</li>
     * <li>The "weak" ones are also erased but saved apart as optional glyphs.
     * Doing so, the {@link SymbolsBuilder} will be able to try all combinations with, as well as
     * without, these optional weak glyphs.</li>
     * </ul>
     * NOTA for text items: A one-char word within a one-word sentence is very suspicious and might
     * well be a symbol. Hence, we consider it as a "weak" inter whatever its assigned grade.
     * <p>
     * TODO: The saving of weak inters is implemented only for glyph-based inters and for notes.
     * Should it be extended to line-based inters (endings, wedges) and area-based inters (barlines,
     * brackets, beams)?
     */
    private static class SymbolsCleaner
            extends PageCleaner
    {
        /**
         * Current system list of weak glyphs.
         * Null value when processing strong inters, non-null value when processing weak ones.
         */
        private List<Glyph> systemWeaks;

        /**
         * Creates a new <code>SymbolsEraser</code> object.
         *
         * @param buffer page buffer
         * @param g      graphics context on buffer
         * @param sheet  related sheet
         */
        SymbolsCleaner (ByteProcessor buffer,
                        Graphics2D g,
                        Sheet sheet)
        {
            super(buffer, g, sheet);
        }

        //---------//
        // canHide //
        //---------//
        /**
         * Check if we can safely hide the inter (which cannot be mistaken for a symbol).
         * TODO: Quick hack, to be better implemented.
         *
         * @param inter the inter to check
         * @return true if we can safely hide the inter
         */
        @Override
        protected boolean canHide (Inter inter)
        {
            final double ctxGrade = inter.getBestGrade();

            if (inter instanceof StemInter) {
                return ctxGrade >= constants.minStemContextualGrade.getValue();
            }

            if (inter instanceof HeadInter) {
                return ctxGrade >= constants.minHeadContextualGrade.getValue();
            }

            return super.canHide(inter);
        }

        //-------------//
        // eraseInters //
        //-------------//
        /**
         * Erase from image graphics all instances of provided shapes.
         * <p>
         * We check text items for '3' or '6' characters, and consider these characters as
         * potential tuplet symbols when rather close from a chord.
         *
         * @param weaksMap (output) populated with the erased weak glyph instances per system
         */
        public void eraseInters (Map<SystemInfo, List<Glyph>> weaksMap)
        {
            final int maxSymbolLength = constants.maxSymbolLength.getValue();

            for (SystemInfo system : sheet.getSystems()) {
                final SIGraph sig = system.getSig();

                // Erase tablature areas
                eraseTablatures(system, constants.staffVerticalMargin);

                // Erase header area on each staff of the system
                eraseStavesHeader(system, constants.staffVerticalMargin);

                // Partition inters into strongs and weaks
                final List<Inter> strongs = new ArrayList<>();
                final List<Inter> weaks = new ArrayList<>();
                systemWeaks = null;

                for (Inter inter : sig.vertexSet()) {
                    if (inter.isRemoved()) {
                        continue;
                    }

                    if (inter.isFrozen()) {
                        strongs.add(inter);
                        continue;
                    }

                    // Check short words
                    if (inter instanceof WordInter word) {
                        if (word.getValue().length() <= maxSymbolLength) {
                            continue;
                        }
                    }

                    // Members are handled via their ensemble
                    // Except for beams and words
                    if ((inter.getEnsemble() != null) //
                            && !(inter instanceof AbstractBeamInter)
                            && !(inter instanceof WordInter)) {
                        continue;
                    }

                    // Sentences are handled via their words
                    if (inter instanceof SentenceInter) {
                        continue;
                    }

                    if (canHide(inter)) {
                        strongs.add(inter);
                    } else {
                        weaks.add(inter);
                    }
                }

                // Simply erase the strongs
                strongs.forEach(inter -> inter.accept(this));

                // Save the weaks apart and erase them
                systemWeaks = new ArrayList<>();
                weaksMap.put(system, systemWeaks);
                weaks.forEach(inter -> inter.accept(this));

                systemWeaks = null;
            }
        }

        //-------------//
        // processArea //
        //-------------//
        @Override
        protected void processArea (Area area)
        {
            // Save the area corresponding glyph(s)?
            if (systemWeaks != null) {
                //                List<Glyph> glyphs = sheet.getGlyphIndex()
                //                        .retrieveGlyphs(
                //                                glyph.getMembers(),
                //                                GlyphLayer.SYMBOL,
                //                                true);
                //
                //                systemWeaks.addAll(glyphs);
            }

            // Erase the area
            super.processArea(area);
        }

        //--------------//
        // processGlyph //
        //--------------//
        @Override
        protected void processGlyph (Glyph glyph)
        {
            // Erase the glyph
            super.processGlyph(glyph);

            // Save the glyph?
            if (systemWeaks != null) {
                // The glyph may be made of several parts, so it's safer to restart from pixels
                List<Glyph> glyphs = GlyphFactory
                        .buildGlyphs(glyph.getRunTable(), glyph.getTopLeft(), GlyphGroup.SYMBOL);
                systemWeaks.addAll(glyphs);
            }
        }

        //------------//
        // savePixels //
        //------------//
        /**
         * Save the provided pixels as optional glyphs.
         *
         * @param box   the absolute bounding box of inter descriptor (perhaps larger than symbol)
         * @param fores foreground pixels with coordinates relative to descriptor bounding box
         */
        private void savePixels (Rectangle box,
                                 List<Point> fores)
        {
            final ByteProcessor buf = new ByteProcessor(box.width, box.height);
            ByteUtil.fill(buf, BACKGROUND);

            fores.forEach(p -> buf.set(p.x, p.y, FOREGROUND));

            // Runs
            final RunTableFactory factory = new RunTableFactory(SYMBOL_ORIENTATION);
            final RunTable runTable = factory.createTable(buf);

            // Glyphs
            final List<Glyph> glyphs =
                    GlyphFactory.buildGlyphs(runTable, new Point(0, 0), GlyphGroup.SYMBOL);

            systemWeaks.addAll(glyphs);
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (HeadInter head)
        {
            final Template tpl = head.getTemplate();
            final Rectangle tplBox = tpl.getBounds(head.getBounds());

            // Use underlying glyph (dilated only for strong inters)
            final List<Point> fores = tpl.getForegroundPixels(tplBox, buffer, systemWeaks == null);

            // Erase foreground pixels
            fores.forEach(p -> g.fillRect(tplBox.x + p.x, tplBox.y + p.y, 1, 1));

            // Save foreground pixels for optional (weak) glyphs
            if (systemWeaks != null) {
                savePixels(tplBox, fores);
            }
        }
    }

    //-------------//
    // SymbolsView //
    //-------------//
    /**
     * View dedicated to symbols.
     */
    private class SymbolsView
            extends ImageView
    {
        /** All optional glyphs. */
        private final Set<Glyph> optionals = new LinkedHashSet<>();

        SymbolsView (BufferedImage image,
                     Map<SystemInfo, List<Glyph>> optionalMap)
        {
            super(image);

            optionalMap.values().forEach(glyphs -> optionals.addAll(glyphs));
        }

        @Override
        protected void renderItems (Graphics2D g)
        {
            final Rectangle clip = g.getClipBounds();

            // Good inters are erased and not taken into account for symbols
            // Weak ones are temporarily erased and used as optional glyphs for symbols
            final Color oldColor = g.getColor();
            g.setColor(Color.GREEN);

            for (Glyph glyph : optionals) {
                final Rectangle box = glyph.getBounds();

                if (box.intersects(clip)) {
                    glyph.getRunTable().render(g, box.getLocation());
                }
            }

            g.setColor(oldColor);

            // Global sheet renderers
            sheet.renderItems(g);
        }
    }
}
