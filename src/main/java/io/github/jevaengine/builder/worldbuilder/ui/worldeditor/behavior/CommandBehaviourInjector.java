/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.jevaengine.builder.worldbuilder.ui.worldeditor.behavior;

import io.github.jevaengine.builder.ui.FileInputQueryFactory;
import io.github.jevaengine.builder.worldbuilder.ui.SelectBrushQuery;
import io.github.jevaengine.builder.worldbuilder.ui.SelectLayerQuery;
import io.github.jevaengine.builder.worldbuilder.ui.worldeditor.EditorWorldViewFactory;
import io.github.jevaengine.builder.worldbuilder.world.*;
import io.github.jevaengine.builder.worldbuilder.world.brush.Brush;
import io.github.jevaengine.builder.worldbuilder.world.brush.MoveEntityBrushBehaviour;
import io.github.jevaengine.builder.worldbuilder.world.brush.NullBrushBehaviour;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.DefaultWorldFactory;
import io.github.jevaengine.world.IWeatherFactory;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.camera.ControlledCamera;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jeremy
 */
public class CommandBehaviourInjector extends BasicBehaviorInjector {

	private final Brush m_workingBrush;
	private final SelectBrushQuery m_selectBrushQuery;
	private final SelectLayerQuery m_selectLayerQuery;
	private final EditorWorld m_world;
	private final ControlledCamera m_camera;
	private final ISceneModelFactory m_modelFactory;
	private final IFontFactory m_fontFactory;
	private final Observers m_observers;
	private final EditorWeatherFactory m_weatherFactory;
	private final URI m_baseDirectory;
	private final Logger m_logger = LoggerFactory.getLogger(CommandBehaviourInjector.class);

	public CommandBehaviourInjector(WindowManager windowManager, IWindowFactory windowFactory, EditorWeatherFactory weatherFactory, URI baseDirectory, IFontFactory fontFactory, Observers observers, EditorWorld world, ISceneBufferFactory sceneBufferFactory, ISceneModelFactory modelFactory, Brush workingBrush, SelectBrushQuery selectBrushQuery, SelectLayerQuery selectLayerQuery) {
		super(windowManager, windowFactory);

		m_selectLayerQuery = selectLayerQuery;
		m_weatherFactory = weatherFactory;
		m_baseDirectory = baseDirectory;
		m_fontFactory = fontFactory;
		m_observers = observers;
		m_world = world;
		m_camera = new ControlledCamera(sceneBufferFactory);
		world.attachCamera(m_camera);
		m_modelFactory = modelFactory;
		m_selectBrushQuery = selectBrushQuery;
		m_workingBrush = workingBrush;
	}

	private EditorZone createUnnamedZone() {
		Set<String> usedNames = new HashSet<>();

		for (EditorZone z : m_world.getZones()) {
			usedNames.add(z.getName());
		}

		for (int i = 0;; i++) {
			String name = "Unnamed" + i;
			if (!usedNames.contains(name)) {
				return new EditorZone(m_fontFactory, name);
			}
		}
	}

	private void moveEntity(final IEntity entity) {
		m_camera.lookAt(entity.getBody().getLocation());
		m_workingBrush.setBehaviour(new MoveEntityBrushBehaviour(entity, new MoveEntityBrushBehaviour.IEntityMovementBrushBehaviorHandler() {
			@Override
			public void moved() {
				m_workingBrush.setBehaviour(new NullBrushBehaviour());
			}
		}));
	}

	private void displayDefaultContextMenu(MenuStrip menuStrip) {
		menuStrip.setContext(new String[]{"Create Entity", "Create Zone"}, new MenuStrip.IMenuStripListener() {
			@Override
			public void onCommand(String command) {
				if (command.equals("Create Entity")) {
					EditorEntity base = createUnnamedEntity();

					base.setLocation(m_camera.getLookAt());
					m_world.addEntity(base);

					moveEntity(base.getEntity());
				} else if (command.equals("Create Zone")) {
					EditorZone base = createUnnamedZone();

					base.setLocation(m_camera.getLookAt());
					m_world.addZone(base);

					moveEntity(base.getEntity());
				}
			}
		});
	}

	private void saveWorld(URI destination) {
		DefaultWorldFactory.WorldConfiguration config = m_world.createWorldConfiguration();
		try (final FileOutputStream fos = new FileOutputStream(new File(m_baseDirectory.resolve(URI.create("/").relativize(destination))))) {
			JsonVariable var = new JsonVariable();
			var.setValue(config);
			var.serialize(fos, true);
			displayMessage("World has been saved successfully.");
		} catch (IOException | ValueSerializationException e) {
			m_logger.error("Unable to save world", e);
			displayMessage("Error occured attmepting to save world. View log for more details.");
		}
	}

	private void verifyAndSaveWorld(URI destination) {
		Map<String, EditorEntity> usedEntityNames = new HashMap<>();
		Map<String, EditorZone> usedZoneNames = new HashMap<>();
		final String message = "Due to a %s name conflict with the name '%s', the world could not be saved. Please resolve this issue before attempting to save again.";
		for (EditorEntity e : m_world.getEntities()) {
			if (usedEntityNames.containsKey(e.getName())) {
				displayMessage(String.format(message, "entity", e.getName()));
				return;
			} else {
				usedEntityNames.put(e.getName(), e);
			}
		}
		for (EditorZone z : m_world.getZones()) {
			if (usedZoneNames.containsKey(z.getName())) {
				displayMessage(String.format(message, "zone", z.getName()));
				return;
			} else {
				usedZoneNames.put(z.getName(), z);
			}
		}
		saveWorld(destination);
	}

	private EditorEntity createUnnamedEntity() {
		Set<String> usedNames = new HashSet<>();
		for (EditorEntity e : m_world.getEntities()) {
			usedNames.add(e.getName());
		}
		for (int i = 0;; i++) {
			String name = "Unnamed" + i;
			if (!usedNames.contains(name)) {
				return new EditorEntity(m_fontFactory, m_modelFactory, name, "");
			}
		}
	}

	@Override
	protected void doInject() throws NoSuchControlException {
		final Timer logicTimer = new Timer();

		final WorldView worldView = getControl(WorldView.class, "worldView");
		final MenuStrip menuStrip = new MenuStrip();

		addControl(menuStrip);
		addControl(logicTimer);

		logicTimer.getObservers().add(new Timer.ITimerObserver() {
			@Override
			public void update(int deltaTime) {
				if (m_selectBrushQuery.isVisible() != CommandBehaviourInjector.this.isVisible() ||
						m_selectLayerQuery.isVisible() != CommandBehaviourInjector.this.isVisible()) {
					m_selectBrushQuery.setVisible(CommandBehaviourInjector.this.isVisible());
					m_selectLayerQuery.setVisible(CommandBehaviourInjector.this.isVisible());
				}
				m_selectBrushQuery.poll();
				m_selectLayerQuery.poll();
				m_world.update(deltaTime);
			}
		});

		worldView.getObservers().add(new WorldView.IWorldViewInputObserver() {
			@Override
			public void keyEvent(InputKeyEvent event) {
			}

			@Override
			public void mouseEvent(InputMouseEvent event) {
				if (event.type != InputMouseEvent.MouseEventType.MouseClicked) {
					return;
				}

				final Vector2D location = event.location;
				final IEntity pickedEntity = worldView.pick(IEntity.class, location);

				if (event.mouseButton == InputMouseEvent.MouseButton.Right) {
					if (pickedEntity == null || pickedEntity instanceof EditorSceneArtifact.DummySceneArtifact) {
						displayDefaultContextMenu(menuStrip);
					}

					menuStrip.setLocation(location.add(worldView.getLocation()));
				} else {
					menuStrip.setVisible(false);
				}
			}
		});

		getControl(Button.class, "btnChangeWeather").getObservers().add(new Button.IButtonPressObserver() {
			@Override
			public void onPress() {
				try {
					EditorWeatherFactory.EditorWeather weather = m_world.getWeather();
					URI currentWeather = weather == null ? m_baseDirectory : weather.getName();
					final FileInputQueryFactory.FileInputQuery query = new FileInputQueryFactory(m_windowManager, m_windowFactory, m_baseDirectory).create(FileInputQueryFactory.FileInputQueryMode.OpenFile, "Weather: ", currentWeather);
					query.getObservers().add(new FileInputQueryFactory.IFileInputQueryObserver() {
						@Override
						public void okay(URI input) {
							try {
								m_world.setWeather(m_weatherFactory.create(input));
								query.dispose();
							} catch (IWeatherFactory.WeatherConstructionException e) {
								displayMessage("Error constructing world weather. Either due to the fact that is it not properly formatted, or it does not exist.");
							}
						}

						@Override
						public void cancel() {
							query.dispose();
						}
					});
				} catch (IWindowFactory.WindowConstructionException e) {
					m_logger.error("Unable to construct text input dialogue for depth adjust", e);
				}
			}
		});

		getControl(Button.class, "btnClose").getObservers().add(new Button.IButtonPressObserver() {
			@Override
			public void onPress() {
				m_observers.raise(EditorWorldViewFactory.IEditorWorldViewObserver.class).close();
			}
		});

		getControl(Button.class, "btnResetBrush").getObservers().add(new Button.IButtonPressObserver() {
			@Override
			public void onPress() {
				m_workingBrush.setBehaviour(new NullBrushBehaviour());
			}
		});

		getControl(Button.class, "btnSave").getObservers().add(new Button.IButtonPressObserver() {
			@Override
			public void onPress() {
				try {
					final FileInputQueryFactory.FileInputQuery query = new FileInputQueryFactory(m_windowManager, m_windowFactory, m_baseDirectory).create(FileInputQueryFactory.FileInputQueryMode.SaveFile, "World Save Location", m_baseDirectory);
					query.getObservers().add(new FileInputQueryFactory.IFileInputQueryObserver() {
						@Override
						public void okay(URI input) {
							verifyAndSaveWorld(input);
							query.dispose();
						}

						@Override
						public void cancel() {
							query.dispose();
						}
					});
				} catch (IWindowFactory.WindowConstructionException e) {
					m_logger.error("Could not construct save world dialogue", e);
				}
			}
		});
	}

}
