package io.github.jevaengine.builder.worldbuilder.world;

import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.scene.model.NullSceneModel;

public final class ClearTileBrushBehaviour implements IBrushBehaviour
{

	@Override
	public boolean isSizable()
	{
		return true;
	}

	@Override
	public ISceneModel getModel()
	{
		return new NullSceneModel();
	}
	
	@Override
	public void apply(EditorWorld world, Vector3F location)
	{
		world.clearTile(location);
	}

}
