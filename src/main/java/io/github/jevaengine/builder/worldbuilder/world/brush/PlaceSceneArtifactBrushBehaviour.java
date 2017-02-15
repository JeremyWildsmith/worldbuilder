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

import io.github.jevaengine.builder.worldbuilder.world.EditorSceneArtifact;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorld;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import java.net.URI;

public final class PlaceSceneArtifactBrushBehaviour implements IBrushBehaviour
{
	private final ISceneModel m_model;
	private final URI m_modelName;
	private final boolean m_isTraversable;
	private final boolean m_isStatic;
	
	public PlaceSceneArtifactBrushBehaviour(IImmutableSceneModel model, URI modelName, Direction direction, boolean isTraversable, boolean isStatic)
	{
		m_model = model.clone();
		m_modelName = modelName;
		m_isTraversable = isTraversable;
		m_isStatic = isStatic;
		
		m_model.setDirection(direction);
	}
	
	@Override
	public IImmutableSceneModel getModel()
	{
		return m_model;
	}
	
	@Override
	public void apply(EditorWorld world, Vector3F location)
	{
		ISceneModel model = m_model.clone();
		world.setTile(new EditorSceneArtifact(model, m_modelName, model.getDirection(), m_isTraversable, m_isStatic), location);
	}

	@Override
	public void setDirection(Direction d)
	{
		m_model.setDirection(d);
	}

	@Override
	public Direction getDirection()
	{
		return m_model.getDirection();
	}
}
