/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.run;

import omr.log.Logger;

import omr.ui.view.RubberPanel;

import java.awt.*;
import java.util.List;

/**
 * Class {@code RunsTableView}
 *
 * @author Hervé Bitteur
 */
public class RunsTableView
    extends RubberPanel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(RunsTableView.class);

    //~ Instance fields --------------------------------------------------------

    /** The underlying table of runs */
    private final RunsTable table;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new RunsTableView object.
     *
     * @param table the underlying table of runs
     */
    public RunsTableView (RunsTable table)
    {
        this.table = table;

        // Set background color
        setBackground(Color.white);
        
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // render //
    //--------//
    /**
     * Render this lag in the provided Graphics context, which may be already
     * scaled
     * @param g the graphics context
     */
    @Override
    public void render (Graphics2D g)
    {
        ///final Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1f);

        // Render all sections, using the colors they have been assigned
        renderRuns(g);

        // Paint additional items, such as recognized items, etc...
        renderItems(g);

        // Restore stroke
        ///g.setStroke(oldStroke);
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Room for rendering additional items, if any
     *
     * @param g the graphic context
     */
    protected void renderItems (Graphics2D g)
    {
        // Void
    }

    //------------//
    // renderRuns //
    //------------//
    protected void renderRuns (Graphics2D g)
    {
        Rectangle clip = g.getClipBounds();

        switch (table.getOrientation()) {
        case HORIZONTAL : {
            int minRow = Math.max(clip.y, 0);
            int maxRow = Math.min((clip.y + clip.height), table.getHeight()) -
                         1;

            for (int row = minRow; row <= maxRow; row++) {
                List<Run> seq = table.getSequence(row);

                for (Run run : seq) {
                    g.setColor(runColor(run));
                    g.fillRect(run.getStart(), row, run.getLength(), 1);
                }
            }
        }

        break;

        case VERTICAL : {
            int minRow = Math.max(clip.x, 0);
            int maxRow = Math.min((clip.x + clip.width), table.getWidth()) - 1;

            for (int row = minRow; row <= maxRow; row++) {
                List<Run> seq = table.getSequence(row);

                for (Run run : seq) {
                    g.setColor(runColor(run));
                    g.fillRect(row, run.getStart(), 1, run.getLength());
                }
            }
        }

        break;
        }
    }

    //----------//
    // runColor //
    //----------//
    protected Color runColor (Run run)
    {
        int level = run.getLevel();

        return new Color(level, level, level);
    }
}
