//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            T a b l e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.jaxb.table;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Class {@code Table}
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "run-table")
public class Table
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Width of the table. */
    @XmlAttribute
    private final int width;

    /** Height of the table. */
    @XmlAttribute
    private final int height;

    /** Sequences of runs per row. */
    @XmlElement(name = "runs")
    public final RunSequence[] sequences;

    //~ Constructors -------------------------------------------------------------------------------
    //longpublic final short[][] sequences;
    /**
     * Creates a new {@code Table} object.
     *
     * @param width     DOCUMENT ME!
     * @param height    DOCUMENT ME!
     * @param sequences DOCUMENT ME!
     */
    public Table (int width,
                  int height,
                  short[][] sequences)
    {
        this.width = width;
        this.height = height;
        //long this.sequences = sequences;
        //OK
        //        this.sequences = new ArrayList<ShortVector>();
        //
        //        for (int i = 0; i < sequences.length; i++) {
        //            short[] runs = sequences[i];
        //            this.sequences.add(new RunSequence(runs));
        //        }

        //OK2
        this.sequences = new RunSequence[sequences.length];

        for (int i = 0; i < sequences.length; i++) {
            short[] runs = sequences[i];
            this.sequences[i] = (runs != null) ? new RunSequence(runs) : null;
        }
    }

    // No-arg constructor meant for JAXB
    private Table ()
    {
        this.width = 0;
        this.height = 0;
        this.sequences = null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // RunSequence //
    //-------------//
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "runs")
    public static class RunSequence
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlValue
        public short[] vector;

        //~ Constructors ---------------------------------------------------------------------------
        public RunSequence ()
        {
        }

        public RunSequence (short[] vector)
        {
            this.vector = vector;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            if (vector == null) {
                return "";
            }

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < vector.length; i++) {
                sb.append(' ').append(vector[i]);
            }

            return sb.toString();
        }
    }
}
