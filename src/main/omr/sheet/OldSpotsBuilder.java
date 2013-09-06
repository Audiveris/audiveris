//----------------------------------------------------------------------------//
//                                                                            //
//                            OldSpotsBuilder                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>

package omr.sheet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
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
import omr.image.PixDistance;
import omr.image.PixelBuffer;
import omr.image.StructureElement;
import omr.image.Template;
import omr.image.TemplateFactory;
import omr.lag.BasicLag;
import omr.lag.JunctionAllPolicy;
import omr.lag.Lag;
import omr.lag.SectionsBuilder;
import omr.run.Orientation;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;
import omr.sheet.ui.MultipleRunsViewer;
import omr.sheet.ui.PixelBoard;
import omr.ui.BoardsPane;
import omr.ui.symbol.MusicFont;
import omr.ui.util.ItemRenderer;
import omr.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code OldSpotsBuilder}
 *
 * @author Hervé Bitteur
 */
public class OldSpotsBuilder {

    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            OldSpotsBuilder.class);

    /** Orientation chosen for spot runs. (not very important) */
    private static final Orientation SPOT_ORIENTATION = Orientation.VERTICAL;

    //~ Instance fields --------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** Scale-dependent constants. */
    private final Parameters params;

    /** Long verticals. */
    private RunsTable vertTable;

    /** Long horizontals. */
    private RunsTable horiTable;

    /** Spots runs. */
    private RunsTable spotTable;

    /** Spots lag. */
    private Lag spotLag;

    /** To measure elapsed time. */
    private final StopWatch watch = new StopWatch("SpotsBuilder");

    private boolean[][] splits;

    private List<PixDistance> locations;

    private Template template;

    private ItemRenderer headRenderer;

    private Lag myVLag;

    private List<Glyph> stemFilaments;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // SpotsBuilder //
    //--------------//
    /**
     * Creates a new SpotsBuilder object.
     *
     * @param sheet the related sheet
     */
    public OldSpotsBuilder (Sheet sheet)
    {
        this.sheet = sheet;

        params = new Parameters(sheet.getScale());
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // buildSpots //
    //------------//
    public void buildSpots ()
    {
        try {
            //            buffer = sheet.getWholeVerticalTable()
            //                          .getBuffer();

            //            // Retrieve major vertical runs
            //            vertTable = retrieveHugeVerticals();
            //
            //            // Retrieve major horizontal runs
            //            horiTable = retrieveHugeHorizontals();

            // Retrieve major spots
            spotTable = retrieveSpots();

            // Allocate sections & glyphs for spots
            buildSpotGlyphs();

            // Classify the spot glyphs
            ///classifyGlyphs();

            // Matching for all possible note heads
            ///locations = matchNoteHeads();

            // Filter vertical sticks

            // Filter horizontal sticks

            // Drive matching via sticks (stems and ledgers/stafflines)

            // Render the matching locations
            //            headRenderer = createHeadRenderer();
            //
            //            if (headRenderer != null) {
            //                sheet.addItemRenderer(headRenderer);
            //            }
        } catch (Exception ex) {
            logger.warn("Error in SpotsBuilder.buildSpots()", ex);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }

        MultipleRunsViewer viewer = new MultipleRunsViewer(sheet);
        viewer.display(spotTable, horiTable, vertTable);
    }

    //-----------------//
    // toBufferedImage //
    //-----------------//
    public BufferedImage toBufferedImage (boolean[][] image)
    {
        StopWatch watch = new StopWatch("Boolean to BufferImage");
        watch.start("toImage");

        final int width = image.length;
        final int height = image[0].length;
        final BufferedImage img = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_BYTE_GRAY);
        final WritableRaster raster = img.getRaster();
        final int[] pixel = new int[1];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel[0] = image[x][y] ? 0 : 255;
                raster.setPixel(x, y, pixel);
            }
        }

        ///watch.print();
        return img;
    }

    //
    //    //----------------//
    //    // buildReference //
    //    //----------------//
    //    private boolean[][] buildReference (String  title,
    //                                        boolean foreground)
    //    {
    //        watch.start("buildReference " + title);
    //
    //        int         width = buffer.getWidth();
    //        int         height = buffer.getHeight();
    //        boolean[][] ref = new boolean[width][height];
    //
    //        for (int y = 0; y < height; y++) {
    //            for (int x = 0; x < width; x++) {
    //                int pix = buffer.getPixel(x, y);
    //
    //                if (pix < 0) {
    //                    pix += 256;
    //                }
    //
    //                ref[x][y] = foreground ? (pix < 127) : (pix >= 127);
    //            }
    //        }
    //
    //        //        // Remove heavy spots
    //        //        if (removeSpots) {
    //        //            List<Glyph> glyphs = new ArrayList<Glyph>(spotNest.getAllGlyphs());
    //        //
    //        //            for (Glyph glyph : glyphs) {
    //        //                if (glyph.getWeight() >= params.maxVoidWeight) {
    //        //                    glyph.setShape(Shape.BEAM);
    //        //
    //        //                    Rectangle box = glyph.getBounds();
    //        //                    BufferedImage img = glyph.getImage();
    //        //
    //        //                    for (int x = 0; x < img.getWidth(); x++) {
    //        //                        for (int y = 0; y < img.getHeight(); y++) {
    //        //                            int pix = img.getRGB(x, y) & 0xFF;
    //        //
    //        //                            if (pix == 255) {
    //        //                                // Wipe out the pixel underneath
    //        //                                ref[x + box.x][y + box.y] = false;
    //        //                            }
    //        //                        }
    //        //                    }
    //        //                }
    //        //            }
    //        //        }
    //
    //        //        // Store buffer on disk for further manual analysis if any
    //        //        BufferedImage img = toBufferedImage(ref);
    //        //
    //        //        try {
    //        //            ImageIO.write(
    //        //                    img,
    //        //                    "png",
    //        //                    new File(
    //        //                    WellKnowns.TEMP_FOLDER,
    //        //                    sheet.getPage().getId() + "." + title + ".png"));
    //        //        } catch (IOException ex) {
    //        //            logger.warn("Error storing ref", ex);
    //        //        }
    //        //
    //        //        // Display the gray-level view
    //        //        if (Main.getGui() != null) {
    //        //            ImageView imageView = new ImageView(sheet, img);
    //        //            sheet.getAssembly()
    //        //                    .addViewTab(
    //        //                    "ref-" + title,
    //        //                    imageView,
    //        //                    new BoardsPane(new PixelBoard(sheet)));
    //        //        }
    //        return ref;
    //    }
    //-----------------//
    // buildSpotGlyphs //
    //-----------------//
    private void buildSpotGlyphs ()
    {
        logger.debug("Run sequences: {}", spotTable.getSize());

        // Build the spotLag out of spots runs
        spotLag = new BasicLag("spotLag", SPOT_ORIENTATION);

        SectionsBuilder sectionsBuilder = new SectionsBuilder(
                spotLag,
                new JunctionAllPolicy());
        sectionsBuilder.createSections(spotTable, false);
        logger.debug("Sections: {}", spotLag.getSections().size());

        List<Glyph> glyphs = GlyphsBuilder.retrieveGlyphs(
                spotLag.getSections(),
                sheet.getNest(),
                GlyphLayer.SPOT,
                sheet.getScale());
        logger.info("Spots retrieved: {}", glyphs.size());

        splits = new boolean[sheet.getWidth()][sheet.getHeight()];
        sheet.setSpotLag(spotLag);

        // Display on all spot glyphs
        SpotsController spotController = new SpotsController(
                sheet,
                spotLag, null);
        spotController.refresh();
    }

    //---------------//
    // buildTemplate //
    //---------------//
    private Template buildTemplate ()
    {
        watch.start("Build template");

        return TemplateFactory.getInstance()
                .getTemplate(
                ///Shape.NOTEHEAD_BLACK, 
                ///Shape.WHOLE_ODD,
                ///Shape.WHOLE_EVEN,
                Shape.VOID_EVEN,
                ///Shape.VOID_ODD,
                sheet.getScale().getInterline());
    }

    //----------------//
    // classifyGlyphs //
    //----------------//
    /**
     * The goal is to retrieve interesting glyphs among the spots.
     * Glyphs can be: [packs of] note heads, [packs of] beams, clutter
     * We can use glyph geometric characteristics, location WRT nearest staves,
     * related glyphs: ledgers for hote heads, stems for note heads and beams,
     * paired beams for beams.
     */
    private void classifyGlyphs ()
    {
        //        List<Glyph> glyphs = new ArrayList<Glyph>(spotNest.getAllGlyphs());
        //
        //        // Look for beams spots?
        //        Collections.sort(glyphs, Glyph.byReverseWeight);
        //
        //        // Apply a watershed split on large glyphs
        //        //List<Integer> vips = Arrays.asList(320);
        //        int maxWeight = sheet.getScale()
        //                .toPixels(constants.maxVoidWeight);
        //
        //        for (Glyph glyph : glyphs) {
        //            ///if (vips.contains(glyph.getId())) {
        //            ///split(glyph);
        //            if (glyph.getWeight() >= maxWeight) {
        //                glyph.setShape(Shape.BEAM);
        //            }
        //        }
    }

    //--------------------//
    // createHeadRenderer //
    //--------------------//
    private ItemRenderer createHeadRenderer ()
    {
        return new ItemRenderer()
        {
            @Override
            public void renderItems (Graphics2D g)
            {
                Rectangle clip = g.getClipBounds();
                final int tWidth = template.getWidth();
                final int tHeight = template.getHeight();
                final int interline = sheet.getScale()
                        .getInterline();
                final MusicFont musicFont = MusicFont.getFont(interline);

                final Color oldColor = g.getColor();
                g.setColor(new Color(255, 0, 0, 150));

                for (PixDistance loc : locations) {
                    Rectangle tBox = new Rectangle(
                            loc.x,
                            loc.y,
                            tWidth,
                            tHeight);

                    if (clip.intersects(tBox)) {
                        template.render(loc.x, loc.y, g, musicFont);
                    }
                }

                g.setColor(oldColor);
            }
        };
    }

    //---------------//
    // filterMatches //
    //---------------//
    private List<PixDistance> filterMatches (List<PixDistance> rawLocs)
    {
        System.out.println();
        System.out.println("Best matches:");

        Collections.sort(rawLocs);

        // Gather matches per close locations

        // Avoid duplicate locations
        List<Aggregate> aggregates = new ArrayList<Aggregate>();

        for (PixDistance loc : rawLocs) {
            // Check among already filtered locations for similar location
            Aggregate aggregate = null;

            for (Aggregate ag : aggregates) {
                Point p = ag.point;
                int dx = loc.x - p.x;
                int dy = loc.y - p.y;

                if ((Math.abs(dx) <= params.maxTemplateDelta)
                    && (Math.abs(dy) <= params.maxTemplateDelta)) {
                    aggregate = ag;

                    break;
                }
            }

            if (aggregate == null) {
                aggregate = new Aggregate();
                aggregates.add(aggregate);
            }

            aggregate.add(loc);
        }

        logger.info("Aggregates: {}", aggregates.size());

        List<PixDistance> filtered = new ArrayList<PixDistance>();

        for (Aggregate ag : aggregates) {
            filtered.add(ag.getMeanLocation());
        }

        for (PixDistance loc : filtered) {
            logger.info(loc.toString());
        }

        logger.info("Filtered: {}", filtered.size());

        return filtered;
    }

    //    //----------------//
    //    // matchNoteHeads //
    //    //----------------//
    //    private List<PixDistance> matchNoteHeads ()
    //    {
    //        template = buildTemplate();
    //
    //        watch.start("Note match");
    //
    //        ChamferMatching matchFG = new ChamferMatching(dist2FG);
    //        List<PixDistance> locs = matchFG.matchAll(
    //                template,
    //                params.maxMatchingDistance);
    //
    //        watch.start("Filter matches");
    //
    //        locations = filterMatches(locs);
    //
    //        watch.stop();
    //
    //        return locations;
    //    }
    //    //-------//
    //    // merge //
    //    //-------//
    //    private boolean[][] merge (Point       origin,
    //                               boolean[][] image,
    //                               boolean[][] lines)
    //    {
    //        final int width = lines.length;
    //        final int height = lines[0].length;
    //
    //        for (int y = 0; y < height; y++) {
    //            for (int x = 0; x < width; x++) {
    //                if (lines[x][y] && !image[x][y]) {
    //                    image[x][y] = true;
    //                    splits[x + origin.x][y + origin.y] = true;
    //                }
    //            }
    //        }
    //
    //        return image;
    //    }
    //    //-------------------------//
    //    // retrieveHugeHorizontals //
    //    //-------------------------//
    //    private RunsTable retrieveHugeHorizontals ()
    //    {
    //        watch.start("huge horizontals");
    //
    //        RunsTable table = new RunsTableFactory(
    //            Orientation.HORIZONTAL,
    //            buffer,
    //            0).createTable("hori");
    //        table.purge(
    //            new Predicate<Run>() {
    //                    @Override
    //                    public final boolean check (Run run)
    //                    {
    //                        return run.getLength() < params.minHorizontalRunLength;
    //                    }
    //                });
    //
    //        sheet.getRunsViewer()
    //             .display(table);
    //
    //        return table;
    //    }
    //
    //    //-----------------------//
    //    // retrieveHugeVerticals //
    //    //-----------------------//
    //    private RunsTable retrieveHugeVerticals ()
    //        throws Exception
    //    {
    //        watch.start("huge verticals");
    //
    //        RunsTable table = sheet.getWholeVerticalTable()
    //                               .copy("vert")
    //                               .purge(
    //            new Predicate<Run>() {
    //                    @Override
    //                    public final boolean check (Run run)
    //                    {
    //                        return run.getLength() < params.minVerticalRunLength;
    //                    }
    //                });
    //
    //        sheet.getRunsViewer()
    //             .display(table);
    //
    //        myVLag = new BasicLag("myVLag", VERTICAL);
    //
    //        SectionsBuilder sectionsBuilder = new SectionsBuilder(
    //            myVLag,
    //            new JunctionRatioPolicy(params.maxLengthRatio));
    //        sectionsBuilder.createSections(table);
    //
    //        // Filaments factory
    //        stemNest = new BasicNest("stemNest", sheet);
    //
    //        FilamentsFactory factory = new FilamentsFactory(
    //            sheet.getScale(),
    //            stemNest,
    //            VERTICAL,
    //            Filament.class);
    //
    //        // Factory parameters adjustment
    //        factory.setMaxSectionThickness(constants.maxStemSectionThickness);
    //        factory.setMaxFilamentThickness(constants.maxStemThickness);
    //
    //        //        factory.setMaxCoordGap(constants.maxCoordGap);
    //        //        factory.setMaxPosGap(constants.maxPosGap);
    //        //        factory.setMaxSpace(constants.maxSpace);
    //        //        factory.setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);
    //
    //        // Retrieve filaments out of vertical sections
    //        stemFilaments = new ArrayList<Glyph>();
    //        stemFilaments.addAll(
    //            factory.retrieveFilaments(myVLag.getVertices(), true));
    //
    //        for (Glyph glyph : stemFilaments) {
    //            glyph.setShape(Shape.STEM);
    //        }
    //
    //        // Display on all stem glyphs
    //        StemController stemController = new StemController(
    //            sheet,
    //            stemNest,
    //            myVLag);
    //        stemController.refresh();
    //
    //        return table;
    //    }
    //---------------//
    // retrieveSpots //
    //---------------//
    private RunsTable retrieveSpots ()
    {
        watch.start("spots");

        int[] offset = {0, 0};
        int beam = sheet.getScale()
                .getMainBeam();
        float radius = (beam - 3) / 2f;
        logger.debug("Spots retrieval beam: {}, radius: {}", beam, radius);

        StructureElement se = new StructureElement(0, 1, radius, offset);

        MorphoProcessor mp = new MorphoProcessor(se);
        PixelBuffer buffer = sheet.getWholeVerticalTable()
                .getBuffer();

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

        // Get and display thresholded spots
        RunsTable table = new RunsTableFactory(SPOT_ORIENTATION, buffer, 0).createTable(
                "spot");
        sheet.getRunsViewer()
                .display(table);

        return table;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Aggregate //
    //-----------//
    /**
     * Describes a aggregate of matches around similar location.
     */
    private static class Aggregate
    {
        //~ Instance fields ----------------------------------------------------

        Point point;

        List<PixDistance> matches = new ArrayList<PixDistance>();

        //~ Methods ------------------------------------------------------------
        public void add (PixDistance match)
        {
            if (point == null) {
                point = new Point(match.x, match.y);
            }

            matches.add(match);
        }

        /**
         * Use barycenter (with weights decreasing with distance? no!)
         *
         * @return a mean location
         */
        public PixDistance getMeanLocation ()
        {
            double xx = 0;
            double yy = 0;
            double dd = 0;

            for (PixDistance match : matches) {
                xx += match.x;
                yy += match.y;
                dd += match.d;
            }

            int n = matches.size();

            PixDistance mean = new PixDistance(
                    (int) Math.rint(xx / n),
                    (int) Math.rint(yy / n),
                    dd / n);
            logger.info("Mean {} details: {}", mean, this);

            return mean;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{");
            sb.append(getClass().getSimpleName());

            if (point != null) {
                sb.append(" point:(")
                        .append(point.x)
                        .append(",")
                        .append(point.y)
                        .append(")");
            }

            sb.append(" ")
                    .append(matches.size())
                    .append(" matches: ");

            for (PixDistance match : matches) {
                sb.append(match);
            }

            sb.append("}");

            return sb.toString();
        }
    }

    //    //-------//
    //    // split //
    //    //-------//
    //    private void split (Glyph glyph)
    //    {
    //        logger.info(glyph.toString());
    //
    //        Rectangle rect = glyph.getBounds();
    //        rect.grow(1, 1); // To add margin around glyph
    //
    //        boolean[][]   refPoints = new boolean[rect.width][rect.height];
    //        BufferedImage img = glyph.getImage();
    //
    //        for (int y = 0; y < rect.height; y++) {
    //            for (int x = 0; x < rect.width; x++) {
    //                if ((x == 0) ||
    //                    (x == (rect.width - 1)) ||
    //                    (y == 0) ||
    //                    (y == (rect.height - 1))) {
    //                    refPoints[x][y] = true;
    //                } else {
    //                    int pix = img.getRGB(x - 1, y - 1) & 0xFF;
    //                    refPoints[x][y] = (pix == 0);
    //                }
    //            }
    //        }
    //
    //        TableUtil.dump("input:", refPoints);
    //
    //        ChamferDistanceInteger chamferDistance = new ChamferDistanceInteger();
    //        int[][]                dists = chamferDistance.compute(refPoints, null);
    //        ///normalize(dists, normalizer);
    //        TableUtil.dump("Raw distances:", dists);
    //
    //        WatershedGrayLevel instance = new WatershedGrayLevel(dists, false);
    //        int                step = 256; // Avoid small fragments
    //        boolean[][]        result = instance.process(step);
    //
    //        boolean[][]        merge = merge(rect.getLocation(), refPoints, result);
    //
    //        TableUtil.dump("regions", merge);
    //    }
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

        final Scale.Fraction minVerticalRunLength = new Scale.Fraction(
                2.0,
                "Minimum length for a huge vertical");

        final Scale.Fraction minHorizontalRunLength = new Scale.Fraction(
                1.8,
                "Minimum length for a huge horizontal");

        final Scale.Fraction maxTemplateDelta = new Scale.Fraction(
                0.75,
                "Maximum dx or dy between similar template instances");

        final Scale.Fraction maxStemSectionThickness = new Scale.Fraction(
                0.3,
                "Maximum thickness of a stem section");

        final Scale.Fraction maxStemThickness = new Scale.Fraction(
                0.3,
                "Maximum thickness of a stem");

        final Scale.AreaFraction maxVoidWeight = new Scale.AreaFraction(
                1.25,
                "Maximum weight for void head spots");

        final Constant.Ratio maxLengthRatio = new Constant.Ratio(
                1.4,
                "Maximum ratio in length for a run to be combined with an existing section");

        final Constant.Double maxMatchingDistance = new Constant.Double(
                "distance**2",
                0.8,
                "Maximum (square) matching distance");

    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all pre-scaled constants
     * related to horizontal frames.
     */
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        final int minVerticalRunLength;

        final int minHorizontalRunLength;

        final int maxTemplateDelta;

        final int maxVoidWeight;

        final double maxMatchingDistance;

        final double maxLengthRatio;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            minVerticalRunLength = scale.toPixels(
                    constants.minVerticalRunLength);
            minHorizontalRunLength = scale.toPixels(
                    constants.minHorizontalRunLength);
            maxTemplateDelta = scale.toPixels(constants.maxTemplateDelta);
            maxVoidWeight = scale.toPixels(constants.maxVoidWeight);
            maxLengthRatio = constants.maxLengthRatio.getValue();
            maxMatchingDistance = constants.maxMatchingDistance.getValue();

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }
}
