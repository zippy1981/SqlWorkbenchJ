/*
 * CompletionPopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

import workbench.interfaces.QuickFilter;
import workbench.log.LogMgr;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.completion.CompletionSearchField;
import workbench.gui.completion.QuickSearchList;
import workbench.gui.components.StringPropertyEditor;
import workbench.gui.components.WbTraversalPolicy;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * @author  Thomas Kellerer
 */
public class TagSearchPopup
	implements FocusListener, MouseListener, KeyListener, WindowListener, QuickSearchList
{
  private CompletionSearchField searchField;
	private JTextComponent inputField;
	private JScrollPane scroll;
	private JList<String> elementList;
	private DefaultListModel<String> data;
  private JWindow window;
  private QuickFilter filter;

	private boolean ignoreSearchChange;

	public TagSearchPopup(JTextComponent input, Set<String> allTags)
  {
    this(input, allTags, null);
  }
	public TagSearchPopup(JTextComponent input, Set<String> allTags, QuickFilter quickFilter)
	{
    inputField = input;
    filter = quickFilter;
		elementList = new JList<>();

    data = new DefaultListModel<>();
    for (String tag : allTags)
    {
      data.addElement(tag);
    }
		elementList.setModel(data);

		elementList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		elementList.addFocusListener(this);
		elementList.addMouseListener(this);
		elementList.addKeyListener(this);
    int count = data.getSize();
    elementList.setVisibleRowCount(count < 12 ? count + 1 : 12);

    JPanel content = new JPanel(new BorderLayout());
    content.setBorder(new EmptyBorder(0,0,0,0));

		searchField = new CompletionSearchField(this, null);
    searchField.addFocusListener(this);

    int height = (int)searchField.getPreferredSize().getHeight();
    int h = (height / 6);
    final Insets insets = new Insets(h, h, h, h);
    JPanel header = new JPanel(new BorderLayout())
    {
      @Override
      public Insets getInsets()
      {
        return insets;
      }
    };

    header.add(searchField, BorderLayout.CENTER);

    scroll = new JScrollPane(this.elementList);
    content.add(scroll, BorderLayout.CENTER);
    scroll.setColumnHeaderView(header);

    window = new JWindow(SwingUtilities.getWindowAncestor(inputField));

    WbTraversalPolicy pol = new WbTraversalPolicy();
    pol.addComponent(searchField);
    pol.addComponent(elementList);
    pol.setDefaultComponent(searchField);

    elementList.setFocusable(true);
    elementList.setFocusTraversalKeysEnabled(false);
    window.setFocusCycleRoot(true);
    window.setFocusTraversalPolicy(pol);

    window.getContentPane().add(content);
    window.addKeyListener(this);
    window.addWindowListener(this);
	}

	public void showPopup()
	{
		try
		{
      final Point p = inputField.getLocationOnScreen();
      p.y += inputField.getHeight();
      Border border = inputField.getBorder();
      if (border != null)
      {
        Insets insets = border.getBorderInsets(inputField);
        p.y -= insets.top;
      }
      Rectangle r = inputField.modelToView(inputField.getCaretPosition());
      p.x += r.x;

			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					if (window != null)
					{
						window.setLocation(p);
						window.pack();
            Dimension size = window.getSize();
            int width = (int)inputField.getWidth() / 4;
            if (width > size.width)
            {
              size.width = width;
              window.setSize(size);
            }
						window.setVisible(true);
            searchField.requestFocusInWindow();
					}
				}
			});
		}
		catch (Exception e)
		{
			LogMgr.logWarning("TagSearchPopup.showPopup()", "Error displaying popup window",e);
		}
	}

	private void dispose()
	{
    data.clear();

    elementList.removeKeyListener(this);
		elementList.removeFocusListener(this);
		elementList.removeMouseListener(this);
    searchField.removeFocusListener(this);

    if (inputField instanceof StringPropertyEditor)
    {
      // make sure the new text is not automatically selected in the profile editor
      ((StringPropertyEditor)inputField).ignoreNextFocus();
    }

    window.removeKeyListener(this);
    window.removeWindowListener(this);
    window.setVisible(false);
    window.dispose();
    window = null;
	}

	/**
	 * Callback from the SearchField when enter has been pressed in the search field
	 */
  @Override
	public void quickSearchValueSelected()
	{
		closePopup(true);
	}

	private void closePopup(boolean doPaste)
	{
		if (this.window == null)
		{
			return;
		}

		try
		{
			if (doPaste)
			{
				doPaste();
			}
		}
		finally
		{
      dispose();
		}
	}

	private void doPaste()
	{
		List<String> selected = this.elementList.getSelectedValuesList();
    if (CollectionUtil.isEmpty(selected))
		{
			return;
		}

		String value = inputField.getText().trim();

		for (String o : selected)
		{
      if (value.length() > 0 && !value.endsWith(","))
      {
        value += ",";
      }
      value += o;
		}

		if (StringUtil.isNonBlank(value))
		{
      inputField.setText(value);
      int len = value.length();
      inputField.setCaretPosition(len);
      inputField.select(len, len);

      if (filter != null)
      {
        EventQueue.invokeLater(filter::applyQuickFilter);
      }
		}
	}

  @Override
	public void selectMatchingEntry(String s)
	{
		if (ignoreSearchChange) return;

		int index = this.findEntry(s);
		if (index >= 0)
		{
			elementList.setSelectedIndex(index);
			elementList.ensureIndexIsVisible(index);
		}
		else
		{
			elementList.clearSelection();
		}
	}

	private int findEntry(String s)
	{
		if (s == null) return -1;
		int count = this.data.getSize();
		if (count == 0) return -1;

    String search = s.toLowerCase();
    for (int i=0; i < count; i++)
    {
      String entry = this.data.getElementAt(i);
      if (entry.toLowerCase().startsWith(search)) return i;
    }
		return -1;
	}

	/**
	 * Implementation of the FocusListener interface
	 */
	@Override
	public void focusGained(FocusEvent focusEvent)
	{
	}

	/**
	 * Implementation of the FocusListener interface
	 */
	@Override
	public void focusLost(FocusEvent focusEvent)
	{
		closePopup(false);
	}

	/**
	 * Implementation of the MouseListener interface
	 */
	@Override
	public void mouseClicked(java.awt.event.MouseEvent mouseEvent)
	{
		int clicks = mouseEvent.getClickCount();
		if (clicks == 2)
		{
			closePopup(true);
		}
	}

	@Override
	public void mouseEntered(MouseEvent mouseEvent)
	{
	}

	@Override
	public void mouseExited(MouseEvent mouseEvent)
	{
	}

	@Override
	public void mousePressed(MouseEvent mouseEvent)
	{
	}

	@Override
	public void mouseReleased(MouseEvent mouseEvent)
	{
	}

	@Override
	public void keyPressed(KeyEvent evt)
	{
		int currentIndex = elementList.getSelectedIndex();
		int nextIndex = -1;
		boolean shiftPressed = WbAction.isShiftPressed(evt.getModifiers());
    boolean syncEntry = false;

		switch (evt.getKeyCode())
		{
			case KeyEvent.VK_TAB:
				evt.consume();
				break;
			case KeyEvent.VK_ENTER:
				closePopup(true);
				evt.consume();
				break;
			case KeyEvent.VK_ESCAPE:
				closePopup(false);
				evt.consume();
				break;

			case KeyEvent.VK_UP:
				if (currentIndex > 0)
				{
					nextIndex = currentIndex - 1;
				}

				if (nextIndex != -1)
				{
					if (shiftPressed)
					{
						elementList.addSelectionInterval(nextIndex,nextIndex);
					}
					else
					{
						elementList.setSelectedIndex(nextIndex);
					}
					elementList.ensureIndexIsVisible(nextIndex);
          syncEntry = true;
					evt.consume();
				}
				break;

			case KeyEvent.VK_DOWN:
				// when moving down while extending the selection, we need to use the last selected entry
				// as the "base" index (not necessary when moving up, as getSelectedIndex() will return the first entry)
				if (shiftPressed)
				{
					int[] selected = elementList.getSelectedIndices();
					if (selected.length > 1)
					{
						currentIndex = selected[selected.length - 1];
					}
				}

				if (currentIndex < data.getSize() - 1)
				{
					nextIndex = currentIndex + 1;
				}

				if (nextIndex != -1)
				{
					if (shiftPressed)
					{
						elementList.addSelectionInterval(nextIndex,nextIndex);
					}
					else
					{
						elementList.setSelectedIndex(nextIndex);
					}
					elementList.ensureIndexIsVisible(nextIndex);
          syncEntry = true;
					evt.consume();
				}
				break;
		}

		if (syncEntry && searchField != null)
		{
			try
			{
				ignoreSearchChange = true;
				String o = elementList.getSelectedValue();
				if (o != null)
				{
					this.searchField.setText(o);
					this.searchField.selectAll();
				}
			}
			finally
			{
				ignoreSearchChange = false;
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent evt)
	{
	}

	@Override
	public void keyReleased(KeyEvent keyEvent)
	{
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
		dispose();
	}

	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

  @Override
  public void closeQuickSearch()
  {
    // This is called from the search field when the user hits the ESC key
    closePopup(false);
  }

}