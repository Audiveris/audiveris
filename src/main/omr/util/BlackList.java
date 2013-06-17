//----------------------------------------------------------------------------//
//                                                                            //
//                             B l a c k L i s t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code BlackList} handles the mechanism of excluding certain
 * files and subdirectories in a directory, according to the presence
 * and content of a specific black-list in this directory.
 *
 * <b>Nota</b>: The scope of this blacklist is just the directory that contains
 * the black-list file, not its subdirectories if any.
 *
 * @author Hervé Bitteur
 */
public class BlackList
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(BlackList.class);

    /** Name of the specific file where blacklist is kept */
    protected static final String BLACK_LIST_NAME = ".glyphignore";

    //~ Instance fields --------------------------------------------------------
    /** Set of black listed file names */
    protected final SortedSet<String> bl = new TreeSet<>();

    /** Containing directory */
    protected final File dir;

    /** Specific blacklist file */
    protected final File blackFile;

    /** Specific filter for blacklist */
    protected final FileFilter blackFilter = new FileFilter()
    {
        @Override
        public boolean accept (File file)
        {
            return isLegal(file);
        }
    };

    //~ Constructors -----------------------------------------------------------
    /**
     * Create a BlackList object, related to provided directory.
     *
     * @param dir the containing directory
     */
    public BlackList (File dir)
    {
        this.dir = dir;
        blackFile = new File(dir, BLACK_LIST_NAME);
        load();
    }

    //~ Methods ----------------------------------------------------------------
    //-----//
    // add //
    //-----//
    /**
     * Blacklist a file.
     *
     * @param file the file to blacklist
     */
    public void add (File file)
    {
        String name = file.getName().trim();

        if (!bl.contains(name)) {
            bl.add(name);
            store();
        }
    }

    //---------//
    // isLegal //
    //---------//
    /**
     * Check whether a file is legal (not blacklisted).
     *
     * @param file the file to check
     * @return true if legal
     */
    public boolean isLegal (File file)
    {
        return !file.equals(blackFile) && !bl.contains(file.getName());
    }

    //-----------//
    // listFiles //
    //-----------//
    /**
     * Report an array of files and directories that are not blacklisted
     * in the containing directory of this BlackList file.
     *
     * @return an array of legal File instances
     */
    public File[] listFiles ()
    {
        return listFiles(null);
    }

    //-----------//
    // listFiles //
    //-----------//
    /**
     * Report an array of files and directories that are not blacklisted
     * in the containing directory of this BlackList file, and that are
     * accepted by the provided file filter.
     *
     * @return an array of filtered legal File instances
     */
    public File[] listFiles (FileFilter filter)
    {
        logger.debug("Retrieving legal slots in directory {}", dir);

        // Getting all legal files & dirs, w/ additional filter if any
        List<File> legals = new ArrayList<>();
        File[] files = dir.listFiles(blackFilter);

        if (files != null) {
            for (File file : files) {
                if ((filter == null) || filter.accept(file)) {
                    legals.add(file);
                }
            }
        } else {
            logger.info("No files in dir {}", dir);
        }

        // Return legals as an array
        return legals.toArray(new File[legals.size()]);
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove a file from the black list.
     *
     * @param file the file to remove
     */
    public void remove (File file)
    {
        String name = file.getName().trim();

        if (bl.contains(name)) {
            bl.remove(name);
            store();
        }
    }

    //------//
    // load //
    //------//
    private void dump ()
    {
        if (!bl.isEmpty()) {
            System.out.println("BlackList for dir : " + dir);

            for (String s : bl) {
                System.out.println(" - " + s);
            }
        }
    }

    //------//
    // load //
    //------//
    private void load ()
    {
        if (blackFile.exists()) {
            BufferedReader in = null;

            try {
                in = new BufferedReader(new FileReader(blackFile));

                String fileName;

                try {
                    // Expect one file name per line
                    while ((fileName = in.readLine()) != null) {
                        bl.add(fileName.trim());
                    }

                    in.close();
                } catch (IOException ex) {
                    logger.warn("IO error while reading file ''{}''",
                            blackFile);
                }
            } catch (FileNotFoundException ex) {
                logger.warn("Cannot find file ''{}''", blackFile);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        if (logger.isDebugEnabled()) {
            dump();
        }
    }

    //-------//
    // store //
    //-------//
    private void store ()
    {
        PrintWriter out = null;

        try {
            out = new PrintWriter(
                    new BufferedWriter(new FileWriter(blackFile)));

            for (String name : bl) {
                out.println(name);
            }
        } catch (IOException ex) {
            logger.warn("IO error while writing file ''{}''", blackFile);
        } finally {
            if (out != null) {
                out.close();
            }
        }

        if (logger.isDebugEnabled()) {
            dump();
        }
    }
}
