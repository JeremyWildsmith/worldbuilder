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
import io.github.jevaengine.builder.worldbuilder.world.EditorZone;
import io.github.jevaengine.math.Rect3F;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector3F;
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

public final class ConfigureZoneQueryFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/configureZone.jwl");
	
	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	
	private final URI m_base;
	
	public ConfigureZoneQueryFactory(WindowManager windowManager, IWindowFactory windowFactory, URI base)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
		m_base = base;
	}
	
	public ConfigureZoneQuery create(EditorZone subject) throws WindowConstructionException
	{
		Observers observers = new Observers();
			
		Window window = m_windowFactory.create(WINDOW_LAYOUT, new ConfigureZoneQueryBehaviourInjector(observers, subject));
		m_windowManager.addWindow(window);
			
		window.center();
		return new ConfigureZoneQuery(observers, window);
	}
	
	public static class ConfigureZoneQuery implements IDisposable
	{
		private final IObserverRegistry m_observers;
		
		private final Window m_window;
		
		private ConfigureZoneQuery(IObserverRegistry observers, Window window)
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
		
		public Vector2D getLocation()
		{
			return m_window.getLocation();
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
	
	private class ConfigureZoneQueryBehaviourInjector extends WindowBehaviourInjector
	{
		private final Logger m_logger = LoggerFactory.getLogger(ConfigureEntityQueryFactory.class);
		
		private final Observers m_observers;
		
		private final EditorZone m_subject;
		
		public ConfigureZoneQueryBehaviourInjector(Observers observers, EditorZone subject)
		{
			m_subject = subject;
			m_observers = observers;
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
		
		
		@Nullable
		private Vector3F parseVector3F(String vector)
		{
			String[] components = vector.split(",[ ]*");
			
			if(components.length != 3)
				return null;
			
			try
			{
				Vector3F buffer = new Vector3F();
				
				buffer.x = Float.parseFloat(components[0]);
				buffer.y = Float.parseFloat(components[1]);
				buffer.z = Float.parseFloat(components[2]);
				
				return buffer;
			}catch(NumberFormatException e)
			{
				return null;
			}
		}
		
		@Override
		protected void doInject() throws NoSuchControlException
		{
			final TextArea txtName = getControl(TextArea.class, "txtName");
			final TextArea txtLocation = getControl(TextArea.class, "txtLocation");
			final TextArea txtBounds = getControl(TextArea.class, "txtBounds");
			
			txtName.setText(m_subject.getName());
			txtLocation.setText(String.format("%.2f, %.2f, %.2f", m_subject.getLocation().x, m_subject.getLocation().y, m_subject.getLocation().z));
			txtBounds.setText(String.format("%.2f, %.2f, %.2f", m_subject.getBounds().width, m_subject.getBounds().height, m_subject.getBounds().depth));

			getControl(Button.class, "btnApply").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					String name = txtName.getText();
					String location = txtLocation.getText();
					String bounds = txtBounds.getText();
					Vector3F locVector = parseVector3F(location);
					Vector3F boundsVector = parseVector3F(bounds);
						
					if(name.length() <= 0)
						validationFailed("You must specify a name ofr the entity.");
					else if(locVector == null)
						validationFailed("Unable to parse location vector. Assure it is properly formatted.");
					else if(boundsVector == null)
						validationFailed("Unable to parse bounds vector. Assure it is properly formatted.");	
					else
					{
						m_subject.setName(name);
						m_subject.setLocation(locVector);
						m_subject.setBounds(new Rect3F(boundsVector.x, boundsVector.y, boundsVector.z));
						m_observers.raise(IConfigureZoneQueryObserver.class).apply();			
					}
				}
			});
			
			getControl(Button.class, "btnCancel").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(IConfigureZoneQueryObserver.class).cancel();
				}
			});
			
			getControl(Button.class, "btnDelete").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(IConfigureZoneQueryObserver.class).delete();
				}
			});
		}
	}
	
	public interface IConfigureZoneQueryObserver
	{
		void apply();
		void delete();
		void cancel();
	}}
