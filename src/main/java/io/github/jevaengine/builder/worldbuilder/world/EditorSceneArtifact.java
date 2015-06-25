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
import io.github.jevaengine.util.NullObservers;
import io.github.jevaengine.world.DefaultWorldFactory.WorldConfiguration.SceneArtifactImportDeclaration;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityTaskModel;
import io.github.jevaengine.world.entity.NullEntityTaskModel;
import io.github.jevaengine.world.entity.WorldAssociationException;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.NonparticipantPhysicsBody;
import io.github.jevaengine.world.physics.NullPhysicsBody;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class EditorSceneArtifact
{
	private static final AtomicInteger m_unnamedCount = new AtomicInteger();
	
	private URI m_sceneModelName;
	private ISceneModel m_sceneModel;
	private Direction m_direction;
	private boolean m_isTraversable;
	private final boolean m_isStatic;
	
	private final DummySceneArtifact m_dummy;

	public EditorSceneArtifact(IImmutableSceneModel sceneModel, URI sceneModelName, Direction direction, boolean isTraversable, boolean isStatic)
	{
		m_direction = direction;
		m_isTraversable = isTraversable;
		m_sceneModelName = sceneModelName;
		m_sceneModel = sceneModel.clone();
		m_isStatic = isStatic;
		m_dummy = new DummySceneArtifact(this.getClass().getName() + m_unnamedCount.getAndIncrement());
	}

	protected DummySceneArtifact getEntity()
	{
		return m_dummy;
	}

	public boolean isStatic()
	{
		return m_isStatic;
	}
	
	public void setTraversable(boolean isTraversable)
	{
		m_isTraversable = isTraversable;
	}
	
	public boolean isTraversable()
	{
		return m_isTraversable;
	}

	public IImmutableSceneModel getModel()
	{
		return m_sceneModel.clone();
	}
	
	public void setModel(ISceneModel sceneModel, URI sceneModelName, Direction direction)
	{
		m_sceneModel = sceneModel;
		m_sceneModelName = sceneModelName;
		m_direction = direction;
	}

	public URI getModelName()
	{
		return m_sceneModelName;
	}

	/*
	 * Location shouldn't be modified unless it is through the EditorWorld
	 */
	protected void setLocation(Vector3F location)
	{
		m_dummy.getBody().setLocation(new Vector3F(location));
	}

	public Vector3F getLocation()
	{
		return new Vector3F(m_dummy.getBody().getLocation());
	}

	public Direction getDirection()
	{
		return m_direction;
	}

	public SceneArtifactImportDeclaration createSceneArtifactDeclaration()
	{
		SceneArtifactImportDeclaration artifactDecl = new SceneArtifactImportDeclaration();
		
		artifactDecl.isTraversable = isTraversable();
		artifactDecl.model = m_sceneModelName.toString();
		artifactDecl.direction = m_direction;
		artifactDecl.isStatic = m_isStatic;
		
		return artifactDecl;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 97 * hash + Objects.hashCode(this.m_sceneModelName);
		hash = 97 * hash + Objects.hashCode(this.m_direction);
		hash = 97 * hash + (this.m_isTraversable ? 1 : 0);
		hash = 97 * hash + (this.m_isStatic ? 1 : 0);
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
		final EditorSceneArtifact other = (EditorSceneArtifact) obj;
		if (!Objects.equals(this.m_sceneModelName, other.m_sceneModelName)) {
			return false;
		}
		if (this.m_direction != other.m_direction) {
			return false;
		}
		if (this.m_isTraversable != other.m_isTraversable) {
			return false;
		}
		if (this.m_isStatic != other.m_isStatic) {
			return false;
		}
		return true;
	}

	public class DummySceneArtifact implements IEntity
	{
		private final String m_name;
		private IPhysicsBody m_body = new NullPhysicsBody();
		private World m_world;
		private final EntityBridge m_bridge;
		
		public DummySceneArtifact(String name)
		{
			m_name = name;
			m_bridge = new EntityBridge(this);
		}

		@Override
		public void dispose()
		{
			if(m_world != null)
				m_world.removeEntity(this);
		}
		
		public EditorSceneArtifact getEditorTile()
		{
			return EditorSceneArtifact.this;
		}
	
		@Override
		public IImmutableSceneModel getModel()
		{
			return m_sceneModel;
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
		public void update(int delta)
		{	
			m_sceneModel.update(delta);
		}
		
		@Override
		public boolean isStatic()
		{
			return m_isStatic;
		}
		
		public IEntityTaskModel getTaskModel()
		{
			return new NullEntityTaskModel();
		}
	}
}
