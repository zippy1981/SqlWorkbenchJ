/*
 * ReplacePanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import workbench.gui.WbSwingUtilities;

import workbench.gui.actions.EscAction;
import workbench.interfaces.Replaceable;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ReplacePanel
	extends javax.swing.JPanel
	implements ActionListener, WindowListener
{
	private static final String PROP_CLASS = "workbench.sql.replace";
	private static final String PROP_KEY_CASE = PROP_CLASS + ".ignoreCase";
	private static final String PROP_KEY_WHOLE_WORD = PROP_CLASS + ".wholeWord";
	private static final String PROP_KEY_SELECTED = PROP_CLASS + ".selectedText";
	private static final String PROP_KEY_USE_REGEX = PROP_CLASS + ".useRegEx";
	private static final String PROP_KEY_CRITERIA = PROP_CLASS + ".criteria";
	private static final String PROP_KEY_REPLACEMENT = PROP_CLASS + ".replacement";

	private Replaceable client;
	private int lastPos = -1;
	private JDialog dialog;
	private EscAction escAction;

	/** Creates new form ReplacePanel */
	public ReplacePanel(Replaceable aClient)
	{
		initComponents();
		this.client = aClient;
		WbTraversalPolicy policy = new WbTraversalPolicy();
		policy.addComponent(criteriaTextField);
		policy.addComponent(this.replaceValueTextField);
		policy.addComponent(this.ignoreCaseCheckBox);
		policy.addComponent(this.wordsOnlyCheckBox);
		policy.addComponent(this.selectedTextCheckBox);
		policy.addComponent(this.findButton);
		policy.addComponent(this.replaceNextButton);
		policy.addComponent(this.replaceAllButton);
		policy.addComponent(this.closeButton);
		policy.setDefaultComponent(criteriaTextField);
		this.setFocusCycleRoot(true);
		this.setFocusTraversalPolicy(policy);

		this.findButton.addActionListener(this);
		this.replaceNextButton.addActionListener(this);
		this.replaceAllButton.addActionListener(this);
		this.closeButton.addActionListener(this);

		this.replaceNextButton.setEnabled(false);

		this.criteriaTextField.addMouseListener(new TextComponentMouseListener());
		this.replaceValueTextField.addMouseListener(new TextComponentMouseListener());
		this.restoreSettings();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    java.awt.GridBagConstraints gridBagConstraints;

    criteriaLabel = new javax.swing.JLabel();
    criteriaTextField = new javax.swing.JTextField();
    replaceValueTextField = new javax.swing.JTextField();
    ignoreCaseCheckBox = new javax.swing.JCheckBox();
    wordsOnlyCheckBox = new javax.swing.JCheckBox();
    replaceLabel = new javax.swing.JLabel();
    spacerPanel = new javax.swing.JPanel();
    findButton = new WbButton();
    replaceNextButton = new WbButton();
    replaceAllButton = new WbButton();
    closeButton = new WbButton();
    selectedTextCheckBox = new javax.swing.JCheckBox();
    useRegexCheckBox = new javax.swing.JCheckBox();

    setLayout(new java.awt.GridBagLayout());

    criteriaLabel.setLabelFor(criteriaTextField);
    criteriaLabel.setText(ResourceMgr.getString("LblSearchCriteria"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
    add(criteriaLabel, gridBagConstraints);

    criteriaTextField.setColumns(30);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 4, 0, 5);
    add(criteriaTextField, gridBagConstraints);

    replaceValueTextField.setColumns(30);
    replaceValueTextField.addFocusListener(new java.awt.event.FocusAdapter()
    {
      public void focusGained(java.awt.event.FocusEvent evt)
      {
        replaceValueTextFieldFocusGained(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 5);
    add(replaceValueTextField, gridBagConstraints);

    ignoreCaseCheckBox.setText(ResourceMgr.getString("LblSearchIgnoreCase"));
    ignoreCaseCheckBox.setToolTipText(ResourceMgr.getDescription("LblSearchIgnoreCase"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
    add(ignoreCaseCheckBox, gridBagConstraints);

    wordsOnlyCheckBox.setText(ResourceMgr.getString("LblSearchWordsOnly"));
    wordsOnlyCheckBox.setToolTipText(ResourceMgr.getDescription("LblSearchWordsOnly"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
    add(wordsOnlyCheckBox, gridBagConstraints);

    replaceLabel.setLabelFor(replaceValueTextField);
    replaceLabel.setText(ResourceMgr.getString("LblReplaceNewValue"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    add(replaceLabel, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.weighty = 1.0;
    add(spacerPanel, gridBagConstraints);

    findButton.setText(ResourceMgr.getString("LblFindNow"));
    findButton.setToolTipText(ResourceMgr.getDescription("LblFindNow"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 5);
    add(findButton, gridBagConstraints);

    replaceNextButton.setText(ResourceMgr.getString("LblReplaceNext"));
    replaceNextButton.setToolTipText(ResourceMgr.getDescription("LblReplaceNext"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
    add(replaceNextButton, gridBagConstraints);

    replaceAllButton.setText(ResourceMgr.getString("LblReplaceAll"));
    replaceAllButton.setToolTipText(ResourceMgr.getDescription("LblReplaceAll"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
    add(replaceAllButton, gridBagConstraints);

    closeButton.setText(ResourceMgr.getString("LblClose"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
    add(closeButton, gridBagConstraints);

    selectedTextCheckBox.setText(ResourceMgr.getString("LblSelectedTextOnly"));
    selectedTextCheckBox.setToolTipText(ResourceMgr.getDescription("LblSelectedTextOnly"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
    add(selectedTextCheckBox, gridBagConstraints);

    useRegexCheckBox.setText(ResourceMgr.getString("LblSearchRegEx"));
    useRegexCheckBox.setToolTipText(ResourceMgr.getDescription("LblSearchRegEx"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
    add(useRegexCheckBox, gridBagConstraints);

  }// </editor-fold>//GEN-END:initComponents

	private void replaceValueTextFieldFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_replaceValueTextFieldFocusGained
	{//GEN-HEADEREND:event_replaceValueTextFieldFocusGained
		// When the popup menu for copy & paste is used, the oppositeComponent()
		// is the RootPane. In this case we don't want to chage the selection
		if (!(evt.getOppositeComponent() instanceof javax.swing.JRootPane))
		{
			this.replaceValueTextField.selectAll();
		}
	}//GEN-LAST:event_replaceValueTextFieldFocusGained

	public void showReplaceDialog(Component caller, final String selectedText)
	{
		if (this.dialog != null)
		{
			this.dialog.setVisible(true);
			this.dialog.requestFocus();
			return;
		}
		try
		{
			Window w = SwingUtilities.getWindowAncestor(caller);
			Frame f = null;
			if (w instanceof Frame)
			{
				f = (Frame)w;
			}
			this.dialog = new JDialog(f);
			this.dialog.setTitle(ResourceMgr.getString("TxtWindowTitleReplaceText"));
			this.dialog.getContentPane().add(this);
			this.dialog.pack();
			this.dialog.setResizable(false);
			if (!Settings.getInstance().restoreWindowPosition(this.dialog, PROP_CLASS + ".window"))
			{
				this.dialog.setLocationRelativeTo(caller);
			}
			this.dialog.addWindowListener(this);

			boolean hasSelectedText = false;

			if (!StringUtil.isEmptyString(selectedText) && selectedText.indexOf('\n') == -1 && selectedText.indexOf('\r') == -1)
			{
				criteriaTextField.setText(selectedText);
				hasSelectedText = true;
			}

			InputMap im = this.dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
			ActionMap am = this.dialog.getRootPane().getActionMap();

			escAction = new EscAction(this);
			im.put(escAction.getAccelerator(), escAction.getActionName());
			am.put(escAction.getActionName(), escAction);

			final boolean criteriaAdded = hasSelectedText;

			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					if (criteriaAdded)
					{
						replaceValueTextField.selectAll();
						replaceValueTextField.requestFocus();
					}
					else
					{
						criteriaTextField.selectAll();
						criteriaTextField.requestFocus();
					}
				}
			});
			this.dialog.setVisible(true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		Object source = e.getSource();
		if (source == this.findButton)
		{
			this.find();
		}
		else if (source == this.replaceNextButton)
		{
			this.replaceNext();
		}
		else if (source == this.replaceAllButton)
		{
			this.replaceAll();
		}
		else if (source == this.closeButton || e.getActionCommand().equals(escAction.getActionName()))
		{
			this.closeWindow();
		}
	}

	private void find()
	{
		String toFind = this.criteriaTextField.getText();
		try
		{
			this.lastPos = this.client.findFirst(toFind, this.ignoreCaseCheckBox.isSelected(), this.wordsOnlyCheckBox.isSelected(), this.useRegexCheckBox.isSelected());
			this.replaceNextButton.setEnabled(this.lastPos > 0);
		}
		catch (Exception e)
		{
			WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(e));
		}
	}

	private void replaceNext()
	{
		if (this.lastPos < 0) this.find();

		if (this.client.replaceCurrent(this.replaceValueTextField.getText()))
		//if (this.client.replaceNext(this.replaceValueTextField.getText()))
		{
			this.find();
		}
		else
		{
			this.replaceNextButton.setEnabled(false);
		}
	}

	private void replaceAll()
	{
		//this.find();
		//if (this.lastPos < 0) return;
		boolean selected = this.selectedTextCheckBox.isEnabled() && this.selectedTextCheckBox.isSelected();
		this.client.replaceAll(this.criteriaTextField.getText(),
		                       this.replaceValueTextField.getText(),
													 selected,
		                       this.ignoreCaseCheckBox.isSelected(),
													 this.wordsOnlyCheckBox.isSelected(),
													 this.useRegexCheckBox.isSelected());
	}

	private void closeWindow()
	{
		if (this.dialog != null)
		{
			this.saveSettings();
			this.escAction = null;
			this.dialog.setVisible(false);
			this.dialog.dispose();
			this.dialog = null;
		}
	}

	private void saveSettings()
	{
		Settings.getInstance().setProperty(PROP_KEY_CASE, Boolean.toString(this.ignoreCaseCheckBox.isSelected()));
		Settings.getInstance().setProperty(PROP_KEY_WHOLE_WORD, Boolean.toString(this.wordsOnlyCheckBox.isSelected()));
		Settings.getInstance().setProperty(PROP_KEY_SELECTED, Boolean.toString(this.selectedTextCheckBox.isSelected()));
		Settings.getInstance().setProperty(PROP_KEY_USE_REGEX, Boolean.toString(this.useRegexCheckBox.isSelected()));
		Settings.getInstance().setProperty(PROP_KEY_CRITERIA, this.criteriaTextField.getText());
		Settings.getInstance().setProperty(PROP_KEY_REPLACEMENT, this.replaceValueTextField.getText());
		Settings.getInstance().storeWindowPosition(this.dialog, PROP_CLASS + ".window");
	}

	private void restoreSettings()
	{
		this.ignoreCaseCheckBox.setSelected(Settings.getInstance().getBoolProperty(PROP_KEY_CASE, true));
		this.wordsOnlyCheckBox.setSelected(Settings.getInstance().getBoolProperty(PROP_KEY_WHOLE_WORD, false));
		this.selectedTextCheckBox.setSelected(Settings.getInstance().getBoolProperty(PROP_KEY_SELECTED, false));
		this.useRegexCheckBox.setSelected(Settings.getInstance().getBoolProperty(PROP_KEY_USE_REGEX, true));
		this.criteriaTextField.setText(Settings.getInstance().getProperty(PROP_KEY_CRITERIA, ""));
		this.replaceValueTextField.setText(Settings.getInstance().getProperty(PROP_KEY_REPLACEMENT, ""));
	}

	public void windowActivated(java.awt.event.WindowEvent e)
	{
	}

	public void windowClosed(java.awt.event.WindowEvent e)
	{
	}

	public void windowClosing(java.awt.event.WindowEvent e)
	{
		this.closeWindow();
	}

	public void windowDeactivated(java.awt.event.WindowEvent e)
	{
	}

	public void windowDeiconified(java.awt.event.WindowEvent e)
	{
	}

	public void windowIconified(java.awt.event.WindowEvent e)
	{
	}

	public void windowOpened(java.awt.event.WindowEvent e)
	{
	}

  // Variables declaration - do not modify//GEN-BEGIN:variables
  protected javax.swing.JButton closeButton;
  protected javax.swing.JLabel criteriaLabel;
  protected javax.swing.JTextField criteriaTextField;
  protected javax.swing.JButton findButton;
  protected javax.swing.JCheckBox ignoreCaseCheckBox;
  protected javax.swing.JButton replaceAllButton;
  protected javax.swing.JLabel replaceLabel;
  protected javax.swing.JButton replaceNextButton;
  protected javax.swing.JTextField replaceValueTextField;
  protected javax.swing.JCheckBox selectedTextCheckBox;
  protected javax.swing.JPanel spacerPanel;
  protected javax.swing.JCheckBox useRegexCheckBox;
  protected javax.swing.JCheckBox wordsOnlyCheckBox;
  // End of variables declaration//GEN-END:variables


}
