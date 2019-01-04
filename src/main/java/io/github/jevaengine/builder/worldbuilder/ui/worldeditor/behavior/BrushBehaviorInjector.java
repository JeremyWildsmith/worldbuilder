/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.jevaengine.builder.worldbuilder.ui.worldeditor.behavior;

import io.github.jevaengine.builder.worldbuilder.world.EditorSceneArtifact;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorld;
import io.github.jevaengine.builder.worldbuilder.world.brush.*;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.ui.*;

import java.awt.event.KeyEvent;

/**
 *
 * @author Jeremy
 */
public class BrushBehaviorInjector extends BasicBehaviorInjector {

	private final Brush m_brush;
	private final EditorWorld m_world;

	public BrushBehaviorInjector(WindowManager windowManager, IWindowFactory windowFactory, EditorWorld world, Brush brush) {
		super(windowManager, windowFactory);
		m_world = world;
		m_brush = brush;
	}

	@Override
	protected void doInject() throws NoSuchControlException {
		final Timer logicTimer = new Timer();
		addControl(logicTimer);

		BrushSelectionController selectionController = new BrushSelectionController(getControl(WorldView.class, "worldView"));
		getObservers().add(selectionController);
		logicTimer.getObservers().add(selectionController);

		m_brush.getObservers().add(new Brush.IBrushBehaviorObserver() {
			@Override
			public void behaviourChanged(IBrushBehaviour behaviour) {
				m_world.getEditCursor().setModel(behaviour.getModel());
			}
		});

		getControl(Button.class, "btnClearBrush").getObservers().add(new Button.IButtonPressObserver() {
			@Override
			public void onPress() {
				m_brush.setBehaviour(new ClearTileBrushBehaviour());
			}
		});
		
		getControl(Button.class, "btnSampleBrush").getObservers().add(new Button.IButtonPressObserver() {
			@Override
			public void onPress() {
				m_brush.setBehaviour(new SampleSceneArtifactBrushBehaviour(new SampleSceneArtifactBrushBehaviour.ISceneArtifactSampleHandler() {
					@Override
					public void sample(EditorSceneArtifact sample) {
						m_brush.setBehaviour(new PlaceSceneArtifactBrushBehaviour(sample.getModel(), sample.getModelName(), sample.getDirection(), sample.isTraversable(), sample.isStatic()));
					}
				}));
			}
		});
	}

	private class BrushSelectionController implements Window.IWindowInputObserver, Window.IWindowFocusObserver, Timer.ITimerObserver {

		private boolean m_applyBrush = false;
		private WorldView m_view;

		private BrushSelectionController(WorldView view) {
			m_view = view;
		}

		@Override
		public void update(int deltaTime) {
			if (m_applyBrush) {
				m_brush.apply(m_world);
			}
		}

		@Override
		public void onKeyEvent(InputKeyEvent event) {
			if (event.keyCode == KeyEvent.VK_R && event.type == InputKeyEvent.KeyEventType.KeyUp) {
				m_brush.setDirection(m_brush.getDirection().getClockwise());
			}
		}

		@Override
		public void onFocusChanged(boolean hasFocus) {
			if (!hasFocus) {
				m_applyBrush = false;
			}
		}

		@Override
		public void onMouseEvent(InputMouseEvent event) {
			if(event.type == InputMouseEvent.MouseEventType.MousePressed) {
				if(m_view.getBounds().add(m_view.getLocation()).contains(event.location))
					m_applyBrush = true;
			} else if (event.type == InputMouseEvent.MouseEventType.MouseReleased) {
				m_applyBrush = false;
			}
		}

	}

}
