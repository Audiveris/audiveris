//----------------------------------------------------------------------------//
//                                                                            //
//                       M u l t i p l e R u n s V i e w er                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import static omr.run.Orientation.HORIZONTAL;
import static omr.run.Orientation.VERTICAL;
import omr.run.Run;
import omr.run.RunsTable;

import omr.sheet.Sheet;

import omr.ui.BoardsPane;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Class {@code MultipleRunsViewer} is similar to RunsViewer but can
 * display multiple RunsTable instances one upon the other.
 *
 * @author Hervé Bitteur
 */
public class MultipleRunsViewer
{
    //~ Static fields/initializers ---------------------------------------------

    /** Sequence of colors to be used. */
    private static final Color[] colors = new Color[]{
        new Color(0, 0, 0),
        new Color(0, 255, 0, 100),
        new Color(255, 0, 0, 100)
    };

    //~ Instance fields --------------------------------------------------------
    /** The related sheet */
    private final Sheet sheet;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // RunsViewer //
    //------------//
    /**
     * Creates a new MultipleRunsViewer object.
     *
     * @param sheet the related sheet
     */
    public MultipleRunsViewer (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // display //
    //---------//
    /**
     * Display a view on provided runs table instances
     *
     * @param tables the tables to display
     */
    public void display (RunsTable... tables)
    {
        RubberPanel view = new MyView(tables);
        view.setName("MultiTables");
        view.setPreferredSize(tables[0].getDimension());

        BoardsPane boards = new BoardsPane(new PixelBoard(sheet));

        sheet.getAssembly()
                .addViewTab("MultiTables", new ScrollView(view), boards);
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------//
    // MyView //
    //--------//
    /**
     * A specific runs view, which displays retrieved lines and bars
     * on top of the runs.
     */
    private class MyView
            extends RubberPanel
    {
        //~ Instance fields ----------------------------------------------------

        private final RunsTable[] tables;

        //~ Constructors -------------------------------------------------------
        public MyView (RunsTable... tables)
        {
            this.tables = tables;

            // Location service
            setLocationService(sheet.getLocationService());

            // Set background color
            setBackground(Color.white);
        }

        //~ Methods ------------------------------------------------------------
        //--------//
        // render //
        //--------//
        /**
         * Render the table in the provided Graphics context, which may be
         * already scaled.
         *
         * @param g the graphics context
         */
        @Override
        public void render (Graphics2D g)
        {
            // Render all sections, using the colors they have been assigned
            int index = 0;

            for (RunsTable table : tables) {
                g.setColor(colors[index++ % colors.length]);
                renderRuns(table, g);
            }

            // Paint additional items, such as recognized items, etc...
            renderItems(g);
        }

        protected void renderItems (Graphics2D g)
        {
            sheet.renderItems(g);
        }

        //------------//
        // renderRuns //
        //------------//
        protected void renderRuns (RunsTable table,
                                   Graphics2D g)
        {
            if (table == null) {
                return;
            }
            
            Rectangle clip = g.getClipBounds();

            switch (table.getOrientation()) {
            case HORIZONTAL: {
                int minRow = Math.max(clip.y, 0);
                int maxRow = Math.min(
                        (clip.y + clip.height),
                        table.getHeight()) - 1;

                for (int row = minRow; row <= maxRow; row++) {
                    List<Run> seq = table.getSequence(row);

                    for (Run run : seq) {
                        g.fillRect(run.getStart(), row, run.getLength(), 1);
                    }
                }
            }

            break;

            case VERTICAL: {
                int minRow = Math.max(clip.x, 0);
                int maxRow = Math.min((clip.x + clip.width), table.getWidth())
                             - 1;

                for (int row = minRow; row <= maxRow; row++) {
                    List<Run> seq = table.getSequence(row);

                    for (Run run : seq) {
                        g.fillRect(row, run.getStart(), 1, run.getLength());
                    }
                }
            }

            break;
            }
        }
    }
}
