//----------------------------------------------------------------------------//
//                                                                            //
//                       S c o r e D i m e n s i o n s                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
import omr.score.Score;
import omr.score.entity.Arpeggiate;
import omr.score.entity.Articulation;
import omr.score.entity.Barline;
import omr.score.entity.Beam;
import omr.score.entity.Chord;
import omr.score.entity.Clef;
import omr.score.entity.Coda;
import omr.score.entity.DirectionStatement;
import omr.score.entity.Dynamics;
import omr.score.entity.Fermata;
import omr.score.entity.KeySignature;
import omr.score.entity.Measure;
import omr.score.entity.MeasureElement;
import omr.score.entity.MeasureNode;
import omr.score.entity.Note;
import omr.score.entity.Ornament;
import omr.score.entity.PartNode;
import omr.score.entity.Pedal;
import omr.score.entity.ScoreNode;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Segno;
import omr.score.entity.Slur;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.entity.Text;
import omr.score.entity.TimeSignature;
import omr.score.entity.Tuplet;
import omr.score.entity.VisitableNode;
import omr.score.entity.Wedge;
import omr.score.visitor.AbstractScoreVisitor;

/**
 * Class <code>AbstractScoreVisitor</code> provides a default implementation of
 * the ScoreVisitor interface, where by default all visit() methods are void and
 * return true (to allow automatic visit of the children of each node).
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreDimensions
    extends AbstractScoreVisitor
{
    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // ScoreDimensions //
    //-----------------//
    /**
     * Creates a new ScoreDimensions object.
     */
    public ScoreDimensions ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // visit Articulation //
    //--------------------//
    @Override
    public boolean visit (Articulation articulation)
    {
        articulation.getBox();
        articulation.getCenter();
        articulation.getReferencePoint();

        return true;
    }

    //------------------//
    // visit Arpeggiate //
    //------------------//
    @Override
    public boolean visit (Arpeggiate arpeggiate)
    {
        arpeggiate.getBox();
        arpeggiate.getCenter();
        arpeggiate.getReferencePoint();

        return true;
    }

    //---------------//
    // visit Barline //
    //---------------//
    @Override
    public boolean visit (Barline barline)
    {
        barline.getBox();
        barline.getCenter();
        barline.getReferencePoint();

        return true;
    }

    //------------//
    // visit Beam //
    //------------//
    @Override
    public boolean visit (Beam beam)
    {
        beam.getBox();
        beam.getCenter();
        beam.getReferencePoint();

        return true;
    }

    //-------------//
    // visit Chord //
    //-------------//
    @Override
    public boolean visit (Chord chord)
    {
        chord.getBox();
        chord.getCenter();
        chord.getReferencePoint();

        return true;
    }

    //------------//
    // visit Clef //
    //------------//
    @Override
    public boolean visit (Clef clef)
    {
        clef.getBox();
        clef.getCenter();
        clef.getReferencePoint();

        return true;
    }

    //------------//
    // visit Coda //
    //------------//
    @Override
    public boolean visit (Coda coda)
    {
        coda.getBox();
        coda.getCenter();
        coda.getReferencePoint();

        return true;
    }

    //--------------------------//
    // visit DirectionStatement //
    //--------------------------//
    @Override
    public boolean visit (DirectionStatement words)
    {
        words.getBox();
        words.getCenter();
        words.getReferencePoint();

        return true;
    }

    //----------------//
    // visit Dynamics //
    //----------------//
    @Override
    public boolean visit (Dynamics dynamics)
    {
        dynamics.getBox();
        dynamics.getCenter();
        dynamics.getReferencePoint();

        return true;
    }

    //---------------//
    // visit Fermata //
    //---------------//
    @Override
    public boolean visit (Fermata fermata)
    {
        fermata.getBox();
        fermata.getCenter();
        fermata.getReferencePoint();

        return true;
    }

    //--------------------//
    // visit KeySignature //
    //--------------------//
    @Override
    public boolean visit (KeySignature keySignature)
    {
        keySignature.getBox();
        keySignature.getCenter();
        keySignature.getReferencePoint();

        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        measure.getBox();
        measure.getCenter();
        measure.getReferencePoint();

        return true;
    }

    //----------------------//
    // visit MeasureElement //
    //----------------------//
    @Override
    public boolean visit (MeasureElement measureElement)
    {
        measureElement.getBox();
        measureElement.getCenter();
        measureElement.getReferencePoint();

        return true;
    }

    //-------------------//
    // visit MeasureNode //
    //-------------------//
    @Override
    public boolean visit (MeasureNode measureNode)
    {
        measureNode.getBox();
        measureNode.getCenter();
        measureNode.getReferencePoint();

        return true;
    }

    //------------//
    // visit Note //
    //------------//
    @Override
    public boolean visit (Note note)
    {
        note.getBox();
        note.getCenter();
        note.getReferencePoint();

        return true;
    }

    //----------------//
    // visit Ornament //
    //----------------//
    @Override
    public boolean visit (Ornament ornament)
    {
        ornament.getBox();
        ornament.getCenter();
        ornament.getReferencePoint();

        return true;
    }

    //----------------//
    // visit PartNode //
    //----------------//
    @Override
    public boolean visit (PartNode partNode)
    {
        partNode.getBox();
        partNode.getCenter();
        partNode.getReferencePoint();

        return true;
    }

    //-------------//
    // visit Pedal //
    //-------------//
    @Override
    public boolean visit (Pedal pedal)
    {
        pedal.getBox();
        pedal.getCenter();
        pedal.getReferencePoint();

        return true;
    }

    //-----------------//
    // visit ScoreNode //
    //-----------------//
    @Override
    public boolean visit (ScoreNode scoreNode)
    {
        //        scoreNode.getBox();
        //        scoreNode.getCenter();
        //        scoreNode.getReferencePoint();
        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        //        score.getBox();
        //        score.getCenter();
        //        score.getReferencePoint();
        score.acceptChildren(this);

        return true;
    }

    //-------------//
    // visit Segno //
    //-------------//
    @Override
    public boolean visit (Segno segno)
    {
        segno.getBox();
        segno.getCenter();
        segno.getReferencePoint();

        return true;
    }

    //------------//
    // visit Slur //
    //------------//
    @Override
    public boolean visit (Slur slur)
    {
        slur.getBox();
        slur.getCenter();
        slur.getReferencePoint();

        return true;
    }

    //-------------//
    // visit Staff //
    //-------------//
    @Override
    public boolean visit (Staff staff)
    {
        staff.getBox();
        staff.getCenter();
        staff.getReferencePoint();

        return true;
    }

    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        system.getBox();
        system.getCenter();

        //        system.getReferencePoint();
        return true;
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart systemPart)
    {
        systemPart.getBox();
        systemPart.getCenter();
        systemPart.getReferencePoint();

        return true;
    }

    //------------//
    // visit Text //
    //------------//
    @Override
    public boolean visit (Text text)
    {
        text.getBox();
        text.getCenter();
        text.getReferencePoint();

        return true;
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    @Override
    public boolean visit (TimeSignature timeSignature)
    {
        timeSignature.getBox();
        timeSignature.getCenter();
        timeSignature.getReferencePoint();

        return true;
    }

    //--------------//
    // visit Tuplet //
    //--------------//
    @Override
    public boolean visit (Tuplet tuplet)
    {
        tuplet.getBox();
        tuplet.getCenter();
        tuplet.getReferencePoint();

        return true;
    }

    //---------------------//
    // visit VisitableNode //
    //---------------------//
    @Override
    public boolean visit (VisitableNode node)
    {
        //        node.getBox();
        //        node.getCenter();
        //        node.getReferencePoint();
        return true;
    }

    //-------------//
    // visit Wedge //
    //-------------//
    @Override
    public boolean visit (Wedge wedge)
    {
        wedge.getBox();
        wedge.getCenter();
        wedge.getReferencePoint();

        return true;
    }
}
