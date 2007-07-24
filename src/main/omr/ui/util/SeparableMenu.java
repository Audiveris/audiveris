/**
 * 
 */
package omr.ui.util;

import javax.swing.*;

public class SeparableMenu extends JMenu
{
	public SeparableMenu()
	{
		super();
	}

	public SeparableMenu(Action action)
	{
		super(action);
	}

	public SeparableMenu(String s, boolean flag)
	{
		super(s, flag);
	}

	public SeparableMenu(String s)
	{
		super(s);
	}

	@Override
	public void addSeparator()
	{
		int count = super.getMenuComponentCount();
		if (count > 0 && !(super.getMenuComponent(count - 1) instanceof JSeparator))
			super.addSeparator();
	}
}