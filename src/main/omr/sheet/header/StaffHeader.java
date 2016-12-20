//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S t a f f H e a d e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.header;

import omr.sig.inter.AbstractTimeInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.KeyInter;
import omr.sig.inter.TimePairInter;
import omr.sig.inter.TimeWholeInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;

/**
 * Class {@code StaffHeader} gathers information about the (Clef + Key + Time) sequence
 * at the beginning of a staff.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class StaffHeader
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            StaffHeader.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /**
     * Abscissa for start of staff header.
     * This is typically the point right after the right-most bar line of the starting bar group,
     * or the beginning abscissa of staff lines when there is no left bar line.
     */
    @XmlAttribute(name = "start")
    public final int start;

    /** Abscissa for end of staff header. */
    @XmlAttribute(name = "stop")
    public int stop;

    /** Clef found. */
    @XmlElement(name = "clef")
    public ClefInter clef;

    /** Key-sig found, if any. */
    @XmlElement(name = "key")
    public KeyInter key;

    /** Time-sig found, if any. */
    @XmlElementRefs({
        @XmlElementRef(type = TimePairInter.class),
        @XmlElementRef(type = TimeWholeInter.class)
    })
    public AbstractTimeInter time;

    // Transient data
    //---------------
    //
    /** Abscissa range for clef. */
    public Range clefRange;

    /** Abscissa range for key. */
    public Range keyRange;

    /** Abscissa for start of each key alter (used for plot only). */
    public List<Integer> alterStarts;

    /** Abscissa range for time. */
    public Range timeRange;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StaffHeader} object.
     *
     * @param start start of header (measure start)
     */
    public StaffHeader (int start)
    {
        this.start = start;
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private StaffHeader ()
    {
        this.start = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Header{");

        sb.append("start=").append(start);
        sb.append(" stop=").append(stop);

        if (clefRange != null) {
            sb.append(" CLEF(").append(clefRange).append(")");
        }

        if (keyRange != null) {
            sb.append(" KEY(").append(keyRange);

            if (alterStarts != null) {
                sb.append(" alters=").append(alterStarts);
            }

            sb.append(")");
        }

        if (timeRange != null) {
            sb.append(" TIME(").append(timeRange).append(")");
        }

        sb.append(" clef:").append((clef != null) ? clef : "null");
        sb.append(" key:").append((key != null) ? key : "null");
        sb.append(" time:").append((time != null) ? time : "null");

        sb.append("}");

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Range //
    //-------//
    public static class Range
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Was the item successfully retrieved?. */
        public boolean valid;

        /** Abscissa for beginning of browsing. */
        public int browseStart;

        /** Abscissa for end of browsing. */
        public int browseStop;

        /** Precise beginning of item, if any. */
        private Integer start;

        /** Precise end of item, if any. */
        private Integer stop;

        //~ Methods --------------------------------------------------------------------------------
        public int getStart ()
        {
            return start;
        }

        public int getStop ()
        {
            if (stop != null) {
                return stop;
            }

            return browseStop;
        }

        public int getWidth ()
        {
            return getStop() - getStart() + 1;
        }

        public boolean hasStart ()
        {
            return start != null;
        }

        public void setStart (int start)
        {
            this.start = start;
        }

        public void setStop (int stop)
        {
            this.stop = stop;
        }

        /**
         * Change stop value <b>ONLY IF</b> new value is smaller than the existing one.
         *
         * @param stop new stop value
         */
        public void shrinkStop (int stop)
        {
            if ((this.stop == null) || (stop <= this.stop)) {
                this.stop = stop;
            } else {
                logger.debug("Range tentative to shrinkStop() from {} to {}", this.stop, stop);
            }
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();

            if (valid) {
                sb.append("SUCCESS");
            } else {
                sb.append("FAILURE");
            }

            if (browseStart != 0) {
                sb.append(" bStart=").append(browseStart);
            }

            if (browseStop != 0) {
                sb.append(" bStop=").append(browseStop);
            }

            if (start != null) {
                sb.append(" start=").append(start);
            }

            if (stop != null) {
                sb.append(" stop=").append(stop);
            }

            return sb.toString();
        }
    }
}
