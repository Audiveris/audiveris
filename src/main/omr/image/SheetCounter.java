//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S h e e t C o u n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import omr.OMR;

import omr.image.ImageLoading.Loader;

import omr.sheet.BasicBook;
import omr.sheet.Book;

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
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Main entry point.
     *
     * @param args list of file names, processing depending on file extension (.omr, .pdf, ...)
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
                        Book book = BasicBook.loadBook(path);
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

    //~ Inner Classes ------------------------------------------------------------------------------
    ///private static void result()
    //------------//
    // Parameters //
    //------------//
    /**
     * The structure that collects the various parameters parsed out of the command line.
     */
    public static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Final arguments, with optional "--" separator. */
        @Argument
        @Option(name = "--", handler = StopOptionHandler.class)
        List<Path> arguments = new ArrayList<Path>();

        //~ Constructors ---------------------------------------------------------------------------
        private Parameters ()
        {
        }
    }
}
