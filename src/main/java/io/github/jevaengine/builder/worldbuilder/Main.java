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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import io.github.jevaengine.IAssetStreamFactory;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.builder.BuilderAssetStreamFactory;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorldFactory;
import io.github.jevaengine.config.CachedConfigurationFactory;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonConfigurationFactory;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.game.FrameRenderer;
import io.github.jevaengine.game.FrameRenderer.RenderFitMode;
import io.github.jevaengine.game.GameDriver;
import io.github.jevaengine.game.IGameFactory;
import io.github.jevaengine.game.IRenderer;
import io.github.jevaengine.graphics.*;
import io.github.jevaengine.joystick.FrameInputSource;
import io.github.jevaengine.joystick.IInputSource;
import io.github.jevaengine.script.IScriptBuilder;
import io.github.jevaengine.script.NullScriptBuilder;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.IEffectMapFactory;
import io.github.jevaengine.world.IWorldFactory;
import io.github.jevaengine.world.TiledEffectMapFactory;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.entity.NullEntityFactory;
import io.github.jevaengine.world.physics.IPhysicsWorldFactory;
import io.github.jevaengine.world.physics.NullPhysicsWorldFactory;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.NullSceneBufferFactory;
import io.github.jevaengine.world.scene.TopologicalOrthographicProjectionSceneBufferFactory;
import io.github.jevaengine.world.scene.model.ExtentionMuxedSceneModelFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import io.github.jevaengine.world.scene.model.particle.DefaultParticleEmitterFactory;
import io.github.jevaengine.world.scene.model.particle.IParticleEmitterFactory;
import io.github.jevaengine.world.scene.model.sprite.SpriteSceneModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.net.URI;

public class Main implements WindowListener, Runnable
{
	
	private static final String CONFIG_NAME = "game.jbc";
	private static final int WINX = 1830;
	private static final int WINY = 975;

	private JFrame m_frame;
	private GameDriver m_gameDriver;
	
	private final Logger m_logger = LoggerFactory.getLogger(Main.class);
	private final File m_assetSource;
	private ISceneBufferFactory m_sceneBufferFactory = new NullSceneBufferFactory();
	private WorldBuilderConfiguration m_config;

	private final boolean m_enableCache;
	
	public static void main(String[] args)
	{
		System.setProperty("sun.java2d.opengl", "true");
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
			m_config = JsonVariable.create(configSource).getValue(WorldBuilderConfiguration.class);
			m_sceneBufferFactory = new TopologicalOrthographicProjectionSceneBufferFactory(m_config.projection);
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
			bind(WorldBuilderConfiguration.class).toInstance(m_config);
			bind(IInputSource.class).toInstance(FrameInputSource.create(m_frame));
			bind(IScriptBuilder.class).toInstance(new NullScriptBuilder());
			bind(IGameFactory.class).to(WorldBuilderFactory.class);
			bind(IPhysicsWorldFactory.class).to(NullPhysicsWorldFactory.class);
			bind(IEntityFactory.class).to(NullEntityFactory.class);
			bind(IWorldFactory.class).to(EditorWorldFactory.class);
			bind(ISceneBufferFactory.class).toInstance(m_sceneBufferFactory);
			bind(IEffectMapFactory.class).to(TiledEffectMapFactory.class);
			bind(IParticleEmitterFactory.class).to(DefaultParticleEmitterFactory.class);
			bind(IParticleEmitterFactory.class).to(DefaultParticleEmitterFactory.class);
			bind(IRenderer.class).toInstance(new FrameRenderer(m_frame, false, RenderFitMode.Stretch));
			
			bind(IAssetStreamFactory.class).toProvider(new Provider<IAssetStreamFactory>() {
				@Override
				public IAssetStreamFactory get() {
					return new BuilderAssetStreamFactory(m_assetSource);
				}
			});
			
			bind(IConfigurationFactory.class).toProvider(new ConfigurationFactoryProvider(m_enableCache));
			
			bind(IGraphicFactory.class).toProvider(new Provider<IGraphicFactory>() {
				@Inject
				private IConfigurationFactory configurationFactory;

				@Inject
				private IAssetStreamFactory assetStreamFactory;
				
				@Inject
				private IRenderer renderer;
				
				@Override
				public IGraphicFactory get() {
					ExtentionMuxedGraphicFactory muxedGraphicFactory = new ExtentionMuxedGraphicFactory(new BufferedImageGraphicFactory(renderer, assetStreamFactory));
					IGraphicFactory graphicFactory = new CachedGraphicFactory(muxedGraphicFactory);
					muxedGraphicFactory.put(".sgf", new ShadedGraphicFactory(new DefaultGraphicShaderFactory(this, configurationFactory), graphicFactory, configurationFactory));
					return graphicFactory;
				}
			});
			
			bind(ISceneModelFactory.class).toProvider(new Provider<ISceneModelFactory>() {
				@Inject
				private IAssetStreamFactory assetStreamFactory;
				
				@Inject
				private ISpriteFactory spriteFactory;
				
				@Inject
				private IConfigurationFactory configurationFactory;
				
				@Inject
				private IAudioClipFactory audioClipFactory;
				
				@Inject
				private IParticleEmitterFactory particleEmitterFactory;
				
				@Override
				public ISceneModelFactory get() {
					ExtentionMuxedSceneModelFactory muxedFactory = new ExtentionMuxedSceneModelFactory(new SpriteSceneModelFactory(configurationFactory, spriteFactory, audioClipFactory));
					muxedFactory.put("jpar", particleEmitterFactory);
					
					return muxedFactory;
				}
			});
		}
	}
	
	private static final class ConfigurationFactoryProvider implements Provider<IConfigurationFactory>
	{
		private final boolean m_isCached;
		
		@Inject
		private IAssetStreamFactory m_assetStreamFactory;
		
		private CachedConfigurationFactory m_configurationFactory;
		
		public ConfigurationFactoryProvider(boolean isCached)
		{
			m_isCached = isCached;
		}
		
		@Override
		public IConfigurationFactory get()
		{
			if(m_isCached && m_configurationFactory == null)
			{
				m_configurationFactory = new CachedConfigurationFactory(new JsonConfigurationFactory(m_assetStreamFactory));
				return m_configurationFactory;
			} else
				return new JsonConfigurationFactory(m_assetStreamFactory);
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
}
