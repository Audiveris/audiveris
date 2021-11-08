//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T i m e V a l u e T a s k                                   //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.sig.inter.TimeCustomInter;

/**
 * Class <code>TimeValueTask</code> handles the time value update of a time signature.
 *
 * @author Hervé Bitteur
 */
public class TimeValueTask
        extends InterTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Old time value. */
    private final TimeRational oldTime;

    /** New time value. */
    private final TimeRational newTime;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>TimeValueTask</code> object.
     *
     * @param custom  custom time signature to modify
     * @param newTime new time value
     */
    public TimeValueTask (TimeCustomInter custom,
                          TimeRational newTime)
    {
        super(custom.getSig(), custom, custom.getBounds(), null, "time");
        this.newTime = newTime;

        oldTime = custom.getTimeRational();
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public TimeCustomInter getInter ()
    {
        return (TimeCustomInter) inter;
    }

    @Override
    public void performDo ()
    {
        getInter().setNumerator(newTime.num);
        getInter().setDenominator(newTime.den);

        sheet.getInterIndex().publish(getInter());
    }

    @Override
    public void performUndo ()
    {
        getInter().setNumerator(oldTime.num);
        getInter().setDenominator(oldTime.den);

        sheet.getInterIndex().publish(getInter());
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(actionName);
        sb.append(" ").append(inter);
        sb.append(" from ").append(oldTime);
        sb.append(" to ").append(newTime);

        return sb.toString();
    }
}
