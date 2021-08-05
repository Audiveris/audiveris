//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B e a m R e s t R e l a t i o n                                //
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
package org.audiveris.omr.sig.relation;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code BeamRestRelation} implements the geometric link between a beam
 * and an interleaved stem.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "beam-rest")
public class BeamRestRelation
        extends Support
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BeamRestRelation.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * Vertical distance (in pixels) between rest center and beam median line.
     * Absolute value.
     */
    @XmlAttribute
    private final int dy;
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code BeamRestRelation} object.
     *
     * @param dy absolute vertical distance beam median and rest center
     */
    public BeamRestRelation (int dy)
    {
        super(1.0);

        this.dy = dy;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getDistance //
    //-------------//
    /**
     * Report the absolute vertical distance between beam median and rest center.
     *
     * @return length of (nearly) vertical distance between beam and rest
     */
    public int getDistance ()
    {
        return dy;
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        // A rest can "belong" to at most one beam.
        return true;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        // A beam can have several interleaved rests.
        return false;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return new StringBuilder(super.internals()).append(" dy:").append(dy).toString();
    }
}
