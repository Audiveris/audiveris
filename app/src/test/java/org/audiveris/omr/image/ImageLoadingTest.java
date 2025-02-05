//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 I m a g e L o a d i n g T e s t                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Unit tests for {@link ImageLoading}
 */
public class ImageLoadingTest
{

    /**
     * Test of getImage method for PDF loading
     */
    @Test
    public void testGetImage ()
            throws IOException
    {
        assertPdfLoadedCorrectly("Dichterliebe01", 2);
        assertPdfLoadedCorrectly("SchbAvMaSample_rotated", 1);
    }

    private void assertPdfLoadedCorrectly(String testCaseName, int expectedImageCount)
            throws IOException
    {
        Path input = getResourcePath(testCaseName + ".pdf");

        ImageLoading.Loader loader = ImageLoading.getLoader(input);
        Assert.assertNotNull(loader);

        try {
            Assert.assertEquals(loader.getImageCount(), expectedImageCount);

            for (int imageId = 1; imageId <= expectedImageCount; imageId++) {
                BufferedImage actual = loader.getImage(imageId);

                Path expectedOutput = getResourcePath(testCaseName + "-" + imageId + ".png");
                BufferedImage expected = ImageIO.read(expectedOutput.toFile());

                assertImageEquals(actual, expected);
            }
        } finally {
            loader.dispose();
        }
    }

    private void assertImageEquals(BufferedImage actual, BufferedImage expected) {
        // allow delta of 1 pixel (due to rounding)
        final int acceptedDelta = 1;

        // compare "meta data"
        Assert.assertEquals(actual.getWidth(), expected.getWidth(), acceptedDelta);
        Assert.assertEquals(actual.getHeight(), expected.getHeight(), acceptedDelta);
        Assert.assertEquals(actual.getColorModel().getColorSpace(), expected.getColorModel().getColorSpace());

        // compare images pixel-wise
        final int height = Math.min(actual.getHeight(), expected.getHeight());
        final int width = Math.min(actual.getWidth(), expected.getWidth());
        int deviationCount = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (actual.getRGB(x, y) != expected.getRGB(x, y)) {
                    deviationCount++;
                }
            }
        }

        // assert amount of differences is small
        final int pixelCount = height * width;
        final float percentDeviations = 100f * deviationCount / pixelCount;
        Assert.assertTrue(String.format("Too many pixels are different (%f percent)", percentDeviations),
                          percentDeviations < 1);
    }

    private Path getResourcePath(String resourceName) {
        try {
            URL resourceUrl = getClass().getResource(resourceName);
            Objects.requireNonNull(resourceUrl, String.format("Could not find resource '%s'", resourceName));
            return Path.of(resourceUrl.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

}
