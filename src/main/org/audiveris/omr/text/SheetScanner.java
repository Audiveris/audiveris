//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S h e e t S c a n n e r                                    //
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
package org.audiveris.omr.text;

import ij.process.ByteProcessor;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.image.ImageUtil;
import org.audiveris.omr.image.ShapeDescriptor;
import org.audiveris.omr.image.Template;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.PageCleaner;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.ui.ImageView;
import org.audiveris.omr.sheet.ui.PixelBoard;
import org.audiveris.omr.sheet.ui.ScrollImageView;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.param.Param;

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
 * Tesseract is used in MULTI_BLOCK layout mode, meaning that the sheet main contain several blocks
 * of text.
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

            final BufferedImage image = getCleanImage(); // This also sets buffer member

            // Perform OCR on whole image
            watch.start("OCR recognize");

            final Param<String> textParam = sheet.getStub().getOcrLanguages();
            final String language = textParam.getValue();
            logger.debug("scanSheet lan:{} on {}", language, sheet);

            return OcrUtil.scan(
                    image,
                    constants.whiteMarginAdded.getValue(),
                    OCR.LayoutMode.MULTI_BLOCK,
                    language,
                    sheet.getScale().getInterline(),
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

        private final Constant.Integer whiteMarginAdded = new Constant.Integer(
                "pixels",
                10,
                "Margin of white pixels added around sheet image");
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
                    if (!inter.isRemoved()) {
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
            RunTable table = new RunTableFactory(Orientation.VERTICAL).createTable(buffer);
            List<Glyph> glyphs = GlyphFactory.buildGlyphs(table, null);
            eraseBorderGlyphs(glyphs, cores);
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (HeadInter inter)
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
