//----------------------------------------------------------------------------//
//                                                                            //
//                              E x p a n d e r                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Class {@code Expander} gathers methods to expand a .gz or a .tar
 * file.
 * These methods can be used to expand a .tar.gz archive.
 *
 * @author Dan Borza (at http://stackoverflow.com/users/510638/dan-borza)
 * @author Hervé Bitteur
 */
public final class Expander
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            Expander.class);

    /** .gz extension. */
    private static final String GZ_EXT = ".gz";

    /** .tar extension. */
    private static final String TAR_EXT = ".tar";

    //~ Constructors -----------------------------------------------------------
    /** Not meant to be instantiated */
    private Expander ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // streamToFile //
    //--------------//
    /**
     * Store the provided input stream to the desired file.
     *
     * @param in      the input stream. It is not closed by this method.
     * @param outFile the desired target file
     * @return the target file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static File streamToFile (final InputStream in,
                                     final File outFile)
            throws FileNotFoundException, IOException
    {
        final OutputStream out = new FileOutputStream(outFile);
        IOUtils.copy(in, out);
        out.close();

        return outFile;
    }

    //--------//
    // unGzip //
    //--------//
    /**
     * Ungzip an input file into an output file.
     *
     * The output file is created in the output folder, having the same name
     * as the input file, minus the '.gz' extension.
     *
     * @param inFile the input .gz file
     * @param outDir the output directory file.
     * @throws IOException
     * @throws FileNotFoundException
     *
     * @return The file with the ungzipped content.
     */
    public static File unGzip (final File inFile,
                               final File outDir)
            throws FileNotFoundException, IOException
    {
        logger.debug("Ungzipping {} to dir {}", inFile, outDir);

        final String inName = inFile.getName();
        assert inName.endsWith(GZ_EXT);

        final InputStream in = new GZIPInputStream(new FileInputStream(inFile));
        final File outFile = streamToFile(
                in,
                new File(
                outDir,
                inName.substring(0, inName.length() - GZ_EXT.length())));
        in.close();

        return outFile;
    }

    //-------//
    // unTar //
    //-------//
    /**
     * Untar an input file into an output directory.
     *
     * @param inFile the input .tar file
     * @param outDir the output directory
     * @throws IOException
     * @throws FileNotFoundException
     * @throws ArchiveException
     *
     * @return The list of files with the untared content.
     */
    public static List<File> unTar (final File inFile,
                                    final File outDir)
            throws FileNotFoundException, IOException, ArchiveException
    {
        logger.debug("Untaring {} to dir {}", inFile, outDir);
        assert inFile.getName()
                .endsWith(TAR_EXT);

        final List<File> untaredFiles = new ArrayList<File>();
        final InputStream is = new FileInputStream(inFile);
        final TarArchiveInputStream tis = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream(
                "tar",
                is);
        ArchiveEntry entry;

        while ((entry = tis.getNextEntry()) != null) {
            final File outFile = new File(outDir, entry.getName());

            if (entry.isDirectory()) {
                logger.debug("Attempting to write output dir {}", outFile);

                if (!outFile.exists()) {
                    logger.debug("Attempting to create output dir {}", outFile);

                    if (!outFile.mkdirs()) {
                        throw new IllegalStateException(
                                String.format(
                                "Couldn't create directory %s",
                                outFile.getAbsolutePath()));
                    }
                }
            } else {
                logger.debug("Creating output file {}", outFile);
                streamToFile(tis, outFile);
            }

            untaredFiles.add(outFile);
        }

        tis.close();

        return untaredFiles;
    }
}
