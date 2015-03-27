/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.jevaengine.builder.worldbuilder.world;

import io.github.jevaengine.math.Matrix3X3;
import io.github.jevaengine.math.Rect3F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
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
	public boolean isSizable()
	{
		return false;
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
	
	public interface IResizeZoneBrushBehaviourHandler
	{
		void resized();
	}
}
