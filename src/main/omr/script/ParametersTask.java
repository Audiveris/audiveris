//----------------------------------------------------------------------------//
//                                                                            //
//                        P a r a m e t e r s T a s k                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.score.Score;
import omr.score.entity.ScorePart;
import omr.score.entity.SlotPolicy;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.step.Step;
import omr.step.Stepping;
import omr.step.Steps;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Class {@code ParametersTask} handles the global parameters of a score,
 * together with {@link omr.score.ui.ScoreParameters}
 *
 * @see omr.score.ui.ScoreParameters
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ParametersTask
    extends ScriptTask
{
    //~ Instance fields --------------------------------------------------------

    /** Max foreground value */
    @XmlAttribute(name = "foreground")
    private Integer foreground;

    /** Language code */
    @XmlAttribute(name = "language")
    private String language;

    /** Slot horizontal margin */
    @XmlAttribute(name = "slot-margin")
    private Double slotMargin;

    /** Slot policy */
    @XmlAttribute(name = "slot-policy")
    private SlotPolicy slotPolicy;

    /** MIDI volume */
    @XmlAttribute(name = "volume")
    private Integer volume;

    /** MIDI tempo */
    @XmlAttribute(name = "tempo")
    private Integer tempo;

    /** Description data for each part */
    @XmlElement(name = "part")
    private List<PartData> parts = new ArrayList<>();

    /** Remember if we have changed these items */
    private boolean foregroundChanged;
    private boolean languageChanged;
    private boolean slotChanged;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // ParametersTask //
    //----------------//
    /** No-arg constructor needed by JAXB */
    public ParametersTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // addPart //
    //---------//
    /**
     * Add data for one part
     * @param name the part name
     * @param program the midi program
     */
    public void addPart (String name,
                         int    program)
    {
        parts.add(new PartData(name, program));
    }

    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
        throws Exception
    {
        Score         score = sheet.getScore();
        StringBuilder sb = new StringBuilder();

        // Foreground
        if (foreground != null) {
            if (!sheet.hasMaxForeground() ||
                !foreground.equals(sheet.getMaxForeground())) {
                sheet.setMaxForeground(foreground);
                sb.append(" foreground:")
                  .append(foreground);
                foregroundChanged = true;
            }
        }

        // Slot policy
        if (slotPolicy != null) {
            if (!score.hasSlotPolicy() ||
                !slotPolicy.equals(score.getSlotPolicy())) {
                score.setSlotPolicy(slotPolicy);
                sb.append(" slotPolicy:")
                  .append(slotPolicy);
                slotChanged = true;
            }
        }

        // Slot margin
        if (slotMargin != null) {
            if (!score.hasSlotMargin() ||
                !slotMargin.equals(score.getSlotMargin())) {
                score.setSlotMargin(slotMargin);
                sb.append(" slotMargin:")
                  .append(slotMargin);
                slotChanged = true;
            }
        }

        // Language
        if (language != null) {
            if (!score.hasLanguage() || !language.equals(score.getLanguage())) {
                score.setLanguage(language);
                sb.append(" language:")
                  .append(language);
                languageChanged = true;
            }
        }

        // Midi tempo
        if (tempo != null) {
            if (!score.hasTempo() || !tempo.equals(score.getTempo())) {
                score.setTempo(tempo);
                sb.append(" tempo:")
                  .append(tempo);
            }
        }

        // Midi volume
        if (volume != null) {
            if (!score.hasVolume() || !volume.equals(score.getVolume())) {
                score.setVolume(volume);
                sb.append(" volume:")
                  .append(volume);
            }
        }

        if (sb.length() > 0) {
            logger.info("Parameters{0}", sb);
        }

        // Parts
        for (int i = 0; i < parts.size(); i++) {
            try {
                ScorePart scorePart = score.getPartList()
                                           .get(i);
                PartData  data = parts.get(i);

                // Part name
                scorePart.setName(data.name);

                // Part midi program
                scorePart.setMidiProgram(data.program);
            } catch (Exception ex) {
                logger.warning(
                    "Error in script Parameters part#" + (i + 1),
                    ex);
            }
        }
    }

    //--------//
    // epilog //
    //--------//
    /**
     * Determine from which step we should rebuild the current score
     * @param sheet the related sheet
     */
    @Override
    public void epilog (Sheet sheet)
    {
        Step latestStep = Stepping.getLatestStep(sheet);
        Step from = null;

        if (slotChanged) {
            from = Steps.valueOf(Steps.PAGES);
        }

        if (languageChanged) {
            from = Steps.valueOf(Steps.SYMBOLS);
        }

        Step loadStep = Steps.valueOf(Steps.LOAD);

        if (foregroundChanged) {
            if (Steps.compare(latestStep, loadStep) > 0) {
                from = loadStep;
            } else {
                from = null;
            }
        }

        if ((from != null) && Stepping.shouldReprocessSheet(from, sheet)) {
            logger.info("Rebuilding from {0}", from);
            Stepping.reprocessSheet(from, sheet, null, true);
        }

        super.epilog(sheet);
    }

    //---------------//
    // setForeground //
    //---------------//
    /**
     * @param foreground the maximum pixel value for foreground
     */
    public void setForeground (int foreground)
    {
        this.foreground = foreground;
    }

    //-------------//
    // setLanguage //
    //-------------//
    /**
     * @param language the language code to set
     */
    public void setLanguage (String language)
    {
        this.language = language;
    }

    //---------------//
    // setSlotMargin //
    //---------------//
    /**
     * @param slotMargin the new Slot horizontal margin
     */
    public void setSlotMargin (double slotMargin)
    {
        this.slotMargin = slotMargin;
    }

    //---------------//
    // setSlotPolicy //
    //---------------//
    /**
     * @param slotPolicy the new Slot Policy
     */
    public void setSlotPolicy (SlotPolicy slotPolicy)
    {
        this.slotPolicy = slotPolicy;
    }

    //----------//
    // setTempo //
    //----------//
    /**
     * @param tempo the tempo to set
     */
    public void setTempo (int tempo)
    {
        this.tempo = tempo;
    }

    //-----------//
    // setVolume //
    //-----------//
    /**
     * @param volume the volume to set
     */
    public void setVolume (int volume)
    {
        this.volume = volume;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(" parameters");

        if (foreground != null) {
            sb.append(" foreground:")
              .append(foreground);
        }

        if (slotPolicy != null) {
            sb.append(" slotPolicy:")
              .append(slotPolicy);
        }

        if (slotMargin != null) {
            sb.append(" slotMargin:")
              .append(slotMargin);
        }

        if (language != null) {
            sb.append(" language:")
              .append(language);
        }

        if (tempo != null) {
            sb.append(" tempo:")
              .append(tempo);
        }

        if (volume != null) {
            sb.append(" volume:")
              .append(volume);
        }

        for (PartData data : parts) {
            sb.append(" ")
              .append(data);
        }

        return sb.toString() + super.internalsString();
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------//
    // PartData //
    //----------//
    private static class PartData
    {
        //~ Instance fields ----------------------------------------------------

        /** Midi Instrument */
        @XmlAttribute
        private final int program;

        /** Name of the part */
        @XmlAttribute
        private final String name;

        //~ Constructors -------------------------------------------------------

        public PartData (String name,
                         int    program)
        {
            this.name = name;
            this.program = program;
        }

        private PartData ()
        {
            name = null;
            program = 0;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            return "{name:" + name + " program:" + program + "}";
        }
    }
}
