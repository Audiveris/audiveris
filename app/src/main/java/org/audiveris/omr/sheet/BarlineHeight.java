//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B a r l i n e H e i g h t                                   //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.util.param.ConstantBasedParam;
import org.audiveris.omr.util.param.Param;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class <code>BarlineHeight</code> defines the typical height of a barline, expressed as
 * a number of interlines.
 * <ul>
 * <li>A typical 5-line staff has barlines that go through the 5 lines, that is 4 interlines.
 * <li>A 1-line staff is different, because barlines can go (virtually):
 * <ul>
 * <li>from 2 interlines above to 2 interlines below, that is 4 interlines (as in a 5-line staff);
 * <li>from 1 interline above to 1 interline below, that is 2 interlines;
 * <li>when starting or ending a multi-staff system, the initial barline can be half-sized:
 * <ul>
 * <li>2 for the initial barline then 4 for the following barlines in staff;
 * <li>1 for the initial barline then 2 for the following barlines in staff.
 * </ul>
 * </ul>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public enum BarlineHeight
{
    four,
    twoThenFour,
    two,
    oneThenTwo;

    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    /** Default height. */
    public static final Param<BarlineHeight> defaultParam = new ConstantBasedParam<>(
            constants.defaultBarlineHeight,
            Param.GLOBAL_SCOPE);

    //~ Constructors -------------------------------------------------------------------------------

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // count //
    //-------//
    /**
     * Report the count of interlines for a standard (non-initial) barline.
     *
     * @return the general count of interlines
     */
    public int count ()
    {
        return switch (this) {
            case four, twoThenFour -> 4;
            case two, oneThenTwo -> 2;
        };
    }

    //--------------//
    // initialCount //
    //--------------//
    /**
     * Report the count of interlines for the initial barline.
     *
     * @return the initial count of interlines
     */
    public int initialCount ()
    {
        return switch (this) {
            case four -> 4;
            case twoThenFour -> 2;
            case two -> 2;
            case oneThenTwo -> 1;
        };
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Enum<BarlineHeight> defaultBarlineHeight = new Constant.Enum<>(
                BarlineHeight.class,
                BarlineHeight.four,
                "Default barline height (in interlines)");
    }

    //-------------//
    // JaxbAdapter //
    //-------------//
    public static class JaxbAdapter
            extends XmlAdapter<BarlineHeight, MyParam>
    {
        @Override
        public BarlineHeight marshal (MyParam bhp)
            throws Exception
        {
            if (bhp == null) {
                return null;
            }

            return bhp.getSpecific();
        }

        @Override
        public MyParam unmarshal (BarlineHeight value)
            throws Exception
        {
            if (value == null) {
                return null;
            }

            final MyParam bhp = new MyParam(null);
            bhp.setSpecific(value);

            return bhp;
        }
    }

    //---------//
    // MyParam //
    //---------//
    public static class MyParam
            extends Param<BarlineHeight>
    {
        public MyParam (Object scope)
        {
            super(scope);
        }
    }
}
