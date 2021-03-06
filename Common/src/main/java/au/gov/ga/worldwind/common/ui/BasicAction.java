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
package au.gov.ga.worldwind.common.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

/**
 * Basic concrete implementation of {@link AbstractAction}. Has a name, icon,
 * and tooltip.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class BasicAction extends AbstractAction
{
	private List<ActionListener> listeners = new ArrayList<ActionListener>();

	public BasicAction(String name, Icon icon)
	{
		this(name, name, icon);
	}

	public BasicAction(String name, String toolTipText, Icon icon)
	{
		super(name, icon);
		setToolTipText(toolTipText);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).actionPerformed(e);
	}

	/**
	 * Add a listener that is notified when this action is performed.
	 * 
	 * @param listener
	 *            Listener to add
	 */
	public void addActionListener(ActionListener listener)
	{
		listeners.add(listener);
	}

	/**
	 * Remove a listener from this action.
	 * 
	 * @param listener
	 */
	public void removeActionListener(ActionListener listener)
	{
		listeners.remove(listener);
	}

	public String getName()
	{
		return (String) getValue(Action.NAME);
	}

	public void setName(String name)
	{
		putValue(Action.NAME, name);
	}

	public Icon getIcon()
	{
		return (Icon) getValue(Action.SMALL_ICON);
	}

	public void setIcon(Icon icon)
	{
		putValue(Action.SMALL_ICON, icon);
	}

	public String getToolTipText()
	{
		return (String) getValue(Action.SHORT_DESCRIPTION);
	}

	public void setToolTipText(String toolTipText)
	{
		putValue(Action.SHORT_DESCRIPTION, toolTipText);
	}
}
