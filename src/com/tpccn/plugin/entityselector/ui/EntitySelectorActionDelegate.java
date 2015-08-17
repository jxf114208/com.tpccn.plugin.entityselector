package com.tpccn.plugin.entityselector.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jpt.common.core.internal.resource.java.binary.BinaryPackageFragmentRoot;
import org.eclipse.jpt.common.core.resource.java.JavaResourceAbstractType;
import org.eclipse.jpt.common.core.resource.java.JavaResourceClassFile;
import org.eclipse.jpt.common.core.resource.java.JavaResourcePackageFragment;
import org.eclipse.jpt.jpa.core.JpaFile;
import org.eclipse.jpt.jpa.core.JpaNode;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.context.persistence.PersistenceUnit;
import org.eclipse.jpt.jpa.ui.JpaRootContextNodeModel;
import org.eclipse.jpt.jpadiagrameditor.ui.internal.JPADiagramEditor;
import org.eclipse.jpt.jpadiagrameditor.ui.internal.JPADiagramEditorPlugin;
import org.eclipse.jpt.jpadiagrameditor.ui.internal.i18n.JPAEditorMessages;
import org.eclipse.jpt.jpadiagrameditor.ui.internal.util.JpaArtifactFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.dialogs.TreeManager;
import org.eclipse.ui.internal.dialogs.TreeManager.TreeItem;

import com.tpccn.plugin.entityselector.Activator;
import com.tpccn.plugin.entityselector.ui.dialog.EntitySelectorDialog;

public class EntitySelectorActionDelegate implements IObjectActionDelegate {

	private JpaProject jpaProject;
	private Shell shell;
	private WeakReference<ISelection> selectionRef = new WeakReference<ISelection>(null);

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	public void run(IAction action) {

		PersistenceUnit persistenceUnit = null;
		try {
			persistenceUnit = obtainJpaProjectAndPersistenceUnit(selectionRef.get());
		} catch (Exception e) {
			handleException(e);
			return;
		}

		ElementTree et = initElementTree();
		LabelProvider labelProvider = new LabelProvider(et);
		TreeContentProvider treeContentProvider = new TreeContentProvider(et);

		EntitySelectorDialog dialog = new EntitySelectorDialog(shell);

		dialog.setTitle("选择 JPA Entity");
		dialog.setMessage("在下方选择要在实体类关系图中进行显示的实体类:");

		dialog.setInput(initTreeItem(Path.ROOT, dialog.getTreeManager(), labelProvider, treeContentProvider));

		int returnCode = dialog.open();

		if (returnCode == Dialog.OK) {
			// 移除persistenceUnit中的所有classRef
			int specifiedClassRefsSize = persistenceUnit.getSpecifiedClassRefsSize();
			while (specifiedClassRefsSize-- > 0) {
				persistenceUnit.removeSpecifiedClassRef(0);
			}

			// 在persistenceUnit中增加选中的实体类
			Object[] result = dialog.getResult();
			List<String> classNameList = convertDialogResultToClassName(result);
			for (String className : classNameList) {
				persistenceUnit.addSpecifiedClassRef(className);
			}

			persistenceUnit.setSpecifiedExcludeUnlistedClasses(true);
		}
	}

	/**
	 * 转换选择结果中的元素为class
	 * 
	 * @param result
	 * @return
	 */
	private List<String> convertDialogResultToClassName(Object[] result) {
		if (result != null && result.length > 0) {
			List<String> classNameList = new ArrayList<String>();
			for (Object object : result) {
				TreeItem treeItem = (TreeItem) object;
				if (treeItem.getParent().getParent() == null) {
					convertTreeItemToClassName(treeItem, classNameList, null);
				}
			}
			return classNameList;
		}
		return Collections.emptyList();
	}

	/**
	 * 转换Tree中的元素对象TreeItem为class
	 * 
	 * @param treeItem
	 * @param classNameList
	 * @param strBuilder
	 */
	private void convertTreeItemToClassName(TreeItem treeItem, List<String> classNameList, StringBuilder strBuilder) {
		if (!treeItem.getState())
			return;

		String label = treeItem.getLabel();
		if (strBuilder == null) {
			strBuilder = new StringBuilder(label);
		} else if (strBuilder.length() != 0) {
			strBuilder.append(".").append(label);
		} else {
			strBuilder.append(label);
		}

		@SuppressWarnings("unchecked")
		List<TreeItem> children = treeItem.getChildren();

		if (children != null && children.size() > 0) {
			int length = children.size();
			StringBuilder parentBuilder = null;
			if (length > 1) {
				parentBuilder = new StringBuilder(strBuilder.toString());
			}

			for (int i = 0; i < length; i++) {
				if (i > 0 && children.get(i - 1).getState())
				{
					strBuilder.append(parentBuilder);
				}
				//排除最后一个子元素未选中时，清空parentBuilder
				else if(i + 1 == length && !children.get(i).getState())
				{
					strBuilder.delete(0, strBuilder.length());
					return;
				}
				convertTreeItemToClassName(children.get(i), classNameList, strBuilder);
			}
		} else {
			classNameList.add(strBuilder.toString());
			strBuilder.delete(0, strBuilder.length());
		}
	}

	/**
	 * 初始化TreeItem对象
	 * 
	 * @return
	 */
	private TreeItem initTreeItem(Object element, TreeManager treeManager, LabelProvider labelProvider, TreeContentProvider treeContentProvider) {
		TreeItem treeItem = treeManager.new TreeItem(labelProvider.getText(element));

		if (treeContentProvider.hasChildren(element)) {
			treeItem.setImageDescriptor(Activator.PACKAGE_ICON_DESCRIPTOR);
			Object[] children = treeContentProvider.getChildren(element);
			for (Object object : children) {
				TreeItem clildTreeItem = initTreeItem(object, treeManager, labelProvider, treeContentProvider);
				treeItem.addChild(clildTreeItem);
			}
		} else {
			treeItem.setImageDescriptor(Activator.CLASS_ICON_DESCRIPTOR);
		}

		return treeItem;
	}

	/**
	 * 初始化ElementTree对象
	 * 
	 * @return
	 */
	private ElementTree initElementTree() {
		final ElementTree et = new ElementTree();

		// 取得java源文件中的实体类
		Iterable<String> annotatedJavaSourceClassNames = jpaProject.getAnnotatedJavaSourceClassNames();
		Iterator<String> iterator = annotatedJavaSourceClassNames.iterator();
		while (iterator.hasNext()) {
			addElementToTree(et, (String) iterator.next());
		}

		// 取得底层jar包中的实体类
		Iterable<JpaFile> jarJpaFiles = jpaProject.getJarJpaFiles();
		Iterator<JpaFile> jarJpaFilesIterator = jarJpaFiles.iterator();
		while (jarJpaFilesIterator.hasNext()) {
			JpaFile jpaFile = (JpaFile) jarJpaFilesIterator.next();
			IFile file = jpaFile.getFile();
			String name = file.getName();
			if (name.startsWith("tpcframework.") || name.startsWith("bap.")) {
				BinaryPackageFragmentRoot binaryPackageFragmentRoot = (BinaryPackageFragmentRoot) jpaFile.getResourceModel();
				Iterable<JavaResourcePackageFragment> packageFragments = binaryPackageFragmentRoot.getPackageFragments();
				Iterator<JavaResourcePackageFragment> iterator2 = packageFragments.iterator();
				while (iterator2.hasNext()) {
					JavaResourcePackageFragment javaResourcePackageFragment = (JavaResourcePackageFragment) iterator2.next();
					Iterable<JavaResourceClassFile> classFiles = javaResourcePackageFragment.getClassFiles();
					Iterator<JavaResourceClassFile> iterator3 = classFiles.iterator();
					while (iterator3.hasNext()) {
						JavaResourceClassFile javaResourceClassFile = (JavaResourceClassFile) iterator3.next();
						JavaResourceAbstractType type = javaResourceClassFile.getType();
						if (type.isAnnotatedWithAnyOf(jpaProject.getTypeMappingAnnotationNames())) {
							addElementToTree(et, type.getQualifiedName());
						}
					}
				}
			}
		}
		return et;
	}

	/**
	 * 在ElementTree对象中增加element
	 * 
	 * @param et
	 * @param str
	 */
	private void addElementToTree(final ElementTree et, String str) {
		StringBuilder annotatedJavaSourceClassName = new StringBuilder(str.replace(".", "/"));
		IPath fullPath = Path.fromOSString(annotatedJavaSourceClassName.toString()).addFileExtension("class");

		if (!et.includesIgnoreCase(fullPath)) {
			int indexOf = annotatedJavaSourceClassName.indexOf("/");
			while (indexOf >= 0) {
				IPath partPath = Path.fromOSString(annotatedJavaSourceClassName.substring(0, indexOf));

				if (!et.includesIgnoreCase(partPath)) {
					et.createElement(partPath, partPath.lastSegment());
				}
				indexOf = annotatedJavaSourceClassName.indexOf("/", indexOf + 1);
				if (indexOf < 0) {
					et.createElement(fullPath, fullPath.removeFileExtension().lastSegment());
				}
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		selectionRef = new WeakReference<ISelection>(selection);
	}

	private PersistenceUnit obtainJpaProjectAndPersistenceUnit(ISelection selection) throws CoreException {
		Object firstElement = ((IStructuredSelection) selection).getFirstElement();
		if (firstElement instanceof JpaRootContextNodeModel) {
			jpaProject = JpaArtifactFactory.instance().getJpaProject(((JpaRootContextNodeModel) firstElement).getProject());
		} else if (firstElement instanceof JpaNode) {
			jpaProject = ((JpaNode) firstElement).getJpaProject();
		} else if (firstElement instanceof IProject) {
			jpaProject = JpaArtifactFactory.instance().getJpaProject((IProject) firstElement);
			int cnt = 0;
			while ((jpaProject == null) && (cnt < 25)) {
				jpaProject = JpaArtifactFactory.instance().getJpaProject((IProject) firstElement);
				if (jpaProject == null) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						JPADiagramEditorPlugin.logError("Thread sleep interrupted", e); //$NON-NLS-1$		 
					}
				}
				cnt++;
			}
		}
		return JpaArtifactFactory.instance().getPersistenceUnit(jpaProject);
	}

	private void handleException(Exception e) {
		IStatus status = new ErrStatus(IStatus.ERROR, JPADiagramEditor.ID, e.toString(), e);
		ErrorDialog.openError(shell, JPAEditorMessages.OpenJpaDiagramActionDelegate_openJPADiagramErrorMsgTitle, "dddddddddddddddddd", status);
	}

	private class ErrStatus extends Status {

		public ErrStatus(int severity, String pluginId, String message, Throwable exception) {
			super(severity, message, message, exception);
		}

		public IStatus[] getChildren() {
			StackTraceElement[] st = getException().getStackTrace();
			IStatus[] res = new IStatus[st == null ? 0 : st.length];
			for (int i = 0; i < st.length; i++)
				res[i] = new Status(IStatus.ERROR, JPADiagramEditor.ID, st[i].toString());
			return res;
		}
	}

	private class LabelProvider implements ILabelProvider {

		public LabelProvider(ElementTree et) {
			this.et = et;
		}

		private ElementTree et;

		@Override
		public void removeListener(ILabelProviderListener listener) {

		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void dispose() {

		}

		@Override
		public void addListener(ILabelProviderListener listener) {

		}

		@Override
		public String getText(Object element) {
			if (element instanceof IPath) {
				IPath key = (IPath) element;
				return (String) et.getElementData(key);
			}
			return null;
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}

	}

	private class TreeContentProvider implements ITreeContentProvider {

		public TreeContentProvider(ElementTree et) {
			this.et = et;
		}

		private ElementTree et;

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean hasChildren(Object element) {
			IPath key = (IPath) element;
			return et.getChildCount(key) > 0;
		}

		@Override
		public Object getParent(Object element) {
			IPath key = (IPath) element;
			IPath parentPath = key.removeLastSegments(1);
			return parentPath.isRoot() ? null : parentPath;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			IPath key = (IPath) inputElement;
			return et.getChildren(key);
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			IPath key = (IPath) parentElement;
			return et.getChildren(key);
		}
	}
}