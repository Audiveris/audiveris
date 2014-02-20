//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R u n s V i e w e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.run.RunBoard;
import omr.run.RunsTable;
import omr.run.RunsTableView;

import omr.sheet.Sheet;

import omr.ui.BoardsPane;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;

import java.awt.Graphics2D;

/**
 * Class {@code RunsViewer} handles the display of instance(s) of
 * {@link RunsTable} in the assembly of the related sheet.
 *
 * @author Hervé Bitteur
 */
public class RunsViewer
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The related sheet */
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------

    //------------//
    // RunsViewer //
    //------------//
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
     * @param table the runs to display
     */
    public void display (RunsTable table)
    {
        RubberPanel view = new MyRunsTableView(table);
        view.setName(table.getName());
        view.setPreferredSize(table.getDimension());

        BoardsPane boards = new BoardsPane(
            new PixelBoard(sheet),
            new BinarizationBoard(sheet),
            new RunBoard(table, true));

        sheet.getAssembly().addViewTab(table.getName(), new ScrollView(view), boards);
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
        extends RunsTableView
    {
        //~ Constructors ---------------------------------------------------------------------------

        public MyRunsTableView (RunsTable table)
        {
            super(table, sheet.getLocationService());
        }

        //~ Methods --------------------------------------------------------------------------------

        @Override
        protected void renderItems (Graphics2D g)
        {
            sheet.renderItems(g);
        }
    }
}
