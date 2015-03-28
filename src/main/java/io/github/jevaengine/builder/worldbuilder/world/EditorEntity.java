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

import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.graphics.IFont;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.graphics.IFontFactory.FontConstructionException;
import io.github.jevaengine.graphics.NullFont;
import io.github.jevaengine.math.Rect3F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.NullObservers;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.DefaultWorldFactory.WorldConfiguration.EntityImportDeclaration;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityTaskModel;
import io.github.jevaengine.world.entity.NullEntityTaskModel;
import io.github.jevaengine.world.entity.WorldAssociationException;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.NonparticipantPhysicsBody;
import io.github.jevaengine.world.scene.model.DecoratedSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel.ISceneModelComponent;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory.SceneModelConstructionException;
import io.github.jevaengine.world.scene.model.NullSceneModel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EditorEntity
{
	private static final URI ENTITY_LABEL_FONT = URI.create("local:///ui/font/pro/pro.juif");
	private static final URI ENTITY_MODEL = URI.create("local:///entity/entity.jmf");
	
	private final Logger m_logger = LoggerFactory.getLogger(EditorEntity.class);
	
	private DummyEntity m_dummy;

	private String m_name;
	private String m_className;
	
	@Nullable
	private URI m_config;
	
	private ISceneModel m_sceneModel;
	
	private JsonVariable m_auxConfig = new JsonVariable();
	
	private IFont m_font;
	
	public EditorEntity(IFontFactory fontFactory, ISceneModelFactory modelFactory, String name, String className, @Nullable URI config)
	{
		m_font = new NullFont();
		m_sceneModel = new NullSceneModel();
		
		try
		{
			m_font = fontFactory.create(ENTITY_LABEL_FONT, Color.white);
		} catch (FontConstructionException e)
		{
			m_logger.error("Unable to construct font to render entity details. Using NullFont", e);
		}
		
		try
		{
			m_sceneModel = modelFactory.create(ENTITY_MODEL);
		} catch (SceneModelConstructionException e)
		{
			m_logger.error("Unable to construct entity sprite. Using NullSceneGraphic border", e);
		}
		
		m_name = name;
		m_className = className;
		m_config = config;
		
		m_dummy = new DummyEntity();
	}
	
	public EditorEntity(IFontFactory fontFactory, ISceneModelFactory modelFactory, String name, String className)
	{
		this(fontFactory, modelFactory, name, className, null);
	}
	
	public void setAuxiliaryConfig(JsonVariable config)
	{
		m_auxConfig = config;
	}
	
	public JsonVariable getAuxiliaryConfig()
	{
		return m_auxConfig;
	}
	
	public void setName(String name)
	{
		m_name = name;
	}

	public String getName()
	{
		return m_name;
	}

	public void setClassName(String className)
	{
		m_className = className;
	}

	public String getClassName()
	{
		return m_className;
	}

	@Nullable
	public URI getConfig()
	{
		return m_config;
	}

	public void setConfig(@Nullable URI config)
	{
		m_config = config;
	}
	
	public void clearConfig()
	{
		setConfig(null);
	}
	
	public Vector3F getLocation()
	{
		return m_dummy.getBody().getLocation();
	}
	
	public void setLocation(Vector3F location)
	{
		m_dummy.getBody().setLocation(location);
	}
	
	public Direction getDirection()
	{
		return m_dummy.getBody().getDirection();
	}
	
	public void setDirection(Direction direction)
	{
		m_dummy.getBody().setDirection(direction);
	}

	protected DummyEntity getEntity()
	{
		return m_dummy;
	}
	
	public EntityImportDeclaration createImportDeclaration()
	{
		EntityImportDeclaration entityDecl = new EntityImportDeclaration();
		entityDecl.config = m_config == null ? null : m_config.toString();
		entityDecl.direction = getDirection();
		entityDecl.location = getLocation();
		entityDecl.type = getClassName();
		entityDecl.name = getName();
		entityDecl.auxConfig = m_auxConfig;
		return entityDecl;
	}

	@Override
	public String toString()
	{
		return String.format("%s of %s", m_name, m_className + (m_config != null ? " with " + m_config : ""));
	}
	
	public class DummyEntity implements IEntity
	{
		private final IPhysicsBody m_body = new NonparticipantPhysicsBody(this);
		private World m_world;
		private final EntityBridge m_bridge;
		
		private DummyEntity()
		{
			m_bridge = new EntityBridge(this);
		}
		
		public EditorEntity getEditorEntity()
		{
			return EditorEntity.this;
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
			return new DecoratedSceneModel(m_sceneModel, new ISceneModelComponent[] {
					new ISceneModelComponent() {
						
						@Override
						public void render(Graphics2D g, int x, int y, float scale) {
							m_font.drawText(g, x, y, scale, getInstanceName());
						}
						
						@Override
						public boolean testPick(int x, int y, float scale) {
							return false;
						}
						
						@Override
						public Rect3F getBounds() {
							Vector3F mountPoint = m_sceneModel.getAABB().getPoint(0.5F, 1.1F, 0.0F);
							return new Rect3F(mountPoint, 0, 0, 0);
						}

						@Override
						public String getName()
						{
							return this.getClass().getName();
						}
						
						@Override
						public Vector3F getOrigin()
						{
							return new Vector3F();
						}
					}
			});
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
	}
}

