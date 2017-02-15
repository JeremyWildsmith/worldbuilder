/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.jevaengine.builder.worldbuilder.ui.worldeditor.behavior;

import io.github.jevaengine.builder.ui.MessageBoxFactory;
import io.github.jevaengine.builder.ui.TextInputQueryFactory;
import io.github.jevaengine.builder.ui.TextInputQueryFactory.ITextInputQueryObserver;
import io.github.jevaengine.builder.ui.TextInputQueryFactory.TextInputQuery;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeremy
 */
public abstract class BasicBehaviorInjector extends WindowBehaviourInjector {
	private final Logger m_logger = LoggerFactory.getLogger(BasicBehaviorInjector.class);
	protected final WindowManager m_windowManager;
	protected final IWindowFactory m_windowFactory;
	
	public BasicBehaviorInjector(WindowManager windowManager, IWindowFactory windowFactory) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}
	
	protected final void displayMessage(String message) {
		try {
			final MessageBoxFactory.MessageBox msgBox = new MessageBoxFactory(m_windowManager, m_windowFactory).create(message);
			msgBox.getObservers().add(new MessageBoxFactory.IMessageBoxObserver() {
				@Override
				public void okay() {
					msgBox.dispose();
				}
			});
		} catch (IWindowFactory.WindowConstructionException e) {
			m_logger.error("Unable to construct message dialogue", e);
		}
	}
	
	protected final void displayTextInput(String query, String value, final ITextInputQueryObserver observer) {
		try {
			final TextInputQuery window = new TextInputQueryFactory(m_windowManager, m_windowFactory).create(query, value);
			window.getObservers().add(new ITextInputQueryObserver() {
				@Override
				public void okay(String input) {
					observer.okay(input);
					window.dispose();
				}

				@Override
				public void cancel() {
					observer.cancel();
					window.dispose();
				}
			});
		} catch (IWindowFactory.WindowConstructionException ex) {
			m_logger.error("Unable to construct message dialogue", ex);
		}
	}
}
