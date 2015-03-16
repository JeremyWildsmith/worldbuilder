/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.jevaengine.builder.worldbuilder.world;

import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

/**
 *
 * @author Jeremy
 */
public class MoveEntityBrush implements IBrushBehaviour
{
	private final EditorEntity m_entity;
	private final IEntityMovementHandler m_movementHandler;
	
	public MoveEntityBrush(EditorEntity entity, IEntityMovementHandler movementHandler)
	{
		m_entity = entity;
		m_movementHandler = movementHandler;
	}
	
	@Override
	public boolean isSizable()
	{
		return false;
	}

	@Override
	public IImmutableSceneModel getModel()
	{
		return m_entity.getEntity().getModel();
	}

	@Override
	public void apply(EditorWorld world, Vector3F location)
	{
		m_entity.setLocation(location);
		m_movementHandler.moved();
	}
	
	public interface IEntityMovementHandler
	{
		void moved();
	}
}
