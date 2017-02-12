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

import io.github.jevaengine.math.Matrix3X3;
import io.github.jevaengine.math.Rect3F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.physics.PhysicsBodyShape;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public final class ResizeZoneBrushBehaviour implements IBrushBehaviour
{
	private final EditorZone m_zone;
	private final IResizeZoneBrushBehaviourHandler m_handler;
	
	public ResizeZoneBrushBehaviour(EditorZone zone, IResizeZoneBrushBehaviourHandler handler)
	{
		m_zone = zone;
		m_handler = handler;
	}

	@Override
	public IImmutableSceneModel getModel()
	{
		return new IImmutableSceneModel() {
			@Override
			public ISceneModel clone() throws IImmutableSceneModel.SceneModelNotCloneableException
			{
				throw new SceneModelNotCloneableException(new Exception("Not cloneable."));
			}

			@Override
			public PhysicsBodyShape getBodyShape()
			{
				return new PhysicsBodyShape();
			}
			
			@Override
			public Collection<ISceneModelComponent> getComponents(final Matrix3X3 projection)
			{
				List<ISceneModelComponent> components = new ArrayList<>();
				
				components.add(new ISceneModelComponent() {

					@Override
					public String getName()
					{
						return this.getClass().getName();
					}

					@Override
					public boolean testPick(int x, int y, float scale)
					{
						return false;
					}

					@Override
					public Rect3F getBounds()
					{
						return new Rect3F();
					}

					@Override
					public Vector3F getOrigin()
					{
						return new Vector3F();
					}

					@Override
					public void render(Graphics2D g, int x, int y, float scale)
					{
						g.setColor(Color.red);
						g.fillOval(x - 2, y - 2, 4, 4);
					}
				});
				
				return components;
			}

			@Override
			public Rect3F getAABB()
			{
				return new Rect3F();
			}

			@Override
			public Direction getDirection()
			{
				return Direction.XYPlus;
			}
		};
	}

	@Override
	public void apply(EditorWorld world, Vector3F location)
	{
		Vector3F a = m_zone.getLocation();
		Vector3F difference = location.difference(a);
		
		Rect3F bounds = new Rect3F(0, 0, 0, a.x, a.y, a.z);
		bounds.width = Math.max(0, difference.x);
		bounds.height = Math.max(0, difference.y);
		bounds.depth = Math.max(0, difference.z);
		
		m_zone.setBounds(bounds);
		m_handler.resized();
	}

	@Override
	public void setDirection(Direction d) { }

	@Override
	public Direction getDirection()
	{
		return Direction.Zero;
	}
	
	public interface IResizeZoneBrushBehaviourHandler
	{
		void resized();
	}
}
