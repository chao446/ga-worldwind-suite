package au.gov.ga.worldwind.theme;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import au.gov.ga.worldwind.panels.dataset.IDataset;
import au.gov.ga.worldwind.panels.dataset.ILayerDefinition;
import au.gov.ga.worldwind.panels.dataset.LayerDefinition;
import au.gov.ga.worldwind.panels.dataset.LazyDataset;
import au.gov.ga.worldwind.util.Icons;
import au.gov.ga.worldwind.util.XMLUtil;

public class ThemeFactory
{
	public static Theme createFromXML(Object source, URL context)
	{
		Element element = XMLUtil.getElementFromSource(source);
		if (element == null)
			return null;

		BasicTheme theme = new BasicTheme(XMLUtil.getText(element, "ThemeName"));

		theme.setMenuBar(XMLUtil.getBoolean(element, "MenuBar", false));
		theme.setStatusBar(XMLUtil.getBoolean(element, "StatusBar", false));

		theme.setHUDs(parseHUDs(element, "HUD"));
		theme.setPanels(parsePanels(element, "Panel"));
		theme.setDatasets(parseDatasets(element, "Dataset", context));
		theme.setLayers(parseLayers(element, "Layer", context));

		theme.setInitialLatitude(XMLUtil.getDouble(element, "InitialLatitude", null));
		theme.setInitialLongitude(XMLUtil.getDouble(element, "InitialLongitude", null));
		theme.setInitialAltitude(XMLUtil.getDouble(element, "InitialAltitude", null));
		theme.setInitialHeading(XMLUtil.getDouble(element, "InitialHeading", null));
		theme.setInitialPitch(XMLUtil.getDouble(element, "InitialPitch", null));

		return theme;
	}

	private static List<ThemeHUD> parseHUDs(Element context, String path)
	{
		List<ThemeHUD> huds = new ArrayList<ThemeHUD>();
		Element[] elements = XMLUtil.getElements(context, path, null);
		if (elements != null)
		{
			for (Element element : elements)
			{
				String className = XMLUtil.getText(element, "@className");
				String position = XMLUtil.getText(element, "@position");
				boolean enabled = XMLUtil.getBoolean(element, "@enabled", true);
				String name = XMLUtil.getText(element, "@name");

				try
				{
					Class<?> c = Class.forName(className);
					Class<? extends ThemeHUD> tc = c.asSubclass(ThemeHUD.class);
					ThemeHUD hud = tc.newInstance();
					if (position != null)
						hud.setPosition(position);
					hud.setOn(enabled);
					hud.setDisplayName(name);
					huds.add(hud);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return huds;
	}

	private static List<ThemePanel> parsePanels(Element context, String path)
	{
		List<ThemePanel> panels = new ArrayList<ThemePanel>();
		Element[] elements = XMLUtil.getElements(context, path, null);
		if (elements != null)
		{
			for (Element element : elements)
			{
				String className = XMLUtil.getText(element, "@className");
				boolean enabled = XMLUtil.getBoolean(element, "@enabled", true);
				String name = XMLUtil.getText(element, "@name");
				Double weightD = XMLUtil.getDouble(element, "@weight", null);
				float weight = weightD != null ? weightD.floatValue() : 1f;
				boolean expanded = XMLUtil.getBoolean(element, "@expanded", true);

				try
				{
					Class<?> c = Class.forName(className);
					Class<? extends ThemePanel> tc = c.asSubclass(ThemePanel.class);
					ThemePanel panel = tc.newInstance();
					panel.setDisplayName(name);
					panel.setOn(enabled);
					panel.setWeight(weight);
					panel.setExpanded(expanded);
					panels.add(panel);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return panels;
	}

	private static List<IDataset> parseDatasets(Element context, String path, URL urlContext)
	{
		List<IDataset> datasets = new ArrayList<IDataset>();
		Element[] elements = XMLUtil.getElements(context, path, null);
		if (elements != null)
		{
			for (Element element : elements)
			{
				try
				{
					String name = XMLUtil.getText(element, "@name");
					URL url = XMLUtil.getURL(element, "@url", urlContext);
					URL description = XMLUtil.getURL(element, "@description", urlContext);
					String icon = XMLUtil.getText(element, "@icon");
					URL iconURL = null;
					if (icon == null)
						iconURL = Icons.earth.getURL();
					else
						iconURL = XMLUtil.getURL(icon, urlContext);

					IDataset dataset = new LazyDataset(name, url, description, iconURL, true);
					datasets.add(dataset);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return datasets;
	}

	private static List<ILayerDefinition> parseLayers(Element context, String path, URL urlContext)
	{
		List<ILayerDefinition> layers = new ArrayList<ILayerDefinition>();
		Element[] elements = XMLUtil.getElements(context, path, null);
		if (elements != null)
		{
			for (Element element : elements)
			{
				try
				{
					String name = XMLUtil.getText(element, "@name");
					URL url = XMLUtil.getURL(element, "@url", urlContext);
					URL description = XMLUtil.getURL(element, "@description", urlContext);
					URL icon = XMLUtil.getURL(element, "@icon", urlContext);
					boolean enabled = XMLUtil.getBoolean(element, "@enabled", true);

					ILayerDefinition layer =
							new LayerDefinition(name, url, description, icon, true, enabled);
					layers.add(layer);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return layers;
	}
}