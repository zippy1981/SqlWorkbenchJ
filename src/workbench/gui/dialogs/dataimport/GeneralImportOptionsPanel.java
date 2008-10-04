/*
 * GeneralImportOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs.dataimport;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author  support@sql-workbench.net
 */
public class GeneralImportOptionsPanel
	extends javax.swing.JPanel
	implements ImportOptions
{
	public GeneralImportOptionsPanel()
	{
		super();
		initComponents();
	}

	public void setEncodingVisible(boolean flag)
	{
		this.encodingPanel.setEnabled(false);
		this.encodingPanel.setVisible(false);
	}

	public void setModeSelectorEnabled(boolean flag)
	{
		this.modeComboBox.setEnabled(flag);
		this.modeComboBox.setSelectedIndex(0);
		this.modeComboBox.setVisible(flag);
	}

	public void saveSettings()
	{
		saveSettings("general");
	}
	public void saveSettings(String key)
	{
		Settings s = Settings.getInstance();
		s.setProperty("workbench.import." + key + ".dateformat", this.getDateFormat());
		s.setProperty("workbench.import." + key + ".timestampformat", this.getTimestampFormat());
		s.setProperty("workbench.import." + key + ".encoding", this.getEncoding());
		s.setProperty("workbench.import." + key + ".mode", this.getMode());
	}

	public void restoreSettings()
	{
		restoreSettings("general");
	}
	public void restoreSettings(String key)
	{
		Settings s = Settings.getInstance();
		this.setDateFormat(s.getProperty("workbench.import." + key + ".dateformat", s.getDefaultDateFormat()));
		this.setTimestampFormat(s.getProperty("workbench.import." + key + ".timestampformat", s.getDefaultTimestampFormat()));
		this.setEncoding(s.getProperty("workbench.export." + key + ".encoding", s.getDefaultDataEncoding()));
		this.setMode(s.getProperty("workbench.import." + key + ".mode", "insert"));
	}

	public String getMode()
	{
		return (String)this.modeComboBox.getSelectedItem();
	}

	public void setMode(String mode)
	{
		this.modeComboBox.setSelectedItem(mode);
	}

	public String getDateFormat()
	{
		return this.dateFormat.getText();
	}

	public String getEncoding()
	{
		return encodingPanel.getEncoding();
	}

	public String getTimestampFormat()
	{
		return this.timestampFormat.getText();
	}

	public void setDateFormat(String format)
	{
		dateFormat.setText(format);
	}

	public void setEncoding(String enc)
	{
		encodingPanel.setEncoding(enc);
	}

	public void setTimestampFormat(String format)
	{
		timestampFormat.setText(format);
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

    encodingPanel = new workbench.gui.components.EncodingPanel();
    dateFormatLabel = new javax.swing.JLabel();
    dateFormat = new javax.swing.JTextField();
    timestampFormatLabel = new javax.swing.JLabel();
    timestampFormat = new javax.swing.JTextField();
    jPanel1 = new javax.swing.JPanel();
    modeLabel = new javax.swing.JLabel();
    modeComboBox = new javax.swing.JComboBox();

    setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    add(encodingPanel, gridBagConstraints);

    dateFormatLabel.setText(ResourceMgr.getString("LblDateFormat"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
    add(dateFormatLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
    add(dateFormat, gridBagConstraints);

    timestampFormatLabel.setText(ResourceMgr.getString("LblTimestampFormat"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
    add(timestampFormatLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
    add(timestampFormat, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(jPanel1, gridBagConstraints);

    modeLabel.setText(ResourceMgr.getString("LblImportMode"));
    modeLabel.setToolTipText(ResourceMgr.getDescription("LblImportMode"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
    add(modeLabel, gridBagConstraints);

    modeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "insert", "update", "insert,update", "update,insert" }));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
    add(modeComboBox, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JTextField dateFormat;
  private javax.swing.JLabel dateFormatLabel;
  private workbench.gui.components.EncodingPanel encodingPanel;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JComboBox modeComboBox;
  private javax.swing.JLabel modeLabel;
  private javax.swing.JTextField timestampFormat;
  private javax.swing.JLabel timestampFormatLabel;
  // End of variables declaration//GEN-END:variables

}
