//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  N u m b e r V a l u e T a s k                                 //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.sig.inter.AbstractNumberInter;

/**
 * Class <code>NumberValueTask</code> handles the value update of a number.
 *
 * @author Hervé Bitteur
 */
public class NumberValueTask
        extends InterTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Old count value. */
    private final Integer oldValue;

    /** New count value. */
    private final Integer newValue;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>NumberValueTask</code> object.
     *
     * @param custom   custom number to modify
     * @param newValue new count value
     */
    public NumberValueTask (AbstractNumberInter custom,
                            Integer newValue)
    {
        super(custom.getSig(), custom, custom.getBounds(), null, "number");
        this.newValue = newValue;

        oldValue = custom.getValue();
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public AbstractNumberInter getInter ()
    {
        return (AbstractNumberInter) inter;
    }

    @Override
    public void performDo ()
    {
        getInter().setValue(newValue);
        sheet.getInterIndex().publish(getInter());
    }

    @Override
    public void performUndo ()
    {
        getInter().setValue(oldValue);
        sheet.getInterIndex().publish(getInter());
    }

    @Override
    public String toString ()
    {
        return new StringBuilder(actionName).append(" ").append(inter).append(" from ").append(
                oldValue).append(" to ").append(newValue).toString();
    }
}
