//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 H e a d e r T i m e C o l u m n                                //
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
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.util.ChartPlotter;

/**
 * A subclass of {@link TimeColumn}, specifically meant for managing times in a system
 * header.
 *
 * @author Hervé Bitteur
 */
public class HeaderTimeColumn
        extends TimeColumn
{

    /**
     * Creates a new {@code HeaderTimeColumn} object.
     *
     * @param system containing system
     */
    public HeaderTimeColumn (SystemInfo system)
    {
        super(system);
    }

    //---------//
    // addPlot //
    //---------//
    /**
     * Contribute to the staff plotter with data related to time signature.
     * <p>
     * Since the user can ask for time plot at any time, we allocate an instance of
     * HeaderTimeBuilder on demand just to provide the plot information that pertains to the
     * desired staff. This saves us the need to keep rather useless instances in memory.
     *
     * @param plotter the staff plotter to populate
     * @param staff   the desired staff
     * @return the time chosen to be added to plot title
     */
    public String addPlot (ChartPlotter plotter,
                           Staff staff)
    {
        final int browseStart;

        if (staff.getKeyStop() != null) {
            browseStart = staff.getKeyStop();
        } else if (staff.getClefStop() != null) {
            browseStart = staff.getClefStop();
        } else {
            browseStart = staff.getHeaderStart();
        }

        HeaderTimeBuilder builder = new HeaderTimeBuilder(staff, this, browseStart);
        builder.addPlot(plotter);

        AbstractTimeInter timeInter = staff.getHeader().time;

        if (timeInter != null) {
            return "time:" + timeInter.getValue();
        } else {
            return null;
        }
    }

    //------------//
    // lookupTime //
    //------------//
    /**
     * Look up for a column of <b>existing</b> time-signatures near headers end.
     *
     * @return ending abscissa offset of time-sig column WRT measure start, or -1 if invalid
     */
    public int lookupTime ()
    {
        // Allocate one time-sig builder for each staff within system
        for (Staff staff : system.getStaves()) {
            if (!staff.isTablature()) {
                builders.put(staff, allocateBuilder(staff));
            }
        }

        // Process each staff on turn, to look up time-sig
        for (TimeBuilder builder : builders.values()) {
            // Look up header time
            AbstractTimeInter time = ((HeaderTimeBuilder) builder).lookupTime();

            if (time == null) {
                return -1;
            }
        }

        // Push abscissa end for each StaffHeader
        int maxTimeOffset = 0;

        for (Staff staff : system.getStaves()) {
            if (!staff.isTablature()) {
                int measureStart = staff.getHeaderStart();
                Integer timeStop = staff.getTimeStop();

                if (timeStop != null) {
                    maxTimeOffset = Math.max(maxTimeOffset, timeStop - measureStart);
                }
            }
        }

        return maxTimeOffset;
    }

    //--------------//
    // retrieveTime //
    //--------------//
    /**
     * {@inheritDoc}
     *
     * @return ending abscissa offset of time-sig column WRT measure start, or -1 if invalid
     */
    @Override
    public int retrieveTime ()
    {
        if (-1 != super.retrieveTime()) {
            // Push abscissa end for each StaffHeader
            int maxTimeOffset = 0;

            for (Staff staff : system.getStaves()) {
                if (!staff.isTablature()) {
                    int measureStart = staff.getHeaderStart();
                    Integer timeStop = staff.getTimeStop();

                    if (timeStop != null) {
                        maxTimeOffset = Math.max(maxTimeOffset, timeStop - measureStart);
                    }
                }
            }

            return maxTimeOffset;
        } else {
            return -1;
        }
    }

    @Override
    protected TimeBuilder allocateBuilder (Staff staff)
    {
        int browseStart = staff.getHeaderStop();

        return new HeaderTimeBuilder(staff, this, browseStart);
    }

    @Override
    protected void cleanup ()
    {
        for (TimeBuilder builder : builders.values()) {
            builder.cleanup();
        }
    }
}
