package omr.plugin.xenoage;

import java.awt.event.ActionEvent;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import javax.swing.AbstractAction;
import omr.plugin.Plugin;
import omr.plugin.PluginType;
import omr.score.Score;
import omr.score.visitor.ScoreExporter;
import omr.sheet.Sheet;
import omr.sheet.SheetManager;
import omr.ui.icon.IconManager;
import omr.util.Logger;
import com.xenoage.player.ExternalPlayer;

/**
 * MIDI player plugin
 * 
 * @author Brenton Partridge
 * @version $Id$
 */
@Plugin(type=PluginType.SCORE_EXPORT)
public class XenoageAction extends AbstractAction
{
	private static final Logger logger = Logger.getLogger(XenoageAction.class);
	
	public XenoageAction()
	{
		super("Xenoage");
		putValue(SMALL_ICON, IconManager.getInstance().loadImageIcon("media/Play"));
		putValue(SHORT_DESCRIPTION, "Play score as MIDI using Xenoage Player");
		setEnabled(true); // not sheet dependent action
	}
	
	public void actionPerformed(ActionEvent e)
	{
		Sheet sheet = SheetManager.getSelectedSheet();
		if (sheet != null)
		{
			Score score = sheet.getScore();
			if (score != null)
			{
				play(score);
			}			
		}		
	}
	
	public static void play (Score score)
	{
		try {
			// A pair of connected piped streams
			final PipedInputStream pis = new PipedInputStream();
			PipedOutputStream      pos = new PipedOutputStream(pis);
			
			// Launch the reading/playing asynchronously
			final Runnable runnable = new Runnable() {
				public void run ()
				{
					try {
						// Get access to a Xenoage player
						ExternalPlayer player = new ExternalPlayer(null);
						// Read the piped input stream
						player.setMusicXMLData(pis);
						logger.info("Playing...");
						player.play();
					} catch (Exception ex) {
						logger.warning("Player error", ex);
					}
				}
			};
			
			// Start the reader
			Thread t = new Thread(runnable);
			t.start();
			
			// Write the score XML data to the piped output stream
			new ScoreExporter(score, pos);
			
			// Close the stream, to notify the end to the reader
			pos.close();
		} catch (Exception ex) {
			logger.warning("Player error", ex);
		}
	}
}
