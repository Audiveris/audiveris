//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                          C o n v e r s i o n S c o r e T e s t C a s e                         //
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

import org.audiveris.omr.util.FileUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

import static org.audiveris.omr.util.FileUtil.fileNameWithoutExtensionMatches;

/**
 * A test case configuration for {@link ConversionScoreRegressionTest}. It consists of
 * - a directory name that should be a subdirectory of src/test/resources/conversion/score, and
 * - an expected conversion score, that will be compared to the actual conversion score.
 *
 * @author Peter Greth
 */
public class ConversionScoreTestCase
{
    private final static String INPUT_FILE_NAME = "input";
    private final static String EXPECTED_OUTPUT_FILE_NAME = "expected-output.xml";

    public String subDirectoryName;
    public int expectedConversionScore;

    public static ConversionScoreTestCase ofSubDirectory (String subDirectoryName)
    {
        ConversionScoreTestCase conversionScoreTestCase = new ConversionScoreTestCase();
        conversionScoreTestCase.subDirectoryName = subDirectoryName;
        return conversionScoreTestCase;
    }

    public ConversionScoreTestCase withExpectedConversionScore (int expectedConversionScore)
    {
        this.expectedConversionScore = expectedConversionScore;
        return this;
    }

    public Path findInputFile ()
    {
        Path testCaseDirectory = getTestCaseDirectory();
        try {
            return FileUtil
                    .findFileInDirectory(testCaseDirectory, fileNameWithoutExtensionMatches(INPUT_FILE_NAME))
                    .orElseThrow()
                    .toAbsolutePath();
        } catch (IOException | NoSuchElementException e) {
            String message = String.format("Could not find file with name '%s.*' in directory %s", INPUT_FILE_NAME,
                                           testCaseDirectory);
            throw new IllegalStateException(message, e);
        }
    }

    public Path findExpectedOutputFile ()
    {
        return getTestCaseDirectory().resolve(EXPECTED_OUTPUT_FILE_NAME).toAbsolutePath();
    }

    private Path getTestCaseDirectory ()
    {
        try {
            return Path.of(getTestCaseDirectoryUrl().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private URL getTestCaseDirectoryUrl ()
    {
        URL resource = getClass().getResource(subDirectoryName);
        String message = String.format("Could not find directory with name '%s' in test resources for package %s",
                                       subDirectoryName, getClass().getPackageName());
        Objects.requireNonNull(resource, message);
        return resource;
    }

    @Override
    public String toString ()
    {
        return String.format("TestCase %s", subDirectoryName);
    }
}
