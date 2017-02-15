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

import io.github.jevaengine.builder.worldbuilder.ui.FloatingToolbarFactory;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.game.DefaultGame;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.graphics.ISpriteFactory.SpriteConstructionException;
import io.github.jevaengine.graphics.NullGraphic;
import io.github.jevaengine.joystick.IInputSource;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.world.IEffectMapFactory;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.IWeatherFactory;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldBuilder extends DefaultGame
{
	private IRenderable m_cursor;
	
	private Logger m_logger = LoggerFactory.getLogger(WorldBuilder.class);
	
	public WorldBuilder(IInputSource inputSource, IConfigurationFactory configurationFactory, ISceneBufferFactory sceneBufferFactory, ISpriteFactory spriteFactory, IWindowFactory windowFactory, IParallelWorldFactory worldFactory, IFontFactory fontFactory, ISceneModelFactory sceneModelFactory, IWeatherFactory weatherFactory, IEffectMapFactory effectMapFactory, Vector2D resolution, URI baseDirectory)
	{
		super(inputSource, resolution);
		
		try
		{
			m_cursor = spriteFactory.create(URI.create("local:///ui/style/tech/cursor/cursor.jsf"));
		} catch (SpriteConstructionException e)
		{
			m_logger.error("Error constructing cursor sprite. Reverting to null graphic for cursor.", e);
			m_cursor = new NullGraphic();
		}
		
		try
		{
			new FloatingToolbarFactory(getWindowManager(), windowFactory, sceneBufferFactory, sceneModelFactory, worldFactory, fontFactory, weatherFactory, effectMapFactory, baseDirectory).create().center();
		} catch (WindowConstructionException e)
		{
			m_logger.error("Error constructing world builder toolbar.", e);
		}
	}

	@Override
	protected IRenderable getCursor()
	{
		return m_cursor;
	}

	@Override
	protected void doLogic(int deltaTime) { }
}
