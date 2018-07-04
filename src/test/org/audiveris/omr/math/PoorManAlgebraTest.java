/*
 *
 * Copyright © Audiveris 2018. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.audiveris.omr.math;

import org.audiveris.omr.math.PoorManAlgebra.INDArray;
import org.audiveris.omr.math.PoorManAlgebra.Nd4j;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 *
 * @author Hervé Bitteur
 */
public class PoorManAlgebraTest
{

    private static final Logger logger = LoggerFactory.getLogger(PoorManAlgebraTest.class);

    private static final double eps = 0.0001;

    public PoorManAlgebraTest ()
    {
    }

    @BeforeClass
    public static void setUpClass ()
    {
    }

    @AfterClass
    public static void tearDownClass ()
    {
    }

    @Before
    public void setUp ()
    {
    }

    @After
    public void tearDown ()
    {
    }

    @Test
    public void testNd4jcreateScalar ()
    {
        double val = 123;
        INDArray scalar = Nd4j.scalar(val);
        logger.info("scalar:{}", scalar);
        assertEquals(val, scalar.getDouble(0), eps);
    }

    @Test
    public void testNd4jcreateVector ()
    {
        double[] val = new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        INDArray vector = Nd4j.create(val);
        logger.info("vector:{}", vector);
        assertEquals(10, vector.columns());
    }

    @Test
    public void testNd4jcreateMatrix ()
    {
        double[] v0 = new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        double[] v1 = new double[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        double[] v2 = new double[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
        double[][] val = new double[3][];
        val[0] = v0;
        val[1] = v1;
        val[2] = v2;
        INDArray matrix = Nd4j.create(val);
        logger.info("matrix:{}", matrix);
        assertEquals(3, matrix.rows());
        assertEquals(10, matrix.columns());

        INDArray means = matrix.mean(0);
        logger.info("means:{}", means);

        INDArray stds = matrix.std(0);
        logger.info("stds:{}", stds);
        logger.info("matrix:{}", matrix);

        matrix.subiRowVector(means);
        logger.info("after subiRowVector matrix:{}", matrix);

        matrix.diviRowVector(stds);
        logger.info("after diviRowVector matrix:{}", matrix);
    }

}
