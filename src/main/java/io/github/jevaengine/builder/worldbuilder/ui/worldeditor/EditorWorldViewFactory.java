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
package io.github.jevaengine.builder.worldbuilder.ui.worldeditor;

import io.github.jevaengine.builder.worldbuilder.ui.worldeditor.behavior.CommandBehaviourInjector;
import io.github.jevaengine.builder.worldbuilder.world.brush.Brush;
import io.github.jevaengine.builder.worldbuilder.ui.SelectBrushQuery;
import io.github.jevaengine.builder.worldbuilder.ui.worldeditor.behavior.BrushBehaviorInjector;
import io.github.jevaengine.builder.worldbuilder.ui.worldeditor.behavior.CameraBehaviorInjector;
import io.github.jevaengine.builder.worldbuilder.ui.worldeditor.behavior.EntityBehaviorInjector;
import io.github.jevaengine.builder.worldbuilder.ui.worldeditor.behavior.ZoneBehaviorInjector;
import io.github.jevaengine.builder.worldbuilder.world.EditorWorld;
import io.github.jevaengine.builder.worldbuilder.world.EditorWeatherFactory;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.WindowManager;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.IWeatherFactory;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.camera.ControlledCamera;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import java.io.File;
import java.net.URI;

public class EditorWorldViewFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/editorWorldView.jwl");
	
	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	private final ISceneBufferFactory m_sceneBufferFactory;
	private final ISceneModelFactory m_modelFactory;

	private final EditorWeatherFactory m_weatherFactory;
	
	private final IFontFactory m_fontFactory;
	
	private final URI m_baseDirectory;
	
	public EditorWorldViewFactory(WindowManager windowManager, IWindowFactory windowFactory,
									ISceneBufferFactory sceneBufferFactory, ISceneModelFactory modelFactory, IFontFactory fontFactory,
									IWeatherFactory weatherFactory,
									URI baseDirectory)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
		
		m_weatherFactory = new EditorWeatherFactory(weatherFactory);
		m_fontFactory = fontFactory;
		m_sceneBufferFactory = sceneBufferFactory;
		m_modelFactory = modelFactory;
		m_baseDirectory = baseDirectory;
	}
	
	public EditorWorldView create(EditorWorld world) throws WindowConstructionException
	{
		Observers observers = new Observers();
		
		Brush brush = new Brush();
		ControlledCamera camera = new ControlledCamera(m_sceneBufferFactory);
		
		SelectBrushQuery selectBrushQuery = new SelectBrushQuery(new File(m_baseDirectory), brush, m_modelFactory);
		
		CommandBehaviourInjector commandInjector = new CommandBehaviourInjector(m_windowManager, m_windowFactory, m_weatherFactory, 
					m_baseDirectory, m_fontFactory, observers, world, m_sceneBufferFactory, 
					m_modelFactory, brush, selectBrushQuery);
		
		BrushBehaviorInjector brushInjector = new BrushBehaviorInjector(m_windowManager, m_windowFactory, world, brush);

		CameraBehaviorInjector cameraInjector = new CameraBehaviorInjector(m_windowManager, m_windowFactory, world, camera);
		
		EntityBehaviorInjector entityInjector = new EntityBehaviorInjector(m_baseDirectory, m_windowManager, m_windowFactory, m_fontFactory, m_modelFactory, camera, world, brush);
		
		ZoneBehaviorInjector zoneInjector = new ZoneBehaviorInjector(m_windowManager, m_windowFactory, world, camera, brush, m_baseDirectory, m_fontFactory);
		
		Window window = m_windowFactory.create(WINDOW_LAYOUT);
		
		try
		{
			commandInjector.inject(window);
			brushInjector.inject(window);
			cameraInjector.inject(window);
			entityInjector.inject(window);
			zoneInjector.inject(window);
			
		}catch(NoSuchControlException e) {
			throw new WindowConstructionException(WINDOW_LAYOUT, e);
		}
		
		m_windowManager.addWindow(window);

		return new EditorWorldView(window, observers, world, selectBrushQuery);
	}
	
	
	public interface IEditorWorldViewObserver
	{
		void close();
	}	
}
