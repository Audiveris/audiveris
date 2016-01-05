//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R u n s V i e w e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.run.RunBoard;
import omr.run.RunService;
import omr.run.RunTable;
import omr.run.RunTableView;

import omr.sheet.Sheet;

import omr.ui.BoardsPane;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;

import java.awt.Graphics2D;

/**
 * Class {@code RunsViewer} handles the display of instance(s) of
 * {@link RunTable} in the assembly of the related sheet.
 *
 * @author Hervé Bitteur
 */
public class RunsViewer
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The related sheet */
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new RunsViewer object.
     *
     * @param sheet the related sheet
     */
    public RunsViewer (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        sheet.getAssembly().addViewTab(name, new ScrollView(view), boards);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
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
        //~ Constructors ---------------------------------------------------------------------------

        public MyRunsTableView (String name,
                                RunTable table)
        {
            super(name, table, sheet.getLocationService());
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected void renderItems (Graphics2D g)
        {
            sheet.renderItems(g);
        }
    }
}
