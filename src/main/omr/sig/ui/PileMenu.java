//----------------------------------------------------------------------------//
//                                                                            //
//                               P i l e M e n u                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.AbstractGlyphMenu;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.sig.AbstractConnection;
import omr.sig.BeamStemRelation;
import omr.sig.HeadStemRelation;
import omr.sig.Inter;
import omr.sig.Relation;
import omr.sig.SIGraph;
import omr.sig.StemInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.Set;

import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;

/**
 * Class {@code PileMenu} displays a collection of glyphs,
 * generally piled one on top of the other on a common point.
 *
 * @author Hervé Bitteur
 */
public class PileMenu
        extends AbstractGlyphMenu
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            PileMenu.class);

    //~ Instance fields --------------------------------------------------------
    private GlyphListener glyphListener = new GlyphListener();

    private RelationListener relationListener = new RelationListener();

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new PileMenu object.
     *
     * @param sheet the related sheet
     */
    public PileMenu (Sheet sheet)
    {
        super(sheet, "Pile ...");
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // updateMenu //
    //------------//
    @Override
    public int updateMenu (Set<Glyph> glyphs)
    {
        super.updateMenu(glyphs);

        // We rebuild the menu items on each update, since the set of glyphs
        // is brand new. 
        menu.removeAll();

        if (glyphNb > 0) {
            insertTitle(menu, "Glyphs:");

            for (Glyph glyph : glyphs) {
                SystemInfo system = glyph.getSystem();
                Collection<Inter> inters = glyph.getInterpretations();

                if (inters.isEmpty()) {
                    JMenuItem item = new JMenuItem(
                            new GlyphAction(glyph, null));
                    item.addMouseListener(glyphListener);
                    menu.add(item);
                } else {
                    // Build a menu with all inters for this glyph
                    JMenu intersMenu = new JMenu("" + glyph.getId());
                    intersMenu.addMouseListener(glyphListener);
                    intersMenu.setToolTipText(tipOf(glyph));
                    insertTitle(intersMenu, "Interpretations:");

                    for (Inter inter : inters) {
                        SIGraph sig = system.getSig();
                        Set<Relation> rels = sig.edgesOf(inter);

                        if ((rels == null) || rels.isEmpty()) {
                            // Just a interpretation item
                            intersMenu.add(
                                    new JMenuItem(new InterAction(inter)));
                        } else {
                            // A whole menu of relations for this interpretation
                            intersMenu.add(
                                    new RelationMenu(sig, inter, rels).menu);
                        }
                    }

                    menu.add(intersMenu);
                }
            }
        }

        return glyphNb;
    }

    //-------------//
    // insertTitle //
    //-------------//
    /**
     * Insert a pseudo-item, to be used as a menu title.
     *
     * @param menu the containing menu
     * @param text the title text
     */
    private static void insertTitle (JMenu menu,
                                     String text)
    {
        JMenuItem title = new JMenuItem(text);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setEnabled(false);
        menu.add(title);
        menu.addSeparator();
    }

    //---------//
    // publish //
    //---------//
    private void publish (Glyph glyph)
    {
        nest.getGlyphService()
                .publish(
                new GlyphEvent(
                this,
                SelectionHint.GLYPH_INIT,
                MouseMovement.PRESSING,
                glyph));
    }

    //-------//
    // tipOf //
    //-------//
    private String tipOf (Glyph glyph)
    {
        String tip = "layer: " + glyph.getLayer();
        Shape shape = glyph.getShape();

        if (shape != null) {
            tip += (", shape: " + shape);
        }

        return tip;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-------------//
    // GlyphAction //
    //-------------//
    /**
     * Publish a glyph.
     */
    private class GlyphAction
            extends AbstractAction
    {
        //~ Instance fields ----------------------------------------------------

        final Glyph glyph;

        //~ Constructors -------------------------------------------------------
        public GlyphAction (Glyph glyph,
                            String text)
        {
            this.glyph = glyph;

            putValue(NAME, (text != null) ? text : ("" + glyph.getId()));
            putValue(SHORT_DESCRIPTION, tipOf(glyph));
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            publish(glyph);
        }
    }

    //---------------//
    // GlyphListener //
    //---------------//
    /**
     * Publish related glyph when entered by mouse.
     */
    private class GlyphListener
            extends MyMouseListener
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public void mouseEntered (MouseEvent e)
        {
            JMenuItem item = (JMenuItem) e.getSource();
            GlyphAction action = (GlyphAction) item.getAction();
            final Glyph glyph;

            if (action != null) {
                glyph = action.glyph;
            } else {
                int id = Integer.parseInt(item.getText());
                glyph = nest.getGlyph(id);
            }

            publish(glyph);
        }
    }

    //-------------//
    // InterAction //
    //-------------//
    /**
     * Interpretation w/o relation.
     */
    private class InterAction
            extends AbstractAction
    {
        //~ Instance fields ----------------------------------------------------

        /** The underlying interpretation. */
        protected final Inter inter;

        //~ Constructors -------------------------------------------------------
        public InterAction (Inter inter)
        {
            this.inter = inter;

            Shape shape = inter.getShape();
            putValue(NAME, String.format("%.2f %s", inter.getGrade(), shape));
            putValue(SHORT_DESCRIPTION, inter.toString());
            putValue(SMALL_ICON, shape.getDecoratedSymbol());
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            logger.info(inter.toString());
        }
    }

    //-----------------//
    // MyMouseListener //
    //-----------------//
    private abstract class MyMouseListener
            implements MouseListener
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public void mouseClicked (MouseEvent e)
        {
        }

        @Override
        public void mouseExited (MouseEvent e)
        {
        }

        @Override
        public void mousePressed (MouseEvent e)
        {
        }

        @Override
        public void mouseReleased (MouseEvent e)
        {
        }
    }

    //----------------//
    // RelationAction //
    //----------------//
    /**
     * Display a relation and select the other glyph (source or target).
     */
    private class RelationAction
            extends AbstractAction
    {
        //~ Instance fields ----------------------------------------------------

        /** Underlying relation. */
        private final Relation relation;

        /** The other inter, if any. */
        private final Inter other;

        //~ Constructors -------------------------------------------------------
        public RelationAction (SIGraph sig,
                               Inter inter,
                               Relation relation,
                               BeamStemRelation beamStemRel)
        {
            this.relation = relation;

            Inter source = sig.getEdgeSource(relation);
            Inter target = sig.getEdgeTarget(relation);
            StringBuilder sb = new StringBuilder();
            sb.append(relation);

            if (source != inter) {
                other = source;
                sb.append(" <- ")
                        .append(source);
            } else if (target != inter) {
                other = target;
                sb.append(" -> ")
                        .append(target);
            } else {
                other = null;
            }

            if (relation instanceof AbstractConnection) {
                AbstractConnection rel = (AbstractConnection) relation;
                double cp = sig.getContextualGrade(rel);
                sb.append(" CP:")
                        .append(String.format("%.2f", cp));

                if (rel instanceof HeadStemRelation
                    && (beamStemRel != null)) {
                    double cp2 = sig.getContextualGrade(
                            inter,
                            rel,
                            beamStemRel);
                    sb.append(" CP2:")
                            .append(String.format("%.2f", cp2));
                }
            }

            putValue(NAME, sb.toString());
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            if (other != null) {
                publish(other.getGlyph());
            }
        }
    }

    //------------------//
    // RelationListener //
    //------------------//
    /**
     * Publish the other interpretation glyph when entered by mouse.
     */
    private class RelationListener
            extends MyMouseListener
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public void mouseEntered (MouseEvent e)
        {
            JMenuItem item = (JMenuItem) e.getSource();
            RelationAction action = (RelationAction) item.getAction();
            publish(action.other.getGlyph());
        }
    }

    //--------------//
    // RelationMenu //
    //--------------//
    /**
     * Build a menu with all relations which interpretation 'inter' is
     * part of.
     */
    private class RelationMenu
    {
        //~ Instance fields ----------------------------------------------------

        private final JMenu menu;

        //~ Constructors -------------------------------------------------------
        public RelationMenu (SIGraph sig,
                             Inter inter,
                             Collection<? extends Relation> rels)
        {
            menu = new JMenu(
                    String.format("%.2f~%s", inter.getGrade(), inter.getShape()));
            menu.setToolTipText(inter.toString());
            menu.setIcon(inter.getShape().getDecoratedSymbol());

            // Show the initial inter
            JMenuItem gItem = new JMenuItem(
                    new GlyphAction(
                    inter.getGlyph(),
                    "Relations of " + inter + ":"));
            gItem.setHorizontalAlignment(SwingConstants.CENTER);
            gItem.addMouseListener(glyphListener);
            menu.add(gItem);
            menu.addSeparator();

            // Look for a BeamStem relation around a stem
            BeamStemRelation beamStemRel = null;

            if (inter instanceof StemInter) {
                for (Relation relation : rels) {
                    if (relation instanceof BeamStemRelation) {
                        beamStemRel = (BeamStemRelation) relation;

                        break;
                    }
                }
            }

            // Show each relation
            for (Relation relation : rels) {
                JMenuItem item = new JMenuItem(
                        new RelationAction(sig, inter, relation, beamStemRel));
                item.addMouseListener(relationListener);
                menu.add(item);
            }
        }
    }
}
