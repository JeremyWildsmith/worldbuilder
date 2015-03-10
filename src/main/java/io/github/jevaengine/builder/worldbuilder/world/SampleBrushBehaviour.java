package io.github.jevaengine.builder.worldbuilder.world;

import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.scene.model.NullSceneModel;

public final class SampleBrushBehaviour implements IBrushBehaviour
{
	private final IBrushSampleHandler m_sampleHandler;
	
	public SampleBrushBehaviour(IBrushSampleHandler sampleHandler)
	{
		m_sampleHandler = sampleHandler;
	}
	
	@Override
	public boolean isSizable()
	{
		return false;
	}

	@Override
	public ISceneModel getModel()
	{
		return new NullSceneModel();
	}
	
	@Override
	public void apply(EditorWorld world, Vector3F location)
	{
		EditorSceneArtifact tile = world.getTile(location);
		
		if(tile == null)
			return;
		
		m_sampleHandler.sample(tile);
	}
	
	public interface IBrushSampleHandler
	{
		void sample(EditorSceneArtifact sample);
	}
}
