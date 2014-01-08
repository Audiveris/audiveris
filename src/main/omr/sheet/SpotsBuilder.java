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

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.image.GlobalFilter;
import omr.image.MorphoProcessor;
import omr.image.PixelBuffer;
import omr.image.PixelFilter;
import omr.image.StructureElement;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.SectionsBuilder;

import omr.run.Orientation;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.sheet.ui.ImageView;
import omr.sheet.ui.PixelBoard;

import omr.ui.BoardsPane;

import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code SpotsBuilder} performs morphology analysis on
 * initial image to retrieve major spots that compose black note heads
 * and beams.
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

        GlyphNest nest = sheet.getNest();
        List<Glyph> glyphs = nest.retrieveGlyphs(
                spotLag.getSections(),
                GlyphLayer.SPOT,
                true,
                Glyph.Linking.NO_LINK);

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
        spotController.refresh();
    }

    //---------------//
    // dispatchSpots //
    //---------------//
    private void dispatchSpots (List<Glyph> glyphs)
    {
        int count = 0;

        List<SystemInfo> relevants = new ArrayList<SystemInfo>();
        SystemManager systemManager = sheet.getSystemManager();

        for (Glyph glyph : glyphs) {
            Point center = glyph.getCentroid();
            systemManager.getSystemsOf(center, relevants);

            boolean created = false;

            for (SystemInfo system : relevants) {
                // Check glyph is within system abscissa boundaries
                if ((center.x >= system.getLeft())
                    && (center.x <= system.getRight())) {
                    glyph.setShape(Shape.BEAM_SPOT);
                    system.registerGlyph(glyph);
                    created = true;
                }
            }

            if (created) {
                count++;
            }
        }

        logger.info("{}Spots retrieved: {}", sheet.getLogPrefix(), count);
    }

    //---------------//
    // retrieveSpots //
    //---------------//
    private RunsTable retrieveSpots ()
    {
        // We need a copy of image that we can overwrite. 
        Picture picture = sheet.getPicture();
        PixelBuffer buffer = (PixelBuffer) picture.getSource(
                Picture.SourceKey.GAUSSIAN);

        watch.start("spots");

        int[] offset = {0, 0};
        Scale scale = sheet.getScale();
        int beam = scale.getMainBeam();

        final double diameter = beam * constants.beamCircleDiameterRatio.getValue();
        final float radius = (float) (diameter - 1) / 2;
        logger.info(
                "{}Spots retrieval beam: {}, diameter: {} ...",
                sheet.getLogPrefix(),
                beam,
                String.format("%.1f", diameter));

        StructureElement se = new StructureElement(0, 1, radius, offset);
        MorphoProcessor mp = new MorphoProcessor(se);

        mp.close(buffer);

        BufferedImage closedImg = buffer.toBufferedImage();

        // Store buffer on disk for further manual analysis if any
        ///ImageUtil.saveOnDisk(closedImg, sheet.getPage().getId() + ".spot");

        // Display the gray-level view of all spots
        if (Main.getGui() != null) {
            ImageView imageView = new ImageView(sheet, closedImg);
            sheet.getAssembly()
                    .addViewTab(
                            "SpotView",
                            imageView,
                            new BoardsPane(new PixelBoard(sheet)));
        }

        // Binarize the spots via a global filter (no illumination problem)
        final PixelFilter source = new GlobalFilter(
                buffer,
                constants.binarizationThreshold.getValue());

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
                0.8,
                "Diameter of circle used to close beam spots, as ratio of beam height");

        final Constant.Integer binarizationThreshold = new Constant.Integer(
                "pixel",
                140,
                "Global threshold used for binarization of gray spots");

    }
}
