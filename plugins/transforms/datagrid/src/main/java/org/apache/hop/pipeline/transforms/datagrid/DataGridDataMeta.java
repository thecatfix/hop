/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.pipeline.transforms.datagrid;

import org.apache.hop.core.Const;
import org.apache.hop.metadata.api.HopMetadataProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataGridDataMeta implements Cloneable {

  @HopMetadataProperty(key = "item")
  private List<String> datalines;

  @HopMetadataProperty(injectionKeyDescription = "DataGrid.Injection.DataLine")
  private String injectionDatalines;

  @HopMetadataProperty(injectionKeyDescription = "DataGrid.Injection.Splitter")
  private String splitter;

  public DataGridDataMeta() {
    datalines = new ArrayList<>();
  }

  public DataGridDataMeta(List<String> datalines) {
    this.datalines = datalines;
  }

  public DataGridDataMeta(DataGridDataMeta m) {
    this.datalines = m.datalines;
  }

  public DataGridDataMeta clone() {
    return new DataGridDataMeta(this);
  }

  public List<String> getDatalines() {
    return datalines;
  }

  public void setDatalines(List<String> datalines) {
    this.datalines = datalines;
  }

  public String getInjectionDatalines() {
    return injectionDatalines;
  }

  public void setInjectionDatalines(String injectionDatalines) {
    this.injectionDatalines = injectionDatalines;
    this.datalines = Arrays.asList(injectionDatalines.split(Const.NVL(splitter, ",")));
  }

  public String getSplitter() {
    return splitter;
  }

  public void setSplitter(String splitter) {
    this.splitter = splitter;
  }
}
