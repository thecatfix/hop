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
 *
 */

package org.apache.hop.ui.hopgui.perspective.execution;

import org.apache.hop.core.Const;
import org.apache.hop.core.Props;
import org.apache.hop.core.Result;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.core.gui.AreaOwner;
import org.apache.hop.core.gui.IGc;
import org.apache.hop.core.gui.Point;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.gui.plugin.key.GuiKeyboardShortcut;
import org.apache.hop.core.gui.plugin.key.GuiOsxKeyboardShortcut;
import org.apache.hop.core.gui.plugin.toolbar.GuiToolbarElement;
import org.apache.hop.core.gui.plugin.toolbar.GuiToolbarElementType;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.RowBuffer;
import org.apache.hop.core.row.RowMetaBuilder;
import org.apache.hop.execution.*;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelinePainter;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.SelectRowDialog;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.gui.GuiToolbarWidgets;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.hopgui.CanvasFacade;
import org.apache.hop.ui.hopgui.HopGui;
import org.apache.hop.ui.hopgui.perspective.TabItemHandler;
import org.apache.hop.ui.hopgui.perspective.dataorch.HopDataOrchestrationPerspective;
import org.apache.hop.ui.hopgui.shared.SwtGc;
import org.apache.hop.ui.hopgui.shared.SwtScrollBar;
import org.apache.hop.workflow.ActionResult;
import org.apache.hop.workflow.WorkflowMeta;
import org.apache.hop.workflow.WorkflowPainter;
import org.apache.hop.workflow.action.ActionMeta;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import java.util.List;
import java.util.*;

@GuiPlugin
public class WorkflowExecutionViewer extends BaseExecutionViewer
    implements IExecutionViewer, PaintListener, MouseListener, MouseMoveListener {
  private static final Class<?> PKG = WorkflowExecutionViewer.class; // For Translator

  public static final String GUI_PLUGIN_TOOLBAR_PARENT_ID = "WorkflowExecutionViewer-Toolbar";

  public static final String TOOLBAR_ITEM_REFRESH = "WorkflowExecutionViewer-Toolbar-10100-Refresh";
  public static final String TOOLBAR_ITEM_ZOOM_LEVEL =
      "WorkflowExecutionViewer-ToolBar-10500-Zoom-Level";
  public static final String TOOLBAR_ITEM_TO_EDITOR =
      "WorkflowExecutionViewer-Toolbar-11100-GoToEditor";
  public static final String TOOLBAR_ITEM_DRILL_DOWN =
      "WorkflowExecutionViewer-Toolbar-11200-DrillDown";
  public static final String TOOLBAR_ITEM_GO_UP = "WorkflowExecutionViewer-Toolbar-11300-GoUp";

  protected final WorkflowMeta workflowMeta;
  protected final ExecutionInfoLocation location;
  protected final Execution execution;

  protected ActionMeta selectedAction;
  protected ExecutionData selectedExecutionData;

  private CTabItem infoTab;
  private TableView infoView;
  private CTabItem logTab;
  private CTabItem dataTab;
  private Text loggingText;
  private SashForm dataSash;
  private org.eclipse.swt.widgets.List dataList;
  private TableView dataView;

  private Map<String, List<ExecutionData>> actionExecutions;

  public WorkflowExecutionViewer(
      Composite parent,
      HopGui hopGui,
      WorkflowMeta workflowMeta,
      ExecutionInfoLocation location,
      Execution execution) {
    super(parent, hopGui);
    this.workflowMeta = workflowMeta;
    this.location = location;
    this.execution = execution;

    actionExecutions = new HashMap<>();

    addWidgets();
  }

  /** Add the widgets in the execution perspective parent tab folder */
  public void addWidgets() {
    setLayout(new FormLayout());

    // A toolbar at the top
    //
    toolBar = new ToolBar(this, SWT.WRAP | SWT.LEFT | SWT.HORIZONTAL);
    toolBarWidgets = new GuiToolbarWidgets();
    toolBarWidgets.registerGuiPluginObject(this);
    toolBarWidgets.createToolbarWidgets(toolBar, GUI_PLUGIN_TOOLBAR_PARENT_ID);
    FormData layoutData = new FormData();
    layoutData.left = new FormAttachment(0, 0);
    layoutData.top = new FormAttachment(0, 0);
    layoutData.right = new FormAttachment(100, 0);
    toolBar.setLayoutData(layoutData);
    toolBar.pack();
    props.setLook(toolBar, Props.WIDGET_STYLE_TOOLBAR);

    // Below the toolbar we have a horizontal splitter: Canvas above and Execution tabs below
    //
    sash = new SashForm(this, SWT.VERTICAL);
    FormData fdSash = new FormData();
    fdSash.left = new FormAttachment(0, 0);
    fdSash.top = new FormAttachment(toolBar, 0);
    fdSash.right = new FormAttachment(100, 0);
    fdSash.bottom = new FormAttachment(100, 0);
    sash.setLayoutData(fdSash);

    // In this sash we have a canvas at the top and a tab folder with information at the bottom
    //
    // The canvas at the top
    //
    canvas = new Canvas(sash, SWT.NO_BACKGROUND);
    FormData fdCanvas = new FormData();
    fdCanvas.left = new FormAttachment(0, 0);
    fdCanvas.top = new FormAttachment(0, 0);
    fdCanvas.right = new FormAttachment(100, 0);
    fdCanvas.bottom = new FormAttachment(100, 0);
    canvas.setLayoutData(fdCanvas);
    canvas.addPaintListener(this);
    canvas.addMouseListener(this);
    canvas.addMouseMoveListener(this);

    // The execution information tabs at the bottom
    //
    tabFolder = new CTabFolder(sash, SWT.MULTI);
    hopGui.getProps().setLook(tabFolder, Props.WIDGET_STYLE_TAB);

    addInfoTab();
    addLogTab();
    addDataTab();

    refresh();

    tabFolder.setSelection(0);
    sash.setWeights(60, 40);
  }

  private void addInfoTab() {
    infoTab = new CTabItem(tabFolder, SWT.NONE);
    infoTab.setImage(GuiResource.getInstance().getImageInfo());
    infoTab.setText(BaseMessages.getString(PKG, "WorkflowExecutionViewer.InfoTab.Title"));

    ColumnInfo[] infoCols =
        new ColumnInfo[] {
          new ColumnInfo("Item", ColumnInfo.COLUMN_TYPE_TEXT, false, true),
          new ColumnInfo("Value", ColumnInfo.COLUMN_TYPE_TEXT, false, true),
        };

    // Let's simply add a table view with all the details on it.
    //
    infoView =
        new TableView(
            hopGui.getVariables(),
            tabFolder,
            SWT.H_SCROLL | SWT.V_SCROLL,
            infoCols,
            1,
            true,
            null,
            props);
    props.setLook(infoView);

    infoTab.setControl(infoView);
  }

  private void refreshStatus() {
    try {
      infoView.clearAll();

      IExecutionInfoLocation iLocation = location.getExecutionInfoLocation();

      ExecutionState state = iLocation.getExecutionState(execution.getId());
      if (state == null) {
        return;
      }

      // Calculate information staleness
      //
      String statusDescription = state.getStatusDescription();
      if ("Running".equals(statusDescription)) {
        long loggingInterval = Const.toLong(location.getDataLoggingInterval(), 20000);
        if (System.currentTimeMillis() - state.getUpdateTime().getTime() > loggingInterval) {
          // The information is stale, not getting updates!
          //
          TableItem item = infoView.add("Update state", "Stale");
          item.setBackground(GuiResource.getInstance().getColorLightBlue());
          item.setForeground(GuiResource.getInstance().getColorWhite());
        }
      }

      infoView.add("Name", execution.getName());
      infoView.add("Type", execution.getExecutionType().name());
      infoView.add("Filename", execution.getFilename());
      infoView.add("ID", execution.getId());
      infoView.add("Parent ID", execution.getParentId());
      infoView.add("Registration", formatDate(execution.getRegistrationDate()));
      infoView.add("Start", formatDate(execution.getExecutionStartDate()));
      infoView.add("Type", state.getExecutionType().name());
      infoView.add("Status", statusDescription);
      infoView.add("Status Last updated", formatDate(state.getUpdateTime()));

      infoView.optimizeTableView();

      loggingText.setText(Const.NVL(state.getLoggingText(), ""));
      // Scroll to the bottom
      loggingText.setSelection(loggingText.getCharCount());

      // Cache the information of all the executed actions...
      //
      actionExecutions.clear();
      for (String id : iLocation.findChildIds(ExecutionType.Workflow, execution.getId())) {
        ExecutionData actionData = iLocation.getExecutionData(execution.getId(), id);

        ExecutionDataSetMeta dataSetMeta = actionData.getDataSetMeta();
        String actionName = dataSetMeta.getName();

        // Add this one under that name
        //
        List<ExecutionData> executionDataList =
            actionExecutions.computeIfAbsent(actionName, k -> new ArrayList<>());
        executionDataList.add(actionData);
      }

    } catch (Exception e) {
      new ErrorDialog(getShell(), "Error", "Error refreshing pipeline status", e);
    }
  }

  private void addDataTab() {
    dataTab = new CTabItem(tabFolder, SWT.NONE);
    dataTab.setImage(GuiResource.getInstance().getImageData());
    dataTab.setText(BaseMessages.getString(PKG, "WorkflowExecutionViewer.DataTab.Title"));

    dataSash = new SashForm(tabFolder, SWT.HORIZONTAL);

    // The list of available data on the left-hand side.
    //
    dataList = new org.eclipse.swt.widgets.List(dataSash, SWT.SINGLE | SWT.LEFT | SWT.V_SCROLL | SWT.H_SCROLL);
    dataList.addListener(SWT.Selection, e -> showDataRows());

    // An empty table view on the right.  This will be populated during a refresh.
    //
    ColumnInfo[] dataColumns = new ColumnInfo[] {};
    dataView =
        new TableView(
            hopGui.getVariables(),
            dataSash,
            SWT.H_SCROLL | SWT.V_SCROLL,
            dataColumns,
            0,
            true,
            null,
            props);
    props.setLook(dataView);

    dataView.optimizeTableView();

    dataSash.setWeights(15, 85);

    dataTab.setControl(dataSash);
  }

  /** An entry is selected in the data list. Show the corresponding rows. */
  private void showDataRows() {
    if (selectedExecutionData == null) {
      return;
    }

    int[] weights = dataSash.getWeights();

    try {
      String[] selection = dataList.getSelection();
      if (selection.length != 1) {
        return;
      }
      String setDescription = selection[0];

      // Look up the key in the metadata cache (built previously)...
      //

      for (ExecutionDataSetMeta setMeta : selectedExecutionData.getSetMetaData().values()) {
        if (setDescription.equals(setMeta.getDescription())) {
          // What's the data for this metadata?
          //
          RowBuffer rowBuffer = selectedExecutionData.getDataSets().get(setMeta.getSetKey());
          List<ColumnInfo> columns = new ArrayList<>();
          IRowMeta rowMeta = rowBuffer.getRowMeta();
          // Add a column for every
          for (IValueMeta valueMeta : rowMeta.getValueMetaList()) {
            ColumnInfo columnInfo =
                new ColumnInfo(
                    valueMeta.getName(), ColumnInfo.COLUMN_TYPE_TEXT, valueMeta.isNumeric());
            columnInfo.setValueMeta(valueMeta);
            columnInfo.setToolTip(valueMeta.toStringMeta());
            columns.add(columnInfo);
          }

          // Dispose of the old table view
          //
          dataView.dispose();

          // Create a new one
          //
          dataView =
              new TableView(
                  hopGui.getVariables(),
                  dataSash,
                  SWT.H_SCROLL | SWT.V_SCROLL,
                  columns.toArray(new ColumnInfo[0]),
                  rowBuffer.size(),
                  true,
                  null,
                  props);

          for (int r = 0; r < rowBuffer.size(); r++) {
            Object[] row = rowBuffer.getBuffer().get(r);
            TableItem item = dataView.table.getItem(r);
            item.setText(0, Integer.toString(r + 1));
            for (int c = 0; c < rowMeta.size(); c++) {
              String value = rowMeta.getString(row, c);
              if (value == null) {
                value = "";
              }
              item.setText(c + 1, value);
            }
          }
          dataView.optWidth(true);
          break;
        }
      }

    } catch (Exception e) {
      new ErrorDialog(getShell(), "Error", "Error showing transform data rows", e);
    } finally {
      layout(true, true);
      dataSash.setWeights(weights);
    }
    canvas.redraw();
  }

  private void addLogTab() {
    logTab = new CTabItem(tabFolder, SWT.NONE);
    logTab.setImage(GuiResource.getInstance().getImageShowLog());
    logTab.setText(BaseMessages.getString(PKG, "WorkflowExecutionViewer.LogTab.Title"));

    loggingText = new Text(tabFolder, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
    props.setLook(loggingText);

    logTab.setControl(loggingText);
  }

  @Override
  public Image getTitleImage() {
    return GuiResource.getInstance().getImageWorkflow();
  }

  @Override
  public String getTitleToolTip() {
    return workflowMeta.getDescription();
  }

  @Override
  public void paintControl(PaintEvent e) {
    Point area = getArea();
    if (area.x == 0 || area.y == 0) {
      return; // nothing to do!
    }

    // Do double buffering to prevent flickering on Windows
    //
    boolean needsDoubleBuffering =
        Const.isWindows() && "GUI".equalsIgnoreCase(Const.getHopPlatformRuntime());

    Image image = null;
    GC swtGc = e.gc;

    if (needsDoubleBuffering) {
      image = new Image(hopDisplay(), area.x, area.y);
      swtGc = new GC(image);
    }

    drawWorkflowImage(swtGc, area.x, area.y, magnification);

    if (needsDoubleBuffering) {
      // Draw the image onto the canvas and get rid of the resources
      //
      e.gc.drawImage(image, 0, 0);
      swtGc.dispose();
      image.dispose();
    }

    setZoomLabel();
  }

  public void setZoomLabel() {
    Combo combo = (Combo) toolBarWidgets.getWidgetsMap().get(TOOLBAR_ITEM_ZOOM_LEVEL);
    if (combo == null || combo.isDisposed()) {
      return;
    }
    String newString = Math.round(magnification * 100) + "%";
    String oldString = combo.getText();
    if (!newString.equals(oldString)) {
      combo.setText(Math.round(magnification * 100) + "%");
    }
  }

  protected Point getArea() {
    org.eclipse.swt.graphics.Rectangle rect = canvas.getClientArea();
    Point area = new Point(rect.width, rect.height);

    return area;
  }

  public void drawWorkflowImage(GC swtGc, int width, int height, float magnificationFactor) {

    IGc gc = new SwtGc(swtGc, width, height, iconSize);
    try {
      PropsUi propsUi = PropsUi.getInstance();

      int gridSize = propsUi.isShowCanvasGridEnabled() ? propsUi.getCanvasGridSize() : 1;

      ScrollBar horizontalScrollBar = canvas.getHorizontalBar();
      ScrollBar verticalScrollBar = canvas.getVerticalBar();

      WorkflowPainter workflowPainter =
          new WorkflowPainter(
              gc,
              hopGui.getVariables(),
              workflowMeta,
              new Point(width, height),
              horizontalScrollBar == null ? null : new SwtScrollBar(horizontalScrollBar),
              verticalScrollBar == null ? null : new SwtScrollBar(verticalScrollBar),
              null,
              null,
              areaOwners,
              propsUi.getIconSize(),
              propsUi.getLineWidth(),
              gridSize,
              propsUi.getNoteFont().getName(),
              propsUi.getNoteFont().getHeight(),
              propsUi.getZoomFactor(),
              false);

      // correct the magnification with the overall zoom factor
      //
      float correctedMagnification = (float) (magnificationFactor * propsUi.getZoomFactor());

      workflowPainter.setMagnification(correctedMagnification);
      workflowPainter.setOffset(offset);

      // See if we can get status' information for the actions...
      //
      List<ActionResult> actionResults = new ArrayList<>();
      List<ActionMeta> activeActions = new ArrayList<>();
      if (actionExecutions != null) {
        for (String actionName : actionExecutions.keySet()) {
          List<ExecutionData> executionDataList = actionExecutions.get(actionName);
          if (executionDataList != null && !executionDataList.isEmpty()) {
            // Just consider the first
            //
            ExecutionData executionData = executionDataList.get(0);

            RowBuffer rowBuffer = executionData.getDataSets().get(ExecutionDataBuilder.KEY_RESULT);
            if (rowBuffer != null) {
              IRowMeta rowMeta = rowBuffer.getRowMeta();
              Result result = new Result();
              for (Object[] row : rowBuffer.getBuffer()) {
                try {
                  if (rowMeta.getString(row, 0).equals(ExecutionDataBuilder.RESULT_KEY_RESULT)) {
                    result.setResult("true".equalsIgnoreCase(rowMeta.getString(row, 1)));
                  }
                  if (rowMeta.getString(row, 0).equals(ExecutionDataBuilder.RESULT_KEY_ERRORS)) {
                    result.setNrErrors(Long.parseLong(rowMeta.getString(row, 1)));
                  }
                  if (rowMeta.getString(row, 0).equals(ExecutionDataBuilder.RESULT_KEY_STOPPED)) {
                    result.setStopped("true".equalsIgnoreCase(rowMeta.getString(row, 1)));
                  }
                } catch (Exception e) {
                  LogChannel.UI.logError("Error getting action result information", e);
                }
              }
              ActionResult actionResult = new ActionResult();
              actionResult.setResult(result);
              actionResult.setActionName(actionName);
              actionResults.add(actionResult);
            }
            if (!executionData.isFinished()) {
              ActionMeta actionMeta = workflowMeta.findAction(actionName);
              if (actionMeta != null) {
                activeActions.add(actionMeta);
              }
            }
          }
        }
      }
      workflowPainter.setActionResults(actionResults);
      workflowPainter.setActiveActions(activeActions);

      try {
        workflowPainter.drawWorkflow();
      } catch (Exception e) {
        new ErrorDialog(hopGui.getShell(), "Error", "Error drawing pipeline image", e);
      }
    } finally {
      gc.dispose();
    }
    CanvasFacade.setData(canvas, magnification, workflowMeta, WorkflowExecutionViewer.class);
  }

  @Override
  public Control getControl() {
    return this;
  }

  /** Refresh the information in the execution panes */
  @GuiToolbarElement(
      root = GUI_PLUGIN_TOOLBAR_PARENT_ID,
      id = TOOLBAR_ITEM_REFRESH,
      toolTip = "i18n::WorkflowExecutionViewer.ToolbarElement.Refresh.Tooltip",
      image = "ui/images/refresh.svg")
  @GuiKeyboardShortcut(key = SWT.F5)
  @GuiOsxKeyboardShortcut(key = SWT.F5)
  public void refresh() {
    refreshStatus();
    refreshActionData();
  }

  @GuiToolbarElement(
      root = GUI_PLUGIN_TOOLBAR_PARENT_ID,
      id = TOOLBAR_ITEM_ZOOM_LEVEL,
      label = "i18n:org.apache.hop.ui.hopgui:HopGui.Toolbar.Zoom",
      toolTip = "i18n::HopGuiPipelineGraph.GuiAction.ZoomInOut.Tooltip",
      type = GuiToolbarElementType.COMBO,
      alignRight = true,
      comboValuesMethod = "getZoomLevels")
  public void zoomLevel() {
    readMagnification();
    canvas.redraw();
  }

  @GuiToolbarElement(
      root = GUI_PLUGIN_TOOLBAR_PARENT_ID,
      id = TOOLBAR_ITEM_TO_EDITOR,
      toolTip = "i18n::WorkflowExecutionViewer.ToolbarElement.NavigateToEditor.Tooltip",
      image = "ui/images/data_orch.svg")
  public void navigateToEditor() {
    try {
      // First try to see if this workflow is running in Hop GUI...
      //
      HopDataOrchestrationPerspective perspective = HopGui.getDataOrchestrationPerspective();
      TabItemHandler item = perspective.findWorkflow(execution.getId());
      if (item != null) {
        perspective.switchToTab(item);
        perspective.activate();
        return;
      }

      // Now see if we have a filename to match with
      //
      String filename = execution.getFilename();
      if (filename != null) {
        hopGui.fileDelegate.fileOpen(filename);
        return;
      }

    } catch (Exception e) {
      new ErrorDialog(getShell(), "Error", "Error navigating to workflow in Hop GUI", e);
    }
  }

  public List<String> getZoomLevels() {
    return Arrays.asList(PipelinePainter.magnificationDescriptions);
  }

  /** Allows for magnifying to any percentage entered by the user... */
  private void readMagnification() {
    Combo zoomLabel = (Combo) toolBarWidgets.getWidgetsMap().get(TOOLBAR_ITEM_ZOOM_LEVEL);
    if (zoomLabel == null) {
      return;
    }
    String possibleText = zoomLabel.getText().replace("%", "");

    float possibleFloatMagnification;
    try {
      possibleFloatMagnification = Float.parseFloat(possibleText) / 100;
      magnification = possibleFloatMagnification;
      if (zoomLabel.getText().indexOf('%') < 0) {
        zoomLabel.setText(zoomLabel.getText().concat("%"));
      }
    } catch (Exception e) {
      // Ignore
    }

    canvas.setFocus();
    redraw();
  }

  /**
   * Gets name
   *
   * @return value of name
   */
  public String getName() {
    return workflowMeta.getName();
  }

  /**
   * Gets logChannelId
   *
   * @return value of logChannelId
   */
  @Override
  public String getLogChannelId() {
    return execution.getId();
  }

  @Override
  public void mouseDoubleClick(MouseEvent e) {}

  @Override
  public void mouseDown(MouseEvent e) {
    Point real = screen2real(e.x, e.y);
    AreaOwner areaOwner = getVisibleAreaOwner(real.x, real.y);
    if (areaOwner == null) {
      return;
    }

    workflowMeta.unselectAll();

    switch (areaOwner.getAreaType()) {
      case ACTION_ICON:
        // Show the data for this transform
        //
        selectedAction = (ActionMeta) areaOwner.getOwner();
        refreshActionData();
        break;
    }

    canvas.redraw();
  }

  public void refreshActionData() {
    if (selectedAction != null) {
      showActionData(selectedAction);
    }
  }

  private void showActionData(ActionMeta actionMeta) {
    actionMeta.setSelected(true);

    // Remember which entry in the list was selected.
    //
    String previousListSelection = null;
    if (dataList.getSelectionCount() == 1) {
      previousListSelection = dataList.getSelection()[0];
    }
    dataList.removeAll();

    try {
      // Find the data for the selected action...
      //
      List<ExecutionData> executionDataList = actionExecutions.get(actionMeta.getName());
      if (executionDataList == null || executionDataList.isEmpty()) {
        return;
      }

      // Get any execution data for the selected action
      // TODO: ask the user to select which one...
      //
      selectedExecutionData = executionDataList.get(0);

      Map<String, ExecutionDataSetMeta> setMetaData = selectedExecutionData.getSetMetaData();
      List<String> items = new ArrayList<>();
      for (String key : setMetaData.keySet()) {
        ExecutionDataSetMeta setMeta = setMetaData.get(key);
        if (actionMeta.getName().equals(setMeta.getName())) {
          // We're in the right place.  We can have different types of data though.
          // We list the types in the List on the left in the data tab.
          //
          items.add(setMeta.getDescription());
        }
      }
      Collections.sort(items);
      dataList.setItems(items.toArray(new String[0]));
      tabFolder.setSelection(dataTab);
      if (previousListSelection != null && items.contains(previousListSelection)) {
        dataList.setSelection(items.indexOf(previousListSelection));
      } else {
        dataList.setSelection(0);
      }
      showDataRows();
    } catch (Exception e) {
      new ErrorDialog(getShell(), "Error", "Error showing transform data", e);
    }
  }

  @Override
  public void mouseUp(MouseEvent e) {}

  @Override
  public void mouseMove(MouseEvent e) {}

  @GuiToolbarElement(
      root = GUI_PLUGIN_TOOLBAR_PARENT_ID,
      id = TOOLBAR_ITEM_DRILL_DOWN,
      toolTip = "i18n::WorkflowExecutionViewer.ToolbarElement.DrillDown.Tooltip",
      image = "ui/images/down.svg")
  public void drillDown() {
    if (selectedExecutionData == null) {
      return;
    }

    // We need to look up a pipeline or workflow execution where the parent is the ID of the action
    //
    try {
      String id = selectedExecutionData.getOwnerId();
      List<Execution> childExecutions = location.getExecutionInfoLocation().findChildExecutions(id);
      if (childExecutions.isEmpty()) {
        return;
      }

      Execution child;

      if (childExecutions.size() == 1) {
        child = childExecutions.get(0);
      } else {
        // Select the execution...
        //
        IRowMeta rowMeta =
            new RowMetaBuilder()
                .addString("Name")
                .addString("Type")
                .addDate("Start date")
                .build();
        List<RowMetaAndData> rows = new ArrayList<>();
        for (Execution childExecution : childExecutions) {
          rows.add(new RowMetaAndData(rowMeta,
                  childExecution.getName(),
                  childExecution.getExecutionType().name(),
                  childExecution.getExecutionStartDate()));
        }

        SelectRowDialog dialog =
            new SelectRowDialog(
                getShell(), hopGui.getVariables(), SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL, rows);
        RowMetaAndData selectedRow = dialog.open();
        if (selectedRow==null) {
          // Operation is canceled
          return;
        }
        int index = rows.indexOf(selectedRow);
        if (index<0) {
          return;
        }
        child = childExecutions.get(index);
      }

      // Open this one
      //
      HopGui.getExecutionPerspective().createExecutionViewer(location, child);
    } catch (Exception e) {
      new ErrorDialog(getShell(), "Error", "Error drilling down into selected action", e);
    }
  }

  @GuiToolbarElement(
      root = GUI_PLUGIN_TOOLBAR_PARENT_ID,
      id = TOOLBAR_ITEM_GO_UP,
      toolTip = "i18n::WorkflowExecutionViewer.ToolbarElement.GoUp.Tooltip",
      image = "ui/images/up.svg")
  public void goUp() {
    try {
      String parentId = execution.getParentId();
      if (parentId == null) {
        return;
      }

      // This parent ID is the ID of the transform or action.
      // We need to find the parent of this child
      //
      String grandParentId = location.getExecutionInfoLocation().findParentId(parentId);
      if (grandParentId == null) {
        return;
      }

      Execution grandParent = location.getExecutionInfoLocation().getExecution(grandParentId);

      // Open this one
      //
      HopGui.getExecutionPerspective().createExecutionViewer(location, grandParent);
    } catch (Exception e) {
      new ErrorDialog(getShell(), "Error", "Error navigating up to parent execution", e);
    }
  }
}