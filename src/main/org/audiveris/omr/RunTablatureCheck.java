//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                R u n T a b l a t u r e C h e c k                               //
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
package org.audiveris.omr;

import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.step.RunClass;
import org.audiveris.omrdataset.api.TablatureAreas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import javax.xml.bind.JAXBException;

/**
 * Class <code>RunTablatureCheck</code> checks for presence of tablatures and export their
 * precise locations if any.
 *
 * @author Hervé Bitteur
 */
public class RunTablatureCheck
        extends RunClass
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RunTablatureCheck.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>RunTablatureCheck</code> object.
     *
     * @param book     book to process
     * @param sheetIds sheet IDS if any
     */
    public RunTablatureCheck (Book book,
                              SortedSet<Integer> sheetIds)
    {
        super(book, sheetIds);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void process ()
    {
        // Write tablature info in separate "tablatures" folder
        final Path outDir = book.getInputPath().getParent().resolveSibling("tablatures");

        try {
            for (SheetStub stub : book.getValidStubs()) {
                if ((sheetIds == null) || sheetIds.contains(stub.getNumber())) {
                    if (stub.isDone(OmrStep.GRID)) {
                        final List<Rectangle> areas = new ArrayList<>();
                        final Sheet sheet = stub.getSheet();

                        for (Staff staff : sheet.getStaffManager().getStaves()) {
                            if (staff.isTablature()) {
                                logger.info("{} tablature at staff#{}",
                                            sheet.getId(), staff.getId());

                                Area area = StaffManager.getCoreArea(staff, 0, 0);
                                Rectangle rect = area.getBounds();
                                areas.add(rect);
                            }
                        }

                        if (!areas.isEmpty()) {
                            TablatureAreas tabs = new TablatureAreas(areas);

                            // Export one xml file per sheet
                            String name = sheet.getId();
                            Path outPath = outDir.resolve(name + ".tablatures.xml");
                            tabs.marshall(outPath);
                            logger.info("Tablatures exported as {}", outPath);
                        }
                    }
                }
            }
        } catch (IOException |
                 JAXBException ex) {
            logger.warn("Error exporting tablature areas {}", ex.toString(), ex);
        }
    }
}
