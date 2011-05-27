//----------------------------------------------------------------------------//
//                                                                            //
//                           G r i d B u i l d e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphsModel;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphsController;

import omr.lag.ui.RunBoard;
import omr.lag.ui.ScrollLagView;
import omr.lag.ui.SectionBoard;

import omr.log.Logger;
import static omr.run.Orientation.*;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.score.ScoresManager;

import omr.sheet.Sheet;
import omr.sheet.picture.Picture;
import omr.sheet.ui.PixelBoard;

import omr.step.Step;
import omr.step.StepException;
import omr.step.Steps;

import omr.ui.BoardsPane;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.*;

/**
 * Class <code>GridBuilder</code> computes the grid of systems of a sheet
 * picture, based on the retrieval of horizontal staff lines and of vertical
 * bar lines.
 *
 * @author Hervé Bitteur
 */
public class GridBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GridBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** The display if any */
    private GridView view;

    /** Companion in charge of staff lines */
    private final LinesRetriever linesRetriever;

    /** Companion in charge of bar lines */
    private final BarsRetriever barsRetriever;

    /** Companion in charge of target */
    private final TargetBuilder targetBuilder;

    /** The dewarped image */
    private RenderedImage dewarpedImage;

    /** Temp ???? */
    private RunsTable wholeVertTable;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // GridBuilder //
    //-------------//
    /**
     * Retrieve the frames of all staff lines
     *
     * @param sheet the sheet to process
     */
    public GridBuilder (Sheet sheet)
    {
        this.sheet = sheet;

        linesRetriever = new LinesRetriever(sheet);
        barsRetriever = new BarsRetriever(sheet);
        targetBuilder = new TargetBuilder(sheet, linesRetriever, barsRetriever);

        sheet.setTargetBuilder(targetBuilder);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Compute and display the system frames of the sheet picture
     */
    public void buildInfo ()
        throws StepException
    {
        // Build the vertical and horizontal lags
        buildAllLags();

        // Display
        if (constants.displayFrame.getValue() && (Main.getGui() != null)) {
            displayFrame();
        }

        // Retrieve the horizontal filaments
        linesRetriever.buildInfo();
        view.refresh();

        // Retrieve the vertical barlines
        barsRetriever.buildInfo();

        // Update the display
        if (view != null) {
            final int viewIndex = linesRetriever.getLag()
                                                .viewIndexOf(view);

            for (Filament fil : linesRetriever.getGarbage()) {
                view.colorizeGlyph(viewIndex, fil, Color.LIGHT_GRAY);
            }

            view.refresh();
        }

        // Define the destination frames
        targetBuilder.buildInfo();

        // Dewarp the initial image
        dewarpImage();

        // Add a view on dewarped image
        sheet.getAssembly()
             .addViewTab(
            "DeWarped",
            new ScrollView(new DewarpedView(dewarpedImage)),
            null);

        // Store dewarped image on disk
        if (constants.storeDewarp.getValue()) {
            storeImage();
        }
    }

    //--------------//
    // buildAllLags //
    //--------------//
    /**
     * From the sheet picture, build the vertical lag (for barlines) and the
     * horizontal lag (for staff lines)
     */
    private void buildAllLags ()
    {
        // Retrieve all foreground pixels into vertical runs
        wholeVertTable = new RunsTableFactory(
            VERTICAL,
            sheet.getPicture(),
            sheet.getPicture().getMaxForeground(),
            0).createTable("whole-vert");
        // Note: from that point on, we could simply discard the sheet picture
        // and save memory, since wholeVertTable contains all foreground pixels.
        // For the time being, it is kept alive for display purpose

        // Add a view on runs table
        sheet.getAssembly()
             .addRunsTab(wholeVertTable);

        // vLag
        barsRetriever.buildLag(wholeVertTable);

        // hLag
        linesRetriever.buildLag(wholeVertTable);
    }

    //-------------//
    // dewarpImage //
    //-------------//
    private void dewarpImage ()
    {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(Picture.invert(sheet.getPicture().getImage()));
        pb.add(targetBuilder.getDewarpGrid());
        pb.add(new InterpolationBilinear());

        dewarpedImage = Picture.invert(JAI.create("warp", pb));
        ((PlanarImage) dewarpedImage).getTiles();
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        GlyphLag         hLag = linesRetriever.getLag();
        GlyphLag         vLag = barsRetriever.getLag();
        GlyphsController controller = new GlyphsController(
            new GlyphsModel(sheet, hLag, Steps.valueOf(Steps.FRAMES)));

        // Create a view
        view = new GridView(
            sheet,
            linesRetriever,
            hLag,
            barsRetriever,
            vLag,
            null,
            controller);
        view.colorizeAllSections();

        // Boards
        final String  unit = sheet.getId() + ":FramesBuilder";
        BoardsPane    boardsPane = new BoardsPane(
            new PixelBoard(unit, sheet),
            new RunBoard(unit, hLag, true),
            new SectionBoard(unit, hLag.getLastVertexId(), hLag, true),
            new GlyphBoard(unit, controller, null, false));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(view);
        sheet.getAssembly()
             .addViewTab(Step.FRAMES_TAB, slv, boardsPane);
    }

    //------------//
    // storeImage //
    //------------//
    private void storeImage ()
    {
        String pageId = sheet.getPage()
                             .getId();
        File   file = new File(
            ScoresManager.getInstance().getDefaultDewarpDirectory(),
            pageId + ".dewarped.png");

        try {
            String path = file.getCanonicalPath();
            ImageIO.write(dewarpedImage, "png", file);
            logger.info("Wrote " + path);
        } catch (IOException ex) {
            logger.warning("Could not write " + file);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean displayFrame = new Constant.Boolean(
            true,
            "Should we display a frame?");
        Constant.Boolean storeDewarp = new Constant.Boolean(
            false,
            "Should we store the dewarped image on disk?");
    }

    //--------------//
    // DewarpedView //
    //--------------//
    private class DewarpedView
        extends RubberPanel
    {
        //~ Instance fields ----------------------------------------------------

        private final AffineTransform identity = new AffineTransform();
        private final RenderedImage   image;

        //~ Constructors -------------------------------------------------------

        public DewarpedView (RenderedImage image)
        {
            this.image = image;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void render (Graphics2D g)
        {
            // Display the dewarped image
            g.drawRenderedImage(image, identity);

            // Display also the Destination Points
            targetBuilder.renderDewarpGrid(g, false);
        }
    }
}
