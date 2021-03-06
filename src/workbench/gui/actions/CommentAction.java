/*
 * ToggleCommentAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.awt.event.ActionEvent;

import workbench.resource.ResourceMgr;

import workbench.gui.editor.TextCommenter;
import workbench.gui.sql.EditorPanel;

/**
 * Action to toggle the "comment" for the currently selected text in the SQL editor.
 *
 * This is done by adding or removing single line comments to each line.
 *
 * @see workbench.gui.editor.TextCommenter#toggleComment()
 *
 * @author  Thomas Kellerer
 */
public class CommentAction
	extends WbAction
{
	private EditorPanel client;

	public CommentAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCommentSelection");
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		TextCommenter commenter = new TextCommenter(client);
		commenter.commentSelection();
	}
}
