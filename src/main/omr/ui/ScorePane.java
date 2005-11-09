//-----------------------------------------------------------------------//
//                                                                       //
//                           S c o r e P a n e                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.Main;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.score.PagePoint;
import omr.score.Score;
import omr.score.ScoreManager;
import omr.score.ScoreView;
import omr.sheet.Sheet;
import omr.util.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>ScorePane</code> encapsulates the horizontal display of all
 * the score systems in a horizontal and scrollable display. This is
 * organized as a JTabbedPane, with one tab for each score handled.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScorePane
        extends JTabbedPane
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(ScorePane.class);

    //~ Instance variables ------------------------------------------------

    // Toolbar needed to add buttons in it
    private final JToolBar toolBar;

    // Collection of score-dependent actions
    private final List<Object> scoreDependentActions
        = new ArrayList<Object>();

    // Menu for score actions
    private final JMenu menu = new JMenu("Score");

    // Should we synchronize the other (sheet) pane
    private boolean synchroWanted = true;

    //~ Constructors ------------------------------------------------------

    //-----------//
    // ScorePane //
    //-----------//

    /**
     * Create the score pane, in the enclosing jui frame, and adding
     * buttons to the provided tool bar.
     *
     * @param jui     the enclosing frame
     * @param toolBar the tool bar to use
     */
    public ScorePane (Jui jui,
                      JToolBar toolBar)
    {
        this.toolBar = toolBar;

        // History menu
        JMenuItem historyMenu = ScoreManager.getInstance().getHistory()
                .menu("History",
                      new HistoryListener());
        historyMenu.setToolTipText("List of previous score files");
        historyMenu.setIcon(IconManager.buttonIconOf("general/History"));
        menu.add(historyMenu);
        menu.addSeparator();

        // Various actions
        new SelectAction();
        //new CloseAction();
        new SaveAction();

        menu.addSeparator();
        new ViewAction();
        new DumpAction();
        new DumpAllAction();

        // Listener on all tab operations
        getModel().addChangeListener(new ChangeListener()
        {
            public void stateChanged (ChangeEvent e)
            {
                viewUpdated();
            }
        });

        // Initially disabled actions
        Jui.enableActions(scoreDependentActions, false);
    }

    //~ Methods -----------------------------------------------------------

    //-----------------//
    // getCurrentScore //
    //-----------------//

    /**
     * Report the currently selected score
     *
     * @return the current score, or null otherwise
     */
    public Score getCurrentScore ()
    {
        ScoreView view = (ScoreView) getSelectedComponent();

        if (view != null) {
            return view.getScore();
        } else {
            return null;
        }
    }

    //--------------//
    // setScoreView //
    //--------------//

    /**
     * Set the various display parameter of a view for the given score, if
     * such score is provided, or a blank tab otherwise.
     * <p/>
     * <p/>
     * This method is called from 'Main' when scores are pre-loaded. </p>
     *
     * @param score the desired score if any, null otherwise
     *
     * @return the tab index of the related view
     */
    public int setScoreView (Score score)
    {
        if (score != null) {
            return setScoreView(score, null);
        } else {
            return -1;
        }
    }

    //--------------//
    // setScoreView //
    //--------------//

    /**
     * Set the various parameters of a view on given score. This may start
     * by allocating a new ScoreView if needed. The ScoreView is then
     * inserted in the tabbed pane, if not done. Finally a focus point may
     * be defined. If no focus point is provided, then the current view
     * focus is used if some exists, otherwise the counterpart of the sheet
     * focus is used if any. If no focus info can be retrieved, then none
     * is used.
     *
     * @param score the score
     * @param pagPt a potential focus point requested, null otherwise.
     *
     * @return the index of the properly set view on score, ready to be
     * shown
     */
    public int setScoreView (Score score,
                             PagePoint pagPt)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("setScoreView score=" + score + " pagPt=" + pagPt);
        }

        if (score != null) {
            // Make sure we have a proper score view
            ScoreView view = score.getView();

            if (view == null) {
                // Build a brand new display on this score
                view = new ScoreView(score);
            } else {
                // So that scroll bars be OK
                view.computePositions();
            }

            // Make sure the view is part of the tabbed pane
            int index = indexOfComponent(view);

            if (index == -1) {
                // Insert in tabbed pane
                addTab(score.getName(), null, view,
                       score.getRadix());

                index = indexOfComponent(view);
            }

            // Focus info
            if (pagPt == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No focus specified on Score side");
                }

                // Does the sheet view has a defined focus ?
                Sheet sheet = score.getSheet();

                if (sheet != null) {
//                     SheetView sheetView = sheet.getView();

//                     if (sheetView != null) {
// //                         pagPt = sheetView.getFocus();
//                     }
                }
            }

            if (pagPt != null) {
                // Use this focus information
                view.setFocus(pagPt);
            }

            return index;
        } else {
            return -1;
        }
    }

    //-------//
    // close //
    //-------//

    /**
     * Remove the view from the tabbed pane
     *
     * @param view The view to be closed
     */
    public void close (Component view)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("close " + ((ScoreView) view).toString());
        }

        // Remove view from tabs
        remove(view);
    }

    //---------------//
    // showScoreView //
    //---------------//

    /**
     * Make the (preset) ScoreView actually visible.
     *
     * @param index the index to the ScoreView to be made current, with its
     *                potential focus point, or -1 to show a blank panel.
     * @param synchro specify whether other side is to be shown also
     */
    public void showScoreView (int index,
                               boolean synchro)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("showScoreView index=" + index + " synchro="
                         + synchro);
        }

        setSynchroWanted(synchro);
        setSelectedIndex(index);
        setSynchroWanted(true);
    }

    //---------------//
    // showScoreView //
    //---------------//

    /**
     * Make the (preset) ScoreView actually visible.
     *
     * @param view    the ScoreView to show, or null
     * @param synchro specify whether other side is to be shown also
     */
    public void showScoreView (ScoreView view,
                               boolean synchro)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("showScoreView view=" + view +
                         " synchro=" + synchro);
        }

        showScoreView(indexOfComponent(view), synchro);
    }

    //---------------//
    // showScoreView //
    //---------------//

    /**
     * Make the (preset) ScoreView actually visible.
     *
     * @param view the ScoreView to show, or null
     */
    public void showScoreView (ScoreView view)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("showScoreView view=" + view);
        }

        showScoreView(view, false);
    }

    //---------//
    // getMenu //
    //---------//

    /**
     * This method is meant for access by Jui (in the same package), to
     * report the menu dedicated to score handling.
     *
     * @return the score menu
     */
    JMenu getMenu ()
    {
        return menu;
    }

    //------------------//
    // setSynchroWanted //
    //------------------//

    /**
     * Allow to register the need (or lack of) for synchronization of the
     * other side (score view).
     *
     * @param synchroWanted the value to set to the flag
     */
    private synchronized void setSynchroWanted (boolean synchroWanted)
    {
        this.synchroWanted = synchroWanted;
        notify();
    }

    //-----------------//
    // isSynchroWanted //
    //-----------------//

    /**
     * Check is synchronization of the other side view (score) is
     * wanted. This is usually true, except in specific case, where the
     * initial order already comes from the other side, so there is no need
     * to go back there.
     *
     * @return the flag value
     */
    private boolean isSynchroWanted ()
    {
        return synchroWanted;
    }

    //-------------//
    // viewUpdated //
    //-------------//

    /**
     * Set the state (enabled or disabled) of all menu items that depend on
     * status of current score. UI frame title is updated accordingly.
     */
    private void viewUpdated ()
    {
        // The currently selected score, which may be null, if none is left
        Score score = getCurrentScore();

        if (logger.isDebugEnabled()) {
            logger.debug("viewUpdated for " + score);
        }

        // New targeted context
        Main.getJui().setTarget(score);

        // Enable actions ?
        Jui.enableActions(scoreDependentActions, score != null);

        // Synchronize the sheet side ?
        ScoreView view = (ScoreView) getSelectedComponent();

        if ((view != null) && isSynchroWanted()) {
            view.showRelatedSheet();
        }

        // Make UI frame title consistent
        Main.getJui().updateTitle();
    }

    //-----------//
    // loadScore //
    //-----------//
    private void loadScore (final File file)
    {
        final SwingWorker<Score> worker = new SwingWorker<Score>()
            {
                public Score construct()
                {
                    if (file.exists()) {
                        return ScoreManager.getInstance().load(file);
                    } else {
                        logger.warning("File not found " + file);
                        return null;
                    }
                }

                //Runs on the event-dispatching thread.
                public void finished()
                {
                    Score score = getValue();
                    if (score != null) {
//                         Sheet sheet = score.getSheet();
//                         if (sheet != null) {
//                             sheet.checkTransientSteps();
//                         }

                        showScoreView(setScoreView(score), true);
                    }
                }
            };
        worker.start();
    }

    //~ Classes -----------------------------------------------------------

    //-----------------//
    // HistoryListener //
    //-----------------//

    /**
     * Class <code>HistoryListener</code> is used to reload a score file,
     * when selected from the history of previous scores.
     */
    private class HistoryListener
        implements ActionListener
    {
        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            loadScore(new File(e.getActionCommand()));
        }
    }

    //-------------//
    // ScoreAction //
    //-------------//

    /**
     * Class <code>ScoreAction</code> is a template for any score-related
     * action : it builds the action, registers it in the list of
     * score-dependent actions if needed, inserts the action in the score
     * menu, and inserts a button in the toolbar if an icon is provided.
     */
    private abstract class ScoreAction
            extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public ScoreAction (boolean enabled,
                            String label,
                            String tip,
                            Icon icon)
        {
            super(label, icon);

            // Score-dependent action ?
            if (!enabled) {
                scoreDependentActions.add(this);
            }

            // Menu item
            menu.add(this).setToolTipText(tip);

            // Tool bar ?
            if (icon != null) {
                final JButton button = toolBar.add(this);
                button.setBorder(Jui.toolBorder);
                button.setToolTipText(tip);
            }
        }
    }

//     //-------------//
//     // CloseAction //
//     //-------------//

//     /**
//      * Class <code>CloseAction</code> handles the closing of the currently
//      * selected score.
//      */
//     private class CloseAction
//             extends ScoreAction
//     {
//         //~ Constructors --------------------------------------------------

//         public CloseAction ()
//         {
//             super(false, "Close Score", "Close the current score",
//                   IconManager.buttonIconOf("general/Remove"));
//         }

//         //~ Methods -------------------------------------------------------

//         public void actionPerformed (ActionEvent e)
//         {
//             getCurrentScore().close();
//         }
//     }

    //------------//
    // SaveAction //
    //------------//

    /**
     * Class <code>SaveAction</code> handles the saving of the currently
     * selected score, using an XML format.
     */
    private class SaveAction
            extends ScoreAction
    {
        //~ Constructors --------------------------------------------------

        public SaveAction ()
        {
            super(false, "Save Score", "Save the current score",
                  IconManager.buttonIconOf("general/Export"));
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            final SwingWorker worker = new SwingWorker()
                {
                    public Object construct()
                    {
                        getCurrentScore().serialize();
                        return null;
                    }
                };
            worker.start();
        }
    }

    //--------------//
    // SelectAction //
    //--------------//

    /**
     * Class <code>SelectAction</code> handles the interactive selection of
     * a score (XML) file to be loaded as a new score.
     */
    private class SelectAction
            extends ScoreAction
    {
        //~ Constructors --------------------------------------------------

        public SelectAction ()
        {
            super(true, "Open Score", "Open a score file",
                  IconManager.buttonIconOf("general/Import"));
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            final JFileChooser fc
                = new JFileChooser(constants.initXmlDir.getValue());

            // Let the user select an xml file
            fc.addChoosableFileFilter
                (new FileFilter("score files",
                                new String[]{
                                    ScoreManager.SCORE_FILE_EXTENSION
                                }));

            if (fc.showOpenDialog(Main.getJui()) == JFileChooser.APPROVE_OPTION) {
                loadScore(fc.getSelectedFile());
            }
        }
    }

    //------------//
    // ViewAction //
    //------------//

    /**
     * Class <code>ViewAction</code> launches the tree display of the
     * current score.
     */
    private class ViewAction
            extends ScoreAction
    {
        //~ Constructors --------------------------------------------------

        public ViewAction ()
        {
            super(false, "View Score", "View the score document",
                  IconManager.buttonIconOf("general/Information"));
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            getCurrentScore().viewScore();
        }
    }

    //------------//
    // DumpAction //
    //------------//
    private class DumpAction
            extends ScoreAction
    {
        //~ Constructors --------------------------------------------------

        public DumpAction ()
        {
            super(false, "Dump Score", "Dump the current score", null);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            getCurrentScore().dump();
        }
    }

    //---------------//
    // DumpAllAction //
    //---------------//
    private class DumpAllAction
            extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public DumpAllAction ()
        {
            super("Dump all scores", IconManager.buttonIconOf("general/Find"));

            final String tiptext = "Dump all score instances";

            menu.add(this).setToolTipText(tiptext);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            ScoreManager.getInstance().dumpAllScores();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        Constant.String initXmlDir = new Constant.String
                ("d:/",
                 "Default directory for selection of score files");

        Constants ()
        {
            initialize();
        }
    }
}
