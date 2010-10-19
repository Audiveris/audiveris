//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e R e d u c t i o n                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.log.Logger;

import proxymusic.Print;
import proxymusic.ScorePartwise;

import proxymusic.ScorePartwise.Part;
import proxymusic.ScorePartwise.Part.Measure;

import proxymusic.YesNo;

import proxymusic.util.Marshalling;

import java.io.*;
import java.util.*;

import javax.xml.bind.JAXBException;

/**
 * Class {@code ScoreReduction} is a first attempt to implement the "reduce"
 * part of a MapReduce Job for a given score.  <ol>
 *
 * <li>Any Map task processes a score page and produces the related XML fragment
 * as its output.</li>
 *
 * <li>The Reduce task takes all the XML fragments as input and consolidates
 * them in a global Score output.</li></ol>
 *
 * <p>Typical calling of the feature is the following:
 * <code>
 * <pre>
 * HashMap&lt;Integer, String&gt; fragments = ...;
 * ScoreReduction reduction = new ScoreReduction(fragments);
 * String output = reduction.reduce();
 * </pre>
 * </code>
 *
 * <p>A main() method is provided to ease the testing of this class, assuming
 * that the individual pages have already been scanned and their XML fragments
 * are available on disk.
 * It requires 3 arguments: name of the folder to lookup, prefix for
 * matching files, suffix for matching files.
 * It with search the specified folder for matching files, read their content as
 * XML fragments and launch a ScoreReduction instance on this data.
 *
 * @author Hervé Bitteur
 */
public class ScoreReduction
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreReduction.class);

    /** Factory for proxymusic entities */
    private static final proxymusic.ObjectFactory factory = new proxymusic.ObjectFactory();

    //~ Instance fields --------------------------------------------------------

    private final Map<Integer, String> fragments;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ScoreReduction object.
     *
     * @param fragments a map of XML fragments, one entry per page, the key
     * being the page number and the value being the MusicXML fragment produced
     * from the page.
     */
    public ScoreReduction (Map<Integer, String> fragments)
    {
        this.fragments = fragments;
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // main //
    //------//
    /**
     * Pseudo-main method, just to allocate an instance of ScoreReduction and
     * launch the reduce() method
     * @param args the template to filter relevant files
     */
    public static void main (String... args)
        throws FileNotFoundException, IOException, JAXBException
    {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                "Expected 3 arguments (folder, prefix, suffix)");
        }

        SortedMap<Integer, File> files = retrieveFiles(args);

        if (files.isEmpty()) {
            return;
        }

        HashMap<Integer, String> fragments = new HashMap<Integer, String>();

        for (Map.Entry<Integer, File> entry : files.entrySet()) {
            BufferedReader input = new BufferedReader(
                new FileReader(entry.getValue()));
            StringBuilder  fragment = new StringBuilder();
            String         line;

            while ((line = input.readLine()) != null) {
                fragment.append(line)
                        .append("\n");
            }

            fragments.put(entry.getKey(), fragment.toString());
        }

        ScoreReduction reduction = new ScoreReduction(fragments);
        String         output = reduction.reduce();

        logger.info("Output.length: " + output.length());

        if (logger.isFineEnabled()) {
            logger.fine("Output:\n" + output);
        }

        // For debugging
        FileOutputStream fos = new FileOutputStream("global.xml");
        fos.write(output.getBytes());
        fos.close();
    }

    //--------//
    // reduce //
    //--------//
    /**
     * Build a score output as the smart concatenation of the fragments produced
     * from each page. The fragments are a map of XML fragments, the map key
     * being the page number in the containing score. They are provided to the
     * ScoreReduction constructor.
     * @return the resulting global XML output for the score
     */
    public String reduce ()
        throws JAXBException, IOException
    {
        // Debug
        //logger.setLevel(Level.FINEST);

        // Load pages
        SortedMap<Integer, ScorePartwise> pages = loadPages(fragments);

        if (pages.isEmpty()) {
            return "";
        }

        // Consolidate
        ScorePartwise globalPartwise = consolidate(pages);

        // Build output
        return buildOutput(globalPartwise);
    }

    //----------//
    // getPrint //
    //----------//
    /**
     * Retrieve the Print element in the object list, even if we need to create
     * a new one and insert it to the list
     * @param noteOrBackupOrForward the list to search (andupdate)
     * @return the print element (old or brand new)
     */
    private Print getPrint (List<Object> noteOrBackupOrForward)
    {
        for (Object obj : noteOrBackupOrForward) {
            if (obj instanceof Print) {
                return (Print) obj;
            }
        }

        // Not found, let's create and insert one
        Print print = factory.createPrint();
        noteOrBackupOrForward.add(print);

        return print;
    }

    //-----------//
    // addHeader //
    //-----------//
    /**
     * Create the header of the global partwise, by replicating information
     * form first page header
     * @param global the global partwise to update
     * @param first the partwise for first page
     */
    private void addHeader (ScorePartwise global,
                            ScorePartwise first)
    {
        // Work
        global.setWork(first.getWork());

        // Identification
        // TODO Encoding:
        // - Signature is inserted twice (page then global)
        // - Source should be the whole score file, not the first page file
        global.setIdentification(first.getIdentification());

        // Defaults
        global.setDefaults(first.getDefaults());

        // Credits
        global.getCredit()
              .addAll(first.getCredit());

        // Part list
        // TODO: Verify consistency, part per part, across pages
        global.setPartList(first.getPartList());
    }

    //----------//
    // addParts //
    //----------//
    /**
     * Add all part elements to the global partwise
     * @param global the global partwise
     * @param pages the individual page partwise instances
     */
    private void addParts (ScorePartwise                     global,
                           SortedMap<Integer, ScorePartwise> pages)
    {
        // TODO: check that all part lists are consistent ...
        ScorePartwise first = pages.get(pages.firstKey());
        int           partCount = first.getPart()
                                       .size();

        // Loop on parts
        for (int ip = 0; ip < partCount; ip++) {
            // We create a brand new part in global partwise
            Part gPart = factory.createScorePartwisePart();
            global.getPart()
                  .add(gPart);

            int     midOffset = 0; // Page offset on measure id
            boolean isFirstPage = true; // First page?

            // Loop on pages
            for (ScorePartwise page : pages.values()) {
                Part part = page.getPart()
                                .get(ip);

                if (isFirstPage) {
                    gPart.setId(part.getId());
                }

                int     mid = 0; // Measure id (in this page)
                boolean isFirstMeasure = true; // First measure? (in this page)

                // Update measure in situ and reference them from containing part
                for (Measure measure : part.getMeasure()) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "ip:" + ip + " Measure #" + measure.getNumber());
                    }

                    // New page?
                    if (!isFirstPage && isFirstMeasure) {
                        // Insert/Update print element
                        getPrint(measure.getNoteOrBackupOrForward())
                            .setNewPage(YesNo.YES);
                    }

                    // Shift measure number
                    mid = Integer.decode(measure.getNumber());
                    measure.setNumber("" + (mid + midOffset));
                    gPart.getMeasure()
                         .add(measure);

                    isFirstMeasure = false;
                }

                midOffset += mid;
                isFirstPage = false;
            }
        }
    }

    //-------------//
    // buildOutput //
    //-------------//
    /**
     * Marshall the global partwise into a String
     * @param globalPartwise the global partwise we have built
     * @return the marshalled string
     * @throws JAXBException
     * @throws IOException
     */
    private String buildOutput (ScorePartwise globalPartwise)
        throws JAXBException, IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Marshalling.marshal(globalPartwise, os, true);

        return os.toString();
    }

    //-------------//
    // consolidate //
    //-------------//
    /**
     * This is the bulk of reduction task, consolidating the outputs of
     * individual pages
     * @param pages the individual pages
     * @return the global score partwise
     */
    private ScorePartwise consolidate (SortedMap<Integer, ScorePartwise> pages)
    {
        ScorePartwise global = new ScorePartwise();
        ScorePartwise first = pages.get(pages.firstKey());

        // Score header: more or less reuse the header of first page
        addHeader(global, first);

        // Append parts content, inserting page breaks, re-numbering measures
        addParts(global, pages);

        // Handle cross-page slurs
        // TBD

        // The end
        return global;
    }

    //-----------//
    // loadPages //
    //-----------//
    /**
     * Retrieve individual page partwise instance, by unmarshalling MusicXML data from the page
     * string fragments
     * @param pageFragments the sequence of input fragments (one string per page)
     * @return the related sequence of partwise instances (one instance per page)
     * @throws JAXBException
     */
    private SortedMap<Integer, ScorePartwise> loadPages (Map<Integer, String> pageFragments)
        throws JAXBException
    {
        // Access the page fragments in the right order
        SortedSet<Integer>                pageNumbers = new TreeSet<Integer>(
            pageFragments.keySet());

        // Load pages content
        SortedMap<Integer, ScorePartwise> pages = new TreeMap<Integer, ScorePartwise>();

        for (int pageNumber : pageNumbers) {
            logger.info("Reading fragment #" + pageNumber + " ...");

            String               fragment = pageFragments.get(pageNumber);
            ByteArrayInputStream is = new ByteArrayInputStream(
                fragment.getBytes());

            ScorePartwise        partwise = Marshalling.unmarshal(is);
            pages.put(pageNumber, partwise);
        }

        return pages;
    }

    //---------------//
    // retrieveFiles //
    //---------------//
    /**
     * Retrieve the map of files whose names match the provided filter
     * @param args an array of 3 strings: folder, prefix, suffix
     * @return the sorted map of matching files, indexed by their number
     */
    private static SortedMap<Integer, File> retrieveFiles (String... args)
    {
        SortedMap<Integer, File> map = new TreeMap<Integer, File>();
        File                     dir = new File(args[0]);
        String                   prefix = args[1];
        String                   suffix = args[2];
        MyFilenameFilter         filter = new MyFilenameFilter(prefix, suffix);
        File[]                   files = dir.listFiles(filter);

        if (files.length == 0) {
            logger.warning(
                "No file matching " + new File(dir, prefix + "*" + suffix));
        } else {
            for (File file : files) {
                map.put(filter.getFileNumber(file.getName()), file);
            }
        }

        return map;
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------------//
    // MyFilenameFilter //
    //------------------//
    /**
     * Specific file filter to retrieve file names that match the filter
     * prefix + <some_number> + suffix
     */
    private static class MyFilenameFilter
        implements FilenameFilter
    {
        //~ Instance fields ----------------------------------------------------

        // Mandatory beginning of file name
        final String prefix;

        // Mandatory ending of file name
        final String suffix;

        //~ Constructors -------------------------------------------------------

        public MyFilenameFilter (String prefix,
                                 String suffix)
        {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        //~ Methods ------------------------------------------------------------

        public Integer getFileNumber (String name)
        {
            String numStr = name.substring(
                prefix.length(),
                name.length() - suffix.length());

            ///logger.info("num:" + numStr);
            try {
                return Integer.decode(numStr);
            } catch (Exception ex) {
                logger.warning(
                    "Cannot decode number \"" + numStr + "\" in " + name);

                return null;
            }
        }

        public boolean accept (File   dir,
                               String name)
        {
            if (!name.startsWith(prefix)) {
                return false;
            }

            if (!name.endsWith(suffix)) {
                return false;
            }

            // Check we can decode a number in this portion of the name
            Integer num = getFileNumber(name);

            return num != null;
        }
    }
}
