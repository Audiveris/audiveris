//----------------------------------------------------------------------------//
//                                                                            //
//                          S e c t i o n B o a r d                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.lag.Lag;
import omr.lag.Section;

import omr.run.Orientation;

import omr.selection.MouseMovement;
import omr.selection.SectionEvent;
import omr.selection.SectionIdEvent;
import omr.selection.SectionSetEvent;
import omr.selection.SelectionHint;
import omr.selection.UserEvent;

import omr.stick.StickRelation;

import omr.ui.Board;
import omr.ui.field.LIntegerField;
import omr.ui.field.SpinnerUtil;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code SectionBoard} defines a board dedicated to the display
 * of {@link omr.lag.Section} information, it can also be used as an
 * input means by directly entering the section id in the proper Id
 * spinner.
 *
 * @author Hervé Bitteur
 */
public class SectionBoard
        extends Board
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SectionBoard.class);

    /** Events this board is interested in */
    private static final Class<?>[] eventsRead = new Class<?>[]{
        SectionEvent.class,
        SectionSetEvent.class
    };

    //~ Instance fields --------------------------------------------------------
    /** Underlying lag */
    protected final Lag lag;

    /** Counter of section selection */
    protected final JLabel count = new JLabel("");

    // Section input devices
    //
    /** Button for section dump */
    private final JButton dump;

    /** Spinner for section id */
    private final JSpinner id = new JSpinner();

    // Output for plain Section
    //
    /** Field for left abscissa */
    private final LIntegerField x = new LIntegerField(
            false,
            "X",
            "Left abscissa in pixels");

    /** Field for top ordinate */
    private final LIntegerField y = new LIntegerField(
            false,
            "Y",
            "Top ordinate in pixels");

    /** Field for width */
    private final LIntegerField width = new LIntegerField(
            false,
            "Width",
            "Horizontal width in pixels");

    /** Field for height */
    private final LIntegerField height = new LIntegerField(
            false,
            "Height",
            "Vertical height in pixels");

    /** Field for weight */
    private final LIntegerField weight = new LIntegerField(
            false,
            "Weight",
            "Number of pixels in this section");

    // Additional output for StickSection (TODO: remove these fields)
    //
    /** Field for role in stick building */
    private final JTextField role = new JTextField();

    private final LIntegerField direction = new LIntegerField(
            false,
            "Dir",
            "Direction from the stick core");

    /** Field for layer number */
    private final LIntegerField layer = new LIntegerField(
            false,
            "Layer",
            "Layer number for this stick section");

    /** To avoid loop, indicate that selecting is being done by the spinner */
    private boolean idSelecting = false;

    /** To avoid loop, indicate that update() method id being processed */
    private boolean updating = false;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // SectionBoard //
    //--------------//
    /**
     * Create a Section Board
     *
     * @param lag      the related lag
     * @param expanded true for initially expanded, false for collapsed
     */
    public SectionBoard (final Lag lag,
                         boolean expanded)
    {
        super(
                Board.SECTION.name
                + ((lag.getOrientation() == Orientation.VERTICAL) ? " Vert" : " Hori"),
                Board.SECTION.position
                + ((lag.getOrientation() == Orientation.VERTICAL) ? 100 : 0),
                lag.getSectionService(),
                eventsRead,
                true, // Dump
                expanded);

        this.lag = lag;

        // Dump button
        dump = getDumpButton();
        dump.setToolTipText("Dump this section");
        dump.addActionListener(
                new ActionListener()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                // Retrieve current section selection
                Section section = (Section) lag.getSectionService()
                        .getSelection(
                        SectionEvent.class);

                if (section != null) {
                    section.dump();
                }
            }
        });
        dump.setEnabled(false); // Until a section selection is made

        // ID Spinner
        id.setToolTipText("General spinner for any section id");
        id.addChangeListener(
                new ChangeListener()
        {
            @Override
            public void stateChanged (ChangeEvent e)
            {
                // Make sure this new Id value is due to user
                // action on an Id spinner, and not the mere update
                // of section fields (which include this id).
                if (!updating) {
                    Integer sectionId = (Integer) id.getValue();
                    logger.debug("sectionId={} for {}", sectionId, lag);

                    idSelecting = true;
                    lag.getSectionService()
                            .publish(
                            new SectionIdEvent(
                            SectionBoard.this,
                            SelectionHint.SECTION_INIT,
                            sectionId));
                    idSelecting = false;
                }
            }
        });
        id.setModel(new SpinnerSectionModel(lag));
        SpinnerUtil.setEditable(id, true);
        SpinnerUtil.setRightAlignment(id);

        // Relation
        if (constants.hideRelationFields.getValue()) {
            direction.setVisible(false);
            layer.setVisible(false);
            role.setVisible(false);
        }

        role.setEditable(false);
        role.setHorizontalAlignment(JTextField.CENTER);
        role.setToolTipText("Role in the composition of the containing stick");

        // Component layout
        defineLayout();
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Section Selection has been modified
     *
     * @param event the section event
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            logger.debug("SectionBoard: {}", event);

            if (event instanceof SectionEvent) {
                handleEvent((SectionEvent) event);
            } else if (event instanceof SectionSetEvent) {
                handleEvent((SectionSetEvent) event);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        FormLayout layout = Panel.makeFormLayout(4, 3);
        PanelBuilder builder = new PanelBuilder(layout, getBody());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();
        int r = 1; // --------------------------------

        builder.add(count, cst.xy(9, r));

        r += 2; // --------------------------------
        builder.addLabel("Id", cst.xy(1, r));
        builder.add(id, cst.xy(3, r));

        builder.add(x.getLabel(), cst.xy(5, r));
        builder.add(x.getField(), cst.xy(7, r));

        builder.add(width.getLabel(), cst.xy(9, r));
        builder.add(width.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
        builder.add(weight.getLabel(), cst.xy(1, r));
        builder.add(weight.getField(), cst.xy(3, r));

        builder.add(y.getLabel(), cst.xy(5, r));
        builder.add(y.getField(), cst.xy(7, r));

        builder.add(height.getLabel(), cst.xy(9, r));
        builder.add(height.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
        builder.add(layer.getLabel(), cst.xy(1, r));
        builder.add(layer.getField(), cst.xy(3, r));

        builder.add(direction.getLabel(), cst.xy(5, r));
        builder.add(direction.getField(), cst.xy(7, r));

        builder.add(role, cst.xyw(9, r, 3));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Section
     *
     * @param sectionEvent
     */
    private void handleEvent (SectionEvent sectionEvent)
    {
        if (updating) {
            return;
        }

        try {
            // Update section fields in this board
            updating = true;

            final Section section = (sectionEvent != null)
                    ? sectionEvent.getData() : null;
            dump.setEnabled(section != null);

            Integer sectionId = null;

            if (idSelecting) {
                sectionId = (Integer) id.getValue();
            }

            emptyFields(getBody());

            if (section == null) {
                // If the user is currently using the Id spinner, make sure we
                // display the right Id value in the spinner, even if there is
                // no corresponding section
                if (idSelecting) {
                    id.setValue(sectionId);
                } else {
                    id.setValue(SpinnerUtil.NO_VALUE);
                }

                if (constants.hideRelationFields.getValue()) {
                    direction.setVisible(false);
                    layer.setVisible(false);
                    role.setVisible(false);
                }
            } else {
                // We have a valid section, let's display its fields
                id.setValue(section.getId());

                Rectangle box = section.getBounds();
                x.setValue(box.x);
                y.setValue(box.y);
                width.setValue(box.width);
                height.setValue(box.height);
                weight.setValue(section.getWeight());

                // Additional relation fields for a StickSection
                StickRelation relation = section.getRelation();

                if (relation != null) {
                    if (constants.hideRelationFields.getValue()) {
                        layer.setVisible(true);
                    }

                    layer.setValue(relation.layer);

                    if (constants.hideRelationFields.getValue()) {
                        direction.setVisible(true);
                    }

                    direction.setValue(relation.direction);

                    if (relation.role != null) {
                        role.setText(relation.role.toString());

                        if (constants.hideRelationFields.getValue()) {
                            role.setVisible(true);
                        }
                    } else {
                        if (constants.hideRelationFields.getValue()) {
                            role.setVisible(false);
                        }
                    }
                } else if (constants.hideRelationFields.getValue()) {
                    direction.setVisible(false);
                    layer.setVisible(false);
                    role.setVisible(false);
                }
            }
        } finally {
            updating = false;
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in SectionSet
     *
     * @param sectionSetEvent
     */
    private void handleEvent (SectionSetEvent sectionSetEvent)
    {
        // Display count of sections in the section set
        Set<Section> sections = sectionSetEvent.getData();

        if ((sections != null) && !sections.isEmpty()) {
            count.setText(Integer.toString(sections.size()));
        } else {
            count.setText("");
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean hideRelationFields = new Constant.Boolean(
                true,
                "Should we hide section relation fields when empty?");

    }
}
