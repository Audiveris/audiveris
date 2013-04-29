//----------------------------------------------------------------------------//
//                                                                            //
//                            J a r E x p a n d e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Class {@code JarExpander} handles the installation of items from a
 * .jar archive.
 * This is targeted to complement the use of .jar archive for which some
 * directories need to be "browsable" and thus expanded as directories and files
 * to a given location prior to their browsing.
 *
 * @author Hervé Bitteur
 */
public class JarExpander
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            JarExpander.class);

    /** Jar file. */
    private final JarFile jar;

    /** Source folder. */
    private final String sourceFolder;

    /** Precise target folder. */
    private final File targetFolder;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // JarExpander //
    //-------------//
    /**
     * Create an JarExpander object for a given folder.
     *
     * @param jar          the jar file to extract data from
     * @param sourceFolder name of source folder
     * @param targetFolder precise target folder
     */
    public JarExpander (final JarFile jar,
                        final String sourceFolder,
                        final File targetFolder)
    {
        this.jar = jar;
        this.sourceFolder = sourceFolder;
        this.targetFolder = targetFolder;

        logger.debug("From jar {} expanding {} entries to folder {}",
                jar.getName(), sourceFolder, targetFolder);
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // install //
    //---------//
    /**
     * Install, if needed, the desired folder.
     *
     * @return the number of entries actually written
     */
    public int install ()
    {
        // Retrieve source entries in jar file
        List<String> sources = getSourceEntries();
        if (sources.isEmpty()) {
            logger.warn("No sources for folder {}", sourceFolder);
            return 0;
        }

        // Compare file by file, and update if necessary
        int copied = 0; // Number of entries copied
        for (String source : sources) {
            copied += process(source);
        }

        if (copied != 0) {
            logger.info("Installation of {} folder. {} entries copied to {}",
                    sourceFolder, copied, targetFolder);
        }

        return copied;
    }

    //------------------//
    // getSourceEntries //
    //------------------//
    /**
     * Retrieve all entries from the jar file that match the folder
     * to copy
     *
     * @return the list of entries found, perhaps empty but not null
     */
    private List<String> getSourceEntries ()
    {
        List<String> found = new ArrayList<>();
        Enumeration<JarEntry> entries = jar.entries();
        String prefix = sourceFolder.endsWith("/")
                ? sourceFolder : sourceFolder + "/";
        logger.debug("Prefix is {}", prefix);

        while (entries.hasMoreElements()) {
            String entry = entries.nextElement().getName();
            if (entry.startsWith(prefix)) {
                logger.trace("Found {}", entry);
                found.add(entry);
            } else {
                logger.trace("Skipping {}", entry);
            }
        }

        return found;
    }

    //---------//
    // process //
    //---------//
    /**
     * Process one entry.
     *
     * @param source the source entry to check and install
     * @return 1 if actually copied, 0 otherwise
     */
    private int process (String source)
    {
        ///logger.debug("Processing source {}", source);
        String sourcePath = source.substring(sourceFolder.length() + 1);
        Path target = Paths.get(targetFolder.toString(), sourcePath);

        try {
            if (source.endsWith("/")) {
                // This is a directory
                if (Files.exists(target)) {
                    if (!Files.isDirectory(target)) {
                        logger.warn("Existing non directory {}", target);
                    } else {
                        logger.trace("Directory {} exists", target);
                    }
                    return 0;
                } else {
                    Files.createDirectories(target);
                    logger.trace("Created dir {}", target);
                    return 1;
                }
            } else {
                ZipEntry entry = jar.getEntry(source);
                
                // Target exists?
                if (Files.exists(target)) {
                    // Compare date
                    FileTime sourceTime = FileTime.fromMillis(entry.getTime());
                    FileTime targetTime = Files.getLastModifiedTime(target);
                    if (targetTime.compareTo(sourceTime) >= 0) {
                        logger.trace("Target {} is up to date", target);
                        return 0;
                    }
                }

                // Do copy
                ///logger.info("About to copy {} to {}", source, target);
                try (InputStream is = jar.getInputStream(entry)) {
                    Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                    logger.trace("Target {} copied", target);
                    return 1;
                }
            }
        } catch (IOException ex) {
            logger.warn("IOException on " + target, ex);
            return 0;
        }
    }
}
