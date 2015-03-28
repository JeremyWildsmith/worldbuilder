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
package io.github.jevaengine.builder.worldbuilder;

import io.github.jevaengine.IEngineThreadPool;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.game.IGame;
import io.github.jevaengine.game.IGameFactory;
import io.github.jevaengine.game.IRenderer;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.joystick.IInputSource;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.IWorldFactory;
import io.github.jevaengine.world.ThreadPooledWorldFactory;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.model.IAnimationSceneModelFactory;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;

public final class WorldBuilderFactory implements IGameFactory
{
	@Inject
	@Named("BASE_DIRECTORY")
	private URI m_baseDirectory;
	
	private final IInputSource m_inputSource;
	private final IRenderer m_renderer;
	private final ISceneBufferFactory m_sceneBufferFactory;
	private final ISpriteFactory m_spriteFactory;
	private final IWindowFactory m_windowFactory;
	private final IParallelWorldFactory m_worldFactory;
	private final IFontFactory m_fontFactory;
	private final IConfigurationFactory m_configurationFactory;
	private final IAnimationSceneModelFactory m_animationSceneModelFactory;
	
	@Inject
	public WorldBuilderFactory(IInputSource inputSource, IConfigurationFactory configurationFactory, IRenderer renderer, ISceneBufferFactory sceneBufferFactory, ISpriteFactory spriteFactory, IWindowFactory windowFactory, IWorldFactory worldFactory, IEngineThreadPool engineThreadPool, IFontFactory fontFactory, IAnimationSceneModelFactory animationSceneModelFactory)
	{
		m_inputSource = inputSource;
		m_configurationFactory = configurationFactory;
		m_renderer = renderer;
		m_sceneBufferFactory = sceneBufferFactory;
		m_spriteFactory = spriteFactory;
		m_windowFactory = windowFactory;
		m_worldFactory = new ThreadPooledWorldFactory(worldFactory, engineThreadPool);
		m_fontFactory = fontFactory;
		m_animationSceneModelFactory = animationSceneModelFactory;
	}
	
	public IGame create()
	{
		//This field is injected typically. If this object is not instantiated by the ioc container
		//it will not be injected. The implementation assumes that it will be.
		assert m_baseDirectory != null: "BASE_DIRECTORY was not injected into WorldBuilder";
		
		return new WorldBuilder(m_inputSource, m_configurationFactory, m_sceneBufferFactory, m_spriteFactory, m_windowFactory, m_worldFactory, m_fontFactory, m_animationSceneModelFactory, m_renderer.getResolution(), m_baseDirectory);
	}
}
