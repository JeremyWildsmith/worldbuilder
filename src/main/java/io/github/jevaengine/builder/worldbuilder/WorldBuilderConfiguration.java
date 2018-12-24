package io.github.jevaengine.builder.worldbuilder;

import io.github.jevaengine.config.*;
import io.github.jevaengine.math.Matrix3X3;

import java.util.HashMap;
import java.util.Map;

public final class WorldBuilderConfiguration implements ISerializable
{
    public Matrix3X3 projection;

    public Map<String, Float> layers = new HashMap<>();

    @Override
    public void serialize(IVariable target) throws ValueSerializationException
    {
        target.addChild("projection").setValue(projection);
    }

    @Override
    public void deserialize(IImmutableVariable source) throws ValueSerializationException
    {
        try
        {
            projection = source.getChild("projection").getValue(Matrix3X3.class);

            if(source.childExists("layers")) {
                for(String c : source.getChild("layers").getChildren()) {
                    layers.put(c, source.getChild("layers").getChild(c).getValue(Double.class).floatValue());
                }
            }

        } catch (NoSuchChildVariableException e)
        {
            throw new ValueSerializationException(e);
        }
    }
}
