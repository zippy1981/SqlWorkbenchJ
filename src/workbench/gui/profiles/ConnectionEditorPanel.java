/*
 * ConnectionEditorPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.BooleanPropertyEditor;
import workbench.gui.components.IntegerPropertyEditor;
import workbench.gui.components.PasswordPropertyEditor;
import workbench.gui.components.StringPropertyEditor;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.help.HtmlViewer;
import workbench.interfaces.SimplePropertyEditor;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.FileDialogUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ConnectionEditorPanel
	extends JPanel
	implements PropertyChangeListener, ActionListener
{
	private ConnectionProfile currentProfile;
	private ProfileListModel sourceModel;
	private boolean init;
	private List editors;

	public ConnectionEditorPanel()
	{
		this.initComponents();

		WbTraversalPolicy policy = new WbTraversalPolicy();
		policy.addComponent(tfProfileName);
		policy.addComponent(cbDrivers);
		policy.addComponent(tfURL);
		policy.addComponent(tfUserName);
		policy.addComponent(tfPwd);
		policy.addComponent(tfFetchSize);
		policy.addComponent(cbAutocommit);
		policy.addComponent(cbStorePassword);
		policy.addComponent(cbSeparateConnections);
		policy.addComponent(cbIgnoreDropErrors);
		policy.addComponent(disableTableCheck);
		policy.addComponent(rollbackBeforeDisconnect);
		policy.addComponent(tfWorkspaceFile);
		policy.addComponent(confirmUpdates);
		policy.addComponent(emptyStringIsNull);
		policy.addComponent(removeComments);
		policy.setDefaultComponent(tfProfileName);

		this.setFocusCycleRoot(true);
		this.setFocusTraversalPolicy(policy);

		this.initEditorList();

		this.selectWkspButton.addActionListener(this);
		this.showPassword.addActionListener(this);
	}

	private void initEditorList()
	{
		this.editors = new ArrayList(10);
		initEditorList(this);
	}
	
	private void initEditorList(Container parent)
	{
		for (int i=0; i < parent.getComponentCount(); i++)
		{
			Component c = parent.getComponent(i);
			if (c instanceof SimplePropertyEditor)
			{
				this.editors.add(c);
				String name = c.getName();
				c.addPropertyChangeListener(name, this);
        ((SimplePropertyEditor)c).setImmediateUpdate(true);
			}
			else if (c instanceof JPanel)
			{
				initEditorList((JPanel)c);
			}
		}
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

    tfProfileName = new StringPropertyEditor();
    cbDrivers = new javax.swing.JComboBox();
    tfURL = new StringPropertyEditor();
    tfUserName = new StringPropertyEditor();
    tfPwd = new PasswordPropertyEditor();
    cbAutocommit = new BooleanPropertyEditor();
    lblUsername = new javax.swing.JLabel();
    lblPwd = new javax.swing.JLabel();
    lblDriver = new javax.swing.JLabel();
    lblUrl = new javax.swing.JLabel();
    jSeparator2 = new javax.swing.JSeparator();
    jSeparator1 = new javax.swing.JSeparator();
    manageDriversButton = new WbButton();
    extendedProps = new javax.swing.JButton();
    helpButton = new WbButton();
    tfFetchSize = new IntegerPropertyEditor();
    fetchSizeLabel = new javax.swing.JLabel();
    showPassword = new javax.swing.JButton();
    wbOptionsPanel = new javax.swing.JPanel();
    cbStorePassword = new BooleanPropertyEditor();
    disableTableCheck = new BooleanPropertyEditor();
    rollbackBeforeDisconnect = new BooleanPropertyEditor();
    confirmUpdates = new BooleanPropertyEditor();
    cbIgnoreDropErrors = new BooleanPropertyEditor();
    cbSeparateConnections = new BooleanPropertyEditor();
    emptyStringIsNull = new BooleanPropertyEditor();
    includeNull = new BooleanPropertyEditor();
    removeComments = new BooleanPropertyEditor();
    jPanel1 = new javax.swing.JPanel();
    tfWorkspaceFile = new StringPropertyEditor();
    selectWkspButton = new javax.swing.JButton();
    workspaceFileLabel = new javax.swing.JLabel();
    jLabel1 = new javax.swing.JLabel();

    setLayout(new java.awt.GridBagLayout());

    setMinimumSize(new java.awt.Dimension(200, 200));
    tfProfileName.setHorizontalAlignment(javax.swing.JTextField.LEFT);
    tfProfileName.setName("name");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(10, 5, 5, 5);
    add(tfProfileName, gridBagConstraints);

    cbDrivers.setFocusCycleRoot(true);
    cbDrivers.setMaximumSize(new java.awt.Dimension(32767, 20));
    cbDrivers.setMinimumSize(new java.awt.Dimension(40, 20));
    cbDrivers.setName("driverclass");
    cbDrivers.setPreferredSize(new java.awt.Dimension(120, 20));
    cbDrivers.setVerifyInputWhenFocusTarget(false);
    cbDrivers.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        cbDriversItemStateChanged(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 6);
    add(cbDrivers, gridBagConstraints);

    tfURL.setHorizontalAlignment(javax.swing.JTextField.LEFT);
    tfURL.setMaximumSize(new java.awt.Dimension(2147483647, 20));
    tfURL.setMinimumSize(new java.awt.Dimension(40, 20));
    tfURL.setName("url");
    tfURL.setPreferredSize(new java.awt.Dimension(100, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 6);
    add(tfURL, gridBagConstraints);

    tfUserName.setHorizontalAlignment(javax.swing.JTextField.LEFT);
    tfUserName.setToolTipText("");
    tfUserName.setMaximumSize(new java.awt.Dimension(2147483647, 20));
    tfUserName.setMinimumSize(new java.awt.Dimension(40, 20));
    tfUserName.setName("username");
    tfUserName.setPreferredSize(new java.awt.Dimension(100, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 6);
    add(tfUserName, gridBagConstraints);

    tfPwd.setName("password");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 2);
    add(tfPwd, gridBagConstraints);

    cbAutocommit.setName("autocommit");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 1, 6, 6);
    add(cbAutocommit, gridBagConstraints);

    lblUsername.setLabelFor(tfUserName);
    lblUsername.setText(ResourceMgr.getString(ResourceMgr.TXT_DB_USERNAME));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 2, 0);
    add(lblUsername, gridBagConstraints);

    lblPwd.setLabelFor(tfPwd);
    lblPwd.setText(ResourceMgr.getString(ResourceMgr.TXT_DB_PASSWORD));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    add(lblPwd, gridBagConstraints);

    lblDriver.setLabelFor(cbDrivers);
    lblDriver.setText(ResourceMgr.getString(ResourceMgr.TXT_DB_DRIVER));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 2, 0);
    add(lblDriver, gridBagConstraints);

    lblUrl.setLabelFor(tfURL);
    lblUrl.setText(ResourceMgr.getString(ResourceMgr.TXT_DB_URL));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 2, 0);
    add(lblUrl, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
    add(jSeparator2, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 15;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
    gridBagConstraints.weighty = 1.0;
    add(jSeparator1, gridBagConstraints);

    manageDriversButton.setText(ResourceMgr.getString("LabelEditDrivers"));
    manageDriversButton.setToolTipText(ResourceMgr.getDescription("EditDrivers"));
    manageDriversButton.setMaximumSize(new java.awt.Dimension(200, 25));
    manageDriversButton.setMinimumSize(new java.awt.Dimension(70, 25));
    manageDriversButton.setPreferredSize(new java.awt.Dimension(150, 25));
    manageDriversButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        showDriverEditorDialog(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 17;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 6, 0);
    add(manageDriversButton, gridBagConstraints);

    extendedProps.setText(ResourceMgr.getString("LabelConnExtendedProps"));
    extendedProps.setToolTipText(ResourceMgr.getDescription("LabelConnExtendedProps"));
    extendedProps.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEtchedBorder(), javax.swing.BorderFactory.createEmptyBorder(1, 6, 1, 6)));
    extendedProps.addMouseListener(new java.awt.event.MouseAdapter()
    {
      public void mouseClicked(java.awt.event.MouseEvent evt)
      {
        extendedPropsMouseClicked(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 5, 6);
    add(extendedProps, gridBagConstraints);

    helpButton.setText(ResourceMgr.getString("LabelHelp"));
    helpButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        helpButtonActionPerformed(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 17;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
    add(helpButton, gridBagConstraints);

    tfFetchSize.setName("defaultFetchSize");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 2);
    add(tfFetchSize, gridBagConstraints);

    fetchSizeLabel.setText(ResourceMgr.getString("LabelFetchSize"));
    fetchSizeLabel.setToolTipText(ResourceMgr.getDescription("LabelFetchSize"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    add(fetchSizeLabel, gridBagConstraints);

    showPassword.setText(ResourceMgr.getString("LabelShowPassword"));
    showPassword.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEtchedBorder(), javax.swing.BorderFactory.createEmptyBorder(1, 6, 1, 6)));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 2, 6);
    add(showPassword, gridBagConstraints);

    wbOptionsPanel.setLayout(new java.awt.GridBagLayout());

    cbStorePassword.setSelected(true);
    cbStorePassword.setText(ResourceMgr.getString("LabelSavePassword"));
    cbStorePassword.setToolTipText(ResourceMgr.getDescription("LabelSavePassword"));
    cbStorePassword.setName("storePassword");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
    wbOptionsPanel.add(cbStorePassword, gridBagConstraints);

    disableTableCheck.setText(ResourceMgr.getString("LabelDisableAutoTableCheck"));
    disableTableCheck.setToolTipText(ResourceMgr.getDescription("LabelDisableAutoTableCheck"));
    disableTableCheck.setName("disableUpdateTableCheck");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
    wbOptionsPanel.add(disableTableCheck, gridBagConstraints);

    rollbackBeforeDisconnect.setText(ResourceMgr.getString("LabelRollbackBeforeDisconnect"));
    rollbackBeforeDisconnect.setToolTipText(ResourceMgr.getDescription("LabelRollbackBeforeDisconnect"));
    rollbackBeforeDisconnect.setName("rollbackBeforeDisconnect");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
    wbOptionsPanel.add(rollbackBeforeDisconnect, gridBagConstraints);

    confirmUpdates.setText(ResourceMgr.getString("LabelConfirmDbUpdates"));
    confirmUpdates.setToolTipText(ResourceMgr.getDescription("LabelConfirmDbUpdates"));
    confirmUpdates.setName("confirmUpdates");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
    wbOptionsPanel.add(confirmUpdates, gridBagConstraints);

    cbIgnoreDropErrors.setSelected(true);
    cbIgnoreDropErrors.setText(ResourceMgr.getString("LabelIgnoreDropErrors"));
    cbIgnoreDropErrors.setToolTipText(ResourceMgr.getDescription("LabelIgnoreDropErrors"));
    cbIgnoreDropErrors.setName("ignoreDropErrors");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
    wbOptionsPanel.add(cbIgnoreDropErrors, gridBagConstraints);

    cbSeparateConnections.setText(ResourceMgr.getString("LabelSeperateConnections"));
    cbSeparateConnections.setToolTipText(ResourceMgr.getDescription("LabelSeperateConnections"));
    cbSeparateConnections.setName("useSeparateConnectionPerTab");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
    wbOptionsPanel.add(cbSeparateConnections, gridBagConstraints);

    emptyStringIsNull.setText(ResourceMgr.getString("LabelEmptyStringIsNull"));
    emptyStringIsNull.setToolTipText(ResourceMgr.getDescription("LabelEmptyStringIsNull"));
    emptyStringIsNull.setName("emptyStringIsNull");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
    wbOptionsPanel.add(emptyStringIsNull, gridBagConstraints);

    includeNull.setText(ResourceMgr.getString("LabelIncludeNullInInsert"));
    includeNull.setToolTipText(ResourceMgr.getString("LabelIncludeNullInInsert"));
    includeNull.setName("includeNullInInsert");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
    wbOptionsPanel.add(includeNull, gridBagConstraints);

    removeComments.setText(ResourceMgr.getString("LabelRemoveComments"));
    removeComments.setToolTipText(ResourceMgr.getDescription("LabelRemoveComments"));
    removeComments.setName("removeComments");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
    wbOptionsPanel.add(removeComments, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.gridheight = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(3, 0, 5, 6);
    add(wbOptionsPanel, gridBagConstraints);

    jPanel1.setLayout(new java.awt.GridBagLayout());

    tfWorkspaceFile.setHorizontalAlignment(javax.swing.JTextField.LEFT);
    tfWorkspaceFile.setMaximumSize(new java.awt.Dimension(2147483647, 20));
    tfWorkspaceFile.setMinimumSize(new java.awt.Dimension(40, 20));
    tfWorkspaceFile.setName("workspaceFile");
    tfWorkspaceFile.setPreferredSize(new java.awt.Dimension(100, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    jPanel1.add(tfWorkspaceFile, gridBagConstraints);

    selectWkspButton.setText("...");
    selectWkspButton.setMaximumSize(new java.awt.Dimension(26, 22));
    selectWkspButton.setMinimumSize(new java.awt.Dimension(26, 22));
    selectWkspButton.setPreferredSize(new java.awt.Dimension(26, 22));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    jPanel1.add(selectWkspButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 13;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 6);
    add(jPanel1, gridBagConstraints);

    workspaceFileLabel.setLabelFor(tfWorkspaceFile);
    workspaceFileLabel.setText(ResourceMgr.getString("LabelOpenWksp"));
    workspaceFileLabel.setToolTipText(ResourceMgr.getDescription("LabelOpenWksp"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 13;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    add(workspaceFileLabel, gridBagConstraints);

    jLabel1.setText("Autocommit");
    jLabel1.addMouseListener(new java.awt.event.MouseAdapter()
    {
      public void mouseClicked(java.awt.event.MouseEvent evt)
      {
        jLabel1MouseClicked(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 3, 0);
    add(jLabel1, gridBagConstraints);

  }// </editor-fold>//GEN-END:initComponents

	private void jLabel1MouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jLabel1MouseClicked
	{//GEN-HEADEREND:event_jLabel1MouseClicked
		this.cbAutocommit.setSelected(!this.cbAutocommit.isSelected());
	}//GEN-LAST:event_jLabel1MouseClicked

	private void helpButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_helpButtonActionPerformed
	{//GEN-HEADEREND:event_helpButtonActionPerformed
		HtmlViewer viewer = new HtmlViewer((JDialog)SwingUtilities.getWindowAncestor(this));
		viewer.showProfileHelp();
	}//GEN-LAST:event_helpButtonActionPerformed

	private void extendedPropsMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_extendedPropsMouseClicked
	{//GEN-HEADEREND:event_extendedPropsMouseClicked
		this.editExtendedProperties();
	}//GEN-LAST:event_extendedPropsMouseClicked

	private void cbDriversItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbDriversItemStateChanged
	{//GEN-HEADEREND:event_cbDriversItemStateChanged
		if (this.init) return;
		if (evt.getStateChange() == ItemEvent.SELECTED)
		{
			String oldDriver = null;
			DbDriver newDriver = null;
			try
			{
				oldDriver = this.currentProfile.getDriverclass();
				newDriver = (DbDriver)this.cbDrivers.getSelectedItem();
				if(this.currentProfile != null)
				{
					this.currentProfile.setDriverclass(newDriver.getDriverClass());
					this.currentProfile.setDriverName(newDriver.getName());
				}
				if (oldDriver == null || !oldDriver.equals(newDriver.getDriverClass()))
				{
					this.tfURL.setText(newDriver.getSampleUrl());
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ConnectionProfilePanel.cbDriversItemStateChanged()", "Error changing driver", e);
			}

			if (!newDriver.canReadLibrary())
			{
				EventQueue.invokeLater(
					new Runnable()
					{
						public void run()
						{
							if (WbSwingUtilities.getYesNo(ConnectionEditorPanel.this, ResourceMgr.getString("MsgDriverLibraryNotReadable")))
							{
								showDriverEditorDialog(null);
							}
						}
					});
			}
		}
	}//GEN-LAST:event_cbDriversItemStateChanged

	private void showDriverEditorDialog(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showDriverEditorDialog
	{//GEN-HEADEREND:event_showDriverEditorDialog
		final Frame parent = (Frame)(SwingUtilities.getWindowAncestor(this)).getParent();
		DbDriver drv = (DbDriver)cbDrivers.getSelectedItem();
		final String drvName;
		if (drv != null)
		{
			drvName = drv.getName();
		}
		else
		{
			drvName = null;
		}
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				DriverEditorDialog d = new DriverEditorDialog(parent, true);
				d.setDriverName(drvName);
				WbSwingUtilities.center(d,parent);
				d.setVisible(true);
				if (!d.isCancelled())
				{
					List drivers = ConnectionMgr.getInstance().getDrivers();
					setDrivers(drivers);
				}
				d.dispose();
			}
		});

	}//GEN-LAST:event_showDriverEditorDialog

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JCheckBox cbAutocommit;
  private javax.swing.JComboBox cbDrivers;
  private javax.swing.JCheckBox cbIgnoreDropErrors;
  private javax.swing.JCheckBox cbSeparateConnections;
  private javax.swing.JCheckBox cbStorePassword;
  private javax.swing.JCheckBox confirmUpdates;
  private javax.swing.JCheckBox disableTableCheck;
  private javax.swing.JCheckBox emptyStringIsNull;
  private javax.swing.JButton extendedProps;
  private javax.swing.JLabel fetchSizeLabel;
  private javax.swing.JButton helpButton;
  private javax.swing.JCheckBox includeNull;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JSeparator jSeparator1;
  private javax.swing.JSeparator jSeparator2;
  private javax.swing.JLabel lblDriver;
  private javax.swing.JLabel lblPwd;
  private javax.swing.JLabel lblUrl;
  private javax.swing.JLabel lblUsername;
  private javax.swing.JButton manageDriversButton;
  private javax.swing.JCheckBox removeComments;
  private javax.swing.JCheckBox rollbackBeforeDisconnect;
  private javax.swing.JButton selectWkspButton;
  private javax.swing.JButton showPassword;
  private javax.swing.JTextField tfFetchSize;
  private javax.swing.JTextField tfProfileName;
  private javax.swing.JPasswordField tfPwd;
  private javax.swing.JTextField tfURL;
  private javax.swing.JTextField tfUserName;
  private javax.swing.JTextField tfWorkspaceFile;
  private javax.swing.JPanel wbOptionsPanel;
  private javax.swing.JLabel workspaceFileLabel;
  // End of variables declaration//GEN-END:variables

	public void setDrivers(List aDriverList)
	{
		if (aDriverList != null)
		{
			this.init = true;
			Object currentDriver = this.cbDrivers.getSelectedItem();
			try
			{
				Collections.sort(aDriverList, DbDriver.getDriverClassComparator());
				this.cbDrivers.setModel(new DefaultComboBoxModel(aDriverList.toArray()));
				if (currentDriver != null)
				{
					this.cbDrivers.setSelectedItem(currentDriver);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ConnectionEditorPanel.setDrivers()", "Error when setting new driver list", e);
			}
			finally
			{
				this.init = false;
			}

		}
	}

	public void editExtendedProperties()
	{
		if (this.currentProfile == null) return;
		Properties p = this.currentProfile.getConnectionProperties();
		ConnectionPropertiesEditor editor = new ConnectionPropertiesEditor(p);
		Dimension d = new Dimension(300,250);
		editor.setMinimumSize(d);
		editor.setPreferredSize(d);

		int choice = JOptionPane.showConfirmDialog(this, editor, ResourceMgr.getString("TxtEditConnPropsWindowTitle"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (choice == JOptionPane.OK_OPTION)
		{
			this.currentProfile.setConnectionProperties(editor.getProperties());
		}
	}
	
	public void selectWorkspace()
	{
		FileDialogUtil util = new FileDialogUtil();
		String filename = util.getWorkspaceFilename(SwingUtilities.getWindowAncestor(this), false, true);
		if (filename == null) return;
		this.tfWorkspaceFile.setText(filename);
	}

	public void setSourceList(ProfileListModel aSource)
	{
		this.sourceModel = aSource;
	}

	public void updateProfile()
	{
		if (this.init) return;
		if (this.currentProfile == null) return;
		if (this.editors == null) return;
		boolean changed = false;

		for (int i=0; i < this.editors.size(); i++)
		{
			SimplePropertyEditor editor = (SimplePropertyEditor)this.editors.get(i);
			changed = changed || editor.isChanged();
			editor.applyChanges();
		}

		if (changed)
		{
			this.sourceModel.profileChanged(this.currentProfile);
		}
	}

	public ConnectionProfile getProfile()
	{
		this.updateProfile();
		return this.currentProfile;
	}

	private void initPropertyEditors()
	{
		if (this.editors == null) return;
		if (this.currentProfile == null) return;

		for (int i=0; i < this.editors.size(); i++)
		{
			SimplePropertyEditor editor = (SimplePropertyEditor)this.editors.get(i);
			Component c = (Component)editor;
			String property = c.getName();
			if (property != null)
			{
				editor.setSourceObject(this.currentProfile, property);
			}
		}
	}

	public void setProfile(ConnectionProfile aProfile)
	{
		if (aProfile == null) return;

		this.currentProfile = aProfile;
		this.initPropertyEditors();

		String drvClass = aProfile.getDriverclass();
		DbDriver drv = null;
		if (drvClass != null)
		{
			String name = aProfile.getDriverName();
			drv = ConnectionMgr.getInstance().findDriverByName(drvClass, name);
		}

		try
		{
			this.init = true;
			cbDrivers.setSelectedItem(drv);
		}
		catch (Exception e)
		{
			LogMgr.logError("ConnectionEditorPanel.setProfile()", "Error setting profile", e);
		}
		finally
		{
			this.init = false;
		}
	}

	/** This method gets called when a bound property is changed.
	 * @param evt A PropertyChangeEvent object describing the event source
	 *   	and the property that has changed.
	 *
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		//this.updateProfile();
    if (!this.init)	this.sourceModel.profileChanged(this.currentProfile);
	}

	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		if (e.getSource() == this.selectWkspButton)
		{
			this.selectWorkspace();
		}
		else if (e.getSource() == this.showPassword)
		{
			String pwd = this.getProfile().getInputPassword();
			String title = ResourceMgr.getString("LabelCurrentPassword");
			title += " " + this.getProfile().getUsername();
			JTextField f = new JTextField();
			f.setDisabledTextColor(Color.BLACK);
			f.setEditable(false);
			f.setText(pwd);
			Border b = new CompoundBorder(new LineBorder(Color.LIGHT_GRAY), new EmptyBorder(2,2,2,2));
			f.setBorder(b);
			TextComponentMouseListener l = new TextComponentMouseListener();
			f.addMouseListener(l);
			//WbSwingUtilities.showMessage(this, f);
			JOptionPane.showMessageDialog(this.getParent(), f, title, JOptionPane.PLAIN_MESSAGE);
		}
	}

}
