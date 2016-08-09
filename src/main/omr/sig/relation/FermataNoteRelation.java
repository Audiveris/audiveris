//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              F e r m a t a N o t e R e l a t i o n                             //
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
package omr.sig.relation;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code FermataNoteRelation} represents the relation between a fermata and
 * a chord note member.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "fermata-note")
public class FermataNoteRelation
        extends AbstractSupport
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return constants.noteSupportCoeff.getValue();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio noteSupportCoeff = new Constant.Ratio(
                2,
                "Supporting coeff for (target) note");
    }
}
