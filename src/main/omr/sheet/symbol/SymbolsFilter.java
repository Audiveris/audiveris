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
package omr.sheet.symbol;

import omr.OMR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.facets.Glyph;

import omr.image.ImageUtil;
import omr.image.ShapeDescriptor;
import omr.image.Template;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;
import omr.lag.SectionFactory;

import omr.run.Orientation;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.sheet.PageCleaner;
import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.SystemManager;
import omr.sheet.ui.ImageView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.ScrollImageView;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.Inter;
import omr.sig.inter.SentenceInter;
import omr.sig.inter.StemInter;

import omr.ui.BoardsPane;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
    private Lag symLag;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SymbolsFilter object.
     *
     * @param sheet the related sheet
     */
    public SymbolsFilter (Sheet sheet)
    {
        this.sheet = sheet;

        // Needs the symLag
        symLag = sheet.getLagManager().getLag(Lags.SYMBOL_LAG);

        if (symLag == null) {
            symLag = new BasicLag(Lags.SYMBOL_LAG, SYMBOL_ORIENTATION);
            sheet.getLagManager().setLag(Lags.SYMBOL_LAG, symLag);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Start from the staff-free image, remove all good inters, and from the remaining
     * pixels build the runs and sections that compose symbols glyphs put in SYMBOL layer.
     * <p>
     * For not good inters (such 'weak' inters have already survived the first REDUCTION step)
     * we put them aside as optional glyphs that can take part of the symbols glyphs clustering and
     * thus compete for valuable compounds.
     *
     * @param optionalsMap (output) all weak glyphs per system
     */
    public void process (Map<SystemInfo, List<Glyph>> optionalsMap)
    {
        logger.debug("SymbolsFilter running...");

        Picture picture = sheet.getPicture();
        ByteProcessor rawBuf = picture.getSource(Picture.SourceKey.NO_STAFF);
        BufferedImage img = rawBuf.getBufferedImage();
        ByteProcessor buffer = new ByteProcessor(img);

        // Prepare the ground for symbols retrieval, noting optional (weak) glyphs per system
        Graphics2D g = img.createGraphics();
        SymbolsCleaner eraser = new SymbolsCleaner(buffer, g, sheet);
        eraser.eraseInters(optionalsMap);
        buffer.threshold(127);

        // Keep a copy on disk?
        if (constants.keepSymbolsBuffer.isSet()) {
            ImageUtil.saveOnDisk(img, sheet.getId() + ".sym");
        }

        // Display for visual check?
        if (constants.displaySymbols.isSet() && (OMR.getGui() != null)) {
            sheet.getAssembly().addViewTab(
                    "Symbols",
                    new ScrollImageView(sheet, new MyView(img, optionalsMap)),
                    new BoardsPane(new PixelBoard(sheet)));
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
        RunTableFactory runFactory = new RunTableFactory(SYMBOL_ORIENTATION);
        RunTable runTable = runFactory.createTable("symbols", buffer);

        // Sections
        SectionFactory sectionsBuilder = new SectionFactory(symLag, new JunctionRatioPolicy());
        List<Section> sections = sectionsBuilder.createSections(runTable);

        // Glyphs
        GlyphNest nest = sheet.getGlyphNest();
        List<Glyph> glyphs = nest.retrieveGlyphs(sections, GlyphLayer.SYMBOL, true);
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

        private final Constant.Boolean displaySymbols = new Constant.Boolean(
                false,
                "Should we display the symbols image?");

        private final Constant.Boolean keepSymbolsBuffer = new Constant.Boolean(
                false,
                "Should we store symbols image on disk?");

        private final Scale.Fraction staffVerticalMargin = new Scale.Fraction(
                0.5,
                "Margin erased above & below staff header area");
    }

    //----------------//
    // SymbolsCleaner //
    //----------------//
    /**
     * Class {@code SymbolsCleaner} erases Inter instances to prepare symbols retrieval.
     * <ul>
     * <li>All the "strong" Inter instances are simply erased.</li>
     * <li>The "weak" ones are also erased but saved apart as optional glyphs.
     * Doing so, the {@link SymbolsBuilder} will be able to try all combinations with, as well as
     * without, these optional weak glyphs.</li>
     * </ul>
     * Nota for text items: A one-char word within a one-word sentence is very suspicious and might
     * well be a symbol. Hence, we consider it as a "weak" inter whatever its assigned grade.
     * <p>
     * TODO: The saving of weak inters is implemented only for glyph-based inters and for notes.
     * Should it be extended to line-based inters (endings, wedges) and area-based inters (barline,
     * brackets, beams)?
     */
    private static class SymbolsCleaner
            extends PageCleaner
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Lag used by optional sections. */
        private final Lag symLag;

        /** Map system -> list of weak glyphs. */
        private final Map<SystemInfo, List<Glyph>> weaksMap = new TreeMap<SystemInfo, List<Glyph>>();

        /**
         * Current system list of weak glyphs.
         * Null value when processing strong inters, non-null value when processing weak ones.
         */
        private List<Glyph> systemWeaks;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new {@code SymbolsEraser} object.
         *
         * @param buffer page buffer
         * @param g      graphics context on buffer
         * @param sheet  related sheet
         */
        public SymbolsCleaner (ByteProcessor buffer,
                               Graphics2D g,
                               Sheet sheet)
        {
            super(buffer, g, sheet);

            symLag = sheet.getLagManager().getLag(Lags.SYMBOL_LAG);
        }

        //~ Methods --------------------------------------------------------------------------------
        //-------------//
        // eraseInters //
        //-------------//
        /**
         * Erase from image graphics all instances of provided shapes.
         *
         * @param weaksMap (output) populated with the erased weak glyph instances per system
         */
        public void eraseInters (Map<SystemInfo, List<Glyph>> weaksMap)
        {
            for (SystemInfo system : sheet.getSystems()) {
                final SIGraph sig = system.getSig();

                // Erase header area on each staff of the system
                eraseStavesHeader(system, constants.staffVerticalMargin);

                final List<Inter> strongs = new ArrayList<Inter>();
                final List<Inter> weaks = new ArrayList<Inter>();
                systemWeaks = null;

                for (Inter inter : sig.vertexSet()) {
                    if (inter.isDeleted()) {
                        continue;
                    }

                    // Members are handled via their ensemble
                    if (inter.getEnsemble() != null) {
                        continue;
                    }

                    // Special case for one-char sentences: they are not erased
                    // since they might be isolated one-letter symbols
                    if (SentenceInter.class.isInstance(inter)) {
                        SentenceInter sentence = (SentenceInter) inter;

                        if (sentence.getValue().length() == 1) {
                            continue;
                        }

                    }

                    if (canHide(inter)) {
                        strongs.add(inter);
                    } else {
                        weaks.add(inter);
                    }
                }

                // Simply erase the strongs
                for (Inter inter : strongs) {
                    inter.accept(this);
                }

                // Erase but save the weaks apart
                systemWeaks = new ArrayList<Glyph>();
                weaksMap.put(system, systemWeaks);

                for (Inter inter : weaks) {
                    inter.accept(this);
                }

                systemWeaks = null;
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (AbstractHeadInter inter)
        {
            final ShapeDescriptor desc = inter.getDescriptor();
            final Template tpl = desc.getTemplate();
            final Rectangle box = desc.getBounds(inter.getBounds());

            // Use underlying glyph (enlarged only for strong inters)
            final List<Point> fores = tpl.getForegroundPixels(box, buffer, systemWeaks == null);

            // Erase foreground pixels
            for (final Point p : fores) {
                g.fillRect(box.x + p.x, box.y + p.y, 1, 1);
            }

            // Save foreground pixels for optional (weak) glyphs
            if (systemWeaks != null) {
                savePixels(box, fores);
            }
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
            double ctxGrade = inter.getBestGrade();

            if (inter instanceof StemInter) {
                return ctxGrade >= 0.7; // TODO
            }

            if (inter instanceof AbstractHeadInter) {
                return ctxGrade >= 0.6; // TODO
            }

            return super.canHide(inter);
        }

        //-------------//
        // processArea //
        //-------------//
        @Override
        protected void processArea (Area area)
        {
            // Save the area corresponding glyph(s)?
            if (systemWeaks != null) {
                //                List<Glyph> glyphs = sheet.getGlyphNest()
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
                // The glyph may be made of several parts, so it's safer to restart from sections
                List<Glyph> glyphs = sheet.getGlyphNest().retrieveGlyphs(
                        glyph.getMembers(),
                        GlyphLayer.SYMBOL,
                        true);

                systemWeaks.addAll(glyphs);
            }
        }

        //------------//
        // savePixels //
        //------------//
        /**
         * Save the provided pixels as optional glyphs.
         *
         * @param box   the absolute bounding box of inter descriptor (perhaps larger than the
         *              symbol)
         * @param fores foreground pixels with coordinates relative to descriptor bounding box
         */
        private void savePixels (Rectangle box,
                                 List<Point> fores)
        {
            ByteProcessor buf = new ByteProcessor(box.width, box.height);
            buf.invert();

            for (Point p : fores) {
                buf.set(p.x, p.y, 0);
            }

            // Runs
            RunTableFactory factory = new RunTableFactory(SYMBOL_ORIENTATION);
            RunTable runTable = factory.createTable("optionals", buf);

            // Sections
            SectionFactory sectionsBuilder = new SectionFactory(symLag, new JunctionRatioPolicy());
            List<Section> sections = sectionsBuilder.createSections(runTable);

            // Translate sections to absolute coordinates
            for (Section section : sections) {
                section.translate(box.getLocation());
            }

            // Glyphs
            List<Glyph> glyphs = sheet.getGlyphNest()
                    .retrieveGlyphs(sections, GlyphLayer.SYMBOL, true);

            for (Glyph glyph : glyphs) {
                systemWeaks.add(glyph);
            }
        }
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

            // Global sheet renderers
            sheet.renderItems(g);
        }
    }
}
