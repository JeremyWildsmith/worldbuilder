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
import io.github.jevaengine.builder.ui.MessageBoxFactory;
import io.github.jevaengine.builder.ui.FileInputQueryFactory.FileInputQuery;
import io.github.jevaengine.builder.ui.FileInputQueryFactory.FileInputQueryMode;
import io.github.jevaengine.builder.ui.FileInputQueryFactory.IFileInputQueryObserver;
import io.github.jevaengine.builder.ui.MessageBoxFactory.IMessageBoxObserver;
import io.github.jevaengine.builder.ui.MessageBoxFactory.MessageBox;
import io.github.jevaengine.builder.worldbuilder.world.PlaceSceneArtifactBrushBehaviour;
import io.github.jevaengine.builder.worldbuilder.world.Brush;
import io.github.jevaengine.builder.worldbuilder.world.EditorSceneArtifact;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.ui.Button;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
import io.github.jevaengine.ui.Checkbox;
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
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory.SceneModelConstructionException;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SelectBrushQueryFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/selectBrush.jwl");

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	private final ISceneModelFactory m_modelFactory;
	
	private final URI m_base;
	
	public SelectBrushQueryFactory(WindowManager windowManager, IWindowFactory windowFactory, ISceneModelFactory modelFactory, URI base)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
		m_modelFactory = modelFactory;
		m_base = base;
	}
	
	public SelectBrushQuery create(Brush workingBrush, @Nullable EditorSceneArtifact defaultValue) throws WindowConstructionException
	{
		Observers observers = new Observers();
		Window window = m_windowFactory.create(WINDOW_LAYOUT, new SelectBrushQueryBehaviourInjector(observers, workingBrush, m_modelFactory, defaultValue));
		m_windowManager.addWindow(window);
			
		window.center();
		return new SelectBrushQuery(observers, window);
	}
	
	public SelectBrushQuery create(Brush workingBrush) throws WindowConstructionException
	{
		return create(workingBrush, null);
	}
	
	public static final class SelectBrushQuery implements IDisposable
	{
		private final IObserverRegistry m_observers;
		private final Window m_window;
		
		private SelectBrushQuery(IObserverRegistry observers, Window window)
		{
			m_observers = observers;
			m_window = window;
		}
		
		@Override
		public void dispose()
		{
			m_window.dispose();
		}
		
		public void setLocation(Vector2D location)
		{
			m_window.setLocation(location);
		}
		
		public void setVisible(boolean isVisible)
		{
			m_window.setVisible(isVisible);
		}
		
		public IObserverRegistry getObservers()
		{
			return m_observers;
		}
	}
	
	public interface ISelectBrushQueryObserver
	{
		void close();
	}
	
	private class SelectBrushQueryBehaviourInjector extends WindowBehaviourInjector
	{
		private final Logger m_logger = LoggerFactory.getLogger(SelectBrushQueryBehaviourInjector.class);
		
		private final EditorSceneArtifact m_defaultValue;
		private final Observers m_observers;
		private final ISceneModelFactory m_modelFactory;
		
		private Brush m_workingBrush;
		
		public SelectBrushQueryBehaviourInjector(Observers observers, Brush workingBrush, ISceneModelFactory modelFactory, EditorSceneArtifact defaultValue)
		{
			m_observers = observers;
			m_workingBrush = workingBrush;
			m_modelFactory = modelFactory;
			m_defaultValue = defaultValue;
		}
		
		private void displayMessage(String cause)
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
			} catch (WindowConstructionException e)
			{
				m_logger.error("Unable to notify user.", e);
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
		
		@Override
		protected void doInject() throws NoSuchControlException
		{
			final TextArea txtModel = getControl(TextArea.class, "txtModel");
			final TextArea txtDirection = getControl(TextArea.class, "txtDirection");
			
			final Checkbox chkIsTraversable = getControl(Checkbox.class, "chkIsTraversable");
			
			if(m_defaultValue != null)
			{
				txtModel.setText(m_defaultValue.getModelName().toString());
				
				Vector2D direction = m_defaultValue.getDirection().getDirectionVector();
				txtDirection.setText(direction.x + ", " + direction.y);
				
				chkIsTraversable.setValue(m_defaultValue.isTraversable());
			}
			
			getControl(Button.class, "btnApply").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress()
				{
					Vector2D dirVector = parseVector2D(txtDirection.getText());
					
					if(dirVector == null)
						displayMessage("Direction must be a properly formed non-zero vector");
					else
					{
						try
						{
							Direction direction = Direction.fromVector(new Vector2F(dirVector));
							URI modelUri = new URI(txtModel.getText());
							
							ISceneModel model = m_modelFactory.create(modelUri);
							model.setDirection(direction);
							
							m_workingBrush.setBehaviour(new PlaceSceneArtifactBrushBehaviour(model, modelUri, direction, chkIsTraversable.getValue()));
						} catch (SceneModelConstructionException e)
						{
							m_logger.info("Unable to construct model for brush behaviour", e);
							displayMessage("Unable to construct model for brush behaviour. Assure you have specified a valid model resource name.");
						} catch (URISyntaxException e)
						{
							m_logger.info("Unable to construct model for brush behaviour", e);
							displayMessage("The specified model resource name is not a properly formed URI string.");	
						}
						
					}
				}
			});
			
			getControl(Button.class, "btnClose").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(ISelectBrushQueryObserver.class).close();
				}
			});
			
			getControl(Button.class, "btnBrowseModel").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					try
					{
						final FileInputQuery query = new FileInputQueryFactory(m_windowManager, m_windowFactory, m_base).create(FileInputQueryMode.OpenFile, "Model Resource", m_defaultValue == null ? URI.create("") : m_defaultValue.getModelName());
						query.getObservers().add(new IFileInputQueryObserver() {
							
							@Override
							public void okay(URI input) {
								txtModel.setText(input.toString());
								query.dispose();
							}
							
							@Override
							public void cancel() {
								query.dispose();
							}
						});
					} catch (WindowConstructionException e)
					{
						m_logger.error("Unable to construct file section dialogue to select sprite resource.", e);
					}
				}
			});
		}
	}
}
