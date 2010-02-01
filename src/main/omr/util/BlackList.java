//----------------------------------------------------------------------------//
//                                                                            //
//                             B l a c k L i s t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.log.Logger;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Class <code>BlackList</code> handles the mechanism of excluding certain files
 * and subdirectories in a directory, according to the presence and content of a
 * specific black-list in this directory. <b>Nota</b>: The scope of this
 * blacklist is just the directory that contains the black-list file, not its
 * subdirectories if any.
 *
 * @author Hervé Bitteur
 */
public class BlackList
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BlackList.class);

    /**
     * Name of the specific file where blacklist is kept.
     */
    private static final String BLACK_LIST_NAME = ".glyphignore";

    //~ Instance fields --------------------------------------------------------

    /** Set of black listed file names */
    private final SortedSet<String> bl = new TreeSet<String>();

    /** Containing directory */
    private final File dir;

    /** Specific blacklist file */
    private final File blackFile;

    /** Specific filter for blacklist */
    private final FileFilter blackFilter = new FileFilter() {
        public boolean accept (File file)
        {
            return isLegal(file);
        }
    };


    //~ Constructors -----------------------------------------------------------

    /**
     * Create a BlackList object, related to provided directory
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

    //---------//
    // isLegal //
    //---------//
    /**
     * Check whether a file is legal (not blacklisted)
     *
     * @param file the file to check
     * @return true if legal
     */
    public boolean isLegal (File file)
    {
        return !file.equals(blackFile) && !bl.contains(file.getName());
    }

    //-----//
    // add //
    //-----//
    /**
     * Blacklist a file
     *
     * @param file the file to blacklist
     */
    public void add (File file)
    {
        String name = file.getName()
                          .trim();

        if (!bl.contains(name)) {
            bl.add(name);
            store();
        }
    }

    //-----------//
    // listFiles //
    //-----------//
    /**
     * Report an array of files and directories that are not blacklisted in the
     * containing directory of this BlackList file
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
     * Report an array of files and directories that are not blacklisted in the
     * containing directory of this BlackList file, and that are accepted by
     * the provided file filter
     *
     * @return an array of filtered legal File instances
     */
    public File[] listFiles (FileFilter filter)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Retrieving legal slots in directory " + dir);
        }

        // Getting all legal files & dirs, w/ additional filter if any
        List<File> legals = new ArrayList<File>();
        File[]     files = dir.listFiles(blackFilter);

        if (files != null) {
            for (File file : files) {
                if ((filter == null) || filter.accept(file)) {
                    legals.add(file);
                }
            }
        } else {
            logger.warning("Could not retrieve slots out of dir " + dir);
        }

        // Return legals as an array
        return legals.toArray(new File[legals.size()]);
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove a file from the black list
     *
     * @param file the file to remove
     */
    public void remove (File file)
    {
        String name = file.getName()
                          .trim();

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
                    logger.warning(
                        "IO error while reading file '" + blackFile + "'");
                }
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find file '" + blackFile + "'");
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        if (logger.isFineEnabled()) {
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
            logger.warning("IO error while writing file '" + blackFile + "'");
        } finally {
            if (out != null) {
                out.close();
            }
        }

        if (logger.isFineEnabled()) {
            dump();
        }
    }
}
