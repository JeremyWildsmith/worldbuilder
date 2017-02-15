/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.jevaengine.builder.worldbuilder.ui.worldeditor.behavior;

import io.github.jevaengine.builder.worldbuilder.ui.ConfigureZoneQueryFactory;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorld;
import io.github.jevaengine.builder.worldbuilder.world.EditorZone;
import io.github.jevaengine.builder.worldbuilder.world.brush.Brush;
import io.github.jevaengine.builder.worldbuilder.world.brush.MoveEntityBrushBehaviour;
import io.github.jevaengine.builder.worldbuilder.world.brush.NullBrushBehaviour;
import io.github.jevaengine.builder.worldbuilder.world.brush.ResizeZoneBrushBehaviour;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.MenuStrip;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.WindowManager;
import io.github.jevaengine.ui.WorldView;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.scene.camera.ControlledCamera;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeremy
 */
public class ZoneBehaviorInjector extends BasicBehaviorInjector {

	private final EditorWorld m_world;
	private final ControlledCamera m_camera;
	private final Brush m_brush;
	private final IFontFactory m_fontFactory;
	private final URI m_baseDir;

	private final Logger m_logger = LoggerFactory.getLogger(ZoneBehaviorInjector.class);

	public ZoneBehaviorInjector(WindowManager windowManager, IWindowFactory windowFactory, EditorWorld world, ControlledCamera camera, Brush brush, URI baseDir, IFontFactory fontFactory) {
		super(windowManager, windowFactory);
		m_world = world;
		m_camera = camera;
		m_brush = brush;
		m_baseDir = baseDir;
		m_fontFactory = fontFactory;
	}

	private void resizeZone(final EditorZone zone) {
		Vector3F bottomRightTop = zone.getBounds().getPoint(1.0F, 1.0F, 1.0F).add(zone.getLocation());
		m_camera.lookAt(bottomRightTop);
		m_brush.setBehaviour(new ResizeZoneBrushBehaviour(zone, new ResizeZoneBrushBehaviour.IResizeZoneBrushBehaviourHandler() {
			@Override
			public void resized() {
				m_brush.setBehaviour(new NullBrushBehaviour());
			}
		}));
	}

	@Nullable
	private ConfigureZoneQueryFactory.ConfigureZoneQuery configureZone(final EditorZone base) {
		try {
			final ConfigureZoneQueryFactory.ConfigureZoneQuery query = new ConfigureZoneQueryFactory(m_windowManager, m_windowFactory, m_baseDir).create(base);
			query.getObservers().add(new ConfigureZoneQueryFactory.IConfigureZoneQueryObserver() {
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
		} catch (IWindowFactory.WindowConstructionException e) {
			m_logger.error("Unable to construct zone configuration dialogue.", e);
			return null;
		}
	}

	private void displayContextMenu(MenuStrip menuStrip, final EditorZone.DummyZone zone) {
		menuStrip.setContext(new String[]{"Move Zone", "Edit Zone", "Resize Zone"}, new MenuStrip.IMenuStripListener() {
			@Override
			public void onCommand(String command) {
				switch (command) {
					case "Move Zone":
						moveZone(zone);
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

	private void moveZone(final IEntity entity) {
		m_camera.lookAt(entity.getBody().getLocation());
		m_brush.setBehaviour(new MoveEntityBrushBehaviour(entity, new MoveEntityBrushBehaviour.IEntityMovementBrushBehaviorHandler() {
			@Override
			public void moved() {
				m_brush.setBehaviour(new NullBrushBehaviour());
			}
		}));
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
				if (event.type != InputMouseEvent.MouseEventType.MouseClicked)
					return;
				
				final Vector2D location = event.location;
				final IEntity pickedEntity = worldView.pick(IEntity.class, location);
				
				if (event.mouseButton == InputMouseEvent.MouseButton.Right) {
					if (pickedEntity instanceof EditorZone.DummyZone)
						displayContextMenu(menuStrip, (EditorZone.DummyZone) pickedEntity);
					
					menuStrip.setLocation(location.add(worldView.getLocation()));
				} else
					menuStrip.setVisible(false);
			}
		});
	}
}
