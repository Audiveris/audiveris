//----------------------------------------------------------------------------//
//                                                                            //
//                          E r r o r s E d i t o r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.ui;

import omr.glyph.Glyph;

import omr.score.ScorePoint;
import omr.score.System;
import omr.score.SystemNode;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionTag;

import omr.sheet.Sheet;

import omr.util.Logger;

import java.awt.Rectangle;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>ErrorsEditor</code> handles the set of error messages
 * recorded during the translation from sheet to score, allowing the user
 * to interactively browse the errors and go to the related locations in
 * the sheet and score views.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ErrorsEditor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ErrorsEditor.class);

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** The list of displayed errors */
    private final JList list;

    /** The scrolling area */
    private final JScrollPane scrollPane;

    /** Selection listener */
    private final ListSelectionListener listener = new MyListener();

    /** Set of error records */
    private final SortedSet<Record> recordSet = new TreeSet<Record>();

    /** Facade model for the JList */
    private final DefaultListModel model = new DefaultListModel();

    /** Selection bus for glyph */
    private final Selection glyphSelection;

    /** Selection bus for score node */
    private final Selection scoreSelection;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // ErrorEditor //
    //-------------//
    /**
     * Create an instance of ErrorEditor (one per sheet / score)
     *
     * @param sheet the related sheet
     */
    public ErrorsEditor (Sheet sheet)
    {
        this.sheet = sheet;
        list = new JList(model);
        scrollPane = new JScrollPane(list);
        list.addListSelectionListener(listener);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        glyphSelection = sheet.getSelectionManager()
                              .getSelection(SelectionTag.VERTICAL_GLYPH);

        scoreSelection = sheet.getSelectionManager()
                              .getSelection(SelectionTag.SCORE_RECTANGLE);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // addError //
    //----------//
    /**
     * Record an error within a SystemNode entity
     * @param node the node in the score hierarchy
     * @param text the message text
     */
    public void addError (SystemNode node,
                          String     text)
    {
        addError(node, null, text);
    }

    //----------//
    // addError //
    //----------//
    /**
     * Record an error within a SystemNode entity, and related to a specific
     * glyph
     * @param node the containing node in score hierarchy
     * @param glyph the related glyph
     * @param text the message text
     */
    public void addError (final SystemNode node,
                          final Glyph      glyph,
                          final String     text)
    {
        SwingUtilities.invokeLater(
            new Runnable() {
                    // This part is run on swing thread
                    public void run ()
                    {
                        if (recordSet.add(new Record(node, glyph, text))) {
                            // Update the model
                            model.removeAllElements();

                            for (Record record : recordSet) {
                                model.addElement(record);
                            }
                        }
                    }
                });
    }

    //-------//
    // clear //
    //-------//
    /**
     * Remove all errors from the editor
     */
    public void clear ()
    {
        SwingUtilities.invokeLater(
            new Runnable() {
                    // This part is run on swing thread
                    public void run ()
                    {
                        recordSet.clear();
                        model.removeAllElements();
                    }
                });
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the real component
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return scrollPane;
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // Record //
    //--------//
    /**
     * A structure to hold the various pieces of an error message
     */
    private static class Record
        implements Comparable<Record>
    {
        SystemNode node;
        Glyph      glyph;
        String     text;

        public Record (SystemNode node,
                       Glyph      glyph,
                       String     text)
        {
            this.node = node;
            this.glyph = glyph;
            this.text = text;
        }

        public int compareTo (Record other)
        {
            // Very basic indeed !!!
            return toString()
                       .compareTo(other.toString());
        }

        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(node.getContextString());
            sb.append("[");

            if (glyph != null) {
                sb.append("Glyph #" + glyph.getId());
            }

            sb.append("]")
              .append(text);

            return sb.toString();
        }
    }

    //------------//
    // MyListener //
    //------------//
    /**
     * A specific listener to handle user selection in the list of errors
     */
    private class MyListener
        implements ListSelectionListener
    {
        public void valueChanged (ListSelectionEvent e)
        {
            if ((e.getSource() == list) && !e.getValueIsAdjusting()) {
                Record record = (Record) list.getSelectedValue();

                if (record != null) {
                    if (logger.isFineEnabled()) {
                        logger.fine("value=" + record);
                    }

                    if (record.glyph != null) {
                        // Use glyph location if available
                        glyphSelection.setEntity(
                            record.glyph,
                            SelectionHint.GLYPH_INIT);
                    } else {
                        // Otherwise use system node location
                        System     system = record.node.getSystem();
                        ScorePoint scrPt = system.toScorePoint(
                            record.node.getCenter());
                        scoreSelection.setEntity(
                            new Rectangle(scrPt),
                            SelectionHint.LOCATION_INIT);
                    }
                }
            }
        }
    }
}
