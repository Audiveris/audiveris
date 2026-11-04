//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      H e a d e r s S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.sheet.header;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.clef.ClefBuilder;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.ClefInter.ClefKind;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class <code>HeadersStep</code> implements <b>HEADERS</b> step, which handles the beginning
 * of every staff in a system.
 *
 * @author Hervé Bitteur
 */
public class HeadersStep
        extends AbstractSystemStep<Void>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HeadersStep.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>HeadersStep</code> object.
     */
    public HeadersStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // doEpilog //
    //----------//
    /**
     * Give a header clef to a staff that got none because its clef read too weakly, when the
     * same staff shows a clear clef of that kind on other systems of the sheet.
     * <p>
     * A staff is matched by its rank among the staves of systems with as many staves.
     */
    @Override
    protected void doEpilog (Sheet sheet,
                             Void context)
        throws StepException
    {
        // Kinds of the clefs read, per count of staves in system, then per staff rank
        final Map<Integer, Map<Integer, Set<ClefKind>>> kinds = new HashMap<>();

        for (SystemInfo system : sheet.getSystems()) {
            final List<Staff> staves = system.getStaves();

            for (int i = 0; i < staves.size(); i++) {
                final StaffHeader header = staves.get(i).getHeader();

                if ((header != null) && (header.clef != null)) {
                    kinds.computeIfAbsent(staves.size(), k -> new HashMap<>()).computeIfAbsent(
                            i,
                            k -> EnumSet.noneOf(ClefKind.class)).add(header.clef.getKind());
                }
            }
        }

        for (SystemInfo system : sheet.getSystems()) {
            final List<Staff> staves = system.getStaves();

            for (int i = 0; i < staves.size(); i++) {
                final Staff staff = staves.get(i);
                final StaffHeader header = staff.getHeader();

                if (staff.isTablature() || staff.isOneLineStaff() || (header == null)
                        || (header.clef != null)) {
                    continue;
                }

                final Set<ClefKind> known = kinds.getOrDefault(staves.size(), Map.of()).get(i);

                if (known != null) {
                    final ClefBuilder builder = new ClefBuilder(staff);
                    builder.setBrowseStart(staff.getHeaderStart());

                    final ClefInter clef = builder.findWeakClef(known);

                    if (clef != null) {
                        clef.freeze();
                        logger.info(
                                "Staff#{} weak header clef {} kept, as on other systems",
                                staff.getId(),
                                clef);
                    }
                }
            }
        }
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
        throws StepException
    {
        new HeaderBuilder(system).processHeader(); // -> Staff clef + key + time
    }
}
