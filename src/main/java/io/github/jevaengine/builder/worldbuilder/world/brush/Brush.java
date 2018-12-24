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
package io.github.jevaengine.builder.worldbuilder.world.brush;

import io.github.jevaengine.builder.worldbuilder.world.EditorWorld;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;

public final class Brush
{
	private IBrushBehaviour m_behaviour = new NullBrushBehaviour();

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
	
	public void apply(EditorWorld world)
	{
		Vector3F location = world.getEditCursor().getLocation();
		m_behaviour.apply(world, location);
	}
	
	public void setDirection(Direction d)
	{
		m_behaviour.setDirection(d);
	}
	
	public Direction getDirection()
	{
		return m_behaviour.getDirection();
	}
	
	public interface IBrushBehaviorObserver
	{
		void behaviourChanged(IBrushBehaviour behaviour);
	}
}