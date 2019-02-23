/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.jevaengine.builder.worldbuilder.ui.worldeditor.behavior;

import io.github.jevaengine.builder.ui.TextInputQueryFactory.ITextInputQueryObserver;
import io.github.jevaengine.builder.worldbuilder.world.EditorSceneArtifact;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorld;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.graphics.NullGraphic;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.*;
import io.github.jevaengine.ui.Button;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.Label;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.scene.ISceneBuffer;
import io.github.jevaengine.world.scene.ISceneBuffer.ISceneBufferEffect;
import io.github.jevaengine.world.scene.ISceneBuffer.ISceneComponentEffect;
import io.github.jevaengine.world.scene.camera.ControlledCamera;
import io.github.jevaengine.world.scene.effect.DebugDrawComponent;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Collection;

/**
 * @author Jeremy
 */
public class CameraBehaviorInjector extends BasicBehaviorInjector {

    private final EditorWorld m_world;
    private final ControlledCamera m_camera;

    public CameraBehaviorInjector(WindowManager windowManager, IWindowFactory windowFactory, EditorWorld world, ControlledCamera camera) {
        super(windowManager, windowFactory);
        m_world = world;
        m_camera = camera;
    }

    @Override
    protected void doInject() throws NoSuchControlException {
        final Timer logicTimer = new Timer();
        final WorldView worldView = getControl(WorldView.class, "worldView");
        final Label lblCursorCoordinates = getControl(Label.class, "lblCursorCoordinates");
        final Label lblIsTraversable = getControl(Label.class, "lblIsTraversable");
        final CameraController cameraController = new CameraController(worldView);

        m_world.attachCamera(m_camera);

        addControl(logicTimer);
        worldView.setCamera(m_camera);

        m_camera.addEffect(new HideOverCursorEffect());

        logicTimer.getObservers().add(cameraController);
        worldView.getObservers().add(cameraController);
        getObservers().add(cameraController);

        logicTimer.getObservers().add(new Timer.ITimerObserver() {
            @Override
            public void update(int deltaTime) {
                Vector3F coordinates = m_world.getEditCursor().getLocation();
                lblCursorCoordinates.setText(String.format("%f, %f, %f; Snap: %f", coordinates.x, coordinates.y, coordinates.z, cameraController.getCursorSnapGridSize()));
                EditorSceneArtifact tile = m_world.getTile(coordinates);
                lblIsTraversable.setText(tile == null || tile.isTraversable() ? "true" : "false");
            }
        });

        getControl(Button.class, "btnAdjustDepth").getObservers().add(new Button.IButtonPressObserver() {
            private ISceneBufferEffect effect = new DebugDrawComponent();

            @Override
            public void onPress() {
                displayTextInput("Cursor Depth", Float.toString(m_world.getEditCursor().getLocation().z), new AdjustDepthListener());
            }
        });

        getControl(Button.class, "btnToggleWireframe").getObservers().add(new Button.IButtonPressObserver() {
            private final ISceneBufferEffect effect = new DebugDrawComponent();
            private boolean applied = false;

            @Override
            public void onPress() {
                if (applied)
                    m_camera.removeEffect(effect);
                else
                    m_camera.addEffect(effect);

                applied = !applied;
            }
        });

        getControl(Button.class, "btnAdjustZoom").getObservers().add(new Button.IButtonPressObserver() {
            @Override
            public void onPress() {
                displayTextInput("Zoom", Float.toString(m_camera.getZoom()), new AdjustZoomListener());
            }
        });

        getControl(Button.class, "btnAdjustSnapGridSize").getObservers().add(new Button.IButtonPressObserver() {
            @Override
            public void onPress() {
                displayTextInput("Snap", Float.toString(cameraController.getCursorSnapGridSize()), new AdjustSnapListener(cameraController));
            }
        });
    }

    private final class AdjustZoomListener implements ITextInputQueryObserver {

        @Override
        public void okay(String input) {
            try {
                m_camera.setZoom(Float.parseFloat(input));
            } catch (NumberFormatException e) {
                displayMessage("Depth must be a properly formed floating point.");
            }
        }

        @Override
        public void cancel() {
        }
    }

    private final class AdjustSnapListener implements ITextInputQueryObserver {

        private final CameraController m_cameraController;

        public AdjustSnapListener(CameraController cameraController) {
            m_cameraController = cameraController;
        }

        @Override
        public void okay(String input) {
            try {
                m_cameraController.setCursorSnapGridSize(Float.parseFloat(input));
            } catch (NumberFormatException e) {
                displayMessage("Snap grid size must be a properly formed floating point.");
            }
        }

        @Override
        public void cancel() {
        }
    }

    private final class AdjustDepthListener implements ITextInputQueryObserver {

        @Override
        public void okay(String input) {
            try {
                Vector3F cursorLocation = m_world.getEditCursor().getLocation();
                cursorLocation.z = Float.parseFloat(input);
                m_world.getEditCursor().setLocation(cursorLocation);
            } catch (NumberFormatException e) {
                displayMessage("Depth must be a properly formed floating point.");
            }
        }

        @Override
        public void cancel() {
        }
    }

    private class HideOverCursorEffect implements ISceneBuffer.ISceneBufferEffect {
        @Override
        public ISceneBuffer.ISceneComponentEffect[] getComponentEffect(final Graphics2D g, int offsetX, int offsetY, float scale, Vector2D renderLocation, Matrix3X3 projection, ISceneBuffer.ISceneBufferEntry subject, Collection<ISceneBuffer.ISceneBufferEntry> beneath) {
            IEntity dispatcher = subject.getDispatcher();

            if (dispatcher == null || dispatcher.getBody().getLocation().z < m_world.getEditCursor().getLocation().z + 0.0001) {
                return new ISceneComponentEffect[0];
            }

            return new ISceneComponentEffect[]{
                    new ISceneComponentEffect() {
                        @Override
                        public void prerender() {
                        }

                        @Override
                        public boolean ignore(IEntity dispatcher, IImmutableSceneModel.ISceneModelComponent c) {
                            return true;
                        }

                        @Override
                        public void postrender() {

                        }
                    }
            };
        }

    }

    private class CameraController implements Window.IWindowFocusObserver, Timer.ITimerObserver, WorldView.IWorldViewInputObserver {

        private Vector3F m_cameraMovement = new Vector3F();
        private float m_snapGridSize = 1.0F;
        private final WorldView m_worldView;

        public CameraController(WorldView worldView) {
            m_worldView = worldView;
        }

        @Override
        public void update(int deltaTime) {
            if (!m_cameraMovement.isZero()) {
                m_camera.move(m_cameraMovement.normalize().multiply(deltaTime / 200.0F * m_snapGridSize * (1.0f / m_camera.getZoom()) * 30));
            }
        }

        public float getCursorSnapGridSize() {
            return m_snapGridSize;
        }

        public void setCursorSnapGridSize(float gridSize) {
            m_snapGridSize = Math.max(gridSize, 0.01F);
        }

        @Override
        public void onFocusChanged(boolean hasFocus) {
            if (!hasFocus) {
                m_cameraMovement = new Vector3F();
            }
        }

        @Override
        public void keyEvent(InputKeyEvent e) {
            if (e.type == InputKeyEvent.KeyEventType.KeyDown) {
                switch (e.keyCode) {
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
            } else if (e.type == InputKeyEvent.KeyEventType.KeyUp) {
                switch (e.keyCode) {
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
        public void mouseEvent(InputMouseEvent event) {
            if (event.type == InputMouseEvent.MouseEventType.MouseMoved) {
                Vector2F location = m_worldView.translateScreenToWorld(new Vector2F(event.location));

                location.x = (int) (location.x / m_snapGridSize) * m_snapGridSize;
                location.y = Math.round(location.y / m_snapGridSize) * m_snapGridSize;
                float z = m_world.getEditCursor().getLocation().z;

                m_world.getEditCursor().setLocation(new Vector3F(location, z));
            }
        }

    }
}
