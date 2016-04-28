//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P i c t u r e                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright (C) Hervé Bitteur and Brenton Partr4dge 2000-2013.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;

import omr.image.FilterDescriptor;
import omr.image.GaussianGrayFilter;
import omr.image.ImageFormatException;
import omr.image.ImageUtil;
import omr.image.MedianGrayFilter;
import omr.image.PixelFilter;
import omr.image.PixelSource;
import static omr.run.Orientation.VERTICAL;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.sheet.grid.LineInfo;

import omr.ui.selection.LocationEvent;
import omr.ui.selection.MouseMovement;
import omr.ui.selection.PixelEvent;
import omr.ui.selection.SelectionService;

import omr.util.Navigable;
import omr.util.StopWatch;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.media.jai.JAI;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Picture} starts from the original BufferedImage to provide all {@link
 * PixelSource} instances derived from it.
 * <p>
 * The {@code Picture} constructor takes a provided original image, whatever its format and color
 * model, and converts it if necessary to come up with a usable gray-level PixelSource: the INITIAL
 * source.
 * <p>
 * Besides the INITIAL source, this class handles a collection of sources, all of the same
 * dimension, with the ability to retrieve them on demand or dispose them, via {@link #getSource}
 * and {@link #disposeSource} methods.
 * <p>
 * Any instance of this class is registered on the related Sheet location service, so that each time
 * a location event is received, the corresponding pixel gray value of the INITIAL sources is
 * published.
 *
 * <p>
 * TODO: When an alpha channel is involved, perform the alpha multiplication if the components are
 * not yet premultiplied.
 *
 * <h4>Overview of transforms:<br>
 * <img src="../image/doc-files/transforms.png">
 * </h4>
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "picture")
public class Picture
        implements EventSubscriber<LocationEvent>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Picture.class);

    //~ Enumerations -------------------------------------------------------------------------------
    /**
     * The set of handled sources.
     */
    public static enum SourceKey
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** The initial gray-level source. */
        INITIAL,
        /** The binarized (black & white) source. */
        BINARY,
        /** The Gaussian-filtered source. */
        GAUSSIAN,
        /** The Median-filtered source. */
        MEDIAN,
        /** The source with staff lines removed. */
        NO_STAFF;
    }

    /**
     * The set of handled tables.
     */
    public static enum TableKey
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        BINARY,
        HEAD_SPOTS;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Image width. */
    @XmlAttribute(name = "width")
    private final int width;

    /** Image height. */
    @XmlAttribute(name = "height")
    private final int height;

    /** Map of all handled run tables. */
    @XmlElement(name = "tables")
    private final EnumMap<TableKey, RunTableHolder> tables = new EnumMap<TableKey, RunTableHolder>(
            TableKey.class);

    // Transient data
    //---------------
    //
    /** Map of all handled sources. */
    private final ConcurrentSkipListMap<SourceKey, WeakReference<ByteProcessor>> sources = new ConcurrentSkipListMap<SourceKey, WeakReference<ByteProcessor>>();

    /** Related sheet. */
    @Navigable(false)
    private Sheet sheet;

    /**
     * Service object where gray level of pixel is to be written to when so asked for
     * by the onEvent() method.
     */
    private final SelectionService levelService;

    /** The initial (gray-level) image, if any. */
    private BufferedImage initialImage;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Build a picture instance from a given original image.
     *
     * @param sheet        the related sheet
     * @param image        the provided original image
     * @param levelService service where pixel events are to be written
     * @throws ImageFormatException
     */
    public Picture (Sheet sheet,
                    BufferedImage image,
                    SelectionService levelService)
            throws ImageFormatException
    {
        initTransients(sheet);
        this.levelService = levelService;

        // Make sure format, colors, etc are OK for us
        ///ImageUtil.printInfo(image, "Original image");
        image = checkImage(image);
        width = image.getWidth();
        height = image.getHeight();

        // Remember the initial image
        initialImage = image;
        logger.debug("InitialImage {}", image);
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private Picture ()
    {
        this.sheet = null;
        this.width = 0;
        this.height = 0;
        this.levelService = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------------//
    // buildNoStaffTable //
    //-------------------//
    public RunTable buildNoStaffTable ()
    {
        return new RunTableFactory(VERTICAL).createTable(getSource(SourceKey.NO_STAFF));
    }

    //---------------------------//
    // buildStaffLineGlyphsImage //
    //---------------------------//
    public BufferedImage buildStaffLineGlyphsImage ()
    {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = img.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.BLACK);

        for (SystemInfo system : sheet.getSystems()) {
            for (Staff staff : system.getStaves()) {
                for (LineInfo li : staff.getLines()) {
                    StaffLine line = (StaffLine) li;
                    Glyph glyph = line.getGlyph();
                    glyph.getRunTable().render(g, glyph.getTopLeft());
                }
            }
        }

        g.dispose();

        return img;
    }

    // For debug only
    public void checkSources ()
    {
        for (SourceKey key : SourceKey.values()) {
            logger.info(String.format("%15s ref:%s", key, getStrongRef(key)));
        }
    }

    //---------------//
    // disposeSource //
    //---------------//
    public void disposeSource (SourceKey key)
    {
        // Nullify cached data, if needed
        if (key == SourceKey.INITIAL) {
            initialImage = null;
        }

        sources.remove(key);
    }

    //
    //    //--------------//
    //    // disposeTable //
    //    //--------------//
    //    public void disposeTable (TableKey key)
    //    {
    //        RunTableHolder tableHolder = tables.get(key);
    //
    //        if (tableHolder != null) {
    //            tableHolder.store();
    //            tableHolder.setData(null);
    //            logger.debug("{} table disposed.", key);
    //        }
    //    }
    //
    //---------------//
    // dumpRectangle //
    //---------------//
    /**
     * Debugging routine, that prints a basic representation of a
     * rectangular portion of the selected image.
     *
     * @param key   the selected image key
     * @param title an optional title for this image dump
     * @param xMin  x first abscissa
     * @param xMax  x last abscissa
     * @param yMin  y first ordinate
     * @param yMax  y last ordinate
     */
    public void dumpRectangle (SourceKey key,
                               String title,
                               int xMin,
                               int xMax,
                               int yMin,
                               int yMax)
    {
        ByteProcessor source = getSource(key);
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%n"));

        if (title != null) {
            sb.append(String.format("%s%n", title));
        }

        // Abscissae
        sb.append("     ");

        for (int x = xMin; x <= xMax; x++) {
            sb.append(String.format("%4d", x));
        }

        sb.append(String.format("%n    +"));

        for (int x = xMin; x <= xMax; x++) {
            sb.append(" ---");
        }

        sb.append(String.format("%n"));

        // Pixels
        for (int y = yMin; y <= yMax; y++) {
            sb.append(String.format("%4d", y));
            sb.append("|");

            for (int x = xMin; x <= xMax; x++) {
                int pix = source.get(x, y);

                if (pix == 255) { // White background
                    sb.append("   .");
                } else {
                    sb.append(String.format("%4d", pix));
                }
            }

            sb.append(String.format("%n"));
        }

        sb.append(String.format("%n"));

        logger.info(sb.toString());
    }

    //------------------//
    // gaussianFiltered //
    //------------------//
    public ByteProcessor gaussianFiltered (ByteProcessor src)
    {
        StopWatch watch = new StopWatch("Gaussian");

        try {
            watch.start("Filter " + src.getWidth() + "x" + src.getHeight());

            final int radius = constants.gaussianRadius.getValue();
            logger.debug("Image blurred with gaussian kernel radius: {}", radius);

            GaussianGrayFilter gaussianFilter = new GaussianGrayFilter(radius);

            return gaussianFilter.filter(src);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //-------------------------------//
    // getDefaultExtractionDirectory //
    //-------------------------------//
    public static String getDefaultExtractionDirectory ()
    {
        return constants.defaultExtractionDirectory.getValue();
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the picture height in pixels.
     *
     * @return the height value
     */
    public int getHeight ()
    {
        return height;
    }

    //----------//
    // getImage //
    //----------//
    /**
     * Report the sheet image, or a sub-image of it if rectangle area is specified.
     * <p>
     * We use the initial image if it is still available.
     * Otherwise we use the binary source.
     *
     * @param rect rectangular area desired, null for whole image
     * @return the (sub) image
     */
    public BufferedImage getImage (Rectangle rect)
    {
        BufferedImage img = initialImage;

        if (img == null) {
            ByteProcessor buffer = getSource(SourceKey.BINARY);
            img = buffer.getBufferedImage();
        }

        if (rect == null) {
            return img;
        } else {
            return img.getSubimage(rect.x, rect.y, rect.width, rect.height);
        }
    }

    //-----------------//
    // getInitialImage //
    //-----------------//
    /** Report the initial (BufferedImage) image.
     *
     * @return the initial image
     */
    public BufferedImage getInitialImage ()
    {
        return initialImage;
    }

    //------------------//
    // getInitialSource //
    //------------------//
    /** Report the initial source.
     *
     * @param img the initial image
     * @return the initial source
     */
    public ByteProcessor getInitialSource (BufferedImage img)
    {
        if (img != null) {
            if (img.getType() != BufferedImage.TYPE_BYTE_GRAY) {
                StopWatch watch = new StopWatch("ToGray");
                watch.start("convertToByteProcessor");

                ColorProcessor cp = new ColorProcessor(img);
                ByteProcessor bp = cp.convertToByteProcessor();

                if (constants.printWatch.isSet()) {
                    watch.print();
                }

                return bp;
            } else {
                return new ByteProcessor(img);
            }
        } else {
            return null;
        }
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name for this Observer.
     *
     * @return Observer name
     */
    public String getName ()
    {
        return "Picture";
    }

    //-----------//
    // getSource //
    //-----------//
    /**
     * Report the desired source.
     * If the source is not yet cached, build the source and store it in cache via weak reference.
     *
     * @param key the key of desired source
     * @return the source ready to use
     */
    public ByteProcessor getSource (SourceKey key)
    {
        ByteProcessor src = getStrongRef(key);

        if (src == null) {
            switch (key) {
            case INITIAL:
                src = getInitialSource(initialImage);

                break;

            case BINARY:

                // Built from binary run table, if available
                RunTable table = getTable(TableKey.BINARY);

                if (table != null) {
                    src = table.getBuffer();
                } else if (initialImage != null) {
                    // Built via binarization of initial source
                    src = binarized(getSource(SourceKey.INITIAL));
                } else {
                    logger.warn("Cannot provide BINARY source");

                    return null;
                }

                break;

            case GAUSSIAN:
                // Built from median
                src = gaussianFiltered(getSource(SourceKey.MEDIAN));

                break;

            case MEDIAN:
                // Built from no_staff
                src = medianFiltered(getSource(SourceKey.NO_STAFF));

                break;

            case NO_STAFF:
                // Built by erasing StaffLines glyphs from binary source
                src = buildNoStaffBuffer();

                break;
            }

            if (src != null) {
                // Store in cache
                sources.put(key, new WeakReference(src));
                logger.debug("{} source built as {}", key, src);
            }
        }

        return src;
    }

    //----------//
    // getTable //
    //----------//
    /**
     * Report the desired table.
     *
     * @param key key of desired table
     * @return the table found, if any, null otherwise
     */
    public RunTable getTable (TableKey key)
    {
        RunTableHolder tableHolder = tables.get(key);

        if (tableHolder == null) {
            return null;
        }

        final RunTable table = tableHolder.getData(sheet);

        return table;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width of the picture image.
     *
     * @return the current width value, in pixels.
     */
    public int getWidth ()
    {
        return width;
    }

    //----------//
    // hasTable //
    //----------//
    /**
     * Report whether the desired table is known
     *
     * @param key key of desired table
     * @return true if we have a tableHolder, false otherwise
     */
    public boolean hasTable (TableKey key)
    {
        return tables.get(key) != null;
    }

    //---------------//
    // hasTableReady //
    //---------------//
    /**
     * Report whether the desired table is known and loaded.
     *
     * @param key key of desired table
     * @return true if we have a tableHolder with loaded data
     */
    public boolean hasTableReady (TableKey key)
    {
        RunTableHolder tableHolder = tables.get(key);

        if (tableHolder == null) {
            return false;
        }

        return tableHolder.hasData();
    }

    //----------------//
    // medianFiltered //
    //----------------//
    public ByteProcessor medianFiltered (ByteProcessor src)
    {
        StopWatch watch = new StopWatch("Median");

        try {
            watch.start("Filter " + src.getWidth() + "x" + src.getHeight());

            final int radius = constants.medianRadius.getValue();
            logger.debug("Image filtered with median kernel radius: {}", radius);

            MedianGrayFilter medianFilter = new MedianGrayFilter(radius);

            return medianFilter.filter(src);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when sheet location has been modified.
     * Based on sheet location, we forward the INITIAL pixel gray level to
     * whoever is interested in it.
     *
     * @param event the (sheet) location event
     */
    @Override
    public void onEvent (LocationEvent event)
    {
        if (initialImage == null) {
            return;
        }

        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            Integer level = null;

            // Compute and forward pixel gray level
            Rectangle rect = event.getData();

            if (rect != null) {
                Point pt = rect.getLocation();

                // Check that we are not pointing outside the image
                if ((pt.x >= 0) && (pt.x < getWidth()) && (pt.y >= 0) && (pt.y < getHeight())) {
                    ByteProcessor src = getSource(SourceKey.INITIAL);

                    if (src != null) {
                        level = src.get(pt.x, pt.y);
                    }
                }
            }

            levelService.publish(new PixelEvent(this, event.hint, event.movement, level));
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //-------------//
    // removeTable //
    //-------------//
    /**
     * Remove a table.
     *
     * @param key table key
     */
    public void removeTable (TableKey key)
    {
        tables.remove(key);
    }

    //-------------------------------//
    // setDefaultExtractionDirectory //
    //-------------------------------//
    public static void setDefaultExtractionDirectory (String dir)
    {
        constants.defaultExtractionDirectory.setValue(dir);
    }

    //----------//
    // setTable //
    //----------//
    /**
     * Register a table.
     *
     * @param key   table key
     * @param table table to register
     */
    public void setTable (TableKey key,
                          RunTable table)
    {
        RunTableHolder tableHolder = new RunTableHolder(key + ".xml");
        tableHolder.setData(table);
        tables.put(key, tableHolder);

        switch (key) {
        case BINARY:
            disposeSource(SourceKey.BINARY);
        }
    }

    //-------//
    // store //
    //-------//
    public void store (Path sheetPath,
                       Path oldSheetPath)
    {
        // Each handled table
        for (Entry<TableKey, RunTableHolder> entry : tables.entrySet()) {
            final TableKey key = entry.getKey();
            final RunTableHolder holder = entry.getValue();
            final Path tablepath = sheetPath.resolve(key + ".xml");

            if (!holder.hasData()) {
                if (oldSheetPath != null) {
                    try {
                        // Copy from old book file to new
                        Path oldTablePath = oldSheetPath.resolve(key + ".xml");
                        Files.copy(oldTablePath, tablepath);
                        logger.info("Copied {}", tablepath);
                    } catch (IOException ex) {
                        logger.warn("Error in picture.store " + ex, ex);
                    }
                }
            } else {
                try {
                    // Too conservative. TODO: Has data been modified WRT file ???
                    Files.deleteIfExists(tablepath);

                    OutputStream os = Files.newOutputStream(tablepath, StandardOpenOption.CREATE);
                    Marshaller m = JAXBContext.newInstance(RunTable.class).createMarshaller();
                    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

                    RunTable table = holder.getData(sheet);
                    m.marshal(table, os);
                    os.close();
                    logger.info("Stored {}", tablepath);
                } catch (Exception ex) {
                    logger.warn("Error in picture.store " + ex, ex);
                }
            }
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return getName();
    }

    //----------------//
    // initTransients //
    //----------------//
    /**
     * (package private) method to initialize needed transient members.
     * (which by definition have not been set by the unmarshalling).
     *
     * @param sheet the containing sheet
     */
    final void initTransients (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //-------------------//
    // adjustImageFormat //
    //-------------------//
    /**
     * Check if the image format (and especially its color model) is
     * properly handled by Audiveris and adjust if needed.
     *
     * @throws ImageFormatException is the format is not supported
     */
    private BufferedImage adjustImageFormat (BufferedImage img)
            throws ImageFormatException
    {
        ColorModel colorModel = img.getColorModel();
        boolean hasAlpha = colorModel.hasAlpha();
        logger.debug("{}", colorModel);

        // Check nb of bands
        SampleModel sampleModel = img.getSampleModel();
        int numBands = sampleModel.getNumBands();
        logger.debug("numBands={}", numBands);

        if (numBands == 1) {
            // Pixel gray value. Nothing to do
            return img;
        } else if ((numBands == 2) && hasAlpha) {
            // Pixel + alpha
            // Discard alpha
            return JAI.create("bandselect", img, new int[]{0}).getAsBufferedImage();
        } else if ((numBands == 3) && !hasAlpha) {
            // RGB
            return ImageUtil.maxRgbToGray(img);
        } else if ((numBands == 4) && hasAlpha) {
            // RGB + alpha
            return ImageUtil.maxRgbaToGray(img);
        } else {
            throw new ImageFormatException(
                    "Unsupported sample model numBands=" + numBands + " hasAlpha=" + hasAlpha);
        }
    }

    //-----------//
    // binarized //
    //-----------//
    private ByteProcessor binarized (ByteProcessor src)
    {
        FilterDescriptor desc = sheet.getStub().getFilterParam().getTarget();
        logger.info("{} {}", "Binarization", desc);
        sheet.getStub().getFilterParam().setActual(desc);

        PixelFilter filter = desc.getFilter(src);

        return filter.filteredImage();
    }

    //--------------------//
    // buildNoStaffBuffer //
    //--------------------//
    private ByteProcessor buildNoStaffBuffer ()
    {
        ByteProcessor src = getSource(SourceKey.BINARY);
        ByteProcessor buf = (ByteProcessor) src.duplicate();
        BufferedImage img = buf.getBufferedImage();
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);

        for (SystemInfo system : sheet.getSystems()) {
            for (Staff staff : system.getStaves()) {
                for (LineInfo li : staff.getLines()) {
                    StaffLine line = (StaffLine) li;
                    Glyph glyph = line.getGlyph();

                    if (glyph == null) {
                        logger.warn("glyph is null for line " + line + " staff:" + staff);
                    } else if (glyph.getRunTable() == null) {
                        logger.warn("glyph runtable is null");
                    }

                    glyph.getRunTable().render(g, glyph.getTopLeft());
                }
            }
        }

        g.dispose();

        return new ByteProcessor(img);
    }

    //------------//
    // checkImage //
    //------------//
    private BufferedImage checkImage (BufferedImage img)
            throws ImageFormatException
    {
        // Check image format
        img = adjustImageFormat(img);

        // Check pixel size and compute grayFactor accordingly
        ColorModel colorModel = img.getColorModel();
        int pixelSize = colorModel.getPixelSize();
        logger.debug("colorModel={} pixelSize={}", colorModel, pixelSize);

        //        if (pixelSize == 1) {
        //            grayFactor = 1;
        //        } else if (pixelSize <= 8) {
        //            grayFactor = (int) Math.rint(128 / Math.pow(2, pixelSize - 1));
        //        } else if (pixelSize <= 16) {
        //            grayFactor = (int) Math.rint(32768 / Math.pow(2, pixelSize - 1));
        //        } else {
        //            throw new RuntimeException("Unsupported pixel size: " + pixelSize);
        //        }
        //
        //        logger.debug("grayFactor={}", grayFactor);
        return img;
    }

    //--------------//
    // getStrongRef //
    //--------------//
    /**
     * Report the actual (strong) reference, if any, of a weak source reference.
     *
     * @param key the source key
     * @return the strong reference, if any
     */
    private ByteProcessor getStrongRef (SourceKey key)
    {
        // Check if key is referenced
        WeakReference<ByteProcessor> ref = sources.get(key);

        if (ref != null) {
            // Actual reference may be null or not (depending on garbage collection)
            return ref.get();
        }

        return null;
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
                "Should we print out the stop watch(es)?");

        private final Constant.Integer gaussianRadius = new Constant.Integer(
                "pixels",
                1,
                "Radius of Gaussian filtering kernel (1 for 3x3, 2 for 5x5, etc)");

        private final Constant.Integer medianRadius = new Constant.Integer(
                "pixels",
                1,
                "Radius of Median filtering kernel (1 for 3x3, 2 for 5x5)");

        private final Constant.String defaultExtractionDirectory = new Constant.String(
                WellKnowns.DEFAULT_SCRIPTS_FOLDER.toString(),
                "Default directory for image extractions");
    }
}
