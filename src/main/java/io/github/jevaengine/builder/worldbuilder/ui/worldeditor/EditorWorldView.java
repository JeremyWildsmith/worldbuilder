/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.jevaengine.builder.worldbuilder.ui.worldeditor;

import io.github.jevaengine.IDisposable;
import io.github.jevaengine.builder.worldbuilder.ui.SelectBrushQuery;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorld;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.util.IObserverRegistry;
import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeremy
 */
public final class EditorWorldView implements IDisposable {
	
	private final Logger m_logger = LoggerFactory.getLogger(EditorWorldView.class);
	private final Window m_window;
	private final IObserverRegistry m_observers;
	private final EditorWorld m_world;
	private final SelectBrushQuery m_selectBrushQuery;

	public EditorWorldView(Window window, IObserverRegistry observers, EditorWorld world, SelectBrushQuery selectBrushQuery) {
		m_observers = observers;
		m_window = window;
		m_world = world;
		m_selectBrushQuery = selectBrushQuery;
	}

	@Override
	public void dispose() {
		m_window.dispose();
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

	public void setVisible(boolean isVisible) {
		m_window.setVisible(isVisible);
	}

	public void setLocation(Vector2D location) {
		m_window.setLocation(location);
	}

	public IObserverRegistry getObservers() {
		return m_observers;
	}
	
}
