//----------------------------------------------------------------------------//
//                                                                            //
//                                  M a i n                                   //
//                                                                            //
//----------------------------------------------------------------------------//
package com.audiveris.musicxmldiff;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.ConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * Main entry point for MusicXmlDiff utility.
 *
 * @author Hervé Bitteur
 */
public class Main
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /** Parameters read from CLI */
    private static CLI.Parameters parameters;

    //~ Instance fields --------------------------------------------------------
    /** Control file. */
    private final File controlFile;

    /** Test file. */
    private final File testFile;

    /** Results. */
    private final Printer output;

    /** Filtering info, if any. */
    private final File filterFile;

    //~ Constructors -----------------------------------------------------------
    //
    //------//
    // Main //
    //------//
    /**
     * Creates a new Main object.
     *
     * @param controlFile Control file
     * @param testFile    Test file
     * @param filterFile  Filter file, or null
     * @param output      PrintStream for output
     */
    public Main (File controlFile,
                 File testFile,
                 File filterFile,
                 PrintStream output)
    {
        this.controlFile = controlFile;
        this.testFile = testFile;
        this.filterFile = filterFile;
        this.output = new MusicPrinter(output);
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // compare //
    //---------//
    public void compare ()
    {
        // Comparing
        try {
            output.println("Comparing " + controlFile + " to " + testFile);

            final Document controlDoc;
            final Document testDoc;

            try (InputStream cis = new FileInputStream(controlFile)) {
                controlDoc = new PositionalXMLReader().readXML(cis);
            }
            try (InputStream tis = new FileInputStream(testFile)) {
                testDoc = new PositionalXMLReader().readXML(tis);
            }

            // Tuning
            XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
            XMLUnit.setNormalizeWhitespace(true);
            XMLUnit.setIgnoreWhitespace(true);

            ///XMLUnit.setIgnoreComments(true); NO!!!!!!!!
            // The setIgnoreComments triggers the use of XSLT transform
            // which 1/ ruins userdata and 2/ fails on xml:space and xml:lang.
            // Moreover, comments are actually ignored by Diff

            ///XMLUnit.setCompareUnmatched(false); NO need

            // Customization
            Filter filter = (filterFile != null)
                    ? new BasicFilter(new FileInputStream(filterFile))
                    : null;

            Diff diff = new Diff(controlDoc, testDoc, null);
            diff.overrideElementQualifier(
                    new ElementNameAndAttributeQualifier("number"));
            diff.overrideDifferenceListener(
                    new MusicDifferenceListener(filter, output));

            output.println("Similar:     " + diff.similar());
            output.println("Identical:   " + diff.identical());

            DetailedDiff detDiff = new DetailedDiff(diff);

            List differences = detDiff.getAllDifferences();
            output.println();
            output.println("Physical differences: " + differences.size());

            int diffId = 0;

            for (Object object : differences) {
                Difference difference = (Difference) object;

                if (!difference.isRecoverable()
                    && ((filter == null) || !filter.canIgnore(difference))) {
                    diffId++;
                    output.dump(diffId, difference);
                }
            }
            output.println("Logical  differences: " + diffId);
            logger.info("Logical  differences: {}", diffId);

        } catch (ConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    //
    //------//
    // main //
    //------//
    /**
     * Compare the XML files provided.
     *
     * @param args control, test, filter if any, output if any
     */
    public static void main (String[] args)
            throws IOException
    {
        // First get the provided arguments if any
        parameters = new CLI(args).getParameters();

        if (parameters == null) {
            logger.warn("Exiting ...");

            // Stop the JVM, with failure status (1)
            Runtime.getRuntime()
                    .exit(1);
        }

        // Input control file
        if (parameters.controlPath == null) {
            logger.error("*** Missing control path");

            return;
        }

        File controlFile = new File(parameters.controlPath);
        logger.info("Control file: {}", controlFile.getCanonicalFile());

        // Input test file
        if (parameters.testPath == null) {
            logger.error("*** Missing test path");

            return;
        }

        File testFile = new File(parameters.testPath);
        logger.info("Test    file: {}", testFile.getCanonicalPath());

        // Filtering infos
        final File filterFile;

        if (parameters.filterPath != null) {
            filterFile = new File(parameters.filterPath);
        } else {
            filterFile = null;
        }

        logger.info("Filter  file: {}", 
                filterFile == null ? null : filterFile.getCanonicalPath());

        // Output file
        PrintStream printStream;

        if (parameters.outputPath != null) {
            String psName = parameters.outputPath;

            try {
                printStream = new PrintStream(psName);
                logger.info("Output  file: {}",
                        new File(psName).getCanonicalPath());
            } catch (FileNotFoundException ex) {
                logger.error("*** Cannot create output {}", psName);
                printStream = System.out;
            }
        } else {
            printStream = System.out;
        }

        // Launch the comparison
        new Main(controlFile, testFile, filterFile, printStream).compare();
    }
}
