//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S a m p l e R e p o s i t o r y                                //
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
package omr.classifier;

import omr.OMR;
import omr.WellKnowns;

import omr.classifier.SheetContainer.Descriptor;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.SymbolSample;

import omr.run.RunTable;

import omr.sheet.Book;
import omr.sheet.Picture;
import omr.sheet.Sheet;
import omr.sheet.Staff;

import omr.ui.OmrGui;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;

import omr.util.FileUtil;
import omr.util.StopWatch;
import omr.util.Zip;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code SampleRepository} handles the store of {@link Sample} instances,
 * across multiple sheets and possibly multiple OMR sessions.
 * <p>
 * The repository is implemented as a collection of {@link SampleSheet} instances, to ease the
 * addition or removal of sheet samples as a whole.
 * <p>
 * A special kind of samples is provided by the use of a musical font with proper scaling. These
 * font-based samples, though being artificial, are considered as part of the training material.
 * There is exactly one font-base sample for every trainable shape, and this sample is always
 * shown in first position among all samples of the same shape.
 * All these font-based samples are gathered in the virtual {@link #SYMBOLS} container.
 * <br>
 * TODO: Provide support for symbols based on additional music fonts, such as Bravura.
 * <p>
 * <img alt="Sample management" src="doc-files/Samples.png">
 *
 *
 * @author Hervé Bitteur
 */
public class SampleRepository
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SampleRepository.class);

    /** Standard interline value: {@value}. (Any value could fit, if used consistently) */
    public static final int STANDARD_INTERLINE = 20;

    /** The single instance of this class. */
    private static volatile SampleRepository INSTANCE;

    /** File name for images material: {@value}. */
    private static final String IMAGES_FILE_NAME = "images.zip";

    /** File name for samples material: {@value}. */
    private static final String SAMPLES_FILE_NAME = "samples.zip";

    /** File path for images material: {@value}. */
    private static final Path IMAGES_FILE = WellKnowns.TRAIN_FOLDER.resolve(IMAGES_FILE_NAME);

    /** File path for training material: {@value}. */
    private static final Path SAMPLES_FILE = WellKnowns.TRAIN_FOLDER.resolve(SAMPLES_FILE_NAME);

    /** Special name to refer to font-based samples: {@value}. */
    private static final String SYMBOLS = "<Font-Based Symbols>";

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sheets, mapped by their ID. */
    private final Map<Integer, SampleSheet> idMap = new TreeMap<Integer, SampleSheet>();

    /** Sheets, mapped by their image. */
    private final Map<RunTable, SampleSheet> imageMap = new HashMap<RunTable, SampleSheet>();

    /** Sheets, mapped by their samples. */
    private final Map<Sample, SampleSheet> sampleMap = new HashMap<Sample, SampleSheet>();

    /** Container for sheet descriptors. */
    private SheetContainer sheetContainer = new SheetContainer();

    /** Is the repository already loaded?. */
    private volatile boolean loaded;

    /** Listeners on repository modifications. */
    private final Set<ChangeListener> listeners = new LinkedHashSet<ChangeListener>();

    //~ Constructors -------------------------------------------------------------------------------
    /** Private singleton constructor. */
    private SampleRepository ()
    {
        // Set application exit listener
        if (OMR.gui != null) {
            OmrGui.getApplication().addExitListener(getExitListener());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // getExitListener //
    //-----------------//
    public final Application.ExitListener getExitListener ()
    {
        return new RepositoryExitListener();
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class, after creating it if needed.
     *
     * @return the single instance
     */
    public static SampleRepository getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new SampleRepository();
        }

        return INSTANCE;
    }

    //-------------//
    // addListener //
    //-------------//
    /**
     * Register a listener on repository updates.
     *
     * @param listener the listener to be kept informed
     */
    public void addListener (ChangeListener listener)
    {
        Objects.requireNonNull(listener, "Cannot add a null listener");
        listeners.add(listener);
    }

    //-----------//
    // addSample //
    //-----------//
    /**
     * Add a new sample to the provided SampleSheet.
     *
     * @param sample      the sample to add, non-null
     * @param sampleSheet the containing sample sheet
     * @see #removeSample(Sample)
     */
    public void addSample (Sample sample,
                           SampleSheet sampleSheet)
    {
        Objects.requireNonNull(sampleSheet, "Cannot add a sample to a null sample sheet");

        sampleSheet.privateAddSample(sample);
        sampleMap.put(sample, sampleSheet);

        fireStateChanged(new AdditionEvent(sample));
    }

    //-----------//
    // addSample //
    //-----------//
    public void addSample (Shape shape,
                           Glyph glyph,
                           Staff staff,
                           Sheet sheet)
    {
        final SampleSheet sampleSheet = SampleRepository.this.findSampleSheet(sheet);
        addSample(shape, glyph, staff.getSpecificInterline(), sampleSheet);
    }

    //-----------//
    // addSample //
    //-----------//
    public void addSample (Shape shape,
                           Glyph glyph,
                           int interline,
                           SampleSheet sampleSheet)
    {
        shape = Sample.getRecordableShape(shape);

        final Sample sample = new Sample(glyph, interline, shape);
        addSample(sample, sampleSheet);
    }

    //-----------------//
    // checkAllSamples //
    //-----------------//
    /**
     * Run checks on all samples to detect samples with identical run table
     * (while having identical interline value).
     * <p>
     * WRONG: Having two samples that share the same run table, but are assigned different shapes,
     * would seriously impact classifier training and must be fixed <b>manually</b>.
     * TODO: provide help to address these cases.
     * <p>
     * REDUNDANT: Even if they are assigned the same shape, only one of these samples should be kept
     * for optimal training, the others are reported via the returned purge list.
     *
     * @return the collection of samples to purge (REDUNDANT ones, not the WRONG ones)
     */
    public List<Sample> checkAllSamples ()
    {
        List<Sample> allSamples = getAllSamples();

        // Sort by weight, then by sheet ID
        Collections.sort(
                allSamples,
                new Comparator<Sample>()
        {
            @Override
            public int compare (Sample s1,
                                Sample s2)
            {
                int comp = Integer.compare(s1.getWeight(), s2.getWeight());

                if (comp != 0) {
                    return comp;
                }

                return Integer.compare(getSheetId(s1), getSheetId(s2));
            }
        });

        List<Sample> purged = new ArrayList<Sample>();
        int n = allSamples.size();
        logger.debug("Checking {} samples...", n);

        boolean[] deleted = new boolean[n];

        for (int i = 0; i < n; i++) {
            if (deleted[i]) {
                continue;
            }

            final Sample sample = allSamples.get(i);
            final int weight = sample.getWeight();
            final RunTable runTable = sample.getRunTable();
            final int interline = sample.getInterline();

            for (int j = i + 1; j < n; j++) {
                if (deleted[j]) {
                    continue;
                }

                Sample s = allSamples.get(j);

                if (s.getWeight() != weight) {
                    break;
                }

                if ((s.getInterline() == interline) && s.getRunTable().equals(runTable)) {
                    if (s.getShape() != sample.getShape()) {
                        logger.warn(
                                "Conflicting shapes between {}/{} and {}/{}",
                                getSheetId(sample),
                                sample,
                                getSheetId(s),
                                s);
                    } else {
                        logger.info(
                                "Same runtable for {}/{} & {}/{}",
                                getSheetId(sample),
                                sample,
                                getSheetId(s),
                                s);
                        purged.add(s);
                        deleted[j] = true;
                    }
                }
            }
        }

        if (!purged.isEmpty()) {
            logger.info("To be purged: {} / {}", purged.size(), allSamples.size());
        }

        return purged;
    }

    //--------------//
    // checkForSave //
    //--------------//
    /**
     * Check whether the repository has been modified and save it if so.
     */
    public void checkForSave ()
    {
        if (isLoaded() && isModified()) {
            storeRepository();
            logger.info("Sample repository saved.");
        } else {
            logger.info("No need to save sample repository");
        }
    }

    //-----------------//
    // findSampleSheet //
    //-----------------//
    public SampleSheet findSampleSheet (Sheet sheet)
    {
        // Handle long name if any
        final Book book = sheet.getStub().getBook();
        String longSheetName = null;

        if (book.getAlias() != null) {
            longSheetName = FileUtil.getNameSansExtension(book.getInputPath());

            if (book.isMultiSheet()) {
                longSheetName = longSheetName + "#" + sheet.getStub().getNumber();
            }
        }

        return findSampleSheet(
                sheet.getId(),
                longSheetName,
                sheet.getPicture().getTable(Picture.TableKey.BINARY));
    }

    //-----------------//
    // findSampleSheet //
    //-----------------//
    /**
     * Find out (or create) the SampleSheet that corresponds to provided name and/or
     * image.
     * <p>
     * If sheet image is provided, the repository is searched for the image.
     * <p>
     * If an identical sheet image already exists, it is used and the provided name is kept as an
     * alias.
     * Otherwise, a new sample sheet is created.
     * <p>
     * If a sheet name is provided but no sheet image, we allocate a sheet with the provided name.
     * Note that this is not reliable.
     *
     * @param name     name of containing sheet, non-null if image is null
     * @param longName an optional longer name, if any
     * @param image    sheet binary image, if any, strongly recommended
     * @return the found or created sheet, where samples can be added to. Non null.
     */
    public SampleSheet findSampleSheet (String name,
                                        String longName,
                                        RunTable image)
    {
        if ((name == null) || ((name.isEmpty()) && (image == null))) {
            throw new IllegalArgumentException("findSheet() needs sheet name or image");
        }

        SampleSheet sheet;

        if (image != null) {
            final int hash = image.persistentHashCode();
            sheet = imageMap.get(image);

            if (sheet == null) {
                // Is there a not yet loaded table?
                List<Descriptor> descs = sheetContainer.getDescriptors(hash);

                if (!descs.isEmpty()) {
                    try {
                        final Path root = Zip.openFileSystem(IMAGES_FILE);

                        for (Descriptor desc : descs) {
                            final Path file = root.resolve(Integer.toString(desc.id)).resolve(
                                    SampleSheet.IMAGE_FILE_NAME);
                            final RunTable rt = RunTable.unmarshal(file);

                            if ((rt != null) && rt.equals(image)) {
                                // We have found the image
                                desc.addAlias(name);
                                desc.addAlias(longName);

                                sheet = idMap.get(desc.id);
                                sheet.setImage(rt);
                                imageMap.put(rt, sheet);

                                break;
                            }
                        }

                        root.getFileSystem().close();
                    } catch (IOException ex) {
                    }
                }

                if (sheet == null) {
                    // Allocate a brand new descriptor
                    int id = sheetContainer.getNewId();
                    Descriptor desc = new Descriptor(id, hash, name);
                    desc.addAlias(longName);

                    sheetContainer.addDescriptor(desc);

                    // Allocate a brand new sheet
                    sheet = new SampleSheet(id);
                    idMap.put(id, sheet);
                    imageMap.put(image, sheet);
                    sheet.setImage(image);
                }
            }
        } else {
            // We have no image, just a sheet name. This is dangerous!
            Descriptor desc = sheetContainer.getDescriptor(name);

            if (desc != null) {
                desc.addAlias(longName);

                return idMap.get(desc.id);
            } else {
                // Allocate a brand new descriptor
                int id = sheetContainer.getNewId();
                desc = new Descriptor(id, null, name);
                desc.addAlias(longName);
                sheetContainer.addDescriptor(desc);

                // Allocate a brand new sheet
                sheet = new SampleSheet(id);
                idMap.put(id, sheet);
            }
        }

        return sheet;
    }

    //-------------------//
    // getAllDescriptors //
    //-------------------//
    /**
     * Report all the descriptors in repository.
     *
     * @return all the sheets descriptors
     */
    public List<Descriptor> getAllDescriptors ()
    {
        return sheetContainer.getAllDescriptors();
    }

    //---------------//
    // getAllSamples //
    //---------------//
    /**
     * Report all the samples in the repository.
     *
     * @return all the repository samples
     */
    public List<Sample> getAllSamples ()
    {
        final List<Sample> allSamples = new ArrayList<Sample>();

        for (SampleSheet sheet : idMap.values()) {
            allSamples.addAll(sheet.getAllSamples());
        }

        return allSamples;
    }

    //----------------//
    // getSampleSheet //
    //----------------//
    /**
     * Report the SampleSheet that contains the provided sample.
     *
     * @param sample the provided sample
     * @return the containing sheet
     */
    public SampleSheet getSampleSheet (Sample sample)
    {
        return sampleMap.get(sample);
    }

    //------------//
    // getSamples //
    //------------//
    /**
     * Report, in the SampleSheet whose ID is provided, all samples assigned the
     * desired shape.
     *
     * @param id    ID of sample sheet
     * @param shape desired shape
     * @return the list of samples related to shape in provided sheet
     */
    public List<Sample> getSamples (int id,
                                    Shape shape)
    {
        SampleSheet sampleSheet = idMap.get(id);

        if (sampleSheet != null) {
            return sampleSheet.getSamples(shape);
        }

        return Collections.emptyList();
    }

    //-----------//
    // getShapes //
    //-----------//
    /**
     * Report all shapes for which the provided sheet/descriptor has concrete samples.
     *
     * @param descriptor descriptor of the sample sheet
     * @return the list of (non-empty) shapes
     */
    public Set<Shape> getShapes (Descriptor descriptor)
    {
        // Symbols?
        if (SampleSheet.isSymbols(descriptor.id)) {
            return ShapeSet.allPhysicalShapes;
        }

        // Standard sheet
        SampleSheet sampleSheet = idMap.get(descriptor.id);

        if (sampleSheet != null) {
            return sampleSheet.getShapes();
        }

        return Collections.emptySet();
    }

    //--------------------//
    // getSheetDescriptor //
    //--------------------//
    /**
     * Report the descriptor of the sample sheet that contains the provided sample.
     *
     * @param sample provided sample
     * @return the descriptor of containing SampleSheet
     */
    public Descriptor getSheetDescriptor (Sample sample)
    {
        SampleSheet sampleSheet = sampleMap.get(sample);

        if (sampleSheet != null) {
            return sheetContainer.getDescriptor(sampleSheet.getId());
        }

        return null;
    }

    //------------//
    // getSheetId //
    //------------//
    /**
     * Report the ID of the sample sheet that contains the provided sample.
     *
     * @param sample provided sample
     * @return the containing SampleSheet ID
     */
    public Integer getSheetId (Sample sample)
    {
        SampleSheet sampleSheet = sampleMap.get(sample);

        if (sampleSheet != null) {
            return sampleSheet.getId();
        }

        return null;
    }

    //----------//
    // isLoaded //
    //----------//
    /**
     * @return the loaded
     */
    public boolean isLoaded ()
    {
        return loaded;
    }

    //------------//
    // isModified //
    //------------//
    public boolean isModified ()
    {
        if (false) {
            // Force writing of everything
            sheetContainer.setModified(true);

            for (SampleSheet sheet : idMap.values()) {
                sheet.setModified(true);
            }
        }

        if (sheetContainer.isModified()) {
            return true;
        }

        for (SampleSheet sheet : idMap.values()) {
            if (sheet.isModified()) {
                return true;
            }
        }

        return false;
    }

    //----------------//
    // loadRepository //
    //----------------//
    /**
     * Load the training material (font-based symbols as well as concrete samples).
     *
     * @param withBinaries if true, sheet binaries are also loaded.
     */
    public void loadRepository (boolean withBinaries)
    {
        final StopWatch watch = new StopWatch("Loading repository");

        try {
            watch.start("open samples.zip");

            final Path samplesRoot = Zip.openFileSystem(SAMPLES_FILE);

            watch.start("loadContainer");

            {
                SheetContainer container = SheetContainer.unmarshal(samplesRoot);

                if (container != null) {
                    if (logger.isDebugEnabled()) {
                        container.dump();
                    }

                    sheetContainer = container;
                }
            }

            //            watch.start("buildSymbols");
            //            buildSymbols();
            //
            watch.start("loadSamples");
            loadSamples(samplesRoot);

            if (withBinaries && Files.exists(IMAGES_FILE)) {
                watch.start("open images.zip");

                final Path imagesRoot = Zip.openFileSystem(IMAGES_FILE);

                watch.start("loadImages");
                loadImages(imagesRoot);
                imagesRoot.getFileSystem().close();
            }

            samplesRoot.getFileSystem().close();
            loaded = true;
        } catch (IOException ex) {
            logger.warn("Error loading repository " + ex, ex);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //----------------//
    // removeListener //
    //----------------//
    public boolean removeListener (ChangeListener listener)
    {
        return listeners.remove(listener);
    }

    //--------------//
    // removeSample //
    //--------------//
    /**
     * Remove the provided sample from the repository.
     *
     * @param sample the sample to remove
     * @see #addSample(Sample, SampleSheet)
     */
    public void removeSample (Sample sample)
    {
        SampleSheet sampleSheet = getSampleSheet(sample);
        sampleSheet.privateRemoveSample(sample);
        sampleMap.remove(sample);

        fireStateChanged(new RemovalEvent(sample));
    }

    //-----------------//
    // storeRepository //
    //-----------------//
    /**
     * Store the (modified parts of) repository to disk.
     */
    public void storeRepository ()
    {
        try {
            final Path samplesRoot = Files.exists(SAMPLES_FILE) ? Zip.openFileSystem(SAMPLES_FILE)
                    : Zip.createFileSystem(SAMPLES_FILE);
            final Path imagesRoot = Files.exists(IMAGES_FILE) ? Zip.openFileSystem(IMAGES_FILE)
                    : Zip.createFileSystem(IMAGES_FILE);

            // Container
            if (sheetContainer.isModified()) {
                sheetContainer.marshal(samplesRoot);
            }

            // Samples
            for (SampleSheet sampleSheet : idMap.values()) {
                if (sampleSheet.isModified()) {
                    sampleSheet.marshal(samplesRoot, imagesRoot);
                }
            }

            samplesRoot.getFileSystem().close();
            imagesRoot.getFileSystem().close();
            setModified(false);
            logger.debug("storeRepository done");
        } catch (Throwable ex) {
            logger.warn("Error storing repository to " + SAMPLES_FILE + " " + ex, ex);
        }
    }

    //-------------------//
    // buildSymbolSample //
    //-------------------//
    /**
     * Build an artificial sample from a symbol descriptor, in order to
     * train a classifier even when we have no concrete sample.
     *
     * @param shape the symbol shape
     * @return the sample built, or null if failed
     */
    private Sample buildSymbolSample (Shape shape)
    {
        Sample sample = null;

        // Make sure we have the drawing available for this shape
        ShapeSymbol symbol = Symbols.getSymbol(shape);

        // If no plain symbol, use the decorated symbol as plan B
        if (symbol == null) {
            symbol = Symbols.getSymbol(shape, true);
        }

        if (symbol != null) {
            sample = SymbolSample.create(shape, symbol, STANDARD_INTERLINE);
            sample.setSymbol(true);
        } else {
            logger.warn("No symbol for {}", shape);
        }

        return sample;
    }

    //--------------//
    // buildSymbols //
    //--------------//
    /**
     * Build all the artificial symbols for a given font.
     * <p>
     * TODO: support additional fonts.
     */
    private void buildSymbols ()
    {
        int id = -1;
        Descriptor desc = sheetContainer.getDescriptor(id);

        if (desc == null) {
            desc = new Descriptor(id, null, SYMBOLS);
            sheetContainer.addDescriptor(desc);
        }

        SampleSheet symbolSheet = new SampleSheet(id);

        for (Shape shape : ShapeSet.allPhysicalShapes) {
            Sample sample = buildSymbolSample(shape);
            symbolSheet.privateAddSample(sample);
            sampleMap.put(sample, symbolSheet);
        }

        idMap.put(id, symbolSheet);
    }

    //------------------//
    // fireStateChanged //
    //------------------//
    private void fireStateChanged (ChangeEvent event)
    {
        for (ChangeListener listener : listeners) {
            listener.stateChanged(event);
        }
    }

    //------------//
    // loadImages //
    //------------//
    /**
     * Unmarshal all the sheet images available in training material.
     */
    private void loadImages (final Path root)
    {
        try {
            Files.walkFileTree(
                    root,
                    new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile (Path file,
                                                  BasicFileAttributes attrs)
                        throws IOException
                {
                    final String fileName = file.getFileName().toString();

                    if (fileName.equals(SampleSheet.IMAGE_FILE_NAME)) {
                        RunTable runTable = RunTable.unmarshal(file);

                        if (runTable != null) {
                            Path parent = file.getParent();
                            String rel = root.relativize(parent).toString();

                            int id = Integer.decode(rel);
                            logger.debug("id: {}", id);

                            SampleSheet sampleSheet = idMap.get(id);

                            if (sampleSheet != null) {
                                sampleSheet.setImage(runTable);
                                logger.debug("Loaded {}", file);
                            } else {
                                logger.warn("No SampleSheet found for image {}", file);
                            }
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            logger.debug("loadBinaries done");
        } catch (Throwable ex) {
            logger.warn("Error loading binaries from " + IMAGES_FILE + " " + ex, ex);
        }
    }

    //-------------//
    // loadSamples //
    //-------------//
    /**
     * Unmarshal the repository concrete samples.
     */
    private void loadSamples (final Path root)
    {
        try {
            Files.walkFileTree(
                    root,
                    new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile (Path file,
                                                  BasicFileAttributes attrs)
                        throws IOException
                {
                    final String fileName = file.getFileName().toString();

                    if (fileName.equals(SampleSheet.SAMPLES_FILE_NAME)) {
                        final SampleSheet sampleSheet = SampleSheet.unmarshal(file);
                        final String rel = root.relativize(file.getParent()).toString();

                        final int id = Integer.decode(rel);
                        final Descriptor desc = sheetContainer.getDescriptor(id);

                        if (desc == null) {
                            logger.warn(
                                    "Samples entry {} not declared in {} is ignored.",
                                    id,
                                    SheetContainer.CONTAINER_ENTRY_NAME);
                        } else {
                            final boolean isSymbol = SampleSheet.isSymbols(id);
                            idMap.put(id, sampleSheet);

                            for (Sample sample : sampleSheet.getAllSamples()) {
                                sample.setSymbol(isSymbol);
                                sampleMap.put(sample, sampleSheet);
                            }
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            logger.debug("load done");
        } catch (Throwable ex) {
            logger.warn("Error loading " + SAMPLES_FILE + " " + ex, ex);
        }
    }

    //-------------//
    // setModified //
    //-------------//
    private void setModified (boolean bool)
    {
        sheetContainer.setModified(bool);

        for (SampleSheet sheet : idMap.values()) {
            sheet.setModified(bool);
        }
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //---------//
    // Monitor //
    //---------//
    /**
     * Interface {@code Monitor} defines the entries to a UI entity
     * which monitors the loading of samples by the sample repository.
     */
    public static interface Monitor
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * Called whenever a new sample has been loaded.
         *
         * @param sample the sample loaded
         */
        void loadedSample (Sample sample);

        /**
         * Called to pass the number of selected samples.
         *
         * @param selected the size of the selection
         */
        void setSelectedSamples (int selected);

        /**
         * Called to pass the total number of available samples in the training material.
         *
         * @param total the size of the training material
         */
        void setTotalSamples (int total);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------//
    // AdditionEvent //
    //---------------//
    /**
     * Event used to carry information about addition performed.
     */
    public class AdditionEvent
            extends ChangeEvent
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final Sample sample; // The added sample

        //~ Constructors ---------------------------------------------------------------------------
        public AdditionEvent (Sample sample)
        {
            super(SampleRepository.this);
            this.sample = sample;
        }
    }

    //--------------//
    // RemovalEvent //
    //--------------//
    /**
     * Event used to carry information about removal performed.
     */
    public class RemovalEvent
            extends ChangeEvent
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final Sample sample; // The removed sample

        //~ Constructors ---------------------------------------------------------------------------
        public RemovalEvent (Sample sample)
        {
            super(SampleRepository.this);
            this.sample = sample;
        }
    }

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
    }

    //------------------------//
    // RepositoryExitListener //
    //------------------------//
    /**
     * Listener called when application asks for exit and does exit.
     */
    private class RepositoryExitListener
            implements Application.ExitListener
    {
        //~ Constructors ---------------------------------------------------------------------------

        public RepositoryExitListener ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean canExit (EventObject eo)
        {
            // Check whether the repository has been saved (or user has declined)
            if (isModified()) {
                SingleFrameApplication appli = (SingleFrameApplication) Application.getInstance();
                int answer = JOptionPane.showConfirmDialog(
                        appli.getMainFrame(),
                        "Save sample repository?");

                if (answer == JOptionPane.YES_OPTION) {
                    storeRepository();

                    return true; // Here user has saved the repository
                }

                // True: user specifically chooses NOT to save the script
                // False: user says Oops!, cancelling the current close request
                return answer == JOptionPane.NO_OPTION;
            }

            return true;
        }

        @Override
        public void willExit (EventObject eo)
        {
        }
    }
}
