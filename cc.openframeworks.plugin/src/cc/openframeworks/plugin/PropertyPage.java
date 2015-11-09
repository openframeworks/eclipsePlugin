package cc.openframeworks.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import cc.openframeworks.plugin.wizards.SelectAddonPage;

public class PropertyPage extends org.eclipse.ui.dialogs.PropertyPage {

	private static final String PATH_TITLE = "&openFrameworks Path:";
	private static final int TEXT_FIELD_WIDTH = 60;
	FileChooser pathValueText;
	SelectAddonPage addonsPage;
	
	public PropertyPage() {
		super();
	}

	private void addFirstSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		//Label for path field
		Label pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText(PATH_TITLE);

		// Path text field
		pathValueText = new FileChooser(composite, true);
		GridData gd = new GridData();
		gd.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
		pathValueText.setLayoutData(gd);
		try {
			pathValueText.setText(((IResource) getElement()).getPersistentProperty(new QualifiedName("", "OF_ROOT")).toString());
		} catch (Exception e) {
			pathValueText.setText(Activator.getDefault().getPreferenceStore().getString("OF_ROOT"));
		}
	}

	private void addAddonsSection(Composite parent) {
		Composite composite = createAddonsComposite(parent);
		addonsPage = new SelectAddonPage();
		addonsPage.createControl(composite);
		
		File projectPath = ((IResource) getElement()).getProject().getLocation().toFile();
		File addonsMake = new File(projectPath.getAbsolutePath() + "/addons.make");
		if(!addonsMake.exists()){
			try {
				addonsMake.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Set<String> currentAddons = new HashSet<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(addonsMake));
			String line;
			while ((line = br.readLine()) != null) {
			   currentAddons.add(line);
			}
			br.close();
		} catch (IOException e1) {
		}
		addonsPage.setAddons(currentAddons);
	}

	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		addFirstSection(composite);
		addAddonsSection(composite);
		return composite;
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	private Composite createAddonsComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	protected void performDefaults() {
		super.performDefaults();
	}
	
	public boolean performOk() {
		// store the value in the owner text field
		try {
			String prevOfRoot = "";
			try{
				prevOfRoot = ((IResource) getElement()).getPersistentProperty(new QualifiedName("", "OF_ROOT")).toString();
			}catch(Exception e){
				
			}
			String ofRoot = pathValueText.getText();
			Utils.reparseAddons(((IResource) getElement()).getProject(), addonsPage.getSelectedAddons(), ofRoot);
			if(!ofRoot.equals(prevOfRoot)){
				((IResource) getElement()).setPersistentProperty(
					new QualifiedName("", "OF_ROOT"),
					ofRoot);
				IProject project = ((IResource) getElement()).getProject();
				IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
				IConfiguration debugConfig = buildInfo.getManagedProject().getConfiguration("cc.openframeworks.configurations.debug");
				IBuilder bld = debugConfig.getEditableBuilder();
				
				bld.setManagedBuildOn(false);
				bld.setCommand("make");
				if(ofRoot!=null && !ofRoot.equals("")){
					bld.setIncrementalBuildTarget("Debug OF_ROOT="+ofRoot);
				}else{
					bld.setIncrementalBuildTarget("Debug");
				}
				
				IConfiguration releaseConfig = buildInfo.getManagedProject().getConfiguration("cc.openframeworks.configurations.release");
				bld = releaseConfig.getEditableBuilder();
				
				bld.setManagedBuildOn(false);
				bld.setCommand("make");
				if(ofRoot!=null && !ofRoot.equals("")){
					bld.setIncrementalBuildTarget("Release OF_ROOT="+ofRoot);
				}else{
					bld.setIncrementalBuildTarget("Release");
				}
				WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
					public void execute(IProgressMonitor monitor) throws CoreException{
						try {
							project.build(project.getActiveBuildConfig(),IncrementalProjectBuilder.CLEAN_BUILD,monitor);
							project.build(project.getActiveBuildConfig(),IncrementalProjectBuilder.INCREMENTAL_BUILD,monitor);
						} finally {
							monitor.done();
						}
					}
				};
				((IResource) getElement()).getWorkspace().run(new IWorkspaceRunnable() {
					
					@Override
					public void run(IProgressMonitor monitor) throws CoreException {
						try {
							op.run(monitor);
						} catch (InvocationTargetException | InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				},null);
			}
		} catch (CoreException e) {
			return false;
		}
		return true;
	}

}