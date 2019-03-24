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
import io.github.jevaengine.builder.worldbuilder.world.EditorEntity;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public final class ConfigureEntityQueryFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/configureEntity.jwl");
	
	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	
	private final URI m_base;
	
	public ConfigureEntityQueryFactory(WindowManager windowManager, IWindowFactory windowFactory, URI base)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
		m_base = base;
	}
	
	public ConfigureEntityQuery create(EditorEntity subject) throws WindowConstructionException
	{
		Observers observers = new Observers();
			
		Window window = m_windowFactory.create(WINDOW_LAYOUT, new ConfigureEntityQueryBehaviourInjector(observers, subject));
		m_windowManager.addWindow(window);
			
		window.center();
		return new ConfigureEntityQuery(observers, window);
	}
	
	public static class ConfigureEntityQuery implements IDisposable
	{
		private final IObserverRegistry m_observers;
		
		private final Window m_window;
		
		private ConfigureEntityQuery(IObserverRegistry observers, Window window)
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

		public void setTopMost(boolean b) {
			m_window.setTopMost(b);
		}
	}
	
	private class ConfigureEntityQueryBehaviourInjector extends WindowBehaviourInjector
	{
		private final Logger m_logger = LoggerFactory.getLogger(ConfigureEntityQueryFactory.class);
		
		private final Observers m_observers;
		
		private final EditorEntity m_subject;
		
		public ConfigureEntityQueryBehaviourInjector(Observers observers, EditorEntity subject)
		{
			m_subject = subject;
			m_observers = observers;
		}
		
		@Nullable
		Vector2D parseVector2D(String vector)
		{
			String[] components = vector.split(",[ ]*");
			
			if(components.length != 2)
				return null;
			
			try
			{
				Vector2D buffer = new Vector2D();
				
				buffer.x = Integer.parseInt(components[0]);
				buffer.y = Integer.parseInt(components[1]);
				
				return buffer;
			}catch(NumberFormatException e)
			{
				return null;
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
			final TextArea txtName = getControl(TextArea.class, "txtName");
			final TextArea txtTypeName = getControl(TextArea.class, "txtTypeName");
			final TextArea txtConfiguration = getControl(TextArea.class, "txtConfiguration");
			final TextArea txtDirection = getControl(TextArea.class, "txtDirection");
			final TextArea txtLocation = getControl(TextArea.class, "txtLocation");
			final TextArea txtAuxConfig = getControl(TextArea.class, "txtAuxConfig");
			
			txtName.setText(m_subject.getName());
			txtTypeName.setText(m_subject.getClassName());
			txtConfiguration.setText(m_subject.getConfig() == null ? "" : m_subject.getConfig().toString());
			txtDirection.setText(String.format("%d, %d", m_subject.getDirection().getDirectionVector().x, m_subject.getDirection().getDirectionVector().y));
			txtLocation.setText(String.format("%.2f, %.2f, %.2f", m_subject.getLocation().x, m_subject.getLocation().y, m_subject.getLocation().z));
			
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream())
			{
				m_subject.getAuxiliaryConfig().serialize(bos, true);
				txtAuxConfig.setText(bos.toString("UTF8"));
			} catch (IOException | ValueSerializationException e)
			{
				m_logger.error("Unable to deserialize subject auxiliary configuration into JSON format. Assuming empty auxiliary configuration.", e);
				txtAuxConfig.setText("{ }" );
			}
			
			getControl(Button.class, "btnApply").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					try
					{
						String name = txtName.getText();
						String typeName = txtTypeName.getText();
						URI configuration = new URI(txtConfiguration.getText());
						String direction = txtDirection.getText();
						String location = txtLocation.getText();
						
						JsonVariable auxConfig = JsonVariable.create(new ByteArrayInputStream(txtAuxConfig.getText().getBytes("UTF8")));
						
						Vector3F locVector = parseVector3F(location);
						Vector2D dirVector = parseVector2D(direction);
						
						if(name.length() <= 0)
						{
							validationFailed("You must specify a name ofr the entity.");
						} else if(typeName.length() <= 0)
						{
							validationFailed("You must specify a type name for the entity.");
						}else if(dirVector == null)
						{
							validationFailed("Unable to parse direction vector. Assure it is properly formatted.");
						}else if(locVector == null)
						{
							validationFailed("Unable to parse location vector. Assure it is properly formatted.");
						}else
						{
							m_subject.setName(name);
							m_subject.setClassName(typeName);
							m_subject.setConfig(configuration);
							m_subject.setDirection(Direction.fromVector(new Vector2F(dirVector)));
							m_subject.setLocation(locVector);
							m_subject.setAuxiliaryConfig(auxConfig);
							m_observers.raise(IConfigureEntityQueryObserver.class).apply();			
						}
					}catch(IOException | ValueSerializationException | URISyntaxException e)
					{
						validationFailed("Error occured parsing auxiliary configuration. Assure it is a properly formatted JSON document.");
					}
				}
			});
			
			getControl(Button.class, "btnCancel").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(IConfigureEntityQueryObserver.class).cancel();
				}
			});
			
			getControl(Button.class, "btnBrowse").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					
					try
					{
						final FileInputQuery query = new FileInputQueryFactory(m_windowManager, m_windowFactory, m_base).create(FileInputQueryMode.OpenFile, "Entity Configuration", m_base);
					
						query.getObservers().add(new IFileInputQueryObserver() {
							
							@Override
							public void okay(URI input) {
								txtConfiguration.setText(input.toString());
								query.dispose();
							}
							
							@Override
							public void cancel() {
								query.dispose();
							}
						});
					} catch (WindowConstructionException e)
					{
						m_logger.error("Error constructing browse dialogue for configuration source selection.", e);
					}
				}
			});
			
			getControl(Button.class, "btnDelete").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(IConfigureEntityQueryObserver.class).delete();
				}
			});
		}
	}
	
	public interface IConfigureEntityQueryObserver
	{
		void apply();
		void delete();
		void cancel();
	}}
