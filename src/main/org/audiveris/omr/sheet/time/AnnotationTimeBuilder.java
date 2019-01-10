//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            A n n o t a t i o n T i m e B u i l d e r                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sheet.time;

import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.util.VerticalSide;

/**
 * Class {@code AnnotationTimeBuilder} is a TimeBuilder based on annotations.
 *
 * @author Hervé Bitteur
 */
public class AnnotationTimeBuilder
        extends TimeBuilder
{

    /**
     * Creates a new {@code AnnotationTimeBuilder} object.
     *
     * @param staff  underlying staff
     * @param column containing time column
     */
    public AnnotationTimeBuilder (Staff staff,
                                  AnnotationTimeColumn column)
    {
        super(staff, column);
    }

    @Override
    public void cleanup ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void findCandidates ()
    {
        AnnotationTimeColumn annotationTimeColumn = (AnnotationTimeColumn) column;

        for (Inter inter : annotationTimeColumn.timeSet) {
            if (inter.getStaff() == staff) {
                if (inter instanceof TimeWholeInter) {
                    wholes.add(inter);
                } else if (inter instanceof TimeNumberInter) {
                    TimeNumberInter number = (TimeNumberInter) inter;
                    VerticalSide side = number.getSide();

                    if (side == VerticalSide.TOP) {
                        nums.add(inter);
                    } else {
                        dens.add(inter);
                    }
                }
            }
        }
    }
}
