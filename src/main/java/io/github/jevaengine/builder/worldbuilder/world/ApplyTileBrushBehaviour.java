package io.github.jevaengine.builder.worldbuilder.world;

import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;

import java.net.URI;

public final class ApplyTileBrushBehaviour implements IBrushBehaviour
{
	private final ISceneModel m_model;
	private final URI m_modelName;
	private final Direction m_direction;
	private final boolean m_isTraversable;
	
	public ApplyTileBrushBehaviour(ISceneModel model, URI modelName, Direction direction, boolean isTraversable)
	{
		m_model = model;
		m_modelName = modelName;
		m_direction = direction;
		m_isTraversable = isTraversable;
	}
	
	@Override
	public boolean isSizable()
	{
		return true;
	}
	
	@Override
	public IImmutableSceneModel getModel()
	{
		return m_model;
	}
	
	@Override
	public void apply(EditorWorld world, Vector3F location)
	{
		ISceneModel model = m_model.clone();
		model.setDirection(m_direction);
		world.setTile(new EditorSceneArtifact(model, m_modelName, m_direction, m_isTraversable), location);
	}
}
