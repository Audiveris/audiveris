//----------------------------------------------------------------------------//
//                                                                            //
//                       S c o r e C o n t r o l l e r                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import static omr.score.ScoreFormat.*;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.ui.Jui;
import omr.ui.icon.IconManager;
import omr.ui.util.FileFilter;
import omr.ui.util.SwingWorker;
import static omr.ui.util.UIUtilities.*;

import omr.util.Logger;
import omr.util.NameSet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.*;

/**
 * Class <code>ScoreController</code> encapsulates a set of user interface means
 * related to score handling, typically menus and buttons, with their related
 * actions.
 *
 * <p>We have defined groups of actions: <ol>
 *
 * <li>Binary actions</li>
 * <li>XML actions</li>
 * <li>Display actions</li>
 * </ol>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreController
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants              constants = new Constants();
    private static final Logger                 logger = Logger.getLogger(
        ScoreController.class);

    //~ Instance fields --------------------------------------------------------

    // Map format -> history
    private final HashMap<ScoreFormat, History> historyMap = new HashMap<ScoreFormat, History>();

    // Menu for score actions
    private final JMenu        scoreMenu = new JMenu("Score");

    // Toolbar needed to add buttons in it
    private final JToolBar     toolBar;

    // Collection of score-dependent actions, that are enabled only if
    // there is a current score.
    private final List<Object> scoreDependentActions = new ArrayList<Object>();

    // Should we synchronize the sheet view
    private boolean synchroWanted = true;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // ScoreController //
    //-----------------//
    /**
     * Create the Controller artifacts.
     *
     * @param toolBar the (global) tool bar to use
     */
    public ScoreController (JToolBar toolBar)
    {
        this.toolBar = toolBar;

        // Binary actions
        new History(BINARY);
        new OpenAction(BINARY);
        new StoreAction(BINARY);
        scoreMenu.addSeparator();

        // XML Actions
        new History(XML);
        new OpenAction(XML);
        new StoreAction(XML);
        scoreMenu.addSeparator();

        // Display Actions
        new BrowseAction();
        new DumpAction();
        new DumpAllAction();
        scoreMenu.addSeparator();

        // Close
        new CloseAction();

        // Initially disabled actions
        enableActions(scoreDependentActions, false);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getCurrentScore //
    //-----------------//
    /**
     * Report the current selected Score (the score related to the currently
     * selected sheet)
     *
     * @return the current score, or null if none selected
     */
    public Score getCurrentScore ()
    {
        Sheet sheet = SheetManager.getSelectedSheet();

        if (sheet != null) {
            return sheet.getScore();
        }

        return null;
    }

    //---------//
    // getMenu //
    //---------//
    /**
     * This method is meant for access by Jui (in the same package), to report
     * the menu dedicated to score handling.
     *
     * @return the score menu
     */
    public JMenu getMenu ()
    {
        return scoreMenu;
    }

    //--------------//
    // setScoreView //
    //--------------//
    /**
     * Set the various display parameter of a view for the given score, if such
     * score is provided, or a blank tab otherwise.
     *
     * @param score the desired score if any, null otherwise
     */
    public void setScoreView (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setScoreView score=" + score);
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

            // Make sure the view is part of the related sheet assembly
            Jui jui = Main.getJui();

            if (jui != null) {
                Sheet sheet = score.getSheet();

                if (sheet != null) {
                    sheet.getAssembly()
                         .setScoreView(view);
                }
            }
        }

        enableActions(scoreDependentActions, score != null);
    }

    //------------------//
    // setSynchroWanted //
    //------------------//
    /**
     * Allow to register the need (or lack of) for synchronization of the other
     * side (score view).
     *
     * @param synchroWanted the value to set to the flag
     */
    private synchronized void setSynchroWanted (boolean synchroWanted)
    {
        this.synchroWanted = synchroWanted;
    }

    //-----------------//
    // isSynchroWanted //
    //-----------------//
    /**
     * Check is synchronization of the other side view (score) is wanted. This
     * is usually true, except in specific case, where the initial order already
     * comes from the other side, so there is no need to go back there.
     *
     * @return the flag value
     */
    private boolean isSynchroWanted ()
    {
        return synchroWanted;
    }

    //-----------//
    // loadScore //
    //-----------//
    private void loadScore (final File        file,
                            final ScoreFormat format)
    {
        final SwingWorker<Score> worker = new SwingWorker<Score>() {
            // This runs on worker's thread
            public Score construct ()
            {
                if (file.exists()) {
                    return ScoreManager.getInstance()
                                       .load(file);
                } else {
                    logger.warning("File not found " + file);

                    return null;
                }
            }

            // This runs on the event-dispatching thread.
            public void finished ()
            {
                Score score = getValue();

                try {
                    String  path = file.getCanonicalPath();
                    History history = historyMap.get(format);

                    // Insert in (or remove from) history
                    if (score != null) {
                        history.names.add(path);
                        setScoreView(score);

                        //  Sheet sheet = score.getSheet();
                        //  if (sheet != null) {
                        //      sheet.checkTransientSteps();
                        //  }

                        ////showScoreView(setScoreView(score), true);
                    } else {
                        history.names.remove(path);
                    }

                    enableActions(scoreDependentActions, score != null);
                } catch (IOException ex) {
                }
            }
        };

        worker.start();
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // FormatAction //
    //--------------//
    /**
     * Class <code>FormatAction</code> is a template for a ScoreAction that
     * depends on the format used for score files
     *
     */
    private abstract class FormatAction
        extends ScoreAction
    {
        protected final ScoreFormat format;

        public FormatAction (ScoreFormat format,
                             boolean     enabled,
                             String      label,
                             String      tip,
                             Icon        icon)
        {
            super(enabled, label, tip, icon);
            this.format = format;
        }
    }

    //--------------//
    // BrowseAction //
    //--------------//
    /**
     * Class <code>BrowseAction</code> launches the tree display of the current
     * score.
     */
    private class BrowseAction
        extends ScoreAction
    {
        public BrowseAction ()
        {
            super(
                false,
                "Browse Score",
                "Browse through the score document",
                IconManager.buttonIconOf("general/PrintPreview"));
        }

        public void actionPerformed (ActionEvent e)
        {
            getCurrentScore()
                .viewScore();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        Constant.Integer maxHistorySize = new Constant.Integer(
            10,
            "Maximum number of score files kept in history");

        Constants ()
        {
            initialize();
        }
    }

    //------------//
    // DumpAction //
    //------------//
    private class DumpAction
        extends ScoreAction
    {
        public DumpAction ()
        {
            super(false, "Dump Score", "Dump the current score", null);
        }

        public void actionPerformed (ActionEvent e)
        {
            getCurrentScore()
                .dump();
        }
    }

    //---------------//
    // DumpAllAction //
    //---------------//
    private class DumpAllAction
        extends ScoreAction
    {
        public DumpAllAction ()
        {
            super(true, "Dump all scores", "Dump all score instances", null);
        }

        public void actionPerformed (ActionEvent e)
        {
            ScoreManager.getInstance()
                        .dumpAllScores();
        }
    }

    //     //--------//
    //     // Format //
    //     //--------//
    //     private static enum Format
    //     {
    //         BINARY("Binary",                // Binary format
    //                ".score",
    //                constants.binaryScoreFolder),

    //             XML("Xml",                  // XML format
    //                 ".xml",
    //                 constants.xmlScoreFolder);

    //         public final String          name;
    //         public final String          extension;
    //         public final Constant.String folder;
    //         private      History         history;

    //         Format (String          name,
    //                 String          extension,
    //                 Constant.String folder)
    //         {
    //             this.name      = name;
    //             this.extension = extension;
    //             this.folder    = folder;
    //         }

    //         public void setHistory (History history)
    //         {
    //             this.history = history;
    //         }
    //     }

    //---------//
    // History //
    //---------//
    private class History
    {
        // Score file history
        private NameSet names;

        public History (final ScoreFormat format)
        {
            // Register this history with its score format
            historyMap.put(format, this);

            names = new NameSet(
                "omr.score.ScoreController." + format,
                constants.maxHistorySize.getValue());

            JMenuItem historyMenu = names.menu(
                format.name + " History",
                new ActionListener() {
                        public void actionPerformed (ActionEvent e)
                        {
                            File file = new File(e.getActionCommand());
                            loadScore(file, format);
                        }
                    });

            historyMenu.setToolTipText(
                "History of " + format.name + " score files");
            historyMenu.setIcon(IconManager.buttonIconOf("general/History"));
            scoreMenu.add(historyMenu);
        }
    }

    //-------------//
    // ScoreAction //
    //-------------//
    /**
     * Class <code>ScoreAction</code> is a template for any score-related action
     * : it builds the action, registers it in the list of score-dependent
     * actions if needed, inserts the action in the score menu, and inserts a
     * button in the toolbar if an icon is provided.
     */
    private abstract class ScoreAction
        extends AbstractAction
    {
        public ScoreAction (boolean enabled,
                            String  label,
                            String  tip,
                            Icon    icon)
        {
            super(label, icon);

            // Is this a Score-dependent action ? If so, it is by default
            // disabled, so we use this characteristic to detect such actions
            if (!enabled) {
                scoreDependentActions.add(this);
            }

            // Add the related Menu item
            scoreMenu.add(this)
                     .setToolTipText(tip);

            // Add an icon in the Tool bar, if any icon is provided
            if (icon != null) {
                final JButton button = toolBar.add(this);
                button.setBorder(getToolBorder());
                button.setToolTipText(tip);
            }
        }
    }

    //-------------//
    // CloseAction //
    //-------------//
    /**
     * Class <code>CloseAction</code> handles the closing of the currently
     * selected score.
     */
    private class CloseAction
        extends ScoreAction
    {
        public CloseAction ()
        {
            super(
                false,
                "Close Score",
                "Close the current score",
                IconManager.buttonIconOf("general/Remove"));
        }

        public void actionPerformed (ActionEvent e)
        {
            Score score = getCurrentScore();

            if (score != null) {
                score.close();
            } else {
                logger.warning("No current score to close");
            }
        }
    }

    //------------//
    // OpenAction //
    //------------//
    /**
     * Class <code>OpenAction</code> handles the interactive selection of a
     * score (binary or xml) file to be loaded as a new score.
     */
    private class OpenAction
        extends FormatAction
    {
        public OpenAction (ScoreFormat format)
        {
            super(
                format,
                true,
                "Open " + format.name + " Score",
                "Open a score " + format.name + " file",
                IconManager.buttonIconOf("general/Import"));
        }

        public void actionPerformed (ActionEvent e)
        {
            // Let the user select a file
            JFileChooser fc = new JFileChooser(format.folder.getValue());
            fc.setAcceptAllFileFilterUsed(false);
            fc.setMultiSelectionEnabled(false);
            fc.addChoosableFileFilter(
                new FileFilter(
                    "Score " + format.name + " Files (" + format.extension +
                    ")",
                    format.extension));

            // Check user action
            int rc = fc.showOpenDialog(Main.getJui().getFrame());

            if (rc == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                loadScore(file, format);
                // Remember folder for next time
                format.folder.setValue(file.getParent());
            }
        }
    }

    //-------------//
    // StoreAction //
    //-------------//
    /**
     * Class <code>StoreAction</code> handles the saving of the currently
     * selected score, using a binary or xml format.
     */
    private class StoreAction
        extends FormatAction
    {
        public StoreAction (ScoreFormat format)
        {
            super(
                format,
                false,
                "Store in " + format.name,
                "Store current score in " + format.name + " format",
                IconManager.buttonIconOf("general/Export"));
        }

        public void actionPerformed (ActionEvent e)
        {
            final SwingWorker worker = new SwingWorker() {
                public Object construct ()
                {
                    try {
                        switch (format) {
                        case BINARY :
                            getCurrentScore()
                                .serialize();

                            break;

                        case XML :
                            getCurrentScore()
                                .store();

                            break;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        logger.warning("Could not store score");
                        logger.warning(ex.toString());
                    }

                    return null;
                }
            };

            worker.start();
        }
    }
}
