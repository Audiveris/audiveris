//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S h e e t C o u n t e r                                    //
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
package org.audiveris.omr.image;

import org.audiveris.omr.OMR;
import org.audiveris.omr.image.ImageLoading.Loader;
import org.audiveris.omr.sheet.Book;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StopOptionHandler;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code SheetCounter} is a stand-alone utility to count sheets in files
 * (either books or image files).
 *
 * @author Hervé Bitteur
 */
public class SheetCounter
{

    /**
     * Main entry point.
     *
     * @param args list of file names, processing depending on file extension (.omr, .pdf, ...)
     * @throws CmdLineException if there was any error parsing CLI arguments
     */
    public static void main (String[] args)
            throws CmdLineException
    {
        System.out.println("SheetCounter running args:" + args.length);

        /** Parameters structure to be populated. */
        final Parameters params = new Parameters();

        /** CLI parser. */
        final CmdLineParser parser = new CmdLineParser(params);

        parser.parseArgument(args);

        for (Path argument : params.arguments) {
            String str = argument.toString().trim().replace('\\', '/');

            if (!str.isEmpty()) {
                final Path path = Paths.get(str);

                try {
                    if (str.endsWith(OMR.BOOK_EXTENSION)) {
                        Book book = Book.loadBook(path);
                        int all = book.getStubs().size();
                        int valid = book.getValidStubs().size();
                        printResult(all, valid, path);
                    } else {
                        Loader loader = ImageLoading.getLoader(path);
                        int all = loader.getImageCount();
                        printResult(all, all, path);
                    }
                } catch (Exception ex) {
                    System.err.println("Error on " + path + " " + ex);
                }
            }
        }
    }

    private static void printResult (int all,
                                     int valid,
                                     Path path)
    {
        System.out.println(String.format("%3d %3d %s", all, valid, path));
    }

    ///private static void result()
    //------------//
    // Parameters //
    //------------//
    /**
     * The structure that collects the various parameters parsed out of the command line.
     */
    public static class Parameters
    {

        /** Final arguments, with optional "--" separator. */
        @Argument
        @Option(name = "--", handler = StopOptionHandler.class)
        List<Path> arguments = new ArrayList<>();

        private Parameters ()
        {
        }
    }
}
