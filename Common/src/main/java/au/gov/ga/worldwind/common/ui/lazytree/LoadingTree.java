/*******************************************************************************
 * Copyright 2012 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package au.gov.ga.worldwind.common.ui.lazytree;

import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * {@link JTree} subclass that detects loading tree nodes and ensures the
 * {@link JTree#imageUpdate(Image, int, int, int, int, int)} function returns
 * the correct result when there are loading nodes.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class LoadingTree extends JTree
{
	public LoadingTree()
	{
	}

	public LoadingTree(TreeModel model)
	{
		super(model);
	}

	@Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h)
	{
		//Bug in SDK: the loading icon will continuously call repaint on JTree
		//(even if loadingIcon.setImageObserver(null) is called) unless this
		//method returns false if painting is not required. This is why we keep
		//track of any nodes using the loading icon (aka loadingNodes).

		boolean anyLoading = false;
		for (int i = 0; i < getRowCount(); i++)
		{
			TreePath path = getPathForRow(i);
			if (path != null)
			{
				Object node = path.getLastPathComponent();
				if (isLoading(node))
				{
					Rectangle bounds = getRowBounds(i);
					if (bounds != null)
					{
						repaint(bounds);
					}
					anyLoading = true;
				}
			}
		}
		return anyLoading;
	}

	protected boolean isLoading(Object node)
	{
		if (node instanceof ILoadingNode)
		{
			return ((ILoadingNode) node).isLoading();
		}
		else if (node instanceof DefaultMutableTreeNode)
		{
			DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) node;
			Object o = dmtn.getUserObject();
			if (o != null && o instanceof ILoadingNode)
				return ((ILoadingNode) o).isLoading();
		}
		return false;
	}
}
