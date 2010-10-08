package au.gov.ga.worldwind.animator.util;

/**
 * Extension of the common {@link au.gov.ga.worldwind.common.util.Icons} accessor class
 * with additional icons specific to the Animator project.
 * 
 * @author James Navin (james.navin@ga.gov.au)
 *
 */
public class Icons extends au.gov.ga.worldwind.common.util.Icons
{

	private static final String ANIMATOR_ICONS_DIRECTORY = "/au/gov/ga/worldwind/animator/data/icons/";
	
	public static final Icons animatableObject = new Icons(ANIMATOR_ICONS_DIRECTORY, "object.gif");
	public static final Icons parameter = new Icons(ANIMATOR_ICONS_DIRECTORY, "parameter.gif");
	public static final Icons partialCheck = new Icons(ANIMATOR_ICONS_DIRECTORY, "partialCheck.gif");
	public static final Icons animatableLayer = new Icons(ANIMATOR_ICONS_DIRECTORY, "layer_object.gif");
	public static final Icons armed = new Icons(ANIMATOR_ICONS_DIRECTORY, "armed.gif");
	public static final Icons disarmed = new Icons(ANIMATOR_ICONS_DIRECTORY, "disarmed.gif");
	public static final Icons partialArmed = new Icons(ANIMATOR_ICONS_DIRECTORY, "partial_armed.gif");
	public static final Icons copy = new Icons(ANIMATOR_ICONS_DIRECTORY, "copy.gif");
	public static final Icons paste = new Icons(ANIMATOR_ICONS_DIRECTORY, "paste.gif");
	public static final Icons cut = new Icons(ANIMATOR_ICONS_DIRECTORY, "cut.gif");
	
	
	public Icons(String directory, String filename)
	{
		super(directory, filename);
	}

	public Icons(String filename)
	{
		super(filename);
	}
	
}
