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
import io.github.jevaengine.builder.ui.MessageBoxFactory;
import io.github.jevaengine.builder.ui.MessageBoxFactory.IMessageBoxObserver;
import io.github.jevaengine.builder.ui.MessageBoxFactory.MessageBox;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.Button;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.TextArea;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WindowManager;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.util.Observers;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CreateWorldQueryFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/createWorld.jwl");
	
	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	
	public CreateWorldQueryFactory(WindowManager windowManager, IWindowFactory windowFactory)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}
	
	public CreateWorldQuery create() throws WindowConstructionException
	{
		Observers observers = new Observers();

		Window window = m_windowFactory.create(WINDOW_LAYOUT, new CreateWorldQueryBehaviourInjector(observers));
		m_windowManager.addWindow(window);
			
		window.center();
		return new CreateWorldQuery(observers, window);
		
	}
	
	public static class CreateWorldQuery implements IDisposable
	{
		private final IObserverRegistry m_observers;
		
		private final Window m_window;
		
		private CreateWorldQuery(IObserverRegistry observers, Window window)
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
	
	private class CreateWorldQueryBehaviourInjector extends WindowBehaviourInjector
	{
		private final Logger m_logger = LoggerFactory.getLogger(CreateWorldQueryBehaviourInjector.class);
		private final Observers m_observers;

		public CreateWorldQueryBehaviourInjector(Observers observers)
		{
			m_observers = observers;
		}
		
		private void validationFailed(String cause)
		{
			try
			{
				final MessageBox msgBox = new MessageBoxFactory(m_windowManager, m_windowFactory).create(cause);
				
				msgBox.getObservers().add(new IMessageBoxObserver() {
					@Override
					public void okay() {
						msgBox.dispose();
					}
				});
				
			} catch (WindowConstructionException e) {
				m_logger.error("Unable to notify use of validation failures", e);
			}
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
		
		@Nullable
		private Float parseFloat(String s)
		{
			try
			{
				return Float.parseFloat(s);
			} catch (NumberFormatException e)
			{
				return null;
			}
		}
		
		@Override
		protected void doInject() throws NoSuchControlException
		{
			final TextArea txtWidth = getControl(TextArea.class, "txtWidth");
			final TextArea txtHeight = getControl(TextArea.class, "txtHeight");
			final TextArea txtFriction = getControl(TextArea.class, "txtFriction");
			final TextArea txtMetersPerUnit = getControl(TextArea.class, "txtMetersPerUnit");
			final TextArea txtLogicPerUnit = getControl(TextArea.class, "txtLogicPerUnit");
			
			getControl(Button.class, "btnOkay").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					Integer width = parseInteger(txtWidth.getText());
					Integer height = parseInteger(txtHeight.getText());
					Float friction = parseFloat(txtFriction.getText());
					Float metersPerUnit = parseFloat(txtMetersPerUnit.getText());
					Float logicPerUnit = parseFloat(txtLogicPerUnit.getText());
					
					if(width == null || width <= 0)
						validationFailed("'Width' property must be a properly formed number greater than 0.");
					else if(height == null || height <= 0)
						validationFailed("'Height' property must be a properly formed number greater than 0.");
					else if(friction == null || friction < 0)
						validationFailed("'Friction' property must be a properly formed floating point greater than 0.");
					else if(metersPerUnit == null || metersPerUnit < 0)
						validationFailed("'MetersPerUnit' property must be a properly formed floating point greater than 0.");
					else if(logicPerUnit == null || logicPerUnit < 0)
						validationFailed("'LogicPerUnit' property must be a properly formed floating point greater than 0.");
					else
						m_observers.raise(ICreateWorldQueryObserver.class).okay(width, height, friction, metersPerUnit, logicPerUnit);
				}
			});
			
			getControl(Button.class, "btnCancel").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(ICreateWorldQueryObserver.class).cancel();
				}
			});
		}
	}
	
	public interface ICreateWorldQueryObserver
	{
		void okay(int width, int height, float friction, float metersPerUnit, float logicPerUnit);
		void cancel();
	}
}
