//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S a m p l e R e p o s i t o r y                                //
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
import org.audiveris.omr.sig.inter.FingeringInter;
import org.audiveris.omr.sig.inter.FretInter;
import org.audiveris.omr.sig.inter.PluckingInter;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.ui.symbol.TextSymbol;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.ZipFileSystem;

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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class <code>SampleRepository</code> handles the store of {@link Sample} instances,
 * across multiple sheets and possibly multiple OMR sessions.
 * <p>
 * The repository is implemented as a collection of {@link SampleSheet} instances, to ease the
 * addition or removal of sheet samples as a whole.
 * <p>
 * A special kind of samples is provided by the use of a music font or text font with proper
 * scaling.
 * These font-based samples, though being artificial, are considered as part of the training
 * material.
 * There is at most one font-base sample in each font family for every trainable shape,
 * and these sample are always shown in first position among all samples of the same shape.
 * All these font-based samples are gathered in virtual containers, whose names start with
 * {@link #SYMBOLS_PREFIX}.
 * <p>
 * <img alt="Sample management" src="doc-files/Samples.png">
 *
 * @author Hervé Bitteur
 */
public class SampleRepository
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SampleRepository.class);

    /** Standard interline value: {@value}. (Any value could fit, if used consistently) */
    public static final int STANDARD_INTERLINE = 20;

    /** File name for samples material: {@value}. */
    public static final String SAMPLES_FILE_NAME = "samples.zip";

    /** Should we support tribes?. */
    public static final boolean USE_TRIBES = constants.useTribes.isSet();

    /** The global repository instance, if any. */
    private static volatile SampleRepository GLOBAL;

    /** File name for images material: {@value}. */
    private static final String IMAGES_FILE_NAME = "images.zip";

    /** Special prefix to refer to font-based samples, according to font family. */
    private static final String SYMBOLS_PREFIX = "# SYMBOLS FROM FONT # ";

    /**
     * Regex pattern for samples archive file name.
     * <ul>
     * <li>samples.zip for the global repository (in WellKnowns.TRAIN_FOLDER)
     * <li>BOOK_NAME-samples.zip for a local book repository (in some folder)
     * </ul>
     */
    private static final Pattern SAMPLES_PATTERN = Pattern.compile("(.*-)?(samples\\.zip)");

    //~ Instance fields ----------------------------------------------------------------------------

    /** Sheets, mapped by their unique name. */
    private final Map<String, SampleSheet> nameMap = new TreeMap<>();

    /** Sheets, mapped by their image. */
    private final Map<RunTable, SampleSheet> imageMap = new HashMap<>();

    /** Sheets, mapped by their samples. */
    private final Map<Sample, SampleSheet> sampleMap = new HashMap<>();

    /** Container for sheet descriptors. */
    private SheetContainer sheetContainer = new SheetContainer();

    /** Is the repository already loaded?. */
    private boolean loaded;

    /** Have the images already been loaded?. */
    private boolean imagesLoaded;

    /** Listeners on repository modifications. */
    private final Set<ChangeListener> listeners = new LinkedHashSet<>();

    /** Book radix for this repository (empty string for the global repository). */
    private final String bookRadix;

    /** File path for samples material: {@value}. */
    private final Path samplesFile;

    /** File path for images material: {@value}. */
    private final Path imagesFile;

    /** To handle save on close. */
    private Application.ExitListener exitListener;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * (Private) constructor.
     * <p>
     * NOTA: The provided samples file is not accessed before {@link #loadRepository()} or
     * {@link storeRepository()} is called.
     *
     * @param samplesFile path to the samples archive file.
     */
    private SampleRepository (Path samplesFile)
    {
        final Path fileName = samplesFile.getFileName();
        final Matcher matcher = SAMPLES_PATTERN.matcher(fileName.toString());

        if (!matcher.find()) {
            throw new IllegalArgumentException("Illegal samples archive name: " + samplesFile);
        }

        String prefix = matcher.group(1);

        if (prefix == null) {
            prefix = "";
        }

        bookRadix = prefix.isEmpty() ? "" : prefix.substring(0, prefix.length() - 1);
        this.samplesFile = samplesFile;
        this.imagesFile = samplesFile.resolveSibling(prefix + IMAGES_FILE_NAME);

        // Set application exit listener
        if (OMR.gui != null) {
            OmrGui.getApplication().addExitListener(getExitListener());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

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

        logger.info("{} added {} to {}", this, sample, sampleSheet);

        fireStateChanged(new AdditionEvent(sample, this));
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
        final Shape physicalShape = Sample.getRecordableShape(shape);

        if (physicalShape != null) {
            final Sample sample = new Sample(glyph, interline, shape, pitch);
            addSample(sample, sampleSheet);
        }
    }

    //-----------//
    // addSample //
    //-----------//
    /**
     * Build and add a sample from a provided glyph and containing sheet.
     * <p>
     * <b>Beware</b>, this method uses sheet global interline rather than staff specific interline.
     *
     * @param shape assigned shape
     * @param glyph underlying glyph
     * @param sheet containing sheet
     */
    @Deprecated
    public void addSample (Shape shape,
                           Glyph glyph,
                           Sheet sheet)
    {
        final SampleSheet sampleSheet = findSampleSheet(sheet);
        addSample(shape, glyph, sheet.getInterline(), sampleSheet, null);
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

    //--------------//
    // buildSymbols //
    //--------------//
    /**
     * Build all the artificial symbols for all supported fonts.
     */
    private void buildSymbols ()
    {
        // Music symbols
        final EnumSet<Shape> textShapes = EnumSet.noneOf(Shape.class);
        textShapes.addAll(ShapeSet.Digits.getShapes());
        textShapes.addAll(ShapeSet.Pluckings.getShapes());
        textShapes.addAll(ShapeSet.Romans.getShapes());
        textShapes.add(Shape.CLUTTER);

        for (TextFamily textFamily : TextFamily.values()) {
            final String sheetName = SYMBOLS_PREFIX + textFamily;
            Descriptor desc = sheetContainer.getDescriptor(sheetName);

            if (desc == null) {
                desc = new Descriptor(sheetName, null);
                sheetContainer.addDescriptor(desc);
            }

            final SampleSheet symbolSheet = new SampleSheet(desc);

            for (Shape shape : textShapes) {
                final Sample sample = buildSymbolSample(textFamily, shape);

                if (sample != null) {
                    symbolSheet.privateAddSample(sample);
                    sampleMap.put(sample, symbolSheet);
                }
            }

            nameMap.put(sheetName, symbolSheet);
        }

        final EnumSet<Shape> musicShapes = EnumSet.copyOf(ShapeSet.allPhysicalShapes);
        musicShapes.removeAll(textShapes);

        for (MusicFamily musicFamily : MusicFamily.values()) {
            final String sheetName = SYMBOLS_PREFIX + musicFamily;
            Descriptor desc = sheetContainer.getDescriptor(sheetName);

            if (desc == null) {
                desc = new Descriptor(sheetName, null);
                sheetContainer.addDescriptor(desc);
            }

            final SampleSheet symbolSheet = new SampleSheet(desc);

            for (Shape shape : musicShapes) {
                final Sample sample = buildSymbolSample(musicFamily, shape);

                if (sample != null) {
                    symbolSheet.privateAddSample(sample);
                    sampleMap.put(sample, symbolSheet);
                }
            }

            nameMap.put(sheetName, symbolSheet);
        }
    }

    //-------------------//
    // buildSymbolSample //
    //-------------------//
    /**
     * Build an artificial sample from a symbol descriptor, in order to
     * train a classifier even when we have no concrete sample.
     *
     * @param family chosen music font family
     * @param shape  the symbol shape
     * @return the sample built, or null if failed
     */
    private Sample buildSymbolSample (MusicFamily family,
                                      Shape shape)
    {
        Sample sample = null;

        // Make sure we have the drawing available for this shape
        ShapeSymbol symbol = family.getSymbols().getSymbol(shape);

        if (symbol != null) {
            final MusicFont font = MusicFont.getBaseFont(family, STANDARD_INTERLINE);
            sample = SymbolSample.create(shape, symbol, font, STANDARD_INTERLINE);
            sample.setSymbol(true);
        } else {
            logger.info("{} family, no artificial sample for {}", family, shape);
        }

        return sample;
    }

    //-------------------//
    // buildSymbolSample //
    //-------------------//
    /**
     * Build an artificial sample from a symbol descriptor, in order to
     * train a classifier even when we have no concrete sample.
     *
     * @param family chosen music font family
     * @param shape  the symbol shape
     * @return the sample built, or null if failed
     */
    private Sample buildSymbolSample (TextFamily family,
                                      Shape shape)
    {
        Sample sample = null;

        // Make sure we have the drawing available for this shape
        final String str;
        if (ShapeSet.Digits.contains(shape))
            str = "" + FingeringInter.valueOf(shape);
        else if (ShapeSet.Pluckings.contains(shape))
            str = String.valueOf(PluckingInter.valueOf(shape));
        else if (ShapeSet.Romans.contains(shape))
            str = FretInter.symbolStringOf(FretInter.valueOf(shape));
        else
            str = null;

        TextSymbol textSymbol = str != null ? new TextSymbol(shape, family, str) : null;

        if (textSymbol != null) {
            final TextFont font = TextFont.getBaseFontBySize(
                    family,
                    MusicFont.getPointSize(STANDARD_INTERLINE));
            sample = SymbolSample.create(shape, textSymbol, font, STANDARD_INTERLINE);
            sample.setSymbol(true);
        } else {
            logger.info("{} family, no artificial sample for {}", family, shape);
        }

        return sample;
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
                (Sample s1,
                 Sample s2) -> {
                    int comp = Integer.compare(s1.getWeight(), s2.getWeight());

                    if (comp != 0) {
                        return comp;
                    }

                    return getSheetName(s1).compareTo(getSheetName(s2));
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
                    if (s.getShape().getPhysicalShape() != sample.getShape().getPhysicalShape()) {
                        logger.warn(
                                "Conflicting shapes between {}/{} and {}/{}",
                                getSheetName(sample),
                                sample,
                                getSheetName(s),
                                s);
                        conflictings.add(sample);
                        conflictings.add(s);
                    } else {
                        logger.debug(
                                "Same runtable for {}/{} & {}/{}",
                                getSheetName(sample),
                                sample,
                                getSheetName(s),
                                s);
                        if (!s.isSymbol()) {
                            redundants.add(s);
                        }
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

    //------------------//
    // checkFontSamples //
    //------------------//
    /**
     * Make sure that all redundant font-based samples are flagged as "ignored".
     */
    public void checkFontSamples ()
    {
        List<Sample> fontSamples = getFontSamples();

        // Sort by weight, then by sheet ID
        Collections.sort(
                fontSamples,
                (Sample s1,
                 Sample s2) -> {
                    int comp = Integer.compare(s1.getWeight(), s2.getWeight());

                    if (comp != 0) {
                        return comp;
                    }

                    return getSheetName(s1).compareTo(getSheetName(s2));
                });

        int n = fontSamples.size();
        logger.debug("Checking {} font samples...", n);

        boolean[] deleted = new boolean[n];
        int ignoredCount = 0;

        for (int i = 0; i < n; i++) {
            if (deleted[i]) {
                continue;
            }

            final Sample sample = fontSamples.get(i);
            final int weight = sample.getWeight();
            final RunTable runTable = sample.getRunTable();
            final int interline = sample.getInterline();

            for (int j = i + 1; j < n; j++) {
                if (deleted[j]) {
                    continue;
                }

                Sample s = fontSamples.get(j);

                if (s.getWeight() != weight) {
                    break;
                }

                if ((s.getInterline() == interline) && s.getRunTable().equals(runTable)) {
                    if (s.getShape().getPhysicalShape() != sample.getShape().getPhysicalShape()) {
                        logger.warn(
                                "Conflicting shapes between {}/{} and {}/{}",
                                getSheetName(sample),
                                sample,
                                getSheetName(s),
                                s);
                    } else {
                        logger.debug("Ignoring redundant {}/{}", getSheetName(s), s);
                        s.setIgnored(true);
                        deleted[j] = true;
                        ignoredCount++;
                    }
                }
            }
        }

        if (ignoredCount > 0) {
            logger.info("Ignored redundant font-based samples: {}", ignoredCount);
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

    //-------//
    // close //
    //-------//
    /**
     * Close the repository.
     */
    public synchronized void close ()
    {
        if (isGlobal()) {
            GLOBAL = null;
        }
    }

    //-----------------//
    // diskImageExists //
    //-----------------//
    /**
     * Report whether the provided descriptor has an image on disk.
     *
     * @param descriptor the provided sheet descriptor
     * @return true if sheet image file exists on disk
     */
    public boolean diskImageExists (Descriptor descriptor)
    {
        if (!Files.exists(imagesFile)) {
            return false;
        }

        try {
            Path imagesRoot = ZipFileSystem.open(imagesFile);

            try {
                Path folderPath = imagesRoot.resolve(descriptor.getName());

                if (!Files.exists(folderPath)) {
                    return false;
                }

                Path imagePath = folderPath.resolve(SampleSheet.IMAGE_FILE_NAME);

                return Files.exists(imagePath);
            } finally {
                if (imagesRoot != null) {
                    imagesRoot.getFileSystem().close();
                }
            }
        } catch (IOException ex) {
            return false;
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
                sheet.getPicture().getVerticalTable(Picture.TableKey.BINARY));
    }

    //-----------------//
    // findSampleSheet //
    //-----------------//
    /**
     * Find out (or create) the SampleSheet that corresponds to provided name and/or
     * image.
     * <p>
     * If sheet image is provided, the repository is searched for the image:
     * <ul>
     * <li>If an identical sheet image already exists, it is used and the provided name is kept as
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
                        final Path root = ZipFileSystem.open(imagesFile);

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
                    } catch (IOException ignored) {}
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

    //------------------//
    // fireStateChanged //
    //------------------//
    private void fireStateChanged (ChangeEvent event)
    {
        for (ChangeListener listener : listeners) {
            listener.stateChanged(event);
        }
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
        final List<Sample> allSamples = new ArrayList<>();

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
        final List<Tribe> allTribes = new ArrayList<>();

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

    //-----------------//
    // getExitListener //
    //-----------------//
    /**
     * Report the ExitListener called at closing time.
     *
     * @return specific exit listener that check if repository has unsaved modifications
     */
    public final synchronized Application.ExitListener getExitListener ()
    {
        if (exitListener == null) {
            exitListener = new RepositoryExitListener();
        }

        return exitListener;
    }

    //----------------//
    // getFontSamples //
    //----------------//
    /**
     * Report the font-based samples in the repository.
     *
     * @return the repository font-based samples
     */
    public List<Sample> getFontSamples ()
    {
        final List<Sample> fontSamples = new ArrayList<>();

        for (Entry<String, SampleSheet> entry : nameMap.entrySet()) {
            if (isSymbols(entry.getKey())) {
                fontSamples.addAll(entry.getValue().getAllSamples());
            }
        }

        return fontSamples;
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
        List<Sample> found = new ArrayList<>();

        for (Descriptor descriptor : descriptors) {
            SampleSheet sampleSheet = nameMap.get(descriptor.getName());

            List<Shape> sheetShapes = new ArrayList<>(sampleSheet.getShapes());
            sheetShapes.retainAll(shapes);

            for (Shape shape : sheetShapes) {
                found.addAll(sampleSheet.getSamples(shape));
            }
        }

        return found;
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
    /**
     * Include the content of another repository into this one.
     *
     * @param source the other repository to include
     */
    public void includeRepository (SampleRepository source)
    {
        source.loadAllImages();

        for (SampleSheet sampleSheet : source.nameMap.values()) {
            // We process all but font-based samples
            if (!isSymbols(sampleSheet.getDescriptor().getName())) {
                includeSampleSheet(sampleSheet);
            }
        }
    }

    //--------------------//
    // includeSamplesFile //
    //--------------------//
    /**
     * Include the content of a samples file.
     *
     * @param samplesFile provided samples file
     */
    public synchronized void includeSamplesFile (Path samplesFile)
    {
        SampleRepository repo = getInstance(samplesFile, true);

        if (repo != null) {
            includeRepository(repo);
        }
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
    /**
     * Report whether the repository has unsaved modifications.
     *
     * @return true if any modification has not been saved
     */
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

    //---------------//
    // loadAllImages //
    //---------------//
    /**
     * Load the sheet images, if available and not already done.
     */
    public void loadAllImages ()
    {
        if (imagesLoaded) {
            logger.info("All images already loaded.");
        } else if (!hasSheetImages()) {
            logger.info("Images file {} not found.", imagesFile);
        } else {
            try {
                final Path imagesRoot = ZipFileSystem.open(imagesFile);
                logger.info("Loading all images from {} ...", imagesFile);
                loadAllImages(imagesRoot);
                imagesRoot.getFileSystem().close();
                imagesLoaded = true;
            } catch (IOException ex) {
                logger.warn("Error loading sheet images " + ex, ex);
            }
        }
    }

    //---------------//
    // loadAllImages //
    //---------------//
    /**
     * Unmarshal all the sheet images available in training material and not yet loaded.
     */
    private void loadAllImages (final Path root)
    {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory (Path dir,
                                                          BasicFileAttributes attrs)
                    throws IOException
                {
                    // Check whether we already have an image for this folder
                    final Path dirFile = dir.getFileName();

                    if (dirFile != null) {
                        String dirName = dirFile.toString();

                        if (dirName.endsWith("/")) {
                            dirName = dirName.substring(0, dirName.length() - 1);
                        }

                        final SampleSheet sampleSheet = nameMap.get(dirName);

                        if ((sampleSheet != null) && (sampleSheet.getImage() != null)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }

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
        } catch (IOException ex) {
            logger.warn("Error loading binaries from " + imagesFile + " " + ex, ex);
        }
    }

    //-----------//
    // loadImage //
    //-----------//
    /**
     * Load the background image, if any, of a sample sheet
     *
     * @param sampleSheet the sheet of samples
     * @return the related image, or null if not found
     */
    public RunTable loadImage (SampleSheet sampleSheet)
    {
        final Descriptor descriptor = sampleSheet.getDescriptor();
        RunTable runTable = null;

        try {
            final Path imagesRoot = ZipFileSystem.open(imagesFile);

            try {
                Path folderPath = imagesRoot.resolve(descriptor.getName());

                if (!Files.exists(folderPath)) {
                    return null;
                }

                Path file = folderPath.resolve(SampleSheet.IMAGE_FILE_NAME);
                runTable = RunTable.unmarshal(file);

                if (runTable != null) {
                    sampleSheet.setImage(runTable, true);
                    logger.debug("Loaded {}", file);
                }
            } finally {
                if (imagesRoot != null) {
                    imagesRoot.getFileSystem().close();
                }
            }
        } catch (IOException ex) {
            logger.warn("Error loading {} image ", descriptor, ex);

            return null;
        }

        return runTable;
    }

    //----------------//
    // loadRepository //
    //----------------//
    /**
     * Load the training material (font-based symbols as well as concrete samples).
     *
     * @param loadListener load listener, or null
     */
    public void loadRepository (LoadListener loadListener)
    {
        final StopWatch watch = new StopWatch("Loading repository");

        try {
            if (Files.exists(samplesFile)) {
                watch.start("open samples.zip");

                final Path samplesRoot = ZipFileSystem.open(samplesFile);

                watch.start("loadContainer");

                {
                    SheetContainer container = SheetContainer.unmarshal(samplesRoot);

                    if (container != null) {
                        if (logger.isDebugEnabled()) {
                            container.dump();
                        }

                        sheetContainer = container;

                        if (loadListener != null) {
                            loadListener.totalSheets(container.getDescriptorCount());
                        }
                    }
                }

                watch.start("loadSamples");
                loadSamples(samplesRoot, loadListener);

                // Build all font-based symbols only *after* samples have been loaded,
                // this allows to cope with new shapes being defined in Shape class.
                watch.start("buildSymbols");
                buildSymbols();

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

    //-------------//
    // loadSamples //
    //-------------//
    /**
     * Unmarshal the repository concrete samples.
     */
    private void loadSamples (final Path root,
                              final LoadListener loadListener)
    {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>()
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
                        SampleSheet sampleSheet = null;

                        if (desc == null) {
                            logger.warn(
                                    "Samples entry {} not declared in {} is ignored.",
                                    folder,
                                    SheetContainer.CONTAINER_ENTRY_NAME);
                        } else {
                            boolean isSymbol = isSymbols(desc.getName());

                            if (isSymbol) {
                                logger.info("Skipping symbols entry");

                                return FileVisitResult.CONTINUE;
                            }

                            sampleSheet = SampleSheet.unmarshal(file, desc);
                            nameMap.put(desc.getName(), sampleSheet);

                            for (Sample sample : sampleSheet.getAllSamples()) {
                                sample.setSymbol(false);
                                sampleMap.put(sample, sampleSheet);
                            }
                        }

                        if (loadListener != null) {
                            loadListener.loadedSheet(sampleSheet);
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
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
            Files.walkFileTree(root, new SimpleFileVisitor<Path>()
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
        } catch (IOException ex) {
            logger.warn("Error loading tribes " + ex, ex);
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
        RunTable image = sheet.getPicture().getVerticalTable(Picture.TableKey.BINARY);

        return imageMap.get(image);
    }

    //-------------------//
    // printSampleCounts //
    //-------------------//
    /**
     * Print out the list of shapes and related samples count.
     */
    private void printSampleCounts ()
    {
        final int[] counts = new int[Shape.values().length];

        for (Sample sample : getAllSamples()) {
            final Shape shape = sample.getShape();
            final int ordinal = shape.ordinal();
            counts[ordinal]++;
        }

        final List<Shape> NNShapes = new ArrayList<>(EnumSet.range(Shape.DOT_set, Shape.CLUTTER));
        Collections.sort(
                NNShapes,
                (Shape o1,
                 Shape o2) -> o1.name().compareTo(o2.name()));

        System.out.println("");
        System.out.println("Sample counts:");
        System.out.println("-------------");
        System.out.println("");
        System.out.println("| Shape | Count |");
        System.out.println("| :---  |  ---: |");

        for (Shape shape : NNShapes) {
            final int ordinal = shape.ordinal();
            //System.out.println(String.format("   %3d: %5d %s", ordinal, counts[ordinal], shape));
            System.out.println(String.format("| %s | %d |", shape, counts[ordinal]));
        }
    }

    //------------------------//
    // purgeOrphanDescriptors //
    //------------------------//
    /**
     *
     */
    public void purgeOrphanDescriptors ()
    {
        for (Descriptor descriptor : new ArrayList<>(getAllDescriptors())) {
            final SampleSheet sampleSheet = getSampleSheet(descriptor);

            if (sampleSheet == null) {
                sheetContainer.removeDescriptor(descriptor);
                logger.info("{} removed orphan descriptor: {}", this, descriptor);
            }
        }
    }

    //-------------------//
    // purgeSampleSheets //
    //-------------------//
    /**
     * Any empty sample sheet is removed, together with its image if any.
     */
    public void purgeSheets ()
    {
        int count = 0;

        for (SampleSheet sampleSheet : nameMap.values()) {
            if (sampleSheet.getAllSamples().isEmpty()) {
                logger.info("Empty {}", sampleSheet);
                sheetContainer.removeDescriptor(sampleSheet.getDescriptor());
                count++;
            }
        }

        logger.info("{} empty sheets purged: {}", this, count);

        if (constants.printSampleCounts.isSet()) {
            printSampleCounts();
        }
    }

    //----------------//
    // removeListener //
    //----------------//
    /**
     * remove a ChangeListener
     *
     * @param listener the listener to remove
     * @return true if actually removed
     */
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
        final SampleSheet sampleSheet = getSampleSheet(sample);

        if (isSymbols(sampleSheet.getDescriptor().getName())) {
            logger.info("A font-based symbol cannot be removed");

            return;
        }

        sampleSheet.privateRemoveSample(sample);
        sampleMap.remove(sample);

        logger.info("{} removed {} from {}", this, sample, sampleSheet);

        fireStateChanged(new RemovalEvent(sample, this));
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
        fireStateChanged(new SheetRemovalEvent(descriptor, this));
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

    //--------//
    // shrink //
    //--------//
    /**
     * Shrink the repository, so that there is a maximum of 'maxCount' samples per shape.
     *
     * @param maxCount maximum number of samples per shape
     */
    public void shrink (int maxCount)
    {
        // Gather samples by shape
        EnumMap<Shape, List<Sample>> shapeSamples = new EnumMap<>(Shape.class);

        for (Sample sample : getAllSamples()) {
            Shape shape = sample.getShape();
            List<Sample> list = shapeSamples.get(shape);

            if (list == null) {
                shapeSamples.put(shape, list = new ArrayList<>());
            }

            list.add(sample);
        }

        for (List<Sample> list : shapeSamples.values()) {
            Collections.shuffle(list);

            for (int i = maxCount; i < list.size(); i++) {
                Sample sample = list.get(i);
                removeSample(sample);
            }
        }
    }

    //-------------------//
    // splitTrainAndTest //
    //-------------------//
    /**
     * Build train collection and test collection out of this repository.
     * <p>
     * In the 'train' collection, no shape collection can contain more than maxShapeSampleCount
     * samples.
     *
     * @param train    output to be populated by train samples
     * @param test     output to be populated by test samples
     * @param minCount minimum sample count per shape (for test)
     * @param maxCount maximum sample count per shape (for train and test)
     */
    public void splitTrainAndTest (List<Sample> train,
                                   List<Sample> test,
                                   int minCount,
                                   int maxCount)
    {
        // Flag redundant font-based samples as such
        checkFontSamples();

        // Gather samples by physical shape
        EnumMap<Shape, List<Sample>> shapeSamples = new EnumMap<>(Shape.class);

        for (Sample sample : getAllSamples()) {
            // Exclude redundant font-based samples
            if (sample.isIgnored()) {
                continue;
            }

            Shape physicalShape = sample.getShape().getPhysicalShape();
            List<Sample> list = shapeSamples.get(physicalShape);

            if (list == null) {
                shapeSamples.put(physicalShape, list = new ArrayList<>());
            }

            list.add(sample);
        }

        for (List<Sample> list : shapeSamples.values()) {
            Collections.shuffle(list);
            train.addAll(list.subList(0, Math.min(list.size(), maxCount)));

            final int size = list.size();
            final int i1 = Math.max(0, size - minCount);
            final int i2 = Math.max(maxCount, size - maxCount);
            test.addAll(list.subList(Math.min(i1, i2), size));
        }

        logger.info("Train: {}, Test: {}", train.size(), test.size());
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
            final Path samplesRoot = Files.exists(samplesFile) ? ZipFileSystem.open(samplesFile)
                    : ZipFileSystem.create(samplesFile);
            final Path imagesRoot = Files.exists(imagesFile) ? ZipFileSystem.open(imagesFile)
                    : ZipFileSystem.create(imagesFile);

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
            logger.info("{} stored to {}", this, samplesFile);
        } catch (IOException ex) {
            logger.warn("Error storing " + this + " to " + samplesFile + " " + ex, ex);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        String name = isGlobal() ? "GLOBAL" : bookRadix;

        return name + " repository";
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------------//
    // getGlobalInstance //
    //-------------------//
    /**
     * Report the (loaded) global repository, after creating it if needed.
     *
     * @return the global instance of SampleRepository
     */
    public static SampleRepository getGlobalInstance ()
    {
        return getGlobalInstance(true);
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
    public static synchronized SampleRepository getGlobalInstance (boolean load)
    {
        if (GLOBAL == null) {
            GLOBAL = getInstance(WellKnowns.TRAIN_FOLDER.resolve(SAMPLES_FILE_NAME), load);
        }

        if (load && (GLOBAL != null) && !GLOBAL.isLoaded()) {
            GLOBAL.loadRepository(null);
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
    public static synchronized SampleRepository getInstance (Book book,
                                                             boolean load)
    {
        return getInstance(getSamplesFile(book), load);
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the repository specifically related to the provided samples archives.
     *
     * @param samplesFile path to the global or specific samples .zip archive
     * @param load        true for a loaded repository
     * @return the specific sample repository, or null
     */
    public static synchronized SampleRepository getInstance (Path samplesFile,
                                                             boolean load)
    {
        try {
            final SampleRepository repo = new SampleRepository(samplesFile);

            if (load && !repo.isLoaded()) {
                logger.info("Repository loading...");
                repo.loadRepository(null);
                logger.info("Repository loaded.");
            }

            return repo;
        } catch (Exception ex) {
            logger.warn("Could not get repository instance at {} " + ex, samplesFile, ex);

            return null;
        }
    }

    //----------------//
    // getSamplesFile //
    //----------------//
    /**
     * Report the path to the (theoretical) samples file for the provided book.
     *
     * @param book the provided book
     * @return the theoretical path to samples file
     */
    private static Path getSamplesFile (Book book)
    {
        final Path bookFolder = BookManager.getDefaultBookFolder(book);

        return bookFolder.resolve(book.getRadix() + "-" + SAMPLES_FILE_NAME);
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
        return name.startsWith(SYMBOLS_PREFIX);
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
        return Files.exists(getSamplesFile(book));
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------------//
    // AdditionEvent //
    //---------------//
    /**
     * Event used to carry information about sample addition performed.
     */
    public static class AdditionEvent
            extends ChangeEvent
    {
        /** The sample added. */
        public final Sample sample;

        /**
         * Create an [@code AdditionEvent}.
         *
         * @param sample the sample added
         * @param repo   the repository where sample is added
         */
        public AdditionEvent (Sample sample,
                              SampleRepository repo)
        {
            super(repo);
            this.sample = sample;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Boolean useTribes = new Constant.Boolean(
                false,
                "Should we support tribes?");

        private final Constant.Boolean printSampleCounts = new Constant.Boolean(
                false,
                "Should we print out the count of samples per shape?");
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------

    //--------------//
    // LoadListener //
    //-------------//
    /**
     * Interface <code>LoadListener</code> defines the entries to a UI entity
     * which monitors the loading of samples by the sample repository.
     */
    public static interface LoadListener
    {
        /**
         * Called whenever a new sample sheet has been loaded.
         *
         * @param sampleSheet the sample sheet loaded
         */
        void loadedSheet (SampleSheet sampleSheet);

        /**
         * Called to pass the total number of sample sheets in repository
         *
         * @param total total number of sample sheets
         */
        void totalSheets (int total);
    }

    //--------------//
    // RemovalEvent //
    //--------------//
    /**
     * Event used to carry information about sample removal performed.
     */
    public static class RemovalEvent
            extends ChangeEvent
    {
        /** The removed sample. */
        public final Sample sample;

        /**
         * Create a removal event.
         *
         * @param sample the removed sample
         * @param repo   the impacted repository
         */
        public RemovalEvent (Sample sample,
                             SampleRepository repo)
        {
            super(repo);
            this.sample = sample;
        }
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
        RepositoryExitListener ()
        {
        }

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

    //-------------------//
    // SheetRemovalEvent //
    //-------------------//
    /**
     * Event used to carry information about sheet removal performed.
     */
    public static class SheetRemovalEvent
            extends ChangeEvent
    {
        /** Descriptor of the removed sheet. */
        public final Descriptor descriptor;

        /**
         * Create a event to remove a sample sheet.
         *
         * @param descriptor sample sheet descriptor
         * @param repo       impacted repository
         */
        public SheetRemovalEvent (Descriptor descriptor,
                                  SampleRepository repo)
        {
            super(repo);
            this.descriptor = descriptor;
        }
    }
}
