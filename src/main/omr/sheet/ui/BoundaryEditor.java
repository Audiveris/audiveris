//----------------------------------------------------------------------------//
//                                                                            //
//                        B o u n d a r y E d i t o r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.sheet.Sheet;
import omr.sheet.SystemBoundary;
import omr.sheet.SystemInfo;

import omr.step.Step;
import omr.step.Steps;

import omr.util.BrokenLine;
import omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code BoundaryEditor} handles the manual modification of
 * systems boundaries.
 *
 * @author Hervé Bitteur
 */
public class BoundaryEditor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(BoundaryEditor.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Related sheet. */
    private final Sheet sheet;

    /** Concrete menu. */
    private final JMenu menu;

    /** Set of actions to update menu according to current selections. */
    private final Collection<DynAction> dynActions = new HashSet<>();

    /** Acceptable distance since last reference point. (while dragging) */
    private final int maxDraggingDelta = BrokenLine.getDraggingDistance();

    /** Ongoing modification session?. */
    private boolean sessionOngoing = false;

    /** Set of modified lines in an edition session. */
    private Set<BrokenLine> modifiedLines = new HashSet<>();

    /** Designated reference point, if any. */
    private Point currentPoint = null;

    /** Line containing the currentPoint, if any. */
    private BrokenLine currentLine = null;

    /**
     * One of the two systems that contain the current line, if any.
     * We don't need to know which one it is precisely.
     */
    private SystemInfo currentSystem = null;

    //~ Constructors -----------------------------------------------------------
    //
    //----------------//
    // BoundaryEditor //
    //----------------//
    /**
     * Creates a new BoundaryEditor object.
     */
    public BoundaryEditor (Sheet sheet)
    {
        this.sheet = sheet;

        menu = new JMenu("Boundaries ...");
        menu.add(new JMenuItem(new StartAction()));
        menu.add(new JMenuItem(new StopAction()));

        updateMenu();
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // getMenu //
    //---------//
    public JMenu getMenu ()
    {
        return menu;
    }

    //-----------------//
    // inspectBoundary //
    //-----------------//
    /**
     * Try to update a system limit.
     * Note that systems limit lines are modified online but subsequent
     * updates on system content (glyph, etc) will not be performed before
     * user explicitly asks for completion, which triggers an asynchronous
     * update of system/staff content and reprocessing of steps.
     *
     * @param pt Current location of user mouse
     */
    public void inspectBoundary (Point pt)
    {
        if (!sessionOngoing) {
            return;
        }

        if (currentPoint != null) {
            // Are we close enough to the current reference point?
            Rectangle rect = new Rectangle(currentPoint);
            rect.grow(maxDraggingDelta, maxDraggingDelta);

            if (!rect.contains(pt)) {
                currentPoint = null;
            }
        }

        if (currentPoint == null) {
            // Are we close enough to any existing reference point?
            currentSystem = sheet.getSystemsNear(pt).iterator().next();

            for (BrokenLine line : currentSystem.getBoundary().getLimits()) {
                currentPoint = line.findPoint(pt);

                if (currentPoint != null) {
                    currentLine = line;

                    break;
                }
            }
        }

        if (currentPoint != null) {
            // Move the current reference point to user pt
            currentPoint.setLocation(pt);
            modifiedLines.add(currentLine);

            // If now we get colinear segments, let's merge them
            if (currentLine.isColinear(currentPoint)) {
                currentLine.removePoint(currentPoint);
                currentPoint = null;
            }

            updateSystemPair();
        } else {
            // Are we close to a segment, to define a new ref point?
            currentSystem = sheet.getSystemsNear(pt).iterator().next();

            for (BrokenLine line : currentSystem.getBoundary().getLimits()) {
                Point segmentStart = line.findSegment(pt);

                if (segmentStart != null) {
                    // Add a new ref point
                    currentPoint = pt;
                    currentLine = line;
                    line.insertPointAfter(pt, segmentStart);
                    modifiedLines.add(currentLine);
                    updateSystemPair();

                    break;
                }
            }
        }
    }

    //------------------//
    // isSessionOngoing //
    //------------------//
    public boolean isSessionOngoing ()
    {
        return sessionOngoing;
    }

    //------------//
    // updateMenu //
    //------------//
    public final void updateMenu ()
    {
        // Enable the menu only once SYSTEMS has been performed
        Step split = Steps.valueOf(Steps.SYSTEMS);
        menu.setEnabled(split.isDone(sheet));

        // Enable actions according to current session status
        for (DynAction action : dynActions) {
            action.update();
        }
    }

    //------------------//
    // updateSystemPair //
    //------------------//
    /**
     * Update the current system, as well as the other system (if any)
     * which shares the currentLine with the provided system.
     */
    private void updateSystemPair ()
    {
        // Update lastSystem
        currentSystem.updateBoundary();

        // Find other system, if any, sharing this currentLine
        List<SystemInfo> systems = sheet.getSystems();
        int sysIdx = currentSystem.getId() - 1;
        SystemBoundary boundary = currentSystem.getBoundary();

        SystemInfo sharingSystem = null;

        if (currentLine == boundary.getLimit(VerticalSide.BOTTOM)) {
            // Sharing system is the following system, if any
            if (sysIdx < (systems.size() - 1)) {
                sharingSystem = systems.get(sysIdx + 1);
            }
        } else {
            // Sharing system is the preceding system, if any
            if (sysIdx > 0) {
                sharingSystem = systems.get(sysIdx - 1);
            }
        }

        if (sharingSystem != null) {
            // Update sharing system
            sharingSystem.updateBoundary();
        }

        // Update user display
        sheet.getSymbolsEditor().refresh();
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // DynAction //
    //-----------//
    /**
     * Base implementation, to register the dynamic actions that need
     * to be updated according to the current context.
     */
    public abstract class DynAction
            extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public DynAction ()
        {
            // Record the instance
            dynActions.add(this);
        }

        //~ Methods ------------------------------------------------------------
        public abstract void update ();
    }

    //-------------//
    // StartAction //
    //-------------//
    private class StartAction
            extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public StartAction ()
        {
            putValue(NAME, "Start edition");
            putValue(SHORT_DESCRIPTION, "Start boundaries edition");
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            logger.info("Boundaries edition started...");
            currentPoint = null;
            currentLine = null;
            modifiedLines.clear();
            sessionOngoing = true;

            // Highlight border lines
            sheet.getSymbolsEditor().refresh();
        }

        @Override
        public void update ()
        {
            setEnabled(!sessionOngoing);
        }
    }

    //------------//
    // StopAction //
    //------------//
    private class StopAction
            extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public StopAction ()
        {
            putValue(NAME, "Complete edition");
            putValue(SHORT_DESCRIPTION, "Complete ongoing edition");
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            if (!modifiedLines.isEmpty()) {
                // At least, one limit line has been modified
                logger.info("Completing boundaries edition...");
                sheet.getSymbolsController()
                        .asyncModifyBoundaries(modifiedLines);
            } else {
                logger.info("No boundary modified");
            }

            sessionOngoing = false;

            // De-highlight border lines
            sheet.getSymbolsEditor().refresh();
        }

        @Override
        public void update ()
        {
            setEnabled(sessionOngoing);
        }
    }
}
