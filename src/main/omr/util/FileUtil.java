//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F i l e U t i l                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    //~ Methods ------------------------------------------------------------------------------------
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
    public static void copyTree (final Path sourceDir,
                                 final Path targetDir)
            throws IOException
    {
        Files.walkFileTree(
                sourceDir,
                new SimpleFileVisitor<Path>()
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

    //--------------//
    // getExtension //
    //--------------//
    /**
     * From a file "path/name.ext", return the final ".ext" portion.
     * <p>
     * <b>Nota</b>: the dot character is part of the extension, since we
     * could have the following cases: <ul>
     * <li> "path/name.ext" -> ".ext"
     * <li> "path/name." -> "." (just the dot)
     * <li> "path/name" -> "" (the empty string) </ul>
     *
     * @param file the File to process
     *
     * @return the extension, which may be ""
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
     * could have the following cases: <ul>
     * <li> "path/name.ext" -> ".ext"
     * <li> "path/name." -> "." (just the dot)
     * <li> "path/name" -> "" (the empty string) </ul>
     *
     * @param path the File to process
     *
     * @return the extension, which may be ""
     */
    public static String getExtension (Path path)
    {
        return getExtension(path.getFileName().toString());
    }

    //--------------//
    // getExtension //
    //--------------//
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
     *
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
     *
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
     * @throws IOException
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
        final List<Path> pathsFound = new ArrayList<Path>();

        if (!Files.exists(folder)) {
            return pathsFound;
        }

        try {
            Files.walkFileTree(
                    folder,
                    new SimpleFileVisitor<Path>()
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
