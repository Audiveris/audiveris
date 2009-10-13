//----------------------------------------------------------------------------//
//                                                                            //
//                         S h e e t s M a n a g e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ui.PaintingParameters;
import omr.score.ui.ScoreOrientation;

import omr.script.ScriptActions;

import omr.sheet.ui.SheetsController;

import omr.util.Dumper;
import omr.util.Implement;
import omr.util.Memory;
import omr.util.NameSet;
import omr.util.Worker;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * Class <code>SheetsManager</code> handles the set of sheet instances in
 * memory as well as the related history.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SheetsManager
    implements PropertyChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetsManager.class);

    /** The single instance of this class */
    private static SheetsManager INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** Instances of sheet */
    private List<Sheet> instances = new ArrayList<Sheet>();

    /** Sheet file history */
    private NameSet history;

    /** The UI controller, if any */
    private SheetsController controller;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // SheetsManager //
    //---------------//
    /**
     * Creates a SheetsManager.
     */
    private SheetsManager ()
    {
        // Listen to system layout property
        PaintingParameters.getInstance()
                          .addPropertyChangeListener(
            PaintingParameters.VERTICAL_LAYOUT,
            this);
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // setController //
    //---------------//
    public void setController (SheetsController controller)
    {
        this.controller = controller;
    }

    //------------//
    // getHistory //
    //------------//
    /**
     * Get access to the list of previously handled sheets
     *
     * @return the history set of sheet files
     */
    public NameSet getHistory ()
    {
        if (history == null) {
            history = new NameSet("omr.sheet.Sheet.history", 10);
        }

        return history;
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class,
     *
     * @return the single instance
     */
    public static SheetsManager getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new SheetsManager();
        }

        return INSTANCE;
    }

    //-------//
    // close //
    //-------//
    /**
     * Close a sheet instance
     * @param sheet the sheet to close
     * @return true if we have done the closing
     */
    public boolean close (Sheet sheet)
    {
        if (logger.isFineEnabled()) {
            logger.fine("close " + sheet);
        }

        // Remove from user selection if any
        if (controller != null) {
            if (!controller.close(sheet)) {
                return false;
            }
        }

        // Remove from list of instances
        if (instances.contains(sheet)) {
            instances.remove(sheet);
        }

        // Suggestion to run the garbage collector
        Memory.gc();

        return true;
    }

    //---------------//
    // dumpAllSheets //
    //---------------//
    /**
     * Dump all sheet instances
     */
    public void dumpAllSheets ()
    {
        java.lang.System.out.println("\n");
        java.lang.System.out.println("* All Sheets *");

        for (Sheet sheet : instances) {
            java.lang.System.out.println(
                "-----------------------------------------------------------------------");
            java.lang.System.out.println(sheet.toString());
            Dumper.dump(sheet);
        }

        java.lang.System.out.println(
            "-----------------------------------------------------------------------");
        logger.info(instances.size() + " sheet(s) dumped");
    }

    //----------------//
    // insertInstance //
    //----------------//
    /**
     * Insert this new sheet in the set of sheet instances
     *
     * @param sheet the sheet to insert
     */
    public void insertInstance (Sheet sheet)
    {
        if (logger.isFineEnabled()) {
            logger.fine("insertInstance " + sheet);
        }

        // Remove duplicate if any
        for (Iterator<Sheet> it = instances.iterator(); it.hasNext();) {
            Sheet s = it.next();

            if (s.getPath()
                 .equals(sheet.getPath())) {
                if (logger.isFineEnabled()) {
                    logger.fine("Removing duplicate " + s);
                }

                it.remove();
                s.close();

                break;
            }
        }

        // Insert new sheet instances
        instances.add(sheet);

        // Insert in sheet history
        getHistory()
            .add(sheet.getPath());
    }

    //----------------//
    // propertyChange //
    //----------------//
    @Implement(PropertyChangeListener.class)
    public void propertyChange (PropertyChangeEvent evt)
    {
        new Worker<Void>() {
                @Override
                public Void construct ()
                {
                    for (Sheet sheet : instances) {
                        Score score = sheet.getScore();

                        if (score != null) {
                            score.setOrientation(
                                PaintingParameters.getInstance().getScoreOrientation());
                        }
                    }

                    return null;
                }
            }.start();
    }
}
