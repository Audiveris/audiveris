//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P i c t u r e                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.image.AdaptiveDescriptor;
import org.audiveris.omr.image.FilterDescriptor;
import org.audiveris.omr.image.GaussianGrayFilter;
import org.audiveris.omr.image.ImageFormatException;
import org.audiveris.omr.image.ImageUtil;
import org.audiveris.omr.image.MedianGrayFilter;
import org.audiveris.omr.image.PixelFilter;
import org.audiveris.omr.image.PixelSource;
import static org.audiveris.omr.image.PixelSource.BACKGROUND;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.PixelEvent;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bushe.swing.event.EventSubscriber;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>Picture</code> starts from the original BufferedImage to provide all {@link
 * PixelSource} instances derived from it.
 * <p>
 * The <code>Picture</code> constructor takes a provided original image, whatever its format and
 * color model, and converts it if necessary to come up with a usable gray-level PixelSource:
 * the GRAY source.
 * <p>
 * Besides the GRAY source, this class handles a collection of sources, all of the same
 * dimension, with the ability to retrieve them on demand or dispose them, via {@link #getSource}
 * and {@link #disposeSource} methods.
 * <p>
 * Any instance of this class is registered on the related Sheet location service, so that each time
 * a location event is received, the corresponding pixel gray value of the GRAY sources is
 * published.
 * <p>
 * TODO: When an alpha channel is involved, perform the alpha multiplication if the components are
 * not yet pre-multiplied.
 * <p>
 * <img src="doc-files/ImageTransforms.png" alt="Image Transforms UML">
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

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** Image width. */
    @XmlAttribute(name = "width")
    private int width;

    /** Image height. */
    @XmlAttribute(name = "height")
    private int height;

    /**
     * <b>Deprecated</b> Old map of all handled run tables.
     * <p>
     * Replaced by standard images.
     */
    @Deprecated
    @XmlElementWrapper(name = "tables")
    private EnumMap<TableKey, RunTableHolder> oldTables = new EnumMap<>(TableKey.class);

    /** Map of all handled images. */
    @XmlElementWrapper(name = "images")
    private final EnumMap<ImageKey, ImageHolder> images = new EnumMap<>(ImageKey.class);

    // Transient data
    //---------------

    /** Map of all handled run tables. */
    private final ConcurrentSkipListMap<TableKey, WeakReference<RunTable>> tables =
            new ConcurrentSkipListMap<>();

    /** Map of all handled sources. */
    private final ConcurrentSkipListMap<SourceKey, WeakReference<ByteProcessor>> sources =
            new ConcurrentSkipListMap<>();

    /** Related sheet. */
    @Navigable(false)
    private Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor needed for JAXB.
     */
    private Picture ()
    {
        sheet = null;
        width = 0;
        height = 0;
        logger.debug("Picture unmarshalled by JAXB");
    }

    /**
     * Build a picture instance from a given original image.
     *
     * @param sheet        the related sheet
     * @param image        the provided original image (perhaps color image)
     * @param adjustFormat if true, check and adjust image format
     * @throws ImageFormatException if the image format is unsupported
     */
    public Picture (Sheet sheet,
                    BufferedImage image,
                    boolean adjustFormat)
            throws ImageFormatException
    {
        initTransients(sheet);

        if (adjustFormat) {
            // Make sure format, colors, etc are OK for us
            image = adjustImageFormat(image);
        }

        width = image.getWidth();
        height = image.getHeight();

        // Remember the gray image
        setImage(ImageKey.GRAY, image, true);

        logger.debug("Picture with gray image {}", image);
    }

    /**
     * Build a picture instance from a binary table.
     *
     * @param sheet       the related sheet
     * @param binaryTable the provided binary table
     */
    public Picture (Sheet sheet,
                    RunTable binaryTable)
    {
        initTransients(sheet);

        width = binaryTable.getWidth();
        height = binaryTable.getHeight();

        setTable(TableKey.BINARY, binaryTable, true); // This sets also binary image

        logger.debug("Picture with BinaryTable {}", binaryTable);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // binarized //
    //-----------//
    private ByteProcessor binarized (ByteProcessor src)
    {
        FilterDescriptor desc = AdaptiveDescriptor.getDefault();
        logger.info("{} {}", "Binarization", desc);

        PixelFilter filter = desc.getFilter(src);

        return filter.filteredImage();
    }

    //-----------------//
    // buildGraySource //
    //-----------------//
    /**
     * Build the initial gray source from gray image.
     *
     * @param img the initial gray image
     * @return the initial gray source
     */
    private ByteProcessor buildGraySource (BufferedImage img)
    {
        if (img != null) {
            if (img.getType() != BufferedImage.TYPE_BYTE_GRAY) {
                final StopWatch watch = new StopWatch("ToGray");
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

    //--------------------//
    // buildNoStaffBuffer //
    //--------------------//
    /**
     * Build a no-staff buffer by painting each staff line glyph in white on the binary image.
     *
     * @return the no-staff buffer
     */
    private ByteProcessor buildNoStaffBuffer ()
    {
        final ByteProcessor src = getSource(SourceKey.BINARY);
        final BufferedImage img = src.getBufferedImage();
        final Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);

        for (SystemInfo system : sheet.getSystems()) {
            for (Staff staff : system.getStaves()) {
                for (LineInfo li : staff.getLines()) {
                    final StaffLine line = (StaffLine) li;
                    final Glyph glyph = line.getGlyph();

                    if (glyph == null) {
                        final int idx = staff.getLines().indexOf(line);
                        logger.warn("Staff #{} no glyph for line #{}", staff.getId(), 1 + idx);
                    } else {
                        glyph.getRunTable().render(g, glyph.getTopLeft());
                    }
                }
            }
        }

        g.dispose();

        return new ByteProcessor(img);
    }

    //-------------------//
    // buildNoStaffTable //
    //-------------------//
    /**
     * Build the vertical table without staff lines pixels.
     *
     * @return the no-staff vertical table
     */
    public RunTable buildNoStaffTable ()
    {
        final ByteProcessor source = getSource(SourceKey.NO_STAFF);

        if (source == null) {
            return null;
        }

        return new RunTableFactory(VERTICAL).createTable(source);
    }

    //---------------------------//
    // buildStaffLineGlyphsImage //
    //---------------------------//
    /**
     * Build an image containing just the removed staff lines.
     *
     * @return image of staff lines
     */
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

    /**
     * For debug only.
     */
    public void checkSources ()
    {
        for (SourceKey key : SourceKey.values()) {
            logger.info(String.format("%15s ref:%s", key, getStrongRef(key)));
        }
    }

    //-------//
    // clamp //
    //-------//
    /**
     * Return the intersection of the provided rectangle with the picture rectangle.
     *
     * @param rect the provided rectangle
     * @return the clamped rectangle
     */
    public Rectangle clamp (Rectangle rect)
    {
        return new Rectangle(0, 0, width, height).intersection(rect);
    }

    //------------------//
    // convertOldTables //
    //------------------//
    /**
     * Migrate from tables as .xml files to images as .png files.
     */
    private void convertOldTables ()
    {
        if (oldTables != null) {
            for (Entry<TableKey, RunTableHolder> entry : oldTables.entrySet()) {
                final RunTableHolder holder = entry.getValue();
                final RunTable table = holder.getData(sheet.getStub());
                setTable(entry.getKey(), table, true); // Sets related image as well
                sheet.getStub().setUpgraded(true);
            }

            oldTables = null;
        }
    }

    //--------------//
    // discardImage //
    //--------------//
    /**
     * Flag image to be discarded from disk when sheet is stored.
     *
     * @param key image key
     */
    public void discardImage (ImageKey key)
    {
        ImageHolder holder = images.get(key);

        if (holder != null) {
            holder.discard();
        }
    }

    //---------------//
    // disposeSource //
    //---------------//
    /**
     * Dispose of the source related to the provided key.
     *
     * @param key provided key
     */
    public void disposeSource (SourceKey key)
    {
        sources.remove(key);
    }

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

                if (pix == BACKGROUND) { // White background
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
    /**
     * Apply a Gaussian filter on the provided source.
     *
     * @param src provided source
     * @return filtered buffer
     */
    public ByteProcessor gaussianFiltered (ByteProcessor src)
    {
        final StopWatch watch = new StopWatch("Gaussian");

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

    //--------------//
    // getGrayImage //
    //--------------//
    /**
     * Report the initial gray image.
     *
     * @return the gray image, perhaps null
     */
    public BufferedImage getGrayImage ()
    {
        BufferedImage gray = getImage(ImageKey.GRAY);

        if (gray == null) {
            try {
                // Try to reload image from sheet input information
                gray = sheet.getStub().loadGrayImage();

                if (gray == null) {
                    return null;
                }

                gray = adjustImageFormat(gray);
                setImage(ImageKey.GRAY, gray, false);
            } catch (ImageFormatException ex) {
                logger.warn("ImageFormatException thrown in getGrayImage", ex);

                return null;
            }
        }

        return gray;
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
     * Report the desired image.
     *
     * @param key key of desired image
     * @return the image found, if any, null otherwise
     */
    public BufferedImage getImage (ImageKey key)
    {
        ImageHolder holder = images.get(key);

        if (holder == null) {
            return null;
        }

        return holder.getData(sheet.getStub());
    }

    //-------------------//
    // getImageRectangle //
    //-------------------//
    /**
     * Report the sheet image, or a sub-image of it if rectangle area is specified.
     * <p>
     * We use the initial gray image if it is still available.
     * Otherwise we use the binary image.
     *
     * @param rect rectangular area desired, null for whole image
     * @return the (sub) image
     */
    public BufferedImage getImageRectangle (Rectangle rect)
    {
        BufferedImage img = getGrayImage();

        if (img == null) {
            img = getImage(ImageKey.BINARY);

            if (img == null) {
                // Kept for backward compatibility
                ByteProcessor buffer = getSource(SourceKey.BINARY);
                img = buffer.getBufferedImage();
            }
        }

        if (rect == null) {
            return img;
        } else {
            return img.getSubimage(rect.x, rect.y, rect.width, rect.height);
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
     * <p>
     * If the source is not yet cached, build the source and store it in cache via weak reference.
     * Users are encouraged to check if the gray source exists (!= null) before using it.
     * <p>
     * An .OMR file which contains an invalid path to the source file may cause the gray source
     * to be null.
     *
     * @param key the key of desired source
     * @return the source ready to use, or null if not available
     */
    public ByteProcessor getSource (SourceKey key)
    {
        ByteProcessor src = getStrongRef(key);

        if (src == null) {
            switch (key) {
                case GRAY -> src = buildGraySource(getGrayImage());

                case BINARY -> {
                    // Built from binary image, if available
                    BufferedImage image = getImage(ImageKey.BINARY);

                    if (image != null) {
                        src = new ByteProcessor(image);
                    } else {
                        // Otherwise, built via binarization of initial gray source if any
                        final ByteProcessor gray = getSource(SourceKey.GRAY);

                        if (gray != null) {
                            src = binarized(gray);

                            // Register binary image for possible future use
                            image = src.getBufferedImage();
                            setImage(ImageKey.BINARY, image, true);
                            sheet.getStub().setModified(true);
                        } else {
                            logger.warn("Cannot provide BINARY source");

                            return null;
                        }
                    }
                }

                case GAUSSIAN -> // Built from median
                        src = gaussianFiltered(getSource(SourceKey.MEDIAN));

                case MEDIAN -> // Built from no_staff
                        src = medianFiltered(getSource(SourceKey.NO_STAFF));

                case NO_STAFF -> // Built by erasing StaffLines glyphs from binary source
                        src = buildNoStaffBuffer();
            }

            if (src != null) {
                // Store in cache
                sources.put(key, new WeakReference<>(src));
                logger.debug("{} source built as {}", key, src);
            }
        }

        return src;
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

    //--------------//
    // getStrongRef //
    //--------------//
    /**
     * Report the actual (strong) reference, if any, of a weak table reference.
     *
     * @param key the table key
     * @return the strong reference, if any
     */
    private RunTable getStrongRef (TableKey key)
    {
        // Check if key is referenced
        WeakReference<RunTable> ref = tables.get(key);

        if (ref != null) {
            // Actual reference may be null or not (depending on garbage collection)
            return ref.get();
        }

        return null;
    }

    //------------------//
    // getVerticalTable //
    //------------------//
    /**
     * Report the desired vertical table.
     *
     * @param key key of desired table
     * @return the vertical table found, if any, null otherwise
     */
    public RunTable getVerticalTable (TableKey key)
    {
        RunTable tbl = getStrongRef(key);

        if (tbl == null) {
            switch (key) {
                case BINARY -> tbl = verticalTableOf(ImageKey.BINARY);
                case HEAD_SPOTS -> tbl = verticalTableOf(ImageKey.HEAD_SPOTS);
            }

            if (tbl != null) {
                // Store in cache
                tables.put(key, new WeakReference<>(tbl));
                logger.debug("{} table built as {}", key, tbl);
            }
        }

        return tbl;
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
    // hasImage //
    //----------//
    /**
     * Report whether the desired image is known
     *
     * @param key key of desired image
     * @return true if we have an ImageHolder, false otherwise
     */
    public boolean hasImage (ImageKey key)
    {
        return images.get(key) != null;
    }

    //---------------//
    // hasImageReady //
    //---------------//
    /**
     * Report whether the desired image is known and loaded.
     *
     * @param key key of desired image
     * @return true if we have an ImageHolder with loaded data
     */
    public boolean hasImageReady (ImageKey key)
    {
        ImageHolder holder = images.get(key);

        if (holder == null) {
            return false;
        }

        return holder.hasDataReady();
    }

    //----------------//
    // hasNoGrayImage //
    //----------------//
    /**
     * Tell whether no initial gray image still exists, even on disk.
     *
     * @return true if no initial image still exists
     */
    public boolean hasNoGrayImage ()
    {
        final ImageHolder holder = images.get(ImageKey.GRAY);

        return ((holder == null) || holder.hasNoData);
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

        // Convert oldTables to images
        convertOldTables();
    }

    //----------------//
    // medianFiltered //
    //----------------//
    /**
     * Apply a median filter on the provided source buffer.
     *
     * @param src provided source
     * @return filtered buffer
     */
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
     * <p>
     * Only if image is available in memory, we read the GRAY pixel level value at sheet location
     * and forward it to whoever is subscribed to this information.
     *
     * @param event the (sheet) location event
     */
    @Override
    public void onEvent (LocationEvent event)
    {
        final ImageHolder grayHolder = images.get(ImageKey.GRAY);

        if ((grayHolder == null) || !grayHolder.hasDataReady()) {
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
                    ByteProcessor src = getSource(SourceKey.GRAY);

                    if (src != null) {
                        level = src.get(pt.x, pt.y);
                    }
                }
            }

            sheet.getLocationService().publish(
                    new PixelEvent(this, event.hint, event.movement, level));
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //-------------//
    // removeImage //
    //-------------//
    /**
     * Remove an image.
     *
     * @param key image key
     */
    public void removeImage (ImageKey key)
    {
        images.remove(key);
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

    //----------//
    // setImage //
    //----------//
    /**
     * Register an image.
     *
     * @param key      image key
     * @param image    image to register
     * @param modified true if not saved on disk
     */
    public final void setImage (ImageKey key,
                                BufferedImage image,
                                boolean modified)
    {
        final ImageHolder imageHolder = new ImageHolder(key);
        imageHolder.setData(image, modified);

        // Reloading an image may result in a slightly different dimension
        // The case was detected when switching from JPodRenderer loader to PdfBox loader
        if (image.getWidth() != width || image.getHeight() != height) {
            logger.info(
                    "New {} image size: {}x{} vs {}x{}",
                    key,
                    image.getWidth(),
                    image.getHeight(),
                    width,
                    height);

            // Discard all sources and tables
            images.clear();
            tables.clear();

            // Use the new dimension
            width = image.getWidth();
            height = image.getHeight();
        }

        images.put(key, imageHolder);
    }

    //----------//
    // setTable //
    //----------//
    /**
     * Register a table (and its related image).
     *
     * @param key      table key
     * @param table    table to register
     * @param modified true if not saved on disk
     */
    public final void setTable (TableKey key,
                                RunTable table,
                                boolean modified)
    {
        if (table != null) {
            tables.put(key, new WeakReference<>(table));
            setImage(key.toImageKey(), table.getBufferedImage(), modified);
        }
    }

    //-------//
    // store //
    //-------//
    /**
     * Store the picture images.
     * <p>
     * If we have the gray image while related switch is off, we remove this image.
     * <p>
     * Tables are no longer stored on disk, but their related images are.
     *
     * @param sheetFolder    target sheet folder
     * @param oldSheetFolder optional source sheet folder (or null)
     */
    public void store (Path sheetFolder,
                       Path oldSheetFolder)
    {
        // Each handled image
        for (Iterator<Entry<ImageKey, ImageHolder>> it = images.entrySet().iterator(); it
                .hasNext();) {
            final Entry<ImageKey, ImageHolder> entry = it.next();
            final ImageKey iKey = entry.getKey();
            final ImageHolder holder = entry.getValue();

            if (holder.isDiscarded()) {
                holder.removeData(sheetFolder);
            } else {
                boolean ok = holder.storeData(sheetFolder, oldSheetFolder);

                if (ok) {
                    // Delete corresponding old table if any
                    final TableKey tKey = iKey.toTableKey();

                    if (tKey != null) {
                        final Path tablePath = sheetFolder.resolve(tKey + ".xml");

                        try {
                            if (Files.deleteIfExists(tablePath)) {
                                logger.info("Washed {}", tablePath);
                            }
                        } catch (IOException ex) {
                            logger.warn("Error deleting {} {}", tablePath, ex);
                        }
                    }
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

    //-----------------//
    // verticalTableOf //
    //----------------//
    /**
     * Build a vertical table from the provided image.
     *
     * @param key key to image
     * @return the table built, or null if image could not be found
     */
    private RunTable verticalTableOf (ImageKey key)
    {
        final ImageHolder imageHolder = images.get(key);

        if ((imageHolder != null) && !imageHolder.hasNoData()) {
            final BufferedImage image = imageHolder.getData(sheet.getStub());

            if (image != null) {
                return verticalTableOf(image);
            }
        }

        return null;
    }

    //--------//
    // xClamp //
    //--------//
    /**
     * Clamp the provided abscissa value within legal values that are precisely [0..width -1].
     *
     * @param x the abscissa to clamp
     * @return the clamped abscissa
     */
    public int xClamp (int x)
    {
        if (x < 0) {
            return 0;
        }

        if (x > (width - 1)) {
            return width - 1;
        }

        return x;
    }

    //--------//
    // yClamp //
    //--------//
    /**
     * Clamp the provided ordinate value within legal values that are precisely [0..height -1].
     *
     * @param y the ordinate to clamp
     * @return the clamped abscissa
     */
    public int yClamp (int y)
    {
        if (y < 0) {
            return 0;
        }

        if (y > (height - 1)) {
            return height - 1;
        }

        return y;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------------//
    // adjustImageFormat //
    //-------------------//
    /**
     * Check if the image format (and especially its color model) is
     * properly handled by Audiveris and adjust if needed.
     *
     * @param img image to be checked
     * @return valid image
     * @throws ImageFormatException is the format is not supported
     */
    public static BufferedImage adjustImageFormat (BufferedImage img)
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
            return ImageUtil.grayAlphaToGray(img);
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

    //-----------------//
    // verticalTableOf //
    //-----------------//
    /**
     * Report the vertical RunTable built from the provided binary image.
     *
     * @param binaryImg provided binary image
     * @return the binary RunTable
     */
    public static RunTable verticalTableOf (BufferedImage binaryImg)
    {
        return new RunTableFactory(VERTICAL).createTable(new ByteProcessor(binaryImg));
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Integer gaussianRadius = new Constant.Integer(
                "pixels",
                1,
                "Radius of Gaussian filtering kernel (1 for 3x3, 2 for 5x5, etc)");

        private final Constant.Integer medianRadius = new Constant.Integer(
                "pixels",
                1,
                "Radius of Median filtering kernel (1 for 3x3, 2 for 5x5)");
    }

    //~ Enumerations -------------------------------------------------------------------------------

    /**
     * The set of handled images ({@link BufferedImage} instances).
     */
    public static enum ImageKey
    {
        /** The initial gray-level source. */
        GRAY,
        /** The binarized (black and white) source. */
        BINARY,
        /** Temporary image of head spots. */
        HEAD_SPOTS;

        public TableKey toTableKey ()
        {
            if (this == GRAY) {
                return null;
            }

            return TableKey.valueOf(name());
        }
    }

    /**
     * The set of handled sources ({@link ByteProcessor} instances).
     */
    public static enum SourceKey
    {
        /** The initial gray-level source. */
        GRAY,
        /** The binarized (black and white) source. */
        BINARY,
        /** The Gaussian-filtered source. */
        GAUSSIAN,
        /** The Median-filtered source. */
        MEDIAN,
        /** The source with staff lines removed. */
        NO_STAFF;
    }

    /**
     * The set of handled tables ({@link RunTable} instances).
     */
    public static enum TableKey
    {
        BINARY,
        HEAD_SPOTS;

        public ImageKey toImageKey ()
        {
            return ImageKey.valueOf(name());
        }
    }
}
