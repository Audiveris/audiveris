//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P o p u l a t i o n T e s t                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.math;

import static junit.framework.Assert.*;

import org.audiveris.omr.util.BaseTestCase;

import org.junit.Test;

/**
 * Class <code>PopulationTest</code> performs unit tests on Population class.
 *
 * @author Hervé Bitteur
 */
public class PopulationTest
        extends BaseTestCase
{

    //-----------//
    // testEmpty //
    //-----------//
    @Test
    public void testEmpty ()
    {
        Population p = new Population();

        assertEquals("No values cumulated so far.", 0, p.getCardinality());

        try {
            double mv = p.getMeanValue();
            fail(
                    "Exception should be raised"
                            + " when retrieving mean value of an empty population");
        } catch (Exception expected) {
            checkException(expected);
        }

        try {
            double sd = p.getStandardDeviation();
            fail(
                    "Exception should be raised"
                            + " when retrieving standard deviation of an empty population");
        } catch (Exception expected) {
            checkException(expected);
        }

        try {
            double v = p.getVariance();
            fail("Exception should be raised" + " when retrieving variance of an empty population");
        } catch (Exception expected) {
            checkException(expected);
        }

        try {
            p.excludeValue(123);
            fail("Exception should be raised" + " when excluding a value from an empty population");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    //-------------//
    // testExclude //
    //-------------//
    @Test
    public void testExclude ()
    {
        Population p = new Population();
        p.includeValue(5);
        p.includeValue(6);
        p.includeValue(8);
        p.includeValue(9);

        p.excludeValue(5);
        p.excludeValue(9);

        assertEquals("Population should contain 2 values.", 2, p.getCardinality());

        assertEquals("Check mean value.", 7d, p.getMeanValue());

        final double v = p.getVariance();
        final double sd = p.getStandardDeviation();

        if (Population.BIASED) {
            assertEquals("Check biased variance of 2 values.", 1d, v);
            assertNears("Check biased standard deviation of 2 values.", Math.sqrt(1), sd);
        } else {
            assertEquals("Check unbiased variance of 2 values.", 2d, v);
            assertNears("Check unbiased standard deviation of 2 values.", Math.sqrt(2), sd);
        }
    }

    //-------------//
    // testInclude //
    //-------------//
    @Test
    public void testInclude ()
    {
        Population p = new Population();
        p.includeValue(5);
        p.includeValue(6);
        p.includeValue(8);
        p.includeValue(9);

        assertEquals("Population should contain 4 values.", 4, p.getCardinality());

        assertEquals("Check mean value.", 7d, p.getMeanValue());

        final double v = p.getVariance();
        final double sd = p.getStandardDeviation();

        if (Population.BIASED) {
            assertEquals("Check biased variance of 4 values.", 2.5, v);
            assertNears("Check biased standard deviation of 4 values.", Math.sqrt(2.5), sd);
        } else {
            assertNears("Check unbiased variance of 4 values.", 3.33333, v);
            assertNears("Check unbiased standard deviation of 4 values.", Math.sqrt(3.33333), sd);
        }
    }

    //---------------//
    // testSingleton //
    //---------------//
    @Test
    public void testSingleton ()
    {
        Population p = new Population();
        double val = 123d;
        p.includeValue(val);

        assertEquals("Population should contain one value.", 1, p.getCardinality());

        assertEquals("Check mean value.", val, p.getMeanValue());

        double var = p.getVariance();
        assertEquals("Check variance.", 0.0, var);

        double std = p.getStandardDeviation();
        assertEquals("Check standard deviation.", 0.0, std);

        p.excludeValue(val);
        assertEquals("Population should contain no value.", 0, p.getCardinality());
    }
}
