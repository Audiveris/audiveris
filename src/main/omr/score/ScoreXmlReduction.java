//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S c o r e X m l R e d u c t i o n                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.score.PartConnection.Candidate;
import omr.score.PartConnection.Result;

import omr.util.StopWatch;
import omr.util.WrappedBoolean;
import omr.util.XmlUtil;

import com.audiveris.proxymusic.Credit;
import com.audiveris.proxymusic.Instrument;
import com.audiveris.proxymusic.MidiInstrument;
import com.audiveris.proxymusic.Note;
import com.audiveris.proxymusic.PartList;
import com.audiveris.proxymusic.Print;
import com.audiveris.proxymusic.ScoreInstrument;
import com.audiveris.proxymusic.ScorePart;
import com.audiveris.proxymusic.ScorePartwise;
import com.audiveris.proxymusic.ScorePartwise.Part;
import com.audiveris.proxymusic.ScorePartwise.Part.Measure;
import com.audiveris.proxymusic.YesNo;
import com.audiveris.proxymusic.util.Marshalling;
import com.audiveris.proxymusic.util.Marshalling.MarshallingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.bind.JAXBException;

/**
 * Class {@code ScoreXmlReduction} is the "reduce" part of a MapReduce job for a score,
 * based on the merge of MusicXML page contents.
 * <ol>
 * <li>Any Map task processes a score page and produces the related XML fragment as its output.</li>
 * <li>The Reduce task takes all the XML fragments as input and consolidates them in a global Score
 * output.</li></ol>
 * <p>
 * Typical calling of the feature is as follows:
 * <code>
 * <pre>
 * Map&lt;Integer, String&gt; fragments = ...;
 * ScoreXmlReduction reduction = new ScoreXmlReduction(fragments);
 * String output = reduction.reduce();
 * Map&lt;Integer, Status&gt; statuses = reduction.getStatuses();
 * </pre>
 * </code>
 * <p>
 * <b>Features not yet implemented:</b> <ul>
 * <li>Connection of slurs between pages</li>
 * <li>In part-list, handling of part-group beside score-part</li>
 * </ul>
 * <p>
 * <b>Test:</b> A main() method is provided only to ease the testing of this class, assuming that
 * the individual pages have already been scanned and their XML fragments are available on disk.
 * It requires 3 arguments: name of the folder to lookup, prefix for matching files, suffix for
 * matching files.
 * It with search the specified folder for matching files, read their content as XML fragments,
 * launch a ScoreXmlReduction instance on this data and finally write the global score in the input
 * folder.
 * <p>
 * <b>Relevant MusicXML elements:</b><br/>
 * <img src="doc-files/Part.jpg" />
 *
 * @author Hervé Bitteur
 */
public class ScoreXmlReduction
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ScoreXmlReduction.class);

    /** Just for debug */
    private static StopWatch watch;

    //~ Enumerations -------------------------------------------------------------------------------
    /** End status of processing for a single XML fragment */
    public static enum Status
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Fragment was processed correctly */
        OK,
        /** Some invalid XML
         * characters had to be skipped */
        CHARACTERS_SKIPPED,
        /** The fragment as
         * a whole could not be processed */
        FRAGMENT_FAILED;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Map of XML fragments, one entry per page */
    private final Map<Integer, String> fragments;

    /** Map of fragments final statuses, one status per page */
    private final Map<Integer, Status> statuses;

    /** Factory for proxymusic entities */
    private final com.audiveris.proxymusic.ObjectFactory factory = new com.audiveris.proxymusic.ObjectFactory();

    /** Global connection of parts */
    private PartConnection connection;

    /** Map of (new) ScorePart -> (new) Part */
    private Map<ScorePart, Part> partData;

    /** Map of old ScorePart -> new ScorePart */
    private Map<ScorePart, ScorePart> newParts;

    /** Map of old ScoreInstrument -> new ScoreInstrument */
    private Map<ScoreInstrument, ScoreInstrument> newInsts;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ScoreXmlReduction object.
     *
     * @param fragments a map of XML fragments, one entry per page, the key
     *                  being the page number and the value being the MusicXML fragment produced
     *                  from the page.
     */
    public ScoreXmlReduction (Map<Integer, String> fragments)
    {
        this.fragments = fragments;

        statuses = new TreeMap<Integer, Status>();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // main //
    //------//
    /**
     * Pseudo-main test method, just to allocate an instance of
     * ScoreXmlReduction,
     * launch the reduce() method, and print the results. The resulting score
     * in written to a global.xml file in the same folder as the input pieces.
     *
     * @param args the template items to filter relevant files
     */
    public static void main (String... args)
            throws MarshallingException, JAXBException, FileNotFoundException, IOException
    {
        //        // TODO QUICK & DIRTY HACK!!!!!!!!!!!!!!!!!!!!!!!!
        //        String[] args = new String[] {
        //                            "u:/soft/audi-bugs/multipage-bis/haffner", "p",
        //                            "^Smartscore-10.2.1.xml"
        //                        };
        watch = new StopWatch("Global measurement");

        // Checking parameters
        if (args.length != 3) {
            for (int i = 0; i < args.length; i++) {
                logger.info("args[{}] = \"{}\"", i, args[i]);
            }

            throw new IllegalArgumentException("Expected 3 arguments (folder, prefix, suffix)");
        }

        // Selecting files
        File dir = new File(args[0]);
        String prefix = args[1].trim();
        String suffix = args[2];
        SortedMap<Integer, File> files = selectFiles(dir, prefix, suffix);

        if ((files == null) || files.isEmpty()) {
            logger.warn("No file selected");

            return;
        }

        // Reading files without any checking
        SortedMap<Integer, String> fragments = readFiles(files);

        // Reduction
        ScoreXmlReduction reduction = new ScoreXmlReduction(fragments);
        String output = reduction.reduce();

        logger.info("Output.length: {}", output.length());

        //        if (logger.isDebugEnabled()) {
        //            logger.debug("Output:\n" + output);
        //        }
        // For debugging
        watch.start("Writing output file");

        File file = new File(dir, prefix + "global.xml");
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(output.getBytes());
        fos.close();
        logger.info("Output written to {}", file);

        watch.print();

        // Final statuses
        System.out.println("\nProcessing results:");

        for (Entry<Integer, Status> entry : reduction.getStatuses().entrySet()) {
            System.out.println(
                    String.format("Fragment #%3d: %s", entry.getKey(), entry.getValue()));
        }
    }

    //-------------//
    // getStatuses //
    //-------------//
    /**
     * Report for each input fragment the final processing status
     *
     * @return a map (fragment ID -> processing status)
     */
    public Map<Integer, Status> getStatuses ()
    {
        return statuses;
    }

    //--------//
    // reduce //
    //--------//
    /**
     * Build a score output as the smart concatenation of the fragments produced
     * from each page. The fragments are a map of XML fragments, the map key
     * being the page number in the containing score. They are provided to the
     * ScoreXmlReduction constructor.
     * <p>
     * The final processing status for each fragment is made available
     * through the {@link #getStatuses()} method.</p>
     *
     * @return the resulting global XML output for the score
     */
    public String reduce ()
            throws MarshallingException, JAXBException
    {
        // Preloading of JAXBContext
        watch.start("Preloading JAXB Context");
        Marshalling.getContext(ScorePartwise.class);

        // Initialize statuses
        for (Integer page : fragments.keySet()) {
            statuses.put(page, Status.OK);
        }

        // Unmarshall pages (MusicXML fragments -> ScorePartwise instances)
        SortedMap<Integer, ScorePartwise> partwises = unmarshallPages(fragments);

        if (partwises.isEmpty()) {
            return "";
        }

        // Consolidate (set of {page ScorePartwise} -> 1! global ScorePartwise)
        ScorePartwise globalPartwise = merge(partwises);

        // Build output (global ScorePartwise -> MusicXML)
        return buildOutput(globalPartwise);
    }

    //-----------//
    // addHeader //
    //-----------//
    /**
     * Create the header of the global partwise, by replicating information
     * form first page header
     *
     * @param global the global partwise to update
     * @param pages  the individual page partwise instances
     */
    private void addHeader (ScorePartwise global,
                            SortedMap<Integer, ScorePartwise> pages)
    {
        //
        //  <!ENTITY % score-header
        //          "(work?, movement-number?, movement-title?,
        //            identification?, defaults?, credit*, part-list)">
        //

        // First page data
        ScorePartwise first = pages.get(pages.firstKey());

        // work?
        if (first.getWork() != null) {
            global.setWork(first.getWork());
        }

        // movement-number?
        if (first.getMovementNumber() != null) {
            global.setMovementNumber(first.getMovementNumber());
        }

        // movement-title?
        if (first.getMovementTitle() != null) {
            global.setMovementTitle(first.getMovementTitle());
        }

        // identification?
        if (first.getIdentification() != null) {
            // TODO Encoding:
            // - Signature is inserted twice (page then global)
            // - Source should be the whole score file, not the first page file
            global.setIdentification(first.getIdentification());
        }

        // defaults?
        if (first.getDefaults() != null) {
            global.setDefaults(first.getDefaults());
        }

        // credit(s) for first page and others as well
        for (Entry<Integer, ScorePartwise> entry : pages.entrySet()) {
            int index = entry.getKey();
            ScorePartwise page = entry.getValue();
            List<Credit> credits = page.getCredit();

            if (!credits.isEmpty()) {
                // Add page index
                insertPageIndex(index, credits);
                global.getCredit().addAll(credits);
            }
        }
    }

    //-------------//
    // addPartList //
    //-------------//
    /**
     * Build the part-list as the sequence of Result/ScorePart instances, and
     * map each of them to a Part.
     * Create list2part, newParts, newInsts
     */
    private void addPartList (ScorePartwise global)
    {
        // Map ScorePart -> Part data
        partData = new HashMap<ScorePart, Part>();

        PartList partList = factory.createPartList();
        global.setPartList(partList);

        for (Result result : connection.getResultMap().keySet()) {
            ScorePart scorePart = (ScorePart) result.getUnderlyingObject();
            partList.getPartGroupOrScorePart().add(scorePart);

            Part globalPart = factory.createScorePartwisePart();
            globalPart.setId(scorePart);
            global.getPart().add(globalPart);
            partData.put(scorePart, globalPart);
        }

        // Align each candidate to its related result */
        newParts = new HashMap<ScorePart, ScorePart>();
        newInsts = new HashMap<ScoreInstrument, ScoreInstrument>();

        for (Result result : connection.getResultMap().keySet()) {
            ScorePart newSP = (ScorePart) result.getUnderlyingObject();

            for (Candidate candidate : connection.getResultMap().get(result)) {
                ScorePart old = (ScorePart) candidate.getUnderlyingObject();
                newParts.put(old, newSP);

                // Map the instruments. We use the same order
                List<ScoreInstrument> newInstruments = newSP.getScoreInstrument();

                for (int idx = 1; idx <= old.getScoreInstrument().size(); idx++) {
                    ScoreInstrument si = old.getScoreInstrument().get(idx - 1);

                    if (idx > newInstruments.size()) {
                        logger.debug("{} #{} Creating {}", result, idx, stringOf(si));
                        newInstruments.add(si);
                        si.setId("P" + result.getId() + "-I" + idx);

                        // Related Midi instrument
                        for (Object obj : old.getMidiDeviceAndMidiInstrument()) {
                            if (obj instanceof MidiInstrument) {
                                MidiInstrument midi = (MidiInstrument) obj;

                                if (midi.getId() == si) {
                                    newSP.getMidiDeviceAndMidiInstrument().add(midi);

                                    break;
                                }
                            }
                        }
                    } else {
                        logger.debug("{} #{} Reusing {}", result, idx, stringOf(si));
                    }

                    newInsts.put(si, newInstruments.get(idx - 1));
                }
            }
        }
    }

    //--------------//
    // addPartsData //
    //--------------//
    /**
     * Populate all part elements
     * Page after page, append to each part the proper measures of the page
     *
     * @param pages the individual page partwise instances
     */
    private void addPartsData (SortedMap<Integer, ScorePartwise> pages)
    {
        int midOffset = 0; // Page offset on measure id
        boolean isFirstPage = true; // First page?

        for (Entry<Integer, ScorePartwise> entry : pages.entrySet()) {
            ScorePartwise page = entry.getValue();
            int mid = 0; // Measure id (in this page)

            for (Part part : page.getPart()) {
                ScorePart oldScorePart = (ScorePart) part.getId();
                ScorePart newScorePart = newParts.get(oldScorePart);
                logger.info(
                        "page:{} old:{} new:{}",
                        entry.getKey(),
                        oldScorePart.getId(),
                        newScorePart.getId());

                Part globalPart = partData.get(newScorePart);

                if (newScorePart != oldScorePart) {
                    part.setId(newScorePart);
                }

                boolean isFirstMeasure = true; // First measure? (in this page)

                // Update measure in situ and reference them from containing part
                for (Measure measure : part.getMeasure()) {
                    logger.debug(
                            "page#{} part:{} Measure#{}",
                            entry.getKey(),
                            oldScorePart.getId(),
                            measure.getNumber());

                    // New page?
                    if (!isFirstPage && isFirstMeasure) {
                        // Insert/Update print element
                        getPrint(measure.getNoteOrBackupOrForward()).setNewPage(YesNo.YES);
                    }

                    // Shift measure number
                    mid = Integer.decode(measure.getNumber());
                    measure.setNumber("" + (mid + midOffset));
                    globalPart.getMeasure().add(measure);

                    // Instrument references, if any
                    for (Object obj : measure.getNoteOrBackupOrForward()) {
                        if (obj instanceof Note) {
                            Note note = (Note) obj;
                            Instrument inst = note.getInstrument();

                            if (inst != null) {
                                inst.setId(newInsts.get((ScoreInstrument) inst.getId()));
                            }
                        }
                    }

                    isFirstMeasure = false;
                }
            }

            midOffset += mid;
            isFirstPage = false;
        }
    }

    //-------------//
    // buildOutput //
    //-------------//
    /**
     * Marshall the global partwise into a String
     *
     * @param globalPartwise the global partwise we have built
     * @return the marshalled string
     * @throws JAXBException
     *                       throws
     *                       IOException
     */
    private String buildOutput (ScorePartwise globalPartwise)
            throws MarshallingException
    {
        watch.start("Marshalling output");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Marshalling.marshal(globalPartwise, os, true, 2);

        return os.toString();
    }

    //-------------------//
    // dumpResultMapping //
    //-------------------//
    /**
     * Debug: List details of all candidates per result
     */
    private void dumpResultMapping ()
    {
        for (Entry<Result, Set<Candidate>> entry : connection.getResultMap().entrySet()) {
            logger.debug("Result: {}", entry.getKey());

            ScorePart spr = (ScorePart) entry.getKey().getUnderlyingObject();

            for (com.audiveris.proxymusic.ScoreInstrument si : spr.getScoreInstrument()) {
                logger.debug("-- final inst: {} {}", si.getId(), si.getInstrumentName());
            }

            for (Candidate candidate : entry.getValue()) {
                logger.debug("* candidate: {}", candidate);

                ScorePart sp = (ScorePart) candidate.getUnderlyingObject();

                for (com.audiveris.proxymusic.ScoreInstrument si : sp.getScoreInstrument()) {
                    logger.debug("-- instrument: {} {}", si.getId(), si.getInstrumentName());
                }
            }
        }
    }

    //-------------//
    // fillResults //
    //-------------//
    /**
     * We fill results with data copied from the candidates
     */
    private void fillResults ()
    {
        for (Result result : connection.getResultMap().keySet()) {
            ScorePart newSP = (ScorePart) result.getUnderlyingObject();

            for (Candidate candidate : connection.getResultMap().get(result)) {
                ScorePart old = (ScorePart) candidate.getUnderlyingObject();

                // Score instruments. We use the same order
                List<ScoreInstrument> newInstruments = newSP.getScoreInstrument();

                for (int idx = 1; idx <= old.getScoreInstrument().size(); idx++) {
                    ScoreInstrument si = old.getScoreInstrument().get(idx - 1);

                    if (idx > newInstruments.size()) {
                        logger.debug("{} #{} Creating {}", result, idx, stringOf(si));
                        newInstruments.add(si);
                        si.setId("P" + result.getId() + "-I" + idx);

                        // Related Midi instrument
                        for (Object obj : old.getMidiDeviceAndMidiInstrument()) {
                            if (obj instanceof MidiInstrument) {
                                MidiInstrument midi = (MidiInstrument) obj;

                                if (midi.getId() == si) {
                                    newSP.getMidiDeviceAndMidiInstrument().add(midi);

                                    break;
                                }
                            }
                        }
                    } else {
                        logger.debug("{} #{} Reusing {}", result, idx, stringOf(si));
                    }

                    newInsts.put(si, newInstruments.get(idx - 1));
                }

                // Group
                if (!old.getGroup().isEmpty() && newSP.getGroup().isEmpty()) {
                    newSP.getGroup().addAll(old.getGroup());
                }

                // Identification
                if ((old.getIdentification() != null) && (newSP.getIdentification() == null)) {
                    newSP.setIdentification(old.getIdentification());
                }

                // Midi device
                //TODO: Translate this from MusicXML 2.0 to 3.0
                //                if ((old.getMidiDevice() != null)
                //                    && (newSP.getMidiDevice() == null)) {
                //                    newSP.setMidiDevice(old.getMidiDevice());
                //                }
                // Name display
                if ((old.getPartNameDisplay() != null) && (newSP.getPartNameDisplay() == null)) {
                    newSP.setPartNameDisplay(old.getPartNameDisplay());
                }

                // Abbreviation display
                if ((old.getPartAbbreviationDisplay() != null)
                    && (newSP.getPartAbbreviationDisplay() == null)) {
                    newSP.setPartAbbreviationDisplay(old.getPartAbbreviationDisplay());
                }
            }
        }
    }

    //----------//
    // getPrint //
    //----------//
    /**
     * Retrieve the Print element in the object list, even if we need to create
     * a new one and insert it to the list
     *
     * @param noteOrBackupOrForward the list to search (and update)
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

    //-----------------//
    // insertPageIndex //
    //-----------------//
    /**
     * Insert proper page index in credit elements
     *
     * @param index   the page index to insert
     * @param credits the credits to update
     */
    private void insertPageIndex (int index,
                                  List<Credit> credits)
    {
        for (Credit credit : credits) {
            credit.setPage(new BigInteger("" + index));
        }
    }

    //-------//
    // merge //
    //-------//
    /**
     * This is the heart of reduction task, consolidating the outputs of
     * individual pages
     *
     * @param pages the individual pages, indexed by their page number
     * @return the resulting global score partwise
     */
    private ScorePartwise merge (SortedMap<Integer, ScorePartwise> pages)
    {
        watch.start("Merge");

        // Resulting data
        ScorePartwise global = new ScorePartwise();

        // Score header: more or less reuse the header of first page
        addHeader(global, pages);

        /* Connect parts across the pages */
        connection = PartConnection.connectProxyPages(pages);

        // Force the ids of all ScorePart's
        if (logger.isDebugEnabled()) {
            numberResults();
        }

        // part-list (-> list2part, newParts, newInsts)
        // and ScoreInstrument's ids
        addPartList(global);

        // Fill each of the score part results with elements from candidates
        fillResults();

        // Debug: List all candidates per result
        dumpResultMapping();

        // parts data, inserting page breaks, re-numbering measures
        addPartsData(pages);

        // Handle cross-page slurs
        // TBD
        // The end
        return global;
    }

    //---------------//
    // numberResults //
    //---------------//
    /**
     * Force the id of each result (score-part) as P1, P2, etc.
     */
    private void numberResults ()
    {
        int partIndex = 0;

        for (Result result : connection.getResultMap().keySet()) {
            ScorePart scorePart = (ScorePart) result.getUnderlyingObject();
            String partId = "P" + ++partIndex;
            scorePart.setId(partId);
        }
    }

    //-----------//
    // readFiles //
    //-----------//
    /**
     * Simply read the files raw content into strings in memory
     *
     * @param files the collection of files to read
     * @return the collection of raw XML fragments
     */
    private static SortedMap<Integer, String> readFiles (SortedMap<Integer, File> files)
    {
        watch.start("Reading input files");

        SortedMap<Integer, String> fragments = new TreeMap<Integer, String>();

        for (Map.Entry<Integer, File> entry : files.entrySet()) {
            BufferedReader input;
            File file = entry.getValue();

            try {
                input = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException ex) {
                System.err.println(ex + " " + file);

                continue;
            }

            StringBuilder fragment = new StringBuilder();
            String line;

            try {
                while ((line = input.readLine()) != null) {
                    fragment.append(line).append("\n");
                }
            } catch (IOException ex) {
                System.err.println(ex + " " + file);

                continue;
            }

            fragments.put(entry.getKey(), fragment.toString());
        }

        return fragments;
    }

    //-------------//
    // selectFiles //
    //-------------//
    /**
     * Retrieve the map of files whose names match the provided filter
     *
     * @param dir    path to folder where files are to be read
     * @param prefix prefix of desired file names
     * @param suffix suffix of desired file names
     * @return the sorted map of matching files, indexed by their number
     */
    private static SortedMap<Integer, File> selectFiles (File dir,
                                                         String prefix,
                                                         String suffix)
    {
        SortedMap<Integer, File> map = new TreeMap<Integer, File>();
        MyFilenameFilter filter = new MyFilenameFilter(prefix, suffix);
        File[] files = dir.listFiles(filter);

        if (files == null) {
            logger.warn("Cannot read folder {}", dir);

            return null;
        }

        File template = new File(dir, prefix + "*" + suffix);
        logger.info("Looking for {}", template);

        if (files.length == 0) {
            logger.warn("No file matching {}", template);
        } else {
            for (File file : files) {
                map.put(filter.getFileNumber(file.getName()), file);
            }
        }

        return map;
    }

    //----------//
    // stringOf //
    //----------//
    private String stringOf (ScoreInstrument si)
    {
        StringBuilder sb = new StringBuilder("{ScoreInstrument");
        sb.append(" id:").append(si.getId());
        sb.append(" name:\"").append(si.getInstrumentName()).append("\"");

        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // unmarshallPages //
    //-----------------//
    /**
     * Retrieve individual page partwise instances, by unmarshalling MusicXML
     * data from the page string fragments
     *
     * @param pageFragments the sequence of input fragments (one string per
     *                      page)
     * @return the related sequence of partwise instances (one instance per
     *         page)
     * @throws JAXBException
     */
    private SortedMap<Integer, ScorePartwise> unmarshallPages (Map<Integer, String> pageFragments)
            throws JAXBException
    {
        ///watch.start("Unmarshalling pages");

        // Access the page fragments in the right order
        SortedSet<Integer> pageNumbers = new TreeSet<Integer>(pageFragments.keySet());
        logger.info("About to read fragments {}", pageNumbers);

        /** For user feedback */
        String range = " of [" + pageNumbers.first() + ".." + pageNumbers.last() + "]...";

        /* Load pages content */
        SortedMap<Integer, ScorePartwise> pages = new TreeMap<Integer, ScorePartwise>();

        for (int pageNumber : pageNumbers) {
            watch.start("Unmarshalling page #" + pageNumber);

            ///logger.info("Unmarshalling fragment " + pageNumber + range);
            String rawFragment = pageFragments.get(pageNumber);

            // Filter out invalid XML characters if any
            WrappedBoolean stripped = new WrappedBoolean(false);
            String fragment = XmlUtil.stripNonValidXMLCharacters(rawFragment, stripped);

            if (stripped.isSet()) {
                logger.warn("Illegal XML characters found in fragment #{}", pageNumber);
                statuses.put(pageNumber, Status.CHARACTERS_SKIPPED);
            }

            ByteArrayInputStream is = new ByteArrayInputStream(fragment.getBytes());

            try {
                ScorePartwise partwise = (ScorePartwise) Marshalling.unmarshal(is);
                pages.put(pageNumber, partwise);
            } catch (Exception ex) {
                logger.warn("Could not unmarshall fragment #{} {}", pageNumber, ex);
                statuses.put(pageNumber, Status.FRAGMENT_FAILED);
            }
        }

        return pages;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
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
        //~ Instance fields ------------------------------------------------------------------------

        // Mandatory beginning of file name
        final String prefix;

        // Mandatory ending of file name
        final String suffix;

        //~ Constructors ---------------------------------------------------------------------------
        public MyFilenameFilter (String prefix,
                                 String suffix)
        {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean accept (File dir,
                               String name)
        {
            ///logger.info("dir: " + dir + " name: " + name);
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

        public Integer getFileNumber (String name)
        {
            String numStr = name.substring(prefix.length(), name.length() - suffix.length());

            // Beware of leading zeros
            while ((numStr.length() > 1) && numStr.startsWith("0")) {
                numStr = numStr.substring(1);
            }

            try {
                return Integer.decode(numStr);
            } catch (Exception ex) {
                logger.warn(
                        "Cannot decode number \"" + numStr + "\" in file name \"" + name + "\"",
                        ex);

                return null;
            }
        }
    }
}
