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

import io.github.jevaengine.builder.worldbuilder.world.EditorEntity.DummyEntity;
import io.github.jevaengine.builder.worldbuilder.world.EditorSceneArtifact.DummySceneArtifact;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorldFactory.EditorWeatherFactory.EditorWeather;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Rect3F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.NullObservers;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.DefaultWorldFactory.WorldConfiguration;
import io.github.jevaengine.world.DefaultWorldFactory.WorldConfiguration.EntityImportDeclaration;
import io.github.jevaengine.world.DefaultWorldFactory.WorldConfiguration.SceneArtifactImportDeclaration;
import io.github.jevaengine.world.DefaultWorldFactory.WorldConfiguration.ZoneDeclaration;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityTaskModel;
import io.github.jevaengine.world.entity.NullEntityTaskModel;
import io.github.jevaengine.world.entity.WorldAssociationException;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.NonparticipantPhysicsBody;
import io.github.jevaengine.world.physics.NullPhysicsBody;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.NullSceneModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EditorWorld
{
	private final World m_world;
	
	private final WorldCursor m_worldCursor;
	private final ArrayList<EditorEntity> m_entities = new ArrayList<>();
	private final ArrayList<EditorZone> m_zones = new ArrayList<>();
	private final HashMap<TileLocation, EditorSceneArtifact> m_tiles = new HashMap<>();
	
	private float m_metersPerUnit = 0;
	private float m_logicPerUnit = 0;
	private float m_maxFrictionForce = 0;
	
	private String m_script = "";
	
	protected EditorWorld(World world, IFontFactory fontFactory)
	{
		m_maxFrictionForce = world.getPhysicsWorld().getMaxFrictionForce();
		m_metersPerUnit = world.getMetersPerUnit();
		m_logicPerUnit = world.getLogicPerUnit();
		
		m_world = world;
		m_worldCursor = new WorldCursor(world);
		
		for(IEntity e : world.getEntities().all())
		{
			if(e instanceof DummySceneArtifact)
				m_tiles.put(new TileLocation(e.getBody().getLocation()), ((DummySceneArtifact)e).getEditorTile());
			else if(e instanceof DummyEntity)
				m_entities.add(((DummyEntity)e).getEditorEntity());
		}
		
		for(Map.Entry<String, Rect3F> zone : world.getZones().entrySet())
		{
			Rect3F bounds = zone.getValue();
			
			EditorZone editorZone = new EditorZone(fontFactory, zone.getKey());
			editorZone.setLocation(bounds.getPoint(0, 0, 0));
			editorZone.setBounds(new Rect3F(0, 0, 0, bounds.width, bounds.height, bounds.depth));
			
			m_world.addEntity(editorZone.getEntity());
			m_zones.add(editorZone);
		}
	}

	private TileLocation getBoundedTileLocation(Vector3F location)
	{
		Rect2D worldBounds = m_world.getBounds();
	
		return new TileLocation(new Vector3F(Math.min(Math.max((float)worldBounds.x, location.x), worldBounds.x + worldBounds.width - 1),
												Math.min(Math.max((float)worldBounds.y, location.y), worldBounds.y + worldBounds.height - 1),
												location.z));
	}
	
	World getWorld()
	{
		return m_world;
	}
	
	public WorldCursor getCursor()
	{
		return m_worldCursor;
	}
	
	@Nullable
	public EditorWeather getWeather()
	{
		if(m_world.getWeather() instanceof EditorWeather)
			return (EditorWeather)m_world.getWeather();
		
		return null;
	}

	public void setWeather(EditorWeather weather)
	{
		m_world.setWeather(weather);
	}
	
	public float getFriction()
	{
		return m_maxFrictionForce;
	}
	
	public void setFriction(float maxFrictionForce)
	{
		m_maxFrictionForce = maxFrictionForce;
	}
	
	public List<EditorEntity> getEntities()
	{
		return new ArrayList<>(m_entities);
	}
	
	public List<EditorZone> getZones()
	{
		return new ArrayList<>(m_zones);
	}
	
	public void addEntity(EditorEntity e)
	{
		m_entities.add(e);
		m_world.addEntity(e.getEntity());
	}
	
	public void removeEntity(EditorEntity e)
	{
		m_entities.remove(e);
		m_world.removeEntity(e.getEntity());
	}

	public void addZone(EditorZone zone)
	{
		m_zones.add(zone);
		m_world.addEntity(zone.getEntity());
	}
	
	public void removeZone(EditorZone zone)
	{
		m_zones.remove(zone);
		m_world.removeEntity(zone.getEntity());
	}
	
	public void setTile(@Nullable EditorSceneArtifact t, Vector3F location)
	{
		TileLocation tileLocation = getBoundedTileLocation(location);
		
		if(t == null)
		{
			EditorSceneArtifact tile = m_tiles.get(tileLocation);
			
			if(tile != null)
			{
				m_world.removeEntity(tile.getEntity());
				m_tiles.remove(tileLocation);
			}
			
		} else
		{
			clearTile(location);			
			m_world.addEntity(t.getEntity());
			t.setLocation(tileLocation.getLocation());
			m_tiles.put(tileLocation, t);
		}
	}
	
	@Nullable
	public EditorSceneArtifact getTile(Vector3F location)
	{
		return m_tiles.get(new TileLocation(location));
	}
	
	public void clearTile(Vector3F location)
	{
		setTile(null, location);
	}
	
	public String getScript()
	{
		return m_script;
	}
	
	public void setScript(String script)
	{
		m_script = script;
	}
	
	private void serializeTiledLayers(WorldConfiguration hostConfiguration)
	{
		Map<EditorSceneArtifact, List<Vector3F>> importedArtifacts = new HashMap<>();
		
		for(Map.Entry<TileLocation, EditorSceneArtifact> e : m_tiles.entrySet())
		{
			if(!importedArtifacts.containsKey(e.getValue()))
				importedArtifacts.put(e.getValue(), new ArrayList<Vector3F>());
		
			importedArtifacts.get(e.getValue()).add(e.getKey().getLocation());
		}
	
		hostConfiguration.artifactImports = new SceneArtifactImportDeclaration[importedArtifacts.size()];
		
		Iterator<Map.Entry<EditorSceneArtifact, List<Vector3F>>> it = importedArtifacts.entrySet().iterator();
		for(int i = 0; i < importedArtifacts.size(); i++)
		{
			Map.Entry<EditorSceneArtifact, List<Vector3F>> e = it.next();
			SceneArtifactImportDeclaration decl = new SceneArtifactImportDeclaration();
			hostConfiguration.artifactImports[i] = decl;
			
			decl.direction = e.getKey().getDirection();
			decl.isTraversable = e.getKey().isTraversable();
			decl.model = e.getKey().getModelName().toString();
			decl.locations = new Vector3F[e.getValue().size()];
			
			for(int x = 0; x < e.getValue().size(); x++)
				decl.locations[x] = new Vector3F(e.getValue().get(x));
		}
	}
	
	private void serializeEntities(WorldConfiguration hostConfiguration)
	{
		ArrayList<EntityImportDeclaration> entities = new ArrayList<>();
		
		for(EditorEntity e : m_entities)
			entities.add(e.createImportDeclaration());
		
		hostConfiguration.entities = entities.toArray(new EntityImportDeclaration[entities.size()]);
	}
	
	private void serializeZones(WorldConfiguration hostConfiguration)
	{
		ArrayList<ZoneDeclaration> zones = new ArrayList<>();
		
		for(EditorZone z : m_zones)
			zones.add(z.createZoneDeclaration());
		
		hostConfiguration.zones = zones.toArray(new ZoneDeclaration[zones.size()]);
	}
	
	public WorldConfiguration createWorldConfiguration()
	{
		WorldConfiguration configuration = new WorldConfiguration();

		configuration.weather = m_world.getWeather() instanceof EditorWeather ? ((EditorWeather)m_world.getWeather()).getName().toString() : null;
		configuration.friction = m_maxFrictionForce;
		configuration.metersPerUnit = m_metersPerUnit;
		configuration.logicPerUnit = m_logicPerUnit;
		configuration.worldWidth = m_world.getBounds().width;
		configuration.worldHeight = m_world.getBounds().height;
		
		if(!m_script.isEmpty())
			configuration.script = m_script;
		
		serializeEntities(configuration);
		serializeZones(configuration);
		serializeTiledLayers(configuration);
		
		return configuration;
	}
	
	public static final class WorldCursor
	{
		private final World m_hostWorld;
		private final CursorEntity m_cursorEntity = new CursorEntity();
		
		private WorldCursor(World host)
		{
			m_hostWorld = host;
			m_hostWorld.addEntity(m_cursorEntity);
			setLocation(new Vector3F());
		}
		
		public void setLocation(Vector3F location)
		{
			Rect2D worldBounds = m_hostWorld.getBounds();
			
			Vector3F boundedLocation = new Vector3F(location);
			boundedLocation.x = Math.min(worldBounds.width - 1, Math.max(0, boundedLocation.x));
			boundedLocation.y = Math.min(worldBounds.height - 1, Math.max(0, boundedLocation.y));
			
			m_cursorEntity.getBody().setLocation(boundedLocation);
		}
		
		public Vector3F getLocation()
		{
			return m_cursorEntity.getBody().getLocation();
		}
		
		public void setModel(IImmutableSceneModel model)
		{
			m_cursorEntity.setModel(model);
		}
		
		public void clearModel()
		{
			setModel(new NullSceneModel());
		}
		
		IEntity getEntity()
		{
			return m_cursorEntity;
		}
		
		private static class CursorEntity implements IEntity
		{	
			@Nullable
			private IImmutableSceneModel m_model = new NullSceneModel();
			
			private IPhysicsBody m_body = new NullPhysicsBody();
			private World m_world;
			
			@Override
			public void dispose()
			{
				if(m_world != null)
					m_world.removeEntity(this);
			}
			
			public void setModel(IImmutableSceneModel model)
			{
				m_model = model;
			}
			
			@Override
			public IImmutableSceneModel getModel()
			{
				return m_model;
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
				m_body = new NonparticipantPhysicsBody(this);
			}

			@Override
			public void disassociate()
			{
				if(m_world == null)
					throw new WorldAssociationException("Entity already dissociated from world.");
			
				m_world = null;
				m_body = new NullPhysicsBody();
			}

			@Override
			public String getInstanceName()
			{
				return this.getClass().getName();
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
				return new EntityBridge(this);
			}

			@Override
			public void update(int delta) { }
			
			@Override
			public boolean isStatic()
			{
				return true;
			}
			
			@Override
			public IEntityTaskModel getTaskModel()
			{
				return new NullEntityTaskModel();
			}
		}
	}
	
	private static final class KeyableFloat
	{
		public static final float TOLERANCE = 0.0001F;
		
		private final float m_value;
		
		public KeyableFloat(float value)
		{
			//Round to fourth decimal place.
			m_value = Math.round(value * (1.0F/TOLERANCE)) / (1.0F/TOLERANCE);
		}
		
		public float getFloatValue()
		{
			return m_value;
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + (int)Math.round(m_value);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyableFloat other = (KeyableFloat) obj;
			if (Math.abs(other.m_value - m_value) > TOLERANCE)
				return false;
			return true;
		}
	}
	
	private static final class TileLocation
	{
		private final KeyableFloat m_x;
		private final KeyableFloat m_y;
		private final KeyableFloat m_z;
		
		public TileLocation(Vector3F location)
		{
			m_x = new KeyableFloat(location.x);
			m_y = new KeyableFloat(location.y);
			m_z = new KeyableFloat(location.z);
		}

		public Vector3F getLocation()
		{
			return new Vector3F(m_x.getFloatValue(), m_y.getFloatValue(), m_z.getFloatValue());
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 67 * hash + Objects.hashCode(this.m_x);
			hash = 67 * hash + Objects.hashCode(this.m_y);
			hash = 67 * hash + Objects.hashCode(this.m_z);
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final TileLocation other = (TileLocation) obj;
			if (!Objects.equals(this.m_x, other.m_x)) {
				return false;
			}
			if (!Objects.equals(this.m_y, other.m_y)) {
				return false;
			}
			if (!Objects.equals(this.m_z, other.m_z)) {
				return false;
			}
			return true;
		}

		
	}
	
	public static final class UnrecognizedWorldEntityException extends RuntimeException
	{
		private static final long serialVersionUID = 1L;
	
		protected UnrecognizedWorldEntityException() { }
	}
}
