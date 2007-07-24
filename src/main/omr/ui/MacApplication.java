package omr.ui;

import javax.swing.Action;
import com.apple.eawt.*;

public class MacApplication extends ApplicationAdapter
{
	private Action about, prefs, exit;
	
	public MacApplication(Action about, Action prefs, Action exit)
	{
		this.about = about;
		this.prefs = prefs;
		this.exit = exit;
		
		Application app = Application.getApplication();
		app.setEnabledAboutMenu(true);
		app.setEnabledPreferencesMenu(true);
		app.addApplicationListener(this);
	}
	
	@Override
	public void handleAbout(ApplicationEvent event)
	{
		about.actionPerformed(null);
	}
	
	@Override
	public void handlePreferences(ApplicationEvent event)
	{
		prefs.actionPerformed(null);
	}
	
	@Override
	public void handleQuit(ApplicationEvent event)
	{
		exit.actionPerformed(null);
	}
}
