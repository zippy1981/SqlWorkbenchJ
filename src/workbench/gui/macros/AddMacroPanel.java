/*
 * AddMacroPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import workbench.resource.ResourceMgr;
import workbench.sql.macros.MacroGroup;
import workbench.sql.macros.MacroManager;

/**
 *
 * @author Thomas Kellerer
 */
public class AddMacroPanel
	extends javax.swing.JPanel
	implements WindowListener
{
	public AddMacroPanel()
	{
		initComponents();
		List<MacroGroup> groups = MacroManager.getInstance().getMacros().getGroups();
		DefaultComboBoxModel model = new DefaultComboBoxModel();
		for (MacroGroup group : groups)
		{
			model.addElement(group);
		}
		groupDropDown.setModel(model);
	}

	public MacroGroup getSelectedGroup()
	{
		return (MacroGroup)groupDropDown.getSelectedItem();
	}

	public String getMacroName()
	{
		return macroName.getText();
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
		GridBagConstraints gridBagConstraints;

    jLabel1 = new JLabel();
    jLabel2 = new JLabel();
    groupDropDown = new JComboBox();
    macroName = new JTextField();

    setLayout(new GridBagLayout());

    jLabel1.setText(ResourceMgr.getString("LblMacroGrpName")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(7, 5, 0, 0);
    add(jLabel1, gridBagConstraints);

    jLabel2.setText(ResourceMgr.getString("LblMacroName")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(13, 5, 5, 0);
    add(jLabel2, gridBagConstraints);

    groupDropDown.setModel(new DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(7, 9, 0, 5);
    add(groupDropDown, gridBagConstraints);

    macroName.setColumns(30);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(12, 9, 5, 5);
    add(macroName, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JComboBox groupDropDown;
  private JLabel jLabel1;
  private JLabel jLabel2;
  private JTextField macroName;
  // End of variables declaration//GEN-END:variables

	public void windowOpened(WindowEvent e)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				macroName.requestFocusInWindow();
			}
		});
	}

	public void windowClosing(WindowEvent e)
	{
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowDeactivated(WindowEvent e)
	{
	}
}
