//----------------------------------------------------------------------------//
//                                                                            //
//                             F i l e C o p i e r                            //
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
import static java.nio.file.FileVisitResult.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import static java.nio.file.StandardCopyOption.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Class {@code FileCopier} copies a collection of source files
 * to a target.
 * <p>
 * If several source files are provided, the target must be a directory.
 * If a source file is a directory and if the {@code recursive} flag is set,
 * then the directory content is copied recursively.
 *
 * @author Hervé Bitteur
 */
public class FileCopier
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
        FileCopier.class);

    //~ Instance fields --------------------------------------------------------

    /** Sources. */
    private final Path[] sources;

    /** Target. */
    private final Path target;

    /** Recursive flag. */
    private final boolean recursive;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // FileCopier //
    //------------//
    /**
     * Creates a new FileCopier object.
     *
     * @param sources   the sources collection, perhaps empty but not null
     * @param target    the target file (perhaps a folder)
     * @param recursive true for a recursive copy
     */
    public FileCopier (Path[]  sources,
                       Path    target,
                       boolean recursive)
    {
        Objects.requireNonNull(sources, "Sources cannot be null");

        // Make a deep copy of sources collection
        this.sources = new Path[sources.length];

        for (int i = 0; i < sources.length; i++) {
            this.sources[i] = sources[i];
        }

        Objects.requireNonNull(target, "Target cannot be null");
        this.target = target;

        this.recursive = recursive;
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // copy //
    //------//
    /**
     * Perform the copy.
     */
    public void copy ()
        throws IOException
    {
        // Is target a directory?
        boolean isDir = Files.isDirectory(target);

        // Copy each source (file or folder) to target
        for (Path source : sources) {
            Path dest = isDir ? target.resolve(source.getFileName()) : target;

            try {
                if (recursive) {
                    Files.walkFileTree(source, new TreeCopier(source, dest));
                } else {
                    if (Files.isDirectory(source)) {
                        logger.warn("{} is a directory", source);
                    } else {
                        Files.copy(source, dest, REPLACE_EXISTING);
                    }
                }
            } catch (IOException ex) {
                logger.info(
                    "Cannot copy '" + source + "' to '" + dest + "' ex:" + ex);
                throw ex;
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // TreeCopier //
    //------------//
    /**
     * TreeCopier is meant to recursively copy a tree of folders
     * rooted at {@code source} to {@code dest} folder.
     */
    private class TreeCopier
        extends SimpleFileVisitor<Path>
    {
        //~ Instance fields ----------------------------------------------------

        private final Path source;
        private final Path dest;

        //~ Constructors -------------------------------------------------------

        public TreeCopier (Path source,
                           Path dest)
        {
            this.source = source;
            this.dest = dest;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public FileVisitResult preVisitDirectory (Path                dir,
                                                  BasicFileAttributes attrs)
            throws IOException
        {
            Path newFolder = dest.resolve(source.relativize(dir));

            if (Files.notExists(newFolder)) {
                logger.debug("Creating folder {}", newFolder);
                Files.copy(dir, newFolder, REPLACE_EXISTING);
            } else {
                logger.debug("Folder {} already exists.", newFolder);
            }

            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFile (Path                file,
                                          BasicFileAttributes attrs)
            throws IOException
        {
            final Path destPath = dest.resolve(source.relativize(file));
            logger.debug("Copying file {} to {}", file, destPath);
            Files.copy(file, destPath, REPLACE_EXISTING);

            return CONTINUE;
        }
    }
}
