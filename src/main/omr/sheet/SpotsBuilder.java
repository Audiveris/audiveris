//----------------------------------------------------------------------------//
//                                                                            //
//                            S p o t s B u i l d e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;
import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphsBuilder;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.image.ImageView;
import omr.image.MorphoProcessor;
import omr.image.Picture;
import omr.image.PixelBuffer;
import omr.image.PixelFilter;
import omr.image.StructureElement;
import omr.image.VerticalFilter;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.SectionsBuilder;

import omr.run.Orientation;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.sheet.ui.PixelBoard;

import omr.ui.BoardsPane;

import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Class {@code SpotsBuilder} performs morphology analysis on original
 * image to retrieve major spots that compose black note heads and
 * beams.
 *
 * @author Hervé Bitteur
 */
public class SpotsBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SpotsBuilder.class);

    /** Orientation chosen for spot runs. */
    public static final Orientation SPOT_ORIENTATION = Orientation.VERTICAL;

    //~ Instance fields --------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** Spots runs. */
    private RunsTable spotTable;

    /** Spots lag. */
    private Lag spotLag;

    /** To measure elapsed time. */
    private final StopWatch watch = new StopWatch("SpotsBuilder");

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // SpotsBuilder //
    //--------------//
    /**
     * Creates a new SpotsBuilder object.
     *
     * @param sheet the related sheet
     */
    public SpotsBuilder (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // buildSpots //
    //------------//
    public void buildSpots ()
    {
        try {
            // Retrieve major spots
            spotTable = retrieveSpots();

            // Allocate sections & glyphs for spots
            buildSpotGlyphs();
        } catch (Exception ex) {
            logger.warn("Error building spots", ex);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //-----------------//
    // buildSpotGlyphs //
    //-----------------//
    private void buildSpotGlyphs ()
    {
        logger.debug("Run sequences: {}", spotTable.getSize());

        // Build the spotLag out of spots runs
        spotLag = new BasicLag(Lags.SPOT_LAG, SPOT_ORIENTATION);

        SectionsBuilder sectionsBuilder = new SectionsBuilder(
                spotLag,
                new JunctionRatioPolicy());
        sectionsBuilder.createSections(spotTable, false);
        logger.debug("Sections: {}", spotLag.getSections().size());

        List<Glyph> glyphs = GlyphsBuilder.retrieveGlyphs(
                spotLag.getSections(),
                sheet.getNest(),
                GlyphLayer.SPOT,
                sheet.getScale());

        sheet.setLag(Lags.SPOT_LAG, spotLag);

        Lag headLag = new BasicLag(Lags.HEAD_LAG, SPOT_ORIENTATION);
        sheet.setLag(Lags.HEAD_LAG, headLag);

        Lag splitLag = new BasicLag(Lags.SPLIT_LAG, SPOT_ORIENTATION);
        sheet.setLag(Lags.SPLIT_LAG, splitLag);

        // Dispatch spots per system, keeping only those within system width
        dispatchSpots(glyphs);

        // Display on all spot glyphs
        SpotsController spotController = new SpotsController(
                sheet,
                spotLag,
                headLag,
                splitLag);
        sheet.setSpotsController(spotController);
        spotController.refresh();
    }

    //---------------//
    // dispatchSpots //
    //---------------//
    private void dispatchSpots (List<Glyph> glyphs)
    {
        int count = 0;

        for (Glyph glyph : glyphs) {
            SystemInfo system = sheet.getSystemOf(glyph);

            if (system != null) {
                // Check glyph is within system abscissa boundaries
                Point center = glyph.getAreaCenter();

                if ((center.x >= system.getLeft())
                    && (center.x <= system.getRight())) {
                    glyph.setShape(Shape.BEAM_SPOT);
                    system.addGlyphAndMembers(glyph);
                    count++;
                }
            }
        }

        logger.info("{}Spots retrieved: {}", sheet.getLogPrefix(), count);
    }

    //---------------//
    // retrieveSpots //
    //---------------//
    private RunsTable retrieveSpots ()
    {
        PixelBuffer buffer = new PixelBuffer(
                sheet.getPicture().getImage(Picture.Key.GAUSSIAN));

        watch.start("spots");

        int[] offset = {0, 0};
        Scale scale = sheet.getScale();
        int beam = scale.getMainBeam();

        final double diameter = beam * constants.beamCircleDiameterRatio.getValue();
        final float radius = (float) (diameter - 1) / 2;
        logger.info(
                "{}Spots retrieval beam: {}, radius: {} ...",
                sheet.getLogPrefix(),
                beam,
                radius);

        StructureElement se = new StructureElement(0, 1, radius, offset);
        MorphoProcessor mp = new MorphoProcessor(se);

        mp.close(buffer);

        BufferedImage fromClosedBuffer = buffer.toBufferedImage();

        // Store buffer on disk for further manual analysis if any
        try {
            ImageIO.write(
                    fromClosedBuffer,
                    "png",
                    new File(
                            WellKnowns.TEMP_FOLDER,
                            sheet.getPage().getId() + ".spot.png"));
        } catch (IOException ex) {
            logger.warn("Error storing spotTable", ex);
        }

        // Display the gray-level view of all spots
        if (Main.getGui() != null) {
            ImageView imageView = new ImageView(sheet, fromClosedBuffer);
            sheet.getAssembly()
                    .addViewTab(
                            "SpotView",
                            imageView,
                            new BoardsPane(new PixelBoard(sheet)));
        }

        // Binarize the spots, with rather low thresholds
        final PixelFilter source = new VerticalFilter(
                buffer,
                constants.meanCoeff.getValue(),
                constants.stdDevCoeff.getValue());

        // Get and display thresholded spots
        RunsTable table = new RunsTableFactory(SPOT_ORIENTATION, source, 0).createTable(
                "spot");

        //        sheet.getRunsViewer()
        //                .display(table);
        return table;
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        final Constant.Ratio beamCircleDiameterRatio = new Constant.Ratio(
                0.75,
                "Diameter of circle used to close beam spots, as ratio of beam height");

        Constant.Ratio meanCoeff = new Constant.Ratio(
                0.4,
                "Specific adaptive meanCoeff value for spots image");

        Constant.Ratio stdDevCoeff = new Constant.Ratio(
                0.6,
                "Specific adaptive stdDevCoeff value for spots image");

    }
}
