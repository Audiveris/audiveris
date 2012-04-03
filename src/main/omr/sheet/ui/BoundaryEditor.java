//----------------------------------------------------------------------------//
//                                                                            //
//                        B o u n d a r y E d i t o r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.log.Logger;

import omr.sheet.Sheet;
import omr.sheet.SystemBoundary;
import omr.sheet.SystemInfo;

import omr.util.BrokenLine;
import omr.util.VerticalSide;

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
 * Class {@code BoundaryEditor} handles the modification of systems boundaries.
 *
 * @author Hervé Bitteur
 */
public class BoundaryEditor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BoundaryEditor.class);

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** Concrete menu */
    private final JMenu menu;

    /** Set of actions to update menu according to current selections */
    private final Collection<DynAction> dynActions = new HashSet<DynAction>();

    /** Acceptable distance since last reference point (while dragging) */
    private final int maxDraggingDelta = BrokenLine.getDefaultDraggingDistance();

    /** Ongoing modification session? */
    private boolean sessionOngoing = false;

    /** Set of modified lines in an edition session */
    private Set<BrokenLine> modifiedLines = new HashSet<BrokenLine>();

    // Latest designated reference point, if any */
    private Point      lastPoint = null;

    // Latest designated line, meaningful only if lastPoint is not null */
    private BrokenLine lastLine = null;

    // Latest designated system, meaningful only if lastPoint is not null */
    private SystemInfo lastSystem;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // BoundaryEditor //
    //----------------//
    /**
     * Creates a new BoundaryEditor object.
     */
    public BoundaryEditor (Sheet sheet)
    {
        this.sheet = sheet;

        menu = new JMenu("Boundaries");
        menu.add(new JMenuItem(new StartAction()));
        menu.add(new JMenuItem(new StopAction()));

        updateMenu();
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // updateMenu //
    //------------//
    public final void updateMenu ()
    {
        for (DynAction action : dynActions) {
            action.update();
        }
    }

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
     * update of system content and reprocessing of steps.
     * @param pt Current location of user mouse
     */
    public void inspectBoundary (Point pt)
    {
        if (!sessionOngoing) {
            return;
        }

        // Are we close to the lastPoint?
        if (lastPoint != null) {
            Rectangle rect = new Rectangle(lastPoint);
            rect.grow(maxDraggingDelta, maxDraggingDelta);

            if (!rect.contains(pt)) {
                lastPoint = null;
            }
        }

        if (lastPoint == null) {
            // Are we close to any existing refPoint?
            lastSystem = sheet.getSystemsNear(pt)
                              .iterator()
                              .next();

            for (BrokenLine line : lastSystem.getBoundary()
                                             .getLimits()) {
                lastPoint = line.findPoint(pt);

                if (lastPoint != null) {
                    lastLine = line;

                    break;
                }
            }
        }

        if (lastPoint != null) {
            // Move the current ref point to user pt
            lastPoint.setLocation(pt);

            // If now we get colinear segments, let's merge them
            if (lastLine.isColinear(lastPoint)) {
                lastLine.removePoint(lastPoint);
                modifiedLines.add(lastLine);
                lastPoint = null;
            }

            updateSystemPair();
        } else {
            // Are we close to a segment, to define a new ref point?
            lastSystem = sheet.getSystemsNear(pt)
                              .iterator()
                              .next();

            for (BrokenLine line : lastSystem.getBoundary()
                                             .getLimits()) {
                Point segmentStart = line.findSegment(pt);

                if (segmentStart != null) {
                    // Add a new ref point
                    lastPoint = pt;
                    lastLine = line;
                    line.insertPointAfter(pt, segmentStart);
                    modifiedLines.add(lastLine);
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

    //------------------//
    // updateSystemPair //
    //------------------//
    /**
     * Update the lastSystem, as well as the system which shares the line with
     * lastSystem, if any.
     */
    private void updateSystemPair ()
    {
        List<SystemInfo> systems = sheet.getSystems();
        int              sysIdx = lastSystem.getId() - 1;
        SystemBoundary   boundary = lastSystem.getBoundary();

        // Update lastSystem
        boundary.update();

        SystemInfo sharingSystem = null;

        if (lastLine == boundary.getLimit(VerticalSide.BOTTOM)) {
            if (sysIdx < (systems.size() - 1)) {
                sharingSystem = systems.get(sysIdx + 1);
            }
        } else {
            if (sysIdx > 0) {
                sharingSystem = systems.get(sysIdx - 1);
            }
        }

        if (sharingSystem != null) {
            // Update sharing system
            sharingSystem.getBoundary()
                         .update();
        }

        sheet.getSymbolsEditor()
             .refresh();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // DynAction //
    //-----------//
    /**
     * Base implementation, to register the dynamic actions that need to be
     * updated according to the current context.
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
            putValue(NAME, "Start");
            putValue(SHORT_DESCRIPTION, "Start boundaries edition");
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            logger.info("Boundaries edition started...");
            lastPoint = null;
            lastLine = null;
            lastSystem = null;
            modifiedLines.clear();
            sessionOngoing = true;

            // Highlignt border lines
            sheet.getSymbolsEditor()
                 .refresh();
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
            putValue(NAME, "Complete");
            putValue(SHORT_DESCRIPTION, "Complete ongoing edition");
        }

        //~ Methods ------------------------------------------------------------

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
            sheet.getSymbolsEditor()
                 .refresh();
        }

        @Override
        public void update ()
        {
            setEnabled(sessionOngoing);
        }
    }
}
