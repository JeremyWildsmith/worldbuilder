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
package io.github.jevaengine.builder.worldbuilder.ui;

import io.github.jevaengine.IDisposable;
import io.github.jevaengine.builder.ui.FileInputQueryFactory;
import io.github.jevaengine.builder.ui.FileInputQueryFactory.FileInputQuery;
import io.github.jevaengine.builder.ui.FileInputQueryFactory.FileInputQueryMode;
import io.github.jevaengine.builder.ui.FileInputQueryFactory.IFileInputQueryObserver;
import io.github.jevaengine.builder.ui.MessageBoxFactory;
import io.github.jevaengine.builder.ui.MessageBoxFactory.IMessageBoxObserver;
import io.github.jevaengine.builder.ui.MessageBoxFactory.MessageBox;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.util.Observers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class OpenWorldQueryFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/openWorld.jwl");
	
	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	private final URI m_base;
	
	public OpenWorldQueryFactory(WindowManager windowManager, IWindowFactory windowFactory, URI base)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
		m_base = base;
	}
	
	public OpenWorldQuery create() throws WindowConstructionException
	{
		Observers observers = new Observers();

		Window window = m_windowFactory.create(WINDOW_LAYOUT, new OpenWorldQueryBehaviourInjector(observers));
		m_windowManager.addWindow(window);
			
		window.center();
		return new OpenWorldQuery(observers, window);
		
	}
	
	public static class OpenWorldQuery implements IDisposable
	{
		private final IObserverRegistry m_observers;
		
		private final Window m_window;
		
		private OpenWorldQuery(IObserverRegistry observers, Window window)
		{
			m_observers = observers;
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
		
		public IObserverRegistry getObservers()
		{
			return m_observers;
		}
	}
	
	private class OpenWorldQueryBehaviourInjector extends WindowBehaviourInjector
	{
		private final Logger m_logger = LoggerFactory.getLogger(OpenWorldQueryBehaviourInjector.class);
		private final Observers m_observers;

		public OpenWorldQueryBehaviourInjector(Observers observers)
		{
			m_observers = observers;
		}
		
		@Nullable
		private Integer parseInteger(String s)
		{
			try
			{
				return Integer.parseInt(s);
			} catch(NumberFormatException e)
			{
				return null;
			}
		}
		
		private void validationFailed(String cause)
		{
			try
			{
				final MessageBox msgBox = new MessageBoxFactory(m_windowManager, m_windowFactory).create("Validation failed: " + cause);
				
				msgBox.getObservers().add(new IMessageBoxObserver() {
					@Override
					public void okay() {
						msgBox.dispose();
					}
				});
			} catch(WindowConstructionException e)
			{
				m_logger.error("Unable to construct messagebox notifying uses of validation failures.", e);
			}
		}
		
		@Override
		protected void doInject() throws NoSuchControlException
		{
			final TextArea txtWorld = getControl(TextArea.class, "txtWorld");
			
			getControl(Button.class, "btnBrowseWorld").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					try {
						final FileInputQuery query = new FileInputQueryFactory(m_windowManager, m_windowFactory, m_base).create(FileInputQueryMode.OpenFile, "World to open", m_base);
						query.getObservers().add(new IFileInputQueryObserver() {
							@Override
							public void okay(URI input) {
								txtWorld.setText(input.toString());
								query.dispose();
							}
							
							@Override
							public void cancel() {
								query.dispose();
							}
						});
					} catch (WindowConstructionException e) {
						m_logger.error("Unable to construct dialogue to browse for world", e);
					}
				}
			});
			
			getControl(Button.class, "btnOkay").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					try {
						m_observers.raise(IOpenWorldQueryObserver.class).okay(new URI(txtWorld.getText()));
					} catch (URISyntaxException e)
					{
						m_logger.error("Unable to parse URI of world path." , e);
						validationFailed("The URI provided to the world directory was not valid.");
					}
				}
			});
			
			getControl(Button.class, "btnCancel").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(IOpenWorldQueryObserver.class).cancel();
				}
			});
		}
	}
	
	public interface IOpenWorldQueryObserver
	{
		void okay(URI world);
		void cancel();
	}
}
