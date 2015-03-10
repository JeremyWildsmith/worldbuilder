package io.github.jevaengine.builder.worldbuilder.world;

import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

public interface IBrushBehaviour
{
	boolean isSizable();
	IImmutableSceneModel getModel();
	void apply(EditorWorld world, Vector3F location);
}
