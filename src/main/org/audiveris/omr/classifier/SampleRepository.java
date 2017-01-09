//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S a m p l e R e p o s i t o r y                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.classifier;

import org.audiveris.omr.OMR;
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.classifier.SheetContainer.Descriptor;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.glyph.SymbolSample;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.Zip;

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
import java.util.Collection;
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
 * @author Hervé Bitteur
 */
public class SampleRepository
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SampleRepository.class);

    /** Should we support tribes?. */
    public static final boolean USE_TRIBES = constants.useTribes.isSet();

    /** Standard interline value: {@value}. (Any value could fit, if used consistently) */
    public static final int STANDARD_INTERLINE = 20;

    /** The global repository instance, if any. */
    private static volatile SampleRepository GLOBAL;

    /** File name for images material: {@value}. */
    private static final String IMAGES_FILE_NAME = "images.zip";

    /** File name for samples material: {@value}. */
    private static final String SAMPLES_FILE_NAME = "samples.zip";

    /** Special name to refer to font-based samples: {@value}. */
    private static final String SYMBOLS = "AAA_FONT_SYMBOLS";

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sheets, mapped by their unique name. */
    private final Map<String, SampleSheet> nameMap = new TreeMap<String, SampleSheet>();

    /** Sheets, mapped by their image. */
    private final Map<RunTable, SampleSheet> imageMap = new HashMap<RunTable, SampleSheet>();

    /** Sheets, mapped by their samples. */
    private final Map<Sample, SampleSheet> sampleMap = new HashMap<Sample, SampleSheet>();

    /** Container for sheet descriptors. */
    private SheetContainer sheetContainer = new SheetContainer();

    /** Is the repository already loaded?. */
    private boolean loaded;

    /** Have the images already been loaded?. */
    private boolean imagesLoaded;

    /** Listeners on repository modifications. */
    private final Set<ChangeListener> listeners = new LinkedHashSet<ChangeListener>();

    /** Folder path for this repository. */
    private final Path folder;

    /** File path for training material: {@value}. */
    private final Path samplesFile;

    /** File path for images material: {@value}. */
    private final Path imagesFile;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * (Private) constructor.
     *
     * @param folder hosting folder (either global train folder or book dedicated folder)
     */
    private SampleRepository (Path folder)
    {
        // Set application exit listener
        if (OMR.gui != null) {
            OmrGui.getApplication().addExitListener(getExitListener());
        }

        this.folder = folder;

        final String prefix = prefixOf(folder);
        imagesFile = folder.resolve(prefix + IMAGES_FILE_NAME);
        samplesFile = folder.resolve(prefix + SAMPLES_FILE_NAME);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // getExitListener //
    //-----------------//
    public final Application.ExitListener getExitListener ()
    {
        return new RepositoryExitListener();
    }

    //-------------------//
    // getGlobalInstance //
    //-------------------//
    /**
     * Report the global repository, after creating it if needed.
     *
     * @param load true for a loaded repository
     * @return the global instance of SampleRepository
     */
    public static SampleRepository getGlobalInstance (boolean load)
    {
        if (GLOBAL == null) {
            GLOBAL = new SampleRepository(WellKnowns.TRAIN_FOLDER);
        }

        if (load && !GLOBAL.isLoaded()) {
            GLOBAL.loadRepository();
        }

        return GLOBAL;
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the repository specifically related to the provided book.
     *
     * @param book the provided book
     * @param load true for a loaded repository
     * @return the specific sample repository for the provided book, or null
     */
    public static SampleRepository getInstance (Book book,
                                                boolean load)
    {
        if (BookManager.useSeparateBookFolders()) {
            final Path bookFolder = BookManager.getDefaultBookFolder(book);
            final Path baseFolder = BookManager.getBaseFolder();

            // Make sure this is a folder dedicated to the book
            if (!baseFolder.toAbsolutePath().normalize()
                    .equals(bookFolder.toAbsolutePath().normalize())) {
                final SampleRepository repo = new SampleRepository(bookFolder);

                if (load && !repo.isLoaded()) {
                    repo.loadRepository();
                }

                return repo;
            }
        }

        return null;
    }

    //-------------//
    // hasInstance //
    //-------------//
    /**
     * Report whether the global repository has been allocated.
     *
     * @return true if GLOBAL exists
     */
    public static boolean hasInstance ()
    {
        return GLOBAL != null;
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
     * Build and add a sample from a provided glyph, with its related staff and
     * containing sheet.
     *
     * @param shape assigned shape
     * @param glyph underlying glyph
     * @param staff related staff
     * @param sheet containing sheet
     */
    public void addSample (Shape shape,
                           Glyph glyph,
                           Staff staff,
                           Sheet sheet)
    {
        final SampleSheet sampleSheet = findSampleSheet(sheet);
        final double pitch = staff.pitchPositionOf(glyph.getCentroid());
        addSample(shape, glyph, staff.getSpecificInterline(), sampleSheet, pitch);
    }

    //-----------//
    // addSample //
    //-----------//
    /**
     * Build and add a sample from the provided glyph into the provided sample sheet.
     *
     * @param shape       assigned shape
     * @param glyph       underlying glyph
     * @param interline   scaling factor
     * @param sampleSheet target sample sheet
     * @param pitch       staff-related pitch, if any
     */
    public void addSample (Shape shape,
                           Glyph glyph,
                           int interline,
                           SampleSheet sampleSheet,
                           Double pitch)
    {
        shape = Sample.getRecordableShape(shape);

        final Sample sample = new Sample(glyph, interline, shape, pitch);
        addSample(sample, sampleSheet);
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

        logger.info("{} added {} to {}", this, sample, sampleSheet);

        fireStateChanged(new AdditionEvent(sample));
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
     * @param conflictings output to be populated by conflicting samples
     * @param redundants   output to be populated by redundant samples
     */
    public void checkAllSamples (Collection<Sample> conflictings,
                                 Collection<Sample> redundants)
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

                return getSheetName(s1).compareTo(getSheetName(s2));
            }
        });

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
                                getSheetName(sample),
                                sample,
                                getSheetName(s),
                                s);
                        conflictings.add(sample);
                        conflictings.add(s);
                    } else {
                        logger.info(
                                "Same runtable for {}/{} & {}/{}",
                                getSheetName(sample),
                                sample,
                                getSheetName(s),
                                s);
                        redundants.add(s);
                        deleted[j] = true;
                    }
                }
            }
        }

        if (!conflictings.isEmpty()) {
            logger.warn("Conflicting samples: {} / {}", conflictings.size(), allSamples.size());
        }

        if (!redundants.isEmpty()) {
            logger.info("Redundant samples: {} / {}", redundants.size(), allSamples.size());
        }
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
        } else {
            logger.info("No need to save {}", this);
        }
    }

    //-----------------//
    // findSampleSheet //
    //-----------------//
    /**
     * Find out (or create) the SampleSheet that corresponds to the provided Sheet.
     *
     * @param sheet provided sheet
     * @return the found or created sample sheet, where samples can be added to. Non-null.
     */
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
     * If sheet image is provided, the repository is searched for the image:<ul>
     * <li> If an identical sheet image already exists, it is used and the provided name is kept as
     * an alias.
     * <li>Otherwise, a new sample sheet is created. If the provided name is already used, a suffix
     * is appended to the name to make it unique within the repository.
     * </ul>
     * If a sheet name is provided but no sheet image, a sample sheet is found or created with the
     * provided name, <i>assumed to be unique</i>.
     * Note that handling sheets without image is <b>not reliable</b>.
     *
     * @param name     name of containing sheet, non-null if image is null
     * @param longName an optional longer name, if any
     * @param image    sheet binary image, if any, strongly recommended
     * @return the found or created sample sheet, where samples can be added to. Non-null.
     */
    public SampleSheet findSampleSheet (String name,
                                        String longName,
                                        RunTable image)
    {
        if ((name == null) || ((name.isEmpty()) && (image == null))) {
            throw new IllegalArgumentException("findSampleSheet() needs sheet name or image");
        }

        SampleSheet sampleSheet;

        if (image != null) {
            final int hash = image.persistentHashCode();
            sampleSheet = imageMap.get(image);

            if (sampleSheet == null) {
                // Is there a not-yet-loaded table?
                List<Descriptor> descs = sheetContainer.getDescriptors(hash);

                if (!descs.isEmpty()) {
                    try {
                        final Path root = Zip.openFileSystem(imagesFile);

                        for (Descriptor desc : descs) {
                            final Path file = root.resolve(desc.getName()).resolve(
                                    SampleSheet.IMAGE_FILE_NAME);
                            final RunTable rt = RunTable.unmarshal(file);

                            if ((rt != null) && rt.equals(image)) {
                                // We have found the image
                                desc.addAlias(name);
                                desc.addAlias(longName);

                                sampleSheet = nameMap.get(desc.getName());
                                sampleSheet.setImage(rt, true);
                                imageMap.put(rt, sampleSheet);

                                break;
                            }
                        }

                        root.getFileSystem().close();
                    } catch (IOException ex) {
                    }
                }

                if (sampleSheet == null) {
                    // Make sure name is unique
                    name = sheetContainer.forgeUnique(name);

                    // Allocate a brand new descriptor
                    Descriptor desc = new Descriptor(name, hash);
                    desc.addAlias(longName);

                    sheetContainer.addDescriptor(desc);

                    // Allocate a brand new sheet
                    sampleSheet = new SampleSheet(desc);
                    nameMap.put(desc.getName(), sampleSheet);
                    imageMap.put(image, sampleSheet);
                    sampleSheet.setImage(image, false);
                }
            }
        } else {
            // We have no image, just a sheet name. This is DANGEROUS!
            Descriptor desc = sheetContainer.getDescriptor(name);

            if (desc != null) {
                desc.addAlias(longName);

                return nameMap.get(desc.getName());
            } else {
                // Allocate a brand new descriptor
                desc = new Descriptor(name, null);
                desc.addAlias(longName);
                sheetContainer.addDescriptor(desc);

                // Allocate a brand new sheet
                sampleSheet = new SampleSheet(desc);
                nameMap.put(desc.getName(), sampleSheet);
            }
        }

        return sampleSheet;
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

        for (SampleSheet sheet : nameMap.values()) {
            allSamples.addAll(sheet.getAllSamples());
        }

        return allSamples;
    }

    //--------------//
    // getAllTribes //
    //--------------//
    /**
     * Report all the tribes in the repository.
     *
     * @return all the repository tribes
     */
    public List<Tribe> getAllTribes ()
    {
        final List<Tribe> allTribes = new ArrayList<Tribe>();

        for (SampleSheet sheet : nameMap.values()) {
            allTribes.addAll(sheet.getTribes());
        }

        return allTribes;
    }

    //---------------//
    // getDescriptor //
    //---------------//
    /**
     * Report the descriptor of the sample sheet that contains the provided sample.
     *
     * @param sample provided sample
     * @return the descriptor of containing SampleSheet
     */
    public Descriptor getDescriptor (Sample sample)
    {
        SampleSheet sampleSheet = getSampleSheet(sample);

        if (sampleSheet != null) {
            return sampleSheet.getDescriptor();
        }

        return null;
    }

    //----------------//
    // getSampleSheet //
    //----------------//
    /**
     * Report the SampleSheet that contains the provided sample.
     *
     * @param sample the provided sample
     * @return the containing sample sheet
     */
    public SampleSheet getSampleSheet (Sample sample)
    {
        return sampleMap.get(sample);
    }

    //----------------//
    // getSampleSheet //
    //----------------//
    /**
     * Report the SampleSheet related to the provided descriptor.
     *
     * @param descriptor the provided descriptor
     * @return the related sample sheet
     */
    public SampleSheet getSampleSheet (Descriptor descriptor)
    {
        return nameMap.get(descriptor.getName());
    }

    //------------//
    // getSamples //
    //------------//
    /**
     * Report, in the SampleSheet whose name is provided, all samples assigned the
     * desired shape.
     *
     * @param name  name of sample sheet
     * @param shape desired shape
     * @return the list of samples related to shape in provided sheet
     */
    public List<Sample> getSamples (String name,
                                    Shape shape)
    {
        SampleSheet sampleSheet = nameMap.get(name);

        if (sampleSheet != null) {
            return sampleSheet.getSamples(shape);
        }

        return Collections.emptyList();
    }

    //------------//
    // getSamples //
    //------------//
    /**
     * Report, in the desired sheet descriptors, the samples of the desired shapes.
     *
     * @param descriptors the desired descriptors
     * @param shapes      the desired shapes
     * @return the list of samples related to desired shapes in desired sheets
     */
    public List<Sample> getSamples (Collection<Descriptor> descriptors,
                                    Collection<Shape> shapes)
    {
        List<Sample> found = new ArrayList<Sample>();

        for (Descriptor descriptor : descriptors) {
            SampleSheet sampleSheet = nameMap.get(descriptor.getName());

            List<Shape> sheetShapes = new ArrayList<Shape>(sampleSheet.getShapes());
            sheetShapes.retainAll(shapes);

            for (Shape shape : sheetShapes) {
                found.addAll(sampleSheet.getSamples(shape));
            }
        }

        return found;
    }

    //-----------//
    // getShapes //
    //-----------//
    /**
     * Report all shapes for which the provided sheet descriptor has concrete samples.
     *
     * @param descriptor descriptor of the sample sheet
     * @return the list of (non-empty) shapes
     */
    public Set<Shape> getShapes (Descriptor descriptor)
    {
        // Symbols?
        if (isSymbols(descriptor.getName())) {
            return ShapeSet.allPhysicalShapes;
        }

        // Standard sheet
        SampleSheet sampleSheet = nameMap.get(descriptor.getName());

        if (sampleSheet != null) {
            return sampleSheet.getShapes();
        }

        return Collections.emptySet();
    }

    //--------------//
    // getSheetName //
    //--------------//
    /**
     * Report the name of the sample sheet that contains the provided sample.
     *
     * @param sample provided sample
     * @return the containing SampleSheet name
     */
    public String getSheetName (Sample sample)
    {
        SampleSheet sampleSheet = sampleMap.get(sample);

        if (sampleSheet != null) {
            return sampleSheet.getDescriptor().getName();
        }

        return null;
    }

    //----------------//
    // hasSheetImages //
    //----------------//
    /**
     * Check whether file of sheet images is available.
     *
     * @return true if images are available
     */
    public boolean hasSheetImages ()
    {
        return Files.exists(imagesFile);
    }

    //-------------------//
    // includeRepository //
    //-------------------//
    public void includeRepository (SampleRepository source)
    {
        source.loadSheetImages();

        for (SampleSheet sampleSheet : source.nameMap.values()) {
            includeSampleSheet(sampleSheet);
        }
    }

    //-----------//
    // isSymbols //
    //-----------//
    /**
     * Report whether the provided sheet name is a font-based symbols sheet
     *
     * @param name provided sheet name
     * @return true if font-based symbols
     */
    public static boolean isSymbols (String name)
    {
        return SYMBOLS.equals(name);
    }

    //------------------//
    // repositoryExists //
    //------------------//
    /**
     * Report whether a repository exists on disk for the provided book.
     *
     * @param book the provided book
     * @return true if repository file(s) exist(s)
     */
    public static boolean repositoryExists (Book book)
    {
        if (BookManager.useSeparateBookFolders()) {
            final Path bookFolder = BookManager.getDefaultBookFolder(book);
            final Path baseFolder = BookManager.getBaseFolder();

            // Make sure this is a folder dedicated to the book
            if (!baseFolder.toAbsolutePath().normalize()
                    .equals(bookFolder.toAbsolutePath().normalize())) {
                final String prefix = prefixOf(bookFolder);
                final Path samplesFile = bookFolder.resolve(prefix + SAMPLES_FILE_NAME);

                if (Files.exists(samplesFile)) {
                    return true;
                }
            }
        }

        return false;
    }

    //--------------------//
    // includeSampleSheet //
    //--------------------//
    /**
     * Include the provided SampleSheet, with all its samples, into the repository.
     * <p>
     * The provided SampleSheet is meant to come from an external repository (typically from a
     * book-specific repository to the global repository).
     *
     * @param extSheet the provided (external) SampleSheet
     * @return the local SampleSheet (created or augmented)
     */
    public SampleSheet includeSampleSheet (SampleSheet extSheet)
    {
        // First, find out (or create) proper local SampleSheet
        final RunTable extImage = extSheet.getImage();
        final Descriptor extDescriptor = extSheet.getDescriptor();
        final SampleSheet localSheet = findSampleSheet(extDescriptor.getName(), null, extImage);

        // Copy external aliases
        final Descriptor localDescriptor = localSheet.getDescriptor();
        localDescriptor.addAlias(extDescriptor.getName());

        for (String alias : extDescriptor.getAliases()) {
            localDescriptor.addAlias(alias);
        }

        // Copy samples from external to local
        for (Sample sample : extSheet.getAllSamples()) {
            addSample(sample, localSheet);
        }

        // Copy tribes from external to local
        for (Tribe tribe : extSheet.getTribes()) {
            final Tribe localTribe = localSheet.getTribe(tribe.getHead());

            for (Sample good : tribe.getGoods()) {
                localTribe.addGood(good);
            }

            for (Sample member : tribe.getMembers()) {
                localTribe.addOther(member);
            }
        }

        return localSheet;
    }

    //----------//
    // isGlobal //
    //----------//
    /**
     * Report whether this repository if the global instance.
     *
     * @return true if global
     */
    public boolean isGlobal ()
    {
        return this == GLOBAL;
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
        if (sheetContainer.isModified()) {
            return true;
        }

        for (SampleSheet sheet : nameMap.values()) {
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
     */
    public void loadRepository ()
    {
        final StopWatch watch = new StopWatch("Loading repository");

        try {
            if (Files.exists(samplesFile)) {
                watch.start("open samples.zip");

                final Path samplesRoot = Zip.openFileSystem(samplesFile);

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

                // Tribes?
                if (USE_TRIBES) {
                    watch.start("loadTribes");
                    loadTribes(samplesRoot);
                }

                samplesRoot.getFileSystem().close();
            } else {
                logger.info(
                        "No {} in folder {}",
                        samplesFile.getFileName(),
                        samplesFile.getParent());
            }

            loaded = true;
        } catch (IOException ex) {
            logger.warn("Error loading " + this + " " + ex, ex);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //-----------------//
    // loadSheetImages //
    //-----------------//
    /**
     * Load the sheet images, if available and not already done.
     */
    public void loadSheetImages ()
    {
        if (!imagesLoaded && hasSheetImages()) {
            try {
                final Path imagesRoot = Zip.openFileSystem(imagesFile);
                loadImages(imagesRoot);
                imagesRoot.getFileSystem().close();
                imagesLoaded = true;
            } catch (IOException ex) {
                logger.warn("Error loading sheet images " + ex, ex);
            }
        } else {
            logger.info("{} not found.", imagesFile);
        }
    }

    //-----------------//
    // pokeSampleSheet //
    //-----------------//
    /**
     * Return SampleSheet for provided sheet, only if it already exists.
     *
     * @param sheet provided sheet
     * @return related SampleSheet if any
     */
    public SampleSheet pokeSampleSheet (Sheet sheet)
    {
        RunTable image = sheet.getPicture().getTable(Picture.TableKey.BINARY);

        return imageMap.get(image);
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

        logger.info("{} removed {} from {}", this, sample, sampleSheet);

        fireStateChanged(new RemovalEvent(sample));
    }

    //-------------//
    // removeSheet //
    //-------------//
    /**
     * Remove the provided sheet with all its samples from the repository.
     *
     * @param descriptor the descriptor of the sampleSheet to remove
     */
    public void removeSheet (Descriptor descriptor)
    {
        final SampleSheet sampleSheet = nameMap.get(descriptor.getName());
        nameMap.remove(descriptor.getName());

        if (sampleSheet.getImage() != null) {
            imageMap.remove(sampleSheet.getImage());
        }

        for (Sample sample : sampleSheet.getAllSamples()) {
            sampleMap.remove(sample);
        }

        sheetContainer.removeDescriptor(descriptor);
        fireStateChanged(new SheetRemovalEvent(descriptor));
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
            final Path samplesRoot = Files.exists(samplesFile) ? Zip.openFileSystem(samplesFile)
                    : Zip.createFileSystem(samplesFile);
            final Path imagesRoot = Files.exists(imagesFile) ? Zip.openFileSystem(imagesFile)
                    : Zip.createFileSystem(imagesFile);

            // Container
            if (sheetContainer.isModified()) {
                sheetContainer.marshal(samplesRoot, imagesRoot);
            }

            // Samples
            for (SampleSheet sampleSheet : nameMap.values()) {
                if (sampleSheet.isModified()) {
                    sampleSheet.marshal(samplesRoot, imagesRoot);
                }
            }

            samplesRoot.getFileSystem().close();
            imagesRoot.getFileSystem().close();

            setModified(false);
            logger.info("{} stored.", this);
        } catch (Throwable ex) {
            logger.warn("Error storing " + this + " to " + samplesFile + " " + ex, ex);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        String name = isGlobal() ? "GLOBAL" : FileUtil.getNameSansExtension(folder);

        return name + " repository";
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
        Descriptor desc = sheetContainer.getDescriptor(SYMBOLS);

        if (desc == null) {
            desc = new Descriptor(SYMBOLS, null);
            sheetContainer.addDescriptor(desc);
        }

        SampleSheet symbolSheet = new SampleSheet(desc);

        for (Shape shape : ShapeSet.allPhysicalShapes) {
            Sample sample = buildSymbolSample(shape);
            symbolSheet.privateAddSample(sample);
            sampleMap.put(sample, symbolSheet);
        }

        nameMap.put(SYMBOLS, symbolSheet);
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
                            Path folder = file.getParent().getFileName();
                            SampleSheet sampleSheet = nameMap.get(folder.toString());

                            if (sampleSheet != null) {
                                sampleSheet.setImage(runTable, true);
                                logger.debug("Loaded {}", file);
                            } else {
                                logger.warn("No SampleSheet found for image {}", file);
                            }
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Throwable ex) {
            logger.warn("Error loading binaries from " + imagesFile + " " + ex, ex);
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
                        Path folder = file.getParent().getFileName();
                        Descriptor desc = sheetContainer.getDescriptor(folder.toString());

                        if (desc == null) {
                            logger.warn(
                                    "Samples entry {} not declared in {} is ignored.",
                                    folder,
                                    SheetContainer.CONTAINER_ENTRY_NAME);
                        } else {
                            SampleSheet sampleSheet = SampleSheet.unmarshal(file, desc);
                            boolean isSymbol = isSymbols(desc.getName());
                            nameMap.put(desc.getName(), sampleSheet);

                            for (Sample sample : sampleSheet.getAllSamples()) {
                                sample.setSymbol(isSymbol);
                                sampleMap.put(sample, sampleSheet);
                            }
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Throwable ex) {
            logger.warn("Error loading " + samplesFile + " " + ex, ex);
        }
    }

    //------------//
    // loadTribes //
    //------------//
    /**
     * Unmarshal all the sheet tribes available in training material.
     */
    private void loadTribes (final Path root)
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

                    if (fileName.equals(SampleSheet.TRIBES_FILE_NAME)) {
                        Path folder = file.getParent().getFileName();
                        SampleSheet sampleSheet = nameMap.get(folder.toString());

                        if (sampleSheet != null) {
                            TribeList tribeList = TribeList.unmarshal(file);
                            sampleSheet.setTribes(tribeList.getTribes());
                            logger.debug("Loaded {}", file);
                        } else {
                            logger.warn("No SampleSheet found for tribes {}", file);
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Throwable ex) {
            logger.warn("Error loading tribes " + ex, ex);
        }
    }

    //----------//
    // prefixOf //
    //----------//
    private static String prefixOf (Path folder)
    {
        return (folder == WellKnowns.TRAIN_FOLDER) ? "" : (folder.getFileName() + "-");
    }

    //-------------//
    // setModified //
    //-------------//
    private void setModified (boolean bool)
    {
        sheetContainer.setModified(bool);

        for (SampleSheet sampleSheet : nameMap.values()) {
            sampleSheet.setModified(bool);
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
     * Event used to carry information about sample addition performed.
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
     * Event used to carry information about sample removal performed.
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

    //-------------------//
    // SheetRemovalEvent //
    //-------------------//
    /**
     * Event used to carry information about sheet removal performed.
     */
    public class SheetRemovalEvent
            extends ChangeEvent
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final Descriptor descriptor; // Descriptor of the removed sheet

        //~ Constructors ---------------------------------------------------------------------------
        public SheetRemovalEvent (Descriptor descriptor)
        {
            super(SampleRepository.this);
            this.descriptor = descriptor;
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

        private final Constant.Boolean useTribes = new Constant.Boolean(
                false,
                "Should we support tribes?");
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
                        "Save " + SampleRepository.this + "?");

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
