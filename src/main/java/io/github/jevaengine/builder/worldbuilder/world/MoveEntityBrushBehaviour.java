/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.jevaengine.builder.worldbuilder.world;

import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

/**
 *
 * @author Jeremy
 */
public class MoveEntityBrushBehaviour implements IBrushBehaviour
{
	private final IEntity m_entity;
	private final IEntityMovementBrushBehaviorHandler m_movementHandler;
	
	public MoveEntityBrushBehaviour(IEntity entity, IEntityMovementBrushBehaviorHandler movementHandler)
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
		return m_entity.getModel();
	}

	@Override
	public void apply(EditorWorld world, Vector3F location)
	{
		m_entity.getBody().setLocation(location);
		m_movementHandler.moved();
	}
	
	public interface IEntityMovementBrushBehaviorHandler
	{
		void moved();
	}
}
