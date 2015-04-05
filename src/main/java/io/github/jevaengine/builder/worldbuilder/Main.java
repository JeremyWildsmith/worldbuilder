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

import io.github.jevaengine.IAssetStreamFactory;
import io.github.jevaengine.builder.BuilderAssetStreamFactory;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorldFactory;
import io.github.jevaengine.config.CachedConfigurationFactory;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.config.IImmutableVariable;
import io.github.jevaengine.config.ISerializable;
import io.github.jevaengine.config.IVariable;
import io.github.jevaengine.config.NoSuchChildVariableException;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonConfigurationFactory;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.game.FrameRenderer;
import io.github.jevaengine.game.FrameRenderer.RenderFitMode;
import io.github.jevaengine.game.GameDriver;
import io.github.jevaengine.game.IGameFactory;
import io.github.jevaengine.game.IRenderer;
import io.github.jevaengine.graphics.BufferedImageGraphicFactory;
import io.github.jevaengine.graphics.CachedGraphicFactory;
import io.github.jevaengine.graphics.IGraphicFactory;
import io.github.jevaengine.joystick.FrameInputSource;
import io.github.jevaengine.joystick.IInputSource;
import io.github.jevaengine.math.Matrix3X3;
import io.github.jevaengine.script.IScriptBuilder;
import io.github.jevaengine.script.NullScriptBuilder;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.IWorldFactory;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.entity.NullEntityFactory;
import io.github.jevaengine.world.physics.IPhysicsWorldFactory;
import io.github.jevaengine.world.physics.NullPhysicsWorldFactory;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.NullSceneBufferFactory;
import io.github.jevaengine.world.scene.TopologicalOrthographicProjectionSceneBufferFactory;

import java.awt.Color;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public class Main implements WindowListener, Runnable
{
	
	private static final String CONFIG_NAME = "game.jbc";
	private static final int WINX = 1220;
	private static final int WINY = 650;

	private JFrame m_frame;
	private GameDriver m_gameDriver;
	
	private final Logger m_logger = LoggerFactory.getLogger(Main.class);
	private final File m_assetSource;
	private ISceneBufferFactory m_sceneBufferFactory = new NullSceneBufferFactory();
	
	private final boolean m_enableCache;
	
	public static void main(String[] args)
	{
		File assetDirectory = args.length >= 1 ? new File(args[0]) : null;
		boolean enableCache = args.length >= 2 ? !args[1].equals("false") : true;
		Main main = new Main(assetDirectory, enableCache);
		main.run();
	}
	
	public Main(@Nullable File assetSource, boolean enableCache)
	{
		m_enableCache = enableCache;
		
		if(assetSource == null)
		{
			m_logger.warn("You have not specified a working directiory [ the root folder of your game's assets. ] The active working directory of the builder will be used instead. This is likely not the behavior you want.");	
			m_assetSource = new File("");
		}else
			m_assetSource = assetSource;
				
		if(!enableCache)
			m_logger.warn("Configuration cache has been disabled. This may result in very poor performance.");
		
		try(InputStream configSource = new FileInputStream(new File(m_assetSource.toURI().resolve("./" + CONFIG_NAME))))
		{
			WorldBuilderConfiguration config = JsonVariable.create(configSource).getValue(WorldBuilderConfiguration.class);
			m_sceneBufferFactory = new TopologicalOrthographicProjectionSceneBufferFactory(config.projection);
		} catch (FileNotFoundException e)
		{
			m_logger.error("The specified project directory does not contain the builder configuration document: " + CONFIG_NAME + ". You will not be able to perform some critical tasks in the world builder.", e);
		} catch (IOException | ValueSerializationException e)
		{
			m_logger.error("The world builder configuration document is improperly formed. Read the stack trace for further details. You will not be able to perform some critical tasks in the world builder.", e);
		}
	}

	public void run()
	{
		m_frame = new JFrame();
		
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{

				public void run()
				{
					m_frame.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(Toolkit.getDefaultToolkit().getImage(""), new Point(), "trans"));
					m_frame.setVisible(true);

					m_frame.setIgnoreRepaint(true);
					m_frame.setBackground(Color.black);
					m_frame.setResizable(false);
					m_frame.setTitle("JevaEngine - World Builder");
					m_frame.setSize(WINX, WINY);
					m_frame.setVisible(true);
					m_frame.addWindowListener(Main.this);
				}
			});
		} catch (Exception ex)
		{
			JOptionPane.showMessageDialog(null, "An error occured initializing the game: " + ex);
			return;
		}

		Injector injector = Guice.createInjector(new WorldBuilderModule(m_assetSource.toURI()));
		m_gameDriver = injector.getInstance(GameDriver.class);
		m_gameDriver.begin();
		//m_frame.dispose();
	}

	@Override
	public void windowClosing(WindowEvent arg0)
	{
		m_gameDriver.stop();
	}
	
	private final class WorldBuilderModule extends AbstractModule
	{
		private final URI m_assetSource;
		
		public WorldBuilderModule(URI assetSource)
		{
			m_assetSource = assetSource;
		}
		
		@Override
		protected void configure()
		{
			bind(URI.class).annotatedWith(Names.named("BASE_DIRECTORY")).toInstance(m_assetSource);
			bind(IInputSource.class).toInstance(FrameInputSource.create(m_frame));
			bind(IScriptBuilder.class).toInstance(new NullScriptBuilder());
			bind(IGameFactory.class).to(WorldBuilderFactory.class);
			bind(IPhysicsWorldFactory.class).to(NullPhysicsWorldFactory.class);
			bind(IEntityFactory.class).to(NullEntityFactory.class);
			bind(IWorldFactory.class).to(EditorWorldFactory.class);
			bind(ISceneBufferFactory.class).toInstance(m_sceneBufferFactory);
			
			IAssetStreamFactory assetStreamFactory = new BuilderAssetStreamFactory(m_assetSource);
			IRenderer frameRenderer = new FrameRenderer(m_frame, false, RenderFitMode.Stretch);
			
			bind(IRenderer.class).toInstance(frameRenderer);
			bind(IGraphicFactory.class).toInstance(new CachedGraphicFactory(new BufferedImageGraphicFactory(frameRenderer, assetStreamFactory)));
			bind(IAssetStreamFactory.class).toInstance(assetStreamFactory);
			
			if(m_enableCache)
				bind(IConfigurationFactory.class).toInstance(new CachedConfigurationFactory(new JsonConfigurationFactory(assetStreamFactory)));
			else
				bind(IConfigurationFactory.class).toInstance(new JsonConfigurationFactory(assetStreamFactory));
		}
	}
	
	@Override
	public void windowActivated(WindowEvent arg0) { }
	
	@Override
	public void windowClosed(WindowEvent arg0) { }

	@Override
	public void windowDeactivated(WindowEvent arg0) { }

	@Override
	public void windowDeiconified(WindowEvent arg0) { }

	@Override
	public void windowIconified(WindowEvent arg0) { }

	@Override
	public void windowOpened(WindowEvent arg0) { }
	
	private static final class WorldBuilderConfiguration implements ISerializable
	{
		private Matrix3X3 projection;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException
		{
			target.addChild("projection").setValue(projection);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException
		{
			try
			{
				projection = source.getChild("projection").getValue(Matrix3X3.class);
			} catch (NoSuchChildVariableException e)
			{
				throw new ValueSerializationException(e);
			}
		}
	}
}
