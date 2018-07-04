//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   P o o r M a n A l g e b r a                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Class {@code PoorManAlgebra} is a "poor man" implementation for a small number of
 * classes of Nd4j and DeepLearning4j.
 * <p>
 * They are meant for temporary use in Audiveris 5.x where we want to remove dependency on these
 * classes (and their need for a 64-bit architecture).
 * The class methods are limited to the strict minimum.
 *
 * @author Hervé Bitteur
 */
public abstract class PoorManAlgebra
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PoorManAlgebra.class);

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //----------//
    // INDArray //
    //----------//
    public static interface INDArray
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * in place addition of two NDArrays
         *
         * @param other the second ndarray to add
         * @return the result of the addition
         */
        INDArray addi (INDArray other);

        /**
         * Returns the number of columns in this matrix (throws exception if not 2d)
         *
         * @return the number of columns in this matrix
         */
        int columns ();

        /**
         * In place division of a row vector
         *
         * @param rowVector the row vector used for division
         * @return the result of the division
         */
        INDArray diviRowVector (INDArray rowVector);

        /**
         * Return the item at the linear index i
         *
         * @param i the index of the item to getScalar
         * @return the item at index j
         */
        double getDouble (int i);

        /**
         * Returns the specified row.
         * Throws an exception if its not a matrix
         *
         * @param i the row to getScalar
         * @return the specified row
         */
        INDArray getRow (int i);

        /**
         * Returns the overall mean of this INDArray
         *
         * @param dimension the dimension to getScalar the mean along
         * @return the mean along the specified dimension of this INDArray
         */
        INDArray mean (int dimension);

        /**
         * Returns the number of rows in this matrix (throws exception if not 2d)
         *
         * @return the number of rows in this matrix
         */
        int rows ();

        /**
         * Standard deviation of an INDArray along a dimension
         *
         * @param dimension the dimension to getScalar the std along
         * @return the standard deviation along a particular dimension
         */
        INDArray std (int dimension);

        /**
         * In place subtraction of a row vector
         *
         * @param rowVector the row vector to subtract
         * @return the result of the subtraction
         */
        INDArray subiRowVector (INDArray rowVector);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // DataSet //
    //---------//
    public static class DataSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final INDArray features;

        private final INDArray labels;

        //~ Constructors ---------------------------------------------------------------------------
        public DataSet (INDArray features,
                        INDArray labels,
                        INDArray featuresMask, // Unused
                        INDArray labelsMask) // Unused

        {
            this.features = features;
            this.labels = labels;
        }

        //~ Methods --------------------------------------------------------------------------------
        public INDArray getFeatures ()
        {
            return features;
        }

        public INDArray getLabels ()
        {
            return labels;
        }
    }

    //------//
    // Nd4j //
    //------//
    public static class Nd4j
    {
        //~ Static fields/initializers -------------------------------------------------------------

        public static double EPS_THRESHOLD = 1e-5;

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Create an INDArray based on the given data layout
         *
         * @param data the data to use
         * @return an INDArray with the given data layout
         */
        public static INDArray create (double[][] data)
        {
            return new Matrix(data);
        }

        /**
         * Create an INDArray based on the given data layout
         *
         * @param data the data to use
         * @return an INDArray with the given data layout
         */
        public static INDArray create (double[] data)
        {
            return new Vector(data);
        }

        /**
         * Read in an INDArray from a data input stream
         *
         * @param dis the data input stream to read from
         * @return the INDArray
         * @throws IOException
         */
        public static INDArray read (DataInputStream dis)
                throws IOException
        {
            throw new UnsupportedOperationException("PoorManAlgebra.Nd4j.read() not supported.");
        }

        /**
         * Create a scalar nd array with the specified value and offset
         *
         * @param value the value of the scalar
         * @return the scalar nd array
         */
        public static INDArray scalar (double value)
        {
            return new Scalar(value);
        }

        /**
         * Write an INDArray to the specified output stream
         *
         * @param arr              the array to write
         * @param dataOutputStream the data output stream to write to
         * @throws IOException
         */
        public static void write (INDArray arr,
                                  DataOutputStream dataOutputStream)
                throws IOException
        {
            throw new UnsupportedOperationException("PoorManAlgebra.Nd4j.write() not supported.");
        }
    }

    private static class Matrix
            implements INDArray
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final double[][] data;

        //~ Constructors ---------------------------------------------------------------------------
        public Matrix (double[][] data)
        {
            final int rowNb = data.length;
            final int colNb = data[0].length;

            this.data = new double[rowNb][];

            for (int ir = 0; ir < rowNb; ir++) {
                final double[] vector = data[ir];
                final double[] thisVector = new double[colNb];
                System.arraycopy(vector, 0, thisVector, 0, colNb);
                this.data[ir] = thisVector;
            }
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public INDArray addi (INDArray other)
        {
            if (other instanceof Scalar) {
                final double val = ((Scalar) other).getDouble(0);
                final int rowNb = rows();
                final int colNb = columns();

                for (int ir = 0; ir < rowNb; ir++) {
                    final double[] vector = data[ir];

                    for (int ic = 0; ic < colNb; ic++) {
                        vector[ic] += val;
                    }
                }

                return this;
            } else {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        @Override
        public int columns ()
        {
            return data[0].length;
        }

        @Override
        public INDArray diviRowVector (INDArray rowVector)
        {
            final int rowNb = rows();
            final int colNb = columns();

            for (int ic = 0; ic < colNb; ic++) {
                double val = rowVector.getDouble(ic);

                for (int ir = 0; ir < rowNb; ir++) {
                    data[ir][ic] /= val;
                }
            }

            return this;
        }

        @Override
        public double getDouble (int i)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public INDArray getRow (int i)
        {
            return Nd4j.create(data[i]);
        }

        @Override
        public INDArray mean (int unused)
        {
            final int rowNb = rows();
            final int colNb = columns();
            final double[] results = new double[colNb];

            for (int ic = 0; ic < colNb; ic++) {
                double s = 0;

                for (int ir = 0; ir < rowNb; ir++) {
                    s += data[ir][ic];
                }

                results[ic] = s / rowNb;
            }

            return Nd4j.create(results);
        }

        @Override
        public int rows ()
        {
            return data.length;
        }

        @Override
        public INDArray std (int unused)
        {
            final int colNb = columns();
            final int rowNb = rows();
            final double[] results = new double[colNb];

            for (int ic = 0; ic < colNb; ic++) {
                double s = 0;
                double s2 = 0d;

                for (int ir = 0; ir < rowNb; ir++) {
                    double val = data[ir][ic];
                    s += val;
                    s2 += (val * val);
                }

                double biasedVariance = Math.max(0, (s2 - ((s * s) / rowNb)) / rowNb);
                double variance = (rowNb * biasedVariance) / (rowNb - 1);
                results[ic] = Math.sqrt(variance);
            }

            return Nd4j.create(results);
        }

        @Override
        public INDArray subiRowVector (INDArray rowVector)
        {
            final int colNb = columns();
            final int rowNb = rows();

            for (int ic = 0; ic < colNb; ic++) {
                double val = rowVector.getDouble(ic);

                for (int ir = 0; ir < rowNb; ir++) {
                    data[ir][ic] -= val;
                }
            }

            return this;
        }

        @Override
        public String toString ()
        {
            final int rowNb = rows();
            final StringBuilder sb = new StringBuilder("[");

            for (int ir = 0; ir < rowNb; ir++) {
                INDArray row = getRow(ir);
                sb.append("\n ").append(row);
            }

            sb.append("\n]");

            return sb.toString();
        }
    }

    private static class Scalar
            implements INDArray
    {
        //~ Instance fields ------------------------------------------------------------------------

        private double data;

        //~ Constructors ---------------------------------------------------------------------------
        public Scalar (double data)
        {
            this.data = data;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public INDArray addi (INDArray other)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int columns ()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public INDArray diviRowVector (INDArray rowVector)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double getDouble (int i)
        {
            return data;
        }

        @Override
        public INDArray getRow (int i)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public INDArray mean (int dimension)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int rows ()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public INDArray std (int dimension)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public INDArray subiRowVector (INDArray rowVector)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String toString ()
        {

            final StringBuilder sb = new StringBuilder("{");

            sb.append(format(data));

            sb.append("}");

            return sb.toString();
        }
    }

    private static String format (double val)
    {
        return String.format("%.2f", val);
    }

    private static class Vector
            implements INDArray
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final double[] data;

        //~ Constructors ---------------------------------------------------------------------------
        public Vector (double[] data)
        {
            this.data = new double[data.length];
            System.arraycopy(data, 0, this.data, 0, data.length);
        }

        // Meant for JAXB
        private Vector ()
        {
            data = null;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public INDArray addi (INDArray other)
        {
            if (other instanceof Scalar) {
                final int colNb = data.length;
                final double val = ((Scalar) other).getDouble(0);

                for (int ic = 0; ic < colNb; ic++) {
                    data[ic] += val;
                }

                return this;
            } else {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        @Override
        public int columns ()
        {
            return data.length;
        }

        @Override
        public INDArray diviRowVector (INDArray rowVector)
        {
            final int colNb = data.length;

            for (int ic = 0; ic < colNb; ic++) {
                double val = rowVector.getDouble(ic);

                data[ic] /= val;
            }

            return this;
        }

        @Override
        public double getDouble (int i)
        {
            return data[i];
        }

        @Override
        public INDArray getRow (int i)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public INDArray mean (int dimension)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int rows ()
        {
            return 1;
        }

        @Override
        public INDArray std (int dimension)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public INDArray subiRowVector (INDArray rowVector)
        {
            final int colNb = columns();

            for (int ic = 0; ic < colNb; ic++) {
                double val = rowVector.getDouble(ic);

                data[ic] -= val;
            }

            return this;
        }

        @Override
        public String toString ()
        {
            final StringBuilder sb = new StringBuilder("[");
            final int colNb = columns();

            for (int ic = 0; ic < colNb; ic++) {
                if (ic > 0) {
                    sb.append(", ");
                }

                sb.append(format(data[ic]));
            }

            sb.append("]");

            return sb.toString();
        }
    }
}
