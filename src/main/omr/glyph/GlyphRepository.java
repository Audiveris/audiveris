//----------------------------------------------------------------------------//
//                                                                            //
//                       G l y p h R e p o s i t o r y                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.WellKnowns;

import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphValue;

import omr.lag.Section;

import omr.sheet.Sheet;

import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;

import omr.util.BlackList;
import omr.util.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * Class {@code GlyphRepository} handles the store of known glyphs,
 * across multiple sheets (and possibly multiple runs).
 *
 * <p> A glyph is known by its full name, whose standard format is
 * <B>sheetName/Shape.id.xml</B>, regardless of the area it is stored (this may
 * be the <I>core</I> area or the global <I>sheets</I> area augmented by the
 * <I>samples</I> area).
 * It can also be an <I>artificial</I> glyph built from a symbol icon,
 * in that case its full name is the similar formats <B>icons/Shape.xml</B> or
 * <B>icons/Shape.nn.xml</B> where "nn" is a differentiating number.
 *
 * <p> The repository handles a private map of all deserialized glyphs so far,
 * since the deserialization is a rather expensive operation.
 *
 * <p> It handles two bases : the "whole base" (all glyphs from sheets and
 * samples folders) and the "core base" (just the glyphs of the core, which is
 * built as a selected subset of the whole base).
 * These bases are accessible respectively by {@link #getWholeBase} and
 * {@link #getCoreBase} methods.
 *
 * @author Hervé Bitteur
 */
public class GlyphRepository
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            GlyphRepository.class);

    /** The single instance of this class */
    private static volatile GlyphRepository INSTANCE;

    /** Extension for training files */
    private static final String FILE_EXTENSION = ".xml";

    /** Extension for place-holder symbol files */
    public static final String SYMBOL_EXTENSION = ".symbol";

    /** Specific subdirectory for sheet glyphs */
    private static final File sheetsFolder = new File(
            WellKnowns.TRAIN_FOLDER,
            "sheets");

    /** Specific subdirectory for core glyphs */
    private static final File coreFolder = new File(
            WellKnowns.TRAIN_FOLDER,
            "core");

    /** Specific subdirectory for additional sample glyphs */
    private static final File samplesFolder = new File(
            WellKnowns.TRAIN_FOLDER,
            "samples");

    /** Specific filter for glyph files */
    private static final FileFilter glyphFilter = new FileFilter()
    {
        @Override
        public boolean accept (File file)
        {
            String ext = FileUtil.getExtension(file);

            return file.isDirectory() || ext.equals(FILE_EXTENSION)
                   || ext.equals(SYMBOL_EXTENSION);
        }
    };

    /** Un/marshalling context for use with JAXB */
    private static volatile JAXBContext jaxbContext;

    /** For comparing shape names */
    public static final Comparator<String> shapeComparator = new Comparator<String>()
    {
        @Override
        public int compare (String s1,
                            String s2)
        {
            String n1 = GlyphRepository.shapeNameOf(s1);
            String n2 = GlyphRepository.shapeNameOf(s2);

            return n1.compareTo(n2);
        }
    };

    //~ Instance fields --------------------------------------------------------
    /** Core collection of glyphs */
    private volatile List<String> coreBase;

    /** Whole collection of glyphs */
    private volatile List<String> wholeBase;

    /**
     * Map of all glyphs deserialized so far, using full glyph name as
     * key. Full glyph name format is : sheetName/Shape.id.xml
     */
    private final Map<String, Glyph> glyphsMap = new TreeMap<>();

    /** Inverse map */
    private final Map<Glyph, String> namesMap = new HashMap<>();

    //~ Constructors -----------------------------------------------------------
    /** Private singleton constructor */
    private GlyphRepository ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // fileNameOf //
    //------------//
    /**
     * Report the file name w/o extension of a gName.
     *
     * @param gName glyph name, using format "folder/name.number.xml"
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
    // shapeNameOf //
    //-------------//
    /**
     * Report the shape name of a gName.
     *
     * @param gName glyph name, using format "folder/name.number.xml" or
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

    //-------------//
    // getCoreBase //
    //-------------//
    /**
     * Return the names of the core collection of glyphs.
     *
     * @return the core collection of recorded glyphs
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

    //----------//
    // getGlyph //
    //----------//
    /**
     * Return a glyph knowing its full glyph name, which is the name of
     * the corresponding training material.
     * If not already done, the glyph is deserialized from the training file,
     * searching first in the icons area, then the train area.
     *
     * @param gName   the full glyph name (format is: sheetName/Shape.id.xml)
     * @param monitor the monitor, if any, to be kept informed of glyph loading
     * @return the glyph instance if found, null otherwise
     */
    public synchronized Glyph getGlyph (String gName,
                                        Monitor monitor)
    {
        // First, try the map of glyphs
        Glyph glyph = glyphsMap.get(gName);

        if (glyph == null) {
            // If failed, actually load the glyph from XML backup file.
            if (isIcon(gName)) {
                glyph = buildSymbolGlyph(gName);
            } else {
                File file = new File(WellKnowns.TRAIN_FOLDER, gName);

                if (!file.exists()) {
                    logger.warn("Unable to find file for glyph {}", gName);

                    return null;
                }

                glyph = buildGlyph(gName, file);
            }

            if (glyph != null) {
                glyphsMap.put(gName, glyph);
                namesMap.put(glyph, gName);
            }

            if (monitor != null) {
                monitor.loadedGlyph(gName);
            }
        }

        return glyph;
    }

    //--------------//
    // getGlyphName //
    //--------------//
    public String getGlyphName (Glyph glyph)
    {
        return namesMap.get(glyph);
    }

    //-------------//
    // getGlyphsIn //
    //-------------//
    /**
     * Report the list of glyph files that are contained within a given
     * directory
     *
     * @param dir the containing directory
     * @return the list of glyph files
     */
    public synchronized List<File> getGlyphsIn (File dir)
    {
        File[] files = listLegalFiles(dir);

        if (files != null) {
            return Arrays.asList(files);
        } else {
            logger.warn("Cannot get files list from dir {}", dir);

            return new ArrayList<>();
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
    public static GlyphRepository getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new GlyphRepository();
        }

        return INSTANCE;
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
    public List<File> getSampleDirectories ()
    {
        return getSubdirectories(samplesFolder);
    }

    //------------------//
    // getSamplesFolder //
    //------------------//
    /**
     * Report the folder where isolated samples glyphs are stored.
     *
     * @return the directory of isolated samples material
     */
    public File getSamplesFolder ()
    {
        return samplesFolder;
    }

    //---------------------//
    // getSheetDirectories //
    //---------------------//
    /**
     * Report the list of all sheet directories found in the training
     * material.
     *
     * @return the list of sheet directories
     */
    public List<File> getSheetDirectories ()
    {
        return getSubdirectories(sheetsFolder);
    }

    //-----------------//
    // getSheetsFolder //
    //-----------------//
    /**
     * Report the folder where all sheet glyphs are stored.
     *
     * @return the directory of all sheets material
     */
    public File getSheetsFolder ()
    {
        return sheetsFolder;
    }

    //--------------//
    // getWholeBase //
    //--------------//
    /**
     * Return the names of the whole collection of glyphs.
     *
     * @return the whole collection of recorded glyphs
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
        return isIcon(new File(gName));
    }

    //---------------//
    // isIconsFolder //
    //---------------//
    public boolean isIconsFolder (String folder)
    {
        return folder.equals(WellKnowns.SYMBOLS_FOLDER.getName());
    }

    //----------//
    // isLoaded //
    //----------//
    public synchronized boolean isLoaded (String gName)
    {
        return glyphsMap.get(gName) != null;
    }

    //----------------//
    // recordOneGlyph //
    //----------------//
    /**
     * Record one glyph on disk (into the samples folder).
     *
     * @param glyph the glyph to record
     * @param sheet its containing sheet
     */
    public void recordOneGlyph (Glyph glyph,
                                Sheet sheet)
    {
        Shape shape = getRecordableShape(glyph);

        if (shape != null) {
            // Prepare target directory, based on sheet id
            File sheetDir = new File(getSamplesFolder(), sheet.getId());

            // Make sure related directory chain exists
            if (sheetDir.mkdirs()) {
                logger.info("Creating directory {}", sheetDir);
            }

            if (recordGlyph(glyph, shape, sheetDir) > 0) {
                logger.info("Stored {} into {}", glyph.idString(), sheetDir);
            }
        } else {
            logger.warn("Not recordable {}", glyph);
        }
    }

    //-------------------//
    // recordSheetGlyphs //
    //-------------------//
    /**
     * Store all known glyphs of the provided sheet as separate XML
     * files, so that they can be later reloaded to train an evaluator.
     * We store glyph for which Shape is not null, and different from NOISE and
     * STEM (CLUTTER is thus stored as well).
     *
     * <p>STRUCTURE shapes are stored in a parallel sub-directory so that they
     * don't get erased by shapes of their leaves.
     *
     * @param sheet           the sheet whose glyphs are to be stored
     * @param emptyStructures flag to specify if the Structure directory must be
     *                        emptied beforehand
     */
    public void recordSheetGlyphs (Sheet sheet,
                                   boolean emptyStructures)
    {
        // Prepare target directory
        File sheetDir = new File(getSheetsFolder(), sheet.getId());

        // Make sure related directory chain exists
        if (sheetDir.mkdirs()) {
            logger.info("Creating directory {}", sheetDir);
        } else {
            deleteXmlFiles(sheetDir);
        }

        // Now record each relevant glyph
        int glyphNb = 0;

        for (Glyph glyph : sheet.getActiveGlyphs()) {
            Shape shape = getRecordableShape(glyph);

            if (shape != null) {
                glyphNb += recordGlyph(glyph, shape, sheetDir);
            }
        }

        // Refresh glyph populations
        refreshBases();

        logger.info("{} glyphs stored from {}", glyphNb, sheet.getId());
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
     * Remove a glyph from the repository memory (this does not delete
     * the actual glyph file on disk).
     * We also remove it from the various bases which is safer.
     *
     * @param gName the full glyph name
     */
    public synchronized void removeGlyph (String gName)
    {
        glyphsMap.remove(gName);
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

    //---------//
    // shapeOf //
    //---------//
    /**
     * Infer the shape of a glyph directly from its full name.
     *
     * @param gName the full glyph name
     * @return the shape of the known glyph
     */
    public Shape shapeOf (String gName)
    {
        return shapeOf(new File(gName));
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
        coreFolder.mkdirs();

        // Empty the directory
        FileUtil.deleteAll(coreFolder.listFiles());

        // Copy the glyph and icon files into the core directory
        int copyNb = 0;

        for (String gName : coreBase) {
            final boolean isIcon = isIcon(gName);
            final File source = isIcon
                    ? new File(
                    WellKnowns.SYMBOLS_FOLDER.getParentFile(),
                    gName) : new File(WellKnowns.TRAIN_FOLDER, gName);

            final File target = new File(coreFolder, gName);
            target.getParentFile().mkdirs();

            logger.debug("Storing {} as core", target);

            try {
                if (isIcon) {
                    target.createNewFile();
                } else {
                    FileUtil.copy(source, target);
                }

                copyNb++;
            } catch (IOException ex) {
                logger.warn("Cannot copy {} to {}", source, target);
            }
        }

        logger.info("{} glyphs copied as core training material", copyNb);
    }

    //-----------------//
    // unloadIconsFrom //
    //-----------------//
    public void unloadIconsFrom (List<String> names)
    {
        for (String gName : names) {
            if (isIcon(gName)) {
                if (isLoaded(gName)) {
                    Glyph glyph = getGlyph(gName, null);

                    for (Section section : glyph.getMembers()) {
                        section.clearViews();
                        section.delete();
                    }
                }

                unloadGlyph(gName);
            }
        }
    }

    //-------------//
    // unloadGlyph //
    //-------------//
    synchronized void unloadGlyph (String gName)
    {
        if (glyphsMap.containsKey(gName)) {
            glyphsMap.remove(gName);
        }
    }

    //------------//
    // buildGlyph //
    //------------//
    private Glyph buildGlyph (String gName,
                              File file)
    {
        logger.debug("Loading glyph {}", file);

        Glyph glyph = null;
        InputStream is = null;

        try {
            is = new FileInputStream(file);
            glyph = jaxbUnmarshal(is);
        } catch (Exception ex) {
            logger.warn("Could not unmarshal file {}", file);
            ex.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }

        return glyph;
    }

    //------------------//
    // buildSymbolGlyph //
    //------------------//
    /**
     * Build an artificial glyph from a symbol descriptor, in order to
     * train an evaluator even when we have no ground-truth glyph.
     *
     * @param gName path to the symbol descriptor on disk
     * @return the glyph built, or null if failed
     */
    private Glyph buildSymbolGlyph (String gName)
    {
        Shape shape = shapeOf(gName);
        Glyph glyph = null;

        // Make sure we have the drawing available for this shape
        ShapeSymbol symbol = Symbols.getSymbol(shape);

        // If no plain symbol, use the decorated symbol as plan B
        if (symbol == null) {
            symbol = Symbols.getSymbol(shape, true);
        }

        if (symbol != null) {
            logger.debug("Building symbol glyph {}", gName);

            File file = new File(WellKnowns.TRAIN_FOLDER, gName);

            if (file.exists()) {
                try {
                    InputStream is = new FileInputStream(file);
                    SymbolGlyphDescriptor desc = SymbolGlyphDescriptor.
                            loadFromXmlStream(
                            is);
                    is.close();

                    logger.debug("Descriptor {}", desc);

                    glyph = new SymbolGlyph(
                            shape,
                            symbol,
                            MusicFont.DEFAULT_INTERLINE,
                            desc);
                } catch (Exception ex) {
                    logger.warn("Cannot process " + file, ex);
                }
            }
        } else {
            //if (logger.isDebugEnabled()) {
            logger.warn("No symbol for {}", gName);

            //}
        }

        return glyph;
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
            jaxbContext = JAXBContext.newInstance(GlyphValue.class);
        }

        return jaxbContext;
    }

    //--------------------//
    // getRecordableShape //
    //--------------------//
    /**
     * Report the shape to record for the provided glyph.
     *
     * @param glyph the provided glyph
     * @return the precise shape to use, or null
     */
    private Shape getRecordableShape (Glyph glyph)
    {
        if ((glyph == null) || glyph.isVirtual() || (glyph.getShape() == null)) {
            return null;
        }

        Shape shape = glyph.getShape().getPhysicalShape();

        if (shape.isTrainable() && (shape != Shape.NOISE)) {
            return shape;
        } else {
            return null;
        }
    }

    //-------------------//
    // getSubdirectories //
    //-------------------//
    private synchronized List<File> getSubdirectories (File folder)
    {
        List<File> dirs = new ArrayList<>();
        File[] files = listLegalFiles(folder);

        for (File file : files) {
            if (file.isDirectory()) {
                dirs.add(file);
            }
        }

        return dirs;
    }

    //-------------//
    // glyphNameOf //
    //-------------//
    /**
     * Build the full glyph name (which will be the unique glyph name)
     * from the file which contains the glyph description.
     *
     * @param file the glyph backup file
     * @return the unique glyph name
     */
    private String glyphNameOf (File file)
    {
        if (isIcon(file)) {
            return file.getParentFile().getName() + File.separator + file.
                    getName();
        } else {
            return file.getParentFile().getParentFile().getName() + File.separator
                   + file.getParentFile().getName() + File.separator + file.
                    getName();
        }
    }

    //--------//
    // isIcon //
    //--------//
    private boolean isIcon (File file)
    {
        String folder = file.getParentFile().getName();

        return isIconsFolder(folder);
    }

    //-------------//
    // jaxbMarshal //
    //-------------//
    private void jaxbMarshal (Glyph glyph,
                              OutputStream os)
            throws JAXBException, Exception
    {
        Marshaller m = getJaxbContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(new GlyphValue(glyph), os);
    }

    //---------------//
    // jaxbUnmarshal //
    //---------------//
    private Glyph jaxbUnmarshal (InputStream is)
            throws JAXBException
    {
        Unmarshaller um = getJaxbContext().createUnmarshaller();
        GlyphValue value = (GlyphValue) um.unmarshal(is);

        return new BasicGlyph(value);
    }

    //----------------//
    // listLegalFiles //
    //----------------//
    private File[] listLegalFiles (File dir)
    {
        return new BlackList(dir).listFiles(glyphFilter);
    }

    //----------//
    // loadBase //
    //----------//
    /**
     * Build the map and return the collection of glyphs names in a
     * collection of directories.
     *
     * @param paths   the array of paths to the directories to load
     * @param monitor the observing entity if any
     * @return the collection of loaded glyphs names
     */
    private synchronized List<String> loadBase (File[] paths,
                                                Monitor monitor)
    {
        // Files in the provided directory & its subdirectories
        List<File> files = new ArrayList<>(4000);

        for (File path : paths) {
            loadDirectory(path, files);
        }

        if (monitor != null) {
            monitor.setTotalGlyphs(files.size());
        }

        // Now, collect the glyphs names
        List<String> base = new ArrayList<>(files.size());

        for (File file : files) {
            base.add(glyphNameOf(file));
        }

        logger.debug("{} glyphs names collected", files.size());

        return base;
    }

    //--------------//
    // loadCoreBase //
    //--------------//
    /**
     * Build the collection of only the core glyphs.
     *
     * @return the collection of core glyphs names
     */
    private List<String> loadCoreBase (Monitor monitor)
    {
        return loadBase(new File[]{coreFolder}, monitor);
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
    private void loadDirectory (File dir,
                                List<File> all)
    {
        File[] files = listLegalFiles(dir);

        logger.debug("Browsing directory {} total:{}", dir, files.length);

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    loadDirectory(file, all); // Recurse through it
                } else {
                    all.add(file);
                }
            }
        } else {
            logger.warn("Directory {} is empty", dir);
        }
    }

    //---------------//
    // loadWholeBase //
    //---------------//
    /**
     * Build the complete map of all glyphs recorded so far, beginning
     * by the builtin icon glyphs, then the recorded glyphs
     * (sheets & samples).
     *
     * @return a collection of (known) glyphs names
     */
    private List<String> loadWholeBase (Monitor monitor)
    {
        return loadBase(
                new File[]{WellKnowns.SYMBOLS_FOLDER, sheetsFolder,
                           samplesFolder},
                monitor);
    }

    //-------------//
    // recordGlyph //
    //-------------//
    /**
     * Record a glyph, using the precise shape into the given directory.
     *
     * @param glyph the glyph to record
     * @param shape the precise shape to use
     * @param dir   the target directory
     * @return 1 if OK, 0 otherwise
     */
    private int recordGlyph (Glyph glyph,
                             Shape shape,
                             File dir)
    {
        OutputStream os = null;

        try {
            logger.debug("Storing {}", glyph);

            StringBuilder sb = new StringBuilder();
            sb.append(shape);
            sb.append(".");
            sb.append(String.format("%04d", glyph.getId()));
            sb.append(FILE_EXTENSION);

            File glyphFile;

            glyphFile = new File(dir, sb.toString());

            os = new FileOutputStream(glyphFile);
            jaxbMarshal(glyph, os);

            return 1;
        } catch (Throwable ex) {
            logger.warn("Error storing " + glyph, ex);
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
     * Infer the shape of a glyph directly from its file name.
     *
     * @param file the file that describes the glyph
     * @return the shape of the known glyph
     */
    private Shape shapeOf (File file)
    {
        try {
            // ex: ONE_32ND_REST.0105.xml (for real glyphs)
            // ex: CODA.xml (for glyphs derived from icons)
            String name = FileUtil.getNameSansExtension(file);
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

    //~ Inner Interfaces -------------------------------------------------------
    //---------//
    // Monitor //
    //---------//
    /**
     * Interface {@code Monitor} defines the entries to a UI entity
     * which monitors the loading of glyphs by the glyph repository.
     */
    public static interface Monitor
    {
        //~ Methods ------------------------------------------------------------

        /**
         * Called whenever a new glyph has been loaded.
         *
         * @param gName the normalized glyph name
         */
        void loadedGlyph (String gName);

        /**
         * Called to pass the number of selected glyphs, which will be
         * later loaded.
         *
         * @param selected the size of the selection
         */
        void setSelectedGlyphs (int selected);

        /**
         * Called to pass the total number of available glyph
         * descriptions in the training material.
         *
         * @param total the size of the training material
         */
        void setTotalGlyphs (int total);
    }
}
