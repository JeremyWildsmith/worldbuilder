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

import io.github.jevaengine.builder.worldbuilder.world.EditorEntity;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorld;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

/**
 *
 * @author Jeremy
 */
public class MoveEntityBrushBehaviour implements IBrushBehaviour
{
	private final float MIN_ENTITY_DISTANCE = 0.0001F;
	private final IEntity m_entity;
	private final IEntityMovementBrushBehaviorHandler m_movementHandler;
	
	public MoveEntityBrushBehaviour(IEntity entity, IEntityMovementBrushBehaviorHandler movementHandler)
	{
		m_entity = entity;
		m_movementHandler = movementHandler;
	}

	@Override
	public IImmutableSceneModel getModel()
	{
		return m_entity.getModel();
	}

	@Override
	public void apply(EditorWorld world, Vector3F location)
	{
		for(EditorEntity e : world.getEntities()) {
			Vector3F delta = e.getLocation().difference(location);
			if(delta.getLength() < MIN_ENTITY_DISTANCE)
				return;
		}
		m_entity.getBody().setLocation(location);
		m_movementHandler.moved();
	}

	@Override
	public void setDirection(Direction d) { }

	@Override
	public Direction getDirection()
	{
		return m_entity.getBody().getDirection();
	}
	
	public interface IEntityMovementBrushBehaviorHandler
	{
		void moved();
	}
}
