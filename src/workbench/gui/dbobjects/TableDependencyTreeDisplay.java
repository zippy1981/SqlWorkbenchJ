/*
 * TableReferenceTree.java
 *
 * Created on October 22, 2002, 5:13 PM
 */

package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import workbench.db.DependencyNode;
import workbench.db.TableDependency;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbScrollPane;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;


/**
 *
 * @author  workbench@kellerer.org
 */
public class TableDependencyTreeDisplay 
  extends JPanel
{
	private WbConnection connection;
	private DefaultTreeCellRenderer renderer;
	private DefaultTreeCellRenderer emptyRenderer;
	
	private	DefaultMutableTreeNode emptyRoot = new DefaultMutableTreeNode("");
	private ArrayList nodesToExpand;
	
	public TableDependencyTreeDisplay()
	{
		this.setLayout(new BorderLayout());
		renderer = new DefaultTreeCellRenderer();
		renderer.setLeafIcon(ResourceMgr.getPicture("foreignKey"));
		ImageIcon table = ResourceMgr.getPicture("table");
		renderer.setOpenIcon(table);
		renderer.setClosedIcon(table);
	}

	public void setConnection(WbConnection aConn)
	{
		this.connection = aConn;
	}
	
	public void readTree(String aCatalog, String aSchema, String aTable)
	{
    try
    {
			WbSwingUtilities.showWaitCursor(this);
      TableDependency dep = new TableDependency();
      dep.setConnection(this.connection);
      dep.setTableName(aCatalog, aSchema, aTable);
      dep.readDependencyTree();
      DependencyNode root = dep.getRootNode();
      this.readTreeNodes(root);
    }
    catch (Exception e)
    {
      LogMgr.logError("TableDependencyTreeDisplay.readTree()", "Error reading three", e);
    }
		WbSwingUtilities.showDefaultCursor(this);
	}

	public void reset()
	{
		this.removeAll();//this.createTreeDisplay(emptyRoot);
		this.invalidate();
		this.repaint();
		this.doLayout();
	}
	
	private void createTreeDisplay(DefaultMutableTreeNode root)
	{
		this.removeAll();
		this.invalidate();
		JTree tree = new JTree(root);
		tree.putClientProperty("JTree.lineStyle", "Angled");
		tree.setCellRenderer(this.renderer);
		WbScrollPane scroll = new WbScrollPane(tree);
		this.expandNodes(tree);
		this.add(scroll, BorderLayout.CENTER);
		this.updateUI();
		this.repaint();
	}
	
  private void readTreeNodes(DependencyNode root)
  {
		DefaultMutableTreeNode treeRoot = null;
		if (root.getChildren().size() > 0)
		{
			treeRoot = new DefaultMutableTreeNode(root, true);
			this.buildTree(root, treeRoot);
			this.createTreeDisplay(treeRoot);
		}
		else
		{
			this.reset();
		}
  }
	
	private void buildTree(DependencyNode parent, DefaultMutableTreeNode treeParent)
	{
		String parentcatalog = parent.getCatalog();
		String parentschema = parent.getSchema();
		String parenttable = parent.getTable();
			
		DependencyNode child = null;
		String currentfk = null;
		String currenttable = null;
		DefaultMutableTreeNode treeNode = null;
		String catalog = null;
		String schema = null;
		String table = null;
		String fkname = null;
			
		List children = parent.getChildren();
		int count = children.size();
		for (int i=0; i<count; i++)
		{
			child = (DependencyNode)children.get(i);

			treeNode = new DefaultMutableTreeNode(child, true);
			treeNode.setAllowsChildren(true);
			treeParent.add(treeNode);

			int childrenCount = child.getChildren().size();

			if (treeNode != null)
			{
				Map columns = child.getColumns();
				Iterator entries = columns.entrySet().iterator();
				while (entries.hasNext())
				{
					Entry entry = (Entry)entries.next();
					StringBuffer coldef = new StringBuffer(100);
					coldef.append("<html><body><b>");
					coldef.append(entry.getKey());
					coldef.append("</b> REFERENCES <b>");
					coldef.append(parenttable);
					coldef.append('.');
					coldef.append(entry.getValue());
					coldef.append("</b></body></html>");
					DefaultMutableTreeNode colnode = new DefaultMutableTreeNode(coldef.toString());
					colnode.setAllowsChildren(false);
					treeNode.add(colnode);
				}
			}
			
			if (childrenCount > 0)
			{
				this.buildTree(child, treeNode);
				TreeNode[] path = treeNode.getPath();
				if (this.nodesToExpand == null) this.nodesToExpand = new ArrayList();
				this.nodesToExpand.add(path);
			}
		}	
	}
	
	private void expandNodes(JTree tree)
	{
		if (this.nodesToExpand == null) return;
		for (int i=0; i < this.nodesToExpand.size(); i++)
		{
			TreeNode[] nodes = (TreeNode[])this.nodesToExpand.get(i);
			TreePath path = new TreePath(nodes);
			tree.expandPath(path);
		}
		this.nodesToExpand.clear();
		this.nodesToExpand = null;
	}
	
	public static void main(String args[])
	{
		Connection con = null;
		try
		{
			Class.forName("com.inet.tds.TdsDriver");
			//Class.forName("oracle.jdbc.OracleDriver");
			con = DriverManager.getConnection("jdbc:inetdae:demsqlvisa02:1433?database=visa_cpl_test", "visa", "savivisa");
			//con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:oradb", "auto", "auto");
			WbConnection wb = new WbConnection(con);
			
			JFrame f = new JFrame("Test");
      TableDependencyTreeDisplay display = new TableDependencyTreeDisplay();
			display.setConnection(wb);
			display.readTree("visa_cpl_test", "dbo", "visa_bid");
			f.getContentPane().add(display);
			f.pack();
			f.show();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { con.close(); } catch (Throwable th) {}
		}
	}
	
}
