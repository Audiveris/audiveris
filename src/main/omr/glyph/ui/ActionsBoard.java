//----------------------------------------------------------------------------//
//                                                                            //
//                          A c t i o n s B o a r d                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.GlyphInspector;
import omr.glyph.GlyphLag;
import static omr.selection.SelectionTag.*;

import omr.sheet.Sheet;

import omr.ui.Board;
import omr.ui.util.Panel;
import omr.ui.util.SwingWorker;

import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.event.*;

import javax.swing.*;

/**
 * Class <code>ActionsBoard</code> is just a collection of buttons whose related
 * action deal with glyph processing in SymbolsEditor.
 *
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ActionsBoard
    extends Board
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ActionsBoard.class);

    //~ Instance fields --------------------------------------------------------

    /** Glyph inspector */
    private final GlyphInspector inspector;

    /** Repository of known glyphs */
    private final GlyphRepository repository = GlyphRepository.getInstance();

    /** Sheet with Loaded glyphs */
    private final Sheet sheet;

    /** Related symbols editor */
    private final SymbolsEditor symbolsBuilder;

    /** Action for recording glyph descriptions */
    private RecordAction recordAction = new RecordAction();

    /** Action for refreshing the display */
    private RefreshAction refreshAction = new RefreshAction();

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ActionsBoard //
    //--------------//
    /**
     * Creates a new ActionsBoard object.
     *
     * @param sheet the related sheet
     * @param symbolsBuilder the symbol processing entity
     */
    public ActionsBoard (Sheet         sheet,
                         SymbolsEditor symbolsBuilder)
    {
        super(Board.Tag.CUSTOM, "Actions");

        this.sheet = sheet;
        this.symbolsBuilder = symbolsBuilder;

        inspector = sheet.getGlyphInspector();

        defineLayout();
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        final String buttonWidth = Panel.getButtonWidth();
        final String fieldInterval = Panel.getFieldInterval();
        final String fieldInterline = Panel.getFieldInterline();

        FormLayout   layout = new FormLayout(
            buttonWidth + "," + fieldInterval + "," + buttonWidth + "," +
            fieldInterval + "," + buttonWidth + "," + fieldInterval + "," +
            buttonWidth,
            "pref," + fieldInterline + "," + "pref," + fieldInterline + "," +
            "pref");

        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int             r = 1; // --------------------------------
        builder.addSeparator("Global", cst.xyw(1, r, 7));

        r += 2; // --------------------------------
                // Add a Refresh button

        JButton refreshButton = new JButton(refreshAction);
        refreshButton.setToolTipText("Refresh display");
        builder.add(refreshButton, cst.xy(1, r));

        // Add an Eval button
        EvalAction evalAction = new EvalAction();
        JButton    evalButton = new JButton(evalAction);
        evalButton.setToolTipText("Evaluate unknown glyphs");
        builder.add(evalButton, cst.xy(3, r));

        // Add a Save button
        SaveAction saveAction = new SaveAction();
        JButton    saveButton = new JButton(saveAction);
        saveButton.setToolTipText("Save current state");
        builder.add(saveButton, cst.xy(5, r));

        // Add a Record action
        JButton recordButton = new JButton(recordAction);
        recordButton.setToolTipText("Record glyphs for training");
        builder.add(recordButton, cst.xy(7, r));

        r += 2; // --------------------------------

        // Add a Verticals action
        VerticalsAction verticalsAction = new VerticalsAction();
        JButton         verticalsButton = new JButton(verticalsAction);
        verticalsButton.setToolTipText("Extract Verticals like Stems");
        builder.add(verticalsButton, cst.xy(1, r));

        // Add a Leaves action
        LeavesAction leavesAction = new LeavesAction();
        JButton      leavesButton = new JButton(leavesAction);
        leavesButton.setToolTipText("Extract stem Leaves");
        builder.add(leavesButton, cst.xy(3, r));

        // Add a Compounds action
        CompoundsAction compoundsAction = new CompoundsAction();
        JButton         compoundsButton = new JButton(compoundsAction);
        compoundsButton.setToolTipText("Gather remaining stuff as Compounds");
        builder.add(compoundsButton, cst.xy(5, r));

        // Add a Cleanup action
        CleanupAction cleanupAction = new CleanupAction();
        JButton       cleanupButton = new JButton(cleanupAction);
        cleanupButton.setToolTipText("Cleanup stems with no symbols attached");
        builder.add(cleanupButton, cst.xy(7, r));
    }

    // Temporary
    private void refresh ()
    {
        symbolsBuilder.refresh();
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------------//
    // CleanupAction //
    //---------------//
    private class CleanupAction
        extends AbstractAction
    {
        public CleanupAction ()
        {
            super("Cleanup");
        }

        public void actionPerformed (ActionEvent e)
        {
            inspector.processUndueStems();
            refresh();
        }
    }

    //-----------------//
    // CompoundsAction //
    //-----------------//
    private class CompoundsAction
        extends AbstractAction
    {
        public CompoundsAction ()
        {
            super("Compounds");
        }

        public void actionPerformed (ActionEvent e)
        {
            inspector.processCompounds(inspector.getLeafMaxGrade());
            refresh();
        }
    }

    //------------//
    // EvalAction //
    //------------//
    private class EvalAction
        extends AbstractAction
    {
        public EvalAction ()
        {
            super("Eval");
        }

        public void actionPerformed (ActionEvent e)
        {
            inspector.evaluateGlyphs(inspector.getLeafMaxGrade());
            refresh();
        }
    }

    //--------------//
    // LeavesAction //
    //--------------//
    private class LeavesAction
        extends AbstractAction
    {
        public LeavesAction ()
        {
            super("Leaves");
        }

        public void actionPerformed (ActionEvent e)
        {
            inspector.processLeaves();
            inspector.evaluateGlyphs(inspector.getLeafMaxGrade());
            refresh();
        }
    }

    //--------------//
    // RecordAction //
    //--------------//
    private class RecordAction
        extends AbstractAction
    {
        public RecordAction ()
        {
            super("Record");
        }

        public void actionPerformed (ActionEvent e)
        {
            final SwingWorker worker = new SwingWorker() {
                public Object construct ()
                {
                    repository.recordSheetGlyphs(
                        sheet, /* emptyStructures => */
                        sheet.isOnSymbols());

                    return null;
                }
            };

            worker.start();
        }
    }

    //---------------//
    // RefreshAction //
    //---------------//
    private class RefreshAction
        extends AbstractAction
    {
        public RefreshAction ()
        {
            super("Refresh");
        }

        public void actionPerformed (ActionEvent e)
        {
            refresh();
        }
    }

    //------------//
    // SaveAction //
    //------------//
    private class SaveAction
        extends AbstractAction
    {
        public SaveAction ()
        {
            super("Save");
        }

        public void actionPerformed (ActionEvent e)
        {
            final SwingWorker worker = new SwingWorker() {
                public Object construct ()
                {
                    try {
                        sheet.getScore()
                             .serialize();
                    } catch (Exception ex) {
                        logger.warning(
                            "Could not serialize " + sheet.getScore());
                    }

                    return null;
                }
            };

            worker.start();
        }
    }

    //-----------------//
    // VerticalsAction //
    //-----------------//
    private class VerticalsAction
        extends AbstractAction
    {
        public VerticalsAction ()
        {
            super("Verticals");
        }

        public void actionPerformed (ActionEvent e)
        {
            inspector.processVerticals();
            refresh();
        }
    }
}
