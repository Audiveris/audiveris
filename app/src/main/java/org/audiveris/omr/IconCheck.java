//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I c o n C h e c k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
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
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class <code>IconCheck</code> is a stand-alone utility to check the icons, in small and large
 * sizes, referred from the BSAF .properties files and copied from the Crystal collection
 * into app/dev/icons folder.
 *
 * @author Hervé Bitteur
 */
public class IconCheck
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(IconCheck.class);

    private static final String PATH = "path";

    private static final String ACTION = "action";

    private static final String actionPat = group(ACTION, "[^=]+");

    private static final String pathPat = group(PATH, ".+");

    private static final String iconPat = actionPat + "\\s*" + "=" + "\\s*" + "\\$\\{icons.root\\}/"
            + pathPat;

    private static final Pattern iconPattern = Pattern.compile(iconPat);

    //~ Instance fields ----------------------------------------------------------------------------

    private final Path root;

    private int fileCount = 0;

    private int iconCount = 0;

    private final TreeSet<String> paths = new TreeSet<>();

    //~ Constructors -------------------------------------------------------------------------------

    public IconCheck (Path root)
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

                    if (fileName.endsWith(".properties")) {
                        System.out.println(String.format("%n%s ", file));
                        fileCount++;

                        try (BufferedReader reader = new BufferedReader(
                                new FileReader(file.toString()))) {
                            String line = reader.readLine();
                            int lnb = 0;

                            while (line != null) {
                                lnb++;

                                if (line.contains("${icons.root}")) {
                                    iconCount++;

                                    final Matcher matcher = iconPattern.matcher(line);

                                    if (matcher.matches()) {
                                        final String action = getGroup(matcher, ACTION);
                                        final String path = getGroup(matcher, PATH);
                                        System.out.println("action: " + action);
                                        System.out.println("path: " + path);
                                        paths.add(path);
                                    }
                                }

                                line = reader.readLine();
                            }
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            System.out.println("files:  " + fileCount);
            System.out.println("icons:  " + iconCount);

            paths.forEach(p -> System.out.println(p));

            // Copy needed files from source folder to target folder
            final Path source = Paths.get(
                    "D:\\soft\\django-crystal-small-2011.10.20\\src\\django_crystal_small\\static\\crystal");
            final Path target = Paths.get("dev\\icons\\crystal");
            copyFiles(source.resolve("22x22"), target.resolve("22x22"));
            copyFiles(source.resolve("32x32"), target.resolve("32x32"));

        } catch (IOException ex) {
            logger.warn("Error checking {}", root, ex);
        }
    }

    private void copyFiles (Path source,
                            Path target)
    {
        paths.forEach(p -> {
            final Path src = source.resolve(p);

            if (Files.exists(src)) {
                final Path tgt = target.resolve(p);

                try {
                    Files.createDirectories(tgt.getParent());
                    Files.copy(src, tgt, REPLACE_EXISTING);
                } catch (IOException ex) {
                    logger.warn("Exception {}", ex.getMessage(), ex);
                }
            } else {
                logger.info("No file {}", src);
            }
        });
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------//
    // main //
    //------//
    /**
     * Check the icon properties hosted at the given location
     *
     * @param args the path to the sources folder of a project.
     *             If null, the sources folder of the current project is chosen
     */
    public static void main (String... args)
    {
        try {
            final String rootString;
            if ((args == null) || (args.length != 1)) {
                rootString = "src/main/java"; // Sources of the current project
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

            new IconCheck(root).process();

        } catch (Exception ex) {
            logger.error("Error  {}", ex, ex);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    // OLD icons                        NEW icons

    // actions/border_horizontal.png    view_top_bottom.png
    // actions/border_vertical.png      view_left_right.png
    // actions/delete.png               editdelete.png
    // actions/move_task_down.png       down.png
    // actions/move_task_up.png         up.png
    // actions/resources.png            db_add.png (or db_update.png)
    // actions/ungroup.png              multimedia2.png
    // actions/yellowled.png            devices/unmount_overlay.png
    //
    // apps/layer-input-output.png      kbemusedsrv.png
    // apps/layer-input.png             manually re-crafted
    // apps/layer-output.png            manually re-crafted
    //
    // mimetypes/font_type.png          gettext.png
}
