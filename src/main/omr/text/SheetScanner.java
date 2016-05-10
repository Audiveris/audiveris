//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S h e e t S c a n n e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.OMR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphFactory;

import omr.image.ImageUtil;
import omr.image.ShapeDescriptor;
import omr.image.Template;

import omr.run.Orientation;
import static omr.run.Orientation.VERTICAL;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.sheet.PageCleaner;
import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.StaffManager;
import omr.sheet.SystemInfo;
import omr.sheet.ui.ImageView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.ScrollImageView;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.Inter;
import omr.sig.inter.LedgerInter;

import omr.ui.BoardsPane;

import omr.util.LiveParam;
import omr.util.StopWatch;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code SheetScanner} runs OCR on the whole sheet, where good inters and
 * staves core areas have been blanked.
 * <p>
 * The raw OCR output will later be processed at system level by dedicated TextBuilder instances.
 *
 * @author Hervé Bitteur
 */
public class SheetScanner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SheetScanner.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** Buffer used by OCR. */
    private ByteProcessor buffer;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TextPageScanner} object.
     *
     * @param sheet the sheet to process
     */
    public SheetScanner (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // getBuffer //
    //-----------//
    /**
     * @return the buffer
     */
    public ByteProcessor getBuffer ()
    {
        return buffer;
    }

    //-----------//
    // scanSheet //
    //-----------//
    /**
     * Get a clean image of whole sheet and run OCR on it.
     *
     * @return the list of OCR'ed lines found
     */
    public List<TextLine> scanSheet ()
    {
        StopWatch watch = new StopWatch("scanSheet");

        try {
            // Get clean page image
            watch.start("getCleanImage");

            BufferedImage image = getCleanImage(); // This also sets buffer member

            // Proper text params
            final LiveParam<String> textParam = sheet.getStub().getLanguageParam();
            final String language = textParam.getTarget();
            logger.debug("scanSheet lan:{} on {}", language, sheet);
            textParam.setActual(language);

            // Perform OCR on whole image
            watch.start("OCR recognize");

            return TextBuilder.getOcr().recognize(
                    sheet.getScale().getInterline(),
                    image,
                    null,
                    language,
                    OCR.LayoutMode.MULTI_BLOCK,
                    sheet.getId());
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //---------------//
    // getCleanImage //
    //---------------//
    private BufferedImage getCleanImage ()
    {
        Picture picture = sheet.getPicture();
        ByteProcessor buf = picture.getSource(Picture.SourceKey.NO_STAFF);

        BufferedImage img = buf.getBufferedImage();
        buffer = new ByteProcessor(img);

        TextsCleaner cleaner = new TextsCleaner(buffer, img.createGraphics(), sheet);
        cleaner.eraseInters();

        // Display for visual check?
        if (constants.displayTexts.isSet() && (OMR.gui != null)) {
            sheet.getStub().getAssembly().addViewTab(
                    "Texts",
                    new ScrollImageView(
                            sheet,
                            new ImageView(img)
                    {
                        @Override
                        protected void renderItems (Graphics2D g)
                        {
                            sheet.renderItems(g); // Apply registered sheet renderers
                        }
                    }),
                    new BoardsPane(new PixelBoard(sheet)));
        }

        // Keep a copy on disk?
        if (constants.keepTextsBuffer.isSet()) {
            ImageUtil.saveOnDisk(img, sheet.getId() + ".text");
        }

        return img;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Boolean displayTexts = new Constant.Boolean(
                false,
                "Should we display the texts image?");

        private final Constant.Boolean keepTextsBuffer = new Constant.Boolean(
                false,
                "Should we store texts buffer on disk?");

        private final Scale.Fraction staffHorizontalMargin = new Scale.Fraction(
                0.25,
                "Horizontal margin around staff core area");

        private final Scale.Fraction staffVerticalMargin = new Scale.Fraction(
                0.25,
                "Vertical margin around staff core area");
    }

    //--------------//
    // TextsCleaner //
    //--------------//
    /**
     * Class {@code TextsCleaner} erases shapes to prepare texts retrieval.
     */
    private static class TextsCleaner
            extends PageCleaner
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Scale-dependent parameters. */
        private final Parameters params;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new {@code TextsCleaner} object.
         *
         * @param buffer page buffer
         * @param g      graphics context on buffer
         * @param sheet  related sheet
         */
        public TextsCleaner (ByteProcessor buffer,
                             Graphics2D g,
                             Sheet sheet)
        {
            super(buffer, g, sheet);
            params = new Parameters(sheet.getScale());
        }

        //~ Methods --------------------------------------------------------------------------------
        //-------------//
        // eraseInters //
        //-------------//
        /**
         * Erase from image graphics all instances of good inter instances.
         */
        public void eraseInters ()
        {
            List<Area> cores = new ArrayList<Area>();

            for (SystemInfo system : sheet.getSystems()) {
                final SIGraph sig = system.getSig();
                final List<Inter> erased = new ArrayList<Inter>();

                for (Inter inter : sig.vertexSet()) {
                    if (!inter.isDeleted()) {
                        if (canHide(inter)) {
                            erased.add(inter);
                        }
                    }
                }

                // Erase the inters
                for (Inter inter : erased) {
                    inter.accept(this);
                }

                // Erase the core area of each staff
                for (Staff staff : system.getStaves()) {
                    Area core = StaffManager.getCoreArea(staff, params.hMargin, params.vMargin);
                    cores.add(core);
                    ///staff.addAttachment("core", core); // Just for visual check
                    g.fill(core);
                }
            }

            // Binarize
            buffer.threshold(127);

            // Build all glyphs out of buffer and erase those that intersect a staff core area
            RunTable table = new RunTableFactory(VERTICAL).createTable(buffer);
            List<Glyph> glyphs = GlyphFactory.buildGlyphs(table, null);
            eraseBorderGlyphs(glyphs, cores);
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

            // Use underlying glyph (enlarged)
            final List<Point> fores = tpl.getForegroundPixels(box, buffer, true);

            // Erase foreground pixels
            for (final Point p : fores) {
                g.fillRect(box.x + p.x, box.y + p.y, 1, 1);
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (LedgerInter ledger)
        {
            super.visit(ledger); // Defaultcleaning by erasing underlying glyph

            // Thicken the ledgerline 1 pixel above & 1 pixel below
            final Stroke oldStroke = g.getStroke();
            final Glyph glyph = ledger.getGlyph();
            float thickness = (float) ledger.getGlyph()
                    .getMeanThickness(Orientation.HORIZONTAL);
            thickness += 2;
            g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            glyph.renderLine(g);
            g.setStroke(oldStroke);
        }

        //-------------------//
        // eraseBorderGlyphs //
        //-------------------//
        /**
         * Erase from text image the glyphs that intersect or touch a staff core area.
         * <p>
         * (We also tried to remove too small glyphs, but this led to poor recognition by OCR)
         *
         * @param glyphs all the glyph instances in image
         * @param cores  all staves cores
         */
        private void eraseBorderGlyphs (List<Glyph> glyphs,
                                        List<Area> cores)
        {
            for (Glyph glyph : glyphs) {
                // Check position WRT staves cores
                Rectangle glyphBox = glyph.getBounds();
                glyphBox.grow(1, 1); // To catch touching glyphs (on top of intersecting ones)

                for (Area core : cores) {
                    if (core.intersects(glyphBox)) {
                        glyph.getRunTable().render(g, glyph.getTopLeft());

                        break;
                    }
                }
            }
        }

        //~ Inner Classes --------------------------------------------------------------------------
        //------------//
        // Parameters //
        //------------//
        private class Parameters
        {
            //~ Instance fields --------------------------------------------------------------------

            final int hMargin;

            final int vMargin;

            //~ Constructors -----------------------------------------------------------------------
            public Parameters (Scale scale)
            {
                hMargin = scale.toPixels(constants.staffHorizontalMargin);
                vMargin = scale.toPixels(constants.staffVerticalMargin);
            }
        }
    }
}
