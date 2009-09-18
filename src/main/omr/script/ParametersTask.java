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

import omr.sheet.Sheet;

import omr.step.Step;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * Class <code>ParametersTask</code> is a script task which handles the global
 * parameters of a score, together with {@link omr.score.ui.ScoreBoard}
 *
 * @see omr.score.ui.ScoreBoard
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ParametersTask
    extends ScriptTask
{
    //~ Instance fields --------------------------------------------------------

    /** MIDI volume */
    @XmlAttribute(name = "volume")
    private int volume;

    /** MIDI tempo */
    @XmlAttribute(name = "tempo")
    private int tempo;

    /** Language code */
    @XmlAttribute(name = "language")
    private String language;

    /** Description data for each part */
    @XmlElement(name = "part")
    private List<PartData> parts = new ArrayList<PartData>();

    /** Remember if we have changed the language */
    private boolean languageChanged;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // ParametersTask //
    //----------------//
    /** No-arg constructor needed by JAXB */
    public ParametersTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------

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
        Score score = sheet.getScore();

        // Language
        if (!language.equals(score.getLanguage())) {
            languageChanged = true;
            score.setLanguage(language);
        }

        // Midi tempo & volume
        score.setTempo(tempo);
        score.setVolume(volume);

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

        // Update the following steps on the impacted systems
        // Only if we use a new language
        if (languageChanged) {
            score.getSheet()
                 .getSheetSteps()
                 .rebuildAfter(Step.VERTICALS, null, true);
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

        if (language != null) {
            sb.append(" language:")
              .append(language);
        }

        sb.append(" tempo:")
          .append(tempo);
        sb.append(" volume:")
          .append(volume);

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
