//----------------------------------------------------------------------------//
//                                                                            //
//                             S t o p W a t c h                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code StopWatch}
 *
 * @author Hervé Bitteur
 */
public class StopWatch
{
    //~ Instance fields --------------------------------------------------------

    /** Name for this watch instance */
    private final String name;

    /** Sequence of all tasks so far */
    private final List<Task> tasks = new ArrayList<>(128);

    /** Current task (null if not running) */
    private Task task;

    /** Current sum of tasks times */
    private long total;

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // StopWatch //
    //-----------//
    /**
     * Creates a new StopWatch object.
     *
     * @param name name of the watch instance
     */
    public StopWatch (String name)
    {
        this.name = name;
    }

    //~ Methods ----------------------------------------------------------------
    //-------//
    // print //
    //-------//
    public void print ()
    {
        print(System.out);
    }

    //-------//
    // print //
    //-------//
    public void print (PrintStream out)
    {
        stop();

        final String format = "%5d %3d%% %s";
        final String dashes = "-----------------------------------";
        out.println(getClass().getSimpleName() + " \"" + name + "\"");
        out.println(dashes);
        out.println("   ms    % Task");
        out.println(dashes);

        for (Task t : tasks) {
            if (t != task) {
                out.println(
                        String.format(
                        format,
                        t.elapsed,
                        (total != 0) ? ((100 * t.elapsed) / total) : 100,
                        t.label));
            }
        }

        out.println(dashes);
        out.println(String.format(format, total, 100, "Total"));
    }

    //-------//
    // start //
    //-------//
    public void start (String label)
    {
        if (task != null) {
            stop();
        }

        tasks.add(task = new Task(label));
    }

    //------//
    // stop //
    //------//
    public void stop ()
    {
        if (task != null) {
            task.elapsed = System.currentTimeMillis() - task.start;
            total += task.elapsed;
            task = null;
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" \"")
                .append(name)
                .append("\"");

        sb.append(" tasks:")
                .append(tasks.size());

        sb.append(" total:")
                .append(total);

        if (task != null) {
            sb.append(" running");
        } else {
            sb.append(" stopped");
        }

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------
    //------//
    // Task //
    //------//
    private static class Task
    {
        //~ Instance fields ----------------------------------------------------

        /** Label for this task */
        private final String label;

        /** Start time */
        private final long start;

        /** Elapsed time */
        private long elapsed;

        //~ Constructors -------------------------------------------------------
        public Task (String label)
        {
            this.label = label;
            start = System.currentTimeMillis();
        }
    }
}
