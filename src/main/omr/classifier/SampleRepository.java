//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S a m p l e R e p o s i t o r y                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier;

import omr.WellKnowns;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.SymbolGlyphDescriptor;
import omr.glyph.SymbolSample;

import omr.sheet.Sheet;

import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;

import omr.util.BlackList;
import omr.util.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Class {@code SampleRepository} handles the store of known samples,
 * across multiple sheets (and possibly multiple OMR sessions).
 * <p>
 * A sample is known by its full name, whose standard format is
 * <B>sheetName/Shape.id.xml</B>, regardless of the area it is stored (this may be the <I>core</I>
 * area or the global <I>sheets</I> area augmented by the <I>samples</I> area).
 * It can also be an <I>artificial</I> sample built from a symbol icon,
 * in that case its full name is the similar formats <B>icons/Shape.xml</B> or
 * <B>icons/Shape.nn.xml</B> where "nn" is a differentiating number.
 * <p>
 * The repository handles a private map of all de-serialized samples so far,
 * since the de-serialization is a rather expensive operation.
 * <p>
 * It handles two bases : the "whole base" (all samples from sheets and samples folders) and the
 * "core base" (just the samples of the core, which is built as a selected subset of the whole
 * base).
 * These bases are accessible respectively by {@link #getWholeBase} and
 * {@link #getCoreBase} methods.
 *
 * @author Hervé Bitteur
 */
public class SampleRepository
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            SampleRepository.class);

    /** The single instance of this class. */
    private static volatile SampleRepository INSTANCE;

    /** Extension for training files. */
    private static final String FILE_EXTENSION = ".xml";

    /** Specific subdirectory for sheet samples. */
    private static final Path sheetsFolder = WellKnowns.TRAIN_FOLDER.resolve("sheets");

    /** Specific subdirectory for core samples. */
    private static final Path coreFolder = WellKnowns.TRAIN_FOLDER.resolve("core");

    /** Specific subdirectory for isolated samples. */
    private static final Path samplesFolder = WellKnowns.TRAIN_FOLDER.resolve("samples");

    /** Specific filter for sample files. */
    private static final Filter<Path> sampleFilter = new Filter<Path>()
    {
        @Override
        public boolean accept (Path path)
        {
            return Files.isDirectory(path) || FileUtil.getExtension(path).equals(FILE_EXTENSION);
        }
    };

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    /** For comparing shape names. */
    public static final Comparator<String> shapeComparator = new Comparator<String>()
    {
        @Override
        public int compare (String s1,
                            String s2)
        {
            String n1 = SampleRepository.shapeNameOf(s1);
            String n2 = SampleRepository.shapeNameOf(s2);

            return n1.compareTo(n2);
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Core collection of samples. */
    private volatile List<String> coreBase;

    /** Whole collection of samples. */
    private volatile List<String> wholeBase;

    /**
     * Map of all samples de-serialized so far, using full sample name as key.
     * Full sample name format is : sheetName/Shape.id.xml
     */
    private final Map<String, Sample> samplesMap = new TreeMap<String, Sample>();

    /** Inverse map. */
    private final Map<Sample, String> namesMap = new HashMap<Sample, String>();

    //~ Constructors -------------------------------------------------------------------------------
    /** Private singleton constructor */
    private SampleRepository ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // fileNameOf //
    //------------//
    /**
     * Report the file name w/o extension of a gName.
     *
     * @param gName sample name, using format "folder/name.number.xml"
     *              or "folder/name.xml"
     * @return the 'name' or 'name.number' part of the format
     */
    public static String fileNameOf (String gName)
    {
        int slash = gName.indexOf('/');
        String nameWithExt = gName.substring(slash + 1);

        int lastDot = nameWithExt.lastIndexOf('.');

        if (lastDot != -1) {
            return nameWithExt.substring(0, lastDot);
        } else {
            return nameWithExt;
        }
    }

    //-------------//
    // getCoreBase //
    //-------------//
    /**
     * Return the names of the core collection of samples.
     *
     * @param monitor on-line monitoring if any
     * @return the core collection of recorded samples
     */
    public List<String> getCoreBase (Monitor monitor)
    {
        if (coreBase == null) {
            synchronized (this) {
                if (coreBase == null) {
                    coreBase = loadCoreBase(monitor);
                }
            }
        }

        return coreBase;
    }

    //--------------//
    // getGlyphName //
    //--------------//
    public String getGlyphName (Sample sample)
    {
        return namesMap.get(sample);
    }

    //-------------//
    // getGlyphsIn //
    //-------------//
    /**
     * Report the list of sample files that are contained within a given
     * directory
     *
     * @param dir the containing directory
     * @return the list of sample files
     */
    public synchronized List<Path> getGlyphsIn (Path dir)
    {
        Path[] files = listLegalFiles(dir);

        if (files != null) {
            return Arrays.asList(files);
        } else {
            logger.warn("Cannot get files list from dir {}", dir);

            return new ArrayList<Path>();
        }
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class, after creating it if
     * needed.
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

    //-----------//
    // getSample //
    //-----------//
    /**
     * Return a sample knowing its full sample name, which is the name of
     * the corresponding training material.
     * If not already done, the sample is de-serialized from the training file,
     * searching first in the icons area, then in the train area.
     *
     * @param gName   the full sample name (format is: sheetName/Shape.id.xml)
     * @param monitor the monitor, if any, to be kept informed of sample loading
     * @return the sample instance if found, null otherwise
     */
    public synchronized Sample getSample (String gName,
                                          Monitor monitor)
    {
        // First, try the map of samples
        Sample sample = samplesMap.get(gName);

        if (sample == null) {
            // If failed, actually load the sample from XML backup file.
            if (isIcon(gName)) {
                sample = buildSymbolSample(gName);
            } else {
                Path path = WellKnowns.TRAIN_FOLDER.resolve(gName);

                if (!Files.exists(path)) {
                    logger.warn("Unable to find file for sample {}", gName);

                    return null;
                }

                sample = loadSample(gName, path);
            }

            if (sample != null) {
                samplesMap.put(gName, sample);
                namesMap.put(sample, gName);
            }

            if (monitor != null) {
                monitor.loadedGlyph(gName);
            }
        }

        return sample;
    }

    //----------------------//
    // getSampleDirectories //
    //----------------------//
    /**
     * Report the list of all samples directories found in the training
     * material.
     *
     * @return the list of samples directories
     */
    public List<Path> getSampleDirectories ()
    {
        return getSubdirectories(samplesFolder);
    }

    //------------------//
    // getSamplesFolder //
    //------------------//
    /**
     * Report the folder where isolated samples samples are stored.
     *
     * @return the directory of isolated samples material
     */
    public Path getSamplesFolder ()
    {
        return samplesFolder;
    }

    //---------------------//
    // getSheetDirectories //
    //---------------------//
    /**
     * Report the list of all sheet directories found in the training material.
     *
     * @return the list of sheet directories
     */
    public List<Path> getSheetDirectories ()
    {
        return getSubdirectories(sheetsFolder);
    }

    //-----------------//
    // getSheetsFolder //
    //-----------------//
    /**
     * Report the folder where all sheet samples are stored.
     *
     * @return the directory of all sheets material
     */
    public Path getSheetsFolder ()
    {
        return sheetsFolder;
    }

    //--------------//
    // getWholeBase //
    //--------------//
    /**
     * Return the names of the whole collection of samples.
     *
     * @return the whole collection of recorded samples
     */
    public List<String> getWholeBase (Monitor monitor)
    {
        if (wholeBase == null) {
            synchronized (this) {
                if (wholeBase == null) {
                    wholeBase = loadWholeBase(monitor);
                }
            }
        }

        return wholeBase;
    }

    //--------//
    // isIcon //
    //--------//
    public boolean isIcon (String gName)
    {
        return isIcon(Paths.get(gName));
    }

    //---------------//
    // isIconsFolder //
    //---------------//
    public boolean isIconsFolder (Path folder)
    {
        return folder.equals(WellKnowns.SYMBOLS_FOLDER.getFileName());
    }

    //----------//
    // isLoaded //
    //----------//
    public synchronized boolean isLoaded (String gName)
    {
        return samplesMap.get(gName) != null;
    }

    //----------------//
    // recordOneGlyph //
    //----------------//
    /**
     * Record one sample on disk (into the samples folder).
     *
     * @param shape the assigned shape
     * @param glyph the glyph to record
     * @param sheet its containing sheet
     */
    public void recordOneGlyph (Shape shape,
                                Glyph glyph,
                                Sheet sheet)
    {
        Shape recordableShape = getRecordableShape(shape);

        if (recordableShape != null) {
            // Prepare target directory, based on sheet id
            Path sheetDir = getSamplesFolder().resolve(sheet.getId());

            try {
                // Make sure related directory chain exists
                Files.createDirectories(sheetDir);

                Sample sample = new Sample(
                        glyph.getLeft(),
                        glyph.getTop(),
                        glyph.getRunTable(),
                        sheet.getInterline(),
                        glyph.getId(),
                        recordableShape);

                if (recordSample(sample, sheetDir) > 0) {
                    logger.info(
                            "Stored {} {} into {}",
                            glyph.idString(),
                            recordableShape,
                            sheetDir);
                }
            } catch (IOException ex) {
                logger.warn("Cannot create folder " + sheetDir, ex);
            }
        } else {
            logger.warn("Not recordable {}", glyph);
        }
    }

    //-------------------//
    // recordSheetGlyphs //
    //-------------------//
    /**
     * Store all known samples of the provided sheet as separate XML
     * files, so that they can be later reloaded to train an evaluator.
     * We store sample for which Shape is not null, and different from NOISE and
     * STEM (CLUTTER is thus stored as well).
     *
     * <p>
     * STRUCTURE shapes are stored in a parallel sub-directory so that they
     * don't get erased by shapes of their leaves.
     *
     * @param sheet           the sheet whose samples are to be stored
     * @param emptyStructures flag to specify if the Structure directory must be
     *                        emptied beforehand
     */
    public void recordSheetGlyphs (Sheet sheet,
                                   boolean emptyStructures)
    {
        //        // Prepare target directory
        //        Path sheetDir = getSheetsFolder().resolve(sheet.getId());
        //
        //        // Make sure related directory chain exists
        //        if (Files.exists(sheetDir)) {
        //            deleteXmlFiles(sheetDir.toFile());
        //        } else {
        //            try {
        //                Files.createDirectories(sheetDir);
        //                logger.info("Creating directory {}", sheetDir);
        //            } catch (IOException ex) {
        //                logger.warn("Error creating dir " + sheetDir, ex);
        //            }
        //        }
        //
        //        // Now record each relevant sample
        //        int glyphNb = 0;
        //
        //        for (GlyphLayer layer : GlyphLayer.concreteValues()) {
        //            for (Sample sample : sheet.getGlyphIndex().getGlyphs(layer)) {
        //                Shape shape = getRecordableShape(sample);
        //
        //                if (shape != null) {
        //                    glyphNb += recordSample(sample, shape, sheetDir);
        //                }
        //            }
        //        }
        //
        //        // Refresh sample populations
        //        refreshBases();
        //
        //        logger.info("{} samples stored from {}", glyphNb, sheet.getId());
    }

    //--------------//
    // refreshBases //
    //--------------//
    public void refreshBases ()
    {
        wholeBase = null;
        coreBase = null;
    }

    //-------------//
    // removeGlyph //
    //-------------//
    /**
     * Remove a sample from the repository memory (this does not delete
     * the actual sample file on disk).
     * We also remove it from the various bases which is safer.
     *
     * @param gName the full sample name
     */
    public synchronized void removeGlyph (String gName)
    {
        samplesMap.remove(gName);
        refreshBases();
    }

    //-------------//
    // setCoreBase //
    //-------------//
    /**
     * Define the provided collection as the core training material.
     *
     * @param base the provided collection
     */
    public synchronized void setCoreBase (List<String> base)
    {
        coreBase = base;
    }

    //-------------//
    // shapeNameOf //
    //-------------//
    /**
     * Report the shape name of a gName.
     *
     * @param gName sample name, using format "folder/name.number.xml" or
     *              "folder/name.xml"
     * @return the 'name' part of the format
     */
    public static String shapeNameOf (String gName)
    {
        int slash = gName.indexOf('/');
        String nameWithExt = gName.substring(slash + 1);

        int firstDot = nameWithExt.indexOf('.');

        if (firstDot != -1) {
            return nameWithExt.substring(0, firstDot);
        } else {
            return nameWithExt;
        }
    }

    //---------//
    // shapeOf //
    //---------//
    /**
     * Infer the shape of a sample directly from its full name.
     *
     * @param gName the full sample name
     * @return the shape of the known sample
     */
    public Shape shapeOf (String gName)
    {
        return shapeOf(Paths.get(gName));
    }

    //---------------//
    // storeCoreBase //
    //---------------//
    /**
     * Store the core training material.
     */
    public synchronized void storeCoreBase ()
    {
        if (coreBase == null) {
            logger.warn("Core base is null");

            return;
        }

        // Create the core directory if needed
        try {
            Files.createDirectories(coreFolder);
        } catch (IOException ex) {
            logger.warn("Cannot create directory " + coreBase, ex);

            return;
        }

        // Empty the directory
        FileUtil.deleteAll(coreFolder.toFile().listFiles());

        // Copy the sample and icon files into the core directory
        int copyNb = 0;

        for (String gName : coreBase) {
            final boolean isIcon = isIcon(gName);
            final Path source = isIcon ? WellKnowns.SYMBOLS_FOLDER.resolveSibling(gName)
                    : WellKnowns.TRAIN_FOLDER.resolve(gName);

            final Path target = coreFolder.resolve(gName);

            try {
                Files.createDirectories(target.getParent());
                logger.debug("Storing {} as core", target);

                if (isIcon) {
                    Files.createFile(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }

                copyNb++;
            } catch (IOException ex) {
                logger.warn("Cannot copy {} to {}", source, target);
            }
        }

        logger.info("{} samples copied as core training material", copyNb);
    }

    //
    //    //-----------------//
    //    // unloadIconsFrom //
    //    //-----------------//
    //    public void unloadIconsFrom (List<String> names)
    //    {
    //        for (String gName : names) {
    //            if (isIcon(gName)) {
    //                if (isLoaded(gName)) {
    //                    Sample sample = getSample(gName, null);
    //
    //                    for (Section section : sample.getMembers()) {
    //                        section.clearViews();
    //                        section.delete();
    //                    }
    //                }
    //
    //                unloadSample(gName);
    //            }
    //        }
    //    }
    //
    //--------------//
    // unloadSample //
    //--------------//
    synchronized void unloadSample (String gName)
    {
        if (samplesMap.containsKey(gName)) {
            samplesMap.remove(gName);
        }
    }

    //-------------------//
    // buildSymbolSample //
    //-------------------//
    /**
     * Build an artificial sample from a symbol descriptor, in order to
     * train an evaluator even when we have no ground-truth sample.
     *
     * @param gName path to the symbol descriptor on disk
     * @return the sample built, or null if failed
     */
    private Sample buildSymbolSample (String gName)
    {
        Shape shape = shapeOf(gName);
        Sample sample = null;

        // Make sure we have the drawing available for this shape
        ShapeSymbol symbol = Symbols.getSymbol(shape);

        // If no plain symbol, use the decorated symbol as plan B
        if (symbol == null) {
            symbol = Symbols.getSymbol(shape, true);
        }

        if (symbol != null) {
            logger.debug("Building symbol sample {}", gName);

            Path path = WellKnowns.TRAIN_FOLDER.resolve(gName);

            if (Files.exists(path)) {
                try {
                    InputStream is = new FileInputStream(path.toFile());
                    SymbolGlyphDescriptor desc = SymbolGlyphDescriptor.loadFromXmlStream(is);
                    is.close();

                    logger.debug("Descriptor {}", desc);

                    sample = SymbolSample.create(shape, symbol, MusicFont.DEFAULT_INTERLINE);
                } catch (Exception ex) {
                    logger.warn("Cannot process " + path, ex);
                }
            }
        } else {
            logger.warn("No symbol for {}", gName);
        }

        return sample;
    }

    //----------------//
    // deleteXmlFiles //
    //----------------//
    private void deleteXmlFiles (File dir)
    {
        File[] files = dir.listFiles();

        for (File file : files) {
            if (FileUtil.getExtension(file).equals(FILE_EXTENSION)) {
                if (!file.delete()) {
                    logger.warn("Could not delete {}", file);
                }
            }
        }
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(Sample.class);
        }

        return jaxbContext;
    }

    //--------------------//
    // getRecordableShape //
    //--------------------//
    /**
     * Report the shape to record for the provided shape.
     *
     * @param sample the provided shape
     * @return the recordable shape to use, or null
     */
    private Shape getRecordableShape (Shape shape)
    {
        if (shape == null) {
            return null;
        }

        Shape physicalShape = shape.getPhysicalShape();

        if (physicalShape.isTrainable() && (physicalShape != Shape.NOISE)) {
            return physicalShape;
        } else {
            return null;
        }
    }

    //-------------------//
    // getSubdirectories //
    //-------------------//
    private synchronized List<Path> getSubdirectories (Path folder)
    {
        List<Path> dirs = new ArrayList<Path>();
        Path[] paths = listLegalFiles(folder);

        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                dirs.add(path);
            }
        }

        return dirs;
    }

    //-------------//
    // glyphNameOf //
    //-------------//
    /**
     * Build the full sample name (which will be the unique sample name)
     * from the file which contains the sample description.
     *
     * @param file the sample backup file
     * @return the unique sample name
     */
    private String glyphNameOf (Path file)
    {
        if (isIcon(file)) {
            return file.getParent().getFileName() + File.separator + file.getFileName();
        } else {
            return file.getParent().getParent().getFileName() + File.separator
                   + file.getParent().getFileName() + File.separator + file.getFileName();
        }
    }

    //--------//
    // isIcon //
    //--------//
    private boolean isIcon (Path file)
    {
        Path folder = file.getParent().getFileName();

        return isIconsFolder(folder);
    }

    //-------------//
    // jaxbMarshal //
    //-------------//
    private void jaxbMarshal (Sample sample,
                              OutputStream os)
            throws JAXBException, Exception
    {
        Marshaller m = getJaxbContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(sample, os);
    }

    //---------------//
    // jaxbUnmarshal //
    //---------------//
    private Sample jaxbUnmarshal (InputStream is)
            throws JAXBException
    {
        Unmarshaller um = getJaxbContext().createUnmarshaller();

        return (Sample) um.unmarshal(is);
    }

    //----------------//
    // listLegalFiles //
    //----------------//
    private Path[] listLegalFiles (Path dir)
    {
        return new BlackList(dir).listFiles(sampleFilter);
    }

    //----------//
    // loadBase //
    //----------//
    /**
     * Build the map and return the collection of samples names in a
     * collection of directories.
     *
     * @param paths   the array of paths to the directories to load
     * @param monitor the observing entity if any
     * @return the collection of loaded samples names
     */
    private synchronized List<String> loadBase (Path[] paths,
                                                Monitor monitor)
    {
        // Files in the provided directory & its subdirectories
        List<Path> files = new ArrayList<Path>(4000);

        for (Path path : paths) {
            loadDirectory(path, files);
        }

        if (monitor != null) {
            monitor.setTotalGlyphs(files.size());
        }

        // Now, collect the samples names
        List<String> base = new ArrayList<String>(files.size());

        for (Path file : files) {
            base.add(glyphNameOf(file));
        }

        logger.debug("{} samples names collected", files.size());

        return base;
    }

    //--------------//
    // loadCoreBase //
    //--------------//
    /**
     * Build the collection of only the core samples.
     *
     * @return the collection of core samples names
     */
    private List<String> loadCoreBase (Monitor monitor)
    {
        return loadBase(new Path[]{coreFolder}, monitor);
    }

    //---------------//
    // loadDirectory //
    //---------------//
    /**
     * Retrieve recursively all files in the hierarchy starting at
     * the given directory, and append them in the provided file list.
     * If a black list exists in a directory, then all black-listed files
     * (and direct sub-directories) hosted in this directory are skipped.
     *
     * @param dir the top directory where search is launched
     * @param all the list to be augmented by found files
     */
    private void loadDirectory (Path dir,
                                List<Path> all)
    {
        Path[] files = listLegalFiles(dir);

        if (files != null) {
            logger.debug("Browsing directory {} total:{}", dir, files.length);

            for (Path file : files) {
                if (Files.isDirectory(file)) {
                    loadDirectory(file, all); // Recurse through it
                } else {
                    all.add(file);
                }
            }
        } else {
            logger.warn("Directory {} is empty", dir);
        }
    }

    //------------//
    // loadSample //
    //------------//
    private Sample loadSample (String gName,
                               Path path)
    {
        logger.debug("Loading sample {}", path);

        Sample sample = null;
        InputStream is = null;

        try {
            is = new FileInputStream(path.toFile());
            sample = jaxbUnmarshal(is);
        } catch (Exception ex) {
            logger.warn("Could not unmarshal file {}", path);
            ex.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }

        return sample;
    }

    //---------------//
    // loadWholeBase //
    //---------------//
    /**
     * Build the complete map of all samples recorded so far, beginning
     * by the builtin icon samples, then the recorded samples
     * (sheets & samples).
     *
     * @return a collection of (known) samples names
     */
    private List<String> loadWholeBase (Monitor monitor)
    {
        return loadBase(
                new Path[]{WellKnowns.SYMBOLS_FOLDER, sheetsFolder, samplesFolder},
                monitor);
    }

    //--------------//
    // recordSample //
    //--------------//
    /**
     * Record a sample, using the precise shape into the given directory.
     *
     * @param sample the sample to record
     * @param dir    the target directory
     * @return 1 if OK, 0 otherwise
     */
    private int recordSample (Sample sample,
                              Path dir)
    {
        OutputStream os = null;

        try {
            logger.debug("Storing {}", sample);

            StringBuilder sb = new StringBuilder();
            sb.append(sample.shape);
            sb.append(".");
            sb.append(String.format("%04d", sample.getId()));
            sb.append(FILE_EXTENSION);

            Path glyphPath = dir.resolve(sb.toString());

            os = new FileOutputStream(glyphPath.toFile());
            jaxbMarshal(sample, os);

            return 1;
        } catch (Throwable ex) {
            logger.warn("Error storing " + sample, ex);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException ex) {
                logger.warn(null, ex);
            }
        }

        return 0;
    }

    //---------//
    // shapeOf //
    //---------//
    /**
     * Infer the shape of a sample directly from its file name.
     *
     * @param path the file that describes the sample
     * @return the shape of the known sample
     */
    private Shape shapeOf (Path path)
    {
        try {
            // ex: ONE_32ND_REST.0105.xml (for real samples)
            // ex: CODA.xml (for samples derived from icons)
            String name = FileUtil.getNameSansExtension(path);
            int dot = name.indexOf('.');

            if (dot != -1) {
                name = name.substring(0, dot);
            }

            return Shape.valueOf(name);
        } catch (Exception ex) {
            // Not recognized
            return null;
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
         * @param gName the normalized sample name
         */
        void loadedGlyph (String gName);

        /**
         * Called to pass the number of selected samples, which will be
         * later loaded.
         *
         * @param selected the size of the selection
         */
        void setSelectedGlyphs (int selected);

        /**
         * Called to pass the total number of available sample
         * descriptions in the training material.
         *
         * @param total the size of the training material
         */
        void setTotalGlyphs (int total);
    }
}
//
//    public static void main (final String[] args)
//    {
//        SampleRepository repo = SampleRepository.getInstance();
//        repo.convertWholeBase();
//    }
//
//    //------------------//
//    // convertWholeBase //
//    //------------------//
//    private void convertWholeBase ()
//    {
//        try {
//            JAXBContext context = JAXBContext.newInstance(GlyphValue.class);
//            List<String> gNames = getWholeBase(null);
//
//            for (String gName : gNames) {
//                if (isIcon(gName)) {
//                    continue;
//                }
//
//                Path path = WellKnowns.TRAIN_FOLDER.resolve(gName);
//
//                if (!Files.exists(path)) {
//                    logger.warn("Unable to find file for sample {}", gName);
//
//                    continue;
//                }
//
//                GlyphValue value = loadValue(context, gName, path);
//                Sample sample = value.toSample();
//                recordNewSample(sample, path.getParent());
//            }
//        } catch (JAXBException ex) {
//            logger.warn("JAXB exception", ex);
//        }
//    }
//
//    //-----------//
//    // loadValue //
//    //-----------//
//    private GlyphValue loadValue (JAXBContext context,
//                                  String gName,
//                                  Path path)
//    {
//        logger.debug("Loading value {}", path);
//
//        GlyphValue value = null;
//        InputStream is = null;
//
//        try {
//            is = new FileInputStream(path.toFile());
//
//            Unmarshaller um = context.createUnmarshaller();
//
//            return (GlyphValue) um.unmarshal(is);
//        } catch (Exception ex) {
//            logger.warn("Could not unmarshal file {}", path);
//            ex.printStackTrace();
//        } finally {
//            if (is != null) {
//                try {
//                    is.close();
//                } catch (Exception ignored) {
//                }
//            }
//        }
//
//        return value;
//    }
//
//    //-----------------//
//    // recordNewSample //
//    //-----------------//
//    /**
//     * Record a sample, using the precise shape into the given directory.
//     *
//     * @param sample the sample to record
//     * @param dir    the target directory
//     * @return 1 if OK, 0 otherwise
//     */
//    private int recordNewSample (Sample sample,
//                                 Path dir)
//    {
//        OutputStream os = null;
//
//        try {
//            logger.debug("Storing {}", sample);
//
//            StringBuilder sb = new StringBuilder();
//            sb.append(sample.shape);
//            sb.append(".");
//            sb.append(String.format("%04d", sample.id));
//            sb.append(FILE_EXTENSION);
//            sb.append("-new");
//
//            Path glyphPath = dir.resolve(sb.toString());
//
//            os = new FileOutputStream(glyphPath.toFile());
//            jaxbMarshal(sample, os);
//
//            return 1;
//        } catch (Throwable ex) {
//            logger.warn("Error storing " + sample, ex);
//        } finally {
//            try {
//                if (os != null) {
//                    os.close();
//                }
//            } catch (IOException ex) {
//                logger.warn(null, ex);
//            }
//        }
//
//        return 0;
//    }
