/*
 * ConnectionEditorPanel.java
 *
 * Created on January 25, 2002, 11:27 PM
 */

package workbench.gui.profiles;

import java.awt.Frame;
import java.awt.event.ItemEvent;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.WbManager;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import workbench.db.DbDriver;
import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  thomas
 */
public class ConnectionEditorPanel extends javax.swing.JPanel
{
	private ConnectionProfile currentProfile;
	private List drivers;
	private boolean init;

	public ConnectionEditorPanel()
	{
		this.initComponents();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	private void initComponents()//GEN-BEGIN:initComponents
	{
		java.awt.GridBagConstraints gridBagConstraints;
		
		lblUsername = new javax.swing.JLabel();
		tfUserName = new javax.swing.JTextField();
		lblPwd = new javax.swing.JLabel();
		jLabel1 = new javax.swing.JLabel();
		cbDrivers = new javax.swing.JComboBox();
		jLabel2 = new javax.swing.JLabel();
		tfURL = new javax.swing.JTextField();
		tfPwd = new javax.swing.JPasswordField();
		cbAutocommit = new javax.swing.JCheckBox();
		dummy = new javax.swing.JPanel();
		tfProfileName = new javax.swing.JTextField();
		manageDriversButton = new javax.swing.JButton();
		
		setLayout(new java.awt.GridBagLayout());
		
		setMinimumSize(new java.awt.Dimension(200, 200));
		lblUsername.setFont(null);
		lblUsername.setText(ResourceMgr.getString(ResourceMgr.TXT_DB_USERNAME));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(0, 5, 2, 0);
		add(lblUsername, gridBagConstraints);
		
		tfUserName.setFont(null);
		tfUserName.setHorizontalAlignment(javax.swing.JTextField.LEFT);
		tfUserName.setToolTipText("");
		tfUserName.setMaximumSize(new java.awt.Dimension(2147483647, 20));
		tfUserName.setMinimumSize(new java.awt.Dimension(40, 20));
		tfUserName.setNextFocusableComponent(tfPwd);
		tfUserName.setPreferredSize(new java.awt.Dimension(100, 20));
		tfUserName.addFocusListener(new java.awt.event.FocusAdapter()
		{
			public void focusLost(java.awt.event.FocusEvent evt)
			{
				fieldFocusLost(evt);
			}
		});
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 6);
		add(tfUserName, gridBagConstraints);
		
		lblPwd.setFont(null);
		lblPwd.setText(ResourceMgr.getString(ResourceMgr.TXT_DB_PASSWORD));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
		add(lblPwd, gridBagConstraints);
		
		jLabel1.setFont(null);
		jLabel1.setText(ResourceMgr.getString(ResourceMgr.TXT_DB_DRIVER));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(0, 5, 2, 0);
		add(jLabel1, gridBagConstraints);
		
		cbDrivers.setEditable(true);
		cbDrivers.setFont(null);
		cbDrivers.setMaximumSize(new java.awt.Dimension(32767, 20));
		cbDrivers.setMinimumSize(new java.awt.Dimension(40, 20));
		cbDrivers.setNextFocusableComponent(tfURL);
		cbDrivers.setPreferredSize(new java.awt.Dimension(120, 20));
		cbDrivers.addFocusListener(new java.awt.event.FocusAdapter()
		{
			public void focusLost(java.awt.event.FocusEvent evt)
			{
				fieldFocusLost(evt);
			}
		});
		
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
		gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 4);
		add(cbDrivers, gridBagConstraints);
		
		jLabel2.setFont(null);
		jLabel2.setText(ResourceMgr.getString(ResourceMgr.TXT_DB_URL));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(0, 5, 2, 0);
		add(jLabel2, gridBagConstraints);
		
		tfURL.setFont(null);
		tfURL.setHorizontalAlignment(javax.swing.JTextField.LEFT);
		tfURL.setMaximumSize(new java.awt.Dimension(2147483647, 20));
		tfURL.setMinimumSize(new java.awt.Dimension(40, 20));
		tfURL.setNextFocusableComponent(tfUserName);
		tfURL.setPreferredSize(new java.awt.Dimension(100, 20));
		tfURL.addFocusListener(new java.awt.event.FocusAdapter()
		{
			public void focusLost(java.awt.event.FocusEvent evt)
			{
				fieldFocusLost(evt);
			}
		});
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 6);
		add(tfURL, gridBagConstraints);
		
		tfPwd.setFont(null);
		tfPwd.addFocusListener(new java.awt.event.FocusAdapter()
		{
			public void focusLost(java.awt.event.FocusEvent evt)
			{
				fieldFocusLost(evt);
			}
		});
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 6);
		add(tfPwd, gridBagConstraints);
		
		cbAutocommit.setFont(null);
		cbAutocommit.setText("Autocommit");
		cbAutocommit.addItemListener(new java.awt.event.ItemListener()
		{
			public void itemStateChanged(java.awt.event.ItemEvent evt)
			{
				cbAutocommitItemStateChanged(evt);
			}
		});
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 6);
		add(cbAutocommit, gridBagConstraints);
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
		gridBagConstraints.weighty = 1.0;
		add(dummy, gridBagConstraints);
		
		tfProfileName.setFont(null);
		tfProfileName.setHorizontalAlignment(javax.swing.JTextField.LEFT);
		tfProfileName.addFocusListener(new java.awt.event.FocusAdapter()
		{
			public void focusLost(java.awt.event.FocusEvent evt)
			{
				fieldFocusLost(evt);
			}
		});
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(10, 5, 5, 5);
		add(tfProfileName, gridBagConstraints);
		
		manageDriversButton.setFont(null);
		manageDriversButton.setText(ResourceMgr.getString("LabelEditDrivers"));
		manageDriversButton.setMaximumSize(new java.awt.Dimension(200, 20));
		manageDriversButton.setMinimumSize(new java.awt.Dimension(70, 20));
		manageDriversButton.setPreferredSize(new java.awt.Dimension(100, 20));
		manageDriversButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				showDriverEditorDialog(evt);
			}
		});
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 1, 6);
		add(manageDriversButton, gridBagConstraints);
		
	}//GEN-END:initComponents

	private void cbDriversItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbDriversItemStateChanged
	{//GEN-HEADEREND:event_cbDriversItemStateChanged
		if (evt.getStateChange() == ItemEvent.SELECTED)
		{
			if (this.tfURL.getText() == null || this.tfURL.getText().trim().length() == 0)
			{
				DbDriver newDriver = (DbDriver)this.cbDrivers.getSelectedItem();
				this.tfURL.setText(newDriver.getSampleUrl());
			}
		}
	}//GEN-LAST:event_cbDriversItemStateChanged

	private void showDriverEditorDialog(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showDriverEditorDialog
	{//GEN-HEADEREND:event_showDriverEditorDialog
		// not really nice, but works until the driver editor can be
		// called from a different location...
		Frame parent = (Frame)(SwingUtilities.getWindowAncestor(this)).getParent();
		DriverEditorDialog d = new DriverEditorDialog(parent, true);
		WbSwingUtilities.center(d,parent);
		d.show();
	}//GEN-LAST:event_showDriverEditorDialog

	private void cbAutocommitItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbAutocommitItemStateChanged
	{//GEN-HEADEREND:event_cbAutocommitItemStateChanged
		this.updateProfile();
	}//GEN-LAST:event_cbAutocommitItemStateChanged

	private void fieldFocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_fieldFocusLost
	{//GEN-HEADEREND:event_fieldFocusLost
		this.updateProfile();
	}//GEN-LAST:event_fieldFocusLost


	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton manageDriversButton;
	private javax.swing.JComboBox cbDrivers;
	private javax.swing.JLabel lblUsername;
	private javax.swing.JPasswordField tfPwd;
	private javax.swing.JPanel dummy;
	private javax.swing.JLabel lblPwd;
	private javax.swing.JCheckBox cbAutocommit;
	private javax.swing.JTextField tfURL;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JTextField tfUserName;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JTextField tfProfileName;
	// End of variables declaration//GEN-END:variables

	public void setDrivers(List aDriverList)
	{
		this.drivers = aDriverList;
		if (aDriverList != null)
			this.cbDrivers.setModel(new DefaultComboBoxModel(aDriverList.toArray()));
	}

	public void updateProfile()
	{
		if (this.init) return;
		if (this.currentProfile == null) return;

		Object driver = cbDrivers.getSelectedItem();
		if (driver != null)
		{
			this.currentProfile.setDriverclass(driver.toString());
		}
		this.currentProfile.setPassword(tfPwd.getText());
		this.currentProfile.setUrl(tfURL.getText());
		this.currentProfile.setUsername(tfUserName.getText());
		this.currentProfile.setAutocommit(cbAutocommit.isSelected());
		this.currentProfile.setName(tfProfileName.getText());


		// dirty trick to update the list display
		// if I update the parent, the divider size gets reset :-(
		JSplitPane parent = (JSplitPane)this.getParent();
		JComponent list = (JComponent)parent.getLeftComponent();
		list.updateUI();
	}

	public ConnectionProfile getProfile()
	{
		this.updateProfile();
		return this.currentProfile;
	}

	public void setProfile(ConnectionProfile aProfile)
	{
		this.init = true;
		this.currentProfile = aProfile;
		this.tfProfileName.setText(aProfile.getName());
		this.tfUserName.setText(aProfile.getUsername());
		this.tfURL.setText(aProfile.getUrl());
		this.tfURL.setCaretPosition(0);
		this.tfPwd.setText(aProfile.decryptPassword());
		this.cbAutocommit.setSelected(aProfile.getAutocommit());
		DbDriver driver;
		int newIndex = -1;
		int count = this.cbDrivers.getItemCount();
		for (int i=0; i < count; i++)
		{
			driver = (DbDriver)this.cbDrivers.getItemAt(i);
			if (driver.getDriverClass().equals(aProfile.getDriverclass()))
			{
				newIndex = i;
				break;
			}
		}
		if (newIndex >= 0 )
		{
			this.cbDrivers.setSelectedIndex(newIndex);
		}
		else
		{
			DbDriver drv = new DbDriver(aProfile.getDriverclass());
			this.cbDrivers.addItem(drv);
			this.cbDrivers.setSelectedIndex(this.cbDrivers.getItemCount() - 1);
		}
		this.init = false;
	}

}
