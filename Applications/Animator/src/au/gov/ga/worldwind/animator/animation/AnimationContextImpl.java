/**
 * 
 */
package au.gov.ga.worldwind.animator.animation;

import au.gov.ga.worldwind.animator.animation.parameter.Parameter;
import au.gov.ga.worldwind.animator.util.Validate;

/**
 * The default implementation of the {@link AnimationContext} interface
 * 
 * @author James Navin (james.navin@ga.gov.au)
 */
public class AnimationContextImpl implements AnimationContext
{

	/** The animation this context is associated with */
	private Animation animation;
	
	/**
	 * Constructor. Initialses the mandatory fields.
	 * 
	 * @param animation The animation this context is associated with
	 */
	public AnimationContextImpl(Animation animation)
	{
		Validate.notNull(animation, "An animation instance is required");
		this.animation = animation;
	}
	
	@Override
	public KeyFrame getKeyFrameWithParameterBeforeFrame(Parameter p, int frame)
	{
		return animation.getKeyFrameWithParameterBeforeFrame(p, frame);
	}

	@Override
	public KeyFrame getKeyFrameWithParameterAfterFrame(Parameter p, int frame)
	{
		return animation.getKeyFrameWithParameterAfterFrame(p, frame);
	}

}