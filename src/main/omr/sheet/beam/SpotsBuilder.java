//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S p o t s B u i l d e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.beam;

import omr.OMR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphFactory;
import omr.glyph.GlyphIndex;
import omr.glyph.Symbol.Group;

import omr.image.ImageUtil;
import omr.image.MorphoProcessor;
import omr.image.StructureElement;

import omr.run.Orientation;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.SystemManager;
import omr.sheet.ui.ImageView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.ScrollImageView;
import omr.sheet.ui.SheetTab;

import omr.ui.BoardsPane;

import omr.util.Navigable;
import omr.util.StopWatch;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code SpotsBuilder} performs morphology analysis to retrieve the major spots
 * that compose beams.
 * <p>
 * It can work on a whole sheet or on a snapshot of cues aggregate.
 *
 * @author Hervé Bitteur
 */
public class SpotsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SpotsBuilder.class);

    /** Orientation chosen for spot runs. */
    public static final Orientation SPOT_ORIENTATION = Orientation.VERTICAL;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SpotsBuilder object.
     *
     * @param sheet the related sheet
     */
    public SpotsBuilder (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // buildSheetSpots //
    //-----------------//
    /**
     * Retrieve all spots from a sheet.
     * All spots are dispatched among their containing system(s).
     */
    public void buildSheetSpots ()
    {
        final StopWatch watch = new StopWatch("buildSheetSpots");

        try {
            watch.start("gaussianBuffer");

            // We need a copy of image that we can overwrite.
            ByteProcessor buffer = getBuffer();

            // Retrieve major spots
            watch.start("buildSpots");

            int beam = sheet.getScale().getBeamThicknessMain();
            List<Glyph> spots = buildSpots(buffer, null, beam, null);

            // Dispatch spots per system(s)
            watch.start("dispatchSheetSpots");
            dispatchSheetSpots(spots);

            // Display on all spot glyphs?
            if ((OMR.gui != null) && constants.displayBeamSpots.isSet()) {
                watch.start("spotsController");

                SpotsController spotController = new SpotsController(sheet, spots);
                spotController.refresh();
            }
        } catch (Exception ex) {
            logger.warn("Error building spots", ex);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //------------//
    // buildSpots //
    //------------//
    /**
     * Build spots out of the provided buffer.
     *
     * @param buffer provided buffer (it will be modified)
     * @param offset buffer offset WRT sheet coordinates, or null
     * @param beam   typical beam height
     * @param cueId  cue id for cue buffer, null for whole sheet buffer
     * @return the collection of spots retrieved
     */
    public List<Glyph> buildSpots (ByteProcessor buffer,
                                   Point offset,
                                   double beam,
                                   String cueId)
    {
        final StopWatch watch = new StopWatch("buildSpots");

        // Erase Header for non-cue buffers
        if (cueId == null) {
            eraseHeaderAreas(buffer);
        }

        final double diameter = beam * constants.beamCircleDiameterRatio.getValue();
        final float radius = (float) (diameter - 1) / 2;
        logger.debug(
                "Spots retrieval beam: {}, diameter: {} ...",
                String.format("%.1f", beam),
                String.format("%.1f", diameter));

        final int[] seOffset = {0, 0};
        StructureElement se = new StructureElement(0, 1, radius, seOffset);
        watch.start("close");
        new MorphoProcessor(se).close(buffer);

        // For visual check
        if (cueId == null) {
            BufferedImage img = null;

            // Store buffer on disk?
            if (constants.keepBeamSpots.isSet()) {
                img = buffer.getBufferedImage();
                ImageUtil.saveOnDisk(img, sheet.getId() + ".spot");
            }

            // Display the gray-level view of all spots
            if ((OMR.gui != null) && constants.displayGraySpots.isSet()) {
                if (img == null) {
                    img = buffer.getBufferedImage();
                }

                sheet.getStub().getAssembly().addViewTab(
                        SheetTab.GRAY_SPOT_TAB,
                        new ScrollImageView(sheet, new ImageView(img)),
                        new BoardsPane(new PixelBoard(sheet)));
            }

            // Save a specific binarized version for HEADS step
            saveHeadRuns((ByteProcessor) buffer.duplicate());
        } else if (constants.keepCueSpots.isSet()) {
            BufferedImage img = buffer.getBufferedImage();
            ImageUtil.saveOnDisk(img, sheet.getId() + "." + cueId + ".spot");
        }

        // Binarize the spots via a global filter (no illumination problem)
        watch.start("binarize");
        buffer.threshold(constants.beamBinarizationThreshold.getValue());

        // Runs
        watch.start("createTable");

        RunTableFactory runFactory = new RunTableFactory(SPOT_ORIENTATION);
        RunTable spotTable = runFactory.createTable(buffer);

        // Glyphs
        watch.start("buildGlyphs");

        List<Glyph> glyphs = GlyphFactory.buildGlyphs(spotTable, offset);

        if (constants.printWatch.isSet()) {
            watch.print();
        }

        return glyphs;
    }

    //--------------------//
    // dispatchSheetSpots //
    //--------------------//
    /**
     * Dispatch sheet spots according to their containing system(s),
     * and keeping only those within system width.
     *
     * @param spots the spots to dispatch
     */
    private void dispatchSheetSpots (List<Glyph> spots)
    {
        int count = 0;

        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        final List<SystemInfo> relevants = new ArrayList<SystemInfo>();
        final SystemManager systemManager = sheet.getSystemManager();

        for (Glyph glyph : spots) {
            Point center = glyph.getCentroid();
            systemManager.getSystemsOf(center, relevants);

            boolean created = false;

            for (SystemInfo system : relevants) {
                // Check glyph is within system abscissa boundaries
                if ((center.x >= system.getLeft()) && (center.x <= system.getRight())) {
                    glyph = glyphIndex.registerOriginal(glyph);
                    glyph.addGroup(Group.BEAM_SPOT);
                    system.addFreeGlyph(glyph);
                    created = true;
                }
            }

            if (created) {
                count++;
            }
        }

        logger.debug("Spots retrieved: {}", count);
    }

    //------------------//
    // eraseHeaderAreas //
    //------------------//
    private void eraseHeaderAreas (ByteProcessor buffer)
    {
        final int dmzDyMargin = sheet.getScale().toPixels(constants.staffVerticalMargin);

        buffer.setValue(255);

        for (SystemInfo system : sheet.getSystems()) {
            Staff firstStaff = system.getFirstStaff();
            Staff lastStaff = system.getLastStaff();
            int start = system.getBounds().x;
            int stop = firstStaff.getHeaderStop();
            int top = firstStaff.getFirstLine().yAt(stop) - dmzDyMargin;
            int bot = lastStaff.getLastLine().yAt(stop) + dmzDyMargin;

            buffer.setRoi(start, top, stop - start + 1, bot - top + 1);
            buffer.fill();
            buffer.resetRoi();
        }

        buffer.setValue(0);
    }

    //-----------//
    // getBuffer //
    //-----------//
    /**
     * Prepare the buffer to be used for beams retrieval.
     * <p>
     * Staff lines and vertical lines (especially stems) are removed because they could lead to
     * artificially larger beam candidates.
     *
     * @return the buffer to be used
     */
    private ByteProcessor getBuffer ()
    {
        StopWatch watch = new StopWatch("SpotsBuilder.getBuffer");

        try {
            final Picture picture = sheet.getPicture();
            final int stemWidth = sheet.getScale().getMaxStem();

            ///return  picture.getSource(Picture.SourceKey.GAUSSIAN);
            watch.start("getSource NO_STAFF");

            ByteProcessor buffer = picture.getSource(Picture.SourceKey.NO_STAFF);

            // Remove stem runs (could be much more efficient if performed on buffer directly)
            watch.start("createtable");

            RunTableFactory factory = new RunTableFactory(
                    Orientation.HORIZONTAL,
                    new RunTableFactory.LengthFilter(stemWidth));
            RunTable table = factory.createTable(buffer);
            watch.start("table to buffer");
            buffer = table.getBuffer();

            // Apply median filter
            watch.start("median");
            buffer = picture.medianFiltered(buffer);

            // Apply gaussian filter
            watch.start("gaussian");

            return picture.gaussianFiltered(buffer);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //--------------//
    // saveHeadRuns //
    //--------------//
    /**
     * To ease (future) HEADS step, save the runs of the properly binarized buffer.
     *
     * @param buffer the buffer copy to binarize
     */
    private void saveHeadRuns (ByteProcessor buffer)
    {
        // Binarize the spots with threshold for heads
        buffer.threshold(constants.noteBinarizationThreshold.getValue());

        // Runs
        RunTableFactory runFactory = new RunTableFactory(SPOT_ORIENTATION);
        RunTable runs = runFactory.createTable(buffer);

        // For visual check
        if (constants.keepHeadSpots.isSet()) {
            BufferedImage img = runs.getBufferedImage();
            ImageUtil.saveOnDisk(img, sheet.getId() + ".headspot");
        }

        // Save it for future HEADS step
        sheet.getPicture().setTable(Picture.TableKey.HEAD_SPOTS, runs);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean displayBeamSpots = new Constant.Boolean(
                false,
                "Should we display the beam Spots view?");

        private final Constant.Boolean displayGraySpots = new Constant.Boolean(
                false,
                "Should we display the gray Spots view?");

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Boolean keepBeamSpots = new Constant.Boolean(
                false,
                "Should we store sheet beam spot images on disk?");

        private final Constant.Boolean keepHeadSpots = new Constant.Boolean(
                false,
                "Should we store sheet head spot images on disk?");

        private final Constant.Boolean keepCueSpots = new Constant.Boolean(
                false,
                "Should we store cue spot images on disk?");

        private final Constant.Ratio beamCircleDiameterRatio = new Constant.Ratio(
                0.8,
                "Diameter of circle used to close beam spots, as ratio of beam height");

        private final Constant.Integer beamBinarizationThreshold = new Constant.Integer(
                "pixel",
                140,
                "Global binarization threshold for beams");

        private final Constant.Integer noteBinarizationThreshold = new Constant.Integer(
                "pixel",
                170,
                "Global binarization threshold for notes");

        private final Scale.Fraction staffVerticalMargin = new Scale.Fraction(
                2.0,
                "Margin erased above & below staff header area");
    }
}
