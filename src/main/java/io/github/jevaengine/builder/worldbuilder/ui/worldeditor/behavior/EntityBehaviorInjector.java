/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.jevaengine.builder.worldbuilder.ui.worldeditor.behavior;

import io.github.jevaengine.builder.worldbuilder.ui.ConfigureEntityQueryFactory;
import io.github.jevaengine.builder.worldbuilder.world.EditorEntity;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorld;
import io.github.jevaengine.builder.worldbuilder.world.brush.Brush;
import io.github.jevaengine.builder.worldbuilder.world.brush.MoveEntityBrushBehaviour;
import io.github.jevaengine.builder.worldbuilder.world.brush.NullBrushBehaviour;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.ui.Button;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.MenuStrip;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.WindowManager;
import io.github.jevaengine.ui.WorldView;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.scene.camera.ControlledCamera;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeremy
 */
public class EntityBehaviorInjector extends BasicBehaviorInjector {

	private final Brush m_brush;
	private final EditorWorld m_world;
	private final ControlledCamera m_camera;
	private final URI m_baseDir;
	private final ISceneModelFactory m_modelFactory;
	private final IFontFactory m_fontFactory;

	private final Logger m_logger = LoggerFactory.getLogger(EntityBehaviorInjector.class);

	public EntityBehaviorInjector(URI baseDir, WindowManager windowManager, IWindowFactory windowFactory, IFontFactory fontFactory, ISceneModelFactory modelFactory, ControlledCamera camera, EditorWorld world, Brush brush) {
		super(windowManager, windowFactory);
		m_baseDir = baseDir;
		m_world = world;
		m_brush = brush;
		m_fontFactory = fontFactory;
		m_modelFactory = modelFactory;
		m_camera = camera;
	}

    private void moveEntity(final IEntity entity) {
	    moveEntity(entity, null);
    }

    private void moveEntity(final IEntity entity, @Nullable MoveEntityBrushBehaviour.IEntityMovementBrushBehaviorHandler movementHandler) {
	    MoveEntityBrushBehaviour.IEntityMovementBrushBehaviorHandler handler = () -> {
			m_brush.setBehaviour(new NullBrushBehaviour());
		};

		m_brush.setBehaviour(new MoveEntityBrushBehaviour(entity, movementHandler == null ? handler : movementHandler));
	}

	private void copyEntity(final EditorEntity.DummyEntity sourceEntity, boolean chained) {
		final EditorEntity clone = createUnnamedEntity();
		EditorEntity source = sourceEntity.getEditorEntity();
		
		clone.setAuxiliaryConfig(source.getAuxiliaryConfig());
		clone.setDirection(source.getDirection());
		clone.setClassName(source.getClassName());
		clone.setConfig(source.getConfig());
		clone.setLocation(source.getLocation());

		m_world.addEntity(clone);

		if(chained)
		    moveEntity(clone.getEntity(), () -> copyEntity(clone.getEntity(), true));
		else
		    moveEntity(clone.getEntity());
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

	@Nullable
	private ConfigureEntityQueryFactory.ConfigureEntityQuery configureEntity(final EditorEntity base) {
		try {
			final ConfigureEntityQueryFactory.ConfigureEntityQuery query = new ConfigureEntityQueryFactory(m_windowManager, m_windowFactory, m_baseDir).create(base);
			query.getObservers().add(new ConfigureEntityQueryFactory.IConfigureEntityQueryObserver() {
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
		} catch (IWindowFactory.WindowConstructionException e) {
			m_logger.error("Unable to construct entity configuration dialogue.", e);
			return null;
		}
	}

	private void displayContextMenu(MenuStrip menuStrip, final EditorEntity.DummyEntity entity) {
		menuStrip.setContext(new String[]{"Move Entity", "Configure Entity", "Copy Entity", "Multiple Copy Entity"}, new MenuStrip.IMenuStripListener() {
			@Override
			public void onCommand(String command) {
				switch (command) {
					case "Move Entity":
						moveEntity(entity);
						break;
                    case "Copy Entity":
                        copyEntity(entity, false);
                        break;
                    case "Multiple Copy Entity":
                        copyEntity(entity, true);
                        break;
					case "Configure Entity":
						configureEntity(entity.getEditorEntity());
						break;
				}
			}
		});
	}

	@Override
	protected void doInject() throws NoSuchControlException {
		final WorldView worldView = getControl(WorldView.class, "worldView");
		final MenuStrip menuStrip = new MenuStrip();

		addControl(menuStrip);

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
					if (pickedEntity instanceof EditorEntity.DummyEntity) {
						displayContextMenu(menuStrip, (EditorEntity.DummyEntity) pickedEntity);
					}

					menuStrip.setLocation(location.add(worldView.getLocation()));
				} else {
					menuStrip.setVisible(false);
				}
			}
		});

		getControl(Button.class, "btnCreateEntity").getObservers().add(new Button.IButtonPressObserver() {
			@Override
			public void onPress() {
				final EditorEntity base = createUnnamedEntity();
				m_world.addEntity(base);
				configureEntity(base);
			}
		});
	}

}
