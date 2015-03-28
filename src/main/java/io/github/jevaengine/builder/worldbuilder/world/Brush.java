/* 
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package io.github.jevaengine.builder.worldbuilder.world;

import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;

public final class Brush
{
	private IBrushBehaviour m_behaviour = new NullBrushBehaviour();
	private int m_size = 1;

	private final Observers m_observers = new Observers();
	
	public IObserverRegistry getObservers()
	{
		return m_observers;
	}
	
	public void setBehaviour(IBrushBehaviour behaviour)
	{
		m_behaviour = behaviour;
		m_observers.raise(IBrushBehaviorObserver.class).behaviourChanged(behaviour);
	}
	
	public int getSize()
	{
		return m_size;
	}
	
	public void setSize(int size)
	{
		m_size = size;
	}
	
	public void apply(EditorWorld world)
	{
		Vector3F location = world.getCursor().getLocation();
		
		if(m_behaviour.isSizable())
		{
			for(int y = Math.max(0, Math.round(location.y) - m_size / 2); y <= Math.round(location.y) + m_size / 2 && y < world.getWorld().getBounds().width; y++)
			{
				for(int x = Math.max(0, Math.round(location.x) - m_size / 2); x <= Math.round(location.x) + m_size / 2 && y < world.getWorld().getBounds().height; x++)
					m_behaviour.apply(world, new Vector3F(x, y, location.z));
			}
		} else
		{
			m_behaviour.apply(world, location);
		}
	}
	
	public interface IBrushBehaviorObserver
	{
		void behaviourChanged(IBrushBehaviour behaviour);
	}
}