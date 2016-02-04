//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            T a b l e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.table;

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
