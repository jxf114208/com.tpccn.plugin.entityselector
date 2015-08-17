package com.tpccn.plugin.entityselector.ui.dialog;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.internal.dialogs.TreeManager;
import org.eclipse.ui.internal.dialogs.TreeManager.TreeItem;

/**
 * 实体类选择弹出框
 * 
 * @author jiaxiaofeng
 *
 */
public class EntitySelectorDialog extends SelectionDialog {

	public EntitySelectorDialog(Shell parentShell) {
		super(parentShell);
	}
	
	private CheckboxTreeViewer viewer;
	private Object input;
	private TreeManager treeManager = new TreeManager();
	private boolean fIsEmpty;
	
	public TreeManager getTreeManager() {
		return treeManager;
	}

	public CheckboxTreeViewer getTreeViewer() {
		return viewer;
	}

	public void setInput(Object fInput) {
		this.input = fInput;
	}
	
	@Override
	public int open() {
		this.fIsEmpty = evaluateIfTreeEmpty(input);
		return super.open();
	}


	@Override
	public boolean close() {
		treeManager.dispose();
		return super.close();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Label messageLabel = createMessageArea(composite);
		CheckboxTreeViewer treeViewer = createTreeViewer(composite);
		Control buttonComposite = createSelectionButtons(composite);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = convertWidthInCharsToPixels(80);
		data.heightHint = convertHeightInCharsToPixels(20);
		Tree treeWidget = treeViewer.getTree();
		treeWidget.setLayoutData(data);
		treeWidget.setFont(parent.getFont());
		if (fIsEmpty) {
            messageLabel.setEnabled(false);
            treeWidget.setEnabled(false);
            buttonComposite.setEnabled(false);
        }
		return composite;
	}

	
	/**
	 * 创建CheckboxTreeViewer
	 * @param composite
	 * @return
	 */
	private CheckboxTreeViewer createTreeViewer(Composite composite) {
		viewer = new CheckboxTreeViewer(composite, SWT.BORDER);
		treeManager.attachAll(viewer);
		viewer.setInput(input);
		return viewer;
	}
	
	 /**
     * 增加全选和全不选按钮
     * 
     * @param composite
     *            the parent composite
     * @return Composite the composite the buttons were created in.
     */
	private Composite createSelectionButtons(Composite composite) {
        Composite buttonComposite = new Composite(composite, SWT.RIGHT);
        GridLayout layout = new GridLayout();
        layout.numColumns = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		buttonComposite.setLayout(layout);
        buttonComposite.setFont(composite.getFont());
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END
                | GridData.GRAB_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        buttonComposite.setLayoutData(data);
        Button selectButton = createButton(buttonComposite,
                IDialogConstants.SELECT_ALL_ID, "全选",
                false);
        SelectionListener listener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
            	@SuppressWarnings("unchecked")
				List<TreeItem> items = ((TreeItem)input).getChildren();
            	for (TreeItem treeItem : items) {
            		treeItem.setChangedByUser(true);
            		treeItem.setCheckState(true);
				}
            }
        };
        selectButton.addSelectionListener(listener);
        Button deselectButton = createButton(buttonComposite,
                IDialogConstants.DESELECT_ALL_ID, "全不选",
                false);
        listener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
            	@SuppressWarnings("unchecked")
				List<TreeItem> items = ((TreeItem)input).getChildren();
            	for (TreeItem treeItem : items) {
            		treeItem.setChangedByUser(true);
            		treeItem.setCheckState(false);
				}
            }
        };
        deselectButton.addSelectionListener(listener);
        return buttonComposite;
    }
	
	@Override
	protected void okPressed() {
		setResult(Arrays.asList(viewer.getCheckedElements()));
		super.okPressed();
	}
	
	 /**
     * Handles cancel button pressed event.
     */
    protected void cancelPressed() {
        setResult(null);
        super.cancelPressed();
    }
    
    private boolean evaluateIfTreeEmpty(Object input) {
        Object[] elements = TreeManager.getTreeContentProvider().getElements(input);
        return elements.length == 0;
    }
}
