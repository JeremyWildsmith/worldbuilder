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

import io.github.jevaengine.graphics.IFont;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.graphics.IFontFactory.FontConstructionException;
import io.github.jevaengine.graphics.NullFont;
import io.github.jevaengine.math.*;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.NullObservers;
import io.github.jevaengine.world.DefaultWorldFactory.WorldConfiguration.ZoneDeclaration;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityTaskModel;
import io.github.jevaengine.world.entity.NullEntityTaskModel;
import io.github.jevaengine.world.entity.WorldAssociationException;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.NonparticipantPhysicsBody;
import io.github.jevaengine.world.physics.PhysicsBodyShape;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URI;
import java.util.*;
import java.util.List;

public final class EditorZone
{
	private static final URI ENTITY_LABEL_FONT = URI.create("local:///ui/font/pro/pro.juif");
	
	private final Logger m_logger = LoggerFactory.getLogger(EditorZone.class);
	
	private DummyZone m_dummy;

	private String m_name;
	private Rect3F m_bounds = new Rect3F();
	
	private IFont m_font;
	
	public EditorZone(IFontFactory fontFactory, String name)
	{
		m_font = new NullFont();
		
		try
		{
			m_font = fontFactory.create(ENTITY_LABEL_FONT, Color.white);
		} catch (FontConstructionException e)
		{
			m_logger.error("Unable to construct font to render entity details. Using NullFont", e);
		}
		
		m_name = name;
		m_dummy = new DummyZone();
	}
	
	public void setName(String name)
	{
		m_name = name;
	}

	public String getName()
	{
		return m_name;
	}
	
	public Rect3F getBounds()
	{
		return new Rect3F(m_bounds);
	}
	
	public void setBounds(Rect3F bounds)
	{
		m_bounds = new Rect3F(bounds);
	}

	public Vector3F getLocation()
	{
		return m_dummy.getBody().getLocation();
	}
	
	public void setLocation(Vector3F location)
	{
		m_dummy.getBody().setLocation(location);
	}

	public DummyZone getEntity()
	{
		return m_dummy;
	}

	public ZoneDeclaration createZoneDeclaration()
	{
		ZoneDeclaration zoneDecl = new ZoneDeclaration();
		zoneDecl.name = m_name;
		zoneDecl.region = new Rect3F(getLocation(), m_bounds.width, m_bounds.height, m_bounds.depth);
		return zoneDecl;
	}
	
	@Override
	public String toString()
	{
		return "Zone: " + getName();
	}
	
	public class DummyZone implements IEntity
	{
		private final IPhysicsBody m_body = new NonparticipantPhysicsBody(this);
		private World m_world;
		private final EntityBridge m_bridge;
		
		private DummyZone()
		{
			m_bridge = new EntityBridge(this);
		}
		
		public EditorZone getEditorEntity()
		{
			return EditorZone.this;
		}
		
		@Override
		public void dispose()
		{
			if(m_world != null)
				m_world.removeEntity(this);
		}
		
		@Override
		public IImmutableSceneModel getModel()
		{
			return new EditorZoneModel();
		}

		@Override
		public World getWorld()
		{
			return m_world;
		}

		@Override
		public void associate(World world)
		{
			if(m_world != null)
				throw new WorldAssociationException("Entity already associated to world.");
		
			m_world = world;
		}

		@Override
		public void disassociate()
		{
			if(m_world == null)
				throw new WorldAssociationException("Entity already dissociated from world.");
		
			m_world = null;
		}

		@Override
		public String getInstanceName()
		{
			return m_name;
		}

		@Override
		public Map<String, Integer> getFlags()
		{
			return new HashMap<>();
		}

		@Override
		public IPhysicsBody getBody()
		{
			return m_body;
		}

		@Override
		public IObserverRegistry getObservers()
		{
			return new NullObservers();
		}
		
		@Override
		public EntityBridge getBridge()
		{
			return m_bridge;
		}

		@Override
		public void update(int delta) { }

		@Override
		public boolean isStatic() {
			return true;
		}

		@Override
		public IEntityTaskModel getTaskModel()
		{
			return new NullEntityTaskModel();
		}
		
		private  final class EditorZoneModel implements IImmutableSceneModel
		{
			@Override
			public ISceneModel clone() throws SceneModelNotCloneableException
			{
				throw new SceneModelNotCloneableException(new Exception("Editor zone model cannot be cloned."));
			}
			
			public PhysicsBodyShape getBodyShape()
			{
				return new PhysicsBodyShape();
			}
			
			@Override
			public Collection<ISceneModelComponent> getComponents(Matrix3X3 projection)
			{
				List<ISceneModelComponent> components = new ArrayList<>();
				
				Rect3F leftWall = new Rect3F(m_bounds);
				Rect3F rightWall = new Rect3F(m_bounds);
				Rect3F backWall = new Rect3F(m_bounds);
				Rect3F frontWall = new Rect3F(m_bounds);
				
				leftWall.width = 0;
				backWall.height = 0;
				
				rightWall.x = rightWall.width;
				rightWall.width = 0;
				frontWall.y = frontWall.height;
				frontWall.height = 0;
				
				components.add(new EditorZoneModelComponentWall(leftWall, projection));
				components.add(new EditorZoneModelComponentWall(backWall, projection));
				components.add(new EditorZoneModelComponentWall(rightWall, projection));
				components.add(new EditorZoneModelComponentWall(frontWall, projection));
				components.add(new EditorZoneModelComponentTag());
				
				return components;
			}

			@Override
			public Rect3F getAABB()
			{
				return m_bounds;
			}

			@Override
			public Direction getDirection()
			{
				return Direction.XYPlus;
			}
			
			private final class EditorZoneModelComponentTag implements ISceneModelComponent
			{
				@Override
				public String getName()
				{
					return this.getClass().getName();
				}

				@Override
				public boolean testPick(int x, int y, float scale)
				{
					return m_font.getTextBounds(EditorZone.this.toString(), scale).contains(new Vector2D(x, y));
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
					Rect2D bounds = m_font.getTextBounds(EditorZone.this.toString(), scale);
					
					g.setColor(Color.red);
					g.fillRect(x - 2, y - 2, bounds.width + 4, bounds.height + 4);
					
					g.setColor(Color.white);
					g.drawRect(x - 2, y - 2, bounds.width + 4, bounds.height + 4);
					
					m_font.drawText(g, x, y, scale, EditorZone.this.toString());
				}
			}
			
			private class EditorZoneModelComponentWall implements ISceneModelComponent
			{
				private final Rect3F m_bounds;
				private final Matrix3X3 m_projection;
				
				public EditorZoneModelComponentWall(Rect3F bounds, Matrix3X3 projection)
				{
					m_bounds = new Rect3F(bounds);
					m_projection = projection;
				}
				
				@Override
				public final String getName()
				{
					return this.getClass().getName();
				}

				@Override
				public final boolean testPick(int x, int y, float scale)
				{
					return false;
				}

				@Override
				public final Rect3F getBounds()
				{
					Rect3F bounds = new Rect3F(m_bounds);
					bounds.x = bounds.y = bounds.z = 0;
					return bounds;
				}
				
				@Override
				public final Vector3F getOrigin()
				{
					return m_bounds.getPoint(0, 0, 0);
				}
				
				private Vector2D translateWorldToScreen(Vector3F v, float scale)
				{
					return m_projection.scale(scale).dot(v).getXy().round();
				}

				@Override
				public final void render(Graphics2D g, int offsetX, int offsetY, float scale)
				{
					Rect3F aabb = new Rect3F(getBounds());
					aabb.x = aabb.y = aabb.z = 0; //Our origin already displaces the model. No need to account for displacement here.
					
					//bottom face
					Vector2D bfA = translateWorldToScreen(aabb.getPoint(0, 1.0F, 0), scale);
					Vector2D bfB = translateWorldToScreen(aabb.getPoint(1.0F, 1.0F, 0), scale);
					Vector2D bfC = translateWorldToScreen(aabb.getPoint(1.0F, 0, 0), scale);

					//top face
					Vector2D tfA = translateWorldToScreen(aabb.getPoint(0, 1.0F, 1.0F), scale);
					Vector2D tfB = translateWorldToScreen(aabb.getPoint(1.0F, 1.0F, 1.0F), scale);
					Vector2D tfC = translateWorldToScreen(aabb.getPoint(1.0F, 0, 1.0F), scale);

					g.setColor(Color.RED);
					Stroke oldStroke = g.getStroke();
					g.setStroke(new BasicStroke(2.0F * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10.0F * scale, new float[] {10.0F * scale}, 0));
					
					g.drawLine(offsetX + bfA.x, offsetY + bfA.y, offsetX + bfB.x, offsetY + bfB.y);
					g.drawLine(offsetX + bfB.x, offsetY + bfB.y, offsetX + bfC.x, offsetY + bfC.y);

					g.drawLine(offsetX + tfA.x, offsetY + tfA.y, offsetX + tfB.x, offsetY + tfB.y);
					g.drawLine(offsetX + tfB.x, offsetY + tfB.y, offsetX + tfC.x, offsetY + tfC.y);

					g.drawLine(offsetX + bfA.x, offsetY + bfA.y, offsetX + tfA.x, offsetY + tfA.y);
					g.drawLine(offsetX + bfB.x, offsetY + bfB.y, offsetX + tfB.x, offsetY + tfB.y);
					g.drawLine(offsetX + bfC.x, offsetY + bfC.y, offsetX + tfC.x, offsetY + tfC.y);
					
					g.setStroke(oldStroke);
				}
			}
		}
	}
}

