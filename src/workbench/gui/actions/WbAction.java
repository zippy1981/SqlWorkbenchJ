package workbench.gui.actions;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbToolbarButton;
import workbench.interfaces.ClipboardSupport;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public abstract class WbAction extends AbstractAction
{
	public static final String ADD_TO_TOOLBAR = "AddToToolbar";
	public static final String MAIN_MENU_ITEM = "MainMenuItem";
	public static final String MENU_SEPARATOR = "MenuSepBefore";
	public static final String TBAR_SEPARATOR = "TbarSepBefore";
	public static final String ALTERNATE_ACCELERATOR = "AlternateAccelerator";
	
	private String actionName;
	protected JMenuItem menuItem;
	protected JButton toolbarButton;

	public WbAction()
	{
		String c = this.getClass().getName();
		this.actionName = c.substring(c.lastIndexOf('.')  + 1);
    this.putValue(ACTION_COMMAND_KEY, this.actionName);
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("blank"));
	}


	public void clearAccelerator()
	{
		this.putValue(Action.ACCELERATOR_KEY, null);
	}

	protected void setActionName(String aName)
	{
		this.actionName = aName;
	}

	public KeyStroke getAlternateAccelerator()
	{
		return (KeyStroke)this.getValue(ALTERNATE_ACCELERATOR);
	}
	
	public KeyStroke getAccelerator()
	{
		return (KeyStroke)this.getValue(Action.ACCELERATOR_KEY);
	}

	public JButton getToolbarButton()
	{
		this.toolbarButton = new WbToolbarButton();
		this.toolbarButton.setAction(this);
		this.toolbarButton.setMnemonic(0);
		/*
		KeyStroke stroke = this.getAccelerator();
		int mod = stroke.getModifiers();
		String delimit = UIManager.getString( "MenuItem.acceleratorDelimiter" );
		String keyTip = KeyEvent.getKeyModifiersText(mod) +
								delimit +
								KeyEvent.getKeyText(stroke.getKeyCode());
		*/
		return this.toolbarButton;
	}

	public void addToToolbar(JToolBar aToolbar)
	{
		aToolbar.add(this.getToolbarButton());
	}

	public void addToMenu(JMenu aMenu)
	{
		aMenu.add(this.getMenuItem());
	}

	public JMenuItem getMenuItem()
	{
		this.menuItem = new WbMenuItem();
		this.menuItem.setMargin(new Insets(0,0,0,0));
		this.menuItem.setAction(this);
		this.menuItem.setAccelerator(this.getAccelerator());
		return this.menuItem;
	}

	public void setCreateToolbarSeparator(boolean aFlag)
	{
		if (aFlag)
		{
			this.putValue(WbAction.TBAR_SEPARATOR, "true");
		}
		else
		{
			putValue(WbAction.TBAR_SEPARATOR, "false");
		}
	}
	public void setCreateMenuSeparator(boolean aFlag)
	{
		if (aFlag)
		{
			this.putValue(WbAction.MENU_SEPARATOR, "true");
		}
		else
		{
			putValue(WbAction.MENU_SEPARATOR, "false");
		}
	}

	public String getActionName()
	{
		return this.actionName;
	}

	public void addToInputMap(InputMap im, ActionMap am)
	{
		im.put(this.getAccelerator(), this.getActionName());
		am.put(this.getActionName(), this);
		
		KeyStroke alternate = this.getAlternateAccelerator();
		if (alternate != null)
		{
			im.put(alternate, this.getActionName());
		}
	}

	public void putValue(String key, Object newValue)
	{
		if (Action.NAME.equals(key) && (newValue instanceof String) && (newValue != null))
		{
			String name = newValue.toString();
			int pos = name.indexOf('&');
			if (pos > -1)
			{
				char mnemonic = name.charAt(pos + 1);
				name = name.substring(0, pos) + name.substring(pos + 1);
				Integer keycode = new Integer((int)mnemonic);
				this.putValue(Action.MNEMONIC_KEY, keycode);
			}
			super.putValue(key, name);
		}
		else
		{
			super.putValue(key, newValue);
		}
	}

}
