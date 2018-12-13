//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R u n s V i e w e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.run.RunBoard;
import org.audiveris.omr.run.RunService;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableView;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.view.RubberPanel;
import org.audiveris.omr.ui.view.ScrollView;

import java.awt.Graphics2D;

/**
 * Class {@code RunsViewer} handles the display of instance(s) of
 * {@link RunTable} in the assembly of the related sheet.
 *
 * @author Hervé Bitteur
 */
public class RunsViewer
{

    /** The related sheet */
    private final Sheet sheet;

    /**
     * Creates a new RunsViewer object.
     *
     * @param sheet the related sheet
     */
    public RunsViewer (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //---------//
    // display //
    //---------//
    /**
     * Display a view on provided runs table
     *
     * @param name  name for the view
     * @param table the runs to display
     */
    public void display (String name,
                         RunTable table)
    {
        RubberPanel view = new MyRunsTableView(name, table);
        view.setPreferredSize(table.getDimension());

        if (table.getRunService() == null) {
            table.setRunService(new RunService(name, table));
        }

        BoardsPane boards = new BoardsPane(
                new PixelBoard(sheet),
                new BinarizationBoard(sheet),
                new RunBoard(table, true));

        // Here we create new tab with the name of the table
        sheet.getStub().getAssembly().addViewTab(name, new ScrollView(view), boards);
    }

    //-----------------//
    // MyRunsTableView //
    //-----------------//
    /**
     * A specific runs view, which displays retrieved lines and bars
     * on top of the runs.
     */
    private class MyRunsTableView
            extends RunTableView
    {

        MyRunsTableView (String name,
                         RunTable table)
        {
            super(name, table, sheet.getLocationService());
        }

        @Override
        protected void renderItems (Graphics2D g)
        {
            sheet.renderItems(g);
        }
    }
}
