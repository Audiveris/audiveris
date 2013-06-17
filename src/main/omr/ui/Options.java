//----------------------------------------------------------------------------//
//                                                                            //
//                               O p t i o n s                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.Main;

import omr.constant.UnitManager;
import omr.constant.UnitModel;
import omr.constant.UnitTreeTable;

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
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

/**
 * Class {@code Options} defines the user interface to display and edit
 * applications options (constants and loggers)
 *
 * @author Hervé Bitteur
 */
public class Options
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Options.class);

    //~ Instance fields --------------------------------------------------------
    /** The interface window */
    private JFrame frame;

    /** The underlying tree/table of all units */
    private UnitTreeTable unitTreeTable;

    /** Field for search entry */
    private JTextField searchField;

    /** Current string being searched */
    private String searchString = "";

    /** The relevant rows */
    private List<Integer> rows = Collections.emptyList();

    /** Current user position in the relevant rows */
    private Integer rowIndex;

    /** Dump */
    private final AbstractAction dumping = new AbstractAction()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            UnitManager.getInstance()
                    .dumpAllUnits();
        }
    };

    /** Check */
    private final AbstractAction checking = new AbstractAction()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            UnitManager.getInstance()
                    .checkAllUnits();
        }
    };

    /** Reset */
    private final AbstractAction resetting = new AbstractAction()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            MainGui gui = Main.getGui();

            if (gui != null) {
                if (true == gui.displayConfirmation(
                        "Reset all constants to their factory value?")) {
                    UnitManager.getInstance()
                            .resetAllUnits();
                }
            }
        }
    };

    /** Back */
    private final AbstractAction backSearch = new AbstractAction()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            setSelection();

            if (!rows.isEmpty()) {
                if ((rowIndex != null) && (rowIndex > 0)) {
                    rowIndex--;
                } else {
                    rowIndex = rows.size() - 1;
                }

                unitTreeTable.scrollRowToVisible(rows.get(rowIndex));
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

            if (!rows.isEmpty()) {
                if ((rowIndex != null) && (rowIndex < (rows.size() - 1))) {
                    rowIndex++;
                } else {
                    rowIndex = 0;
                }

                unitTreeTable.scrollRowToVisible(rows.get(rowIndex));
            }
        }
    };

    //~ Constructors -----------------------------------------------------------
    //---------//
    // Options //
    //---------//
    /**
     * Creates a new Options object.
     */
    public Options ()
    {
        // Preload constant units
        UnitManager.getInstance()
                .preLoadUnits(Main.class.getName());

        frame = new JFrame();
        frame.setName("optionsFrame");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.getContentPane()
                .setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        frame.add(toolBar, BorderLayout.NORTH);

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

        // Search entry
        searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(200, 28));
        searchField.setName("optionsSearchField");
        searchField.setHorizontalAlignment(JTextField.LEFT);
        toolBar.add(searchField);

        // Forward button
        JButton forwardButton = new JButton(forwardSearch);
        forwardButton.setName("optionsForwardButton");
        toolBar.add(forwardButton);

        // TreeTable
        UnitModel unitModel = new UnitModel();
        unitTreeTable = new UnitTreeTable(unitModel);
        frame.add(new JScrollPane(unitTreeTable), BorderLayout.CENTER);

        // Needed to process user input when RETURN/ENTER is pressed
        toolBar.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ENTER"), "EnterAction");
        toolBar.getActionMap()
                .put("EnterAction", forwardSearch);

        // Resources injection
        ResourceMap resource = Application.getInstance()
                .getContext()
                .getResourceMap(getClass());
        resource.injectComponents(frame);

        // Make sure the search entry field gets the focus at creation time
        frame.addWindowListener(
                new WindowAdapter()
        {
            @Override
            public void windowOpened (WindowEvent e)
            {
                searchField.requestFocus();
            }
        });
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // getComponent //
    //--------------//
    public JFrame getComponent ()
    {
        return frame;
    }

    //--------------//
    // setSelection //
    //--------------//
    /**
     * Pre-select the matching rows of the table, if any.
     */
    private void setSelection ()
    {
        searchString = searchField.getText()
                .trim();

        Set<Object> matches = UnitManager.getInstance()
                .searchUnits(searchString);
        rows = unitTreeTable.setNodesSelection(matches);

        if (rows == null) {
            rowIndex = null;
        }
    }
}
