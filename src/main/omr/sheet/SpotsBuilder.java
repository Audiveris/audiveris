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

import omr.image.ImageUtil;
import omr.image.MorphoProcessor;
import omr.image.StructureElement;

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

import omr.util.StopWatch;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code SpotsBuilder} performs morphology analysis to retrieve
 * the major spots that compose beams.
 * <p>
 * It can work on a whole page or on a snapshot of cues aggregate.
 *
 * @author Hervé Bitteur
 */
public class SpotsBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SpotsBuilder.class);

    /** Orientation chosen for spot runs. */
    public static final Orientation SPOT_ORIENTATION = Orientation.VERTICAL;

    //~ Instance fields --------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** Spots lag. */
    private final Lag spotLag;

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

        // Create the spotLag
        spotLag = new BasicLag(Lags.SPOT_LAG, SPOT_ORIENTATION);
        sheet.setLag(Lags.SPOT_LAG, spotLag);
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // buildPageSpots //
    //----------------//
    /**
     * Retrieve all spots from a page.
     * All spots are dispatched among their containing system(s).
     */
    public void buildPageSpots ()
    {
        final StopWatch watch = new StopWatch("buildPageSpots");

        try {
            watch.start("gaussianBuffer");

            // We need a copy of image that we can overwrite. 
            Picture picture = sheet.getPicture();
            ByteProcessor buffer = picture.getSource(
                    Picture.SourceKey.GAUSSIAN);

            // Retrieve major spots
            watch.start("buildSpots");

            Scale scale = sheet.getScale();
            int beam = scale.getMainBeam();
            List<Glyph> spots = buildSpots(buffer, null, beam, null);

            // Dispatch spots per system(s)
            dispatchPageSpots(spots);

            // Display on all spot glyphs
            if (Main.getGui() != null) {
                SpotsController spotController = new SpotsController(
                        sheet,
                        spotLag);
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
     * @param offset buffer offset WRT page coordinates, or null
     * @param beam   typical beam height
     * @param cueId  cue id, null for page
     * @return the collection of spots retrieved
     */
    public List<Glyph> buildSpots (ByteProcessor buffer,
                                   Point offset,
                                   double beam,
                                   String cueId)
    {
        final double diameter = beam * constants.beamCircleDiameterRatio.getValue();
        final float radius = (float) (diameter - 1) / 2;
        logger.info(
                "{}Spots retrieval beam: {}, diameter: {} ...",
                sheet.getLogPrefix(),
                String.format("%.1f", beam),
                String.format("%.1f", diameter));

        final int[] seOffset = {0, 0};
        StructureElement se = new StructureElement(0, 1, radius, seOffset);
        new MorphoProcessor(se).close(buffer);

        // Visual check
        final String pageId = sheet.getPage()
                .getId();

        if (cueId == null) {
            BufferedImage img = buffer.getBufferedImage();

            // Store buffer on disk?
            if (constants.keepPageSpots.isSet()) {
                ImageUtil.saveOnDisk(img, pageId + ".spot");
            }

            // Display the gray-level view of all spots
            if (Main.getGui() != null) {
                sheet.getAssembly()
                        .addViewTab(
                                "SpotView",
                                new ScrollImageView(sheet, new ImageView(img)),
                                new BoardsPane(new PixelBoard(sheet)));
            }
        } else {
            if (constants.keepCueSpots.isSet()) {
                BufferedImage img = buffer.getBufferedImage();
                ImageUtil.saveOnDisk(img, pageId + "." + cueId + ".spot");
            }
        }

        // Binarize the spots via a global filter (no illumination problem)
        buffer.threshold(constants.binarizationThreshold.getValue());

        // Runs
        RunsTable spotTable = new RunsTableFactory(
                SPOT_ORIENTATION,
                buffer,
                0).createTable("spot");

        // Sections
        SectionsBuilder sectionsBuilder = new SectionsBuilder(
                spotLag,
                new JunctionRatioPolicy());
        List<Section> sections = sectionsBuilder.createSections(
                spotTable,
                false);

        if (offset != null) {
            for (Section section : sections) {
                section.translate(offset);
            }
        }

        // Glyphs
        GlyphNest nest = sheet.getNest();
        List<Glyph> glyphs = nest.retrieveGlyphs(
                sections,
                GlyphLayer.SPOT,
                true,
                Glyph.Linking.NO_LINK);

        return glyphs;
    }

    //-------------------//
    // dispatchPageSpots //
    //-------------------//
    /**
     * Dispatch page spots according to their containing system(s),
     * and keeping only those within system width.
     *
     * @param spots the spots to dispatch
     */
    private void dispatchPageSpots (List<Glyph> spots)
    {
        int count = 0;

        List<SystemInfo> relevants = new ArrayList<SystemInfo>();
        SystemManager systemManager = sheet.getSystemManager();

        for (Glyph glyph : spots) {
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

    //~ Inner Classes ----------------------------------------------------------
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

        final Constant.Boolean keepPageSpots = new Constant.Boolean(
                false,
                "Should we store page spot images on disk?");

        final Constant.Boolean keepCueSpots = new Constant.Boolean(
                false,
                "Should we store cue spot images on disk?");

        final Constant.Ratio beamCircleDiameterRatio = new Constant.Ratio(
                0.8,
                "Diameter of circle used to close beam spots, as ratio of beam height");

        final Constant.Integer binarizationThreshold = new Constant.Integer(
                "pixel",
                140,
                "Global threshold used for binarization of gray spots");
    }
}
