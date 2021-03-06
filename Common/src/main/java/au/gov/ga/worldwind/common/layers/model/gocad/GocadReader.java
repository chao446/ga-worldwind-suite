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
package au.gov.ga.worldwind.common.layers.model.gocad;

import java.net.URL;
import java.util.regex.Pattern;

import au.gov.ga.worldwind.common.util.FastShape;

/**
 * A reader that creates a {@link FastShape} from a GOCAD file.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public interface GocadReader
{
	final static Pattern vertexPattern =
			Pattern.compile("P?VRTX\\s+(\\d+)\\s+([\\d.\\-]+)\\s+([\\d.\\-]+)\\s+([\\d.\\-]+)([\\s\\d.\\-e]*)\\s*(?:\\D+)?\\s*");
	final static Pattern atomPattern = Pattern.compile("P?ATOM\\s+(\\d+)\\s+(\\d+)([\\s\\d.\\-e]*)\\s*");
	final static Pattern zpositivePattern = Pattern.compile("ZPOSITIVE\\s+(\\w+)\\s*");
	final static Pattern namePattern = Pattern.compile("name:\\s*(.*)\\s*");
	final static Pattern solidColorPattern = Pattern.compile("\\*solid\\*color:.+");
	final static Pattern lineColorPattern = Pattern.compile("\\*line\\*color:.+");
	final static Pattern paintedVariablePattern = Pattern.compile("\\*painted\\*variable:\\s*(.*?)\\s*");
	final static Pattern nodataValuesPattern = Pattern.compile("NO_DATA_VALUES\\s*([\\s\\d.\\-e]*)\\s*");
	final static Pattern propertiesPattern = Pattern.compile("PROPERTIES\\s+(.*)\\s*");
	final static Pattern propertyPattern = Pattern.compile("PROP_(\\S+)\\s+(\\d+)\\s+(\\S+).*");

	/**
	 * Called before reading any lines.
	 * 
	 * @param parameters
	 *            Reader parameters to use when reading this file
	 */
	void begin(GocadReaderParameters parameters);

	/**
	 * Parse a line from the GOCAD file. The HEADER line and the END line are
	 * not passed to this function.
	 * 
	 * @param line
	 *            Single line read from the GOCAD file
	 */
	void addLine(String line);

	/**
	 * Called after reading all the lines from this GOCAD object. A
	 * {@link FastShape} can be created from the read geometry and returned.
	 * 
	 * @param context
	 *            URL context in which this object is being read; can be used to
	 *            resolve relative references
	 * 
	 * @return A {@link FastShape} containing the geometry read
	 */
	FastShape end(URL context);
}
