/*******************************************************************************
 * Copyright 2012 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package au.gov.ga.worldwind.common.layers.volume;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Plane;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;

import javax.media.opengl.GL;

import org.gdal.osr.CoordinateTransformation;

import au.gov.ga.worldwind.common.layers.Wireframeable;
import au.gov.ga.worldwind.common.util.AVKeyMore;
import au.gov.ga.worldwind.common.util.ColorMap;
import au.gov.ga.worldwind.common.util.CoordinateTransformationUtil;
import au.gov.ga.worldwind.common.util.FastShape;
import au.gov.ga.worldwind.common.util.GeometryUtil;
import au.gov.ga.worldwind.common.util.Util;
import au.gov.ga.worldwind.common.util.Validate;

import com.sun.opengl.util.j2d.TextureRenderer;

/**
 * Basic implementation of the {@link VolumeLayer} interface.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class BasicVolumeLayer extends AbstractLayer implements VolumeLayer, Wireframeable, SelectListener
{
	protected URL context;
	protected String url;
	protected String dataCacheName;
	protected VolumeDataProvider dataProvider;
	protected Double minimumDistance;
	protected double maxVariance = 0;
	protected CoordinateTransformation coordinateTransformation;
	protected ColorMap colorMap;
	protected Color noDataColor;

	protected final Object dataLock = new Object();
	protected boolean dataAvailable = false;
	protected FastShape topSurface, bottomSurface;
	protected TopBottomFastShape minLonCurtain, maxLonCurtain, minLatCurtain, maxLatCurtain;
	protected FastShape boundingBoxShape;
	protected TextureRenderer topTexture, bottomTexture, minLonTexture, maxLonTexture, minLatTexture, maxLatTexture;
	protected int topOffset = 0, bottomOffset = 0, minLonOffset = 0, maxLonOffset = 0, minLatOffset = 0,
			maxLatOffset = 0;
	protected int lastTopOffset = -1, lastBottomOffset = -1, lastMinLonOffset = -1, lastMaxLonOffset = -1,
			lastMinLatOffset = -1, lastMaxLatOffset = -1;

	protected final double[] curtainTextureMatrix = new double[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };

	protected boolean minLonClipDirty = false, maxLonClipDirty = false, minLatClipDirty = false,
			maxLatClipDirty = false, topClipDirty = false, bottomClipDirty = false;
	protected final double[] topClippingPlanes = new double[4 * 4];
	protected final double[] bottomClippingPlanes = new double[4 * 4];
	protected final double[] curtainClippingPlanes = new double[4 * 4];
	protected double lastVerticalExaggeration = -1;

	protected boolean wireframe = false;
	protected final PickSupport pickSupport = new PickSupport();

	protected boolean dragging = false;
	protected double dragStartPosition;
	protected int dragStartSlice;
	protected Vec4 dragStartCenter;

	protected WorldWindow wwd;

	/**
	 * Create a new {@link BasicVolumeLayer}, using the provided layer params.
	 * 
	 * @param params
	 */
	public BasicVolumeLayer(AVList params)
	{
		context = (URL) params.getValue(AVKeyMore.CONTEXT_URL);
		url = params.getStringValue(AVKey.URL);
		dataCacheName = params.getStringValue(AVKey.DATA_CACHE_NAME);
		dataProvider = (VolumeDataProvider) params.getValue(AVKeyMore.DATA_LAYER_PROVIDER);

		minimumDistance = (Double) params.getValue(AVKeyMore.MINIMUM_DISTANCE);
		colorMap = (ColorMap) params.getValue(AVKeyMore.COLOR_MAP);
		noDataColor = (Color) params.getValue(AVKeyMore.NO_DATA_COLOR);

		Double d = (Double) params.getValue(AVKeyMore.MAX_VARIANCE);
		if (d != null)
			maxVariance = d;

		String s = (String) params.getValue(AVKey.COORDINATE_SYSTEM);
		if (s != null)
			coordinateTransformation = CoordinateTransformationUtil.getTransformationToWGS84(s);

		Integer i = (Integer) params.getValue(AVKeyMore.INITIAL_OFFSET_MIN_U);
		if (i != null)
			minLonOffset = i;
		i = (Integer) params.getValue(AVKeyMore.INITIAL_OFFSET_MAX_U);
		if (i != null)
			maxLonOffset = i;
		i = (Integer) params.getValue(AVKeyMore.INITIAL_OFFSET_MIN_V);
		if (i != null)
			minLatOffset = i;
		i = (Integer) params.getValue(AVKeyMore.INITIAL_OFFSET_MAX_V);
		if (i != null)
			maxLatOffset = i;
		i = (Integer) params.getValue(AVKeyMore.INITIAL_OFFSET_MIN_W);
		if (i != null)
			topOffset = i;
		i = (Integer) params.getValue(AVKeyMore.INITIAL_OFFSET_MAX_W);
		if (i != null)
			bottomOffset = i;

		Validate.notBlank(url, "Model data url not set");
		Validate.notBlank(dataCacheName, "Model data cache name not set");
		Validate.notNull(dataProvider, "Model data provider is null");
	}

	@Override
	public URL getUrl() throws MalformedURLException
	{
		return new URL(context, url);
	}

	@Override
	public String getDataCacheName()
	{
		return dataCacheName;
	}

	@Override
	public boolean isLoading()
	{
		return dataProvider.isLoading();
	}

	@Override
	public void addLoadingListener(LoadingListener listener)
	{
		dataProvider.addLoadingListener(listener);
	}

	@Override
	public void removeLoadingListener(LoadingListener listener)
	{
		dataProvider.removeLoadingListener(listener);
	}

	@Override
	public void setup(WorldWindow wwd)
	{
		this.wwd = wwd;
		wwd.addSelectListener(this);
	}

	@Override
	public Sector getSector()
	{
		return dataProvider.getSector();
	}

	@Override
	public void dataAvailable(VolumeDataProvider provider)
	{
		calculateSurfaces();
		dataAvailable = true;
	}

	/**
	 * Calculate the 4 curtain and 2 horizontal surfaces used to render this
	 * volume. Should be called once after the {@link VolumeDataProvider}
	 * notifies this layer that the data is available.
	 */
	protected void calculateSurfaces()
	{
		double topElevation = 0;
		double bottomElevation = -dataProvider.getDepth();

		minLonCurtain = dataProvider.createLongitudeCurtain(0);
		minLonCurtain.setLighted(true);
		minLonCurtain.setCalculateNormals(true);
		minLonCurtain.setTopElevationOffset(topElevation);
		minLonCurtain.setBottomElevationOffset(bottomElevation);
		minLonCurtain.setTextureMatrix(curtainTextureMatrix);

		maxLonCurtain = dataProvider.createLongitudeCurtain(dataProvider.getXSize() - 1);
		maxLonCurtain.setLighted(true);
		maxLonCurtain.setCalculateNormals(true);
		maxLonCurtain.setReverseNormals(true);
		maxLonCurtain.setTopElevationOffset(topElevation);
		maxLonCurtain.setBottomElevationOffset(bottomElevation);
		maxLonCurtain.setTextureMatrix(curtainTextureMatrix);

		minLatCurtain = dataProvider.createLatitudeCurtain(0);
		minLatCurtain.setLighted(true);
		minLatCurtain.setCalculateNormals(true);
		minLatCurtain.setReverseNormals(true);
		minLatCurtain.setTopElevationOffset(topElevation);
		minLatCurtain.setBottomElevationOffset(bottomElevation);
		minLatCurtain.setTextureMatrix(curtainTextureMatrix);

		maxLatCurtain = dataProvider.createLatitudeCurtain(dataProvider.getYSize() - 1);
		maxLatCurtain.setLighted(true);
		maxLatCurtain.setCalculateNormals(true);
		maxLatCurtain.setTopElevationOffset(topElevation);
		maxLatCurtain.setBottomElevationOffset(bottomElevation);
		maxLatCurtain.setTextureMatrix(curtainTextureMatrix);

		Rectangle rectangle = new Rectangle(0, 0, dataProvider.getXSize(), dataProvider.getYSize());
		topSurface = dataProvider.createHorizontalSurface((float) maxVariance, rectangle);
		topSurface.setLighted(true);
		topSurface.setCalculateNormals(true);
		topSurface.setElevation(topElevation);

		bottomSurface = dataProvider.createHorizontalSurface((float) maxVariance, rectangle);
		bottomSurface.setLighted(true);
		bottomSurface.setCalculateNormals(true);
		bottomSurface.setReverseNormals(true);
		bottomSurface.setElevation(bottomElevation);

		//create the textures
		topTexture = new TextureRenderer(dataProvider.getXSize(), dataProvider.getYSize(), true, true);
		bottomTexture = new TextureRenderer(dataProvider.getXSize(), dataProvider.getYSize(), true, true);
		minLonTexture = new TextureRenderer(dataProvider.getYSize(), dataProvider.getZSize(), true, true);
		maxLonTexture = new TextureRenderer(dataProvider.getYSize(), dataProvider.getZSize(), true, true);
		minLatTexture = new TextureRenderer(dataProvider.getXSize(), dataProvider.getZSize(), true, true);
		maxLatTexture = new TextureRenderer(dataProvider.getXSize(), dataProvider.getZSize(), true, true);

		//update each shape's wireframe property so they match the layer's
		setWireframe(isWireframe());
	}

	/**
	 * Recalculate any surfaces that require recalculation. This includes
	 * regenerating textures when the user has dragged a surface to a different
	 * slice.
	 */
	protected void recalculateSurfaces()
	{
		if (!dataAvailable)
			return;

		//ensure the min/max offsets don't overlap one-another
		minLonOffset = Util.clamp(minLonOffset, 0, dataProvider.getXSize() - 1);
		maxLonOffset = Util.clamp(maxLonOffset, 0, dataProvider.getXSize() - 1 - minLonOffset);
		minLatOffset = Util.clamp(minLatOffset, 0, dataProvider.getYSize() - 1);
		maxLatOffset = Util.clamp(maxLatOffset, 0, dataProvider.getYSize() - 1 - minLatOffset);
		topOffset = Util.clamp(topOffset, 0, dataProvider.getZSize() - 1);
		bottomOffset = Util.clamp(bottomOffset, 0, dataProvider.getZSize() - 1 - topOffset);

		int maxLonSlice = dataProvider.getXSize() - 1 - maxLonOffset;
		int maxLatSlice = dataProvider.getYSize() - 1 - maxLatOffset;
		int bottomSlice = dataProvider.getZSize() - 1 - bottomOffset;

		//only recalculate those that have changed
		boolean recalculateMinLon = lastMinLonOffset != minLonOffset;
		boolean recalculateMaxLon = lastMaxLonOffset != maxLonOffset;
		boolean recalculateMinLat = lastMinLatOffset != minLatOffset;
		boolean recalculateMaxLat = lastMaxLatOffset != maxLatOffset;
		boolean recalculateTop = lastTopOffset != topOffset;
		boolean recalculateBottom = lastBottomOffset != bottomOffset;

		Rectangle lonRectangle = new Rectangle(0, 0, dataProvider.getYSize(), dataProvider.getZSize());
		Rectangle latRectangle = new Rectangle(0, 0, dataProvider.getXSize(), dataProvider.getZSize());
		Rectangle elevationRectangle = new Rectangle(0, 0, dataProvider.getXSize(), dataProvider.getYSize());
		double topPercent = topOffset / (double) Math.max(dataProvider.getZSize() - 1, 1);
		double bottomPercent = bottomSlice / (double) Math.max(dataProvider.getZSize() - 1, 1);

		if (recalculateMinLon)
		{
			minLonClipDirty = true;

			TopBottomFastShape newMinLonCurtain = dataProvider.createLongitudeCurtain(minLonOffset);
			minLonCurtain.setPositions(newMinLonCurtain.getPositions());

			updateTexture(generateTexture(0, minLonOffset, lonRectangle), minLonTexture, minLonCurtain);
			lastMinLonOffset = minLonOffset;
		}
		if (recalculateMaxLon)
		{
			maxLonClipDirty = true;

			TopBottomFastShape newMaxLonCurtain =
					dataProvider.createLongitudeCurtain(dataProvider.getXSize() - 1 - maxLonOffset);
			maxLonCurtain.setPositions(newMaxLonCurtain.getPositions());

			updateTexture(generateTexture(0, maxLonSlice, lonRectangle), maxLonTexture, maxLonCurtain);
			lastMaxLonOffset = maxLonOffset;
		}
		if (recalculateMinLat)
		{
			minLatClipDirty = true;

			TopBottomFastShape newMinLatCurtain = dataProvider.createLatitudeCurtain(minLatOffset);
			minLatCurtain.setPositions(newMinLatCurtain.getPositions());

			updateTexture(generateTexture(1, minLatOffset, latRectangle), minLatTexture, minLatCurtain);
			lastMinLatOffset = minLatOffset;
		}
		if (recalculateMaxLat)
		{
			maxLatClipDirty = true;

			TopBottomFastShape newMaxLatCurtain =
					dataProvider.createLatitudeCurtain(dataProvider.getYSize() - 1 - maxLatOffset);
			maxLatCurtain.setPositions(newMaxLatCurtain.getPositions());

			updateTexture(generateTexture(1, maxLatSlice, latRectangle), maxLatTexture, maxLatCurtain);
			lastMaxLatOffset = maxLatOffset;
		}
		if (recalculateTop)
		{
			topClipDirty = true;
			double elevation = -dataProvider.getDepth() * topPercent;

			updateTexture(generateTexture(2, topOffset, elevationRectangle), topTexture, topSurface);
			lastTopOffset = topOffset;

			topSurface.setElevation(elevation);
			minLonCurtain.setTopElevationOffset(elevation);
			maxLonCurtain.setTopElevationOffset(elevation);
			minLatCurtain.setTopElevationOffset(elevation);
			maxLatCurtain.setTopElevationOffset(elevation);

			recalculateTextureMatrix(topPercent, bottomPercent);
		}
		if (recalculateBottom)
		{
			bottomClipDirty = true;
			double elevation = -dataProvider.getDepth() * bottomPercent;

			updateTexture(generateTexture(2, bottomSlice, elevationRectangle), bottomTexture, bottomSurface);
			lastBottomOffset = bottomOffset;

			bottomSurface.setElevation(elevation);
			minLonCurtain.setBottomElevationOffset(elevation);
			maxLonCurtain.setBottomElevationOffset(elevation);
			minLatCurtain.setBottomElevationOffset(elevation);
			maxLatCurtain.setBottomElevationOffset(elevation);

			recalculateTextureMatrix(topPercent, bottomPercent);
		}
	}

	/**
	 * Recalculate the curtain texture matrix. When the top and bottom surface
	 * offsets aren't 0, the OpenGL texture matrix is used to offset the curtain
	 * textures.
	 * 
	 * @param topPercent
	 *            Location of the top surface (normalized to 0..1)
	 * @param bottomPercent
	 *            Location of the bottom surface (normalized to 0..1)
	 */
	protected void recalculateTextureMatrix(double topPercent, double bottomPercent)
	{
		Matrix m =
				Matrix.fromTranslation(0, topPercent, 0).multiply(Matrix.fromScale(1, bottomPercent - topPercent, 1));
		m.toArray(curtainTextureMatrix, 0, false);
	}

	/**
	 * Recalculate the clipping planes used to clip the surfaces when the user
	 * drags them.
	 * 
	 * @param dc
	 */
	protected void recalculateClippingPlanes(DrawContext dc)
	{
		if (!dataAvailable)
			return;

		boolean verticalExaggerationChanged = lastVerticalExaggeration != dc.getVerticalExaggeration();
		lastVerticalExaggeration = dc.getVerticalExaggeration();

		boolean minLon = minLonClipDirty || verticalExaggerationChanged;
		boolean maxLon = maxLonClipDirty || verticalExaggerationChanged;
		boolean minLat = minLatClipDirty || verticalExaggerationChanged;
		boolean maxLat = maxLatClipDirty || verticalExaggerationChanged;

		boolean sw = minLon || minLat;
		boolean nw = minLon || maxLat;
		boolean se = maxLon || minLat;
		boolean ne = maxLon || maxLat;

		minLon |= topClipDirty || bottomClipDirty;
		maxLon |= topClipDirty || bottomClipDirty;
		minLat |= topClipDirty || bottomClipDirty;
		maxLat |= topClipDirty || bottomClipDirty;

		if (!(minLon || maxLon || minLat || maxLat))
			return;

		int maxLonSlice = dataProvider.getXSize() - 1 - maxLonOffset;
		int maxLatSlice = dataProvider.getYSize() - 1 - maxLatOffset;
		int bottomSlice = dataProvider.getZSize() - 1 - bottomOffset;

		double top = dataProvider.getTop();
		double depth = dataProvider.getDepth();

		double topPercent = topOffset / (double) Math.max(dataProvider.getZSize() - 1, 1);
		double bottomPercent = bottomSlice / (double) Math.max(dataProvider.getZSize() - 1, 1);
		double topElevation = top - topPercent * depth;
		double bottomElevation = top - bottomPercent * depth;

		Position swPosTop = dataProvider.getPosition(minLonOffset, minLatOffset);
		Position nwPosTop = dataProvider.getPosition(minLonOffset, maxLatSlice);
		Position sePosTop = dataProvider.getPosition(maxLonSlice, minLatOffset);
		Position nePosTop = dataProvider.getPosition(maxLonSlice, maxLatSlice);

		if (depth != 0 && dc.getVerticalExaggeration() > 0)
		{
			double deltaLat = 0.005, deltaLon = 0.005;
			if (sw)
			{
				Position swPosBottom = new Position(swPosTop, swPosTop.elevation - depth);
				Position otherPos = swPosTop.add(Position.fromDegrees(-deltaLat, deltaLon, 0));
				insertClippingPlaneForPositions(dc, curtainClippingPlanes, 0, swPosTop, swPosBottom, otherPos);
			}
			if (nw)
			{
				Position nwPosBottom = new Position(nwPosTop, nwPosTop.elevation - depth);
				Position otherPos = nwPosTop.add(Position.fromDegrees(deltaLat, deltaLon, 0));
				insertClippingPlaneForPositions(dc, curtainClippingPlanes, 4, nwPosTop, otherPos, nwPosBottom);
			}
			if (se)
			{
				Position sePosBottom = new Position(sePosTop, sePosTop.elevation - depth);
				Position otherPos = sePosTop.add(Position.fromDegrees(-deltaLat, -deltaLon, 0));
				insertClippingPlaneForPositions(dc, curtainClippingPlanes, 8, sePosTop, otherPos, sePosBottom);
			}
			if (ne)
			{
				Position nePosBottom = new Position(nePosTop, nePosTop.elevation - depth);
				Position otherPos = nePosTop.add(Position.fromDegrees(deltaLat, -deltaLon, 0));
				insertClippingPlaneForPositions(dc, curtainClippingPlanes, 12, nePosTop, nePosBottom, otherPos);
			}
		}

		//the following only works for a spherical earth (as opposed to flat earth), as it relies on adjacent
		//points not being colinear (3 points along a latitude are not colinear when wrapped around a sphere)

		if (minLon)
		{
			Position middlePos = dataProvider.getPosition(minLonOffset, (maxLatSlice + minLatOffset) / 2);
			middlePos = midpointPositionIfEqual(middlePos, nwPosTop, swPosTop);
			insertClippingPlaneForLatLons(dc, topClippingPlanes, 0, middlePos, nwPosTop, swPosTop, topElevation);
			insertClippingPlaneForLatLons(dc, bottomClippingPlanes, 0, middlePos, nwPosTop, swPosTop, bottomElevation);
		}
		if (maxLon)
		{
			Position middlePos = dataProvider.getPosition(maxLonSlice, (maxLatSlice + minLatOffset) / 2);
			middlePos = midpointPositionIfEqual(middlePos, sePosTop, nePosTop);
			insertClippingPlaneForLatLons(dc, topClippingPlanes, 4, middlePos, sePosTop, nePosTop, topElevation);
			insertClippingPlaneForLatLons(dc, bottomClippingPlanes, 4, middlePos, sePosTop, nePosTop, bottomElevation);
		}
		if (minLat)
		{
			Position middlePos = dataProvider.getPosition((maxLonSlice + minLonOffset) / 2, minLatOffset);
			middlePos = midpointPositionIfEqual(middlePos, swPosTop, sePosTop);
			insertClippingPlaneForLatLons(dc, topClippingPlanes, 8, middlePos, swPosTop, sePosTop, topElevation);
			insertClippingPlaneForLatLons(dc, bottomClippingPlanes, 8, middlePos, swPosTop, sePosTop, bottomElevation);
		}
		if (maxLat)
		{
			Position middlePos = dataProvider.getPosition((maxLonSlice + minLonOffset) / 2, maxLatSlice);
			middlePos = midpointPositionIfEqual(middlePos, nePosTop, nwPosTop);
			insertClippingPlaneForLatLons(dc, topClippingPlanes, 12, middlePos, nePosTop, nwPosTop, topElevation);
			insertClippingPlaneForLatLons(dc, bottomClippingPlanes, 12, middlePos, nePosTop, nwPosTop, bottomElevation);
		}

		minLonClipDirty = maxLonClipDirty = minLatClipDirty = maxLatClipDirty = topClipDirty = bottomClipDirty = false;
	}

	/**
	 * Return the midpoint of the two end positions if the given middle position
	 * is equal to one of the ends.
	 * 
	 * @param middle
	 * @param end1
	 * @param end2
	 * @return Midpoint between end1 and end2 if middle equals end1 or end2.
	 */
	protected Position midpointPositionIfEqual(Position middle, Position end1, Position end2)
	{
		if (middle.equals(end1) || middle.equals(end2))
		{
			return Position.interpolate(0.5, end1, end2);
		}
		return middle;
	}

	/**
	 * Insert a clipping plane vector into the given array. The vector is
	 * calculated by finding a plane that intersects the three given positions.
	 * 
	 * @param dc
	 * @param clippingPlaneArray
	 *            Array to insert clipping plane vector into
	 * @param arrayOffset
	 *            Array start offset to begin inserting values at
	 * @param p1
	 *            First position that the plane must intersect
	 * @param p2
	 *            Second position that the plane must intersect
	 * @param p3
	 *            Third position that the plane must intersect
	 */
	protected void insertClippingPlaneForPositions(DrawContext dc, double[] clippingPlaneArray, int arrayOffset,
			Position p1, Position p2, Position p3)
	{
		Globe globe = dc.getGlobe();
		Vec4 v1 = globe.computePointFromPosition(p1, p1.elevation * dc.getVerticalExaggeration());
		Vec4 v2 = globe.computePointFromPosition(p2, p2.elevation * dc.getVerticalExaggeration());
		Vec4 v3 = globe.computePointFromPosition(p3, p3.elevation * dc.getVerticalExaggeration());
		insertClippingPlaneForPoints(clippingPlaneArray, arrayOffset, v1, v2, v3);
	}

	/**
	 * Insert a clipping plane vector into the given array. The vector is
	 * calculated by finding a plane that intersects the three given latlons at
	 * the given elevation.
	 * 
	 * @param dc
	 * @param clippingPlaneArray
	 *            Array to insert clipping plane vector into
	 * @param arrayOffset
	 *            Array start offset to begin inserting values at
	 * @param l1
	 *            First latlon that the plane must intersect
	 * @param l2
	 *            Second latlon that the plane must intersect
	 * @param l3
	 *            Third latlon that the plane must intersect
	 * @param elevation
	 *            Elevation of the latlons
	 */
	protected void insertClippingPlaneForLatLons(DrawContext dc, double[] clippingPlaneArray, int arrayOffset,
			LatLon l1, LatLon l2, LatLon l3, double elevation)
	{
		Globe globe = dc.getGlobe();
		double exaggeratedElevation = elevation * dc.getVerticalExaggeration();
		Vec4 v1 = globe.computePointFromPosition(l1, exaggeratedElevation);
		Vec4 v2 = globe.computePointFromPosition(l2, exaggeratedElevation);
		Vec4 v3 = globe.computePointFromPosition(l3, exaggeratedElevation);
		insertClippingPlaneForPoints(clippingPlaneArray, arrayOffset, v1, v2, v3);
	}

	/**
	 * Insert a clipping plane vector into the given array. The vector is
	 * calculated by finding a plane that intersects the three given points.
	 * 
	 * @param clippingPlaneArray
	 *            Array to insert clipping plane vector into
	 * @param arrayOffset
	 *            Array start offset to begin inserting values at
	 * @param v1
	 *            First point that the plane must intersect
	 * @param v2
	 *            Second point that the plane must intersect
	 * @param v3
	 *            Third point that the plane must intersect
	 */
	protected void insertClippingPlaneForPoints(double[] clippingPlaneArray, int arrayOffset, Vec4 v1, Vec4 v2, Vec4 v3)
	{
		Line l1 = Line.fromSegment(v1, v3);
		Line l2 = Line.fromSegment(v1, v2);
		Plane plane = GeometryUtil.createPlaneContainingLines(l1, l2);
		Vec4 v = plane.getVector();
		clippingPlaneArray[arrayOffset + 0] = v.x;
		clippingPlaneArray[arrayOffset + 1] = v.y;
		clippingPlaneArray[arrayOffset + 2] = v.z;
		clippingPlaneArray[arrayOffset + 3] = v.w;
	}

	/**
	 * Generate a texture slice through the volume at the given position. Uses a
	 * {@link ColorMap} to map values to colors (or simply interpolates the hue
	 * if no colormap is provided - assumes values between 0 and 1).
	 * 
	 * @param axis
	 *            Slicing axis (0 for a longitude slice, 1 for a latitude slice,
	 *            2 for an elevation slice).
	 * @param position
	 *            Longitude, latitude, or elevation at which to slice.
	 * @param rectangle
	 *            Sub-rectangle within the volume slice to get texture data for.
	 * @return A {@link BufferedImage} containing a representation of the volume
	 *         slice.
	 */
	protected BufferedImage generateTexture(int axis, int position, Rectangle rectangle)
	{
		BufferedImage image = new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_INT_ARGB);
		float minimum = dataProvider.getMinValue();
		float maximum = dataProvider.getMaxValue();
		for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++)
		{
			for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++)
			{
				int vx = axis == 2 ? x : axis == 1 ? x : position;
				int vy = axis == 2 ? y : axis == 1 ? position : x;
				int vz = axis == 2 ? position : y;
				float value = dataProvider.getValue(vx, vy, vz);
				int rgb = noDataColor != null ? noDataColor.getRGB() : 0;
				if (value != dataProvider.getNoDataValue())
				{
					if (colorMap != null)
						rgb = colorMap.calculateColorNotingIsValuesPercentages(value, minimum, maximum).getRGB();
					else
						rgb = Color.HSBtoRGB(-0.3f - value * 0.7f, 1.0f, 1.0f);
				}
				image.setRGB(x - rectangle.x, y - rectangle.y, rgb);
			}
		}
		return image;
	}

	/**
	 * Update the given {@link TextureRenderer} with the provided image, and
	 * sets the {@link FastShape}'s texture it.
	 * 
	 * @param image
	 *            Image to update texture with
	 * @param texture
	 *            Texture to update
	 * @param shape
	 *            Shape to set texture in
	 */
	protected void updateTexture(BufferedImage image, TextureRenderer texture, FastShape shape)
	{
		Graphics2D g = null;
		try
		{
			g = (Graphics2D) texture.getImage().getGraphics();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
			g.drawImage(image, 0, 0, null);
		}
		finally
		{
			if (g != null)
			{
				g.dispose();
			}
		}
		texture.markDirty(0, 0, texture.getWidth(), texture.getHeight());
		shape.setTexture(texture.getTexture());
	}

	@Override
	public CoordinateTransformation getCoordinateTransformation()
	{
		return coordinateTransformation;
	}

	@Override
	protected void doPick(DrawContext dc, Point point)
	{
		doRender(dc);
	}

	@Override
	protected void doRender(DrawContext dc)
	{
		if (!isEnabled())
		{
			return;
		}

		dataProvider.requestData(this);

		synchronized (dataLock)
		{
			//recalculate surfaces and clipping planes each frame (in case user drags one of the surfaces)
			recalculateSurfaces();
			recalculateClippingPlanes(dc);

			//sort the shapes from back-to-front
			FastShape[] shapes =
					new FastShape[] { topSurface, bottomSurface, minLonCurtain, maxLonCurtain, minLatCurtain,
							maxLatCurtain };
			Arrays.sort(shapes, new ShapeComparator(dc));

			//test all the shapes with the minimum distance, culling them if they are outside
			if (minimumDistance != null)
			{
				for (int i = 0; i < shapes.length; i++)
				{
					if (shapes[i] != null)
					{
						Extent extent = shapes[i].getExtent();
						if (extent != null)
						{
							double distanceToEye =
									extent.getCenter().distanceTo3(dc.getView().getEyePoint()) - extent.getRadius();
							if (distanceToEye > minimumDistance)
							{
								shapes[i] = null;
							}
						}
					}
				}
			}

			GL gl = dc.getGL();
			try
			{
				//push the OpenGL clipping plane state on the attribute stack
				gl.glPushAttrib(GL.GL_TRANSFORM_BIT);

				boolean oldDeepPicking = dc.isDeepPickingEnabled();
				try
				{
					//deep picking needs to be enabled, because the shapes could be below the surface
					if (dc.isPickingMode())
					{
						dc.setDeepPickingEnabled(true);
						pickSupport.beginPicking(dc);
					}

					//draw each shape
					for (FastShape shape : shapes)
					{
						if (shape != null)
						{
							setupClippingPlanes(dc, shape == topSurface, shape == bottomSurface);

							//if in picking mode, render the shape with a unique picking color, and don't light or texture
							if (dc.isPickingMode())
							{
								Color color = dc.getUniquePickColor();
								pickSupport.addPickableObject(color.getRGB(), shape);
								shape.setColor(color);
							}
							else
							{
								shape.setColor(Color.white);
							}
							shape.setLighted(!dc.isPickingMode());
							shape.setTextured(!dc.isPickingMode());
							shape.render(dc);
						}
					}

					//disable all clipping planes enabled earlier
					for (int i = 0; i < 4; i++)
					{
						gl.glDisable(GL.GL_CLIP_PLANE0 + i);
					}

					if (dc.isPickingMode())
					{
						pickSupport.resolvePick(dc, dc.getPickPoint(), this);
					}
					else if (dragging)
					{
						//render a bounding box around the data if the user is dragging a surface
						renderBoundingBox(dc);
					}
				}
				finally
				{
					//reset the deep picking flag
					if (dc.isPickingMode())
					{
						pickSupport.endPicking(dc);
						dc.setDeepPickingEnabled(oldDeepPicking);
					}
				}
			}
			finally
			{
				gl.glPopAttrib();
			}
		}
	}

	protected void setupClippingPlanes(DrawContext dc, boolean top, boolean bottom)
	{
		boolean minLon = minLonOffset > 0;
		boolean maxLon = maxLonOffset > 0;
		boolean minLat = minLatOffset > 0;
		boolean maxLat = maxLatOffset > 0;

		boolean[] enabled;
		double[] array;

		GL gl = dc.getGL();
		if (top || bottom)
		{
			array = top ? topClippingPlanes : bottomClippingPlanes;
			enabled = new boolean[] { minLon, maxLon, minLat, maxLat };
		}
		else
		{
			array = curtainClippingPlanes;
			boolean sw = minLon || minLat;
			boolean nw = minLon || maxLat;
			boolean se = maxLon || minLat;
			boolean ne = maxLon || maxLat;
			enabled = new boolean[] { sw, nw, se, ne };
		}

		for (int i = 0; i < 4; i++)
		{
			gl.glClipPlane(GL.GL_CLIP_PLANE0 + i, array, i * 4);
			if (enabled[i])
			{
				gl.glEnable(GL.GL_CLIP_PLANE0 + i);
			}
			else
			{
				gl.glDisable(GL.GL_CLIP_PLANE0 + i);
			}
		}
	}

	/**
	 * Render a bounding box around the data. Used when dragging surfaces, so
	 * user has an idea of where the data extents lie when slicing.
	 * 
	 * @param dc
	 */
	protected void renderBoundingBox(DrawContext dc)
	{
		if(boundingBoxShape == null)
		{
			boundingBoxShape = dataProvider.createBoundingBox();
		}
		boundingBoxShape.render(dc);
		
		/*Position bl = dataProvider.getPosition(0, 0);
		Position br = dataProvider.getPosition(dataProvider.getXSize() - 1, 0);
		Position tl = dataProvider.getPosition(0, dataProvider.getYSize() - 1);
		Position tr = dataProvider.getPosition(dataProvider.getXSize() - 1, dataProvider.getYSize() - 1);
		Position cb = Position.interpolate(0.5, bl, br);
		Position ct = Position.interpolate(0.5, tl, tr);
		Globe globe = dc.getGlobe();
		Vec4 blv = globe.computePointFromPosition(bl);
		Vec4 brv = globe.computePointFromPosition(br);
		Vec4 tlv = globe.computePointFromPosition(tl);
		Vec4 trv = globe.computePointFromPosition(tr);
		double bw = blv.distanceTo3(brv) / 2d;
		double tw = tlv.distanceTo3(trv) / 2d;
		Box box = new Box(cb, ct, bw, tw);
		box.setAltitudes(dataProvider.getTop() - dataProvider.getDepth(), dataProvider.getTop());
		box.getAttributes().setDrawInterior(false);
		box.getAttributes().setDrawOutline(true);
		box.getAttributes().setOutlineMaterial(Material.WHITE);
		box.getAttributes().setOutlineWidth(2.0);
		box.render(dc);*/
		
		/*Sector sector = dataProvider.getSector();
		Position center = new Position(sector.getCentroid(), dataProvider.getTop() - dataProvider.getDepth() / 2);
		Vec4 v1 = dc.getGlobe().computePointFromPosition(sector.getMinLatitude(), center.longitude, center.elevation);
		Vec4 v2 = dc.getGlobe().computePointFromPosition(sector.getMaxLatitude(), center.longitude, center.elevation);
		double distance = v1.distanceTo3(v2) / 2;
		LatLon latlon1 = new LatLon(center.latitude, sector.getMinLongitude());
		LatLon latlon2 = new LatLon(center.latitude, sector.getMaxLongitude());
		Box box = new Box(latlon1, latlon2, distance, distance);
		box.setAltitudes(dataProvider.getTop() - dataProvider.getDepth(), dataProvider.getTop());
		box.getAttributes().setDrawInterior(false);
		box.getAttributes().setDrawOutline(true);
		box.getAttributes().setOutlineMaterial(Material.WHITE);
		box.getAttributes().setOutlineWidth(2.0);
		box.render(dc);*/
	}

	@Override
	public boolean isWireframe()
	{
		return wireframe;
	}

	@Override
	public void setWireframe(boolean wireframe)
	{
		this.wireframe = wireframe;
		synchronized (dataLock)
		{
			if (topSurface != null)
			{
				topSurface.setWireframe(wireframe);
				bottomSurface.setWireframe(wireframe);
				minLonCurtain.setWireframe(wireframe);
				maxLonCurtain.setWireframe(wireframe);
				minLatCurtain.setWireframe(wireframe);
				maxLatCurtain.setWireframe(wireframe);
			}
		}
	}

	@Override
	public void selected(SelectEvent event)
	{
		//ignore this event if ctrl, alt, or shift are down
		if (event.getMouseEvent() != null)
		{
			int onmask = MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK | MouseEvent.ALT_DOWN_MASK;
			if ((event.getMouseEvent().getModifiersEx() & onmask) == onmask)
			{
				return;
			}
		}

		//don't allow dragging if there's only one layer in any one direction
		if (dataProvider.getXSize() <= 1 || dataProvider.getYSize() <= 1 || dataProvider.getZSize() <= 1)
		{
			return;
		}

		//we only care about drag events
		boolean drag = event.getEventAction().equals(SelectEvent.DRAG);
		boolean dragEnd = event.getEventAction().equals(SelectEvent.DRAG_END);
		if (!(drag || dragEnd))
		{
			return;
		}

		Object topObject = event.getTopObject();
		FastShape pickedShape = topObject instanceof FastShape ? (FastShape) topObject : null;
		if (pickedShape == null)
		{
			return;
		}

		boolean top = pickedShape == topSurface;
		boolean bottom = pickedShape == bottomSurface;
		boolean minLon = pickedShape == minLonCurtain;
		boolean maxLon = pickedShape == maxLonCurtain;
		boolean minLat = pickedShape == minLatCurtain;
		boolean maxLat = pickedShape == maxLatCurtain;
		if (top || bottom || minLon || maxLon || minLat || maxLat)
		{
			if (dragEnd)
			{
				dragging = false;
				event.consume();
			}
			else if (drag)
			{
				if (!dragging || dragStartCenter == null)
				{
					Extent extent = pickedShape.getExtent();
					if (extent != null)
					{
						dragStartCenter = extent.getCenter();
					}
				}

				if (dragStartCenter != null)
				{
					if (top || bottom)
					{
						dragElevation(event.getPickPoint(), pickedShape);
					}
					else if (minLon || maxLon)
					{
						dragLongitude(event.getPickPoint(), pickedShape);
					}
					else
					{
						dragLatitude(event.getPickPoint(), pickedShape);
					}
				}
				dragging = true;
				event.consume();
			}
		}
	}

	/**
	 * Drag an elevation surface up and down.
	 * 
	 * @param pickPoint
	 *            Point at which the user is dragging the mouse.
	 * @param shape
	 *            Shape to drag
	 */
	protected void dragElevation(Point pickPoint, FastShape shape)
	{
		// Calculate the plane projected from screen y=pickPoint.y
		Line screenLeftRay = wwd.getView().computeRayFromScreenPoint(pickPoint.x - 100, pickPoint.y);
		Line screenRightRay = wwd.getView().computeRayFromScreenPoint(pickPoint.x + 100, pickPoint.y);

		// As the two lines are very close to parallel, use an arbitrary line joining them rather than the two lines to avoid precision problems
		Line joiner = Line.fromSegment(screenLeftRay.getPointAt(500), screenRightRay.getPointAt(500));
		Plane screenPlane = GeometryUtil.createPlaneContainingLines(screenLeftRay, joiner);
		if (screenPlane == null)
		{
			return;
		}

		// Calculate the origin-marker ray
		Globe globe = wwd.getModel().getGlobe();
		Line centreRay = Line.fromSegment(globe.getCenter(), dragStartCenter);
		Vec4 intersection = screenPlane.intersect(centreRay);
		if (intersection == null)
		{
			return;
		}

		Position intersectionPosition = globe.computePositionFromPoint(intersection);
		if (!dragging)
		{
			dragStartPosition = intersectionPosition.elevation;
			dragStartSlice = shape == topSurface ? topOffset : bottomOffset;
		}
		else
		{
			double deltaElevation =
					(dragStartPosition - intersectionPosition.elevation)
							/ (wwd.getSceneController().getVerticalExaggeration());
			double deltaPercentage = deltaElevation / dataProvider.getDepth();
			int sliceMovement = (int) (deltaPercentage * (dataProvider.getZSize() - 1));
			if (shape == topSurface)
			{
				topOffset = Util.clamp(dragStartSlice + sliceMovement, 0, dataProvider.getZSize() - 1);
				bottomOffset = Util.clamp(bottomOffset, 0, dataProvider.getZSize() - 1 - topOffset);
			}
			else
			{
				bottomOffset = Util.clamp(dragStartSlice - sliceMovement, 0, dataProvider.getZSize() - 1);
				topOffset = Util.clamp(topOffset, 0, dataProvider.getZSize() - 1 - bottomOffset);
			}
		}
	}

	/**
	 * Drag a longitude curtain left and right.
	 * 
	 * @param pickPoint
	 *            Point at which the user is dragging the mouse.
	 * @param shape
	 *            Shape to drag
	 */
	protected void dragLongitude(Point pickPoint, FastShape shape)
	{
		Globe globe = wwd.getView().getGlobe();
		double centerElevation = globe.computePositionFromPoint(dragStartCenter).elevation;

		// Compute the ray from the screen point
		Line ray = wwd.getView().computeRayFromScreenPoint(pickPoint.x, pickPoint.y);
		Intersection[] intersections = globe.intersect(ray, centerElevation);
		if (intersections == null || intersections.length == 0)
		{
			return;
		}
		Vec4 intersection = ray.nearestIntersectionPoint(intersections);
		if (intersection == null)
		{
			return;
		}

		Position position = globe.computePositionFromPoint(intersection);
		if (!dragging)
		{
			dragStartPosition = position.longitude.degrees;
			dragStartSlice = shape == minLonCurtain ? minLonOffset : maxLonOffset;
		}
		else
		{
			double deltaLongitude = position.longitude.degrees - dragStartPosition;
			double deltaPercentage = deltaLongitude / dataProvider.getSector().getDeltaLonDegrees();
			int sliceMovement = (int) (deltaPercentage * (dataProvider.getXSize() - 1));
			if (shape == minLonCurtain)
			{
				minLonOffset = Util.clamp(dragStartSlice + sliceMovement, 0, dataProvider.getXSize() - 1);
				maxLonOffset = Util.clamp(maxLonOffset, 0, dataProvider.getXSize() - 1 - minLonOffset);
			}
			else
			{
				maxLonOffset = Util.clamp(dragStartSlice - sliceMovement, 0, dataProvider.getXSize() - 1);
				minLonOffset = Util.clamp(minLonOffset, 0, dataProvider.getXSize() - 1 - maxLonOffset);
			}
		}
	}

	/**
	 * Drag a latitude curtain left and right.
	 * 
	 * @param pickPoint
	 *            Point at which the user is dragging the mouse.
	 * @param shape
	 *            Shape to drag
	 */
	protected void dragLatitude(Point pickPoint, FastShape shape)
	{
		Globe globe = wwd.getView().getGlobe();
		double centerElevation = globe.computePositionFromPoint(dragStartCenter).elevation;

		// Compute the ray from the screen point
		Line ray = wwd.getView().computeRayFromScreenPoint(pickPoint.x, pickPoint.y);
		Intersection[] intersections = globe.intersect(ray, centerElevation);
		if (intersections == null || intersections.length == 0)
		{
			return;
		}
		Vec4 intersection = ray.nearestIntersectionPoint(intersections);
		if (intersection == null)
		{
			return;
		}

		Position position = globe.computePositionFromPoint(intersection);
		if (!dragging)
		{
			dragStartPosition = position.latitude.degrees;
			dragStartSlice = shape == minLatCurtain ? minLatOffset : maxLatOffset;
		}
		else
		{
			double deltaLatitude = position.latitude.degrees - dragStartPosition;
			double deltaPercentage = deltaLatitude / dataProvider.getSector().getDeltaLatDegrees();
			int sliceMovement = (int) (deltaPercentage * (dataProvider.getYSize() - 1));
			if (shape == minLatCurtain)
			{
				minLatOffset = Util.clamp(dragStartSlice + sliceMovement, 0, dataProvider.getYSize() - 1);
				maxLatOffset = Util.clamp(maxLatOffset, 0, dataProvider.getYSize() - 1 - minLatOffset);
			}
			else
			{
				maxLatOffset = Util.clamp(dragStartSlice - sliceMovement, 0, dataProvider.getYSize() - 1);
				minLatOffset = Util.clamp(minLatOffset, 0, dataProvider.getYSize() - 1 - maxLatOffset);
			}
		}
	}

	/**
	 * {@link Comparator} used to sort {@link FastShape}s from back-to-front
	 * (from the view eye point).
	 */
	protected class ShapeComparator implements Comparator<FastShape>
	{
		private final DrawContext dc;

		public ShapeComparator(DrawContext dc)
		{
			this.dc = dc;
		}

		@Override
		public int compare(FastShape o1, FastShape o2)
		{
			if (o1 == o2)
				return 0;
			if (o2 == null)
				return -1;
			if (o1 == null)
				return 1;

			Extent e1 = o1.getExtent();
			Extent e2 = o2.getExtent();
			if (e2 == null)
				return -1;
			if (e1 == null)
				return 1;

			Vec4 eyePoint = dc.getView().getEyePoint();
			double d1 = e1.getCenter().distanceToSquared3(eyePoint);
			double d2 = e2.getCenter().distanceToSquared3(eyePoint);
			return -Double.compare(d1, d2);
		}
	}
}
