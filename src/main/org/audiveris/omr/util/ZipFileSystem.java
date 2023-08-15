//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    Z i p F i l e S y s t e m                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

    //--------//
    // create //
    //--------//
    /**
     * Create a new zip file system at the location provided by '<code>path</code>' parameter.
     * If such file already exists, it is deleted beforehand.
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

        final Map<String, String> env = new HashMap<>(); // Empty map
        final URI uri = URI.create("jar:" + path.toUri());
        final FileSystem fs = FileSystems.newFileSystem(uri, env, null);

        return fs.getPath(fs.getSeparator());
    }
}
