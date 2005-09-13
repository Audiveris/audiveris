//-----------------------------------------------------------------------//
//                                                                       //
//                     G l y p h R e p o s i t o r y                     //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//


package omr.glyph.ui;

import omr.Main;
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
import omr.glyph.ui.GlyphRepository.Monitor;

/**
 * Class <code>GlyphRepository</code> handles the store of known glyphs,
 * across multiple sheets (and possibly multiple runs).
 *
 * <p> A glyph is known by its full name, whose format is
 * <B>sheetName/Shape.id.xml</B>, regardless of the area it is stored (this
 * may be the <I>core</I> area or the global <I>sheets</I> area)
 *
 * <p> The repository handles a private map of all deserialized glyphs so
 * far, since the deserialization is a rather expensive operation.
 *
 * <p> It handles two bases : the "whole base" (all glyphs) and the "core
 * base" (just the glyphs of the core) which are accessible by {@link
 * #getWholeBase} and {@link #getCoreBase} methods.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class GlyphRepository
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(GlyphRepository.class);

    // The single instance of this class
    private static GlyphRepository INSTANCE;

    // Extension for training files
    private static final String FILE_EXTENSION = ".xml";

    // Specific subdirectories for training files
    private static final File sheetsPath = new File(Main.getTrainPath(),
                                                    "sheets");
    private static final File corePath   = new File(Main.getTrainPath(),
                                                    "core");

    //~ Instance variables ------------------------------------------------

    // Specific glyph Castor mapper
    private XmlMapper<Glyph> glyphMapper;

    // Map of all glyphs deserialized so far, using full glyph name as key
    // Full glyph name format is :  sheetName/Shape.id.xml
    private Map<String,Glyph> glyphsMap = new TreeMap<String,Glyph>();

    // Whole collection of glyphs
    private Collection<String> wholeBase;

    // Core collection of glyphs
    private Collection<String> coreBase;

    //~ Constructors ------------------------------------------------------

    // Private (singleton)
    private GlyphRepository()
    {
    }

    //~ Methods -----------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class, after creating it if
     * needed
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

    //-------------//
    // getCoreBase //
    //-------------//
    /**
     * Return the names of the core collection of glyphs
     *
     * @return the core collection of recorded glyphs
     */
    synchronized Collection<String> getCoreBase (Monitor monitor)
    {
        if (coreBase == null) {
            coreBase = loadCoreBase(monitor);
        }

        return coreBase;
    }

    //-------------//
    // getCorePath //
    //-------------//
    /**
     * Report the directory where selected core glyphs are stored
     *
     * @return the directory of core material
     */
    static File getCorePath()
    {
        return corePath;
    }

    //----------//
    // getGlyph //
    //----------//
    /**
     * Return a glyph knowing its full glyph name, which is the name of the
     * corresponding training material. If not already done, the glyph is
     * deserialized from the training file, searching first in the core
     * area, then in the global sheets area.
     *
     * @param gName the full glyph name (format is: sheetName/Shape.id.xml)
     *
     * @return the glyph instance if found, null otherwise
     */
    Glyph getGlyph (String gName)
    {
        // First, try the map of glyphs
        Glyph glyph = glyphsMap.get(gName);

        // If failed, actually load the glyph from XML Castor file
        if (glyph == null) {
            // We try to load from the core repository first, then from the
            // sheets repository
            File file;
            file = new File(getCorePath(), gName);
            if (!file.exists()) {
                file = new File(getSheetsPath(), gName);
                if (!file.exists()) {
                    logger.error("Unable to find file for glyph " + gName);
                    return null;
                }
            }

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

    //---------------------//
    // getSheetDirectories //
    //---------------------//
    /**
     * Report the list of all sheet directories found in the training
     * material
     *
     * @return the list of sheet directories
     */
    synchronized List<File> getSheetDirectories()
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
    synchronized List<File> getSheetGlyphs (File dir)
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
    // getSheetsPath //
    //---------------//
    /**
     * Report the directory where all sheet glyphs are stored
     *
     * @return the directory of all sheets material
     */
    static File getSheetsPath()
    {
        return sheetsPath;
    }

    //--------------//
    // getWholeBase //
    //--------------//
    /**
     * Return the names of the whole collection of glyphs
     *
     * @return the whole collection of recorded glyphs
     */
    synchronized Collection<String> getWholeBase (Monitor monitor)
    {
        if (wholeBase == null) {
            wholeBase = loadWholeBase(monitor);
        }

        return wholeBase;
    }

    //--------------------//
    // preloadGlyphMapper //
    //--------------------//
    /**
     * Allows to pre-load (in the background) the Castor mapper, so that it
     * will be immediately available when an actual glyph load is requested
     */
    void preloadGlyphMapper ()
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

    //-------------------//
    // recordSheetGlyphs //
    //-------------------//
    /**
     * Store all known glyphs of the provided sheet as separate XML files,
     * so that they can be later re-loaded to train an evaluator. We store
     * glyph for which Shape is not null, and different from Noise and Stem
     * (Clutter is thus stored as well).
     *
     * @param sheet the sheet whose glyphs are to be stored
     */
    void recordSheetGlyphs (Sheet sheet)
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
                    Shape shape = glyph.getShape();
                    if (shape != null &&
                        shape != Shape.NOISE &&
                        shape != Shape.COMBINING_STEM) {

                        if (logger.isDebugEnabled()) {
                            logger.debug("Storing " + glyph);
                        }

                        int y = glyph.getMembers().get(0).getStart();
                        glyph.setInterline
                            (system.getStaveAtY(y).getScale().interline());

                        // Build the proper glyph file
                        StringBuffer sb = new StringBuffer();
                        sb.append(shape);
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

    //-------------//
    // removeGlyph //
    //-------------//
    /**
     * Remove a glyph from the repository memory (this does not delete the
     * actual glyph file on disk). We also remove it from the various bases
     * which is safer
     *
     * @param gName the full glyph name
     */
    synchronized void removeGlyph (String gName)
    {
        glyphsMap.remove(gName);
        if (wholeBase != null) {
            wholeBase.remove(gName);
        }
        if (coreBase != null) {
            coreBase.remove(gName);
        }
    }

    //-------------//
    // setCoreBase //
    //-------------//
    /**
     * Define the provided collection as the core training material
     * @param base the provided collection
     */
    synchronized void setCoreBase (Collection<String> base)
    {
        coreBase = base;
    }

    //---------//
    // shapeOf //
    //---------//
    /**
     * Infer the shape of a glyph directly from its full name
     *
     * @param gName the full glyph name
     * @return the shape of the known glyph
     */
    Shape shapeOf (String gName)
    {
        return shapeOf(new File(gName));
    }

    //---------------//
    // storeCoreBase //
    //---------------//
    /**
     * Store the core training material
     */
    synchronized void storeCoreBase ()
    {
        if (coreBase == null) {
            logger.error("Core base is null");
            return;
        }

        // Create the core directory if needed
        File coreDir = getCorePath();
        coreDir.mkdirs();

        // Empty the directory
        FileUtil.deleteAll(coreDir.listFiles());

        // Copy the glyph files into the core directory
        for (String gName : coreBase) {
            File source = new File(getSheetsPath(), gName);
            File targetDir = new File(coreDir,
                                      source.getParentFile().getName());
            targetDir.mkdirs();
            File target = new File(targetDir, source.getName());
            if (logger.isDebugEnabled()) {
                logger.debug("Storing " + gName + " as core");
            }

            try {
                FileUtil.copy(source, target);
            } catch (IOException ex) {
                logger.error("Cannot copy " + source + " to " + target);
            }
        }

        logger.info(coreBase.size() +
                    " glyphs copied as core training material");
    }
    //~ Methods Private ---------------------------------------------------

    //----------------//
    // getGlyphMapper //
    //----------------//
    private synchronized XmlMapper<Glyph> getGlyphMapper ()
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

    //-------------//
    // glyphNameOf //
    //-------------//
    /**
     * Build the full glyph name (which will be the unique glyph name) from
     * the file which contains the glyph description
     *
     * @param file the glyph backup file
     * @return the unique glyph name
     */
    private static String glyphNameOf (File file)
    {
        return file.getParentFile().getName() +
            File.separator + file.getName();
    }

    //----------//
    // loadBase //
    //----------//
    /**
     * Build the map and return the collection of glyphs names in a given
     * directory
     *
     * @param path the path of the directory to load
     * @param monitor the observing entity if any
     * @return the collection of loaded glyphs names
     */
    private synchronized Collection<String> loadBase (File    path,
                                                      Monitor monitor)
    {
        // Files in the provided directory & its subdirectories
        List<File> files = new ArrayList<File>(1000);
        loadDirectory(path, files);
        if (monitor != null) {
            monitor.setTotalGlyphs(files.size());
        }

        // Now, actually load the related glyphs if needed
        Collection<String> base = new ArrayList<String>(1000);
        for (File file : files) {
            String gName = glyphNameOf(file);
            base.add(gName);
            Glyph glyph = getGlyph(gName);
            if (monitor != null) {
                monitor.loadedGlyph(glyph);
            }
        }

        return base;
    }

    //---------------//
    // loadDirectory //
    //---------------//
    private void loadDirectory (File dir,
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

    //--------------//
    // loadCoreBase //
    //--------------//
    /**
     * Build the collection of only the core glyphs
     *
     * @return the collection of core glyphs names
     */
    private Collection<String> loadCoreBase (Monitor monitor)
    {
        return loadBase(getCorePath(), monitor);
    }

    //---------------//
    // loadWholeBase //
    //---------------//
    /**
     * Build the complete map of all glyphs recorded so far
     *
     * @return a collection of (known) glyphs names
     */
    private Collection<String> loadWholeBase (Monitor monitor)
    {
        return loadBase(getSheetsPath(), monitor);
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
    private Shape shapeOf (File file)
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
}
