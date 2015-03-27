package io.github.jevaengine.builder.worldbuilder.world;

import io.github.jevaengine.FutureResult;
import io.github.jevaengine.IDisposable;
import io.github.jevaengine.IInitializationMonitor;
import io.github.jevaengine.builder.ui.MessageBoxFactory;
import io.github.jevaengine.builder.ui.StatusDialogueFactory;
import io.github.jevaengine.builder.ui.MessageBoxFactory.IMessageBoxObserver;
import io.github.jevaengine.builder.ui.MessageBoxFactory.MessageBox;
import io.github.jevaengine.builder.ui.StatusDialogueFactory.StatusDialogue;
import io.github.jevaengine.builder.worldbuilder.ui.CreateWorldQueryFactory;
import io.github.jevaengine.builder.worldbuilder.ui.OpenWorldQueryFactory;
import io.github.jevaengine.builder.worldbuilder.ui.CreateWorldQueryFactory.CreateWorldQuery;
import io.github.jevaengine.builder.worldbuilder.ui.CreateWorldQueryFactory.ICreateWorldQueryObserver;
import io.github.jevaengine.builder.worldbuilder.ui.OpenWorldQueryFactory.IOpenWorldQueryObserver;
import io.github.jevaengine.builder.worldbuilder.ui.OpenWorldQueryFactory.OpenWorldQuery;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorldViewFactory.EditorWorldView;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorldViewFactory.IEditorWorldViewObserver;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.Button;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WindowManager;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.IWorldFactory;
import io.github.jevaengine.world.IWorldFactory.WorldConstructionException;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.NullEntityFactory;
import io.github.jevaengine.world.physics.NullPhysicsWorld;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FloatingToolbarFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/toolbar.jwl");
	
	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	private final ISceneModelFactory m_modelFactory;
	private final IParallelWorldFactory m_worldFactory;
	
	private final ISceneBufferFactory m_sceneBufferFactory;
	
	private final IFontFactory m_fontFactory;
	
	private final URI m_baseDirectory;
	
	public FloatingToolbarFactory(WindowManager windowManager, IWindowFactory windowFactory, ISceneBufferFactory sceneBufferFactory, ISceneModelFactory modelFactory, IParallelWorldFactory worldFactory, IFontFactory fontFactory, URI baseDirectory)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
		m_sceneBufferFactory = sceneBufferFactory;
		m_modelFactory = modelFactory;
		m_worldFactory = worldFactory;
		m_fontFactory = fontFactory;
		m_baseDirectory = baseDirectory;
	}
	
	public FloatingToolbar create() throws WindowConstructionException
	{
		Window window = m_windowFactory.create(WINDOW_LAYOUT, new FloatingToolbarBehaviourInjector());
		m_windowManager.addWindow(window);

		return new FloatingToolbar(window);
	}
	
	public static class FloatingToolbar implements IDisposable
	{
		private Window m_window;
		
		private FloatingToolbar(Window window)
		{
			m_window = window;
		}
		
		@Override
		public void dispose()
		{
			m_window.dispose();
		}
		
		public void setVisible(boolean isVisible)
		{
			m_window.setVisible(isVisible);
		}
		
		public void setLocation(Vector2D location)
		{
			m_window.setLocation(location);
		}
		
		public void center()
		{
			m_window.center();
		}
	}
	
	private class FloatingToolbarBehaviourInjector extends WindowBehaviourInjector
	{
		private Logger m_logger = LoggerFactory.getLogger(FloatingToolbarBehaviourInjector.class);

		private void displayMessage(String message)
		{
			try
			{
				final MessageBox msgBox = new MessageBoxFactory(m_windowManager, m_windowFactory).create(message);
				msgBox.getObservers().add(new IMessageBoxObserver() {
					@Override
					public void okay() {
						msgBox.dispose();
					}
				});
			} catch(WindowConstructionException e)
			{
				m_logger.error("Unable to construct message box", e);
			}
		}
		
		private void createEditorView(World world)
		{
			try
			{
				final EditorWorldView worldView = new EditorWorldViewFactory(m_windowManager, m_windowFactory, m_sceneBufferFactory, m_modelFactory, m_fontFactory, m_baseDirectory).create(new EditorWorld(world, m_fontFactory));
			
				worldView.getObservers().add(new IEditorWorldViewObserver() {
					@Override
					public void close() {
						worldView.dispose();
					}
				});
			} catch (WindowConstructionException e)
			{
				world.dispose();
				m_logger.error("Unable to construct world editor view window", e);
			}
		}

		private void loadWorld(URI name)
		{
			try
			{
				final StatusDialogue statusDialogue = new StatusDialogueFactory(m_windowManager, m_windowFactory).create();	
				
				m_worldFactory.create(name, 1.0F, 1.0F, new IInitializationMonitor<World, IWorldFactory.WorldConstructionException>() {
					
					@Override
					public void statusChanged(float progress, String status)
					{
						if(statusDialogue != null)
							statusDialogue.setStatus(status, progress);
					}
					
					@Override
					public void completed(FutureResult<World, WorldConstructionException> result) {
						statusDialogue.dispose();
						try
						{
							createEditorView(result.get());
						} catch (WorldConstructionException e)
						{
							displayMessage("Unable to load the specified world. Assure all of its dependencies are accessible via the editor and that the world is proerply formatted. View error log for further details.");
							m_logger.info("Unable to load world", e);
						}
					}
				});
			} catch (WindowConstructionException e)
			{
				m_logger.error("Unable to construct status dialogue to display progress of world loading.", e);
			}
		}
		
		@Override
		protected void doInject() throws NoSuchControlException
		{
			
			getControl(Button.class, "btnOpenWorld").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					try
					{
						final OpenWorldQuery query = new OpenWorldQueryFactory(m_windowManager, m_windowFactory, m_baseDirectory).create();
						query.getObservers().add(new IOpenWorldQueryObserver() {
							
							@Override
							public void okay(URI world) {
								loadWorld(world);
								query.dispose();
							}
							
							@Override
							public void cancel() {
								query.dispose();
							}
						});
					} catch(WindowConstructionException e)
					{
						m_logger.error("Unable to construct world selection dialogue", e);
					}
				}
			});
			
			getControl(Button.class, "btnNewWorld").getObservers().add(new IButtonPressObserver() {	
				@Override
				public void onPress()
				{
					try
					{
						final CreateWorldQuery query = new CreateWorldQueryFactory(m_windowManager, m_windowFactory).create();
						
						query.getObservers().add(new ICreateWorldQueryObserver() {
							
							@Override
							public void okay(int width, int height, float friction)
							{
								World baseWorld = new World(width, height, new NullPhysicsWorld(friction), new NullEntityFactory());
								createEditorView(baseWorld);
								query.dispose();
							}
							
							@Override
							public void cancel() {
								query.dispose();
							}
						});
					} catch (WindowConstructionException e)
					{
						m_logger.error("Error create world window", e);
					}
				}
			});
		}
	}
}
