//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                    C o n v e r s i o n S c o r e R e g r e s s i o n T e s t                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package conversion.score;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.audiveris.omr.Main;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.mxl.Mxl;
import org.audiveris.proxymusic.util.Marshalling;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;

import static org.audiveris.omr.OMR.COMPRESSED_SCORE_EXTENSION;
import static org.audiveris.omr.OMR.SCORE_EXTENSION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression test that checks Audiveris' accuracy according to samples located in src/test/resources/conversion/score.
 * Similarity of converted input and expected output is measured using class {@link ScoreSimilarity}.
 * To add a new sample, please include its directory name and the resulting conversion score in {@link #TEST_CASES}.
 *
 * @author Peter Greth
 */
@RunWith(Parameterized.class)
public class ConversionScoreRegressionTest
{
    /**
     * List of test cases. Each of these will be tested separately
     */
    private final static List<ConversionScoreTestCase> TEST_CASES = List.of(
            ConversionScoreTestCase.ofSubDirectory("01-klavier").withExpectedConversionScore(15)
    );

    /**
     * The name of the input file for each test case
     */
    private final static String INPUT_FILE_NAME = "input";

    /**
     * The current test case (provided by {@link #testCaseProvider()}, executed separately by JUnit)
     */
    @Parameter
    public ConversionScoreTestCase underTest;

    /**
     * The directory into which Audiveris can output the parsed score
     */
    private Path outputDirectory;

    /**
     * @return a list of test cases that are executed each
     */
    @Parameters(name = "{0}") // "{0}" provides a readable test name using conversion.score.TestCase::toString
    public static Collection<ConversionScoreTestCase> testCaseProvider ()
    {
        return TEST_CASES;
    }

    /**
     * Reduce logging verbosity (increases execution time significantly)
     */
    @BeforeClass
    public static void reduceLoggingVerbosity ()
    {
        Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);
    }

    @Before
    public void createOutputDirectory ()
            throws IOException
    {
        outputDirectory = Files.createTempDirectory(String.format("audiveris-test-%s", underTest.subDirectoryName));
    }

    @After
    public void removeOutputDirectory ()
            throws IOException
    {
        if (Files.exists(outputDirectory)) {
            FileUtil.deleteDirectory(outputDirectory);
        }
    }

    /**
     * The actual regression test, executed for each test case defined in {@link #TEST_CASES}.
     * Converts the input file using Audiveris, then compares the produced result with the expected result.
     * For comparison {@link ScoreSimilarity#conversionScore(ScorePartwise.Part, ScorePartwise.Part)} is used.
     * This test will fail if the conversion score changed in any direction.
     */
    @Test
    public void testConversionScoreChanged ()
            throws IOException,
                   JAXBException,
                   Mxl.MxlException,
                   Marshalling.UnmarshallingException
    {
        ScorePartwise expectedScore = loadXmlScore(underTest.findExpectedOutputFile());
        assertFalse("Could not load expected output: Contains no Part", expectedScore.getPart().isEmpty());

        Path outputMxl = audiverisBatchExport(outputDirectory, underTest.findInputFile());
        ScorePartwise actualScore = loadMxlScore(outputMxl);
        assertFalse("Could not load actual output: Contains no Part", actualScore.getPart().isEmpty());

        int actualConversionScore = ScoreSimilarity.conversionScore(expectedScore, actualScore);
        failIfConversionScoreDecreased(underTest.expectedConversionScore, actualConversionScore);
        failIfConversionScoreIncreased(underTest.expectedConversionScore, actualConversionScore);
    }

    private static ScorePartwise loadXmlScore (Path xmlFile)
            throws IOException,
                   Marshalling.UnmarshallingException
    {
        try (InputStream inputStream = new FileInputStream(xmlFile.toFile())) {
            Object score = Marshalling.unmarshal(inputStream);
            assertTrue(score instanceof ScorePartwise);
            return (ScorePartwise) score;
        }
    }

    private static Path audiverisBatchExport (Path outputDirectory, Path inputFile)
    {
        Main.main(new String[]{
                "-batch",
                "-export",
                "-output", outputDirectory.toAbsolutePath().toString(),
                inputFile.toAbsolutePath().toString()
        });
        Path outputMxlFile = outputDirectory
                .resolve(INPUT_FILE_NAME) // folder named equal to input file name
                .resolve(INPUT_FILE_NAME + COMPRESSED_SCORE_EXTENSION);
        assertTrue(Files.exists(outputMxlFile));
        return outputMxlFile;
    }

    private static ScorePartwise loadMxlScore (Path mxlFile)
            throws Mxl.MxlException,
                   Marshalling.UnmarshallingException,
                   JAXBException,
                   IOException
    {
        try (Mxl.Input outputMxlFileReader = new Mxl.Input(mxlFile.toFile())) {
            ZipEntry xmlEntry = outputMxlFileReader.getEntry(INPUT_FILE_NAME + SCORE_EXTENSION);
            Object score = Marshalling.unmarshal(outputMxlFileReader.getInputStream(xmlEntry));
            assertTrue(score instanceof ScorePartwise);
            return (ScorePartwise) score;
        }
    }

    private static void failIfConversionScoreDecreased (int expectedConversionScore, int actualConversionScore)
    {
        String message = String.format("The conversion score decreased from %d to %d (diff: %d).",
                                       expectedConversionScore,
                                       actualConversionScore,
                                       actualConversionScore - expectedConversionScore);
        assertFalse(message, actualConversionScore < expectedConversionScore);
    }

    private static void failIfConversionScoreIncreased (int expectedConversionScore, int actualConversionScore)
    {
        String message = String.format("Well done, the conversion score increased from %d to %d (diff: %d). " +
                                               "Please adapt conversion.score.TestCase::TEST_CASES accordingly.",
                                       expectedConversionScore,
                                       actualConversionScore,
                                       actualConversionScore - expectedConversionScore);
        assertFalse(message, actualConversionScore > expectedConversionScore);
    }

}
