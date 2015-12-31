/*
 * OptionsDialogAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import workbench.WbManager;
import workbench.gui.settings.SettingsPanel;
import workbench.resource.ResourceMgr;

/**
 * @author Thomas Kellerer
 */
public class OptionsDialogAction
	extends WbAction
{
	public OptionsDialogAction()
	{
		super();
		initMenuDefinition(ResourceMgr.MNU_TXT_OPTIONS);
		this.removeIcon();
	}
	
	@Override
	public void executeAction(ActionEvent e)
	{
		showOptionsDialog();
	}
	
	public static void showOptionsDialog()
	{
		final JFrame parent = WbManager.getInstance().getCurrentWindow();
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				SettingsPanel panel = new SettingsPanel();
				panel.showSettingsDialog(parent);
			}
		});
	}
}
