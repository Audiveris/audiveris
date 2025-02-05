//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         D o c C h e c k                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import static org.audiveris.omr.util.RegexUtil.getGroup;
import static org.audiveris.omr.util.RegexUtil.group;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class <code>DocCheck</code> is meant to check Audiveris documentation,
 * essentially the correctness of cross-links in the handbook.
 *
 * @author Hervé Bitteur
 */
public class DocCheck
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DocCheck.class);

    private static final String LOCAL = "local";

    private static final String localPat = group(LOCAL, "\\[[^\\]]*\\]");

    private static final String REMOTE = "remote";

    private static final String remotePat = group(REMOTE, "\\([^\\)]*\\)");

    private static final String LINK = "link";

    private static final String linkPat = ".*" + group(LINK, localPat + remotePat) + ".*";

    private static final Pattern linkPattern = Pattern.compile(linkPat);

    //~ Instance fields ----------------------------------------------------------------------------

    private final Path root;

    private int fileCount = 0;

    private int linkCount = 0;

    private int errorCount = 0;

    //~ Constructors -------------------------------------------------------------------------------

    public DocCheck (Path root)
    {
        this.root = root;
    }

    //~ Methods ------------------------------------------------------------------------------------

    private void process ()
    {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile (Path file,
                                                  BasicFileAttributes attrs)
                    throws IOException
                {
                    final String fileName = file.getFileName().toString();

                    if (fileName.endsWith(".md")) {
                        fileCount++;
                        final Path miniPath = root.relativize(file);
                        boolean filePrinted = false;

                        try (BufferedReader reader = new BufferedReader(
                                new FileReader(file.toString()))) {
                            String line = reader.readLine();
                            int lnb = 0;

                            while (line != null) {
                                lnb++;

                                if (line.contains("](")) {
                                    linkCount++;

                                    final Matcher matcher = linkPattern.matcher(line);

                                    if (matcher.matches()) {
                                        final String local = getGroup(matcher, LOCAL);
                                        final String remote = getGroup(matcher, REMOTE);

                                        String rem = remote.substring(1, remote.length() - 1);

                                        if (rem.startsWith("http")) {
                                            ///System.out.println("   ABSOLUTE");
                                        } else {
                                            // Remove #suffix if any
                                            final int sharp = rem.indexOf('#');
                                            String ext = null;

                                            if (sharp != -1) {
                                                ext = rem.substring(sharp, rem.length());
                                                rem = rem.substring(0, sharp);
                                            }

                                            final Path target = file.resolveSibling(rem);

                                            if (Files.exists(target)) {
                                                ///System.out.println("   OK");
                                            } else {
                                                if (!filePrinted) {
                                                    System.out.println();
                                                    System.out.println(miniPath);
                                                    filePrinted = true;
                                                }

                                                System.out.println(
                                                        String.format("%6d %s MISSING", lnb, line));
                                                errorCount++;
                                            }
                                            if (ext != null) {
                                                ///System.out.println("   extension: " + ext);
                                            }
                                        }
                                    }
                                }

                                // if (line.contains("[^")) {
                                //     if (!filePrinted) {
                                //         System.out.println();
                                //         System.out.println(miniPath);
                                //         filePrinted = true;
                                //     }
                                //
                                //     System.out.println(String.format("%6d %s NOTE", lnb, line));
                                // }

                                line = reader.readLine();
                            }
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            System.out.println("files:  " + fileCount);
            System.out.println("links:  " + linkCount);
            System.out.println("errors: " + errorCount);
        } catch (IOException ex) {
            logger.warn("Error checking {}", root, ex);
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------//
    // main //
    //------//
    /**
     * Check the documentation hosted at the given location
     *
     * @param args the path to the "docs/_pages" folder of a project.
     *             If null, the docs/_pages of the current project is chosen
     */
    public static void main (String... args)
    {
        try {
            final String rootString;
            if ((args == null) || (args.length != 1)) {
                ///rootString = "D:\\soft\\audiveris-github\\doc\\docs\\_pages";
                rootString = "../docs/_pages"; // Docs of the current project
            } else {
                rootString = args[0]; // Path provided explicitly
            }

            final Path root = Paths.get(rootString);

            System.out.println("Checking: " + root);
            System.out.println("Abs path: " + root.toAbsolutePath().normalize());
            System.out.println();

            if (!Files.isDirectory(root)) {
                logger.warn("Not a directory {}", root);
                return;
            }

            final DocCheck docCheck = new DocCheck(root);
            docCheck.process();

        } catch (Exception ex) {
            logger.error("Error  {}", ex, ex);
        }
    }
}
