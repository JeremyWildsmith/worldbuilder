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

import io.github.jevaengine.IDisposable;
import io.github.jevaengine.builder.ui.FileInputQueryFactory;
import io.github.jevaengine.builder.ui.FileInputQueryFactory.FileInputQuery;
import io.github.jevaengine.builder.ui.FileInputQueryFactory.FileInputQueryMode;
import io.github.jevaengine.builder.ui.FileInputQueryFactory.IFileInputQueryObserver;
import io.github.jevaengine.builder.ui.MessageBoxFactory;
import io.github.jevaengine.builder.ui.MessageBoxFactory.IMessageBoxObserver;
import io.github.jevaengine.builder.ui.MessageBoxFactory.MessageBox;
import io.github.jevaengine.builder.ui.TextInputQueryFactory;
import io.github.jevaengine.builder.ui.TextInputQueryFactory.ITextInputQueryObserver;
import io.github.jevaengine.builder.ui.TextInputQueryFactory.TextInputQuery;
import io.github.jevaengine.builder.worldbuilder.ui.ConfigureEntityQueryFactory;
import io.github.jevaengine.builder.worldbuilder.ui.ConfigureEntityQueryFactory.ConfigureEntityQuery;
import io.github.jevaengine.builder.worldbuilder.ui.ConfigureEntityQueryFactory.IConfigureEntityQueryObserver;
import io.github.jevaengine.builder.worldbuilder.ui.ConfigureZoneQueryFactory;
import io.github.jevaengine.builder.worldbuilder.ui.ConfigureZoneQueryFactory.ConfigureZoneQuery;
import io.github.jevaengine.builder.worldbuilder.ui.ConfigureZoneQueryFactory.IConfigureZoneQueryObserver;
import io.github.jevaengine.builder.worldbuilder.ui.SelectBrushQuery;
import io.github.jevaengine.builder.worldbuilder.world.Brush.IBrushBehaviorObserver;
import io.github.jevaengine.builder.worldbuilder.world.EditorEntity.DummyEntity;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorldFactory.EditorWeatherFactory;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorldFactory.EditorWeatherFactory.EditorWeather;
import io.github.jevaengine.builder.worldbuilder.world.EditorZone.DummyZone;
import io.github.jevaengine.builder.worldbuilder.world.ResizeZoneBrushBehaviour.IResizeZoneBrushBehaviourHandler;
import io.github.jevaengine.builder.worldbuilder.world.SampleSceneArtifactBrush.ISceneArtifactSampleHandler;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputKeyEvent.KeyEventType;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.joystick.InputMouseEvent.MouseButton;
import io.github.jevaengine.joystick.InputMouseEvent.MouseEventType;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.ui.Button;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.Label;
import io.github.jevaengine.ui.MenuStrip;
import io.github.jevaengine.ui.MenuStrip.IMenuStripListener;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.Timer;
import io.github.jevaengine.ui.Timer.ITimerObserver;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.Window.IWindowFocusObserver;
import io.github.jevaengine.ui.Window.IWindowInputObserver;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WindowManager;
import io.github.jevaengine.ui.WorldView;
import io.github.jevaengine.ui.WorldView.IWorldViewInputObserver;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.DefaultWorldFactory.WorldConfiguration;
import io.github.jevaengine.world.IWeatherFactory;
import io.github.jevaengine.world.IWeatherFactory.WeatherConstructionException;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.camera.ControlledCamera;
import io.github.jevaengine.world.scene.effect.DebugDrawComponent;
import io.github.jevaengine.world.scene.effect.HideEntityObstructionsEffect;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorWorldViewFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/editorWorldView.jwl");
	
	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	private final ISceneBufferFactory m_sceneBufferFactory;
	private final ISceneModelFactory m_modelFactory;

	private final EditorWeatherFactory m_weatherFactory;
	
	private final IFontFactory m_fontFactory;
	
	private final URI m_baseDirectory;
	
	public EditorWorldViewFactory(WindowManager windowManager, IWindowFactory windowFactory,
									ISceneBufferFactory sceneBufferFactory, ISceneModelFactory modelFactory, IFontFactory fontFactory,
									IWeatherFactory weatherFactory,
									URI baseDirectory)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
		
		m_weatherFactory = new EditorWeatherFactory(weatherFactory);
		m_fontFactory = fontFactory;
		m_sceneBufferFactory = sceneBufferFactory;
		m_modelFactory = modelFactory;
		m_baseDirectory = baseDirectory;
	}
	
	public EditorWorldView create(EditorWorld world) throws WindowConstructionException
	{
		Observers observers = new Observers();
		
		Brush brush = new Brush();
		SelectBrushQuery selectBrushQuery = new SelectBrushQuery(new File(m_baseDirectory), brush, m_modelFactory);
		
		Window window = m_windowFactory.create(WINDOW_LAYOUT, new EditorWorldViewBehaviourInjector(observers, world, m_sceneBufferFactory, m_modelFactory, brush, selectBrushQuery));
		m_windowManager.addWindow(window);

		return new EditorWorldView(window, observers, world.getWorld(), selectBrushQuery);
	}
	
	public static final class EditorWorldView implements IDisposable
	{
		private final Logger m_logger = LoggerFactory.getLogger(EditorWorldView.class);
		
		private final Window m_window;
		private final IObserverRegistry m_observers;
		private final World m_world;
		private final SelectBrushQuery m_selectBrushQuery;
		
		private EditorWorldView(Window window, IObserverRegistry observers, World world, SelectBrushQuery selectBrushQuery)
		{
			m_observers = observers;
			m_window = window;
			m_world = world;
			m_selectBrushQuery = selectBrushQuery;
		}
		
		@Override
		public void dispose()
		{
			m_window.dispose();
			m_world.dispose();
			
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						m_selectBrushQuery.dispose();
					}
				});
			} catch (InterruptedException | InvocationTargetException ex) {
				m_logger.error("Unable to destory brush selection query.", ex);
			}
		}
		
		public void setVisible(boolean isVisible)
		{
			m_window.setVisible(isVisible);
		}
		
		public void setLocation(Vector2D location)
		{
			m_window.setLocation(location);
		}
		
		public IObserverRegistry getObservers()
		{
			return m_observers;
		}
	}
	
	public interface IEditorWorldViewObserver
	{
		void close();
	}
	
	private class EditorWorldViewBehaviourInjector extends WindowBehaviourInjector
	{
		
		private final Brush m_workingBrush;
		private final SelectBrushQuery m_selectBrushQuery;
		
		private final EditorWorld m_world;
		private final ControlledCamera m_camera;
		private final ISceneModelFactory m_modelFactory;
		
		private final Observers m_observers;
		
		private final Logger m_logger = LoggerFactory.getLogger(EditorWorldViewBehaviourInjector.class);
		
		public EditorWorldViewBehaviourInjector(Observers observers, EditorWorld world, ISceneBufferFactory sceneBufferFactory, ISceneModelFactory modelFactory, Brush workingBrush, SelectBrushQuery selectBrushQuery)
		{
			m_observers = observers;
			m_world = world;
			m_camera = new ControlledCamera(sceneBufferFactory);
			m_camera.attach(world.getWorld());
			m_camera.addEffect(new DebugDrawComponent());
			m_camera.addEffect(new HideEntityObstructionsEffect(m_world.getCursor().getEntity(), 0.4F));
			m_modelFactory = modelFactory;
			
			m_selectBrushQuery = selectBrushQuery;
			m_workingBrush = workingBrush;
		}
		
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
			} catch (WindowConstructionException e)
			{
				m_logger.error("Unable to construct message dialogue", e);
			}
		}
		
		private void moveEntity(final IEntity entity)
		{
			m_camera.lookAt(entity.getBody().getLocation());
			m_workingBrush.setBehaviour(new MoveEntityBrushBehaviour(entity, new MoveEntityBrushBehaviour.IEntityMovementBrushBehaviorHandler() {
				@Override
				public void moved() {
					m_workingBrush.setBehaviour(new NullBrushBehaviour());
				}
			}));
		}
		
		private void resizeZone(final EditorZone zone)
		{
			Vector3F bottomRightTop = zone.getBounds().getPoint(1.0F, 1.0F, 1.0F).add(zone.getLocation());
			m_camera.lookAt(bottomRightTop);
			m_workingBrush.setBehaviour(new ResizeZoneBrushBehaviour(zone, new IResizeZoneBrushBehaviourHandler() {
				@Override
				public void resized() {
					m_workingBrush.setBehaviour(new NullBrushBehaviour());
				}
			}));
		}
		
		@Nullable
		private ConfigureZoneQuery configureZone(final EditorZone base)
		{
			try
			{
				final ConfigureZoneQuery query = new ConfigureZoneQueryFactory(m_windowManager, m_windowFactory, m_baseDirectory).create(base);
				query.getObservers().add(new IConfigureZoneQueryObserver() {
					@Override
					public void delete() {
						m_world.removeZone(base);
						query.dispose();
					}
							
					@Override
					public void cancel() {
						query.dispose();
					}
							
					@Override
					public void apply() {
						query.dispose();
					}
				});
				
				return query;
			} catch (WindowConstructionException e)
			{
				m_logger.error("Unable to construct zone configuration dialogue.", e);
				
				return null;
			}
		}
		
		@Nullable
		private ConfigureEntityQuery configureEntity(final EditorEntity base)
		{		
			try
			{
				final ConfigureEntityQuery query = new ConfigureEntityQueryFactory(m_windowManager, m_windowFactory, m_baseDirectory).create(base);
				query.getObservers().add(new IConfigureEntityQueryObserver() {
					@Override
					public void delete() {
						m_world.removeEntity(base);
						query.dispose();
					}
							
					@Override
					public void cancel() {
						query.dispose();
					}
							
					@Override
					public void apply() {
						query.dispose();
					}
				});
				
				return query;
			} catch (WindowConstructionException e)
			{
				m_logger.error("Unable to construct entity configuration dialogue.", e);
				return null;
			}			
		}
		
		private void displayContextMenu(MenuStrip menuStrip, final DummyEntity entity)
		{
			menuStrip.setContext(new String[] {"Move Entity", "Configure Entity"}, new IMenuStripListener() {
				@Override
				public void onCommand(String command)
				{
					switch (command)
					{
						case "Move Entity":
							moveEntity(entity);
							break;
						case "Configure Entity":
							configureEntity(entity.getEditorEntity());
							break;
					}
				}
			});			
		}
		
		private void displayContextMenu(MenuStrip menuStrip, final DummyZone zone)
		{
			menuStrip.setContext(new String[] {"Move Zone", "Edit Zone", "Resize Zone"}, new IMenuStripListener() {
				@Override
				public void onCommand(String command)
				{
					switch (command) 
					{
						case "Move Zone":
							moveEntity(zone);
							break;
						case "Edit Zone":
							configureZone(zone.getEditorEntity());
							break;
						case "Resize Zone":
							resizeZone(zone.getEditorEntity());
							break;
					}
				}
			});
		}
		
		private void displayDefaultContextMenu(final WorldView worldView, final Vector2D location, MenuStrip menuStrip)
		{
			menuStrip.setContext(new String[] {"Create Entity", "Create Zone"}, new IMenuStripListener() {
				@Override
				public void onCommand(String command)
				{
					if(command.equals("Create Entity"))
					{
						EditorEntity base = createUnnamedEntity();
						base.setLocation(new Vector3F(worldView.translateScreenToWorld(new Vector2F(location)), m_camera.getLookAt().z));
						m_world.addEntity(base);
						configureEntity(base);
					}else if(command.equals("Create Zone"))
					{
						EditorZone base = createUnnamedZone();
						base.setLocation(new Vector3F(worldView.translateScreenToWorld(new Vector2F(location)), m_camera.getLookAt().z));
						m_world.addZone(base);
						configureZone(base);
					}
				}
			});
		}
		
		private void saveWorld(URI destination)
		{
			WorldConfiguration config = m_world.createWorldConfiguration();
			
			try(FileOutputStream fos = new FileOutputStream(new File(m_baseDirectory.resolve(URI.create("/").relativize(destination)))))
			{
				JsonVariable var = new JsonVariable();
				var.setValue(config);
				var.serialize(fos, true);
				displayMessage("World has been saved successfully.");
			} catch (IOException | ValueSerializationException e)
			{
				m_logger.error("Unable to save world", e);
				displayMessage("Error occured attmepting to save world. View log for more details.");
			}
		}
		
		private void showEntityNameConflict(EditorEntity a, EditorEntity b)
		{
			ConfigureEntityQuery q = configureEntity(a);
			configureEntity(b);
			
			if(q != null)
				q.setLocation(q.getLocation().add(new Vector2D(-5, -5)));
		}
		
		private void showZoneNameConflict(EditorZone a, EditorZone b)
		{
			ConfigureZoneQuery q = configureZone(a);
			configureZone(b);
			
			if(q != null)
				q.setLocation(q.getLocation().add(new Vector2D(-5, -5)));
		}
		
		private void verifyAndSaveWorld(URI destination)
		{
			Map<String, EditorEntity> usedEntityNames = new HashMap<>();
			Map<String, EditorZone> usedZoneNames = new HashMap<>();
			
			final String message = "Due to a %s name conflict with the name '%s', the world could not be saved. Please resolve this issue before attempting to save again.";
			
			for(EditorEntity e : m_world.getEntities())
			{
				if(usedEntityNames.containsKey(e.getName()))
				{
					showEntityNameConflict(e, usedEntityNames.get(e.getName()));
					displayMessage(String.format(message, "entity", e.getName()));
					return;
				}else
					usedEntityNames.put(e.getName(), e);
			}
			
			for(EditorZone z : m_world.getZones())
			{
				if(usedZoneNames.containsKey(z.getName()))
				{
					showZoneNameConflict(z, usedZoneNames.get(z.getName()));
					displayMessage(String.format(message, "zone", z.getName()));
					return;
				}else
					usedZoneNames.put(z.getName(), z);
			}
			
			saveWorld(destination);
		}
		
		private EditorEntity createUnnamedEntity()
		{
			Set<String> usedNames = new HashSet<>();
			
			for(EditorEntity e : m_world.getEntities())
				usedNames.add(e.getName());
			
			for(int i = 0; ; i++)
			{
				String name = "Unnamed" + i;
				if(!usedNames.contains(name))
					return new EditorEntity(m_fontFactory, m_modelFactory, name, "");
			}
		}
		
		private EditorZone createUnnamedZone()
		{
			Set<String> usedNames = new HashSet<>();
			
			for(EditorZone z : m_world.getZones())
				usedNames.add(z.getName());
			
			for(int i = 0; ; i++)
			{
				String name = "Unnamed" + i;
				if(!usedNames.contains(name))
					return new EditorZone(m_fontFactory, name);
			}
		}
		
		@Override
		protected void doInject() throws NoSuchControlException
		{
			final Timer logicTimer = new Timer();
			final WorldView worldView = getControl(WorldView.class, "worldView");
			final Label lblCursorCoordinates = getControl(Label.class, "lblCursorCoordinates");
			final Label lblIsTraversable = getControl(Label.class, "lblIsTraversable");
			final MenuStrip menuStrip = new MenuStrip();
			
			addControl(menuStrip);
			addControl(logicTimer);			
			worldView.setCamera(m_camera);
			
			final CameraController cameraController = new CameraController(worldView, m_camera, m_world);
			logicTimer.getObservers().add(cameraController);
			worldView.getObservers().add(cameraController);
			getObservers().add(cameraController);

			SelectionController selectionController = new SelectionController(m_world, m_workingBrush);
			getObservers().add(selectionController);
			logicTimer.getObservers().add(selectionController);
			
			logicTimer.getObservers().add(new ITimerObserver() {
				@Override
				public void update(int deltaTime) {
					if(m_selectBrushQuery.isVisible() != EditorWorldViewBehaviourInjector.this.isVisible())
						m_selectBrushQuery.setVisible(EditorWorldViewBehaviourInjector.this.isVisible());
					
					m_selectBrushQuery.poll();
					m_world.getWorld().update(deltaTime);
					Vector3F coordinates = m_world.getCursor().getLocation();
					lblCursorCoordinates.setText(String.format("%f, %f, %f; Snap: %f", coordinates.x, coordinates.y, coordinates.z, cameraController.getCursorSnapGridSize()));
					
					EditorSceneArtifact tile = m_world.getTile(coordinates);
					lblIsTraversable.setText(tile == null || tile.isTraversable() ? "true" : "false");
				}
			});
			
			worldView.getObservers().add(new IWorldViewInputObserver() {
				@Override
				public void keyEvent(InputKeyEvent event) { }

				@Override
				public void mouseEvent(InputMouseEvent event)
				{
					if(event.type != MouseEventType.MouseClicked)
						return;
					
					final Vector2D location = event.location;
					final IEntity pickedEntity = worldView.pick(IEntity.class, location);
					
					if(event.mouseButton == MouseButton.Right)
					{
						if(pickedEntity instanceof DummyEntity)
							displayContextMenu(menuStrip, (DummyEntity)pickedEntity);
						else if(pickedEntity instanceof DummyZone)
							displayContextMenu(menuStrip, (DummyZone)pickedEntity);
						else
							displayDefaultContextMenu(worldView, location, menuStrip);
						
						menuStrip.setLocation(location.add(worldView.getLocation()));
					}else
						menuStrip.setVisible(false);
				}
			});
			
			m_workingBrush.getObservers().add(new IBrushBehaviorObserver() {
				@Override
				public void behaviourChanged(IBrushBehaviour behaviour) {
					m_world.getCursor().setModel(behaviour.getModel());
				}
			});
			
			getControl(Button.class, "btnCreateEntity").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					
					final EditorEntity base = createUnnamedEntity();
					m_world.addEntity(base);
					configureEntity(base);
				}
			});
			
			getControl(Button.class, "btnChangeWeather").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					try
					{
						EditorWeather weather = m_world.getWeather();
						URI currentWeather = weather == null ? m_baseDirectory : weather.getName();
						
						final FileInputQuery query = new FileInputQueryFactory(m_windowManager, m_windowFactory, m_baseDirectory).create(FileInputQueryMode.OpenFile, "Weather: ", currentWeather);
						query.getObservers().add(new IFileInputQueryObserver() {			
							@Override
							public void okay(URI input)
							{
								try
								{
									m_world.setWeather(m_weatherFactory.create(input));
									query.dispose();
								} catch(WeatherConstructionException e)
								{
									displayMessage("Error constructing world weather. Either due to the fact that is it not properly formatted, or it does not exist.");
								}
							}
							@Override
							public void cancel() {
								query.dispose();
							}
						});
					} catch (WindowConstructionException e) {
						m_logger.error("Unable to construct text input dialogue for depth adjust", e);
					}
				}
			});
			
			getControl(Button.class, "btnClose").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(IEditorWorldViewObserver.class).close();
				}
			});
			
			getControl(Button.class, "btnAdjustDepth").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress()
				{
					try {
						final TextInputQuery query = new TextInputQueryFactory(m_windowManager, m_windowFactory).create("Cursor Depth", Float.toString(m_world.getCursor().getLocation().z));
						query.getObservers().add(new ITextInputQueryObserver() {
							
							@Override
							public void okay(String input)
							{
								try
								{
									Vector3F cursorLocation = m_world.getCursor().getLocation();
									cursorLocation.z = Float.parseFloat(input);
									m_camera.lookAt(cursorLocation);
									query.dispose();
								} catch(NumberFormatException e)
								{
									displayMessage("Depth must be a properly formed floating point.");
								}
							}
							@Override
							public void cancel() {
								query.dispose();
							}
						});
					} catch (WindowConstructionException e) {
						m_logger.error("Unable to construct text input dialogue for depth adjust", e);
					}
				}
			});
			
			getControl(Button.class, "btnAdjustZoom").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress()
				{
					try {
						final TextInputQuery query = new TextInputQueryFactory(m_windowManager, m_windowFactory).create("Cursor Depth", Float.toString(m_world.getCursor().getLocation().z));
						query.getObservers().add(new ITextInputQueryObserver() {			
							@Override
							public void okay(String input)
							{
								try
								{
									m_camera.setZoom(Float.parseFloat(input));
									query.dispose();
								} catch(NumberFormatException e)
								{
									displayMessage("Depth must be a properly formed floating point.");
								}
							}
							@Override
							public void cancel() {
								query.dispose();
							}
						});
					} catch (WindowConstructionException e) {
						m_logger.error("Unable to construct text input dialogue for depth adjust", e);
					}
				}
			});

			getControl(Button.class, "btnAdjustSnapGridSize").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress()
				{
					try {
						final TextInputQuery query = new TextInputQueryFactory(m_windowManager, m_windowFactory).create("Cursor Snap Grid size:", Float.toString(cameraController.getCursorSnapGridSize()));
						query.getObservers().add(new ITextInputQueryObserver() {
							@Override
							public void okay(String input)
							{
								try
								{
									cameraController.setCursorSnapGridSize(Float.parseFloat(input));
									query.dispose();
								} catch(NumberFormatException e)
								{
									displayMessage("Snap grid size must be a properly formed floating point.");
								}
							}
							@Override
							public void cancel() {
								query.dispose();
							}
						});
					} catch (WindowConstructionException e) {
						m_logger.error("Unable to construct text input dialogue for snap grid size adjust", e);
					}
				}
			});

			getControl(Button.class, "btnClearBrush").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_workingBrush.setBehaviour(new ClearTileBrushBehaviour());
				}
			});
			
			getControl(Button.class, "btnSampleBrush").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_workingBrush.setBehaviour(new SampleSceneArtifactBrush(new ISceneArtifactSampleHandler() {
						@Override
						public void sample(EditorSceneArtifact sample) {
							m_workingBrush.setBehaviour(new PlaceSceneArtifactBrushBehaviour(sample.getModel(), sample.getModelName(), sample.getDirection(), sample.isTraversable(), sample.isStatic()));					
						}
					}));
				}
			});
			
			getControl(Button.class, "btnSave").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					
					try {
						final FileInputQuery query = new FileInputQueryFactory(m_windowManager, m_windowFactory, m_baseDirectory).create(FileInputQueryMode.SaveFile, "World Save Location", m_baseDirectory);
						query.getObservers().add(new IFileInputQueryObserver() {
							
							@Override
							public void okay(URI input)
							{
								verifyAndSaveWorld(input);
								query.dispose();
							}
							
							@Override
							public void cancel()
							{
								query.dispose();
							}
						});
					} catch (WindowConstructionException e) {
						m_logger.error("Could not construct save world dialogue", e);
					}
				}
			});
		}
	}

	private class CameraController implements IWindowFocusObserver, ITimerObserver, IWorldViewInputObserver
	{
		private final ControlledCamera m_camera;
		private final WorldView m_worldView;
		private final EditorWorld m_world;
		
		private Vector3F m_cameraMovement = new Vector3F();
		
		private float m_snapGridSize = 0.5F;
		
		public CameraController(WorldView worldView, ControlledCamera camera, EditorWorld world)
		{
			m_worldView = worldView;
			m_camera = camera;
			m_world = world;
		}

		@Override
		public void update(int deltaTime)
		{
			if (!m_cameraMovement.isZero())
				m_camera.move(m_cameraMovement.normalize().multiply(deltaTime / 200.0F * m_snapGridSize));
		
			float x = (int)(m_camera.getLookAt().x / m_snapGridSize) * m_snapGridSize;
			float y = Math.round(m_camera.getLookAt().y / m_snapGridSize) * m_snapGridSize;
			
			m_world.getCursor().setLocation(new Vector3F(x, y, m_camera.getLookAt().z));
		}
		
		public float getCursorSnapGridSize()
		{
			return m_snapGridSize;
		}
		
		public void setCursorSnapGridSize(float gridSize)
		{
			m_snapGridSize = Math.max(gridSize, 0.01F);
		}
		
		@Override
		public void onFocusChanged(boolean hasFocus)
		{
			if(!hasFocus)
				m_cameraMovement = new Vector3F();
		}
		
		@Override
		public void keyEvent(InputKeyEvent e) 
		{
			if(e.type == KeyEventType.KeyDown)
			{
				switch (e.keyCode)
				{
					case KeyEvent.VK_UP:
						m_cameraMovement.y = -1;
						break;
					case KeyEvent.VK_RIGHT:
						m_cameraMovement.x = 1;
						break;
					case KeyEvent.VK_DOWN:
						m_cameraMovement.y = 1;
						break;
					case KeyEvent.VK_LEFT:
						m_cameraMovement.x = -1;
						break;
				}
			} else if(e.type == KeyEventType.KeyUp)
			{
				switch (e.keyCode)
				{
					case KeyEvent.VK_UP:
					case KeyEvent.VK_DOWN:
						m_cameraMovement.y = 0;
						break;
					case KeyEvent.VK_RIGHT:
					case KeyEvent.VK_LEFT:
						m_cameraMovement.x = 0;
						break;
				}
			}
		}
		
		@Override
		public void mouseEvent(InputMouseEvent event)
		{
			if(event.mouseButton == MouseButton.Left && event.type == MouseEventType.MouseClicked)
			{
				IEntity tile = m_worldView.pick(IEntity.class, event.location);
				
				if(tile != null)
					m_camera.lookAt(tile.getBody().getLocation());
			}
		}
	}
		
	private static final class SelectionController implements IWindowInputObserver, IWindowFocusObserver, ITimerObserver
	{
		private final EditorWorld m_world;
		private final Brush m_brush;
				
		private boolean m_applyBrush = false;
		
		public SelectionController(EditorWorld world, Brush brush)
		{
			m_brush = brush;
			m_world = world;
		}

		@Override
		public void update(int deltaTime)
		{
			if(m_applyBrush)
				m_brush.apply(m_world);
		}

		@Override
		public void onKeyEvent(InputKeyEvent event)
		{
			if(event.keyCode != KeyEvent.VK_ENTER)
				return;
			
			if(event.type == KeyEventType.KeyUp)
				m_applyBrush = false;
			else if(event.type == KeyEventType.KeyDown)
				m_applyBrush = true;
		}

		@Override
		public void onFocusChanged(boolean hasFocus)
		{
			if(!hasFocus)
				m_applyBrush = false;
		}
		
		@Override
		public void onMouseEvent(InputMouseEvent event) { }
	}
}
