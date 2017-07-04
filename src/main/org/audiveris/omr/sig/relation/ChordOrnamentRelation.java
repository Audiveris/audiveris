//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            C h o r d O r n a m e n t R e l a t i o n                           //
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
package org.audiveris.omr.sig.relation;

import javax.xml.bind.annotation.XmlRootElement;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;

/**
 * Class {@code ChordOrnamentRelation} represents the relation between a head-chord and
 * an ornament.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "chord-ornament")
public class ChordOrnamentRelation
        extends AbstractSupport
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ChordOrnamentRelation} object.
     *
     * @param grade relation quality
     */
    public ChordOrnamentRelation (double grade)
    {
        super(grade);
    }

    /**
     * Creates a new {@code ChordOrnamentRelation} object.
     */
    public ChordOrnamentRelation ()
    {
        super();
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    protected double getTargetCoeff ()
    {
        return constants.ornamentTargetCoeff.getValue();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio ornamentTargetCoeff = new Constant.Ratio(
                0.5,
                "Supporting coeff for (target) ornament");
    }

}
