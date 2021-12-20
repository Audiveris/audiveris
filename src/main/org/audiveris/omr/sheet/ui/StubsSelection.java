//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t u b s S e l e c t i o n                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.OMR;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.ui.field.LTextField;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.util.NaturalSpec;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import static java.text.MessageFormat.format;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * Class <code>StubsSelection</code> handles a dialog where user can specify stubs
 * selection.
 *
 * @author Hervé Bitteur
 */
public class StubsSelection
        implements ActionListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StubsSelection.class);

    /** Resource injection. */
    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(StubsSelection.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related book. */
    private final Book book;

    /** The editable specification field. */
    private final LTextField specField;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>StubsSelection</code> object for the provided book.
     *
     * @param book related book
     */
    public StubsSelection (Book book)
    {
        this.book = book;
        specField = new LTextField(
                true,
                format(resources.getString("specField.text.pattern"), book.size()),
                format(resources.getString("specField.shortDescription.pattern"), "1,4-6"));
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // getSheetsSpec //
    //---------------//
    /**
     * Open the dialog and report user selection of stub IDs.
     *
     * @return whether the selection specification was actually modified
     */
    public boolean getSheetsSpec ()
    {

        // Simple case, where user will not be prompted
        final List<SheetStub> validStubs = book.getValidStubs();

        if (validStubs.isEmpty()) {
            logger.info("No valid sheets in {}", book);
            return false;
        }

        // Prepare prompt dialog
        final String frameTitle = format(resources.getString("frameTitle.pattern"), book.getRadix());
        final JOptionPane pane = defineLayout();

        // Initial sheets selection, as read from book
        final String lastSpec = book.getSheetsSelection();

        specField.setText(lastSpec != null ? lastSpec : "");

        while (true) {
            final JDialog dialog = pane.createDialog(OMR.gui.getFrame(), frameTitle);
            dialog.setVisible(true); // Blocking on modal dialog

            final Object ret = pane.getValue();

            if (!(ret instanceof Integer) || (((Integer) ret) != JOptionPane.OK_OPTION)) {
                return false;
            }

            try {
                // Check if spec is valid
                final int maxId = book.size();
                final String spec = specField.getText().trim();

                if (spec.isBlank()) {
                    logger.info("Sheets selection: <all>");
                    return book.setSheetsSelection(null);
                } else {
                    final List<Integer> ids = NaturalSpec.decode(spec, true, maxId);

                    // Check if all sheet IDs are in our book
                    final List<Integer> discarded = new ArrayList<>();

                    for (int id : ids) {
                        if ((id < 1) || (id > maxId)) {
                            discarded.add(id);
                        }
                    }

                    if (!discarded.isEmpty()) {
                        logger.warn("{} does not contain sheet(s) {}", book, discarded);
                        ids.removeAll(discarded);
                    } else {
                        final String normalizedSpec = NaturalSpec.encode(ids);
                        logger.info("Sheets selection:\"{}\" IDs:{}", normalizedSpec, ids);
                        return book.setSheetsSelection(normalizedSpec);
                    }
                }
            } catch (NumberFormatException ex) {
                logger.warn("Illegal naturals specification");
            } catch (IllegalArgumentException ex) {
                logger.warn("Sheet IDs must be strictly increasing");
            } catch (Exception ex) {
                logger.warn("{}", ex.toString());
            }
        }
    }

    //-----------------//
    // actionPerformed //
    //-----------------//
    @Override
    public void actionPerformed (ActionEvent e)
    {
        logger.info("actionPerformed");
    }

    //--------------//
    // defineLayout //
    //--------------//
    private JOptionPane defineLayout ()
    {
        // Use a panel with one line to enter detailed spec
        final FormLayout layout = new FormLayout(
                new StringBuilder() // Columns spec
                        .append("pref").append(',')
                        .append(Panel.getLabelInterval()).append(',')
                        .append("100dlu")
                        .toString(),
                Panel.makeRows(1)); // Rows spec
        final CellConstraints cst = new CellConstraints();
        final Panel panel = new Panel();
        final PanelBuilder builder = new PanelBuilder(layout, panel);
        panel.setNoInsets();

        builder.add(specField.getLabel(), cst.xy(1, 1));
        builder.add(specField.getField(), cst.xy(3, 1));
        specField.getField().setHorizontalAlignment(JTextField.LEFT);

        return new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
    }
}
