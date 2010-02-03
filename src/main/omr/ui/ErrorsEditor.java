//----------------------------------------------------------------------------//
//                                                                            //
//                          E r r o r s E d i t o r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.ScoreLocation;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.entity.Measure;
import omr.score.entity.MeasureNode;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemNode;

import omr.selection.GlyphEvent;
import omr.selection.ScoreLocationEvent;
import omr.selection.SelectionHint;

import omr.sheet.Sheet;

import omr.step.Step;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>ErrorsEditor</code> handles the set of error messages
 * recorded during the translation from sheet to score, allowing the user
 * to interactively browse the errors and go to the related locations in
 * the sheet and score views.
 *
 * @author Hervé Bitteur
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
    }

    //~ Methods ----------------------------------------------------------------

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
        final Step step = getCurrentStep(node);
        SwingUtilities.invokeLater(
            new Runnable() {
                    // This part is run on swing thread
                    public void run ()
                    {
                        if (recordSet.add(new Record(step, node, glyph, text))) {
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
     * Remove all errors from the editor (Not used?)
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

    //-------------//
    // clearSystem //
    //-------------//
    /**
     * Clear all messages related to the provided system id (we use system id
     * rather than system, since a system may be reallocated by SystemsBuilder)
     * @param step the step we are interested in
     * @param systemId the id of system to clear
     */
    public void clearSystem (final Step step,
                             final int  systemId)
    {
        SwingUtilities.invokeLater(
            new Runnable() {
                    // This part is run on swing thread
                    public void run ()
                    {
                        for (Iterator<Record> it = recordSet.iterator();
                             it.hasNext();) {
                            Record record = it.next();

                            if ((record.step == step) &&
                                (record.node.getSystem()
                                            .getId() == systemId)) {
                                it.remove();
                            }
                        }

                        // Update the model
                        model.removeAllElements();

                        for (Record record : recordSet) {
                            model.addElement(record);
                        }
                    }
                });
    }

    //----------------//
    // getCurrentStep //
    //----------------//
    /**
     * Retrieve the step being performed on the system the provided node belongs
     * to
     * @param node the SystemNode the error relates to
     * @return the step being done
     */
    private Step getCurrentStep (SystemNode node)
    {
        return node.getSystem()
                   .getInfo()
                   .getSheet()
                   .getSheetSteps()
                   .getCurrentStep();
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
        //~ Instance fields ----------------------------------------------------

        final Step       step;
        final SystemNode node;
        final Glyph      glyph;
        final String     text;

        //~ Constructors -------------------------------------------------------

        public Record (Step       step,
                       SystemNode node,
                       Glyph      glyph,
                       String     text)
        {
            this.step = step;
            this.node = node;
            this.glyph = glyph;
            this.text = text;
        }

        //~ Methods ------------------------------------------------------------

        public int compareTo (Record other)
        {
            // Very basic indeed !!!
            return toString()
                       .compareTo(other.toString());
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(node.getContextString());

            sb.append(" [");

            if (glyph != null) {
                sb.append("Glyph #" + glyph.getId());
            }

            sb.append("]");

            sb.append(" ")
              .append(step);

            sb.append(" ")
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
        //~ Methods ------------------------------------------------------------

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
                        sheet.getVerticalLag()
                             .getSelectionService()
                             .publish(
                            new GlyphEvent(
                                this,
                                SelectionHint.GLYPH_INIT,
                                null,
                                record.glyph));
                    } else {
                        // Otherwise use system node location as possible
                        try {
                            ScoreSystem system = record.node.getSystem();
                            SystemPoint sysPt = null;

                            try {
                                sysPt = record.node.getCenter();
                            } catch (Exception ex) {
                            }

                            if (sysPt == null) {
                                if (record.node instanceof MeasureNode) {
                                    MeasureNode mn = (MeasureNode) record.node;
                                    Measure     measure = mn.getMeasure();

                                    if (measure != null) {
                                        sysPt = measure.getCenter();
                                    }
                                }
                            }

                            sheet.getSelectionService()
                                 .publish(
                                new ScoreLocationEvent(
                                    ErrorsEditor.this,
                                    SelectionHint.LOCATION_INIT,
                                    null,
                                    new ScoreLocation(
                                        system.getId(),
                                        new SystemRectangle(sysPt))));
                        } catch (Exception ex) {
                            logger.warning(
                                "Failed pointing to " + record.node,
                                ex);
                        }
                    }
                }
            }
        }
    }
}
