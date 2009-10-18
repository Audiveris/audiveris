//----------------------------------------------------------------------------//
//                                                                            //
//                        P a r a m e t e r s T a s k                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.score.Score;
import omr.score.entity.ScorePart;

import omr.sheet.Scale.InterlineFraction;
import omr.sheet.Sheet;
import omr.sheet.picture.Picture;

import omr.step.Step;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * Class <code>ParametersTask</code> is a script task which handles the global
 * parameters of a score, together with {@link omr.score.ui.ScoreParameters}
 *
 * @see omr.score.ui.ScoreParameters
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ParametersTask
    extends ScriptTask
{
    //~ Instance fields --------------------------------------------------------

    /** Max foreground value */
    @XmlAttribute(name = "foreground")
    private Integer foreground;

    /** Histogram percentage for staves */
    @XmlAttribute(name = "histo-ratio")
    private Double histoRatio;

    /** Language code */
    @XmlAttribute(name = "language")
    private String language;

    /** Slot horizontal margin */
    @XmlElement(name = "slot-margin")
    private InterlineFraction slotMargin;

    /** MIDI volume */
    @XmlAttribute(name = "volume")
    private Integer volume;

    /** MIDI tempo */
    @XmlAttribute(name = "tempo")
    private Integer tempo;

    /** Description data for each part */
    @XmlElement(name = "part")
    private List<PartData> parts = new ArrayList<PartData>();

    /** Remember if we have changed these items */
    private boolean foregroundChanged;
    private boolean languageChanged;
    private boolean histoRatioChanged;
    private boolean slotMarginChanged;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // ParametersTask //
    //----------------//
    /** No-arg constructor needed by JAXB */
    public ParametersTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------

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

    //---------------//
    // setHistoRatio //
    //---------------//
    /**
     * @param ratio the percentage of histogram for staves
     */
    public void setHistoRatio (double ratio)
    {
        histoRatio = ratio;
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
        this.slotMargin = new InterlineFraction(slotMargin);
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
        Picture       picture = sheet.getPicture();
        StringBuilder sb = new StringBuilder();

        // Foreground
        if (foreground != null) {
            if (!picture.hasMaxForeground() ||
                !foreground.equals(picture.getMaxForeground())) {
                picture.setMaxForeground(foreground);
                sb.append(" foreground:")
                  .append(foreground);
                foregroundChanged = true;
            }
        }

        // Histo Frac
        if (histoRatio != null) {
            if (!sheet.hasHistoRatio() ||
                (!histoRatio.equals(sheet.getHistoRatio()))) {
                sheet.setHistoRatio(histoRatio);
                sb.append(" histoRatio:")
                  .append(histoRatio);
                histoRatioChanged = true;
            }
        }

        // Slot margin
        if (slotMargin != null) {
            if (!score.hasSlotMargin() ||
                !slotMargin.equals(score.getSlotMargin())) {
                score.setSlotMargin(slotMargin);
                sb.append(" slotMargin:")
                  .append(slotMargin);
                slotMarginChanged = true;
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
            logger.info("Parameters" + sb);
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
    @Override
    public void epilog (Sheet sheet)
    {
        Score score = sheet.getScore();

        Step  from = null;

        if (slotMarginChanged) {
            from = Step.SCORE;
        }

        if (languageChanged) {
            from = Step.PATTERNS;
        }

        if (histoRatioChanged) {
            from = Step.LINES;
        }

        if (foregroundChanged) {
            from = Step.LOAD;
        }

        if (from != null) {
            logger.info("Rebuilding from " + from);
            score.getSheet()
                 .getSheetSteps()
                 .rebuildFrom(from, null, true);
        }

        super.epilog(sheet);
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

        if (histoRatio != null) {
            sb.append(" histoRatio:")
              .append(histoRatio);
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
