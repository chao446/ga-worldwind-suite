package au.gov.ga.worldwind.panels.layers;

import java.net.URL;

import au.gov.ga.worldwind.panels.dataset.ILayerDefinition;

public class LayerNode extends AbstractNode implements ILayerNode
{
	private URL layerURL;
	private URL descriptionURL;
	private boolean enabled;
	private double opacity;
	private Exception error = null;
	private boolean layerLoading = false;

	public LayerNode(String name, URL iconURL, boolean expanded, URL layerURL, URL descriptionURL,
			boolean enabled, double opacity)
	{
		super(name, iconURL, expanded);
		setLayerURL(layerURL);
		setDescriptionURL(descriptionURL);
		setEnabled(enabled);
		setOpacity(opacity);
	}

	public URL getLayerURL()
	{
		return layerURL;
	}

	public void setLayerURL(URL layerURL)
	{
		this.layerURL = layerURL;
	}

	public URL getDescriptionURL()
	{
		return descriptionURL;
	}

	public void setDescriptionURL(URL descriptionURL)
	{
		this.descriptionURL = descriptionURL;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
		if (enabled && getOpacity() <= 0.2)
			setOpacity(1d);
	}

	public static LayerNode createFromLayerDefinition(ILayerDefinition definition)
	{
		return new LayerNode(definition.getName(), definition.getIconURL(), true, definition
				.getLayerURL(), definition.getDescriptionURL(), definition.isEnabled(), 1.0);
	}

	@Override
	public boolean isLoading()
	{
		return super.isLoading() || isLayerLoading();
	}

	@Override
	public boolean isLayerLoading()
	{
		return layerLoading;
	}

	@Override
	public void setLayerLoading(boolean layerLoading)
	{
		this.layerLoading = layerLoading;
	}

	@Override
	public boolean hasError()
	{
		return error != null;
	}

	@Override
	public Exception getError()
	{
		return error;
	}

	@Override
	public void setError(Exception error)
	{
		this.error = error;
	}

	@Override
	public double getOpacity()
	{
		return opacity;
	}

	@Override
	public void setOpacity(double opacity)
	{
		this.opacity = opacity;
	}
}