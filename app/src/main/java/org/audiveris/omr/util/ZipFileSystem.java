//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    Z i p F i l e S y s t e m                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class <code>ZipFileSystem</code> gathers utility methods to read from and write to a zip
 * file (considered as a file system).
 * <p>
 * The {@link #create(java.nio.file.Path)} and {@link #open(java.nio.file.Path)} methods expect an
 * abstract path to a zip file and return the root path of the zip file system.
 * This root path can subsequently be used to create/delete/read/write folders and files
 * transparently under this root umbrella.
 * <p>
 * When IO operations are finished, the file system must be closed via a {@link FileSystem#close()}
 * on the root path like <code>root.getFileSystem().close();</code>
 * <p>
 * <b>Refactor (Path 1): Support directory-mode storage</b><br>
 * If target path is a directory (or extension is neither .omr nor .mxl), use native file system
 * instead of ZIP packaging. Directory-mode does not require fileSystem.close();
 * use {@link #closeRoot(Path, Path)} for unified cleanup.
 *
 * @author Hervé Bitteur
 */
public abstract class ZipFileSystem
{
    //~ Constructors -------------------------------------------------------------------------------

    /** Not meant to be instantiated. */
    private ZipFileSystem ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // isDirectoryPath //
    //------------------//
    /**
     * Determine whether the target path should use directory-mode (instead of ZIP-mode).
     * <p>
     * NEW: Path 1 refactor - unified entry point to distinguish directory storage from ZIP packages.
     * <p>
     * Rules:
     * - If path already exists -> check actual type (file vs directory)
     * - If path does not exist -> convention by extension: non-.omr and non-.mxl is directory-mode
     *
     * @param path the path to check
     * @return true = directory-mode, false = ZIP-mode
     */
    public static boolean isDirectoryPath (Path path)
    {
        Objects.requireNonNull(path, "ZipFileSystem.isDirectoryPath: path is null");

        if (Files.exists(path)) {
            return Files.isDirectory(path); // Exists -> check actual type
        }

        // Not exists -> convention: only .omr and .mxl use ZIP-mode
        final String name = path.getFileName().toString().toLowerCase();
        return !name.endsWith(".omr") && !name.endsWith(".mxl");
    }

    //-----------//
    // closeRoot //
    //-----------//
    /**
     * Safely close a file system root path.
     * <p>
     * NEW: Path 1 refactor - unified cleanup for directory-mode and ZIP-mode.
     * <p>
     * - Directory-mode: root is a local directory Path, no close needed
     * - ZIP-mode: root is a ZIP file system root, must call getFileSystem().close()
     *
     * @param root     file system root path (may be null)
     * @param bookPath the original path, used to determine mode
     */
    public static void closeRoot (Path root,
                                   Path bookPath)
    {
        if (root != null && !isDirectoryPath(bookPath)) {
            try {
                root.getFileSystem().close();
            } catch (IOException ignored) {
            }
        }
    }

    //--------//
    // create //
    //--------//
    /**
     * Create a new zip file system at the location provided by '<code>path</code>' parameter.
     * If such file already exists, it is deleted beforehand.
     * <p>
     * REFACTORED: If target path uses directory-mode, create directory and return it as root.
     * ZIP-mode: original logic unchanged.
     * <p>
     * When IO operations are finished, the file system must be closed via {@link FileSystem#close}
     *
     * @param path path to zip file system
     * @return the root path of the (zipped) file system
     * @throws IOException if anything goes wrong
     */
    public static Path create (Path path)
        throws IOException
    {
        Objects.requireNonNull(path, "ZipFileSystem.create: path is null");

        // NEW: directory-mode branch
        if (isDirectoryPath(path)) {
            Files.createDirectories(path); // Create directory as root
            return path;
        }

        // Original ZIP logic: delete old file -> ensure parent dir -> create ZIP FS
        Files.deleteIfExists(path);

        // Make sure the containing folder exists
        Files.createDirectories(path.getParent());

        final Map<String, String> env = new HashMap<>();
        env.put("create", "true");

        final URI uri = URI.create("jar:" + path.toUri());
        final FileSystem fs = FileSystems.newFileSystem(uri, env, null);

        return fs.getPath(fs.getSeparator());
    }

    //------//
    // open //
    //------//
    /**
     * Open a zip file system (supposed to already exist at location provided by
     * '<code>path</code>' parameter) for reading or writing.
     * <p>
     * REFACTORED: If target path uses directory-mode, return path itself as root.
     * ZIP-mode: original logic unchanged.
     * <p>
     * When IO operations are finished, the file system must be closed via {@link FileSystem#close}
     *
     * @param path (zip) file path
     * @return the root path of the (zipped) file system
     * @throws IOException if anything goes wrong
     */
    public static Path open (Path path)
        throws IOException
    {
        Objects.requireNonNull(path, "ZipFileSystem.open: path is null");

        // NEW: directory-mode branch
        if (isDirectoryPath(path)) {
            return path; // Directory itself is root, no ZIP FS needed
        }

        // Original ZIP logic: open ZIP file system
        final Map<String, String> env = new HashMap<>(); // Empty map
        final URI uri = URI.create("jar:" + path.toUri());
        final FileSystem fs = FileSystems.newFileSystem(uri, env, null);

        return fs.getPath(fs.getSeparator());
    }
}
