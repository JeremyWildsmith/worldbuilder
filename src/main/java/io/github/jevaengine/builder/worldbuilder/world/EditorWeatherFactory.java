/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.jevaengine.builder.worldbuilder.world;

import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.math.Matrix3X3;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.world.IWeatherFactory;
import io.github.jevaengine.world.scene.ISceneBuffer;
import java.awt.Graphics2D;
import java.net.URI;
import java.util.Collection;

/**
 *
 * @author Jeremy
 */
public final class EditorWeatherFactory implements IWeatherFactory {
	
	private final IWeatherFactory m_weatherFactory;

	public EditorWeatherFactory(IWeatherFactory weatherFactory) {
		m_weatherFactory = weatherFactory;
	}

	@Override
	public EditorWeather create(URI name) throws IWeatherFactory.WeatherConstructionException {
		return new EditorWeather(name, m_weatherFactory.create(name));
	}

	public static final class EditorWeather implements IWeather {

		private final URI m_name;
		private final IWeatherFactory.IWeather m_weather;

		public EditorWeather(URI name, IWeatherFactory.IWeather weather) {
			m_name = name;
			m_weather = weather;
		}

		public URI getName() {
			return m_name;
		}

		@Override
		public void update(int deltaTime) {
			m_weather.update(deltaTime);
		}

		@Override
		public void dispose() {
			m_weather.dispose();
		}

		@Override
		public IRenderable getUnderlay(Rect2D bounds, Matrix3X3 projection) {
			return m_weather.getUnderlay(bounds, projection);
		}

		@Override
		public IRenderable getOverlay(Rect2D bounds, Matrix3X3 projection) {
			return m_weather.getOverlay(bounds, projection);
		}

		@Override
		public ISceneBuffer.ISceneComponentEffect[] getComponentEffect(Graphics2D g, int offsetX, int offsetY, float scale, Vector2D renderLocation, Matrix3X3 projection, ISceneBuffer.ISceneBufferEntry subject, Collection<ISceneBuffer.ISceneBufferEntry> beneath) {
			return m_weather.getComponentEffect(g, offsetX, offsetY, scale, renderLocation, projection, subject, beneath);
		}
	}
	
}
