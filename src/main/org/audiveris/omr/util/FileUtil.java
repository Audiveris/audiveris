//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F i l e U t i l                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code FileUtil} gathers convenient utility methods for files (and paths).
 *
 * @author Hervé Bitteur
 */
public abstract class FileUtil
{

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    private static final String BACKUP_EXT_RADIX = ".backup_";

    private static final int BACKUP_MAX = 99;

    private FileUtil ()
    {
    }

    //-----------------//
    // avoidExtensions //
    //-----------------//
    /**
     * Remove the undesired extensions from the provided path.
     * <p>
     * The method is able to handle multiple extensions (such as .foo.bar) as well as simple
     * extensions (such .bar) PROVIDED THAT the multiple extensions appear first in the sequence of
     * undesired extensions.
     * Otherwise, in the provided example, "name.foo.bar" would give "name.foo" rather than "name"
     *
     * @param path    the path to check for extensions
     * @param toAvoid the extensions to avoid (not case-sensitive)
     * @return the path without any of the undesired extensions
     */
    public static Path avoidExtensions (Path path,
                                        String... toAvoid)
    {
        final String pathStr = path.toString().toLowerCase();

        for (String s : toAvoid) {
            final String str = s.toLowerCase();
            int li = pathStr.lastIndexOf(str);

            if ((li != -1) && ((li + str.length()) == pathStr.length())) {
                return Paths.get(path.toString().substring(0, li));
            }
        }

        return path;
    }

    //--------//
    // backup //
    //--------//
    /**
     * Rename the (existing) file as a backup.
     * This is done by appending ".backup_N" to the file name, N being the first available number
     *
     * @param source the path to rename
     * @return the path to the renamed file, or null if failed
     */
    public static Path backup (Path source)
    {
        Path target = null;

        try {
            String radix = source + BACKUP_EXT_RADIX;

            for (int i = 1; i <= BACKUP_MAX; i++) {
                target = Paths.get(String.format("%s%02d", radix, i));

                if (!Files.exists(target)) {
                    Files.move(source, target);

                    return target;
                }
            }
        } catch (IOException ex) {
            logger.warn("Could not rename {} as {}", source, target, ex);
        }

        return null;
    }

    //------//
    // copy //
    //------//
    /**
     * Copy one file to another.
     *
     * @param source the file to be read
     * @param target the file to be written
     * @exception IOException raised if operation fails
     */
    @Deprecated
    public static void copy (File source,
                             File target)
            throws IOException
    {
        FileChannel input = null;
        FileChannel output = null;

        try {
            input = new FileInputStream(source).getChannel();
            output = new FileOutputStream(target).getChannel();

            MappedByteBuffer buffer = input.map(FileChannel.MapMode.READ_ONLY, 0, input.size());
            output.write(buffer);
        } finally {
            if (input != null) {
                input.close();
            }

            if (output != null) {
                output.close();
            }
        }
    }

    //----------//
    // copyTree //
    //----------//
    /**
     * Recursively copy a hierarchy of files and directories to another.
     *
     * @param sourceDir source directory
     * @param targetDir target directory
     * @throws IOException if an IO problem occurs
     */
    public static void copyTree (final Path sourceDir,
                                 final Path targetDir)
            throws IOException
    {
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>()
                   {
                       @Override
                       public FileVisitResult preVisitDirectory (Path dir,
                                                                 BasicFileAttributes attrs)
                               throws IOException
                       {
                           Path target = targetDir.resolve(sourceDir.relativize(dir));

                           try {
                               Files.copy(dir, target);
                           } catch (FileAlreadyExistsException e) {
                               if (!Files.isDirectory(target)) {
                                   throw e;
                               }
                           }

                           return FileVisitResult.CONTINUE;
                       }

                       @Override
                       public FileVisitResult visitFile (Path file,
                                                         BasicFileAttributes attrs)
                               throws IOException
                       {
                           Files.copy(file, targetDir.resolve(sourceDir.relativize(file)));

                           return FileVisitResult.CONTINUE;
                       }
                   });
    }

    //-----------//
    // deleteAll //
    //-----------//
    /**
     * Recursively delete the provided files and directories
     *
     * @param files the array of files/directories to delete
     * @return the number of items (files or directories) actually deleted
     */
    public static int deleteAll (File... files)
    {
        int deletions = 0;

        for (File file : files) {
            if (file.isDirectory()) {
                deletions += deleteAll(file.listFiles());
            }

            if (!file.delete()) {
                String kind = file.isDirectory() ? "directory" : "file";
                logger.warn("Could not delete {} {}", kind, file);
            } else {
                deletions++;
            }
        }

        return deletions;
    }

    //-----------------//
    // deleteDirectory //
    //-----------------//
    /**
     * Delete a directory with all its content in a recursive manner
     *
     * @param directory directory to delete
     * @throws IOException in case deletion is unsuccessful
     */
    public static void deleteDirectory (Path directory)
            throws IOException
    {
        if (!Files.exists(directory)) {
            throw new IllegalArgumentException(directory + " does not exist");
        }

        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>()
                   {
                       @Override
                       public FileVisitResult visitFile (Path file,
                                                         BasicFileAttributes attrs)
                               throws IOException
                       {
                           Files.delete(file);

                           return FileVisitResult.CONTINUE;
                       }

                       @Override
                       public FileVisitResult postVisitDirectory (Path dir,
                                                                  IOException exc)
                               throws IOException
                       {
                           Files.delete(dir);

                           return FileVisitResult.CONTINUE;
                       }
                   });
    }

    //--------------//
    // getExtension //
    //--------------//
    /**
     * From a file "path/name.ext", return the final ".ext" portion.
     * <p>
     * <b>Nota</b>: the dot character is part of the extension, since we
     * could have the following cases:
     * <ul>
     * <li>"path/name.ext" &rarr; ".ext"
     * <li>"path/name." &rarr; "." (just the dot)
     * <li>"path/name" &rarr; "" (the empty string)
     * </ul>
     *
     * @param file the File to process
     * @return the extension, which can be the empty string ("")
     */
    public static String getExtension (File file)
    {
        return getExtension(file.getName());
    }

    //--------------//
    // getExtension //
    //--------------//
    /**
     * From a path "path/name.ext", return the final ".ext" portion.
     * <p>
     * <b>Nota</b>: the dot character is part of the extension, since we
     * could have the following cases:
     * <ul>
     * <li>"path/name.ext" &rarr; ".ext"
     * <li>"path/name." &rarr; "." (just the dot)
     * <li>"path/name" &rarr; "" (the empty string)
     * </ul>
     *
     * @param path the File to process
     * @return the extension, which can be the empty string ("")
     */
    public static String getExtension (Path path)
    {
        return getExtension(path.getFileName().toString());
    }

    //--------------//
    // getExtension //
    //--------------//
    /**
     * Report the (last) extension in provided string.
     *
     * @param name the input file name
     * @return its last extension, or "" if none
     */
    public static String getExtension (String name)
    {
        int i = name.lastIndexOf('.');

        if (i >= 0) {
            return name.substring(i);
        } else {
            return "";
        }
    }

    //----------------------//
    // getNameSansExtension //
    //----------------------//
    /**
     * From a file "path/name.ext", return the "name" portion.
     * From a file "path/name.ext1.ext2", return the "name.ext1" portion.
     *
     * @param file provided abstract path name
     * @return just the name, w/o path and final extension
     */
    public static String getNameSansExtension (File file)
    {
        return sansExtension(file.getName());
    }

    //----------------------//
    // getNameSansExtension //
    //----------------------//
    /**
     * From a file "path/name.ext", return the "name" portion.
     * From a file "path/name.ext1.ext2", return the "name.ext1" portion.
     *
     * @param path provided abstract path name
     * @return just the name, w/o path and final extension
     */
    public static String getNameSansExtension (Path path)
    {
        return sansExtension(path.getFileName().toString());
    }

    //--------------------//
    // newDirectoryStream //
    //--------------------//
    /**
     * Opens a directory stream on provided 'dir' folder, filtering file names according
     * to the provided glob syntax.
     * <p>
     * See http://blog.eyallupu.com/2011/11/java-7-working-with-directories.html
     *
     * @param dir  the directory to read from
     * @param glob the glob matching
     * @return the opened DirectoryStream (remaining to be closed)
     * @throws IOException if anything goes wrong
     */
    public static DirectoryStream newDirectoryStream (Path dir,
                                                      String glob)
            throws IOException
    {
        // create a matcher and return a filter that uses it.
        final FileSystem fs = dir.getFileSystem();
        final PathMatcher matcher = fs.getPathMatcher("glob:" + glob);
        final DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>()
        {
            @Override
            public boolean accept (Path entry)
            {
                return matcher.matches(entry.getFileName());
            }
        };

        return fs.provider().newDirectoryStream(dir, filter);
    }

    //---------------//
    // sansExtension //
    //---------------//
    /**
     * Report the provided name without its last extension.
     *
     * @param name input file name
     * @return name with its last extension removed
     */
    public static String sansExtension (String name)
    {
        int i = name.lastIndexOf('.');

        if (i >= 0) {
            return name.substring(0, i);
        } else {
            return name;
        }
    }

    //----------//
    // walkDown //
    //----------//
    /**
     * Browse a file tree for retrieving relevant directories and files.
     *
     * @param folder   the folder where browsing starts
     * @param dirGlob  glob pattern for relevant directories
     * @param fileGlob glob pattern for relevant files
     * @return the list of paths found
     */
    public static List<Path> walkDown (final Path folder,
                                       final String dirGlob,
                                       final String fileGlob)
    {
        logger.debug(" dirGlob is {}", dirGlob);
        logger.debug("fileGlob is {}", fileGlob);

        final FileSystem fs = FileSystems.getDefault();
        final PathMatcher dirMatcher = fs.getPathMatcher(dirGlob);
        final PathMatcher fileMatcher = fs.getPathMatcher(fileGlob);
        final List<Path> pathsFound = new ArrayList<>();

        if (!Files.exists(folder)) {
            return pathsFound;
        }

        try {
            Files.walkFileTree(folder, new SimpleFileVisitor<Path>()
                       {
                           @Override
                           public FileVisitResult preVisitDirectory (Path dir,
                                                                     BasicFileAttributes attrs)
                                   throws IOException
                           {
                               if (dir.equals(folder) || dirMatcher.matches(dir)) {
                                   return FileVisitResult.CONTINUE;
                               } else {
                                   return FileVisitResult.SKIP_SUBTREE;
                               }
                           }

                           @Override
                           public FileVisitResult visitFile (Path file,
                                                             BasicFileAttributes attrs)
                                   throws IOException
                           {
                               if (fileMatcher.matches(file)) {
                                   pathsFound.add(file);
                               }

                               return FileVisitResult.CONTINUE;
                           }

                           @Override
                           public FileVisitResult postVisitDirectory (Path dir,
                                                                      IOException exc)
                                   throws IOException
                           {
                               if (dirMatcher.matches(dir)) {
                                   pathsFound.add(dir);
                               }

                               return FileVisitResult.CONTINUE;
                           }
                       });
        } catch (IOException ex) {
            logger.warn("Error in browsing " + folder + " " + ex, ex);
        }

        return pathsFound;
    }
}
