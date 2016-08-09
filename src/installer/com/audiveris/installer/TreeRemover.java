//----------------------------------------------------------------------------//
//                                                                            //
//                            T r e e R e m o v e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Class {@code TreeRemover} is a FileVisitor that deletes a folder
 * recursively.
 *
 * @author Hervé Bitteur
 */
public class TreeRemover
        extends SimpleFileVisitor<Path>
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            TreeRemover.class);

    //~ Methods ----------------------------------------------------------------
    @Override
    public FileVisitResult postVisitDirectory (Path dir,
                                               IOException ex)
            throws IOException
    {
        if (ex == null) {
            logger.debug("Deleting directory {}", dir);
            Files.delete(dir);

            return FileVisitResult.CONTINUE;
        } else {
            // directory iteration failed
            throw ex;
        }
    }

    //--------//
    // remove //
    //--------//
    /**
     * Convenient method to remove a folder recursively.
     *
     * @param path the folder to remove, with all its content
     * @throws IOException
     */
    public static void remove (Path path)
            throws IOException
    {
        Files.walkFileTree(path, new TreeRemover());
    }

    @Override
    public FileVisitResult visitFile (Path file,
                                      BasicFileAttributes attrs)
            throws IOException
    {
        logger.debug("Deleting file {}", file);
        Files.delete(file);

        return FileVisitResult.CONTINUE;
    }
}
