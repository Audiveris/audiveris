//----------------------------------------------------------------------------//
//                                                                            //
//                            C h e c k P a n e l                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.check;

import omr.constant.Constant;

import omr.log.Logger;

import omr.ui.util.Panel;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.*;

/**
 * Class <code>CheckPanel</code> handles a panel to display the results of a
 * check suite.
 *
 * @param <C> the subtype of Checkable-compatible objects used in the
 * homogeneous collection of checks of the suite
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class CheckPanel<C extends Checkable>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(CheckPanel.class);

    // Colors
    private static final Color  GREEN_COLOR = new Color(100, 150, 0);
    private static final Color  ORANGE_COLOR = new Color(255, 150, 0);
    private static final Color  RED_COLOR = new Color(255, 0, 0);

    // Sizes
    private static final String LINE_GAP = "1dlu";
    private static final String COLUMN_GAP = "1dlu";
    private static final int    FIELD_WIDTH = 4;

    //~ Instance fields --------------------------------------------------------

    /** The related check suite (the model) */
    private CheckSuite<C> suite;

    /** The swing component that includes all the fields */
    private Panel component;

    /** The field for global result */
    private JTextField globalField;

    /** Matrix of all value fields */
    private JTextField[][] values;

    /** Matrix of all bound fields */
    private JTextField[][] bounds;

    /** Last object checked */
    private C object;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // CheckPanel //
    //------------//
    /**
     * Create a check panel for a given suite
     *
     * @param suite the suite whose results are to be displayed
     */
    public CheckPanel (CheckSuite<C> suite)
    {
        // Global field (for global result)
        globalField = new JTextField(FIELD_WIDTH);
        globalField.setEditable(false);
        globalField.setHorizontalAlignment(JTextField.CENTER);

        setSuite(suite);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return component;
    }

    //----------//
    // passForm //
    //----------//
    /**
     * Pass the whole suite on the provided checkable object, and display the
     * results
     *
     * @param object the object to be checked
     */
    public void passForm (C object)
    {
        // Remember the 'current' object'
        this.object = object;

        if (object == null) {
            return;
        }

        CheckResult result = new CheckResult();
        double      grade = 0d;
        boolean     failed = false;

        // Fill one row per check
        for (int index = 0; index < suite.getChecks()
                                         .size(); index++) {
            Check<C> check = suite.getChecks()
                                  .get(index);

            // Run this check
            check.pass(object, result, false);
            grade += (result.flag * suite.getWeights()
                                         .get(index));

            // Update proper field to display check result
            JTextField field = null;

            switch (result.flag) {
            case Check.RED :
                failed = true;

                if (check.isCovariant()) {
                    field = values[index][0];
                    field.setToolTipText("Value is too low");
                } else {
                    field = values[index][2];
                    field.setToolTipText("Value is too high");
                }

                field.setForeground(RED_COLOR);

                break;

            case Check.ORANGE :
                field = values[index][1];
                field.setToolTipText("Value is acceptable");
                field.setForeground(ORANGE_COLOR);

                break;

            case Check.GREEN :

                if (check.isCovariant()) {
                    field = values[index][2];
                } else {
                    field = values[index][0];
                }

                field.setToolTipText("Value is OK");
                field.setForeground(GREEN_COLOR);

                break;
            }

            field.setText(textOf(result.value));
        }

        // Global suite result
        if (failed) {
            globalField.setForeground(RED_COLOR);
            globalField.setToolTipText("Check has failed!");
            globalField.setText("Failed");
        } else {
            grade /= suite.getTotalWeight();

            if (grade >= suite.getThreshold()) {
                globalField.setForeground(GREEN_COLOR);
                globalField.setToolTipText("Check has succeeded!");
            } else {
                globalField.setForeground(RED_COLOR);
                globalField.setToolTipText("Check has failed!");
            }

            globalField.setText(textOf(grade));
        }
    }

    //----------//
    // setSuite //
    //----------//
    /**
     * Assign a (new) suite to the check pane
     *
     * @param suite the (new) check suite to be used
     */
    public void setSuite (CheckSuite<C> suite)
    {
        this.suite = suite;

        if (suite != null) {
            createValueFields(); // Values
            createBoundFields(); // Bounds
            buildComponent(); // Create/update component
        }

        // Refresh the display
        if (component != null) {
            component.validate();
            component.repaint();
        }
    }

    //----------------//
    // buildComponent //
    //----------------//
    private void buildComponent ()
    {
        // Either allocate a new Panel or empty the existing one
        if (component == null) {
            component = new Panel();
            component.setNoInsets();

            // Needed to process user input when RETURN/ENTER is pressed
            component.getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                     .put(KeyStroke.getKeyStroke("ENTER"), "ParamAction");
            component.getActionMap()
                     .put("ParamAction", new ParamAction());
        } else {
            component.removeAll();
        }

        final int    checkNb = suite.getChecks()
                                    .size();
        PanelBuilder b = new PanelBuilder(createLayout(checkNb), component);
        b.setDefaultDialogBorder();

        CellConstraints c = new CellConstraints();

        // Rows
        int ic = -1; // Check index
        int r = -1; // Row index

        for (Check check : suite.getChecks()) {
            ic++;
            r += 2;

            // Covariance label
            JLabel covariantLabel;

            if (check.isCovariant()) {
                covariantLabel = new JLabel("T");
                covariantLabel.setToolTipText("Check is covariant");
            } else {
                covariantLabel = new JLabel("F");
                covariantLabel.setToolTipText("Check is contravariant");
            }

            b.add(covariantLabel, c.xy(1, r));

            // Weight label
            JLabel weightLabel = new JLabel(
                String.format("%s", suite.getWeights().get(ic).intValue()));
            weightLabel.setToolTipText("Relative weight for this check");
            b.add(weightLabel, c.xy(3, r));

            // Name label with proper tooltip
            JLabel nameLabel = new JLabel(check.getName());

            if (check.getDescription() != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("<html>");
                sb.append(check.getDescription());

                Constant constant = check.getLowConstant();

                // Tell data unit if relevant
                sb.append("<br/>");

                if (constant.getQuantityUnit() != null) {
                    sb.append("Unit=")
                      .append(constant.getQuantityUnit());
                } else {
                    // Otherwise, simply tell the data type
                    sb.append("Type=")
                      .append(constant.getShortTypeName());
                }

                sb.append("</html>");

                nameLabel.setToolTipText(sb.toString());
            }

            b.add(nameLabel, c.xy(5, r));

            // Value & bound fields
            b.add(values[ic][0], c.xy(7, r));
            b.add(bounds[ic][0], c.xy(9, r));
            b.add(values[ic][1], c.xy(11, r));
            b.add(bounds[ic][1], c.xy(13, r));
            b.add(values[ic][2], c.xy(15, r));
        }

        // Last row for global result
        r += 2;

        JLabel globalLabel = new JLabel("Result");
        globalLabel.setToolTipText("Global check result");
        b.add(globalLabel, c.xy(7, r));
        b.add(globalField, c.xy(11, r));
    }

    //-------------------//
    // createBoundFields //
    //-------------------//
    private void createBoundFields ()
    {
        // Allocate bound fields (2 per check)
        final int checkNb = suite.getChecks()
                                 .size();
        bounds = new JTextField[checkNb][];

        for (int ic = 0; ic < checkNb; ic++) {
            Check check = suite.getChecks()
                               .get(ic);
            bounds[ic] = new JTextField[2];

            for (int i = 0; i <= 1; i++) {
                JTextField field = new JTextField(FIELD_WIDTH);
                field.setHorizontalAlignment(JTextField.CENTER);
                bounds[ic][i] = field;

                Constant.Double constant = (i == 0) ? check.getLowConstant()
                                           : check.getHighConstant();

                field.setText(textOf(constant.getValue()));
                field.setToolTipText(
                    "<html>" + constant.getName() + "<br/>" +
                    constant.getDescription() + "</html>");
            }
        }
    }

    //--------------//
    // createLayout //
    //--------------//
    private FormLayout createLayout (int checkNb)
    {
        // Build proper column specification
        StringBuffer sbc = new StringBuffer();
        sbc.append("center:pref")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", "); // Covariance
        sbc.append("center:pref")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", "); // Weight
        sbc.append(" right:46dlu")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", "); // Name
        sbc.append(" right:pref")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", ");
        sbc.append(" right:pref")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", "); // Low limit
        sbc.append(" right:pref")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", ");
        sbc.append(" right:pref")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", "); // High Limit
        sbc.append(" right:pref");

        // Build proper row specification
        StringBuffer sbr = new StringBuffer();

        for (int n = 0; n <= checkNb; n++) {
            if (n != 0) {
                sbr.append(", ")
                   .append(LINE_GAP)
                   .append(", ");
            }

            sbr.append("pref");
        }

        if (logger.isFineEnabled()) {
            logger.fine("sb cols=" + sbc);
            logger.fine("sb rows=" + sbr);
        }

        // Create proper form layout
        return new FormLayout(
            sbc.toString(), //cols
            sbr.toString()); //rows
    }

    //-------------------//
    // createValueFields //
    //-------------------//
    private void createValueFields ()
    {
        // Allocate value fields (3 per check)
        final int checkNb = suite.getChecks()
                                 .size();
        values = new JTextField[checkNb][];

        for (int n = 0; n < checkNb; n++) {
            values[n] = new JTextField[3];

            for (int i = 0; i <= 2; i++) {
                JTextField field = new JTextField(FIELD_WIDTH);
                field.setEditable(false);
                field.setHorizontalAlignment(JTextField.CENTER);
                values[n][i] = field;
            }
        }
    }

    //--------//
    // textOf //
    //--------//
    private String textOf (double val)
    {
        return String.format(Locale.getDefault(), "%5.2f", val);
    }

    //---------//
    // valueOf //
    //---------//
    private double valueOf (String text)
    {
        Scanner scanner = new Scanner(text);
        scanner.useLocale(Locale.getDefault());

        while (scanner.hasNext()) {
            if (scanner.hasNextDouble()) {
                return scanner.nextDouble();
            } else {
                scanner.next();
            }
        }

        // Kludge!
        return Double.NaN;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        // Method run whenever user presses Return/Enter in one of the parameter
        // fields
        public void actionPerformed (ActionEvent e)
        {
            // Any & several bounds may have been modified by the user
            // Since the same constant can be used in several fields, we have to
            // take a snapshot of all constants values, before modifying any one
            Map<Constant.Double, Double> values = new HashMap<Constant.Double, Double>();

            for (Check check : suite.getChecks()) {
                values.put(
                    check.getLowConstant(),
                    check.getLowConstant().getValue());
                values.put(
                    check.getHighConstant(),
                    check.getHighConstant().getValue());
            }

            boolean modified = false;
            int     ic = -1;

            for (Check check : suite.getChecks()) {
                ic++;

                // Check the bounds wrt the corresponding fields
                for (int i = 0; i < 2; i++) {
                    final Constant.Double constant = (i == 0)
                                                     ? check.getLowConstant()
                                                     : check.getHighConstant();

                    // Simplistic test to detect modification
                    final JTextField field = bounds[ic][i];
                    final String     oldString = textOf(values.get(constant))
                                                     .trim();
                    final String     newString = field.getText()
                                                      .trim();

                    if (!oldString.equals(newString)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Check '")
                          .append(check.getName())
                          .append("':");

                        if (i == 0) {
                            sb.append(" Low");
                        } else {
                            sb.append(" High");
                        }

                        sb.append(" bound '")
                          .append(constant.getName())
                          .append("'");

                        final String context = sb.toString();

                        // Actually convert the value and update the constant
                        try {
                            constant.setValue(valueOf(newString));
                            modified = true;
                            sb.append(" modified from ")
                              .append(oldString)
                              .append(" to ")
                              .append(newString);
                            logger.info(sb.toString());
                        } catch (Exception ex) {
                            logger.warning(
                                "Error in " + context + ", " +
                                ex.getLocalizedMessage());
                        }
                    }
                }
            }

            // If at least one modification has been made, update the whole 
            // table with both suite parameters and object results
            if (modified) {
                setSuite(suite);
                passForm(object);
            }
        }
    }
}
