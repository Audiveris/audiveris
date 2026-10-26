//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P r o j e c t E n c o d i n g                                 //
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
package org.audiveris.omr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Class <code>ProjectEncoding</code> generates the XML fragments meant for NetBeans
 * to let every .properties file use the project encoding.
 * <p>
 * All the XML fragments are stored in a file named "encoding-fragments.xml" located in
 * Audiveris user temp folder.
 * <p>
 * Second phase:
 * 1/ Exit NetBeans if any instance is running. This is mandatory!
 * 2/ The content of the "encoding-fragments.xml" file must be copied manually
 * (via NotePad or another editor, except NetBeans) into the NetBeans user configuration file
 * named "attributes.xml" and located in a folder like
 * C:\Users\herve\AppData\Roaming\NetBeans\31\var
 * Beware, the location depends on NetBeans version (here: 31).
 * 3/Re-launch NetBeans (it will load the "attributes.xml" file, and rewrite it at exit time)
 *
 * @author Hervé Bitteur
 */
public class ProjectEncoding
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ProjectEncoding.class);

    private static final String SEPARATOR_REGEX = WellKnowns.FILE_SEPARATOR.equals("/") //
            ? "/" // Linux, macOS
            : "\\\\"; // Windows

    //~ Instance fields ----------------------------------------------------------------------------

    private final Path root;

    private int fileCount = 0;

    //~ Constructors -------------------------------------------------------------------------------

    public ProjectEncoding (Path root)
    {
        this.root = root;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    // <fileobject name="D:|soft|audiveris-github|dev|app|src|main|java|org|audiveris|omr|ui|resources|AboutAction_ro.properties">
    //   <attr name="projectEncoding" boolvalue="true"/>
    // </fileobject>
    //

    private void printFragment (Path path,
                                PrintWriter out)
    {
        // Replace all '/' or '\' folder separators by a vertical line '|'
        final String f = path.toString().replaceAll(SEPARATOR_REGEX, "|");
        out.print("    <fileobject name=\"");
        out.print(f);
        out.println("\">");

        out.println("        <attr name=\"projectEncoding\" boolvalue=\"true\"/>");
        out.println("    </fileobject>");
    }

    private void process ()
    {
        final Path outPath = WellKnowns.TEMP_FOLDER.resolve("encoding-fragments.xml");

        try (FileOutputStream fos = new FileOutputStream(outPath.toFile());
                OutputStreamWriter osw = new OutputStreamWriter(fos, WellKnowns.FILE_ENCODING);
                PrintWriter out = new PrintWriter(new BufferedWriter(osw))) {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory (Path dir,
                                                          BasicFileAttributes attrs)
                    throws IOException
                {
                    final Path path = dir.toAbsolutePath().normalize();
                    logger.trace("path: {}", path);
                    final String fileName = path.getFileName().toString();

                    if (fileName.equals("build") //
                            || fileName.startsWith("."))
                        return FileVisitResult.SKIP_SUBTREE;
                    else
                        return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile (Path file,
                                                  BasicFileAttributes attrs)
                    throws IOException
                {
                    final String fileName = file.getFileName().toString();

                    if (fileName.endsWith(".properties")) {
                        fileCount++;

                        final Path path = file.toAbsolutePath().normalize();
                        System.out.println(path.toString());
                        printFragment(path, out);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            logger.info("files: {} ", fileCount);
            logger.info("All fragments written in file: {}", outPath.toAbsolutePath().normalize());
        } catch (IOException ex) {
            logger.warn("Error processing {}", root, ex);
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------//
    // main //
    //------//
    /**
     * Run the program on the provided root location
     *
     * @param args the path to the roo folder of a project.
     *             If null, the root of the current project is chosen
     */
    public static void main (String... args)
    {
        try {
            final String rootString;
            if ((args == null) || (args.length != 1)) {
                rootString = ".."; // Root of the current project (seen from 'app' sub-project)
            } else {
                rootString = args[0]; // Path provided explicitly
            }

            final Path root = Paths.get(rootString);

            System.out.println("Processing: " + root);
            System.out.println("Abs path: " + root.toAbsolutePath().normalize());
            System.out.println();

            if (!Files.isDirectory(root)) {
                logger.warn("Not a directory {}", root);
                return;
            }

            final ProjectEncoding instance = new ProjectEncoding(root);
            instance.process();

        } catch (Exception ex) {
            logger.error("Error  {}", ex, ex);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

}
