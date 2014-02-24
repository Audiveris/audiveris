//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S i g S o l v e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.ShapeSet;

import omr.grid.StaffInfo;

import omr.math.GeoOrder;
import omr.math.GeoUtil;

import omr.sheet.SystemInfo;
import static omr.sig.StemPortion.*;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;
import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Class {@code SigSolver} deals with SIG resolution.
 *
 * @author Hervé Bitteur
 */
public class SigSolver
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SigSolver.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    private final SIGraph sig;

    //~ Constructors -------------------------------------------------------------------------------
    //-----------//
    // SigSolver //
    //-----------//
    /**
     * Creates a new SigSolver object.
     *
     * @param system the related system
     * @param sig    the system SIG
     */
    public SigSolver (SystemInfo system,
                      SIGraph sig)
    {
        this.system = system;
        this.sig = sig;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // contextualize //
    //---------------//
    /**
     * Compute contextual grades of target interpretations based on
     * their supporting sources.
     */
    public void contextualize ()
    {
        try {
            for (Inter inter : sig.vertexSet()) {
                sig.computeContextualGrade(inter, false);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    //-------//
    // solve //
    //-------//
    /**
     * Reduce the interpretations and relations of the SIG.
     */
    public void solve ()
    {
        final boolean logging = false;

        if (logging) {
            logger.info("S#{} solving sig ...", system.getId());
        }

        // General overlap checks
        flagOverlaps();
        flagHeadInconsistency();

        int modifs; // modifications done in current iteration
        int reductions; // Count of reductions performed
        int deletions; // Count of deletions performed

        do {
            // First, remove all inters with too low contextual grade
            deletions = purgeWeakInters();

            do {
                modifs = 0;
                // Detect lack of mandatory support relation for certain inters
                modifs += checkHeads();
                deletions += purgeWeakInters();

                modifs += checkBeams();
                deletions += purgeWeakInters();

                modifs += checkHooks();
                deletions += purgeWeakInters();

                modifs += checkLedgers();
                deletions += purgeWeakInters();

                modifs += checkStems();
                deletions += purgeWeakInters();

                if (logging) {
                    logger.info("S#{} modifs: {}", system.getId(), modifs);
                }
            } while (modifs > 0);

            // Remaining exclusions
            reductions = sig.reduceExclusions().size();

            if (logging) {
                logger.info("S#{} reductions: {}", system.getId(), reductions);
            }
        } while ((reductions > 0) || (deletions > 0));
    }

    //------------------//
    // beamHasBothStems //
    //------------------//
    private boolean beamHasBothStems (FullBeamInter beam)
    {
        boolean hasLeft = false;
        boolean hasRight = false;

        if (beam.isVip()) {
            logger.info("VIP beamHasBothStems for {}", beam);
        }

        for (Relation rel : sig.edgesOf(beam)) {
            if (rel instanceof BeamStemRelation) {
                BeamStemRelation bsRel = (BeamStemRelation) rel;
                BeamPortion portion = bsRel.getBeamPortion();

                if (portion == BeamPortion.LEFT) {
                    hasLeft = true;
                } else if (portion == BeamPortion.RIGHT) {
                    hasRight = true;
                }
            }
        }

        return hasLeft && hasRight;
    }

    //------------//
    // checkBeams //
    //------------//
    /**
     * Perform checks on beams.
     *
     * @return the count of modifications done
     */
    private int checkBeams ()
    {
        int modifs = 0;
        List<Inter> beams = sig.inters(FullBeamInter.class);

        for (Iterator<Inter> it = beams.iterator(); it.hasNext();) {
            FullBeamInter beam = (FullBeamInter) it.next();

            if (!beamHasBothStems(beam)) {
                if (beam.isVip() || logger.isDebugEnabled()) {
                    logger.info("VIP Deleting beam lacking stem {}", beam);
                }

                sig.removeVertex(beam);
                it.remove();
                modifs++;
            }
        }

        return modifs;
    }

    //---------------//
    // checkHeadSide //
    //---------------//
    /**
     * If head is on the wrong side of the stem, check if there is a
     * head on the other side, located one or two step(s) further.
     *
     * @param head the head inter (black or void)
     * @return the number of modifications done
     */
    private int checkHeadSide (Inter head)
    {
        int modifs = 0;

        // Check all connected stems
        List<HeadStemRelation> stemRels = new ArrayList<HeadStemRelation>();

        for (Relation rel : sig.edgesOf(head)) {
            if (rel instanceof HeadStemRelation) {
                stemRels.add((HeadStemRelation) rel);
            }
        }

        RelsLoop:
        for (HeadStemRelation rel : stemRels) {
            StemInter stem = (StemInter) sig.getEdgeTarget(rel);

            // What is the stem direction? (up: dir < 0, down: dir > 0)
            int dir = stemDirection(stem);

            // Side is normal?
            HorizontalSide headSide = rel.getHeadSide();

            if (((headSide == LEFT) && (dir > 0)) || ((headSide == RIGHT) && (dir < 0))) {
                continue; // It's OK
            }

            // Pitch of the note head
            int pitch = ((AbstractNoteInter) head).getPitch();

            // Target side and target pitches of other head
            // Look for presence of head on other side with target pitch
            HorizontalSide targetSide = (headSide == LEFT) ? RIGHT : LEFT;

            for (int s : new int[]{1, 2}) {
                int targetPitch = pitch + ((headSide == LEFT) ? s : (-s));

                if (stem.lookupHead(targetSide, targetPitch) != null) {
                    continue RelsLoop; // OK
                }
            }

            // We have a bad head+stem couple, let's remove the relationship
            if (head.isVip() || logger.isDebugEnabled()) {
                logger.info("Wrong side for {} on {}", head, stem);
            }

            sig.removeEdge(rel);
            sig.insertExclusion(head, stem, Exclusion.Cause.INCOMPATIBLE);
            modifs++;
        }

        return modifs;
    }

    //------------//
    // checkHeads //
    //------------//
    /**
     * Perform checks on heads.
     *
     * @return the count of modifications done
     */
    private int checkHeads ()
    {
        int modifs = 0;
        final List<Inter> heads = sig.inters(ShapeSet.NoteHeads.getShapes());

        for (Iterator<Inter> it = heads.iterator(); it.hasNext();) {
            final Inter head = it.next();

            if (!headHasStem(head)) {
                if (head.isVip() || logger.isDebugEnabled()) {
                    logger.info("No stem for {}", head);
                }

                sig.removeVertex(head);
                it.remove();
                modifs++;

                continue;
            }

            modifs += checkHeadSide(head);
        }

        return modifs;
    }

    //------------//
    // checkHooks //
    //------------//
    /**
     * Perform checks on beam hooks.
     *
     * @return the count of modifications done
     */
    private int checkHooks ()
    {
        int modifs = 0;
        List<Inter> hooks = sig.inters(BeamHookInter.class);

        for (Iterator<Inter> it = hooks.iterator(); it.hasNext();) {
            BeamHookInter hook = (BeamHookInter) it.next();

            if (!hookHasStem(hook)) {
                if (hook.isVip() || logger.isDebugEnabled()) {
                    logger.info("Deleting beam hook lacking stem {}", hook);
                }

                sig.removeVertex(hook);
                it.remove();
                modifs++;

                continue;
            }

            //TODO: Check the hook has a beam nearby on the same stem
        }

        return modifs;
    }

    //--------------//
    // checkLedgers //
    //--------------//
    /**
     * Perform checks on ledger.
     *
     * @return the count of modifications done
     */
    private int checkLedgers ()
    {
        // All system notes, sorted by abscissa
        List<Inter> allNotes = sig.inters(
                ShapeSet.shapesOf(ShapeSet.NoteHeads.getShapes(), ShapeSet.Notes.getShapes()));
        Collections.sort(allNotes, Inter.byAbscissa);

        int modifs = 0;
        boolean modified;

        do {
            modified = false;

            for (StaffInfo staff : system.getStaves()) {
                SortedMap<Integer, SortedSet<LedgerInter>> map = staff.getLedgerMap();

                for (Entry<Integer, SortedSet<LedgerInter>> entry : map.entrySet()) {
                    int index = entry.getKey();
                    SortedSet<LedgerInter> ledgers = entry.getValue();
                    List<LedgerInter> toRemove = new ArrayList<LedgerInter>();

                    for (LedgerInter ledger : ledgers) {
                        if (ledger.isVip()) {
                            logger.info("VIP ledger {}", ledger);
                        }

                        if (!ledgerHasNoteOrLedger(staff, index, ledger, allNotes)) {
                            if (ledger.isVip() || logger.isDebugEnabled()) {
                                logger.info("Deleting orphan ledger {}", ledger);
                            }

                            sig.removeVertex(ledger);
                            toRemove.add(ledger);
                            modified = true;
                            modifs++;
                        }
                    }

                    if (!toRemove.isEmpty()) {
                        ledgers.removeAll(toRemove);
                    }
                }
            }
        } while (modified);

        return modifs;
    }

    //------------//
    // checkStems //
    //------------//
    /**
     * Perform checks on stems.
     *
     * @return the count of modifications done
     */
    private int checkStems ()
    {
        int modifs = 0;
        List<Inter> stems = sig.inters(Shape.STEM);

        for (Iterator<Inter> it = stems.iterator(); it.hasNext();) {
            StemInter stem = (StemInter) it.next();

            if (!stemHasHeadAtStart(stem)) {
                if (stem.isVip() || logger.isDebugEnabled()) {
                    logger.info("Deleting stem lacking starting head {}", stem);
                }

                sig.removeVertex(stem);
                it.remove();
                modifs++;

                continue;
            }

            if (!stemHasSingleHeadEnd(stem)) {
                modifs++;
            }
        }

        return modifs;
    }

    //---------//
    // exclude //
    //---------//
    private void exclude (Set<Inter> set1,
                          Set<Inter> set2)
    {
        for (Inter i1 : set1) {
            for (Inter i2 : set2) {
                sig.insertExclusion(i1, i2, Exclusion.Cause.INCOMPATIBLE);
            }
        }
    }

    //-----------------------//
    // flagHeadInconsistency //
    //-----------------------//
    /**
     * Flag inconsistency of note heads attached to a (good) stem
     */
    private void flagHeadInconsistency ()
    {
        List<Inter> stems = sig.inters(Shape.STEM);

        for (Inter si : stems) {
            if (!si.isGood()) {
                continue;
            }

            Set<Class> classes = new HashSet<Class>();

            for (Relation rel : sig.edgesOf(si)) {
                if (rel instanceof HeadStemRelation) {
                    classes.add(sig.getEdgeSource(rel).getClass());
                }
            }

            if (classes.size() > 1) {
                //logger.info("Several head classes around {} {}", si, classes);
                Map<Class, Set<Inter>> heads = new HashMap<Class, Set<Inter>>();

                for (Relation rel : sig.edgesOf(si)) {
                    if (rel instanceof HeadStemRelation) {
                        Inter head = sig.getEdgeSource(rel);
                        Class classe = head.getClass();
                        Set<Inter> set = heads.get(classe);

                        if (set == null) {
                            heads.put(classe, set = new HashSet<Inter>());
                        }

                        set.add(head);
                    }
                }

                List<Class> clist = new ArrayList<Class>(heads.keySet());

                for (int ic = 0; ic < (clist.size() - 1); ic++) {
                    Class c1 = clist.get(ic);
                    Set set1 = heads.get(c1);

                    for (Class c2 : clist.subList(ic + 1, clist.size())) {
                        Set set2 = heads.get(c2);
                        exclude(set1, set2);
                    }
                }
            }
        }
    }

    //--------------//
    // flagOverlaps //
    //--------------//
    /**
     * (Prototype).
     */
    private void flagOverlaps ()
    {
        // Take all inters except ledgers (and perhaps others, TODO)
        List<Inter> inters = sig.inters(
                new Predicate<Inter>()
                {
                    @Override
                    public boolean check (Inter inter)
                    {
                        return !(inter instanceof LedgerInter);
                    }
                });

        Collections.sort(inters, Inter.byAbscissa);

        for (int i = 0, iBreak = inters.size() - 1; i < iBreak; i++) {
            Inter left = inters.get(i);
            Rectangle leftBox = left.getBounds();
            double xMax = leftBox.getMaxX();

            for (Inter right : inters.subList(i + 1, inters.size())) {
                // Overlap test beam/beam doesn't work (and is useless in fact)
                if (left instanceof AbstractBeamInter && right instanceof AbstractBeamInter) {
                    continue;
                }

                Rectangle rightBox = right.getBounds();

                if (leftBox.intersects(rightBox)) {
                    // Have a more precise look
                    if (left.overlaps(right)) {
                        // If there is no relation between left & right
                        // insert an exclusion
                        Set<Relation> rels1 = sig.getAllEdges(left, right);
                        Set<Relation> rels2 = sig.getAllEdges(right, left);

                        if (rels1.isEmpty() && rels2.isEmpty()) {
                            sig.insertExclusion(left, right, Exclusion.Cause.OVERLAP);
                        }
                    }
                } else if (rightBox.x > xMax) {
                    break;
                }
            }
        }
    }

    //-------------//
    // headHasStem //
    //-------------//
    /**
     * Check if the head has a stem relation.
     *
     * @param inter the head inter (black of void)
     * @return true if OK
     */
    private boolean headHasStem (Inter inter)
    {
        for (Relation rel : sig.edgesOf(inter)) {
            if (rel instanceof HeadStemRelation) {
                return true;
            }
        }

        return false;
    }

    //-------------//
    // hookHasStem //
    //-------------//
    /**
     * Check if a beam hook has a stem.
     */
    private boolean hookHasStem (BeamHookInter hook)
    {
        boolean hasLeft = false;
        boolean hasRight = false;

        if (hook.isVip()) {
            logger.info("VIP hookHasStem for {}", hook);
        }

        for (Relation rel : sig.edgesOf(hook)) {
            if (rel instanceof BeamStemRelation) {
                BeamStemRelation bsRel = (BeamStemRelation) rel;
                BeamPortion portion = bsRel.getBeamPortion();

                if (portion == BeamPortion.LEFT) {
                    hasLeft = true;
                } else if (portion == BeamPortion.RIGHT) {
                    hasRight = true;
                }
            }
        }

        return hasLeft || hasRight;
    }

    //-----------------------//
    // ledgerHasNoteOrLedger //
    //-----------------------//
    /**
     * Check if the provided ledger has either a note centered on it
     * (or one step further) or another ledger right further.
     *
     * @param staff    the containing staff
     * @param index    the ledger line index
     * @param ledger   the ledger to check
     * @param allNotes the abscissa-ordered list of notes in the system
     * @return true if OK
     */
    private boolean ledgerHasNoteOrLedger (StaffInfo staff,
                                           int index,
                                           LedgerInter ledger,
                                           List<Inter> allNotes)
    {
        Rectangle ledgerBox = new Rectangle(ledger.getBounds());
        int interline = system.getSheet().getScale().getInterline();
        ledgerBox.grow(0, interline); // Very high box, but that's OK

        // Check for another ledger on next line
        int nextIndex = index + Integer.signum(index);
        SortedSet<LedgerInter> nextLedgers = staff.getLedgers(nextIndex);

        if (nextLedgers != null) {
            for (LedgerInter nextLedger : nextLedgers) {
                // Check abscissa compatibility
                if (GeoUtil.xOverlap(ledgerBox, nextLedger.getBounds()) > 0) {
                    return true;
                }
            }
        }

        // Else, check for a note centered on ledger, or just on next pitch
        final int ledgerPitch = StaffInfo.getLedgerPitchPosition(index);
        final int nextPitch = ledgerPitch + Integer.signum(index);

        final List<Inter> notes = sig.intersectedInters(allNotes, GeoOrder.BY_ABSCISSA, ledgerBox);

        for (Inter inter : notes) {
            final AbstractNoteInter note = (AbstractNoteInter) inter;
            final int notePitch = note.getPitch();

            if ((notePitch == ledgerPitch) || (notePitch == nextPitch)) {
                return true;
            }
        }

        return false;
    }

    //------------------//
    // lookupExclusions //
    //------------------//
    private int lookupExclusions ()
    {
        // Deletions
        Set<Inter> toRemove = new HashSet<Inter>();

        for (Relation rel : sig.edgeSet()) {
            if (rel instanceof Exclusion) {
                final Inter source = sig.getEdgeSource(rel);
                final double scp = source.getContextualGrade();
                final Inter target = sig.getEdgeTarget(rel);
                final double tcp = target.getContextualGrade();
                Inter weaker = (scp < tcp) ? source : target;

                if (weaker.isVip()) {
                    logger.info("Remaining {} deleting weaker {}", rel.toLongString(sig), weaker);
                }

                toRemove.add(weaker);
            }
        }

        for (Inter inter : toRemove) {
            sig.removeVertex(inter);
        }

        return toRemove.size();
    }

    //-----------------//
    // purgeWeakInters //
    //-----------------//
    private int purgeWeakInters ()
    {
        contextualize();

        return sig.deleteWeakInters().size();
    }

    //---------------//
    // stemDirection //
    //---------------//
    /**
     * Report the direction of the provided stem.
     * <p>
     * For this, we check what is found on each stem end (beam/flag or head)
     * and use contextual grade to choose the best reference.
     *
     * @param stem the stem to check
     * @return -1 for stem up, +1 for stem down, 0 for unknown
     */
    private int stemDirection (StemInter stem)
    {
        double up = 0;
        double down = 0;

        for (Relation rel : sig.edgesOf(stem)) {
            if (rel instanceof HeadStemRelation) {
                HeadStemRelation headStem = (HeadStemRelation) rel;
                StemPortion portion = headStem.getStemPortion();

                if (portion == StemPortion.STEM_BOTTOM) {
                    if (headStem.getHeadSide() == RIGHT) {
                        Inter head = sig.getEdgeSource(rel);
                        up = Math.max(up, head.getContextualGrade());
                    }
                } else if (portion == StemPortion.STEM_TOP) {
                    if (headStem.getHeadSide() == LEFT) {
                        Inter head = sig.getEdgeSource(rel);
                        down = Math.max(down, head.getContextualGrade());
                    }
                }
            } else if (rel instanceof BeamStemRelation) {
                BeamStemRelation beamStem = (BeamStemRelation) rel;
                StemPortion portion = beamStem.getStemPortion();

                if (portion == StemPortion.STEM_BOTTOM) {
                    Inter beam = sig.getEdgeSource(rel);
                    down = Math.max(down, beam.getContextualGrade());
                } else if (portion == StemPortion.STEM_TOP) {
                    Inter beam = sig.getEdgeSource(rel);
                    up = Math.max(up, beam.getContextualGrade());
                }
            }

            //TODO: one day, check for flag?
        }

        return Double.compare(down, up);
    }

    //--------------------//
    // stemHasHeadAtStart //
    //--------------------//
    /**
     * Check if the stem has a head at one (starting) end.
     *
     * @param stem the stem inter
     * @return true if OK
     */
    private boolean stemHasHeadAtStart (StemInter stem)
    {
        for (Relation rel : sig.edgesOf(stem)) {
            if (rel instanceof HeadStemRelation) {
                HeadStemRelation hsRel = (HeadStemRelation) rel;

                // Check stem portion
                if (hsRel.getStemPortion() != StemPortion.STEM_MIDDLE) {
                    return true;
                }
            }
        }

        return false;
    }

    //----------------------//
    // stemHasSingleHeadEnd //
    //----------------------//
    /**
     * Check if the stem does not have one head at each end
     *
     * @param stem the stem inter
     * @return true if OK
     */
    private boolean stemHasSingleHeadEnd (StemInter stem)
    {
        final int dir = stemDirection(stem);

        if (dir == 0) {
            return true; // We cannot decide
        }

        final StemPortion forbidden = (dir > 0) ? STEM_BOTTOM : STEM_TOP;
        final List<Relation> toRemove = new ArrayList<Relation>();

        for (Relation rel : sig.edgesOf(stem)) {
            if (rel instanceof HeadStemRelation) {
                // Check stem portion
                HeadStemRelation hsRel = (HeadStemRelation) rel;
                StemPortion portion = hsRel.getStemPortion();

                if (portion == forbidden) {
                    if (stem.isVip() || logger.isDebugEnabled()) {
                        logger.info("Cutting rel between {} and {}", stem, sig.getEdgeSource(rel));
                    }

                    toRemove.add(rel);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            sig.removeAllEdges(toRemove);
        }

        return toRemove.isEmpty();
    }
}
