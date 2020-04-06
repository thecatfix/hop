/*! ******************************************************************************
 *
 * Hop : The Hop Orchestration Platform
 *
 * http://www.project-hop.org
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.pipeline.transforms.syslog;

import org.apache.hop.core.CheckResult;
import org.apache.hop.core.CheckResultInterface;
import org.apache.hop.core.exception.HopXMLException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.iVariables;
import org.apache.hop.core.xml.XMLHandler;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.job.entries.syslog.SyslogDefs;
import org.apache.hop.metastore.api.IMetaStore;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransformData;
import org.apache.hop.pipeline.transform.ITransform;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transform.ITransform;
import org.w3c.dom.Node;

import java.util.List;

public class SyslogMessageMeta extends BaseTransformMeta implements ITransform {
  private static Class<?> PKG = SyslogMessageMeta.class; // for i18n purposes, needed by Translator!!

  /**
   * dynamic message fieldname
   */
  private String messagefieldname;
  private String serverName;
  private String port;
  private String facility;
  private String priority;
  private String datePattern;
  private boolean addTimestamp;
  private boolean addHostName;

  public SyslogMessageMeta() {
    super(); // allocate BaseTransformMeta
  }

  public void loadXML( Node transformNode, IMetaStore metaStore ) throws HopXMLException {
    readData( transformNode, metaStore );
  }

  public Object clone() {
    SyslogMessageMeta retval = (SyslogMessageMeta) super.clone();

    return retval;
  }

  public void setDefault() {
    messagefieldname = null;
    port = String.valueOf( SyslogDefs.DEFAULT_PORT );
    serverName = null;
    facility = SyslogDefs.FACILITYS[ 0 ];
    priority = SyslogDefs.PRIORITYS[ 0 ];
    datePattern = SyslogDefs.DEFAULT_DATE_FORMAT;
    addTimestamp = true;
    addHostName = true;
  }

  /**
   * @return Returns the serverName.
   */
  public String getServerName() {
    return serverName;
  }

  /**
   * @param serverName The serverName to set.
   */
  public void setServerName( String serverName ) {
    this.serverName = serverName;
  }

  /**
   * @return Returns the Facility.
   */
  public String getFacility() {
    return facility;
  }

  /**
   * @param facility The facility to set.
   */
  public void setFacility( String facility ) {
    this.facility = facility;
  }

  /**
   * @param priority The priority to set.
   */
  public void setPriority( String priority ) {
    this.priority = priority;
  }

  /**
   * @return Returns the priority.
   */
  public String getPriority() {
    return priority;
  }

  /**
   * @param messagefieldname The messagefieldname to set.
   */
  public void setMessageFieldName( String messagefieldname ) {
    this.messagefieldname = messagefieldname;
  }

  /**
   * @return Returns the messagefieldname.
   */
  public String getMessageFieldName() {
    return messagefieldname;
  }

  /**
   * @return Returns the port.
   */
  public String getPort() {
    return port;
  }

  /**
   * @param port The port to set.
   */
  public void setPort( String port ) {
    this.port = port;
  }

  /**
   * @param value
   * @deprecated use {@link #setAddTimestamp(boolean)} instead
   */
  @Deprecated
  public void addTimestamp( boolean value ) {
    setAddTimestamp( value );
  }

  public void setAddTimestamp( boolean value ) {
    this.addTimestamp = value;
  }

  /**
   * @return Returns the addTimestamp.
   */
  public boolean isAddTimestamp() {
    return addTimestamp;
  }

  /**
   * @param pattern The datePattern to set.
   */
  public void setDatePattern( String pattern ) {
    this.datePattern = pattern;
  }

  /**
   * @return Returns the datePattern.
   */
  public String getDatePattern() {
    return datePattern;
  }

  /**
   * @param value
   * @deprecated use {@link #setAddHostName(boolean)} instead
   */
  @Deprecated
  public void addHostName( boolean value ) {
    setAddHostName( value );
  }

  public void setAddHostName( boolean value ) {
    this.addHostName = value;
  }

  /**
   * @return Returns the addHostName.
   */
  public boolean isAddHostName() {
    return addHostName;
  }

  public String getXML() {
    StringBuilder retval = new StringBuilder();

    retval.append( "    " + XMLHandler.addTagValue( "messagefieldname", messagefieldname ) );
    retval.append( "    " + XMLHandler.addTagValue( "port", port ) );
    retval.append( "    " + XMLHandler.addTagValue( "servername", serverName ) );
    retval.append( "    " + XMLHandler.addTagValue( "facility", facility ) );
    retval.append( "    " + XMLHandler.addTagValue( "priority", priority ) );
    retval.append( "    " + XMLHandler.addTagValue( "addTimestamp", addTimestamp ) );
    retval.append( "    " + XMLHandler.addTagValue( "datePattern", datePattern ) );
    retval.append( "    " + XMLHandler.addTagValue( "addHostName", addHostName ) );

    return retval.toString();
  }

  private void readData( Node transformNode, IMetaStore metaStore ) throws HopXMLException {
    try {
      messagefieldname = XMLHandler.getTagValue( transformNode, "messagefieldname" );
      port = XMLHandler.getTagValue( transformNode, "port" );
      serverName = XMLHandler.getTagValue( transformNode, "servername" );
      facility = XMLHandler.getTagValue( transformNode, "facility" );
      priority = XMLHandler.getTagValue( transformNode, "priority" );
      datePattern = XMLHandler.getTagValue( transformNode, "datePattern" );
      addTimestamp = "Y".equalsIgnoreCase( XMLHandler.getTagValue( transformNode, "addTimestamp" ) );
      addHostName = "Y".equalsIgnoreCase( XMLHandler.getTagValue( transformNode, "addHostName" ) );

    } catch ( Exception e ) {
      throw new HopXMLException( BaseMessages.getString(
        PKG, "SyslogMessageMeta.Exception.UnableToReadTransformMeta" ), e );
    }
  }

  public void check( List<CheckResultInterface> remarks, PipelineMeta pipelineMeta, TransformMeta transformMeta,
                     IRowMeta prev, String[] input, String[] output, IRowMeta info, iVariables variables,
                     IMetaStore metaStore ) {
    CheckResult cr;
    String error_message = "";

    // source filename
    if ( Utils.isEmpty( messagefieldname ) ) {
      error_message = BaseMessages.getString( PKG, "SyslogMessageMeta.CheckResult.MessageFieldMissing" );
      cr = new CheckResult( CheckResult.TYPE_RESULT_ERROR, error_message, transformMeta );
      remarks.add( cr );
    } else {
      error_message = BaseMessages.getString( PKG, "SyslogMessageMeta.CheckResult.MessageFieldOK" );
      cr = new CheckResult( CheckResult.TYPE_RESULT_OK, error_message, transformMeta );
      remarks.add( cr );
    }

    // See if we have input streams leading to this transform!
    if ( input.length > 0 ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SyslogMessageMeta.CheckResult.ReceivingInfoFromOtherTransforms" ), transformMeta );
      remarks.add( cr );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SyslogMessageMeta.CheckResult.NoInpuReceived" ), transformMeta );
      remarks.add( cr );
    }

  }

  public ITransform getTransform( TransformMeta transformMeta, ITransformData data, int cnr,
                                PipelineMeta pipelineMeta, Pipeline pipeline ) {
    return new SyslogMessage( transformMeta, this, data, cnr, pipelineMeta, pipeline );
  }

  public ITransformData getTransformData() {
    return new SyslogMessageData();
  }

  public boolean supportsErrorHandling() {
    return true;
  }

}