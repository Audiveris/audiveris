//-----------------------------------------------------------------------//
//                                                                       //
//                     G l y p h R e p o s i t o r y                     //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$

package omr.glyph.ui;

import omr.Main;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.glyph.Evaluator;
import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.stick.StickSection;
import omr.util.FileUtil;
import omr.util.Logger;
import omr.util.XmlMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collection;
import java.io.IOException;

/**
 * Class <code>GlyphRepository</code> handles the store of known glyphs,
 * across multiple sheets (and possibly multiple runs)
 */
public class GlyphRepository
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(GlyphRepository.class);
    private static final Constants constants = new Constants();

    // Extension for training files
    private static final String FILE_EXTENSION = ".xml";

    // Specific subdirectories for training files
    private static File sheetsPath;
    private static File corePath;

    // Specific glyph Castor mapper
    private static XmlMapper<Glyph> glyphMapper;

    // Map of glyphs
    private static Map<String,Glyph> glyphsMap
        = new TreeMap<String,Glyph>();

    // Last collection of glyphs names
    private static Collection<String> base;

    //~ Methods -----------------------------------------------------------

    //----------//
    // isLoaded //
    //----------//
    /**
     * Check whether a glyph is already loaded
     *
     * @param gName the glyph file name
     * @return true if loaded
     */
    public static boolean isLoaded (String gName)
    {
        return glyphsMap.get(gName) != null;
    }

    //----------//
    // getGlyph //
    //----------//
    /**
     * Return a glyph knowing the name of the corresponding training
     * material. If not already loaded, the glyph is deserialized from the
     * training file.
     *
     * @param gName the glyph file name (the current format is:
     * sheetName/Shape.id.xml)
     *
     * @return the glyph instance
     */
    public static Glyph getGlyph (String gName)
    {
        // First, try the map of glyphs
        Glyph glyph = glyphsMap.get(gName);

        // If failed, actually load the glyph from XML Castor file
        if (glyph == null) {
            File file = new File(getSheetsPath(), gName);
            if (logger.isDebugEnabled()) {
                logger.debug("Reading " + file);
            }
            try {
                glyph = getGlyphMapper().load(file);
                glyphsMap.put(gName, glyph);
            } catch (Exception ex) {
                // User already informed
            }
        }

        return glyph;
    }

    //-------------//
    // removeGlyph //
    //-------------//
    /**
     * Remove a glyph from the repository memory (this does not delete the
     * actual glyph file on disk)
     *
     * @param gName the glyph file name
     */
    public static void removeGlyph (String gName)
    {
        glyphsMap.remove(gName);
    }

    //--------------//
    // getGlyphBase //
    //--------------//
    /**
     * Return the names of selected collection of glyphs
     *
     * @return the map of (known) glyphs
     */
    public static synchronized
        Collection<String> getGlyphBase (Monitor monitor)
    {
        if (base == null) {
            loadGlyphBase(monitor);
        }

        return base;
    }

    //---------------//
    // getSheetsPath //
    //---------------//
    /**
     * Report the directory where all sheet glyphs are stored
     *
     * @return the directory of all sheets material
     */
    public static File getSheetsPath()
    {
        if (sheetsPath == null) {
            sheetsPath = new File(Main.getTrainPath(), "sheets");
        }

        return sheetsPath;
    }

    //-------------//
    // getCorePath //
    //-------------//
    /**
     * Report the directory where selected core glyphs are stored
     *
     * @return the directory of core material
     */
    public static File getCorePath()
    {
        if (corePath == null) {
            corePath = new File(Main.getTrainPath(), "core");
        }

        return corePath;
    }

    //---------------------//
    // getSheetDirectories //
    //---------------------//
    /**
     * Report the list of all sheet directories found in the training
     * material
     *
     * @return the list of sheet directories
     */
    public static synchronized List<File> getSheetDirectories()
    {
        // One directory per sheet
        List<File> dirs = new ArrayList<File>();

        File[] files = getSheetsPath().listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                dirs.add(file);
            }
        }

        return dirs;
    }

    //----------------//
    // getSheetGlyphs //
    //----------------//
    /**
     * Report the list of glyph files that are contained within a given
     * directory
     *
     * @param dir the containing directory
     * @return the list of glyph files
     */
    public static synchronized List<File> getSheetGlyphs (File dir)
    {
        List<File> glyphFiles = new ArrayList<File>();
        File[] files = dir.listFiles();

        for (File file : files) {
            if (FileUtil.getExtension(file).equals(FILE_EXTENSION)) {
                logger.debug("Adding " + file);
                glyphFiles.add(file);
            }
        }

        return glyphFiles;
    }

    //---------------//
    // loadGlyphBase //
    //---------------//
    /**
     * Build a map of glyphs, by selecting part of the stored
     * population of glyphs.
     *
     * @return a collection of (known) glyphs names
     */
    public static synchronized
        Collection<String> loadGlyphBase (Monitor monitor)
    {
        // Files in the train directory & its subdirectories
        List<File> files = new ArrayList<File>(1000);
        loadDirectory(getSheetsPath(), files);
        if (monitor != null) {
            monitor.setTotalGlyphs(files.size());
        }

        // Make a selection of the files (w/o loading the glyphs)
        files = selectFiles(files);
        if (monitor != null) {
            monitor.setSelectedGlyphs(files.size());
        }

        // Now, actually load the related glyphs when needed
        base = new ArrayList<String>();
        for (File file : files) {
            String gName = file.getParentFile().getName() +
                File.separator + file.getName();
            base.add(gName);
            Glyph glyph = getGlyph(gName);

            if (monitor != null) {
                monitor.loadedGlyph(glyph);
            }
        }

        return base;
    }

    //----------------//
    // storeGlyphBase //
    //----------------//
    /**
     * Store the current glyph selection as the core training material
     */
    public static void storeGlyphBase ()
    {
        // Create the core directory if needed
        File coreDir = getCorePath();
        coreDir.mkdirs();

        // Empty the directory
        FileUtil.deleteAll(coreDir.listFiles());

        // Copy the glyph files into the core directory
        for (String fullName : base) {
            File source = new File(getSheetsPath(), fullName);
            File targetDir = new File(coreDir,
                                      source.getParentFile().getName());
            targetDir.mkdirs();
            File target = new File(targetDir, source.getName());
            if (logger.isDebugEnabled()) {
                logger.debug("Storing " + fullName + " as core");
            }

            try {
                FileUtil.copy(source, target);
            } catch (IOException ex) {
                logger.error("Cannot copy " + source + " to " + target);
            }
        }

        logger.info(base.size() + " glyphs copied as core training material");
    }

    //-------------------//
    // recordSheetGlyphs //
    //-------------------//
    /**
     * Store all known glyphs of the provided sheet as separate XML files,
     * so that they can be later re-loaded to train an evaluator
     *
     * @param sheet the sheet whose glyphs are to be stored
     */
    public static void recordSheetGlyphs (Sheet sheet)
    {
        try {
            // Prepare target directory
            File sheetDir = new File(getSheetsPath(), sheet.getName());
            if (!sheetDir.exists()) {
                // Make sure related directory chain exists
                logger.info("Creating directory " + sheetDir);
                sheetDir.mkdirs();
            } else {
                // Empty the directory from previous xml data
                File[] files = sheetDir.listFiles();
                for (File file : files) {
                    if (FileUtil.getExtension(file).equals(FILE_EXTENSION)) {
                        file.delete();
                    }
                }
            }

            int glyphNb = 0;
            for (SystemInfo system : sheet.getSystems()) {
                for (Glyph glyph : system.getGlyphs()) {
                    if (glyph.isKnown() &&
                        glyph.getShape() != Shape.COMBINING_STEM) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Storing " + glyph);
                        }

                        int y = glyph.getMembers().get(0).getStart();
                        glyph.setInterline
                            (system.getStaveAtY(y).getScale().interline());

                        // Build the proper glyph file
                        StringBuffer sb = new StringBuffer();
                        sb.append(glyph.getShape());
                        sb.append(".");
                        sb.append(String.format("%04d", glyph.getId()));
                        sb.append(FILE_EXTENSION);
                        File glyphFile = new File(sheetDir, sb.toString());

                        // Store the glyph
                        getGlyphMapper().store(glyph, glyphFile);
                        glyphNb++;
                    }
                }
            }
            logger.info(glyphNb + " glyphs stored from " + sheet.getName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //---------------//
    // setMaxSimilar //
    //---------------//
    /**
     * Modify the maximum number of glyph samples for the same shape
     *
     * @param maxSimilar upper limit for glyphs in the same shape
     */
    static void setMaxSimilar (int maxSimilar)
    {
        constants.maxSimilar.setValue(maxSimilar);
    }

    //---------------//
    // getMaxSimilar //
    //---------------//
    /**
     * Report the current limit on number of glyphs accepted for training
     * within the same shape
     *
     * @return the upper limit
     */
    static int getMaxSimilar ()
    {
        return constants.maxSimilar.getValue();
    }

    //--------------------//
    // setMaxSimilarRatio //
    //--------------------//
    /**
     * Modify the upper limit for glyphs with the same shape, expressed as
     * the ratio with regard to the total number of samples in the training
     * material
     *
     * @param maxSimilarRatio upper ratio limit
     */
    static void setMaxSimilarRatio (double maxSimilarRatio)
    {
        constants.maxSimilarRatio.setValue(maxSimilarRatio);
    }

    //--------------------//
    // getMaxSimilarRatio //
    //--------------------//
    /**
     * Report the upper ratio limit for glyphs with the same shape
     *
     * @return the upper ratio limit
     */
    static double getMaxSimilarRatio ()
    {
        return constants.maxSimilarRatio.getValue();
    }

    //----------------//
    // getGlyphMapper //
    //----------------//
    private static synchronized XmlMapper<Glyph> getGlyphMapper ()
        throws Exception
    {
        if (glyphMapper == null) {
            try {
                glyphMapper = new XmlMapper<Glyph>
                        ("/config/castor-glyph-mapping.xml");
                if (logger.isDebugEnabled()) {
                    logger.debug("Glyph XmlMapper loaded.");
                }
            } catch (Exception ex) {
                // User has already been notified
                throw ex;
            }
        }

        return glyphMapper;
    }

    //--------------------//
    // preloadGlyphMapper //
    //--------------------//
    /**
     * Allows to pre-load (in the background) the Castor mapper, so that it
     * will be immediately available when an actual glyph load is requested
     */
    public static void preloadGlyphMapper ()
    {
        class Worker
            extends Thread
        {
            public void run()
            {
                try {
                    glyphMapper = getGlyphMapper();
                    logger.info("Glyph XmlMapper preloaded.");
                } catch (Exception ex) {
                    logger.error("Could not preload Glyph XmlMapper");
                }
            }
        }

        Worker worker = new Worker();
        worker.setPriority(Thread.MIN_PRIORITY);
        worker.start();
    }

    //---------------//
    // loadDirectory //
    //---------------//
    private static void loadDirectory (File dir,
                                       List<File> all)
    {
        logger.debug("Recursing through " + dir);
        File[] files = dir.listFiles();
        if (files == null) {
            logger.warning("Directory " + dir + " not found");
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Recurse through it
                loadDirectory(file, all);
            } else {
                if (FileUtil.getExtension(file).equals(FILE_EXTENSION)) {
                    logger.debug("Adding " + file);
                    all.add(file);
                }
            }
        }
    }

    //-------------//
    // selectFiles //
    //-------------//
    /**
     * Make a fair selection among glyph files
     *
     * @param files [input/output] the file list to be modified
     *
     * @return an expurged list of files
     */
    private static List<File> selectFiles (List<File> files)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Raw list of " + files.size() + " files");
        }

        // Allocate shape-based buckets
        List<List<File>> buckets = new ArrayList<List<File>>();
        for (int i = 0; i < Evaluator.outSize; i++) {
            buckets.add(new ArrayList<File>(100));
        }

        // Fill the various buckets
        for (File file : files) {
            Shape shape = shapeOf(file);
            if (shape != null) {
                try {
                    buckets.get(shape.ordinal()).add(file);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    logger.info("Curious Shape=" + shape);
                }
            }
        }

        final int maxSimilar = Math.min
            ((int) (files.size() * constants.maxSimilarRatio.getValue()),
             constants.maxSimilar.getValue());

        // Print out the size of each bucket
        if (logger.isDebugEnabled()) {
            System.out.println("Buckets: (maxSimilar=" + maxSimilar + ")");
            for (int i = 0; i < Evaluator.outSize; i++) {
                System.out.printf("%30s -> %4d\n",
                                  Shape.values()[i],
                                  buckets.get(i).size());
            }
        }

        // Limit the number in each bucket
        List<File> base = new ArrayList<File>(1000);
        for (List<File> list : buckets) {
            Collections.shuffle(list);
            int k = 0;
            for (File file : list) {
                if (k++ < maxSimilar) {
                    base.add(file);
                }
            }
        }

        logger.info("Loading " + base.size() + " glyph files among "
                    + files.size() + " ...");

        return base;
    }

    //---------//
    // shapeOf //
    //---------//
    /**
     * Infer the shape of a glyph directly from its file name
     *
     * @param file the file that describes the glyph
     * @return the shape of the known glyph
     */
    private static Shape shapeOf (File file)
    {
        try {
            // ex: THIRTY_SECOND_REST.0105.xml
            String name = FileUtil.getNameSansExtension(file);
            int dot = name.indexOf(".");
            if (dot != -1) {
                name = name.substring(0, dot);
            }

            return Shape.valueOf(name);
        } catch (Exception ex) {
            // Not recognized
            return null;
        }

    }

    //~ Classes -----------------------------------------------------------

    //---------//
    // Monitor //
    //---------//
    /**
     * Interface <code>Monitor</code> defines the entries to a UI entity
     * which monitors the loading of glyphs by the glyph repository
     *
     */
    static interface Monitor
    {
        /**
         * Called whenever a new glyph has been loaded
         *
         * @param glyph the glyph just loaded
         */
        void loadedGlyph (Glyph glyph);

        /**
         * Called to pass the number of selected glyphs, which will be
         * later loaded
         *
         * @param selected the size of the selection
         */
        void setSelectedGlyphs (int selected);

        /**
         * Called to pass the total number of available glyph descriptions
         * in the training material
         *
         * @param total the size of the training material
         */
        void setTotalGlyphs (int total);
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        Constant.Double maxSimilarRatio = new Constant.Double
                (0.05,
                 "Maximum ratio of instances for the same shape used in training");

        Constant.Integer maxSimilar = new Constant.Integer
                (100,
                 "Absolute maximum number of instances for the same shape used in training");

        Constants ()
        {
            initialize();
        }
    }
}
