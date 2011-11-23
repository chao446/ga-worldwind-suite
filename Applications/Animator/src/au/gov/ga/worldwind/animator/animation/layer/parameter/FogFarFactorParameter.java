package au.gov.ga.worldwind.animator.animation.layer.parameter;

import static au.gov.ga.worldwind.animator.util.message.AnimationMessageConstants.getFogFarParameterNameKey;
import static au.gov.ga.worldwind.common.util.message.MessageSourceAccessor.getMessage;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.layers.FogLayer;
import gov.nasa.worldwind.layers.Layer;
import au.gov.ga.worldwind.animator.animation.Animation;
import au.gov.ga.worldwind.animator.animation.AnimationContext;
import au.gov.ga.worldwind.animator.animation.annotation.EditableParameter;
import au.gov.ga.worldwind.animator.animation.io.AnimationIOConstants;
import au.gov.ga.worldwind.animator.animation.parameter.ParameterBase;
import au.gov.ga.worldwind.animator.animation.parameter.ParameterValue;
import au.gov.ga.worldwind.animator.animation.parameter.ParameterValueFactory;
import au.gov.ga.worldwind.common.util.Validate;

/**
 * A layer parameter controlling the far factor of a {@link FogLayer}
 */
@EditableParameter
public class FogFarFactorParameter extends LayerParameterBase
{

	private static final long serialVersionUID = 1L;

	public FogFarFactorParameter(Animation animation, FogLayer layer)
	{
		super(getMessage(getFogFarParameterNameKey()), animation, layer);
		setDefaultValue(layer.getFarFactor());
	}

	public FogFarFactorParameter()
	{
		super();
	}

	@Override
	public Type getType()
	{
		return Type.FAR;
	}

	@Override
	public ParameterValue getCurrentValue(AnimationContext context)
	{
		return ParameterValueFactory.createParameterValue(this, ((FogLayer)getLayer()).getFarFactor(), context.getCurrentFrame());
	}

	@Override
	protected void doApplyValue(double value)
	{
		((FogLayer)getLayer()).setFarFactor((float)value);
	}

	@Override
	protected ParameterBase createParameter(AVList context, AnimationIOConstants constants)
	{
		Layer parameterLayer = (Layer)context.getValue(constants.getCurrentLayerKey());
		Validate.notNull(parameterLayer, "No layer found in the context. Expected one under the key '" + constants.getCurrentLayerKey() + "'.");
		
		FogFarFactorParameter result = new FogFarFactorParameter();
		result.setLayer(parameterLayer);
		return result;
	}

}
