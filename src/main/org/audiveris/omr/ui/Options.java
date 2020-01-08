//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         O p t i o n s                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.ui;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.UnitManager;
import org.audiveris.omr.constant.UnitModel;
import org.audiveris.omr.constant.UnitTreeTable;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class {@code Options} defines the user interface to edit application constants.
 *
 * @author Hervé Bitteur
 */
public class Options
{

    private static final Logger logger = LoggerFactory.getLogger(Options.class);

    /** The interface window. */
    private final JDialog dialog;

    /** The underlying tree/table of all units. */
    private UnitTreeTable unitTreeTable;

    /** Field for search entry. */
    private JTextField searchField;

    /** Label for search message. */
    private final JLabel msgLabel = new JLabel("");

    /** Current string being searched. */
    private String searchString = "";

    /** The relevant rows. */
    private List<Integer> rows = Collections.emptyList();

    /** Current user position in the relevant rows. */
    private Integer rowIndex;

    /** Listener on searchField modif. */
    private final DocumentListener docListener = new DocumentListener()
    {
        @Override
        public void changedUpdate (DocumentEvent e)
        {
            rowIndex = null;
        }

        @Override
        public void insertUpdate (DocumentEvent e)
        {
            rowIndex = null;
        }

        @Override
        public void removeUpdate (DocumentEvent e)
        {
            rowIndex = null;
        }
    };

    /** Dump */
    private final AbstractAction dumping = new AbstractAction()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            UnitManager.getInstance().dumpAllUnits();
        }
    };

    /** Check */
    private final AbstractAction checking = new AbstractAction()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            UnitManager.getInstance().checkAllUnits();
        }
    };

    /** Reset */
    private final AbstractAction resetting = new AbstractAction()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            if (OMR.gui != null) {
                if (true == OMR.gui.displayConfirmation(
                        "Reset all constants to their factory value?")) {
                    UnitManager.getInstance().resetAllUnits();
                }
            }
        }
    };

    /** Find */
    private final AbstractAction find = new AbstractAction()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            searchField.requestFocus();
        }
    };

    /** Back */
    private final AbstractAction backSearch = new AbstractAction()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            setSelection();
            msgLabel.setText("");

            if (!rows.isEmpty()) {
                if (rowIndex == null) {
                    rowIndex = rows.size() - 1;
                    unitTreeTable.scrollRowToVisible(rows.get(rowIndex));
                } else if (rowIndex > 0) {
                    rowIndex--;
                    unitTreeTable.scrollRowToVisible(rows.get(rowIndex));
                } else {
                    msgLabel.setText("not found");
                }
            } else {
                msgLabel.setText("not found");
            }
        }
    };

    /** Forward */
    private final AbstractAction forwardSearch = new AbstractAction()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            setSelection();
            msgLabel.setText("");

            if (!rows.isEmpty()) {
                if (rowIndex == null) {
                    rowIndex = 0;
                    unitTreeTable.scrollRowToVisible(rows.get(rowIndex));
                } else if (rowIndex < (rows.size() - 1)) {
                    rowIndex++;
                    unitTreeTable.scrollRowToVisible(rows.get(rowIndex));
                } else {
                    msgLabel.setText("not found");
                }
            } else {
                msgLabel.setText("not found");
            }
        }
    };

    /**
     * Creates a new Options object.
     */
    public Options ()
    {
        // Preload constant units
        UnitManager.getInstance().preLoadUnits();

        dialog = new JDialog(OMR.gui.getFrame());
        dialog.setName("OptionsFrame");  // For SAF life cycle

        JComponent framePane = (JComponent) dialog.getContentPane();
        framePane.setLayout(new BorderLayout());

        InputMap inputMap = framePane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = framePane.getActionMap();

        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        framePane.add(toolBar, BorderLayout.NORTH);

        // Dump button
        JButton dumpButton = new JButton(dumping);
        dumpButton.setName("optionsDumpButton");
        toolBar.add(dumpButton);

        // Check button
        JButton checkButton = new JButton(checking);
        checkButton.setName("optionsCheckButton");
        toolBar.add(checkButton);

        // Reset button
        JButton resetButton = new JButton(resetting);
        resetButton.setName("optionsResetButton");
        toolBar.add(resetButton);

        // Some space
        toolBar.add(Box.createHorizontalStrut(100));

        toolBar.add(new JLabel("Search:"));

        // Back button
        JButton backButton = new JButton(backSearch);
        backButton.setName("optionsBackButton");
        toolBar.add(backButton);
        inputMap.put(KeyStroke.getKeyStroke("shift F3"), "backSearch");
        actionMap.put("backSearch", backSearch);

        // Search entry
        searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(200, 28));
        searchField.setName("optionsSearchField");
        searchField.setHorizontalAlignment(JTextField.LEFT);
        toolBar.add(searchField);
        inputMap.put(KeyStroke.getKeyStroke("ctrl F"), "find");
        actionMap.put("find", find);
        searchField.getDocument().addDocumentListener(docListener);

        // Forward button
        JButton forwardButton = new JButton(forwardSearch);
        forwardButton.setName("optionsForwardButton");
        toolBar.add(forwardButton);

        // Some space, message field
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(msgLabel);

        // TreeTable
        UnitModel unitModel = new UnitModel();
        unitTreeTable = new UnitTreeTable(unitModel);
        framePane.add(new JScrollPane(unitTreeTable), BorderLayout.CENTER);

        // Needed to process user input when RETURN/ENTER is pressed
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "forwardSearch");
        inputMap.put(KeyStroke.getKeyStroke("F3"), "forwardSearch");
        actionMap.put("forwardSearch", forwardSearch);

        // Resources injection
        ResourceMap resources = Application.getInstance().getContext().getResourceMap(getClass());
        resources.injectComponents(dialog);

        // Make sure the search entry field gets the focus at creation time
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowOpened (WindowEvent e)
            {
                searchField.requestFocus();
            }
        });
    }

    //--------------//
    // getComponent //
    //--------------//
    public JDialog getComponent ()
    {
        return dialog;
    }

    //--------------//
    // setSelection //
    //--------------//
    /**
     * Pre-select the matching rows of the table, if any.
     */
    private void setSelection ()
    {
        searchString = searchField.getText().trim();

        Set<Object> matches = UnitManager.getInstance().searchUnits(searchString);
        rows = unitTreeTable.setNodesSelection(matches);

        if (rows == null) {
            rowIndex = null;
        }
    }
}
