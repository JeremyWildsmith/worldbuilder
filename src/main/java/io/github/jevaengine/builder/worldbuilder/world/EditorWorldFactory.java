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

import io.github.jevaengine.IEngineThreadPool;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.script.IScriptBuilderFactory;
import io.github.jevaengine.world.DefaultWorldFactory;
import io.github.jevaengine.world.DefaultWorldFactory.WorldConfiguration.EntityImportDeclaration;
import io.github.jevaengine.world.DefaultWorldFactory.WorldConfiguration.SceneArtifactImportDeclaration;
import io.github.jevaengine.world.IEffectMapFactory;
import io.github.jevaengine.world.IWeatherFactory;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.entity.IEntityFactory.EntityConstructionException;
import io.github.jevaengine.world.physics.IPhysicsWorldFactory;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory.SceneModelConstructionException;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;

public final class EditorWorldFactory extends DefaultWorldFactory
{
	private final IFontFactory m_fontFactory;
	
	@Inject
	public EditorWorldFactory(IEngineThreadPool threadPool,
			IEntityFactory entityFactory, IScriptBuilderFactory scriptFactory,
			IConfigurationFactory configurationFactory,
			ISpriteFactory spriteFactory, IAudioClipFactory audioClipFactory,
			IPhysicsWorldFactory physicsWorldFactory,
			IFontFactory fontFactory,
			ISceneModelFactory animationSceneModelFactory,
			IWeatherFactory weatherFactory,
			IEffectMapFactory effectMapFactory) {
	
		super(threadPool, entityFactory, scriptFactory, configurationFactory,
				spriteFactory, audioClipFactory, physicsWorldFactory, animationSceneModelFactory,
				new EditorWeatherFactory(weatherFactory), effectMapFactory);
	
		m_fontFactory = fontFactory;
	}

	@Override
	protected IEntity createSceneArtifact(SceneArtifactImportDeclaration artifactDecl, URI context) throws EntityConstructionException
	{
		try
		{
			URI modelUri = context.resolve(new URI(artifactDecl.model));
			
			ISceneModel model = m_sceneModelFactory.create(modelUri);
			model.setDirection(artifactDecl.direction);
			return new EditorSceneArtifact(model, modelUri, artifactDecl.direction, artifactDecl.isTraversable, artifactDecl.isStatic).getEntity();
		} catch (SceneModelConstructionException | URISyntaxException e)
		{
			throw new EntityConstructionException("Unnamed Scene Artifact", e);
		}
	}
	
	@Override
	protected IEntity createEntity(EntityImportDeclaration entityConfig, URI context) throws EntityConstructionException
	{
		try
		{
			EditorEntity e = new EditorEntity(m_fontFactory, m_sceneModelFactory, entityConfig.name, entityConfig.type, entityConfig.config == null ? null : new URI(entityConfig.config));
			
			if(entityConfig.auxConfig != null)
			{
				JsonVariable auxConfig = new JsonVariable();
				entityConfig.auxConfig.serialize(auxConfig);
				e.setAuxiliaryConfig(auxConfig);
			}
			
			return e.getEntity();
		} catch (ValueSerializationException | URISyntaxException e)
		{
			throw new EntityConstructionException(entityConfig.name, e);
		}
	}
}
